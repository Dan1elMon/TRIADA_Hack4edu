package com.example.triada.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.triada.domain.model.*

@Composable
fun StickerAlbumScreen(state: LearningUiState, profileId: String, onBack: () -> Unit, onRetry: () -> Unit) {
    CatalogPage(state, profileId, "Mi álbum", "Cada lección completada deja un recuerdo de tu aventura.", "Volver a mis grados", onBack, onRetry) { catalog ->
        val earned = state.rewards.filter { it.profileId == profileId }.map { it.stickerId }.toSet()
        item { Text("${StickerCatalog.all.count { it.id in earned }} de ${StickerCatalog.all.size} stickers obtenidos", modifier = Modifier.testTag("album-count")) }
        StickerCatalog.all.groupBy { it.category }.forEach { (category, stickers) ->
            item { Heading(category, "Una colección para seguir descubriendo.") }
            items(stickers, key = { it.id }) { sticker ->
                val obtained = sticker.id in earned
                Card(modifier = Modifier.fillMaxWidth().testTag("sticker-${sticker.id}"), colors = CardDefaults.cardColors(
                    containerColor = if (obtained) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (obtained) sticker.symbol else "?", fontSize = 44.sp, modifier = Modifier.semantics { contentDescription = if (obtained) sticker.title else "Sticker pendiente" })
                        Text(sticker.title, style = MaterialTheme.typography.titleLarge)
                        Text(if (obtained) "Obtenido" else "Pendiente", modifier = Modifier.testTag("sticker-status-${sticker.id}"))
                        val title = catalog.lessons.firstOrNull { it.id == sticker.lessonId }?.title ?: "la lección correspondiente"
                        Text("Completa $title. Repetir la lección conserva este sticker sin duplicarlo.")
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressScreen(state: LearningUiState, profileId: String, onBack: () -> Unit, onRetry: () -> Unit, onLesson: (String) -> Unit) {
    CatalogPage(state, profileId, "Mi progreso", "Mira todo lo que has descubierto, paso a paso.", "Volver a mis grados", onBack, onRetry) { catalog ->
        val overview = ProgressOverview.from(catalog)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${overview.completedLessons} de ${overview.totalLessons} lecciones completadas", modifier = Modifier.testTag("overview-lessons"))
                Text("${overview.solvedExercises} de ${overview.totalExercises} ejercicios resueltos", modifier = Modifier.testTag("overview-exercises"))
                LinearProgressIndicator(progress = { overview.fraction }, modifier = Modifier.fillMaxWidth())
                Text("Se muestra tu mejor avance. Las prácticas nuevas y los temas en preparación no lo reducen.")
            }
        }
        catalog.subjects.forEach { subject ->
            val topicIds = catalog.topics.filter { it.subjectId == subject.id }.map { it.id }.toSet()
            val lessons = catalog.lessons.filter { it.topicId in topicIds }
            item { Heading(subject.title, "${lessons.count { it.status == LearningStatus.COMPLETED }} de ${lessons.size} lecciones completadas") }
            items(lessons, key = { "${it.id}-${it.contentVersion}" }) { lesson ->
                OutlinedCard(onClick = { onLesson(lesson.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("progress-lesson-${lesson.id}")) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleLarge)
                        Text("${lesson.solvedQuestions} de ${lesson.totalQuestions} ejercicios resueltos")
                        Text(when (lesson.status) { LearningStatus.COMPLETED -> "Completada · Volver a practicar"; LearningStatus.IN_PROGRESS -> "En progreso · Continuar"; else -> "Sin empezar · Explorar" })
                    }
                }
            }
        }
    }
}
