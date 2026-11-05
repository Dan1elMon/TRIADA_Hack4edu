package com.example.triada

import android.media.MediaMetadataRetriever
import androidx.test.platform.app.InstrumentationRegistry
import com.example.triada.core.audio.LocalLessonAudio
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class LocalAudioTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun allBundledWordsArePlayableWaveFiles() {
        listOf("mano", "mesa", "sol", "luna", "sapo", "pato", "lobo").forEach { key ->
            val retriever = MediaMetadataRetriever()
            try {
                context.assets.openFd("audio/$key.wav").use { retriever.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                assertTrue("Audio must contain spoken content: $key", duration > 100)
            } finally { retriever.release() }
        }
    }

    @Test fun nativePlaybackAndMissingResourcesAreHandled() = runBlocking {
        val audio = LocalLessonAudio(context)
        try {
            instrumentation.runOnMainSync { audio.play("mano") }
            withTimeout(10_000) { while (audio.status != "Audio reproducido.") delay(50) }
            instrumentation.runOnMainSync { audio.play("missing") }
            assertTrue(audio.status!!.contains("no está disponible"))
        } finally { instrumentation.runOnMainSync { audio.release() } }
    }
}
