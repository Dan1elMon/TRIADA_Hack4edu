package com.example.triada.data.packageio

import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import org.json.JSONObject

/** Restores into a deterministic, separate local profile. Claims of success are re-evaluated. */
class StudentResultsImporter(private val db: LearningDatabase) {
    suspend fun import(input: InputStream): String = withContext(Dispatchers.IO) {
        val files = SafePackageReader().read(input)
        require(files.keys == setOf("manifest.json", "student.json", "attempts.json", "answers.json", "progress.json", "rewards.json", "checksums.json"))
        PackageJson.verify(files, "aula-result", "learning-results")
        val student = PackageJson.parse(files.getValue("student.json")).getString("studentId")
        require(student.matches(Regex("learner-[a-f0-9]{24}")))
        fun id(type: String, source: String): String {
            require(source.length in 1..160)
            return "r:$student:$type:$source"
        }
        val profileId = "recovered-$student"
        val attempts = PackageJson.parse(files.getValue("attempts.json"),8*1024*1024).getJSONArray("attempts")
        val answers = PackageJson.parse(files.getValue("answers.json"),8*1024*1024).getJSONArray("answers")
        val progress = PackageJson.parse(files.getValue("progress.json"),8*1024*1024).getJSONArray("lessons")
        PackageJson.parse(files.getValue("rewards.json")) // Rewards are re-derived from completed lessons, never trusted as authority.
        require(attempts.length() <= 10_000 && answers.length() <= 50_000)
        db.withTransaction {
            val dao = db.learningDao()
            if (dao.profile(profileId) == null) dao.insertProfile(ProfileEntity(profileId, "Recuperado ${student.takeLast(6)}", "star", System.currentTimeMillis()))
            val attemptIds = mutableSetOf<String>(); val answerIds = mutableSetOf<String>()
            val answerRows = (0 until answers.length()).map { answers.getJSONObject(it) }
            val answersByAttempt = answerRows.groupBy { it.getString("attemptId") }
            val sourceProgress = mutableMapOf<Pair<String,Int>, MutableSet<String>>()
            val sourceCompleted = mutableSetOf<Pair<String,Int>>()
            val completedGoals = mutableMapOf<String,Long>()
            repeat(attempts.length()) { n ->
                val row = attempts.getJSONObject(n); val sourceId = row.getString("attemptId")
                require(attemptIds.add(sourceId))
                val attemptId = if (dao.attempt(sourceId)?.profileId == profileId) sourceId else id("attempt", sourceId)
                val lessonId = row.getString("lessonId"); val version = row.getInt("contentVersion")
                requireNotNull(dao.lesson(lessonId,version)) { "Import lesson content before results" }
                val questions = dao.questions(lessonId,version); require(questions.isNotEmpty())
                val byId = questions.associateBy { it.id }
                val index = row.getInt("currentIndex"); require(index in questions.indices)
                val started = row.getLong("startedAt"); val finished = if (row.isNull("completedAt")) null else row.getLong("completedAt")
                require(started >= 0 && (finished == null || finished >= started))
                val relevant = answersByAttempt[sourceId].orEmpty()
                val correct = mutableSetOf<String>()
                val importedAnswers = relevant.map { answer ->
                    val sourceAnswer = answer.getString("answerId"); require(answerIds.add(sourceAnswer))
                    val questionId = answer.getString("questionId"); val q = requireNotNull(byId[questionId])
                    require(answer.getInt("contentVersion") == version)
                    val response = ActivityCodec.decodeAnswer(answer.getJSONObject("answer"))
                    val evaluation = ExerciseEvaluator.evaluate(ActivityCodec.decode(q,dao.options(q.id,version).map { it.value }), response)
                    require(evaluation != Evaluation.Invalid && answer.getBoolean("isCorrect") == (evaluation == Evaluation.Correct))
                    val hints = answer.getInt("hintsUsed"); require(hints in 0..dao.hints(questionId,version).size)
                    val time = answer.getLong("answeredAt"); require(time >= started && (finished == null || time <= finished))
                    if (evaluation == Evaluation.Correct) correct.add(questionId)
                    val answerId = if (dao.answer(sourceAnswer)?.attemptId == attemptId) sourceAnswer else id("answer",sourceAnswer)
                    AnswerEntity(answerId,attemptId,questionId,version,(response as? LearningAnswer.Choice)?.id?.toIntOrNull() ?: 0,
                        evaluation == Evaluation.Correct,hints,time,ActivityCodec.answer(response))
                }
                val stateArray = row.getJSONArray("questions"); val stateIds = mutableSetOf<String>()
                val states = (0 until stateArray.length()).map { s ->
                    val state = stateArray.getJSONObject(s); val qid = state.getString("questionId")
                    require(stateIds.add(qid) && qid in byId && state.getInt("contentVersion") == version)
                    val hints = state.getInt("hintsUsed"); require(hints in 0..dao.hints(qid,version).size)
                    require(state.getBoolean("solved") == (qid in correct))
                    require(importedAnswers.filter { it.questionId == qid }.all { it.hintsUsed <= hints })
                    QuestionStateEntity(attemptId,qid,version,qid in correct,hints)
                }
                require(stateIds.containsAll(correct))
                require(questions.take(index).all { it.id in correct })
                require(correct.all { qid -> questions.indexOfFirst { it.id == qid } <= index })
                require((finished != null) == (correct.size == questions.size))
                if (finished != null) require(index == questions.lastIndex)
                val incoming = AttemptEntity(attemptId,profileId,lessonId,version,index,started,finished)
                val old = dao.attempt(attemptId)
                if (old == null) dao.insertAttempt(incoming) else {
                    require(old.profileId == profileId && old.lessonId == lessonId && old.version == version && old.startedAt == started)
                    require(index >= old.currentIndex && (old.completedAt == null || old.completedAt == finished)) { "Recovery cannot regress progress" }
                    val oldAnswers = dao.answers(attemptId)
                    require(importedAnswers.map { it.eventId }.containsAll(oldAnswers.map { it.eventId }))
                    dao.restorePosition(attemptId,index,finished)
                }
                importedAnswers.forEach { answer -> val existing = dao.answer(answer.eventId); if (existing == null) dao.insertAnswer(answer) else require(existing == answer) }
                val oldStates = dao.states(attemptId)
                require(states.map { it.questionId }.containsAll(oldStates.map { it.questionId }))
                states.forEach { state -> oldStates.firstOrNull { it.questionId == state.questionId }?.let { require(state.hintsUsed >= it.hintsUsed && (!it.solved || state.solved)) }; dao.saveState(state) }
                val key = lessonId to version
                val best = sourceProgress.getOrPut(key) { mutableSetOf() }
                // Sequential lessons: solved positions are prefixes, so the longest attempt is the best progress.
                if (correct.size > best.size) { best.clear(); best.addAll(correct) }
                if (finished != null) {
                    sourceCompleted.add(key)
                    completedGoals[lessonId] = minOf(completedGoals[lessonId] ?: Long.MAX_VALUE, finished)
                }
            }
            require(answerRows.all { it.getString("attemptId") in attemptIds })
            require(progress.length() == sourceProgress.size)
            val keys = mutableSetOf<Pair<String,Int>>()
            repeat(progress.length()) { n ->
                val row = progress.getJSONObject(n); val key = row.getString("lessonId") to row.getInt("contentVersion")
                require(keys.add(key) && key in sourceProgress)
                require(row.getInt("solvedQuestions") == sourceProgress.getValue(key).size && row.getInt("totalQuestions") == dao.questions(key.first,key.second).size)
                require(row.getString("status") == if (key in sourceCompleted) "COMPLETED" else "IN_PROGRESS")
            }
            completedGoals.forEach { (lesson, time) -> StickerCatalog.forLesson(lesson)?.let { db.rewardsDao().award(EarnedStickerEntity(profileId,lesson,it.id,time)) } }
            profileId
        }
    }
}
