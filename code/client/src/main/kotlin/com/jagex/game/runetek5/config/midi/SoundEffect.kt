package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.buffer.Packet

/**
 * Sound effect synthesizer.
 *
 * @author Brainpower - 05/02/2023
 */
class SoundEffect(buffer: Packet) {

    @JvmField val instruments: Array<Instrument?> = arrayOfNulls(10)
    @JvmField var loopBegin: Int = 0
    @JvmField var loopEnd: Int = 0

    init {
        for (index in 0 until 10) {
            val hasInstruments = buffer.readUByte()
            if (hasInstruments != 0) {
                buffer.currentPosition--
                instruments[index] = Instrument()
                instruments[index]!!.decodeInstruments(buffer)
            }
        }

        loopBegin = buffer.readUShort()
        loopEnd = buffer.readUShort()
    }

    fun getMixedAudio(): MixedAudioBuffer {
        return MixedAudioBuffer(mix(), loopBegin * 22050 / 1000, loopEnd * 22050 / 1000)
    }

    fun getDelay(): Int {
        var delay = 9999999
        for (index in 0 until 10) {
            if (instruments[index] != null && instruments[index]!!.offset / 20 < delay) {
                delay = instruments[index]!!.offset / 20
            }
        }

        if (loopBegin < loopEnd && loopBegin / 20 < delay) {
            delay = loopBegin / 20
        }

        if (delay != 9999999 && delay != 0) {
            for (index in 0 until 10) {
                if (instruments[index] != null) {
                    instruments[index]!!.offset -= delay * 20
                }
            }

            if (loopBegin < loopEnd) {
                loopBegin -= delay * 20
                loopEnd -= delay * 20
            }

            return delay
        }
        return 0
    }

    fun mix(): ByteArray {
        var duration = 0

        for (i in 0 until 10) {
            if (instruments[i] != null && instruments[i]!!.duration + instruments[i]!!.offset > duration) {
                duration = instruments[i]!!.duration + instruments[i]!!.offset
            }
        }

        if (duration == 0) {
            return ByteArray(0)
        }
        val ns = duration * 22050 / 1000
        val mixed = ByteArray(ns)

        for (i in 0 until 10) {
            if (instruments[i] != null) {
                val mixDuration = instruments[i]!!.duration * 22050 / 1000
                val offset = instruments[i]!!.offset * 22050 / 1000
                val samples = instruments[i]!!.synthesize(mixDuration, instruments[i]!!.duration)
                for (j in 0 until mixDuration) {
                    var out = (samples[j] shr 8) + mixed[j + offset]
                    if ((out + 128 and -256) != 0) {
                        out = out shr 31 xor 0x7f
                    }
                    mixed[j + offset] = out.toByte()
                }
            }
        }
        return mixed
    }
}
