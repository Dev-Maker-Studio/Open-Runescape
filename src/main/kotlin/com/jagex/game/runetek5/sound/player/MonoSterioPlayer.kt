package com.jagex.game.runetek5.sound.player

import com.jagex.game.runetek5.utils.MathUtils
import javax.sound.sampled.*
import javax.sound.sampled.Mixer.Info

class MonoSterioPlayer : PcmPlayer() {

    @JvmField var audioFormat: AudioFormat? = null
    @JvmField var buffer: ByteArray? = null
    @JvmField var hasSoundmax: Boolean = false
    @JvmField var sourceDataLine: SourceDataLine? = null
    @JvmField var bufferLength: Int = 0

    override fun initialize() {
        val mixerInfos = AudioSystem.getMixerInfo()
        if (mixerInfos != null) {
            for (mixerInfo in mixerInfos) {
                if (mixerInfo != null) {
                    val mixerInfoName = mixerInfo.name
                    if (mixerInfoName != null && mixerInfoName.lowercase().indexOf("soundmax") >= 0) {
                        hasSoundmax = true
                    }
                }
            }
        }

        audioFormat = AudioFormat(
            PcmPlayerManager.sampleRate.toFloat(),
            16,
            if (PcmPlayerManager.stereo) 2 else 1,
            true,
            false
        )
        buffer = ByteArray(256 shl if (PcmPlayerManager.stereo) 2 else 1)
    }

    override fun play(bufferLength: Int) {
        try {
            val dataline = DataLine.Info(
                SourceDataLine::class.java,
                audioFormat,
                bufferLength shl if (PcmPlayerManager.stereo) 2 else 1
            )
            sourceDataLine = AudioSystem.getLine(dataline) as SourceDataLine
            sourceDataLine!!.open()
            sourceDataLine!!.start()
            this.bufferLength = bufferLength
        } catch (lineUnavailableException: LineUnavailableException) {
            if (invalid(bufferLength) == 1) {
                sourceDataLine = null
                throw lineUnavailableException
            }

            play(MathUtils.nextPowerOfTwo(bufferLength))
        }
    }

    override fun writeToBuffer() {
        var byteCount = 256
        if (PcmPlayerManager.stereo) {
            byteCount = byteCount shl 1
        }

        for (index in 0 until byteCount) {
            var data = bitpackedSoundData!![index]
            if (data + 8388608 and 0xffffff.inv() != 0) {
                data = 0x7fffff xor (data shr 31)
            }

            buffer!![index * 2] = (data shr 8).toByte()
            buffer!![index * 2 + 1] = (data shr 16).toByte()
        }

        sourceDataLine!!.write(buffer, 0, byteCount shl 1)
    }

    override fun close() {
        if (sourceDataLine != null) {
            sourceDataLine!!.close()
            sourceDataLine = null
        }
    }

    override fun open() {
        sourceDataLine!!.flush()
        if (hasSoundmax) {
            sourceDataLine!!.close()
            sourceDataLine = null
            val dataline = DataLine.Info(
                SourceDataLine::class.java,
                audioFormat,
                bufferLength shl if (PcmPlayerManager.stereo) 2 else 1
            )
            sourceDataLine = AudioSystem.getLine(dataline) as SourceDataLine
            sourceDataLine!!.open()
            sourceDataLine!!.start()
        }
    }

    override fun dataLeft(): Int {
        return bufferLength - (sourceDataLine!!.available() shr if (PcmPlayerManager.stereo) 2 else 1)
    }

    companion object {
        @JvmStatic
        fun invalid(bufferLength: Int): Int {
            var value = bufferLength
            value = (value and 0x55555555) + (value ushr 1 and 0x55555555)
            value = (value ushr 2 and 0x33333333) + (value and 0x33333333)
            value = value + (value ushr 4) and 0xf0f0f0f
            value += value ushr 8
            value += value ushr 16
            return value and 0xff
        }
    }
}
