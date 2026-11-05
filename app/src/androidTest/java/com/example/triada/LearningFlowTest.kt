package com.example.triada

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.core.navigation.AulaNexoNavHost
import com.example.triada.data.local.AssetCatalogSource
import com.example.triada.data.local.LearningDatabase
import com.example.triada.data.repository.RoomCatalogRepository
import com.example.triada.data.repository.RoomLearningRepository
import com.example.triada.domain.repository.ProfilePreferences
import com.example.triada.feature.learning.LearningViewModel
import com.example.triada.ui.theme.AulaNexoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LearningFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
    private val repository = RoomLearningRepository(database)
    private val store = ViewModelStore()
    private lateinit var viewModel: LearningViewModel

    @After fun close() { store.clear(); database.close() }

    private fun start() {
        val preferences = object : ProfilePreferences {
            override val lastProfileId = MutableStateFlow<String?>(null)
            override suspend fun select(profileId: String) { lastProfileId.value = profileId }
        }
        viewModel = LearningViewModel(repository, preferences, RoomCatalogRepository(database, AssetCatalogSource(context, false)))
        store.put("learning", viewModel)
        compose.setContent { AulaNexoTheme { AulaNexoNavHost(viewModel) } }
        compose.waitUntil(10_000) { !viewModel.state.value.loadingProfiles }
    }

    private fun idle() {
        compose.waitForIdle()
        compose.waitUntil(10_000) { !viewModel.state.value.busy }
    }

    private fun catalogClick(tag: String) {
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
        idle()
    }

    private fun back(label: String) {
        if (compose.onAllNodesWithTag("catalog-list").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("catalog-list").performScrollToNode(hasText(label))
            compose.onNodeWithText(label).performClick()
        } else compose.onNodeWithText(label).performScrollTo().performClick()
        idle()
    }

    private fun select(name: String) {
        compose.onNodeWithTag("profile-$name").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.catalog != null && !viewModel.state.value.busy }
    }

    private fun mathOverview() {
        catalogClick("grade-g1")
        catalogClick("subject-MAT")
        catalogClick("unit-g1-mat-u1")
        catalogClick("lesson-g1-mat-count-1-9")
        compose.waitUntil(10_000) { viewModel.state.value.session != null && !viewModel.state.value.busy }
    }

    private fun profilesFromOverview() {
        back("Volver a mis lecciones")
        back("Volver a mis unidades")
        back("Volver a mis áreas")
        back("Volver a mis grados")
        back("Cambiar de perfil")
    }

    @Test fun profileLessonErrorHintsAndConfirmedProgress() {
        start()
        compose.onNodeWithTag("profile-name").performScrollTo().performTextInput("Explorador")
        compose.onNodeWithTag("create-profile").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.profiles.isNotEmpty() && !viewModel.state.value.busy }
        select("Explorador")
        mathOverview()
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
        runBlocking {
            val a = repository.createProfile("Luna", "star")
            repository.createProfile("Sol", "sun")
            val session = repository.openLesson(a.id)
            repository.answer(session.attemptId, a.id, session.question.id, 4, "luna-answer")
        }
        start()
        select("Luna")
        mathOverview()
        compose.onNodeWithTag("progress").performScrollTo().assertTextEquals("1 de 3 ejercicios resueltos")
        profilesFromOverview()
        select("Sol")
        mathOverview()
        compose.onNodeWithTag("progress").performScrollTo().assertTextEquals("0 de 3 ejercicios resueltos")
        profilesFromOverview()
        select("Luna")
        mathOverview()
        compose.onNodeWithTag("progress").performScrollTo().assertTextEquals("1 de 3 ejercicios resueltos")
    }

    @Test fun sixAreasAndReferenceTopicsAreBrowseableWithoutStartingAttempts() {
        val profile = runBlocking { repository.createProfile("Temario", "flower") }
        start()
        select("Temario")
        catalogClick("grade-g1")
        listOf("MAT", "COM", "CYA", "PSO", "RMA", "RVE").forEach { code ->
            compose.onNodeWithTag("catalog-list").performScrollToNode(hasTestTag("subject-$code"))
            compose.onNodeWithTag("subject-$code").assertHasClickAction()
        }
        catalogClick("subject-COM")
        catalogClick("unit-g1-com-u1")
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasTestTag("lesson-g1-com-vowels"))
        compose.onNodeWithTag("lesson-g1-com-vowels").assertHasClickAction()
        compose.onNodeWithTag("open-lesson").assertDoesNotExist()
        back("Volver a mis unidades")
        catalogClick("view-reference")
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasTestTag("reference-g1-com-t58"))
        compose.onNodeWithTag("reference-g1-com-t58").assertHasNoClickAction()
        runBlocking { assertNull(database.learningDao().latestAttempt(profile.id, "g1-mat-count-1-9", 1)) }
    }
}
