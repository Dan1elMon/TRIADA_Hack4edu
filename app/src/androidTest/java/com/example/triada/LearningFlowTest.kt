package com.example.triada

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.core.navigation.AulaNexoNavHost
import com.example.triada.data.local.LearningDatabase
import com.example.triada.data.repository.RoomLearningRepository
import com.example.triada.domain.repository.ProfilePreferences
import com.example.triada.feature.learning.LearningViewModel
import com.example.triada.ui.theme.AulaNexoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

class LearningFlowTest {
    @get:Rule val compose = createComposeRule()
    private val database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext, LearningDatabase::class.java,
    ).build()
    private val store = ViewModelStore()

    @After fun close() {
        store.clear()
        database.close()
    }

    @Test fun profileLessonErrorHintsAndConfirmedProgress() {
        val preferences = object : ProfilePreferences {
            override val lastProfileId = MutableStateFlow<String?>(null)
            override suspend fun select(profileId: String) { lastProfileId.value = profileId }
        }
        val viewModel = LearningViewModel(RoomLearningRepository(database), preferences)
        store.put("learning", viewModel)
        compose.setContent { AulaNexoTheme { AulaNexoNavHost(viewModel) } }
        compose.waitUntil(10_000) { !viewModel.state.value.loadingProfiles }
        compose.onNodeWithTag("profile-name").performScrollTo().performTextInput("Explorador")
        compose.onNodeWithTag("create-profile").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.profiles.isNotEmpty() && !viewModel.state.value.busy }
        compose.onNodeWithTag("profile-Explorador").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session != null && !viewModel.state.value.busy }
        compose.onNodeWithTag("open-lesson").performScrollTo().performClick()
        compose.onNodeWithTag("answer-3").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.feedback?.startsWith("Contemos") == true && !viewModel.state.value.busy }
        compose.onNodeWithText("Contemos otra vez, despacio. Puedes pedir una pista.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("hint").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session?.visibleHints?.size == 1 && !viewModel.state.value.busy }
        compose.onNodeWithTag("hint").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session?.hintsExhausted == true && !viewModel.state.value.busy }
        compose.onNodeWithTag("hint").assertIsNotEnabled()
        compose.onNodeWithTag("answer-4").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session?.solvedQuestions == 1 && !viewModel.state.value.busy }
        compose.onNodeWithTag("next-question").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session?.currentIndex == 1 && !viewModel.state.value.busy }
        compose.onNodeWithTag("question-position").performScrollTo().assertTextEquals("Ejercicio 2 de 3")
        compose.onNodeWithTag("progress").performScrollTo().assertTextEquals("1 de 3 ejercicios resueltos")
    }

    @Test fun switchingProfilesLoadsTheirOwnProgress() {
        val repository = RoomLearningRepository(database)
        runBlocking {
            repository.createProfile("Luna", "star")
            repository.createProfile("Sol", "sun")
        }
        val preferences = object : ProfilePreferences {
            override val lastProfileId = MutableStateFlow<String?>(null)
            override suspend fun select(profileId: String) { lastProfileId.value = profileId }
        }
        val viewModel = LearningViewModel(repository, preferences)
        store.put("learning", viewModel)
        compose.setContent { AulaNexoTheme { AulaNexoNavHost(viewModel) } }
        compose.waitUntil(10_000) { viewModel.state.value.profiles.size == 2 }
        compose.onNodeWithTag("profile-Luna").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session != null && !viewModel.state.value.busy }
        val firstId = viewModel.state.value.session!!.profileId
        compose.onNodeWithTag("open-lesson").performScrollTo().performClick()
        compose.onNodeWithTag("answer-4").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session?.solvedQuestions == 1 && !viewModel.state.value.busy }
        compose.onNodeWithText("Volver a mi aventura").performScrollTo().performClick()
        compose.waitUntil(10_000) { !viewModel.state.value.busy }
        compose.onNodeWithText("Cambiar de perfil").performScrollTo().performClick()
        compose.onNodeWithTag("profile-Sol").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            val session = viewModel.state.value.session
            session != null && session.profileId != firstId && !viewModel.state.value.busy
        }
        compose.onNodeWithTag("progress").performScrollTo().assertTextEquals("0 de 3 ejercicios resueltos")
        compose.onNodeWithText("Cambiar de perfil").performScrollTo().performClick()
        compose.onNodeWithTag("profile-Luna").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.session?.profileId == firstId && !viewModel.state.value.busy }
        compose.onNodeWithTag("progress").performScrollTo().assertTextEquals("1 de 3 ejercicios resueltos")
    }
}
