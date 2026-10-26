package com.miaugrid.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class Sound { MARK, PLACE, ERROR, WIN, LOSE, HINT }

/**
 * Efeitos sonoros originais, sintetizados em tempo de execução (sem arquivos de áudio de
 * terceiros). Cada som é gerado uma vez, numa thread própria, e reproduzido com AudioTrack
 * estático. Falhas de áudio nunca interrompem o jogo.
 */
class SoundPlayer {

    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "miaugrid-audio").apply { isDaemon = true } }
    private val tracks = HashMap<Sound, AudioTrack>()

    @Volatile
    var enabled: Boolean = true

    init {
        executor.execute { Sound.entries.forEach { sound -> runCatching { tracks[sound] = build(synthesize(sound)) } } }
    }

    fun play(sound: Sound) {
        if (!enabled) return
        executor.execute {
            val track = tracks[sound] ?: return@execute
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                track.reloadStaticData()
                track.play()
            } catch (e: IllegalStateException) {
                Log.w("MiauGridAudio", "Falha ao tocar $sound", e)
            }
        }
    }

    private fun build(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }

    private fun synthesize(sound: Sound): ShortArray = when (sound) {
        Sound.MARK -> tone(durationMs = 35, from = 1400.0, to = 1200.0, volume = 0.25)
        Sound.PLACE -> meow(durationMs = 190, base = 780.0, volume = 0.35)
        Sound.ERROR -> tone(durationMs = 190, from = 220.0, to = 160.0, volume = 0.35, square = true)
        Sound.HINT -> concat(tone(70, 880.0, 880.0, 0.25), tone(110, 1175.0, 1175.0, 0.25))
        Sound.WIN -> concat(
            tone(110, 523.0, 523.0, 0.3), tone(110, 659.0, 659.0, 0.3),
            tone(110, 784.0, 784.0, 0.3), meow(260, 1046.0, 0.35),
        )
        Sound.LOSE -> concat(tone(160, 392.0, 392.0, 0.3), tone(160, 330.0, 330.0, 0.3), tone(260, 262.0, 240.0, 0.3))
    }

    /** Tom com glissando linear e envelope de ataque/decaimento. */
    private fun tone(durationMs: Int, from: Double, to: Double, volume: Double, square: Boolean = false): ShortArray {
        val n = SAMPLE_RATE * durationMs / 1000
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / n
            val freq = from + (to - from) * t
            phase += 2 * PI * freq / SAMPLE_RATE
            val wave = if (square) (if (sin(phase) >= 0) 0.6 else -0.6) else sin(phase)
            (wave * envelope(i, n) * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** "Miau" estilizado: sobe e desce de altura com vibrato e um harmônico. */
    private fun meow(durationMs: Int, base: Double, volume: Double): ShortArray {
        val n = SAMPLE_RATE * durationMs / 1000
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / n
            val contour = 1.0 + 0.35 * sin(PI * t) - 0.15 * t
            val vibrato = 1.0 + 0.02 * sin(2 * PI * 30 * t)
            phase += 2 * PI * base * contour * vibrato / SAMPLE_RATE
            val wave = 0.75 * sin(phase) + 0.25 * sin(2 * phase)
            (wave * envelope(i, n) * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun envelope(i: Int, n: Int): Double {
        val attack = min(1.0, i / (SAMPLE_RATE * 0.005))
        val release = exp(-3.0 * i / n)
        return attack * release
    }

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var offset = 0
        for (p in parts) { p.copyInto(out, offset); offset += p.size }
        return out
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
    }
}
