package com.example.triada.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class LocalLessonAudio(private val context: Context) {
    var status: String? by mutableStateOf(null)
        private set
    private var player: MediaPlayer? = null

    fun play(key: String, speed:Float=1f) {
        release()
        if (!key.matches(Regex("[a-z0-9-]{1,80}"))) { status = "Este audio no está disponible. Puedes seguir con la actividad."; return }
        val next = MediaPlayer()
        player = next
        try {
            next.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            try { context.assets.openFd("audio/$key.wav").use { next.setDataSource(it.fileDescriptor, it.startOffset, it.length) } }
            catch (_: java.io.IOException) { next.setDataSource(java.io.File(context.filesDir, "content/audio/$key.wav").absolutePath) }
            next.setOnPreparedListener { if (player === it) {
                try { it.playbackParams=it.playbackParams.setSpeed(speed.coerceIn(.5f,1.5f));it.start();status="Escuchando…" }
                catch (_:Exception){status="No pudimos iniciar el audio. Puedes volver a intentarlo.";release()}
            } }
            next.setOnCompletionListener { status = "Audio reproducido."; release() }
            next.setOnErrorListener { _, _, _ -> status = "No pudimos reproducir este audio. Puedes volver a intentarlo."; release(); true }
            status = "Preparando audio…"
            next.prepareAsync()
        } catch (_: Exception) {
            status = "Este audio no está disponible. Puedes seguir con la actividad."
            release()
        }
    }

    fun release() {
        player?.let { it.setOnPreparedListener(null); it.setOnCompletionListener(null); it.setOnErrorListener(null); it.release() }
        player = null
    }
}

@Composable
fun rememberLessonAudio(key: String): LocalLessonAudio {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val audio = remember(key) { LocalLessonAudio(context) }
    DisposableEffect(audio, owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) audio.release() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); audio.release() }
    }
    return audio
}
