package com.example.triada.data.packageio

import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.LearningAnswer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Consistent, read-only snapshot of one learner. No names, avatars, prompts or answer keys. */
class StudentResultsExporter(private val database: LearningDatabase) {
    suspend fun export(profileId: String, output: OutputStream) = withContext(Dispatchers.IO) {
        val files = database.withTransaction {
            val dao = database.learningDao()
            requireNotNull(dao.profile(profileId)) { "Unknown profile" }
            require(dao.profileAnswerCount(profileId) <= 50_000) { "Export answer limit exceeded" }
            val attempts = dao.profileAttempts(profileId)
            require(attempts.size <= 10_000) { "Export attempt limit exceeded" }
            val studentId = if (profileId.matches(Regex("recovered-learner-[a-f0-9]{24}"))) profileId.removePrefix("recovered-")
                else "learner-" + hash(("aulanexo-results-v1:" + profileId).toByteArray(Charsets.UTF_8)).take(24)
            fun sourceId(type: String, id: String) = if (profileId.startsWith("recovered-")) id.removePrefix("r:$studentId:$type:") else id
            val attemptRows = JSONArray()
            val answerRows = JSONArray()
            attempts.forEach { attempt ->
                val states = JSONArray()
                dao.states(attempt.id).forEach { state -> states.put(JSONObject().put("questionId", state.questionId)
                    .put("contentVersion", state.version).put("solved", state.solved).put("hintsUsed", state.hintsUsed)) }
                attemptRows.put(JSONObject().put("attemptId", sourceId("attempt",attempt.id)).put("lessonId", attempt.lessonId)
                    .put("contentVersion", attempt.version).put("currentIndex", attempt.currentIndex)
                    .put("startedAt", attempt.startedAt).put("completedAt", attempt.completedAt ?: JSONObject.NULL).put("questions", states))
                dao.answers(attempt.id).forEach { answer ->
                    val payload = answer.answerPayloadJson.ifEmpty { ActivityCodec.answer(LearningAnswer.Choice(answer.choice.toString())) }
                    answerRows.put(JSONObject().put("answerId", sourceId("answer",answer.eventId)).put("attemptId", sourceId("attempt",attempt.id))
                        .put("questionId", answer.questionId).put("contentVersion", answer.version)
                        .put("answer", JSONObject(payload)).put("isCorrect", answer.correct)
                        .put("hintsUsed", answer.hintsUsed).put("answeredAt", answer.answeredAt))
                }
            }
            val progress = JSONArray()
            attempts.map { it.lessonId to it.version }.distinct().forEach { (id, version) ->
                val summary = dao.progress(profileId, id, version)
                progress.put(JSONObject().put("lessonId", id).put("contentVersion", version)
                    .put("solvedQuestions", summary.solvedQuestions).put("totalQuestions", dao.questions(id, version).size)
                    .put("status", if (summary.completed) "COMPLETED" else "IN_PROGRESS"))
            }
            val stickers = JSONArray()
            database.rewardsDao().earned(profileId).forEach { stickers.put(JSONObject().put("stickerId", it.stickerId)
                .put("lessonId", it.lessonId).put("obtainedAt", it.obtainedAt)) }
            linkedMapOf(
                "manifest.json" to JSONObject().put("format", "aula-result").put("formatVersion", 1)
                    .put("kind", "learning-results").put("createdAtEpochMillis", System.currentTimeMillis()),
                "student.json" to JSONObject().put("studentId", studentId),
                "attempts.json" to JSONObject().put("attempts", attemptRows),
                "answers.json" to JSONObject().put("answers", answerRows),
                "progress.json" to JSONObject().put("lessons", progress),
                "rewards.json" to JSONObject().put("stickers", stickers),
            ).mapValues { it.value.toString().toByteArray(Charsets.UTF_8) }
        }
        val hashes = JSONObject()
        require(files.values.all { it.size <= 8 * 1024 * 1024 } && files.values.sumOf { it.size.toLong() } <= 32L * 1024 * 1024) { "Results exceed portable package limits" }
        files.forEach { (name, data) -> hashes.put(name, hash(data)) }
        ZipOutputStream(output).use { zip ->
            (files + ("checksums.json" to hashes.toString().toByteArray(Charsets.UTF_8))).forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
        }
    }
    private fun hash(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}
