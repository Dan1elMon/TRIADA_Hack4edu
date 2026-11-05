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
import org.junit.Rule
import org.junit.Test

class RewardsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
    private val store = ViewModelStore()
    private lateinit var vm: LearningViewModel
    @After fun close() { store.clear(); db.close() }
    private fun click(tag: String) {
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle(); compose.waitUntil(10_000) { !vm.state.value.busy }
    }
    private fun back(label: String) {
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle(); compose.waitUntil(10_000) { !vm.state.value.busy }
    }
    private fun select(name: String) {
        compose.onNodeWithTag("profile-$name").performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.state.value.catalog != null && !vm.state.value.busy }
    }
    @Test fun albumAndOverviewDisplayOnlyTheSelectedProfile() {
        val repo = RoomLearningRepository(db)
        runBlocking {
            val a = repo.createProfile("Luna", "star")
            repo.createProfile("Sol", "sun")
            var session = repo.openLesson(a.id)
            listOf(4,6,9).forEachIndexed { index, choice ->
                repo.answer(session.attemptId, a.id, session.question.id, choice, "answer-$index")
                if (index < 2) session = repo.advance(session.attemptId, a.id)
            }
        }
        val preferences = object : ProfilePreferences {
            override val lastProfileId = MutableStateFlow<String?>(null)
            override suspend fun select(profileId: String) { lastProfileId.value = profileId }
        }
        vm = LearningViewModel(repo, preferences, RoomCatalogRepository(db, AssetCatalogSource(context, false)))
        store.put("learning", vm)
        compose.setContent { AulaNexoTheme { AulaNexoNavHost(vm) } }
        compose.waitUntil(10_000) { !vm.state.value.loadingProfiles }
        select("Luna"); click("open-album")
        compose.onNodeWithTag("album-count").assertTextEquals("1 de 6 stickers obtenidos")
        compose.onNodeWithTag("catalog-list").performScrollToNode(hasTestTag("sticker-status-counting-sun"))
        compose.onNodeWithTag("sticker-status-counting-sun").assertTextEquals("Obtenido")
        back("Volver a mis grados"); click("open-progress")
        compose.onNodeWithTag("overview-lessons").assertTextEquals("1 de 6 lecciones completadas")
        compose.onNodeWithTag("overview-exercises").assertTextEquals("3 de 18 ejercicios resueltos")
        back("Volver a mis grados"); back("Cambiar de perfil")
        select("Sol"); click("open-album")
        compose.onNodeWithTag("album-count").assertTextEquals("0 de 6 stickers obtenidos")
        back("Volver a mis grados"); click("open-progress")
        compose.onNodeWithTag("overview-lessons").assertTextEquals("0 de 6 lecciones completadas")
    }
}
