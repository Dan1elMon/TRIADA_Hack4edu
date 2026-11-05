package com.example.triada

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.LearningDatabase
import com.example.triada.data.repository.RoomLearningRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ProcessCheckpointTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="acceptance-checkpoint.db"
    @Test fun a_prepareConfirmedCheckpoint() = runBlocking {
        context.deleteDatabase(name)
        val db=Room.databaseBuilder(context,LearningDatabase::class.java,name).build()
        try {
            val repo=RoomLearningRepository(db);val p=repo.createProfile("Checkpoint","sun")
            var session=repo.openLesson(p.id)
            repo.answer(session.attemptId,p.id,session.question.id,4,"checkpoint-answer")
            session=repo.advance(session.attemptId,p.id)
            repo.requestHint(session.attemptId,p.id,session.question.id)
            File(context.cacheDir,"acceptance-checkpoint.pid").writeText(android.os.Process.myPid().toString())
        } finally {db.close()}
    }
    @Test fun b_restoreConfirmedCheckpoint() = runBlocking {
        val db=Room.databaseBuilder(context,LearningDatabase::class.java,name).build()
        try {
            if(InstrumentationRegistry.getArguments().getString("requireFreshProcess")=="true")
                assertNotEquals(File(context.cacheDir,"acceptance-checkpoint.pid").readText(),android.os.Process.myPid().toString())
            val repo=RoomLearningRepository(db);val p=repo.profiles().first().single()
            val session=repo.openLesson(p.id)
            assertEquals(1,session.currentIndex);assertEquals(1,session.solvedQuestions);assertEquals(1,session.visibleHints.size)
        } finally {db.close();context.deleteDatabase(name);File(context.cacheDir,"acceptance-checkpoint.pid").delete()}
    }
}
