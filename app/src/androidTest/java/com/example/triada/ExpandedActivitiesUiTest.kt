package com.example.triada

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
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpandedActivitiesUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
    private val repository = RoomLearningRepository(database)
    private val catalog = RoomCatalogRepository(database, AssetCatalogSource(context, false))
    private val store = ViewModelStore()
    private lateinit var viewModel: LearningViewModel
    @After fun close() { store.clear(); database.close() }

    private fun open(code: String, lessonId: String, unknown: Boolean = false) {
        val profile = runBlocking {
            val profile = repository.createProfile("Prueba", "sun")
            catalog.load(profile.id)
            if (unknown) database.openHelper.writableDatabase.execSQL("UPDATE questions SET activityType = 'FUTURE_TYPE' WHERE id = 'g1-com-vowels-q01'")
            profile
        }
        val preferences = object : ProfilePreferences {
            override val lastProfileId = MutableStateFlow<String?>(null)
            override suspend fun select(profileId: String) { lastProfileId.value = profileId }
        }
        viewModel = LearningViewModel(repository, preferences, catalog)
        store.put("learning", viewModel)
        compose.setContent { AulaNexoTheme { AulaNexoNavHost(viewModel) } }
        compose.waitUntil(10_000) { !viewModel.state.value.loadingProfiles }
        compose.onNodeWithTag("profile-Prueba").performScrollTo().performClick()
        compose.waitUntil(10_000) { viewModel.state.value.catalog?.profileId == profile.id && !viewModel.state.value.busy }
        fun catalogClick(tag: String) {
            compose.onNodeWithTag("catalog-list").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).performClick()
            idle()
        }
        catalogClick("grade-g1")
        catalogClick("subject-$code")
        catalogClick("unit-g1-${code.lowercase()}-u1")
        catalogClick("lesson-$lessonId")
        compose.waitUntil(10_000) { viewModel.state.value.session?.lessonId == lessonId && !viewModel.state.value.busy }
        tap("open-lesson")
    }
    private fun idle() { compose.waitForIdle(); compose.waitUntil(10_000) { !viewModel.state.value.busy } }
    private fun tap(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick(); idle() }

    @Test fun vowelsAndLetterTilesCanBeCompletedWithCorrection() {
        open("COM", "g1-com-vowels")
        tap("answer-o2"); tap("next-question")
        tap("answer-o1"); tap("next-question")
        tap("tile-t1"); tap("tile-t2"); tap("tile-t3"); tap("submit-word")
        assertFalse(viewModel.state.value.session!!.currentSolved)
        assertEquals(2, viewModel.state.value.session!!.solvedQuestions)
        tap("clear-word")
        tap("tile-t2"); tap("tile-t3"); tap("tile-t1"); tap("submit-word")
        assertTrue(viewModel.state.value.session!!.completed)
        compose.onNodeWithText("¡Lo lograste!").performScrollTo().assertIsDisplayed()
    }

    @Test fun associationsRequireEveryPairAndCanBeReassigned() {
        open("CYA", "g1-cya-body")
        tap("pair-left-word-hand"); tap("pair-right-image-foot")
        compose.onNodeWithTag("submit-pairs").performScrollTo().assertIsNotEnabled()
        tap("pair-left-word-foot"); tap("pair-right-image-hand"); tap("submit-pairs")
        assertEquals(0, viewModel.state.value.session!!.solvedQuestions)
        tap("pair-left-word-hand"); tap("pair-right-image-hand")
        compose.onNodeWithTag("submit-pairs").performScrollTo().assertIsNotEnabled()
        tap("pair-left-word-foot"); tap("pair-right-image-foot"); tap("submit-pairs")
        assertEquals(1, viewModel.state.value.session!!.solvedQuestions)
        tap("next-question")
        compose.onNodeWithTag("pair-left-word-eye").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("submit-pairs").performScrollTo().assertIsNotEnabled()
    }

    @Test fun sequencesShowLabelsAndAllowAnIncorrectAnswerBeforeCompletion() {
        open("RMA", "g1-rma-patterns")
        compose.onNodeWithTag("sequence-0").performScrollTo().assertIsDisplayed()
        tap("answer-square")
        assertEquals(0, viewModel.state.value.session!!.solvedQuestions)
        tap("answer-circle"); tap("next-question")
        tap("answer-circle"); tap("next-question")
        tap("answer-triangle")
        assertTrue(viewModel.state.value.session!!.completed)
    }

    @Test fun soundLessonUsesLocalAudioAndSavesTheChosenWord() {
        open("RVE", "g1-rve-initial-sounds")
        tap("audio-cue")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Audio reproducido.").fetchSemanticsNodes().isNotEmpty() }
        tap("audio-o2")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Audio reproducido.").fetchSemanticsNodes().isNotEmpty() }
        tap("answer-o2")
        assertEquals(1, viewModel.state.value.session!!.solvedQuestions)
        tap("next-question")
        compose.onNodeWithTag("audio-cue").performScrollTo().assertIsEnabled()
        tap("answer-o3"); tap("next-question"); tap("answer-o1")
        assertTrue(viewModel.state.value.session!!.completed)
    }

    @Test fun unknownActivityShowsARecoverableMessage() {
        open("COM", "g1-com-vowels", unknown = true)
        compose.onNodeWithText("Esta actividad todavía no está disponible. Puedes volver a tus lecciones y elegir otra.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("answer-o2").assertDoesNotExist()
        compose.onNodeWithText("Volver a mi aventura").performScrollTo().performClick()
        idle()
        compose.onNodeWithTag("open-lesson").performScrollTo().assertExists()
    }
}
