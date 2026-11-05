package com.example.triada

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class RewardsRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
    private val repo = RoomLearningRepository(db)
    @After fun close() { db.close() }

    private suspend fun ready(profile: String): LessonSession {
        var session = repo.openLesson(profile)
        listOf(4, 6).forEachIndexed { index, choice ->
            repo.answer(session.attemptId, profile, session.question.id, choice, "${session.attemptId}-$index")
            session = repo.advance(session.attemptId, profile)
        }
        return session
    }

    @Test fun concurrentReplayAndPracticeNeverDuplicateTheReward() = runBlocking {
        val a = repo.createProfile("Luna", "star")
        val b = repo.createProfile("Sol", "sun")
        val catalog = RoomCatalogRepository(db, AssetCatalogSource(context))
        catalog.load(a.id)
        var session = ready(a.id)
        assertTrue(repo.rewards(a.id).isEmpty())
        repo.answer(session.attemptId, a.id, session.question.id, 8, "wrong")
        assertTrue(repo.rewards(a.id).isEmpty())
        val completed = session
        coroutineScope { List(2) { async { repo.answer(completed.attemptId, a.id, completed.question.id, 9, "last") } }.awaitAll() }
        val earned = repo.rewards(a.id).single()
        assertEquals("counting-sun", earned.stickerId)
        assertEquals(earned, repo.session(session.attemptId, a.id).earnedSticker)
        repo.openLesson(a.id, practiceAgain = true)
        session = ready(a.id)
        repo.answer(session.attemptId, a.id, session.question.id, 9, "repeat-last")
        assertEquals(listOf(earned), repo.rewards(a.id))
        assertTrue(repo.rewards(b.id).isEmpty())
        val overview = ProgressOverview.from(catalog.load(a.id))
        assertEquals(1, overview.completedLessons)
        assertEquals(3, overview.solvedExercises)
        assertEquals(18, overview.totalExercises)
    }

    @Test fun rewardFailureRollsBackTheAnswerAndCompletion() = runBlocking {
        val p = repo.createProfile("Transacción", "sun")
        val session = ready(p.id)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_reward BEFORE INSERT ON earned_stickers BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { repo.answer(session.attemptId, p.id, session.question.id, 9, "retry"); fail("Expected failure") } catch (_: android.database.sqlite.SQLiteException) { }
        assertNull(db.learningDao().answer("retry"))
        assertFalse(repo.session(session.attemptId, p.id).completed)
        assertEquals(2, repo.session(session.attemptId, p.id).solvedQuestions)
        assertTrue(repo.rewards(p.id).isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_reward")
        repo.answer(session.attemptId, p.id, session.question.id, 9, "retry")
        assertTrue(repo.session(session.attemptId, p.id).completed)
        assertEquals(1, repo.rewards(p.id).size)
    }
}
