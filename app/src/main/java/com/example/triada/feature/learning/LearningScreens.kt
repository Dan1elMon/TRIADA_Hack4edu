package com.example.triada.feature.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.triada.domain.model.LessonSession

@Composable
fun LearningPage(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp), content = content,
        )
    }
}

@Composable
private fun Heading(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text(subtitle, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun Eyebrow(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondary, shape = CircleShape) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Notice(text: String, isError: Boolean = false) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(text, Modifier.padding(16.dp), color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag),
    ) { Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 6.dp)) }
}

@Composable
fun ProfilesScreen(state: LearningUiState, onCreate: (String, String) -> Unit, onSelect: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf("sun") }
    LearningPage {
        Text("AulaNexo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Eyebrow("APRENDEMOS JUNTOS")
        Heading("¿Quién va a explorar hoy?", "Elige tu perfil. Cada aventura se guarda por separado.")
        if (state.loadingProfiles) CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando perfiles" })
        state.profiles.forEach { profile ->
            OutlinedCard(onClick = { onSelect(profile.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("profile-${profile.displayName}")) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Avatar(profile.avatar)
                    Column(Modifier.weight(1f)) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (profile.id == state.lastProfileId) "Tu última aventura" else "Vamos a aprender", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Entrar", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (!state.loadingProfiles && state.profiles.isEmpty()) Notice("Tu primera aventura empieza con un perfil. Puedes usar un apodo.")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Un nuevo explorador", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name, onValueChange = { if (it.length <= 24) name = it },
                    label = { Text("Nombre o apodo") }, singleLine = true, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().testTag("profile-name"),
                    supportingText = { Text("Hasta 24 caracteres. No necesitas tu nombre completo.") },
                )
                Text("Elige tu compañero")
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sun" to "Sol", "star" to "Estrella", "flower" to "Flor").forEach { (id, label) ->
                        FilterChip(selected = avatar == id, onClick = { avatar = id }, enabled = !state.busy, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                ActionButton("Crear perfil", !state.busy && !state.loadingProfiles && name.isNotBlank(), "create-profile") { onCreate(name, avatar) }
            }
        }
        state.feedback?.let { Notice(it) }
        state.error?.let { Notice(it, true) }
        Text("Sin cuentas · Sin anuncios · Aprendizaje sin conexión", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Avatar(id: String) {
    val (symbol, label) = when (id) {
        "star" -> "★" to "Estrella"
        "flower" -> "✿" to "Flor"
        else -> "☀" to "Sol"
    }
    Box(
        Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondary, CircleShape).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, fontSize = 30.sp) }
}

@Composable
fun MapScreen(state: LearningUiState, profileId: String, onBack: () -> Unit, onOpen: () -> Unit, onRetry: () -> Unit) {
    LearningPage {
        TextButton(onClick = onBack, enabled = !state.busy) { Text("Cambiar de perfil") }
        Eyebrow("PRIMER GRADO · MATEMÁTICA")
        val name = state.profiles.firstOrNull { it.id == profileId }?.displayName ?: "explorador"
        Heading("¡Hola, $name!", "Hoy descubrimos los números, paso a paso.")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("01  /  MI PRIMERA AVENTURA", style = MaterialTheme.typography.labelLarge)
                Text("Cuento del 1 al 9", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Cuenta círculos y elige un número. Puedes pedir ayuda cuando quieras.")
                val session = state.session?.takeIf { it.profileId == profileId }
                if (session != null) {
                    Progress(session, overall = true)
                    if (session.everCompleted && !session.completed) Text("Lección completada. Ahora estás en un nuevo intento de práctica.")
                    ActionButton(
                        when { session.completed -> "Ver mi resultado"; session.everCompleted -> "Continuar práctica"; session.solvedQuestions > 0 || session.visibleHints.isNotEmpty() -> "Continuar aventura"; else -> "Comenzar aventura" },
                        !state.busy, "open-lesson", onOpen,
                    )
                } else if (state.busy) CircularProgressIndicator() else {
                    ActionButton("Volver a cargar", true, "reload-lesson", onRetry)
                }
            }
        }
        Notice("Puedes salir y volver. Tu avance confirmado queda guardado en este dispositivo.")
        state.error?.let { Notice(it, true) }
    }
}

@Composable
private fun Progress(session: LessonSession, overall: Boolean = false) {
    val solved = if (overall) session.bestSolvedQuestions else session.solvedQuestions
    Text("$solved de ${session.totalQuestions} ejercicios resueltos", modifier = Modifier.testTag("progress"))
    LinearProgressIndicator(
        progress = { solved.toFloat() / session.totalQuestions },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
fun ActivityScreen(state: LearningUiState, profileId: String, onBack: () -> Unit, onAnswer: (Int) -> Unit, onHint: () -> Unit, onNext: () -> Unit, onPractice: () -> Unit, onReload: () -> Unit) {
    val session = state.session?.takeIf { it.profileId == profileId }
    LearningPage {
        TextButton(onClick = onBack, enabled = !state.busy) { Text("Volver a mi aventura") }
        if (session == null) {
            if (state.busy) CircularProgressIndicator() else ActionButton("Volver a cargar", true, "reload-lesson", onReload)
        } else if (session.completed) {
            Eyebrow("¡AVENTURA COMPLETADA!")
            Heading("¡Lo lograste!", "Practicaste contar con calma, un círculo a la vez.")
            Progress(session)
            Notice("Tu avance está guardado. Puedes volver a practicar cuando quieras.")
            ActionButton("Volver a practicar", !state.busy, "practice-again", onPractice)
            OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Ver mi aventura") }
        } else {
            Eyebrow("MATEMÁTICA · CONTAMOS JUNTOS")
            Text("Ejercicio ${session.currentIndex + 1} de ${session.totalQuestions}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("question-position"))
            Heading(session.question.prompt, "Señala cada círculo y cuenta despacio.")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat((session.question.objectCount + 2) / 3) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            repeat(minOf(3, session.question.objectCount - row * 3)) {
                                Box(
                                    Modifier.size(44.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        .semantics { contentDescription = "Círculo" },
                                )
                            }
                        }
                    }
                }
            }
            Text("Elige un número", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                session.question.options.forEach { choice ->
                    OutlinedButton(
                        onClick = { onAnswer(choice) }, enabled = !state.busy && !session.currentSolved,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("answer-$choice"),
                    ) { Text("$choice", style = MaterialTheme.typography.headlineMedium) }
                }
            }
            state.feedback?.let { Notice(it) }
            if (session.currentSolved) {
                if (state.feedback == null) Notice("¡Este ejercicio ya está resuelto! Sigamos con el siguiente.")
                ActionButton("Siguiente ejercicio", !state.busy, "next-question", onNext)
            } else {
                OutlinedButton(onClick = onHint, enabled = !state.busy && !session.hintsExhausted, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("hint")) {
                    Text(if (session.visibleHints.isEmpty()) "Necesito una pista" else "Otra pista")
                }
                session.visibleHints.forEachIndexed { index, hint -> Notice("Pista ${index + 1}: $hint") }
                if (session.hintsExhausted) Notice("Ya vimos todas las pistas. Vuelve a contar con calma.")
            }
            Progress(session)
        }
        if (state.busy) Text("Guardando…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        state.error?.let {
            Notice(it, true)
            TextButton(onClick = onReload, enabled = !state.busy) { Text("Recuperar mi avance guardado") }
        }
    }
}
