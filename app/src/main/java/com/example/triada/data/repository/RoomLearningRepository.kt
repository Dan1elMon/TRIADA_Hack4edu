package com.example.triada.data.repository

import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.*
import com.example.triada.domain.repository.LearningRepository
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomLearningRepository(private val database: LearningDatabase) : LearningRepository {
    private val dao = database.learningDao()
    override suspend fun dailyGoal(profileId:String):Pair<Int,Int> {
        requireNotNull(dao.profile(profileId))
        val start=java.util.Calendar.getInstance().apply{set(java.util.Calendar.HOUR_OF_DAY,0);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0)}.timeInMillis
        val end=java.util.Calendar.getInstance().apply{timeInMillis=start;add(java.util.Calendar.DAY_OF_MONTH,1)}.timeInMillis
        val done=dao.profileAttempts(profileId).filter{it.completedAt?.let{t->t>=start&&t<end}==true}.map{it.lessonId}.distinct().size
        return done to (database.extensionDao().goal(profileId)?.target ?: 1)
    }
    override suspend fun setDailyGoal(profileId:String,target:Int) {
        requireNotNull(dao.profile(profileId));require(target in 1..3)
        database.extensionDao().saveGoal(DailyGoalEntity(profileId,target))
    }
    override suspend fun updateProfile(id: String, name: String, avatar: String) = database.withTransaction {
        requireNotNull(dao.profile(id))
        require(name.trim().length in 1..24 && avatar in listOf("sun","star","flower"))
        dao.updateProfile(id,name.trim(),avatar)
    }
    override suspend fun rewards(profileId: String): List<EarnedSticker> = database.withTransaction {
        requireNotNull(dao.profile(profileId))
        database.rewardsDao().earned(profileId).map { EarnedSticker(it.profileId, it.stickerId, it.obtainedAt) }
    }

    override fun profiles() = dao.profiles().map { rows ->
        rows.map { LearnerProfile(it.id, it.displayName, it.avatar) }
    }

    override suspend fun createProfile(name: String, avatar: String): LearnerProfile {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty() && cleanName.length <= 24)
        require(avatar in listOf("sun", "star", "flower"))
        val profile = LearnerProfile(UUID.randomUUID().toString(), cleanName, avatar)
        dao.insertProfile(ProfileEntity(profile.id, cleanName, avatar, System.currentTimeMillis()))
        return profile
    }

    /** Inserting the same published version is idempotent and never replaces learner data. */
    suspend fun seed() = database.withTransaction {
        val lesson = StarterContent.lesson
        dao.insertLesson(LessonEntity(lesson.id, lesson.version, lesson.title, lesson.objective))
        dao.insertQuestions(lesson.questions.mapIndexed { index, q ->
            QuestionEntity(q.id, lesson.version, lesson.id, index, q.prompt, q.objectCount)
        })
        dao.insertOptions(lesson.questions.flatMap { q ->
            q.options.mapIndexed { index, value -> OptionEntity(q.id, lesson.version, value, index) }
        })
        dao.insertHints(lesson.questions.flatMap { q ->
            q.hints.mapIndexed { index, text -> HintEntity(q.id, lesson.version, index, text) }
        })
        AdditionalContent.lessons.forEach { additional ->
            dao.insertLesson(LessonEntity(additional.id, additional.version, additional.title, additional.objective))
            dao.insertQuestions(additional.questions.mapIndexed { index, q ->
                val content = q.exercise.content
                QuestionEntity(content.id, additional.version, additional.id, index, content.prompt, 0, content.type.name,
                    ActivityCodec.payload(content), ActivityCodec.key(q.exercise))
            })
            dao.insertHints(additional.questions.flatMap { q ->
                q.hints.mapIndexed { index, text -> HintEntity(q.exercise.content.id, additional.version, index, text) }
            })
        }
    }

    override suspend fun openLesson(profileId: String, practiceAgain: Boolean, lessonId: String): LessonSession {
        seed()
        return database.withTransaction {
            requireNotNull(dao.profile(profileId))
            val catalog = database.catalogDao()
            val link = catalog.lesson(lessonId)
            if (catalog.metadata() != null) {
                require(link != null && catalog.topic(link.topicId)?.contentStatus == ContentAvailability.AVAILABLE.name) { "Lesson is not available" }
            } else require(lessonId == StarterContent.lesson.id) // Legacy starter callers before catalog installation.
            val lesson = requireNotNull(dao.lesson(lessonId, link?.contentVersion ?: StarterContent.lesson.version))
            val previous = dao.latestAttempt(profileId, lesson.id, lesson.version)
            // An unfinished attempt is always resumed, even after a repeated practice request.
            val attempt = if (previous != null && (!practiceAgain || previous.completedAt == null)) previous else {
                AttemptEntity(UUID.randomUUID().toString(), profileId, lesson.id, lesson.version, 0, System.currentTimeMillis(), null)
                    .also { dao.insertAttempt(it) }
            }
            snapshot(attempt)
        }
    }

    override suspend fun session(attemptId: String, profileId: String): LessonSession = database.withTransaction {
        snapshot(ownedAttempt(attemptId, profileId))
    }

    override suspend fun answer(
        attemptId: String, profileId: String, questionId: String, choice: Int, eventId: String,
    ): Evaluation = submitAnswer(attemptId, profileId, questionId, LearningAnswer.Choice(choice.toString()), eventId)

    override suspend fun submitAnswer(
        attemptId: String, profileId: String, questionId: String, answer: LearningAnswer, eventId: String,
    ): Evaluation = database.withTransaction {
        val attempt = ownedAttempt(attemptId, profileId)
        val encoded = ActivityCodec.answer(answer)
        val previous = dao.answer(eventId)
        if (previous != null) {
            val oldPayload = previous.answerPayloadJson.ifEmpty { ActivityCodec.answer(LearningAnswer.Choice(previous.choice.toString())) }
            require(previous.attemptId == attemptId && previous.questionId == questionId && oldPayload == encoded)
            return@withTransaction if (previous.correct) Evaluation.Correct else Evaluation.Incorrect
        }
        require(attempt.completedAt == null)
        val questions = dao.questions(attempt.lessonId, attempt.version)
        val question = questions[attempt.currentIndex]
        require(question.id == questionId)
        val state = state(attempt, question)
        check(!state.solved)
        val evaluation = ExerciseEvaluator.evaluate(loadQuestion(question), answer)
        if (evaluation == Evaluation.Invalid) return@withTransaction evaluation
        val now = System.currentTimeMillis()
        val legacyChoice = (answer as? LearningAnswer.Choice)?.id?.toIntOrNull() ?: 0
        dao.insertAnswer(AnswerEntity(eventId, attemptId, questionId, attempt.version, legacyChoice, evaluation == Evaluation.Correct, state.hintsUsed, now, encoded))
        if (evaluation == Evaluation.Correct) {
            dao.saveState(state.copy(solved = true))
            if (dao.states(attemptId).count { it.solved } == questions.size) {
                dao.complete(attemptId, now)
                StickerCatalog.forLesson(attempt.lessonId)?.let { sticker ->
                    database.rewardsDao().award(EarnedStickerEntity(profileId, attempt.lessonId, sticker.id, now))
                }
            }
        }
        evaluation
    }

    override suspend fun requestHint(attemptId: String, profileId: String, questionId: String): LessonSession = database.withTransaction {
        val attempt = ownedAttempt(attemptId, profileId)
        require(attempt.completedAt == null)
        val question = dao.questions(attempt.lessonId, attempt.version)[attempt.currentIndex]
        require(question.id == questionId)
        val current = state(attempt, question)
        check(!current.solved)
        val total = dao.hints(questionId, attempt.version).size
        dao.saveState(current.copy(hintsUsed = ProgressiveHints.nextCount(total, current.hintsUsed)))
        snapshot(attempt)
    }

    override suspend fun advance(attemptId: String, profileId: String): LessonSession = database.withTransaction {
        val attempt = ownedAttempt(attemptId, profileId)
        val questions = dao.questions(attempt.lessonId, attempt.version)
        check(state(attempt, questions[attempt.currentIndex]).solved)
        val index = (attempt.currentIndex + 1).coerceAtMost(questions.lastIndex)
        dao.setIndex(attemptId, index)
        snapshot(attempt.copy(currentIndex = index))
    }

    private suspend fun ownedAttempt(id: String, profileId: String): AttemptEntity = requireNotNull(dao.attempt(id)).also {
        require(it.profileId == profileId)
    }

    private suspend fun state(attempt: AttemptEntity, question: QuestionEntity): QuestionStateEntity =
        dao.states(attempt.id).firstOrNull { it.questionId == question.id }
            ?: QuestionStateEntity(attempt.id, question.id, attempt.version, false, 0)

    private suspend fun loadQuestion(row: QuestionEntity) = ActivityCodec.decode(row, dao.options(row.id, row.version).map { it.value })

    private suspend fun snapshot(attempt: AttemptEntity): LessonSession {
        val lesson = requireNotNull(dao.lesson(attempt.lessonId, attempt.version))
        val questions = dao.questions(attempt.lessonId, attempt.version)
        val question = loadQuestion(questions[attempt.currentIndex]).content
        val hints = dao.hints(question.id, attempt.version).map { it.text }
        val states = dao.states(attempt.id)
        val state = states.firstOrNull { it.questionId == question.id }
        val used = state?.hintsUsed ?: 0
        val progress = dao.progress(attempt.profileId, attempt.lessonId, attempt.version)
        return LessonSession(
            attempt.id, attempt.profileId, attempt.lessonId, attempt.version, lesson.title, lesson.objective,
            attempt.currentIndex, questions.size, states.count { it.solved },
            question,
            state?.solved == true, ProgressiveHints.visible(hints, used), used >= hints.size,
            attempt.completedAt != null,
            progress.solvedQuestions, progress.completed,
            database.rewardsDao().earned(attempt.profileId).firstOrNull { it.lessonId == attempt.lessonId }
                ?.let { EarnedSticker(it.profileId, it.stickerId, it.obtainedAt) },
            dao.answers(attempt.id).count { it.questionId == question.id && !it.correct },
        )
    }
}
