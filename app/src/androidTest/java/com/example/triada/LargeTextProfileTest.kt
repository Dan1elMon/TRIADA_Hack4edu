package com.example.triada

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.core.navigation.AulaNexoNavHost
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.repository.ProfilePreferences
import com.example.triada.feature.learning.LearningViewModel
import com.example.triada.ui.theme.AulaNexoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class LargeTextProfileTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
    private val store=ViewModelStore()
    @After fun close(){store.clear();db.close()}
    @Test fun profileCanBeEditedAtDoubleFontScaleWithoutLosingProgress() {
        val repo=RoomLearningRepository(db)
        val original=runBlocking {
            val p=repo.createProfile("Original","star");val session=repo.openLesson(p.id)
            repo.answer(session.attemptId,p.id,session.question.id,4,"kept")
            p to session
        }
        val preferences=object:ProfilePreferences {
            override val lastProfileId=MutableStateFlow<String?>(null)
            override suspend fun select(profileId:String){lastProfileId.value=profileId}
        }
        val vm=LearningViewModel(repo,preferences,RoomCatalogRepository(db,AssetCatalogSource(context, false)));store.put("vm",vm)
        compose.setContent {
            val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,2f)){AulaNexoTheme{AulaNexoNavHost(vm)}}
        }
        compose.waitUntil(10_000){!vm.state.value.loadingProfiles}
        compose.onNodeWithTag("edit-Original").performScrollTo().performClick()
        compose.onNodeWithTag("edit-name").performTextClearance();compose.onNodeWithTag("edit-name").performTextInput("Nuevo")
        compose.onNodeWithTag("edit-avatar-flower").performClick()
        compose.onNodeWithTag("save-profile").performClick()
        compose.waitUntil(10_000){!vm.state.value.busy && vm.state.value.profiles.first().displayName=="Nuevo"}
        compose.onNodeWithTag("profile-Nuevo").performScrollTo().assertIsDisplayed()
        runBlocking {
            assertEquals("flower",db.learningDao().profile(original.first.id)!!.avatar)
            assertEquals(1,repo.session(original.second.attemptId,original.first.id).solvedQuestions)
        }
    }
}
