package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.math.sin

object DuoSoundPlayer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var appContextRef: WeakReference<Context>? = null
    var isSoundEnabled: Boolean = true

    fun init(context: Context) {
        appContextRef = WeakReference(context.applicationContext)
    }

    private fun playRawResource(resId: Int, onFallback: (() -> Unit)? = null) {
        if (!isSoundEnabled) return
        val context = appContextRef?.get() ?: run {
            onFallback?.invoke()
            return
        }
        scope.launch {
            try {
                val mp = MediaPlayer.create(context, resId)
                if (mp != null) {
                    mp.setOnCompletionListener {
                        try { it.release() } catch (e: Exception) {}
                    }
                    mp.setOnErrorListener { player, _, _ ->
                        try { player.release() } catch (e: Exception) {}
                        onFallback?.invoke()
                        true
                    }
                    mp.start()
                } else {
                    onFallback?.invoke()
                }
            } catch (e: Exception) {
                onFallback?.invoke()
            }
        }
    }

    fun playClick() {
        if (!isSoundEnabled) return
        // Duo's classic clean/cute interface tap
        scope.launch {
            playTone(
                frequencies = floatArrayOf(600f, 450f),
                durationsMs = intArrayOf(30, 40),
                type = WaveType.SINE,
                volume = 0.5f
            )
        }
    }

    fun playCorrect() {
        playRawResource(com.example.R.raw.duo_correct) {
            // Duo's cheerful success bell fallback (C5 -> E5 -> G5)
            scope.launch {
                playTone(
                    frequencies = floatArrayOf(523.25f, 659.25f, 783.99f),
                    durationsMs = intArrayOf(100, 100, 200),
                    type = WaveType.SINE,
                    volume = 0.6f
                )
            }
        }
    }

    fun playMistake() {
        playRawResource(com.example.R.raw.duo_wrong) {
            // Duo's warning buzzer fallback
            scope.launch {
                playTone(
                    frequencies = floatArrayOf(220f, 180f),
                    durationsMs = intArrayOf(120, 180),
                    type = WaveType.TRIANGLE,
                    volume = 0.5f
                )
            }
        }
    }

    fun playFanfare() {
        playRawResource(com.example.R.raw.duo_lesson_finished) {
            // High-energy fanfare fallback (C5 -> E5 -> G5 -> C6)
            scope.launch {
                playTone(
                    frequencies = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f),
                    durationsMs = intArrayOf(120, 120, 120, 450),
                    type = WaveType.SINE,
                    volume = 0.7f
                )
            }
        }
    }

    enum class WaveType {
        SINE, TRIANGLE
    }

    private fun playTone(
        frequencies: FloatArray,
        durationsMs: IntArray,
        type: WaveType,
        volume: Float
    ) {
        val sampleRate = 44100
        val totalDurationMs = durationsMs.sum()
        val totalSamples = (totalDurationMs * sampleRate) / 1000
        val buffer = ShortArray(totalSamples)

        var sampleIndex = 0
        for (i in frequencies.indices) {
            val freq = frequencies[i]
            val durationMs = durationsMs[i]
            val samplesCount = (durationMs * sampleRate) / 1000

            for (s in 0 until samplesCount) {
                if (sampleIndex >= totalSamples) break
                val t = s.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * freq * t
                val waveVal = when (type) {
                    WaveType.SINE -> sin(angle)
                    WaveType.TRIANGLE -> {
                        // triangle wave: peaks at 1 and troughs at -1
                        2.0 * Math.abs(2.0 * (freq * t - Math.floor(freq * t + 0.5))) - 1.0
                    }
                }

                // apply nice envelope fade in and fade out to avoid clicks
                val fadeRatio = if (s > samplesCount - 441) { // last 10ms
                    (samplesCount - s).toDouble() / 441.0
                } else if (s < 441) { // first 10ms
                    s.toDouble() / 441.0
                } else {
                    1.0
                }

                buffer[sampleIndex] = (waveVal * 32767.0 * volume * fadeRatio).toInt().toShort()
                sampleIndex++
            }
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Wait for completion (plus a tiny safety buffer) before freeing up the resource
            Thread.sleep((totalDurationMs + 50).toLong())
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
