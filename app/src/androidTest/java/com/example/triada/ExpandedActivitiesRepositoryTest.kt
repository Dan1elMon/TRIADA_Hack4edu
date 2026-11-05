package com.example.triada

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ExpandedActivitiesRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
    private val repository = RoomLearningRepository(database)
    private val catalog = RoomCatalogRepository(database, AssetCatalogSource(context))
    @After fun close() { database.close() }

    @Test fun sixLessonsCanBeCompletedAndResultsStayIndependent() = runBlocking {
        val a = repository.createProfile("Aprendiz", "sun")
        val b = repository.createProfile("Otra aventura", "star")
        catalog.load(a.id)
        fun choice(id: String) = LearningAnswer.Choice(id)
        fun pairs(vararg names: String) = LearningAnswer.Pairs(names.map { PairSelection("word-$it", "image-$it") })
        val answers = linkedMapOf(
            "g1-mat-count-1-9" to listOf(choice("4"), choice("6"), choice("9")),
            "g1-com-vowels" to listOf(choice("o2"), choice("o1"), LearningAnswer.Letters(listOf("t2", "t3", "t1"))),
            "g1-rma-patterns" to listOf(choice("circle"), choice("circle"), choice("triangle")),
            "g1-cya-body" to listOf(pairs("hand", "foot"), pairs("eye", "ear"), pairs("mouth", "nose")),
            "g1-pso-coexistence" to listOf(choice("o2"), choice("o1"), choice("o3")),
            "g1-rve-initial-sounds" to listOf(choice("o2"), choice("o3"), choice("o1")),
        )
        answers.forEach { (lessonId, expected) ->
            var session = repository.openLesson(a.id, lessonId = lessonId)
            expected.forEachIndexed { index, answer ->
                assertEquals(Evaluation.Correct, repository.submitAnswer(session.attemptId, a.id, session.question.id, answer, "$lessonId-$index"))
                session = repository.session(session.attemptId, a.id)
                if (!session.completed) session = repository.advance(session.attemptId, a.id)
            }
            assertTrue(session.completed)
            assertEquals(3, database.learningDao().answers(session.attemptId).size)
            assertTrue(database.learningDao().answers(session.attemptId).all { it.answerPayloadJson.isNotBlank() })
        }
        assertTrue(catalog.load(a.id).lessons.all { it.status == LearningStatus.COMPLETED })
        assertTrue(catalog.load(b.id).lessons.all { it.status == LearningStatus.NOT_STARTED && it.solvedQuestions == 0 })
    }

    @Test fun invalidPairsNeverAdvanceAndRepeatedTypedEventsAreIdempotent() = runBlocking {
        val profile = repository.createProfile("Pares", "flower")
        catalog.load(profile.id)
        val session = repository.openLesson(profile.id, lessonId = "g1-cya-body")
        val incomplete = LearningAnswer.Pairs(listOf(PairSelection("word-hand", "image-hand")))
        assertEquals(Evaluation.Invalid, repository.submitAnswer(session.attemptId, profile.id, session.question.id, incomplete, "incomplete"))
        assertTrue(database.learningDao().answers(session.attemptId).isEmpty())
        repository.requestHint(session.attemptId, profile.id, session.question.id)
        val wrong = LearningAnswer.Pairs(listOf(PairSelection("word-hand", "image-foot"), PairSelection("word-foot", "image-hand")))
        assertEquals(Evaluation.Incorrect, repository.submitAnswer(session.attemptId, profile.id, session.question.id, wrong, "wrong"))
        assertEquals(Evaluation.Incorrect, repository.submitAnswer(session.attemptId, profile.id, session.question.id, wrong.copy(values = wrong.values.reversed()), "wrong"))
        assertEquals(1, database.learningDao().answers(session.attemptId).size)
        assertEquals(1, database.learningDao().answers(session.attemptId).single().hintsUsed)
        assertEquals(0, repository.session(session.attemptId, profile.id).solvedQuestions)
    }

    @Test fun typedAnswersAndHintsSurviveDiskReopening() = runBlocking {
        val filename = "typed-answers-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, LearningDatabase::class.java, filename).build()
        var disk = open()
        try {
            var repo = RoomLearningRepository(disk)
            val profile = repo.createProfile("Persistente", "star")
            RoomCatalogRepository(disk, AssetCatalogSource(context)).load(profile.id)
            var session = repo.openLesson(profile.id, lessonId = "g1-cya-body")
            val answer = LearningAnswer.Pairs(listOf(PairSelection("word-hand", "image-hand"), PairSelection("word-foot", "image-foot")))
            repo.submitAnswer(session.attemptId, profile.id, session.question.id, answer, "persisted-pairs")
            session = repo.advance(session.attemptId, profile.id)
            repo.requestHint(session.attemptId, profile.id, session.question.id)
            disk.close(); disk = open(); repo = RoomLearningRepository(disk)
            val restored = repo.openLesson(profile.id, lessonId = "g1-cya-body")
            assertEquals(1, restored.currentIndex)
            assertEquals(1, restored.solvedQuestions)
            assertEquals(1, restored.visibleHints.size)
            assertEquals(ActivityType.MATCH_PAIRS, restored.question.type)
            assertEquals(ActivityCodec.answer(answer), disk.learningDao().answers(session.attemptId).single().answerPayloadJson)
        } finally { disk.close(); context.deleteDatabase(filename) }
    }

    @Test fun unknownTypesDoNotCrashOrRecordAFalseSuccess() = runBlocking {
        val profile = repository.createProfile("Futuro", "sun")
        catalog.load(profile.id)
        val session = repository.openLesson(profile.id, lessonId = "g1-com-vowels")
        database.openHelper.writableDatabase.execSQL("UPDATE questions SET activityType = 'FUTURE_TYPE' WHERE id = ?", arrayOf(session.question.id))
        assertEquals(ActivityType.UNKNOWN, repository.session(session.attemptId, profile.id).question.type)
        assertEquals(Evaluation.Invalid, repository.submitAnswer(session.attemptId, profile.id, session.question.id, LearningAnswer.Choice("o2"), "unknown"))
        assertTrue(database.learningDao().answers(session.attemptId).isEmpty())
    }

    @Test fun missingAudioPreventsActivatingIncompleteLessons() = runBlocking {
        val assets = AssetCatalogSource(context)
        val source = object : CatalogSource {
            override fun read() = assets.read()
            override fun hasAudio(key: String) = key != "mesa" && assets.hasAudio(key)
        }
        try { RoomCatalogRepository(database, source).install(); fail("Missing audio must abort activation") } catch (_: IllegalArgumentException) { }
        assertNull(database.catalogDao().metadata())
        assertTrue(database.catalogDao().lessons().isEmpty())
    }
}
