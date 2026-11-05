package com.example.triada.feature.learning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.triada.core.audio.rememberLessonAudio
import com.example.triada.domain.model.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExerciseRenderer(session: LessonSession, enabled: Boolean, onAnswer: (LearningAnswer) -> Unit) {
    val q = session.question
    val audio = rememberLessonAudio("${session.attemptId}/${q.id}")
    q.cueAudioKey?.let { key ->
        OutlinedButton(onClick = { audio.play(key) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("audio-cue")) { Text("Escuchar palabra de referencia") }
    }
    q.illustrationKey?.let { BodyIllustration(it, Modifier.size(112.dp)) }
    when (q.type) {
        ActivityType.COUNT_OBJECTS -> {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat((q.objectCount + 2) / 3) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            repeat(minOf(3, q.objectCount - row * 3)) {
                                Canvas(Modifier.size(44.dp).semantics { contentDescription = "Círculo" }) {
                                    drawCircle(Color(0xFF00A0E9)); drawCircle(Color(0xFF2C2C2C), style = Stroke(2.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
            Text("Elige un número", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                q.options.forEach { choice ->
                    OutlinedButton(onClick = { onAnswer(LearningAnswer.Choice(choice.toString())) }, enabled = enabled,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("answer-$choice")) { Text("$choice", style = MaterialTheme.typography.headlineMedium) }
                }
            }
        }
        ActivityType.SINGLE_CHOICE, ActivityType.SEQUENCE -> {
            if (q.type == ActivityType.SEQUENCE) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    q.sequence.forEachIndexed { index, item -> SymbolCard(item, Modifier.width(92.dp).testTag("sequence-$index")) }
                    Text("?", fontSize = 32.sp, modifier = Modifier.padding(12.dp).semantics { contentDescription = "¿Qué sigue?" })
                }
            }
            q.choices.forEach { option ->
                OutlinedButton(onClick = { onAnswer(LearningAnswer.Choice(option.id)) }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("answer-${option.id}")) {
                    if (option.symbol != null) Text("${option.symbol}  ", fontSize = 28.sp)
                    Text(option.text)
                }
                option.audioKey?.let { key ->
                    TextButton(onClick = { audio.play(key) }, modifier = Modifier.heightIn(min = 48.dp).testTag("audio-${option.id}")) { Text("Escuchar ${option.text}") }
                }
            }
        }
        ActivityType.MATCH_PAIRS -> PairBoard(session, enabled, onAnswer)
        ActivityType.WORD_BUILDING -> WordBoard(session, enabled, onAnswer)
        ActivityType.UNKNOWN -> Notice("Esta actividad todavía no está disponible. Puedes volver a tus lecciones y elegir otra.", true)
    }
    audio.status?.let { Notice(it) }
}

@Composable
private fun SymbolCard(item: ExerciseItem, modifier: Modifier) {
    val color = when (item.color) { "red" -> Color(0xFFB72C35); "blue" -> Color(0xFF005A8A); else -> Color(0xFF886500) }
    Card(modifier) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.symbol ?: "", fontSize = 32.sp, color = color)
            Text(item.text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PairBoard(session: LessonSession, enabled: Boolean, onAnswer: (LearningAnswer) -> Unit) {
    val q = session.question
    var selected by rememberSaveable(session.attemptId, q.id) { mutableIntStateOf(-1) }
    var assigned by rememberSaveable(session.attemptId, q.id) { mutableStateOf(List(q.left.size) { "" }) }
    q.left.forEachIndexed { index, item ->
        OutlinedButton(onClick = { selected = index }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("pair-left-${item.id}")) {
            val target = q.right.firstOrNull { it.id == assigned[index] }?.text
            Text("${item.text}${if (target == null) "" else " → $target"}${if (selected == index) " · Seleccionada" else ""}")
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        q.right.forEach { item ->
            OutlinedButton(onClick = {
                assigned = assigned.mapIndexed { index, previous -> if (index == selected) item.id else if (previous == item.id) "" else previous }
                selected = -1
            }, enabled = enabled && selected >= 0, modifier = Modifier.heightIn(min = 56.dp).testTag("pair-right-${item.id}")) {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.illustrationKey?.let { BodyIllustration(it, Modifier.size(80.dp)) }
                    Text(item.text)
                }
            }
        }
    }
    ActionButton("Comprobar uniones", enabled && assigned.none { it.isBlank() }, "submit-pairs") {
        onAnswer(LearningAnswer.Pairs(q.left.mapIndexed { index, item -> PairSelection(item.id, assigned[index]) }))
    }
    TextButton(onClick = { assigned = List(q.left.size) { "" }; selected = -1 }, enabled = enabled) { Text("Borrar uniones") }
}

@Composable
private fun WordBoard(session: LessonSession, enabled: Boolean, onAnswer: (LearningAnswer) -> Unit) {
    val q = session.question
    var selected by rememberSaveable(session.attemptId, q.id) { mutableStateOf(emptyList<String>()) }
    val text = selected.joinToString(" ") { id -> q.tiles.first { it.id == id }.text }
    Text(text.ifEmpty { "Elige las letras para formar la palabra" }, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("word-answer"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        q.tiles.forEach { tile ->
            OutlinedButton(onClick = { selected = selected + tile.id }, enabled = enabled && tile.id !in selected, modifier = Modifier.heightIn(min = 64.dp).testTag("tile-${tile.id}")) { Text(tile.text, fontSize = 28.sp) }
        }
    }
    TextButton(onClick = { selected = selected.dropLast(1) }, enabled = enabled && selected.isNotEmpty(), modifier = Modifier.testTag("remove-letter")) { Text("Borrar última letra") }
    TextButton(onClick = { selected = emptyList() }, enabled = enabled && selected.isNotEmpty(), modifier = Modifier.testTag("clear-word")) { Text("Empezar la palabra otra vez") }
    ActionButton("Comprobar palabra", enabled && selected.isNotEmpty(), "submit-word") { onAnswer(LearningAnswer.Letters(selected)) }
}

/** Original vector illustrations, packaged in code; no external images or fonts. */
@Composable
fun BodyIllustration(key: String, modifier: Modifier = Modifier) {
    val label = when (key) { "hand" -> "mano"; "foot" -> "pie"; "eye" -> "ojo"; "ear" -> "oreja"; "mouth" -> "boca"; "nose" -> "nariz"; "sun" -> "sol"; else -> "imagen no disponible" }
    Canvas(modifier.semantics { contentDescription = "Ilustración: $label" }) {
        withTransform({ scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) }) {
            val ink = Color(0xFF2C2C2C)
            val skin = Color(0xFFFFD7AD)
            val stroke = Stroke(4f, cap = StrokeCap.Round)
            when (key) {
                "eye" -> { drawOval(Color.White, Offset(5f, 28f), Size(90f, 45f)); drawOval(ink, Offset(5f, 28f), Size(90f, 45f), style = stroke); drawCircle(Color(0xFF00A0E9), 18f, Offset(50f, 50f)); drawCircle(ink, 9f, Offset(50f, 50f)) }
                "ear" -> {
                    val path = Path().apply { moveTo(32f, 78f); cubicTo(6f, 20f, 65f, 3f, 78f, 36f); cubicTo(88f, 58f, 60f, 65f, 54f, 82f); cubicTo(48f, 99f, 28f, 96f, 32f, 78f) }
                    drawPath(path, skin); drawPath(path, ink, style = stroke)
                    drawPath(Path().apply { moveTo(37f, 39f); cubicTo(42f, 22f, 67f, 31f, 61f, 48f); lineTo(45f, 61f) }, ink, style = stroke)
                }
                "mouth" -> { drawOval(Color(0xFFEF7777), Offset(8f, 24f), Size(84f, 50f)); drawOval(ink, Offset(8f, 24f), Size(84f, 50f), style = stroke); drawLine(ink, Offset(12f, 49f), Offset(88f, 49f), 4f) }
                "nose" -> { val path = Path().apply { moveTo(50f, 12f); lineTo(29f, 68f); cubicTo(21f, 86f, 81f, 87f, 73f, 68f); lineTo(62f, 59f) }; drawPath(path, skin); drawPath(path, ink, style = stroke); drawCircle(ink, 4f, Offset(40f, 74f)); drawCircle(ink, 4f, Offset(62f, 74f)) }
                "hand" -> {
                    val path = Path().apply { moveTo(29f, 89f); lineTo(13f, 57f); quadraticTo(5f, 40f, 19f, 45f); lineTo(31f, 57f); lineTo(29f, 18f); quadraticTo(31f, 6f, 39f, 18f); lineTo(42f, 45f); lineTo(44f, 9f); quadraticTo(50f, 0f, 55f, 11f); lineTo(55f, 43f); lineTo(62f, 15f); quadraticTo(69f, 9f, 72f, 20f); lineTo(67f, 47f); lineTo(78f, 31f); quadraticTo(90f, 25f, 87f, 39f); lineTo(77f, 75f); lineTo(65f, 91f); close() }
                    drawPath(path, skin); drawPath(path, ink, style = stroke)
                }
                "foot" -> {
                    val path = Path().apply { moveTo(40f, 12f); lineTo(65f, 12f); lineTo(65f, 54f); cubicTo(65f, 72f, 95f, 62f, 94f, 80f); cubicTo(90f, 96f, 28f, 94f, 22f, 79f); quadraticTo(20f, 68f, 40f, 52f); close() }
                    drawPath(path, skin); drawPath(path, ink, style = stroke)
                    repeat(4) { drawLine(ink, Offset(65f + it * 7f, 75f), Offset(65f + it * 7f, 84f), 2f) }
                }
                "sun" -> {
                    repeat(8) { index -> val angle = index * Math.PI / 4; drawLine(ink, Offset(50f + cos(angle).toFloat() * 33f, 50f + sin(angle).toFloat() * 33f), Offset(50f + cos(angle).toFloat() * 46f, 50f + sin(angle).toFloat() * 46f), 4f) }
                    drawCircle(Color(0xFFFFC107), 25f, Offset(50f, 50f)); drawCircle(ink, 25f, Offset(50f, 50f), style = stroke)
                }
                else -> drawLine(ink, Offset(15f, 15f), Offset(85f, 85f), 4f)
            }
        }
    }
}
