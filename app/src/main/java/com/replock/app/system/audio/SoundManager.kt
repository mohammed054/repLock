package com.replock.app.system.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class SoundManager {

    private var clickTrack: AudioTrack? = null

    fun playRepComplete() {
        try {
            val sampleRate = 44100
            val duration = 0.08f
            val samples = (sampleRate * duration).toInt()
            val buffer = ShortArray(samples)

            val frequency = 880.0
            val frequency2 = 1320.0

            for (i in 0 until samples) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / samples)
                val envelope2 = 1.0 - (i.toDouble() / samples)

                val sample1 = Math.sin(2.0 * Math.PI * frequency * t) * envelope
                val sample2 = Math.sin(2.0 * Math.PI * frequency2 * t) * envelope2 * 0.5

                val combined = (sample1 + sample2) * 0.7
                buffer[i] = (combined * Short.MAX_VALUE).toInt().toShort()
            }

            clickTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
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

            clickTrack?.write(buffer, 0, buffer.size)
            clickTrack?.play()

            clickTrack?.setOnCompletionListener {
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        clickTrack?.stop()
        clickTrack?.release()
        clickTrack = null
    }
}