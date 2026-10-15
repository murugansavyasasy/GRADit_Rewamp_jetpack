package com.vsca.vsnapvoicecollege.ui.dashboard.messages

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.vsca.vsnapvoicecollege.R
import java.io.File
import kotlinx.coroutines.delay

/**
 * Plays one voice clip at a time for a screen: starting another clip stops the
 * current one. Exposes the playing clip and its position for the player bars.
 */
@Stable
class VoicePlaybackController(private val context: Context) {

    /** Key of the clip currently playing, or null. */
    var playingKey by mutableStateOf<String?>(null)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    private var player: MediaPlayer? = null

    fun isPlaying(key: String) = playingKey == key

    /** Plays [clip] under [key], or stops it if that clip is already playing. */
    fun toggle(key: String, clip: VoiceClip) {
        if (playingKey == key) {
            stop()
            return
        }
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(context, clip.uri.toUri())
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            playingKey = key
        } catch (e: Exception) {
            stop()
            Toast.makeText(context, R.string.msg_voice_play_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /** Polls the player position while a clip is playing. */
    internal suspend fun trackPosition() {
        while (playingKey != null) {
            positionMs = player?.currentPosition?.toLong() ?: 0L
            delay(POSITION_POLL_MS)
        }
        positionMs = 0L
    }

    fun stop() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        playingKey = null
        positionMs = 0L
    }

    private companion object {
        const val POSITION_POLL_MS = 100L
    }
}

/** A [VoicePlaybackController] scoped to the calling screen; released when it leaves. */
@Composable
fun rememberVoicePlaybackController(): VoicePlaybackController {
    val context = LocalContext.current
    val controller = remember { VoicePlaybackController(context.applicationContext) }
    LaunchedEffect(controller.playingKey) { controller.trackPosition() }
    DisposableEffect(controller) { onDispose { controller.stop() } }
    return controller
}

/**
 * Records a voice message to an AAC file in the cache directory.
 * The caller must hold the RECORD_AUDIO permission before calling [start].
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Milliseconds recorded so far. */
    val elapsedMs: Long get() = if (isRecording) SystemClock.elapsedRealtime() - startedAt else 0L

    /** Starts recording; returns false if the microphone couldn't be opened. */
    fun start(): Boolean {
        val output = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(64_000)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            file = output
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (e: Exception) {
            newRecorder.release()
            output.delete()
            false
        }
    }

    /** Stops recording and returns the clip, or null if nothing usable was captured. */
    fun stop(): VoiceClip? {
        val active = recorder ?: return null
        val duration = elapsedMs
        val output = file
        recorder = null
        file = null
        val stopped = runCatching { active.stop() }.isSuccess
        active.release()
        if (!stopped || output == null || duration < MIN_CLIP_MS) {
            output?.delete()
            return null
        }
        return VoiceClip(output.toUri().toString(), duration)
    }

    /** Stops and discards any recording in progress. */
    fun cancel() {
        val active = recorder ?: return
        runCatching { active.stop() }
        active.release()
        recorder = null
        file?.delete()
        file = null
    }

    companion object {
        /** Clips shorter than this are treated as accidental taps. */
        private const val MIN_CLIP_MS = 700L

        /** Longest voice message allowed. */
        const val MAX_CLIP_MS = 2 * 60 * 1000L
    }
}

/** "0:12" / "1:05". */
fun formatClipTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** "00:12". */
fun formatClipTimePadded(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
