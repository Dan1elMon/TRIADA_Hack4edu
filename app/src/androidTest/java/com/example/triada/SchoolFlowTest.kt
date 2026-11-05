package com.example.triada

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.core.navigation.AulaNexoNavHost
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.SchoolRole
import com.example.triada.domain.repository.ProfilePreferences
import com.example.triada.feature.learning.*
import com.example.triada.ui.theme.AulaNexoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.util.UUID

class SchoolFlowTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
    private val store=ViewModelStore()
    @After fun close(){store.clear();db.close()}
    @Test fun teacherLinksStudentAndBothSeeTheSameGradeInTheirOwnSections() {
        val schoolRepo=SchoolRepository(db)
        val ana=runBlocking {
            schoolRepo.create("Ana",SchoolRole.STUDENT)
            val student=schoolRepo.accounts().first{it.name=="Ana"}
            val learning=RoomLearningRepository(db);val profile=requireNotNull(student.profileId)
            var session=learning.openLesson(profile)
            while(!session.completed) {
                learning.answer(session.attemptId,profile,session.question.id,session.question.objectCount,UUID.randomUUID().toString())
                session=learning.advance(session.attemptId,profile)
            }
            student
        }
        val prefs=object:ProfilePreferences {
            override val lastProfileId=MutableStateFlow<String?>(null)
            override suspend fun select(profileId:String){lastProfileId.value=profileId}
        }
        val learning=LearningViewModel(RoomLearningRepository(db),prefs,RoomCatalogRepository(db,AssetCatalogSource(context)))
        val school=SchoolViewModel(db);store.put("learning",learning);store.put("school",school)
        compose.setContent{AulaNexoTheme{AulaNexoNavHost(learning,school=school)}}
        compose.waitUntil(10_000){!school.state.value.busy && school.state.value.accounts.isNotEmpty()}
        compose.onNodeWithTag("account-demo-teacher").performScrollTo().performClick()
        compose.waitUntil(10_000){!school.state.value.busy && school.state.value.active?.id=="demo-teacher"}
        compose.onNodeWithText("Vincular alumnos").performScrollTo().performClick()
        compose.onNodeWithTag("link-${ana.id}").performScrollTo().performClick()
        compose.waitUntil(10_000){!school.state.value.busy && school.state.value.students.size==1}
        compose.onNodeWithTag("grades-${ana.id}").performScrollTo().performClick()
        compose.waitUntil(10_000){!school.state.value.busy && school.state.value.gradeStudent?.id==ana.id}
        compose.onNodeWithTag("grade-0").performScrollTo().assertTextContains("20",substring=true)
        compose.onNodeWithText("Volver a mi espacio").performScrollTo().performClick()
        compose.onNodeWithText("Cambiar de cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("account-${ana.id}").performScrollTo().performClick()
        compose.waitUntil(10_000){!school.state.value.busy && school.state.value.active?.id==ana.id}
        compose.onNodeWithTag("student-grades").performScrollTo().performClick()
        compose.waitUntil(10_000){!school.state.value.busy && school.state.value.gradeStudent?.id==ana.id}
        compose.onNodeWithText("Mis notas").assertExists()
        compose.onNodeWithTag("grade-0").performScrollTo().assertTextContains("20",substring=true)
    }
}
