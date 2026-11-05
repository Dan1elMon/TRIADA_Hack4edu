package com.example.triada

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.LearningDatabase
import com.example.triada.data.repository.RoomLearningRepository
import com.example.triada.domain.model.Evaluation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RoomLearningRepositoryTest {
    private lateinit var database: LearningDatabase
    private lateinit var repository: RoomLearningRepository
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
        repository = RoomLearningRepository(database)
    }
    @After fun tearDown() { database.close() }

    @Test fun errorsAndHintUsageAreRetained() = runBlocking {
        val profile = repository.createProfile("Luna", "star")
        val session = repository.openLesson(profile.id)
        repository.requestHint(session.attemptId, profile.id, session.question.id)
        assertEquals(Evaluation.Incorrect, repository.answer(session.attemptId, profile.id, session.question.id, 3, "wrong"))
        assertEquals(0, repository.session(session.attemptId, profile.id).solvedQuestions)
        assertEquals(Evaluation.Correct, repository.answer(session.attemptId, profile.id, session.question.id, 4, "correct"))
        val answers = database.learningDao().answers(session.attemptId)
        assertEquals(listOf(false, true), answers.map { it.correct })
        assertEquals(listOf(1, 1), answers.map { it.hintsUsed })
        assertEquals(1, repository.session(session.attemptId, profile.id).solvedQuestions)
    }

    @Test fun profileResultsStayIndependentAndOwnershipIsEnforced() = runBlocking {
        val a = repository.createProfile("A", "sun")
        val b = repository.createProfile("B", "flower")
        val first = repository.openLesson(a.id)
        repository.answer(first.attemptId, a.id, first.question.id, 4, "a-answer")
        assertEquals(0, repository.openLesson(b.id).solvedQuestions)
        try {
            repository.session(first.attemptId, b.id)
            fail("Another profile must not read an attempt")
        } catch (_: IllegalArgumentException) { }
        try {
            repository.answer(first.attemptId, b.id, first.question.id, 4, "cross-profile")
            fail("Another profile must not write an attempt")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1, database.learningDao().answers(first.attemptId).size)
    }

    @Test fun repeatedEventAndSeedDoNotDuplicateOrResetProgress() = runBlocking {
        val profile = repository.createProfile("Sol", "sun")
        val session = repository.openLesson(profile.id)
        repeat(2) { repository.answer(session.attemptId, profile.id, session.question.id, 4, "same-event") }
        repeat(2) { repository.seed() }
        val resumed = repository.openLesson(profile.id)
        assertEquals(session.attemptId, resumed.attemptId)
        assertEquals(1, resumed.solvedQuestions)
        assertEquals(1, database.learningDao().answers(session.attemptId).size)
        assertEquals(3, database.learningDao().questions(session.lessonId, session.contentVersion).size)
    }

    @Test fun hintsAreOrderedAndExhaustionIsPersisted() = runBlocking {
        val profile = repository.createProfile("Flor", "flower")
        val session = repository.openLesson(profile.id)
        val one = repository.requestHint(session.attemptId, profile.id, session.question.id)
        val two = repository.requestHint(session.attemptId, profile.id, session.question.id)
        val three = repository.requestHint(session.attemptId, profile.id, session.question.id)
        assertEquals(1, one.visibleHints.size)
        assertEquals(one.visibleHints.first(), two.visibleHints.first())
        assertEquals(2, two.visibleHints.size)
        assertEquals(two.visibleHints, three.visibleHints)
        assertTrue(three.hintsExhausted)
        assertEquals(0, three.solvedQuestions)
    }

    @Test fun invalidChoicesAndEarlyAdvanceDoNotChangeProgress() = runBlocking {
        val profile = repository.createProfile("Nube", "star")
        val session = repository.openLesson(profile.id)
        assertEquals(Evaluation.Invalid, repository.answer(session.attemptId, profile.id, session.question.id, 99, "invalid"))
        try {
            repository.advance(session.attemptId, profile.id)
            fail("Unsolved questions must not be skipped")
        } catch (_: IllegalStateException) { }
        assertEquals(0, repository.session(session.attemptId, profile.id).currentIndex)
        assertTrue(database.learningDao().answers(session.attemptId).isEmpty())
    }

    @Test fun completionAndPracticeKeepThePreviousAttempt() = runBlocking {
        val profile = repository.createProfile("Luz", "sun")
        var session = repository.openLesson(profile.id)
        listOf(4, 6, 9).forEachIndexed { index, value ->
            repository.answer(session.attemptId, profile.id, session.question.id, value, "answer-$index")
            session = repository.session(session.attemptId, profile.id)
            if (!session.completed) session = repository.advance(session.attemptId, profile.id)
        }
        assertTrue(session.completed)
        assertEquals(3, session.solvedQuestions)
        assertTrue(repository.openLesson(profile.id).completed)
        val practice = repository.openLesson(profile.id, practiceAgain = true)
        assertNotEquals(session.attemptId, practice.attemptId)
        assertEquals(0, practice.solvedQuestions)
        assertEquals(3, practice.bestSolvedQuestions)
        assertTrue(practice.everCompleted)
        assertEquals(practice.attemptId, repository.openLesson(profile.id, practiceAgain = true).attemptId)
        assertTrue(repository.session(session.attemptId, profile.id).completed)
    }

    @Test fun closingDatabaseAndOpeningAgainRestoresConfirmedPositionAndHints() = runBlocking {
        val filename = "aulanexo-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, LearningDatabase::class.java, filename).build()
        var disk = open()
        try {
            var repo = RoomLearningRepository(disk)
            val profile = repo.createProfile("Persistente", "star")
            var session = repo.openLesson(profile.id)
            repo.answer(session.attemptId, profile.id, session.question.id, 4, "persisted-answer")
            session = repo.advance(session.attemptId, profile.id)
            repo.requestHint(session.attemptId, profile.id, session.question.id)
            disk.close()
            disk = open()
            repo = RoomLearningRepository(disk)
            val resumed = repo.openLesson(profile.id)
            assertEquals(session.attemptId, resumed.attemptId)
            assertEquals(1, resumed.currentIndex)
            assertEquals(1, resumed.solvedQuestions)
            assertEquals(1, resumed.visibleHints.size)
            assertEquals(1, disk.learningDao().answers(session.attemptId).size)
        } finally {
            disk.close()
            context.deleteDatabase(filename)
        }
    }
}
