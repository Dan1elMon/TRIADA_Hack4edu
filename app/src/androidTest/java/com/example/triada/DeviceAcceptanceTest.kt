package com.example.triada

import android.content.Context
import android.net.ConnectivityManager
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DeviceAcceptanceTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private fun shell(command:String):String = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }
    @Test fun sixLessonsWorkWithWifiAndMobileDataDisabledAndSettingsAreRestored() = runBlocking {
        val wifi=shell("settings get global wifi_on");val data=shell("settings get global mobile_data")
        require(wifi in setOf("0","1") && data in setOf("0","1")) { "Radio settings cannot be preserved safely" }
        val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
        try {
            shell("svc wifi disable");shell("svc data disable")
            val connectivity=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            // No network permission is requested by the app. Observe radio settings through the shell.
            withTimeout(15_000) { while(shell("settings get global wifi_on")=="1" || shell("settings get global mobile_data")=="1") delay(100) }
            val repo=RoomLearningRepository(db);val p=repo.createProfile("Sin conexión","sun")
            val catalog=RoomCatalogRepository(db,AssetCatalogSource(context)).load(p.id)
            assertEquals(6,catalog.lessons.size)
            for(lesson in catalog.lessons) {
                var session=repo.openLesson(p.id,lessonId=lesson.id)
                while(!session.completed) {
                    repo.requestHint(session.attemptId,p.id,session.question.id)
                    val row=db.learningDao().questions(lesson.id,lesson.contentVersion)[session.currentIndex]
                    val definition=ActivityCodec.decode(row,db.learningDao().options(row.id,row.version).map{it.value})
                    val q=definition.content
                    val answer:LearningAnswer=when(q.type) {
                        ActivityType.COUNT_OBJECTS->LearningAnswer.Choice(q.objectCount.toString())
                        ActivityType.SINGLE_CHOICE,ActivityType.SEQUENCE->LearningAnswer.Choice(requireNotNull(definition.correctChoiceId))
                        ActivityType.MATCH_PAIRS->LearningAnswer.Pairs(definition.correctPairs.map{PairSelection(it.key,it.value)})
                        ActivityType.WORD_BUILDING->{val remaining=q.tiles.toMutableList();LearningAnswer.Letters(requireNotNull(definition.correctWord).map{letter->remaining.first{it.text==letter.toString()}.also{remaining.remove(it)}.id})}
                        else->error("Unknown type")
                    }
                    assertEquals(Evaluation.Correct,repo.submitAnswer(session.attemptId,p.id,q.id,answer,java.util.UUID.randomUUID().toString()))
                    session=repo.session(session.attemptId,p.id)
                    if(!session.completed)session=repo.advance(session.attemptId,p.id)
                }
            }
            assertEquals(6,repo.rewards(p.id).size)
        } finally {
            db.close()
            shell(if(wifi=="1")"svc wifi enable" else "svc wifi disable")
            shell(if(data=="1")"svc data enable" else "svc data disable")
        }
    }
}
