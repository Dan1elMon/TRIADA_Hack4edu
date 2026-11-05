package com.example.triada.data.repository

import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.*
import com.example.triada.domain.repository.LearningRepository
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomLearningRepository(private val database: LearningDatabase) : LearningRepository {
    private val dao = database.learningDao()

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
    }

    override suspend fun openLesson(profileId: String, practiceAgain: Boolean): LessonSession {
        seed()
        return database.withTransaction {
            requireNotNull(dao.profile(profileId))
            val lesson = StarterContent.lesson
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
    ): Evaluation = database.withTransaction {
        val attempt = ownedAttempt(attemptId, profileId)
        val previous = dao.answer(eventId)
        if (previous != null) {
            require(previous.attemptId == attemptId && previous.questionId == questionId && previous.choice == choice)
            return@withTransaction if (previous.correct) Evaluation.Correct else Evaluation.Incorrect
        }
        require(attempt.completedAt == null)
        val questions = dao.questions(attempt.lessonId, attempt.version)
        val question = questions[attempt.currentIndex]
        require(question.id == questionId)
        val state = state(attempt, question)
        check(!state.solved)
        val evaluation = CountEvaluator.evaluate(loadQuestion(question), choice)
        if (evaluation == Evaluation.Invalid) return@withTransaction evaluation
        val now = System.currentTimeMillis()
        dao.insertAnswer(AnswerEntity(eventId, attemptId, questionId, attempt.version, choice, evaluation == Evaluation.Correct, state.hintsUsed, now))
        if (evaluation == Evaluation.Correct) {
            dao.saveState(state.copy(solved = true))
            if (dao.states(attemptId).count { it.solved } == questions.size) dao.complete(attemptId, now)
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

    private suspend fun loadQuestion(row: QuestionEntity) = CountQuestion(
        row.id, row.prompt, row.objectCount,
        dao.options(row.id, row.version).map { it.value },
        dao.hints(row.id, row.version).map { it.text },
    )

    private suspend fun snapshot(attempt: AttemptEntity): LessonSession {
        val lesson = requireNotNull(dao.lesson(attempt.lessonId, attempt.version))
        val questions = dao.questions(attempt.lessonId, attempt.version)
        val question = loadQuestion(questions[attempt.currentIndex])
        val states = dao.states(attempt.id)
        val state = states.firstOrNull { it.questionId == question.id }
        val used = state?.hintsUsed ?: 0
        val progress = dao.progress(attempt.profileId, attempt.lessonId, attempt.version)
        return LessonSession(
            attempt.id, attempt.profileId, attempt.lessonId, attempt.version, lesson.title, lesson.objective,
            attempt.currentIndex, questions.size, states.count { it.solved },
            QuestionPresentation(question.id, question.prompt, question.objectCount, question.options),
            state?.solved == true, ProgressiveHints.visible(question.hints, used), used >= question.hints.size,
            attempt.completedAt != null,
            progress.solvedQuestions, progress.completed,
        )
    }
}
