package com.example.triada

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.data.packageio.*
import com.example.triada.domain.model.LearningAnswer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class StudentResultsExporterTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun exportContainsOnlyOneProfileAndPreservesTypedAnswersAndVersions() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        try {
            val repo = RoomLearningRepository(db)
            val a = repo.createProfile("PRIVATE LUNA", "star")
            val b = repo.createProfile("PRIVATE SOL", "sun")
            RoomCatalogRepository(db, AssetCatalogSource(context)).load(a.id)
            val foreign = repo.openLesson(b.id)
            repo.answer(foreign.attemptId, b.id, foreign.question.id, 4, "foreign-answer")
            var session = repo.openLesson(a.id, lessonId = "g1-com-vowels")
            repo.requestHint(session.attemptId, a.id, session.question.id)
            repo.submitAnswer(session.attemptId, a.id, session.question.id, LearningAnswer.Choice("o1"), "wrong")
            repo.submitAnswer(session.attemptId, a.id, session.question.id, LearningAnswer.Choice("o2"), "right")
            session = repo.advance(session.attemptId, a.id)
            repo.submitAnswer(session.attemptId, a.id, session.question.id, LearningAnswer.Choice("o1"), "second")
            session = repo.advance(session.attemptId, a.id)
            repo.submitAnswer(session.attemptId, a.id, session.question.id, LearningAnswer.Letters(listOf("t2", "t3", "t1")), "word")
            val bytes = ByteArrayOutputStream()
            StudentResultsExporter(db).export(a.id, bytes)
            val files = SafePackageReader().read(bytes.toByteArray().inputStream())
            fun json(name: String) = JSONObject(files.getValue(name).toString(Charsets.UTF_8))
            assertEquals("aula-result", json("manifest.json").getString("format"))
            assertTrue(json("student.json").getString("studentId").startsWith("learner-"))
            val answers = json("answers.json").getJSONArray("answers")
            assertEquals(4, answers.length())
            assertFalse(answers.getJSONObject(0).getBoolean("isCorrect"))
            assertEquals(1, answers.getJSONObject(0).getInt("hintsUsed"))
            assertEquals(1, answers.getJSONObject(3).getInt("contentVersion"))
            assertEquals("letters", answers.getJSONObject(3).getJSONObject("answer").getString("type"))
            assertEquals("COMPLETED", json("progress.json").getJSONArray("lessons").getJSONObject(0).getString("status"))
            assertEquals(1, json("rewards.json").getJSONArray("stickers").length())
            val all = files.values.joinToString { it.toString(Charsets.UTF_8) }
            listOf(a.displayName, b.displayName, a.id, b.id, "foreign-answer", "answerKeyJson").forEach { assertFalse("Unexpected disclosure: $it", all.contains(it)) }
            val checksums = json("checksums.json")
            files.filterKeys { it != "checksums.json" }.forEach { (name, data) ->
                val expected = java.security.MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
                assertEquals(expected, checksums.getString(name))
            }
            assertEquals(4, db.learningDao().answers(session.attemptId).size)
        } finally { db.close() }
    }

    @Test fun emptyProfileExportsEmptyCollectionsAndUnknownProfileWritesNothing() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        try {
            val p = RoomLearningRepository(db).createProfile("Empty", "sun")
            val bytes = ByteArrayOutputStream()
            StudentResultsExporter(db).export(p.id, bytes)
            val files = SafePackageReader().read(bytes.toByteArray().inputStream())
            assertEquals(0, JSONObject(files.getValue("attempts.json").toString(Charsets.UTF_8)).getJSONArray("attempts").length())
            val rejected = ByteArrayOutputStream()
            try { StudentResultsExporter(db).export("missing", rejected); fail("Unknown profile") } catch (_: IllegalArgumentException) { }
            assertEquals(0, rejected.size())
        } finally { db.close() }
    }
}
