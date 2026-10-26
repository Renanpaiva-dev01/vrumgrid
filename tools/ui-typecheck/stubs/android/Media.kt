@file:Suppress("unused", "UNUSED_PARAMETER")
package android.media
class AudioAttributes {
    class Builder { fun setUsage(u: Int) = this; fun setContentType(c: Int) = this; fun build() = AudioAttributes() }
    companion object { const val USAGE_GAME = 14; const val CONTENT_TYPE_SONIFICATION = 4 }
}
class AudioFormat {
    class Builder { fun setEncoding(e: Int) = this; fun setSampleRate(r: Int) = this; fun setChannelMask(m: Int) = this; fun build() = AudioFormat() }
    companion object { const val ENCODING_PCM_16BIT = 2; const val CHANNEL_OUT_MONO = 4 }
}
class AudioTrack {
    class Builder {
        fun setAudioAttributes(a: AudioAttributes) = this
        fun setAudioFormat(f: AudioFormat) = this
        fun setTransferMode(m: Int) = this
        fun setBufferSizeInBytes(b: Int) = this
        fun build() = AudioTrack()
    }
    val playState: Int get() = 0
    fun stop() {}
    fun play() {}
    fun reloadStaticData(): Int = 0
    fun write(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int = 0
    companion object { const val MODE_STATIC = 0; const val PLAYSTATE_PLAYING = 3 }
}
