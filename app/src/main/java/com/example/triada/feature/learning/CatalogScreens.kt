package com.example.triada.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.triada.domain.model.*

@Composable
internal fun CatalogPage(
    state: LearningUiState, profileId: String, title: String, subtitle: String,
    backLabel: String, onBack: () -> Unit, onRetry: () -> Unit,
    content: LazyListScope.(LearningCatalog) -> Unit,
) {
    val catalog = state.catalog?.takeIf { it.profileId == profileId }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("catalog-list"),
            contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { TextButton(onClick = onBack, enabled = !state.busy) { Text(backLabel) } }
            item { Text("AulaNexo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { Heading(title, subtitle) }
            if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.error != null) item { Notice(state.error, true) }
            if (catalog != null) content(catalog)
            else if (!state.busy) item { ActionButton("Volver a cargar", true, "retry-catalog", onRetry) }
        }
    }
}

@Composable
private fun CatalogCard(tag: String, title: String, description: String, badge: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Eyebrow(badge)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyLarge)
            Text("Explorar", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GradesScreen(state: LearningUiState, profileId: String, onBack: () -> Unit, onGrade: (String) -> Unit, onRetry: () -> Unit, onAlbum: () -> Unit, onProgress: () -> Unit, onPackages: (() -> Unit)? = null) {
    val name = state.profiles.firstOrNull { it.id == profileId }?.displayName ?: "explorador"
    CatalogPage(state, profileId, "¡Hola, $name!", "Elige tu grado y descubre tu próxima aventura.", "Cambiar de perfil", onBack, onRetry) { catalog ->
        items(catalog.grades, key = { it.id }) { grade ->
            val areas = catalog.subjects.count { it.gradeId == grade.id }
            CatalogCard("grade-${grade.id}", grade.label, "$areas áreas para explorar a tu ritmo.", "MI APRENDIZAJE", !state.busy) { onGrade(grade.id) }
        }
        item { ActionButton("Mi álbum de stickers", !state.busy, "open-album", onAlbum) }
        item { ActionButton("Mi progreso", !state.busy, "open-progress", onProgress) }
        if (onPackages != null) item { ActionButton("Intercambiar catálogo", !state.busy, "open-packages", onPackages) }
        item { Notice("Tus avances se guardan en este dispositivo, por separado para cada perfil.") }
    }
}

@Composable
fun SubjectsScreen(state: LearningUiState, profileId: String, gradeId: String, onBack: () -> Unit, onSubject: (String) -> Unit, onRetry: () -> Unit) {
    val grade = state.catalog?.grades?.firstOrNull { it.id == gradeId }
    CatalogPage(state, profileId, grade?.label ?: "Tus áreas", "¿Qué te gustaría descubrir hoy?", "Volver a mis grados", onBack, onRetry) { catalog ->
        val subjects = catalog.subjects.filter { it.gradeId == gradeId }
        if (subjects.isEmpty()) item { Notice("No encontramos áreas para este grado. Puedes volver y elegir otro.") }
        items(subjects, key = { it.id }) { subject ->
            val topics = catalog.topics.filter { it.subjectId == subject.id }.map { it.id }.toSet()
            val lessons = catalog.lessons.filter { it.topicId in topics }
            val description = if (lessons.isEmpty()) "Próximas lecciones · ${subject.referenceCount} temas de referencia" else
                "${lessons.size} lección disponible · ${lessons.count { it.status == LearningStatus.COMPLETED }} completadas"
            CatalogCard("subject-${subject.code}", subject.title, description, subject.code, !state.busy) { onSubject(subject.id) }
        }
    }
}

@Composable
fun UnitsScreen(state: LearningUiState, profileId: String, subjectId: String, onBack: () -> Unit, onUnit: (String) -> Unit, onReference: () -> Unit, onRetry: () -> Unit) {
    val subject = state.catalog?.subjects?.firstOrNull { it.id == subjectId }
    CatalogPage(state, profileId, subject?.title ?: "Tus unidades", "Pequeños pasos para aprender algo nuevo.", "Volver a mis áreas", onBack, onRetry) { catalog ->
        val units = catalog.units.filter { it.subjectId == subjectId }
        if (units.isEmpty()) item { Notice("Todavía no hay unidades en esta área.") }
        items(units, key = { it.id }) { unit ->
            val topicIds = catalog.topics.filter { it.unitId == unit.id }.map { it.id }.toSet()
            val available = catalog.lessons.count { it.topicId in topicIds }
            CatalogCard("unit-${unit.id}", unit.title, unit.description, if (available > 0) "$available LECCIÓN DISPONIBLE" else "EN PREPARACIÓN", !state.busy) { onUnit(unit.id) }
        }
        if (subject != null) item {
            OutlinedButton(onClick = onReference, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("view-reference")) {
                Text("Ver los ${subject.referenceCount} temas de referencia")
            }
        }
    }
}

@Composable
fun UnitLessonsScreen(state: LearningUiState, profileId: String, unitId: String, onBack: () -> Unit, onLesson: (String) -> Unit, onRetry: () -> Unit) {
    val unit = state.catalog?.units?.firstOrNull { it.id == unitId }
    CatalogPage(state, profileId, unit?.title ?: "Tus lecciones", unit?.description ?: "Exploramos a tu ritmo.", "Volver a mis unidades", onBack, onRetry) { catalog ->
        val topics = catalog.topics.filter { it.unitId == unitId }
        val topicIds = topics.map { it.id }.toSet()
        val lessons = catalog.lessons.filter { it.topicId in topicIds }
        if (lessons.isEmpty()) item { Notice("Estamos preparando las actividades de esta unidad. Puedes explorar otra área mientras tanto.") }
        items(lessons, key = { "${it.id}-${it.contentVersion}" }) { lesson ->
            val label = when (lesson.status) {
                LearningStatus.NOT_STARTED -> "LISTA PARA COMENZAR"
                LearningStatus.IN_PROGRESS -> "EN PROGRESO"
                LearningStatus.COMPLETED -> "COMPLETADA"
            }
            CatalogCard("lesson-${lesson.id}", lesson.title, "${lesson.solvedQuestions} de ${lesson.totalQuestions} ejercicios resueltos", label, !state.busy) { onLesson(lesson.id) }
        }
        items(topics.filter { it.contentStatus == ContentAvailability.CATALOG_ONLY }, key = { it.id }) { topic -> ReferenceTopic(topic) }
        if (unit == null) item { Notice("No encontramos esta unidad. Vuelve a las áreas para continuar.") }
    }
}

@Composable
private fun ReferenceTopic(topic: Topic) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().testTag("reference-${topic.id}")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${topic.sourceOrder}. ${topic.title}", style = MaterialTheme.typography.titleMedium)
            Text("En preparación · Tema de referencia, sin actividades todavía", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ReferenceTopicsScreen(state: LearningUiState, profileId: String, subjectId: String, onBack: () -> Unit, onLesson: (String) -> Unit, onRetry: () -> Unit) {
    val subject = state.catalog?.subjects?.firstOrNull { it.id == subjectId }
    CatalogPage(state, profileId, "Temas de referencia", subject?.title ?: "Exploramos el temario", "Volver a mis unidades", onBack, onRetry) { catalog ->
        item { Notice("Esta lista reúne los títulos del temario. Los temas en preparación todavía no tienen actividades. Puedes elegir las lecciones disponibles.") }
        items(catalog.topics.filter { it.subjectId == subjectId }.sortedBy { it.sourceOrder }, key = { it.id }) { topic ->
            val lesson = catalog.lessons.firstOrNull { it.topicId == topic.id }
            if (topic.contentStatus == ContentAvailability.AVAILABLE && lesson != null) {
                CatalogCard("available-${topic.id}", "${topic.sourceOrder}. ${topic.title}", lesson.title, "LECCIÓN DISPONIBLE", !state.busy) { onLesson(lesson.id) }
            } else ReferenceTopic(topic)
        }
    }
}
