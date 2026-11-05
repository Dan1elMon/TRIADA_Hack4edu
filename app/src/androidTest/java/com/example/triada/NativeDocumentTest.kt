package com.example.triada

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.data.local.*
import com.example.triada.data.packageio.SafePackageReader
import com.example.triada.data.repository.*
import com.example.triada.feature.learning.*
import com.example.triada.ui.theme.AulaNexoTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NativeDocumentTest {
    @get:Rule val compose=createComposeRule()
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private val db=Room.inMemoryDatabaseBuilder(context,LearningDatabase::class.java).build()
    private val store=ViewModelStore()
    private var created:android.net.Uri?=null
    private val name="aulanexo-test-${java.util.UUID.randomUUID()}.aula"
    @After fun close() {
        created?.let { uri ->
            context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { cursor ->
                if(cursor.moveToFirst() && cursor.getString(0) in listOf(name,"$name.zip")) android.provider.DocumentsContract.deleteDocument(context.contentResolver,uri)
            }
        }
        store.clear();db.close()
    }
    private fun find(node:AccessibilityNodeInfo?, predicate:(AccessibilityNodeInfo)->Boolean):AccessibilityNodeInfo? {
        if(node==null)return null
        if(predicate(node))return node
        repeat(node.childCount){ n->find(node.getChild(n),predicate)?.let{return it} }
        return null
    }
    @Test fun nativeSaveWritesARealDocumentAndCancelDoesNotImport() {
        val vm=PackageViewModel(db,context);store.put("packages",vm)
        val profile=runBlocking {RoomLearningRepository(db).createProfile("Documento","star")}
        compose.setContent{AulaNexoTheme{PackageScreen(vm,profile.id,{})}}
        compose.onNodeWithTag("export-catalog").performScrollTo().performClick()
        var field:AccessibilityNodeInfo?=null
        compose.waitUntil(15_000){field=find(instrumentation.uiAutomation.rootInActiveWindow){it.isVisibleToUser && it.isEditable && it.className?.toString()?.contains("EditText")==true};field!=null}
        assertTrue(field!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,name)}))
        var save:AccessibilityNodeInfo?=null
        compose.waitUntil(10_000){save=find(instrumentation.uiAutomation.rootInActiveWindow){it.isEnabled && it.isClickable && it.text?.toString()?.lowercase() in listOf("guardar","save")};save!=null}
        assertTrue(save!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(15_000){vm.state.value.savedUri!=null && !vm.state.value.busy}
        created=vm.state.value.savedUri
        val files=context.contentResolver.openInputStream(requireNotNull(created))!!.use{SafePackageReader().read(it)}
        assertTrue(files.containsKey("catalog.json"))
        compose.onNodeWithTag("share-export").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("import-catalog").performScrollTo().performClick()
        compose.waitUntil(10_000){instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui")==true}
        repeat(4) {
            if(instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui")==true) {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 4")).use { it.readBytes() }
                android.os.SystemClock.sleep(500)
            }
        }
        compose.waitUntil(15_000){runCatching { compose.onAllNodesWithTag("export-catalog").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)}
        compose.waitForIdle()
        compose.onNodeWithTag("export-catalog").performScrollTo().assertIsEnabled()
        runBlocking{assertNull(db.catalogDao().metadata())}
    }
}
