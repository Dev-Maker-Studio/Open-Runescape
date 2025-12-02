package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.buffer.Packet
import java.util.Arrays
import java.util.Random
import kotlin.math.sin

class Instrument {

    internal var pitch: Envelope? = null
    internal var volume: Envelope? = null
    internal var pitchModifier: Envelope? = null
    internal var pitchModifierAmplitude: Envelope? = null
    internal var volumeMultiplier: Envelope? = null
    internal var volumeAmplitude: Envelope? = null
    internal var release: Envelope? = null
    internal var attack: Envelope? = null
    internal var delayTime: Int = 0
    internal var delayDecay: Int = 100
    internal var duration: Int = 500
    internal var offset: Int = 0
    internal var filter: Filter? = null
    internal var filterEnvelope: Envelope? = null
    val oscillatorVolume: IntArray = IntArray(5)
    val oscillatorPitch: IntArray = IntArray(5)
    val oscillatorDelays: IntArray = IntArray(5)

    fun synthesize(mixDuration: Int, instrDuration: Int): IntArray {
        Arrays.fill(OUTPUT, 0, mixDuration, 0)
        if (instrDuration >= 10) {
            val fs = mixDuration / (instrDuration + 0.0)
            this.pitch!!.reset()
            this.volume!!.reset()
            var pitchModStep = 0
            var pitchModBaseStep = 0
            var pitchModPhase = 0
            if (this.pitchModifier != null) {
                this.pitchModifier!!.reset()
                this.pitchModifierAmplitude!!.reset()
                pitchModStep = ((this.pitchModifier!!.end - this.pitchModifier!!.start) * 32.768 / fs).toInt()
                pitchModBaseStep = (this.pitchModifier!!.start * 32.768 / fs).toInt()
            }

            var volumeModStep = 0
            var volumeModBaseStep = 0
            var volumeModPhase = 0
            if (this.volumeMultiplier != null) {
                this.volumeMultiplier!!.reset()
                this.volumeAmplitude!!.reset()
                volumeModStep = ((this.volumeMultiplier!!.end - this.volumeMultiplier!!.start) * 32.768 / fs).toInt()
                volumeModBaseStep = (this.volumeMultiplier!!.start * 32.768 / fs).toInt()
            }

            for (i in 0..4) {
                if (this.oscillatorVolume[i] != 0) {
                    PHASES[i] = 0
                    DELAYS[i] = (this.oscillatorDelays[i] * fs).toInt()
                    VOLUME_STEP[i] = (this.oscillatorVolume[i] shl 14) / 100
                    PITCH_STEP[i] = ((this.pitch!!.end - this.pitch!!.start) * 32.768 * Math.pow(1.0057929410678534, this.oscillatorPitch[i].toDouble()) / fs).toInt()
                    PITCH_BASE_STEP[i] = (this.pitch!!.start * 32.768 / fs).toInt()
                }
            }

            for (i in 0 until mixDuration) {
                var pitchChange = this.pitch!!.step(mixDuration)
                var volumeChange = this.volume!!.step(mixDuration)
                if (this.pitchModifier != null) {
                    val mod = this.pitchModifier!!.step(mixDuration)
                    val modAmplitude = this.pitchModifierAmplitude!!.step(mixDuration)
                    pitchChange += evaluateWave(pitchModPhase, modAmplitude, this.pitchModifier!!.form) shr 1
                    pitchModPhase += pitchModBaseStep + (mod * pitchModStep shr 16)
                }

                if (this.volumeMultiplier != null) {
                    val mod = this.volumeMultiplier!!.step(mixDuration)
                    val modAmplitude = this.volumeAmplitude!!.step(mixDuration)
                    volumeChange = volumeChange * ((evaluateWave(volumeModPhase, modAmplitude, this.volumeMultiplier!!.form) shr 1) + 32768) shr 15
                    volumeModPhase += volumeModBaseStep + (mod * volumeModStep shr 16)
                }

                for (j in 0..4) {
                    if (this.oscillatorVolume[j] != 0) {
                        val i_15 = DELAYS[j] + i
                        if (i_15 < mixDuration) {
                            OUTPUT[i_15] += evaluateWave(PHASES[j], volumeChange * VOLUME_STEP[j] shr 15, this.pitch!!.form)
                            PHASES[j] += (pitchChange * PITCH_STEP[j] shr 16) + PITCH_BASE_STEP[j]
                        }
                    }
                }
            }

            if (this.release != null) {
                this.release!!.reset()
                this.attack!!.reset()
                var counter = 0
                var muted = true

                for (i in 0 until mixDuration) {
                    val onStep = this.release!!.step(mixDuration)
                    val offStep = this.attack!!.step(mixDuration)
                    val threshold: Int = if (muted) {
                        (onStep * (this.release!!.end - this.release!!.start) shr 8) + this.release!!.start
                    } else {
                        (offStep * (this.release!!.end - this.release!!.start) shr 8) + this.release!!.start
                    }

                    counter += 256
                    if (counter >= threshold) {
                        counter = 0
                        muted = !muted
                    }

                    if (muted) {
                        OUTPUT[i] = 0
                    }
                }
            }

            if (this.delayTime > 0 && this.delayDecay > 0) {
                val delay = (this.delayTime * fs).toInt()

                for (i in delay until mixDuration) {
                    OUTPUT[i] += OUTPUT[i - delay] * this.delayDecay / 100
                }
            }

            if (this.filter!!.numPairs[0] > 0 || this.filter!!.numPairs[1] > 0) {
                this.filterEnvelope!!.reset()
                var t = this.filterEnvelope!!.step(mixDuration + 1)
                var M = this.filter!!.compute(0, t / 65536.0f)
                var N = this.filter!!.compute(1, t / 65536.0f)
                if (mixDuration >= M + N) {
                    var n = 0
                    var delay = N
                    if (N > mixDuration - M) {
                        delay = mixDuration - M
                    }

                    while (n < delay) {
                        var y = (OUTPUT[n + M].toLong() * Filter.invUnityInt shr 16).toInt()

                        for (i_17 in 0 until M) {
                            y += (OUTPUT[n + M - 1 - i_17].toLong() * Filter.COEFFICIENT_INT[0][i_17] shr 16).toInt()
                        }

                        for (i_17 in 0 until n) {
                            y -= (OUTPUT[n - 1 - i_17].toLong() * Filter.COEFFICIENT_INT[1][i_17] shr 16).toInt()
                        }

                        OUTPUT[n] = y
                        t = this.filterEnvelope!!.step(mixDuration + 1)
                        ++n
                    }

                    delay = 128

                    while (true) {
                        if (delay > mixDuration - M) {
                            delay = mixDuration - M
                        }

                        while (n < delay) {
                            var y = (OUTPUT[n + M].toLong() * Filter.invUnityInt shr 16).toInt()

                            for (i in 0 until M) {
                                y += (OUTPUT[n + M - 1 - i].toLong() * Filter.COEFFICIENT_INT[0][i] shr 16).toInt()
                            }

                            for (i in 0 until N) {
                                y -= (OUTPUT[n - 1 - i].toLong() * Filter.COEFFICIENT_INT[1][i] shr 16).toInt()
                            }

                            OUTPUT[n] = y
                            t = this.filterEnvelope!!.step(mixDuration + 1)
                            ++n
                        }

                        if (n >= mixDuration - M) {
                            while (n < mixDuration) {
                                var y = 0

                                for (i in n + M - mixDuration until M) {
                                    y += (OUTPUT[n + M - 1 - i].toLong() * Filter.COEFFICIENT_INT[0][i] shr 16).toInt()
                                }

                                for (i in 0 until N) {
                                    y -= (OUTPUT[n - 1 - i].toLong() * Filter.COEFFICIENT_INT[1][i] shr 16).toInt()
                                }

                                OUTPUT[n] = y
                                this.filterEnvelope!!.step(mixDuration + 1)
                                ++n
                            }
                            break
                        }

                        M = this.filter!!.compute(0, t / 65536.0f)
                        N = this.filter!!.compute(1, t / 65536.0f)
                        delay += 128
                    }
                }
            }

            for (i in 0 until mixDuration) {
                if (OUTPUT[i] < -32768) {
                    OUTPUT[i] = -32768
                }

                if (OUTPUT[i] > 32767) {
                    OUTPUT[i] = 32767
                }
            }

        }
        return OUTPUT
    }

    fun evaluateWave(table: Int, phase: Int, amplitude: Int): Int {
        return when (amplitude) {
            1 -> if ((table and 0x7fff) < 16384) phase else -phase
            2 -> SINE[table and 0x7fff] * phase shr 14
            3 -> (phase * (table and 0x7fff) shr 14) - phase
            4 -> phase * NOISE[table / 2607 and 0x7fff]
            else -> 0
        }
    }

    fun decodeInstruments(buffer: Packet) {
        this.pitch = Envelope()
        this.pitch!!.decode(buffer)
        this.volume = Envelope()
        this.volume!!.decode(buffer)
        var option = buffer.readUByte()
        if (option != 0) {
            buffer.currentPosition--
            this.pitchModifier = Envelope()
            this.pitchModifier!!.decode(buffer)
            this.pitchModifierAmplitude = Envelope()
            this.pitchModifierAmplitude!!.decode(buffer)
        }

        option = buffer.readUByte()
        if (option != 0) {
            buffer.currentPosition--
            this.volumeMultiplier = Envelope()
            this.volumeMultiplier!!.decode(buffer)
            this.volumeAmplitude = Envelope()
            this.volumeAmplitude!!.decode(buffer)
        }

        option = buffer.readUByte()
        if (option != 0) {
            buffer.currentPosition--
            this.release = Envelope()
            this.release!!.decode(buffer)
            this.attack = Envelope()
            this.attack!!.decode(buffer)
        }

        for (i in 0..9) {
            val volume = buffer.readUSmart()
            if (volume == 0) {
                break
            }

            this.oscillatorVolume[i] = volume
            this.oscillatorPitch[i] = buffer.readSmart()
            this.oscillatorDelays[i] = buffer.readUSmart()
        }

        this.delayTime = buffer.readUSmart()
        this.delayDecay = buffer.readUSmart()
        this.duration = buffer.readUShort()
        this.offset = buffer.readUShort()
        this.filter = Filter()
        this.filterEnvelope = Envelope()
        this.filter!!.decode(buffer, this.filterEnvelope!!)
    }

    companion object {
        @JvmField
        val NOISE = IntArray(32768)

        @JvmField
        val SINE: IntArray

        @JvmField
        val OUTPUT: IntArray

        @JvmField
        val PHASES: IntArray

        @JvmField
        val DELAYS: IntArray

        @JvmField
        val VOLUME_STEP: IntArray

        @JvmField
        val PITCH_STEP: IntArray

        @JvmField
        val PITCH_BASE_STEP: IntArray

        init {
            val rand = Random(0L)
            for (i in 0..32767) {
                NOISE[i] = (rand.nextInt() and 0x2) - 1
            }
            SINE = IntArray(32768)
            for (i in 0..32767) {
                SINE[i] = (sin(i / 5215.1903) * 16384.0).toInt()
            }
            OUTPUT = IntArray(220500)
            PHASES = IntArray(5)
            DELAYS = IntArray(5)
            VOLUME_STEP = IntArray(5)
            PITCH_STEP = IntArray(5)
            PITCH_BASE_STEP = IntArray(5)
        }
    }
}
