package com.example.triada

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.*
import com.example.triada.feature.learning.*
import com.example.triada.ui.theme.AulaNexoTheme
import kotlinx.coroutines.runBlocking
import org.junit.*

class ExtendedUiTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
    private val store=ViewModelStore()
    @After fun close(){store.clear();db.close()}
    @Test fun loginAndCredentialFieldsRemainUsableAtDoubleTextScale(){
        val vm=SchoolViewModel(db);store.put("school",vm)
        var credentials by mutableStateOf(false)
        compose.setContent{KeepTestScreenOn();val density=LocalDensity.current.density;CompositionLocalProvider(LocalDensity provides Density(density,2f)){AulaNexoTheme{
            val state by vm.state.collectAsState()
            if(credentials)CredentialsScreen(state,{},vm::changeCredentials) else AccountsScreen(state,vm::login,vm::refresh)
        }}}
        compose.waitUntil(90_000){!vm.state.value.busy}
        org.junit.Assert.assertNull(vm.state.value.error)
        org.junit.Assert.assertTrue(vm.state.value.accounts.isNotEmpty())
        compose.onNodeWithTag("login-user").performScrollTo().performTextInput("10000001")
        compose.onNodeWithTag("login-password").performScrollTo().performTextInput("admin")
        compose.onNodeWithTag("login").performScrollTo().performClick()
        compose.waitUntil(10_000){!vm.state.value.busy&&vm.state.value.active!=null}
        compose.runOnIdle{credentials=true}
        compose.onNodeWithTag("new-username").performScrollTo().performTextClearance()
        compose.onNodeWithTag("new-username").performTextInput("12345678")
        compose.onNodeWithTag("current-password").performScrollTo().performTextInput("admin")
        compose.onNodeWithTag("new-password").performScrollTo().performTextInput("nuevaClave")
        compose.onNodeWithTag("confirm-password").performScrollTo().performTextInput("nuevaClave")
        compose.onNodeWithTag("save-credentials").performScrollTo().performClick()
        compose.waitUntil(10_000){!vm.state.value.busy&&vm.state.value.active?.username=="12345678"}
        compose.onNodeWithText("Usuario y contraseña actualizados.").performScrollTo().assertIsDisplayed()
    }
    @Test fun readerCanNavigateListenAndAnswerAtDoubleTextScale(){
        val profile=runBlocking{RoomLearningRepository(db).createProfile("Lectura","star")}
        val vm=ReadingViewModel(db,context);store.put("reading",vm)
        vm.open(profile.id,"semilla")
        compose.setContent{KeepTestScreenOn();val density=LocalDensity.current.density;CompositionLocalProvider(LocalDensity provides Density(density,2f)){AulaNexoTheme{
            val state by vm.state.collectAsState();ReaderScreen(state,profile.id,"semilla",{},vm::move,vm::finish,vm::answer,{vm.open(profile.id,"semilla")})
        }}}
        compose.waitUntil(10_000){!vm.state.value.busy&&vm.state.value.book!=null}
        compose.onNodeWithTag("listen-page").performScrollTo().performClick()
        repeat(2){n->compose.onNodeWithTag("reading-next").performScrollTo().performClick();compose.waitUntil(10_000){!vm.state.value.busy&&vm.state.value.progress?.pageIndex==n+1}}
        compose.onNodeWithTag("finish-reading").performScrollTo().performClick()
        compose.waitUntil(10_000){!vm.state.value.busy&&vm.state.value.progress?.completed==true}
        compose.onNodeWithTag("reading-answer-0").performScrollTo().performClick()
        compose.waitUntil(10_000){!vm.state.value.busy&&vm.state.value.progress?.comprehensionCorrect==true}
    }
}
