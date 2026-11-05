package com.example.triada.feature.learning

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triada.data.local.*
import com.example.triada.data.packageio.CatalogPackageService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PackageUiState(val busy: Boolean = false, val message: String? = null, val savedUri: Uri? = null)

@HiltViewModel
class PackageViewModel @Inject constructor(private val database: LearningDatabase, @ApplicationContext private val context: Context) : ViewModel() {
    private val mutable = MutableStateFlow(PackageUiState())
    val state = mutable.asStateFlow()
    private val service = CatalogPackageService(database, AssetCatalogSource(context))
    fun reset() { if (!mutable.value.busy) mutable.value = PackageUiState() }
    fun importResults(uri: Uri?) { if (uri != null) runOperation("Resultados recuperados en un perfil separado. Vuelve a seleccionar perfil para revisarlos.") {
        requireNotNull(context.contentResolver.openInputStream(uri)).use {
            com.example.triada.data.packageio.StudentResultsImporter(database).import(it)
        }
    } }
    fun exportContent(uri: Uri?) { if (uri != null) runOperation("Contenido instalado exportado con sus ejercicios y audios.") {
        val staged = java.io.ByteArrayOutputStream()
        com.example.triada.data.packageio.LessonPackageService(database, context).export(staged)
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(staged.toByteArray()) }
        mutable.value = mutable.value.copy(savedUri = uri)
    } }
    fun exportResults(uri: Uri?, profileId: String) { if (uri != null) runOperation("Resultados del estudiante exportados.") {
        val staged = java.io.ByteArrayOutputStream()
        com.example.triada.data.packageio.StudentResultsExporter(database).export(profileId, staged)
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(staged.toByteArray()) }
        mutable.value = mutable.value.copy(savedUri = uri)
    } }
    fun importFile(uri: Uri?) { if (uri != null) runOperation("Catálogo importado. Tus avances se conservaron.") {
        requireNotNull(context.contentResolver.openInputStream(uri)).use {
            val files = com.example.triada.data.packageio.SafePackageReader().read(it)
            val manifest = org.json.JSONObject(files.getValue("manifest.json").toString(Charsets.UTF_8))
            if (manifest.optString("kind") == "learning-content") com.example.triada.data.packageio.LessonPackageService(database, context).importFiles(files)
            else service.importFiles(files)
        }
    } }
    fun exportFile(uri: Uri?) { if (uri != null) runOperation("Catálogo incluido en la app exportado.") {
        val staged = java.io.ByteArrayOutputStream()
        service.exportBundledCatalog(staged)
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(staged.toByteArray()) }
        mutable.value = mutable.value.copy(savedUri = uri)
    } }
    private fun runOperation(success: String, operation: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = PackageUiState(true)
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { operation() }; mutable.value = mutable.value.copy(busy = false, message = success) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.value = PackageUiState(message = "No se pudo completar la operación. Comprueba el archivo y vuelve a intentarlo. Una exportación fallida puede dejar un archivo incompleto.")
            }
        }
    }
}
