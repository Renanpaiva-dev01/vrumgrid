package com.miaugrid.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

enum class Sound { MARK, PLACE, ENGINE, ERROR, WIN, LOSE, HINT }

/**
 * Efeitos sonoros originais, sintetizados em tempo de execução (sem arquivos de áudio de
 * terceiros). Cada som é gerado uma vez, numa thread própria, e reproduzido com AudioTrack
 * estático. Falhas de áudio nunca interrompem o jogo.
 */
class SoundPlayer {

    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "vrumgrid-audio").apply { isDaemon = true } }
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
        Sound.PLACE -> concat(tone(45, 520.0, 610.0, 0.3, square = true), tone(70, 880.0, 880.0, 0.22))
        Sound.ENGINE -> engine(durationMs = 520, from = 70.0, to = 160.0, volume = 0.32)
        Sound.ERROR -> tone(durationMs = 190, from = 220.0, to = 160.0, volume = 0.35, square = true)
        Sound.HINT -> concat(tone(70, 880.0, 880.0, 0.25), tone(110, 1175.0, 1175.0, 0.25))
        Sound.WIN -> concat(
            tone(110, 523.0, 523.0, 0.3), tone(110, 659.0, 659.0, 0.3),
            tone(110, 784.0, 784.0, 0.3), horn(durationMs = 140), silence(60), horn(durationMs = 320),
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

    /**
     * "Vrum": ronco de motor com a rotação subindo. Uma onda dente-de-serra grave com
     * pulsação de cilindros (modulação de amplitude) e um harmônico para dar corpo.
     */
    private fun engine(durationMs: Int, from: Double, to: Double, volume: Double): ShortArray {
        val n = SAMPLE_RATE * durationMs / 1000
        var phase = 0.0
        var firing = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / n
            val rpm = from + (to - from) * (1 - (1 - t) * (1 - t))
            phase += rpm / SAMPLE_RATE
            firing += 2 * PI * rpm * 0.5 / SAMPLE_RATE
            val saw = 2 * (phase - floor(phase)) - 1
            val pulse = 0.65 + 0.35 * sin(firing)
            val wave = (0.7 * saw + 0.3 * sin(2 * PI * phase * 2)) * pulse
            val fade = min(1.0, i / (SAMPLE_RATE * 0.02)) * min(1.0, (n - i) / (SAMPLE_RATE * 0.08))
            (wave * fade * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Buzina de dois tons (terça maior), com sustentação plana e corte suave. */
    private fun horn(durationMs: Int): ShortArray {
        val n = SAMPLE_RATE * durationMs / 1000
        var a = 0.0
        var b = 0.0
        return ShortArray(n) { i ->
            a += 2 * PI * 440.0 / SAMPLE_RATE
            b += 2 * PI * 554.0 / SAMPLE_RATE
            val square = (if (sin(a) >= 0) 0.5 else -0.5) + (if (sin(b) >= 0) 0.5 else -0.5)
            val fade = min(1.0, i / (SAMPLE_RATE * 0.01)) * min(1.0, (n - i) / (SAMPLE_RATE * 0.03))
            (square * 0.5 * fade * 0.3 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun silence(durationMs: Int) = ShortArray(SAMPLE_RATE * durationMs / 1000)

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
