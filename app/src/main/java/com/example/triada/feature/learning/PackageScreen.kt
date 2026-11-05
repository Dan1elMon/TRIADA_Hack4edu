package com.example.triada.feature.learning

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.platform.LocalContext

class SaveLearningDocument : ActivityResultContract<String, Uri?>() {
    private val delegate = ActivityResultContracts.CreateDocument("application/zip")
    override fun createIntent(context: Context, input: String): Intent = delegate.createIntent(context,input).apply {
        if (android.os.Build.VERSION.SDK_INT >= 26) putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,
            "content://com.android.externalstorage.documents/document/primary:Download".toUri())
    }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = delegate.parseResult(resultCode,intent)
}

@Composable
fun PackageScreen(viewModel: PackageViewModel, profileId: String, onBack: () -> Unit,contentOnly:Boolean=false) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(profileId) { viewModel.reset() }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), viewModel::importFile)
    val exporter = rememberLauncherForActivityResult(SaveLearningDocument(), viewModel::exportFile)
    val results = rememberLauncherForActivityResult(SaveLearningDocument()) { uri -> viewModel.exportResults(uri, profileId) }
    val content = rememberLauncherForActivityResult(SaveLearningDocument(), viewModel::exportContent)
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), viewModel::importResults)
    androidx.activity.compose.BackHandler(state.busy) { }
    LearningPage {
        TextButton(onClick = onBack, enabled = !state.busy) { Text(if(contentOnly)"Volver a mi aula" else "Volver a mis grados") }
        Heading("Catálogo y resultados", "Guarda tu progreso o intercambia actividades con audio.")
        Notice("Los paquetes de contenido incluyen catálogo, ejercicios y audios WAV. Se validan antes de activarse. Los contenidos publicados no se sobrescriben: los cambios requieren una nueva versión.")
        ActionButton("Importar catálogo", !state.busy, "import-catalog") { importer.launch(arrayOf("*/*")) }
        ActionButton("Exportar catálogo de la app", !state.busy, "export-catalog") { exporter.launch("aulanexo-catalogo.aula") }
        ActionButton("Exportar contenido instalado", !state.busy, "export-content") { content.launch("aulanexo-contenido.aula") }
        if(!contentOnly) {
        Notice("Los resultados corresponden al perfil con el que abriste esta pantalla. Incluyen respuestas, pistas y progreso con un identificador seudónimo, sin nombre ni avatar. La recuperación crea un perfil separado y requiere tener las mismas lecciones y versiones instaladas.")
        ActionButton("Exportar mis resultados", !state.busy, "export-results") { results.launch("aulanexo-resultados.aula-result") }
        ActionButton("Recuperar resultados", !state.busy, "import-results") { restore.launch(arrayOf("*/*")) }
        }
        if (state.busy) CircularProgressIndicator()
        state.message?.let { Notice(it) }
        state.savedUri?.let { uri ->
            ActionButton("Compartir archivo exportado", !state.busy, "share-export") {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"; putExtra(Intent.EXTRA_STREAM,uri)
                    clipData = android.content.ClipData.newRawUri("AulaNexo",uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try { context.startActivity(Intent.createChooser(intent,"Compartir archivo de AulaNexo")) }
                catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context,"No hay aplicaciones disponibles para compartir.",android.widget.Toast.LENGTH_LONG).show() }
            }
        }
    }
}

