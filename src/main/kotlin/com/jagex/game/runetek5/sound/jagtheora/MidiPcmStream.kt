package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.config.midi.SoundBankCache
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.sound.player.PcmPlayerManager
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

open class MidiPcmStream : PcmStream {

    @JvmField var queuedTrackTick: Int = 0
    @JvmField var queuedAllSoundOff: Boolean = false
    @JvmField var musicVolume: Int = 256
    @JvmField var volumeMultiplier: Int = 256
    @JvmField val ticks: Int = 1000000
    @JvmField val channelVolume: IntArray = IntArray(16)
    @JvmField val volume: IntArray = IntArray(16)
    @JvmField val pan: IntArray = IntArray(16)
    @JvmField val expression: IntArray = IntArray(16)
    @JvmField val defaultProgram: IntArray = IntArray(16)
    @JvmField val programChange: IntArray = IntArray(16)
    @JvmField val bankSelect: IntArray = IntArray(16)
    @JvmField val pitchBend: IntArray = IntArray(16)
    @JvmField val modulation: IntArray = IntArray(16)
    @JvmField val portamento: IntArray = IntArray(16)
    @JvmField val sustain: IntArray = IntArray(16)
    @JvmField val nonRegisteredParameter: IntArray = IntArray(16)
    @JvmField val dataEntry: IntArray = IntArray(16)
    @JvmField val generalPurpose1: IntArray = IntArray(16)
    @JvmField val effectSpeedRaw: IntArray = IntArray(16)
    @JvmField val effectSpeedScaled: IntArray = IntArray(16)
    @JvmField val activeNotes: Array<Array<MusicPatchNode?>> = Array(16) { arrayOfNulls(128) }
    @JvmField val loopGroupNotes: Array<Array<MusicPatchNode?>> = Array(16) { arrayOfNulls(128) }
    @JvmField val fileReader: MidiFileReader = MidiFileReader()
    @JvmField val patchStream: MusicPatchPcmStream = MusicPatchPcmStream(this)
    @JvmField val musicPatches: IterableNodeMap<MusicPatch>
    @JvmField var currentTickTime: Long = 0
    @JvmField var queuedTrack: MusicTrack? = null
    @JvmField var prioritizedTrack: Int = 0
    @JvmField var prioritizedTrackLength: Int = 0
    @JvmField var nextEventTickTime: Long = 0
    @JvmField var isLooping: Boolean = false

    constructor(stream: MidiPcmStream) {
        this.musicPatches = stream.musicPatches
        setChannelVolume(-1, 256)
        sendSystemResetMessage(true)
    }

    constructor() {
        this.musicPatches = IterableNodeMap<MusicPatch>(128)
        setChannelVolume(-1, 256)
        sendSystemResetMessage(true)
    }

    fun setPitchBend(index: Int, value: Int) {
        this.pitchBend[index] = value
    }

    @Synchronized
    fun setVolumeMultiplier(volumeMultiplier: Int) {
        this.volumeMultiplier = volumeMultiplier
    }

    @Synchronized
    fun setChannelVolume(i_1: Int, i_2: Int) {
        if (i_1 < 0) {
            for (i_4 in 0..15) {
                this.channelVolume[i_4] = i_2
            }
        } else {
            this.channelVolume[i_1] = i_2
        }
    }

    @Synchronized
    override fun fill(ints_1: IntArray, i_2: Int, i_3: Int) {
        var offset = i_2
        var length = i_3
        if (this.fileReader.dataLoaded()) {
            val i_4 = this.fileReader.division * this.ticks / PcmPlayerManager.sampleRate

            do {
                val long_5 = (i_4.toLong()) * length + this.currentTickTime
                if (this.nextEventTickTime - long_5 >= 0L) {
                    this.currentTickTime = long_5
                    break
                }

                val i_7 = ((this.nextEventTickTime - this.currentTickTime + i_4 - 1L) / i_4).toInt()
                this.currentTickTime += (i_7.toLong()) * i_4
                this.patchStream.fill(ints_1, offset, i_7)
                offset += i_7
                length -= i_7
                processNextMidiEvents()
            } while (this.fileReader.dataLoaded())
        }

        this.patchStream.fill(ints_1, offset, length)
    }

    @Synchronized
    fun resetMusicPatches() {
        for (musicPatch in this.musicPatches) {
            musicPatch.reset()
        }
    }

    @Synchronized
    fun unlinkMusicPatches() {
        for (musicPatch in this.musicPatches) {
            musicPatch.unlink()
        }
    }

    @Synchronized
    fun setMusicTrack(class282Sub71: MusicTrack, bool2: Boolean) {
        loadAndPlayTrack(class282Sub71, bool2, true)
    }

    fun applyInitialSampleOffset(patchNode: MusicPatchNode, bool2: Boolean) {
        val audioFile = patchNode.audioFile ?: return
        val stream = patchNode.stream ?: return
        var samples = audioFile.getTotalSamples()
        val i_5: Int
        if (bool2 && audioFile.enableLoop) {
            val i_6 = samples + samples - audioFile.start
            i_5 = ((this.generalPurpose1[patchNode.currentTrack].toLong()) * i_6 shr 6).toInt()
            samples = samples shl 8
            if (i_5 >= samples) {
                stream.reversePlaybackDirection()
            }
        } else {
            i_5 = ((this.generalPurpose1[patchNode.currentTrack].toLong()) * samples shr 6).toInt()
        }

        stream.setPlaybackPosition(i_5)
    }

    @Synchronized
    fun setMusicTrackAtPosition(class282Sub71: MusicTrack, bool2: Boolean, long_4: Long) {
        loadAndPlayTrack(class282Sub71, bool2, true)
        advanceToTick(this.fileReader.division * long_4)
    }

    @Synchronized
    fun turnOffSound() {
        clearTrack(true)
    }

    @Synchronized
    fun clearTrack(bool1: Boolean) {
        this.fileReader.clear()
        this.queuedTrack = null
        sendSystemResetMessage(bool1)
    }

    @Synchronized
    fun dataLoaded(): Boolean {
        return this.fileReader.dataLoaded()
    }

    fun setEffectSpeed(i_1: Int, i_2: Int) {
        this.effectSpeedRaw[i_1] = i_2
        this.effectSpeedScaled[i_1] = (2097152.0 * (2.0).pow((i_2.toDouble()) * (5.4931640625E-4)) + 0.5).toInt()
    }

    fun initDrumChannel() {
        this.defaultProgram[9] = 128
        this.bankSelect[9] = 128
        setProgramChange(9, 128)
    }

    fun setNoteOn(channel: Int, note: Int, velocity: Int) {
        setNoteOff(channel, note)
        if ((this.sustain[channel] and 0x2) != 0) {
            var patchNode: MusicPatchNode? = this.patchStream.queue.first() as? MusicPatchNode
            while (patchNode != null) {
                if (patchNode.currentTrack == channel && patchNode.releaseState < 0) {
                    this.activeNotes[channel][patchNode.currentNotePitch] = null
                    this.activeNotes[channel][note] = patchNode
                    val i_9 = (patchNode.portamentoFactor * patchNode.portamentoPitchOffset shr 12) + patchNode.notePitchAdjustment
                    patchNode.notePitchAdjustment += (note - patchNode.currentNotePitch) shl 8
                    patchNode.portamentoPitchOffset = i_9 - patchNode.notePitchAdjustment
                    patchNode.portamentoFactor = 4096
                    patchNode.currentNotePitch = note
                    return
                }
                patchNode = this.patchStream.queue.getNext() as? MusicPatchNode
            }
        }

        val musicPatch = this.musicPatches.get(this.programChange[channel].toLong())
        if (musicPatch != null) {
            val audioFile = musicPatch.audioFiles[note]
            if (audioFile != null) {
                val musicPatchNode = MusicPatchNode()
                musicPatchNode.currentTrack = channel
                musicPatchNode.patch = musicPatch
                musicPatchNode.audioFile = audioFile
                musicPatchNode.musicPatchInfo = musicPatch.musicPatchNodes2[note]
                musicPatchNode.loopVariable = musicPatch.loopMode[note].toInt()
                musicPatchNode.currentNotePitch = note
                musicPatchNode.maxVolumeLevel = velocity * velocity * musicPatch.volumeOffset[note] * musicPatch.baseVelocity + 1024 shr 11
                musicPatchNode.currentPanValue = (musicPatch.panOffset[note].toInt()) and 0xff
                musicPatchNode.notePitchAdjustment = (note shl 8) - ((musicPatch.pitchOffset[note].toInt()) and 0x7fff)
                musicPatchNode.volumeEnvelopeTime = 0
                musicPatchNode.volumeEnvelopePosition = 0
                musicPatchNode.volumeEnvelopeIndex = 0
                musicPatchNode.releaseState = -1
                musicPatchNode.releaseEnvelopeIndex = 0
                if (this.generalPurpose1[channel] == 0) {
                    musicPatchNode.stream = audioFile.getStream(calculatePitch(musicPatchNode), calculateVolume(musicPatchNode), calculatePan(musicPatchNode))
                } else {
                    musicPatchNode.stream = audioFile.getStream(calculatePitch(musicPatchNode), 0, calculatePan(musicPatchNode))
                    applyInitialSampleOffset(musicPatchNode, musicPatch.pitchOffset[note] < 0)
                }

                if (musicPatch.pitchOffset[note] < 0) {
                    musicPatchNode.stream?.setNumLoops(-1)
                }

                if (musicPatchNode.loopVariable >= 0) {
                    val patchNode = this.loopGroupNotes[channel][musicPatchNode.loopVariable]
                    if (patchNode != null && patchNode.releaseState < 0) {
                        this.activeNotes[channel][patchNode.currentNotePitch] = null
                        patchNode.releaseState = 0
                    }

                    this.loopGroupNotes[channel][musicPatchNode.loopVariable] = musicPatchNode
                }

                this.patchStream.queue.addFirst(musicPatchNode)
                this.activeNotes[channel][note] = musicPatchNode
            }
        }
    }

    fun calculateVolume(class282Sub131: MusicPatchNode): Int {
        if (this.channelVolume[class282Sub131.currentTrack] == 0) {
            return 0
        } else {
            val class1103 = class282Sub131.musicPatchInfo ?: return 0
            var i_4 = this.expression[class282Sub131.currentTrack] * this.volume[class282Sub131.currentTrack] + 4096 shr 13
            i_4 = i_4 * i_4 + 16384 shr 15
            i_4 = i_4 * class282Sub131.maxVolumeLevel + 16384 shr 15
            i_4 = i_4 * this.musicVolume + 128 shr 8
            i_4 = i_4 * this.volumeMultiplier shr 8
            i_4 = i_4 * this.channelVolume[class282Sub131.currentTrack] + 128 shr 8
            if (class1103.volumeEnvelopeDecay > 0) {
                i_4 = (i_4 * (0.5).pow((class282Sub131.volumeEnvelopeTime.toDouble()) * (1.953125E-5) * (class1103.volumeEnvelopeDecay.toDouble())) + 0.5).toInt()
            }

            var i_5: Int
            var i_6: Int
            var i_7: Int
            var i_8: Int
            class1103.volumeEnvelopeData?.let {
                i_5 = class282Sub131.volumeEnvelopePosition
                i_6 = (it[class282Sub131.volumeEnvelopeIndex + 1].toInt() and 0xff)
                if (class282Sub131.volumeEnvelopeIndex < it.size - 2) {
                    i_7 = (it[class282Sub131.volumeEnvelopeIndex].toInt() and 0xff) shl 8
                    i_8 = (it[class282Sub131.volumeEnvelopeIndex + 2].toInt() and 0xff) shl 8
                    i_6 += (i_5 - i_7) * (it[class282Sub131.volumeEnvelopeIndex + 3].toInt() - i_6) / (i_8 - i_7)
                }

                i_4 = i_4 * i_6 + 32 shr 6
            }

            class1103.releaseEnvelopeData?.let {
                if (class282Sub131.releaseState > 0) {
                    i_5 = class282Sub131.releaseState
                    i_6 = (it[class282Sub131.releaseEnvelopeIndex + 1].toInt() and 0xff)
                    if (class282Sub131.releaseEnvelopeIndex < it.size - 2) {
                        i_7 = (it[class282Sub131.releaseEnvelopeIndex].toInt() and 0xff) shl 8
                        i_8 = (it[class282Sub131.releaseEnvelopeIndex + 2].toInt() and 0xff) shl 8
                        i_6 += (it[class282Sub131.releaseEnvelopeIndex + 3].toInt() - i_6) * (i_5 - i_7) / (i_8 - i_7)
                    }

                    i_4 = i_6 * i_4 + 32 shr 6
                }
            }

            return i_4
        }
    }

    fun setNoteOff(channel: Int, i_2: Int) {
        val patch1 = this.activeNotes[channel][i_2]
        if (patch1 != null) {
            this.activeNotes[channel][i_2] = null
            if ((this.sustain[channel] and 0x2) != 0) {
                var streamedPatch: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
                while (streamedPatch != null) {
                    if (patch1.currentTrack == streamedPatch.currentTrack && streamedPatch.releaseState < 0 && streamedPatch != patch1) {
                        patch1.releaseState = 0
                        break
                    }
                    streamedPatch = this.patchStream.queue.next() as? MusicPatchNode
                }
            } else {
                patch1.releaseState = 0
            }
        }
    }

    fun turnAllSoundOff(i_1: Int) {
        var class282Sub133: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
        while (class282Sub133 != null) {
            if (i_1 < 0 || class282Sub133.currentTrack == i_1) {
                class282Sub133.stream?.let { stream ->
                    stream.fadeOutAndStop(PcmPlayerManager.sampleRate / 100)
                    if (stream.isFading()) {
                        this.patchStream.fadeOutScheduler.addSubStream(stream)
                    }
                }

                class282Sub133.clearMusicPatchNode()

                if (class282Sub133.releaseState < 0) {
                    this.activeNotes[class282Sub133.currentTrack][class282Sub133.currentNotePitch] = null
                }

                class282Sub133.unlink()
            }
            class282Sub133 = this.patchStream.queue.next() as? MusicPatchNode
        }
    }

    fun resetAllControllers(i_1: Int) {
        var idx = i_1
        if (idx < 0) {
            for (i_1 in 0..15) {
                resetAllControllers(i_1)
            }
        } else {
            this.volume[idx] = 12800
            this.pan[idx] = 8192
            this.expression[idx] = 16383
            this.pitchBend[idx] = 8192
            this.modulation[idx] = 0
            this.portamento[idx] = 8192
            resetSustainPedal(idx)
            resetEffectSwitch(idx, 41.toByte())
            this.sustain[idx] = 0
            this.nonRegisteredParameter[idx] = 32767
            this.dataEntry[idx] = 256
            this.generalPurpose1[idx] = 0
            setEffectSpeed(idx, 8192)
        }
    }

    fun isPatchNodeDead(class282Sub131: MusicPatchNode): Boolean {
        if (class282Sub131.stream == null) {
            if (class282Sub131.releaseState >= 0) {
                class282Sub131.unlink()
                if (class282Sub131.loopVariable > 0 && class282Sub131 == this.loopGroupNotes[class282Sub131.currentTrack][class282Sub131.loopVariable]) {
                    this.loopGroupNotes[class282Sub131.currentTrack][class282Sub131.loopVariable] = null
                }
            }

            return true
        } else {
            return false
        }
    }

    fun sendSystemResetMessage(allSoundOff: Boolean) {
        if (allSoundOff) {
            turnAllSoundOff(-1)
        } else {
            turnAllNotesOff(-1)
        }

        resetAllControllers(-1)

        this.defaultProgram.copyInto(this.programChange, 0, 0, 16)

        for (index in 0..15) {
            this.bankSelect[index] = this.defaultProgram[index] and -128
        }
    }

    fun resetSustainPedal(index: Int) {
        if ((this.sustain[index] and 0x2) != 0) {
            var musicPatch: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
            while (musicPatch != null) {
                if (musicPatch.currentTrack == index && this.activeNotes[index][musicPatch.currentNotePitch] == null && musicPatch.releaseState < 0) {
                    musicPatch.releaseState = 0
                }
                musicPatch = this.patchStream.queue.next() as? MusicPatchNode
            }
        }
    }

    fun resetEffectSwitch(index: Int, b_2: Byte) {
        if ((this.sustain[index] and 0x4) != 0 && b_2 > 1) {
            var musicPatch: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
            while (musicPatch != null) {
                if (musicPatch.currentTrack == index) {
                    musicPatch.retriggerPhase = 0
                }
                musicPatch = this.patchStream.queue.next() as? MusicPatchNode
            }
        }
    }

    fun parseMessage(bitpackedMidiMessage: Int) {
        val message = bitpackedMidiMessage and 0xf0
        var channel: Int
        var data1: Int
        var data2: Int
        if (message == 128) {
            channel = bitpackedMidiMessage and 0xf
            data1 = bitpackedMidiMessage shr 8 and 0x7f
            data2 = bitpackedMidiMessage shr 16 and 0x7f
            setNoteOff(channel, data1)
        } else if (message == 144) {
            channel = bitpackedMidiMessage and 0xf
            data1 = bitpackedMidiMessage shr 8 and 0x7f
            data2 = bitpackedMidiMessage shr 16 and 0x7f
            if (data2 > 0) {
                setNoteOn(channel, data1, data2)
            } else {
                setNoteOff(channel, data1)
            }
        } else if (message == 160) {
            channel = bitpackedMidiMessage and 0xf
            data1 = bitpackedMidiMessage shr 8 and 0x7f
            data2 = bitpackedMidiMessage shr 16 and 0x7f
            setKeyPressure()
        } else if (message == 176) {
            channel = bitpackedMidiMessage and 0xf
            data1 = bitpackedMidiMessage shr 8 and 0x7f
            data2 = bitpackedMidiMessage shr 16 and 0x7f
            if (data1 == 0) {
                this.bankSelect[channel] = (data2 shl 14) + (this.bankSelect[channel] and -2080769)
            }

            if (data1 == 32) {
                this.bankSelect[channel] = (data2 shl 7) + (this.bankSelect[channel] and -16257)
            }

            if (data1 == 1) {
                this.modulation[channel] = (data2 shl 7) + (this.modulation[channel] and -16257)
            }

            if (data1 == 33) {
                this.modulation[channel] = data2 + (this.modulation[channel] and -128)
            }

            if (data1 == 5) {
                this.portamento[channel] = (data2 shl 7) + (this.portamento[channel] and -16257)
            }

            if (data1 == 37) {
                this.portamento[channel] = data2 + (this.portamento[channel] and -128)
            }

            if (data1 == 7) {
                this.volume[channel] = (data2 shl 7) + (this.volume[channel] and -16257)
            }

            if (data1 == 39) {
                this.volume[channel] = data2 + (this.volume[channel] and -128)
            }

            if (data1 == 10) {
                this.pan[channel] = (data2 shl 7) + (this.pan[channel] and -16257)
            }

            if (data1 == 42) {
                this.pan[channel] = data2 + (this.pan[channel] and -128)
            }

            if (data1 == 11) {
                this.expression[channel] = (data2 shl 7) + (this.expression[channel] and -16257)
            }

            if (data1 == 43) {
                this.expression[channel] = data2 + (this.expression[channel] and -128)
            }

            if (data1 == 64) {
                if (data2 >= 64) {
                    this.sustain[channel] = this.sustain[channel] or 0x1
                } else {
                    this.sustain[channel] = this.sustain[channel] and -2
                }
            }

            if (data1 == 65) {
                if (data2 >= 64) {
                    this.sustain[channel] = this.sustain[channel] or 0x2
                } else {
                    resetSustainPedal(channel)
                    this.sustain[channel] = this.sustain[channel] and -3
                }
            }

            if (data1 == 99) {
                this.nonRegisteredParameter[channel] = (data2 shl 7) + (this.nonRegisteredParameter[channel] and 0x7f)
            }

            if (data1 == 98) {
                this.nonRegisteredParameter[channel] = (this.nonRegisteredParameter[channel] and 0x3f80) + data2
            }

            if (data1 == 101) {
                this.nonRegisteredParameter[channel] = (data2 shl 7) + (this.nonRegisteredParameter[channel] and 0x7f) + 16384
            }

            if (data1 == 100) {
                this.nonRegisteredParameter[channel] = (this.nonRegisteredParameter[channel] and 0x3f80) + data2 + 16384
            }

            if (data1 == 120) {
                turnAllSoundOff(channel)
            }

            if (data1 == 121) {
                resetAllControllers(channel)
            }

            if (data1 == 123) {
                turnAllNotesOff(channel)
            }

            var i_7: Int
            if (data1 == 6) {
                i_7 = this.nonRegisteredParameter[channel]
                if (i_7 == 16384) {
                    this.dataEntry[channel] = (data2 shl 7) + (this.dataEntry[channel] and -16257)
                }
            }

            if (data1 == 38) {
                i_7 = this.nonRegisteredParameter[channel]
                if (i_7 == 16384) {
                    this.dataEntry[channel] = data2 + (this.dataEntry[channel] and -128)
                }
            }

            if (data1 == 16) {
                this.generalPurpose1[channel] = (data2 shl 7) + (this.generalPurpose1[channel] and -16257)
            }

            if (data1 == 48) {
                this.generalPurpose1[channel] = data2 + (this.generalPurpose1[channel] and -128)
            }

            if (data1 == 81) {
                if (data2 >= 64) {
                    this.sustain[channel] = this.sustain[channel] or 0x4
                } else {
                    resetEffectSwitch(channel, 97.toByte())
                    this.sustain[channel] = this.sustain[channel] and -5
                }
            }

            if (data1 == 17) {
                setEffectSpeed(channel, (data2 shl 7) + (this.effectSpeedRaw[channel] and -16257))
            }

            if (data1 == 49) {
                setEffectSpeed(channel, data2 + (this.effectSpeedRaw[channel] and -128))
            }
        } else if (message == 192) {
            channel = bitpackedMidiMessage and 0xf
            data1 = bitpackedMidiMessage shr 8 and 0x7f
            setProgramChange(channel, data1 + this.bankSelect[channel])
        } else if (message == 208) {
            channel = bitpackedMidiMessage and 0xf
            data1 = bitpackedMidiMessage shr 8 and 0x7f
            setAfterTouch()
        } else if (message == 224) {
            channel = bitpackedMidiMessage and 0xf
            data1 = (bitpackedMidiMessage shr 8 and 0x7f) + (bitpackedMidiMessage shr 9 and 0x3f80)
            setPitchBend(channel, data1)
        } else {
            val msg = bitpackedMidiMessage and 0xff
            if (msg == 255) {
                sendSystemResetMessage(true)
            }
        }
    }

    fun calculatePitch(class282Sub131: MusicPatchNode): Int {
        var i_3 = (class282Sub131.portamentoPitchOffset * class282Sub131.portamentoFactor shr 12) + class282Sub131.notePitchAdjustment
        i_3 += (this.pitchBend[class282Sub131.currentTrack] - 8192) * this.dataEntry[class282Sub131.currentTrack] shr 12
        val class1104 = class282Sub131.musicPatchInfo
        var i_5: Int
        if (class1104 != null && class1104.volumeEnvelopeRelease > 0 && (class1104.vibratoLfoPitch > 0 || this.modulation[class282Sub131.currentTrack] > 0)) {
            i_5 = class1104.vibratoLfoPitch shl 2
            val i_6 = class1104.vibratoDelay shl 1
            if (class282Sub131.tickCounter < i_6) {
                i_5 = i_5 * class282Sub131.tickCounter / i_6
            }

            i_5 += this.modulation[class282Sub131.currentTrack] shr 7
            val d_7 = sin(0.01227184630308513 * (class282Sub131.vibratoPhase and 0x1ff))
            i_3 += (d_7 * i_5).toInt()
        }

        val audioFile = class282Sub131.audioFile
        i_5 = if (audioFile != null) {
            ((audioFile.sampleRate * 256) * (2.0).pow((3.255208333333333E-4) * (i_3.toDouble())) / PcmPlayerManager.sampleRate + 0.5).toInt()
        } else {
            0
        }
        return max(1, i_5)
    }

    fun setAfterTouch() {
    }

    fun calculatePan(class282Sub131: MusicPatchNode): Int {
        val i_3 = this.pan[class282Sub131.currentTrack]
        return if (i_3 < 8192) i_3 * class282Sub131.currentPanValue + 32 shr 6 else 16384 - ((128 - class282Sub131.currentPanValue) * (16384 - i_3) + 32 shr 6)
    }

    @Synchronized
    override fun getFirstStream(): PcmStream {
        return this.patchStream
    }

    fun getMusicVolume(): Int {
        return this.musicVolume
    }

    @Synchronized
    override fun skip(i_1: Int) {
        var offset = i_1
        if (this.fileReader.dataLoaded()) {
            val i_2 = this.fileReader.division * this.ticks / PcmPlayerManager.sampleRate

            do {
                val long_3 = (offset.toLong()) * i_2 + this.currentTickTime
                if (this.nextEventTickTime - long_3 >= 0L) {
                    this.currentTickTime = long_3
                    break
                }

                val i_5 = ((this.nextEventTickTime - this.currentTickTime + i_2 - 1L) / i_2).toInt()
                this.currentTickTime += (i_5.toLong()) * i_2
                this.patchStream.skip(i_5)
                offset -= i_5
                processNextMidiEvents()
            } while (this.fileReader.dataLoaded())
        }

        this.patchStream.skip(offset)
    }

    fun advanceToTick(long_1: Long) {
        while (this.nextEventTickTime <= long_1) {
            var track = this.prioritizedTrack
            var length = this.prioritizedTrackLength

            var long_3: Long
            while (true) {
                long_3 = this.nextEventTickTime
                if (length != this.prioritizedTrackLength) {
                    break
                }
                val trackLengths = this.fileReader.trackLengths
                while (trackLengths != null && length == trackLengths[track]) {
                    this.fileReader.getGotoTrack(track)
                    val flags = this.fileReader.readMessage(track)
                    if (flags == 1) {
                        this.fileReader.setTrackDone()
                        this.fileReader.markTracePosition(track)
                        if (this.fileReader.isDone()) {
                            if (!this.isLooping || length == 0) {
                                sendSystemResetMessage(true)
                                this.fileReader.clear()
                                return
                            }

                            this.fileReader.resetTracksFromTimestamp(long_3)
                        }
                        break
                    }

                    if ((flags and 0x80) != 0 && (flags and 0xf0) != 144) {
                        parseMessage(flags)
                    }

                    this.fileReader.getTrackLength(track)
                    this.fileReader.markTracePosition(track)
                }

                this.currentTickTime = long_3
                track = this.fileReader.getPrioritizedTrack()
                val trackLengths2 = this.fileReader.trackLengths
                length = if (trackLengths2 != null) trackLengths2[track] else 0
            }

            this.prioritizedTrack = track
            this.prioritizedTrackLength = length
            this.nextEventTickTime = long_3
        }
    }

    fun processNextMidiEvents() {
        var i_2 = this.prioritizedTrack
        var i_3 = this.prioritizedTrackLength
        var long_4 = this.nextEventTickTime
        if (this.queuedTrack != null && i_3 == this.queuedTrackTick) {
            loadAndPlayTrack(this.queuedTrack!!, this.isLooping, this.queuedAllSoundOff)
            processNextMidiEvents()
        } else {
            while (true) {
                if (i_3 != this.prioritizedTrackLength) {
                    this.prioritizedTrack = i_2
                    this.prioritizedTrackLength = i_3
                    this.nextEventTickTime = long_4
                    if (this.queuedTrack != null && this.queuedTrackTick < i_3) {
                        this.prioritizedTrack = -1
                        this.prioritizedTrackLength = this.queuedTrackTick
                        this.nextEventTickTime = this.fileReader.getTickTimestamp(this.prioritizedTrackLength)
                    }
                    break
                }

                val trackLengths = this.fileReader.trackLengths
                while (trackLengths != null && i_3 == trackLengths[i_2]) {
                    this.fileReader.getGotoTrack(i_2)
                    val i_6 = this.fileReader.readMessage(i_2)
                    if (i_6 == 1) {
                        this.fileReader.setTrackDone()
                        this.fileReader.markTracePosition(i_2)
                        if (this.fileReader.isDone()) {
                            if (this.queuedTrack != null) {
                                setMusicTrack(this.queuedTrack!!, this.isLooping)
                                processNextMidiEvents()
                                return
                            }

                            if (!this.isLooping || i_3 == 0) {
                                sendSystemResetMessage(true)
                                this.fileReader.clear()
                                return
                            }

                            this.fileReader.resetTracksFromTimestamp(long_4)
                        }
                        break
                    }

                    if ((i_6 and 0x80) != 0) {
                        parseMessage(i_6)
                    }

                    this.fileReader.getTrackLength(i_2)
                    this.fileReader.markTracePosition(i_2)
                }

                i_2 = this.fileReader.getPrioritizedTrack()
                val trackLengths2 = this.fileReader.trackLengths
                i_3 = if (trackLengths2 != null) trackLengths2[i_2] else 0
                long_4 = this.fileReader.getTickTimestamp(i_3)
            }
        }
    }

    @Synchronized
    override fun getActiveVoiceCount(): Int {
        return 0
    }

    fun updatePatchNode(class282Sub131: MusicPatchNode, ints_2: IntArray?, i_3: Int, i_4: Int): Boolean {
        val stream = class282Sub131.stream ?: return true
        class282Sub131.tickSampleLength = PcmPlayerManager.sampleRate / 100
        if (class282Sub131.releaseState < 0 || !stream.isFinished()) {
            var i_6 = class282Sub131.portamentoFactor
            if (i_6 > 0) {
                i_6 -= (16.0 * (2.0).pow((this.portamento[class282Sub131.currentTrack].toDouble()) * (4.921259842519685E-4)) + 0.5).toInt()
                if (i_6 < 0) {
                    i_6 = 0
                }

                class282Sub131.portamentoFactor = i_6
            }

            stream.setPlaybackRate(calculatePitch(class282Sub131))
            val class1107 = class282Sub131.musicPatchInfo ?: return true
            var bool8 = false
            ++class282Sub131.tickCounter
            class282Sub131.vibratoPhase += class1107.volumeEnvelopeRelease
            val d_9 = 5.086263020833333E-6 * ((class282Sub131.currentNotePitch - 60 shl 8) + (class282Sub131.portamentoPitchOffset * class282Sub131.portamentoFactor shr 12))
            if (class1107.volumeEnvelopeDecay > 0) {
                if (class1107.vibratoLfoFrequency > 0) {
                    class282Sub131.volumeEnvelopeTime += (128.0 * (2.0).pow((d_9) * (class1107.vibratoLfoFrequency.toDouble())) + 0.5).toInt()
                } else {
                    class282Sub131.volumeEnvelopeTime += 128
                }

                if (class1107.volumeEnvelopeDecay * class282Sub131.volumeEnvelopeTime >= 819200) {
                    bool8 = true
                }
            }

            class1107.volumeEnvelopeData?.let { array ->
                if (class1107.volumeEnvelopeSustain > 0) {
                    class282Sub131.volumeEnvelopePosition += (128.0 * (2.0).pow((d_9) * (class1107.volumeEnvelopeSustain.toDouble())) + 0.5).toInt()
                } else {
                    class282Sub131.volumeEnvelopePosition += 128
                }

                while (class282Sub131.volumeEnvelopeIndex < array.size - 2 && class282Sub131.volumeEnvelopePosition > (array[class282Sub131.volumeEnvelopeIndex + 2].toInt() and 0xff) shl 8) {
                    class282Sub131.volumeEnvelopeIndex += 2
                }

                if (array.size - 2 == class282Sub131.volumeEnvelopeIndex && array[class282Sub131.volumeEnvelopeIndex + 1].toInt() == 0) {
                    bool8 = true
                }
            }

            if (class282Sub131.releaseState >= 0 && (this.sustain[class282Sub131.currentTrack] and 0x1) == 0 && (class282Sub131.loopVariable < 0 || class282Sub131 != this.loopGroupNotes[class282Sub131.currentTrack][class282Sub131.loopVariable])) {
                class1107.releaseEnvelopeData?.let { array ->
                    if (class1107.vibratoLfoDelay > 0) {
                        class282Sub131.releaseState += (128.0 * (2.0).pow((d_9) * (class1107.vibratoLfoDelay.toDouble())) + 0.5).toInt()
                    } else {
                        class282Sub131.releaseState += 128
                    }

                    while (class282Sub131.releaseEnvelopeIndex < array.size - 2 && class282Sub131.releaseState > (array[class282Sub131.releaseEnvelopeIndex + 2].toInt() and 0xff) shl 8) {
                        class282Sub131.releaseEnvelopeIndex += 2
                    }

                    if (array.size - 2 == class282Sub131.releaseEnvelopeIndex) {
                        bool8 = true
                    }
                }
            }

            if (bool8) {
                stream.fadeOutAndStop(class282Sub131.tickSampleLength)
                if (ints_2 != null) {
                    stream.fill(ints_2, i_3, i_4)
                } else {
                    stream.skip(i_4)
                }

                if (stream.isFading()) {
                    this.patchStream.fadeOutScheduler.addSubStream(stream)
                }

                class282Sub131.clearMusicPatchNode()
                if (class282Sub131.releaseState >= 0) {
                    class282Sub131.unlink()
                    if (class282Sub131.loopVariable > 0 && class282Sub131 == this.loopGroupNotes[class282Sub131.currentTrack][class282Sub131.loopVariable]) {
                        this.loopGroupNotes[class282Sub131.currentTrack][class282Sub131.loopVariable] = null
                    }
                }

                return true
            } else {
                stream.fadeToVolumeAndPan(class282Sub131.tickSampleLength, calculateVolume(class282Sub131), calculatePan(class282Sub131))
                return false
            }
        } else {
            class282Sub131.clearMusicPatchNode()
            class282Sub131.unlink()
            if (class282Sub131.loopVariable > 0 && class282Sub131 == this.loopGroupNotes[class282Sub131.currentTrack][class282Sub131.loopVariable]) {
                this.loopGroupNotes[class282Sub131.currentTrack][class282Sub131.loopVariable] = null
            }

            return true
        }
    }

    @Synchronized
    fun loadAndPlayTrack(musicTrack: MusicTrack, bool2: Boolean, bool3: Boolean) {
        clearTrack(bool3)
        val midi = musicTrack.midi
        if (midi != null) {
            this.fileReader.decode(midi)
            this.isLooping = bool2
            this.currentTickTime = 0L
            val trackCount = this.fileReader.getTrackCount()

            for (track in 0 until trackCount) {
                this.fileReader.getGotoTrack(track)
                this.fileReader.getTrackLength(track)
                this.fileReader.markTracePosition(track)
            }

            this.prioritizedTrack = this.fileReader.getPrioritizedTrack()
            val trackLengths = this.fileReader.trackLengths
            this.prioritizedTrackLength = if (trackLengths != null) trackLengths[this.prioritizedTrack] else 0
            this.nextEventTickTime = this.fileReader.getTickTimestamp(this.prioritizedTrackLength)
        }
    }

    @Synchronized
    fun load() {
        initDrumChannel()
    }

    @Synchronized
    fun setMusicVolume(volume: Int) {
        this.musicVolume = volume
    }

    @Synchronized
    override fun getNextStream(): PcmStream? {
        return null
    }

    @Synchronized
    fun ready(track: MusicTrack, resource: Js5ResourceProvider, cache: SoundBankCache): Boolean {
        track.buildPatchTable()
        var ready = true
        val ints_7 = intArrayOf(22050)

        val table = track.table
        if (table != null) {
            var class282Sub478: ObjectNode? = table.first() as? ObjectNode
            while (class282Sub478 != null) {
                val key = class282Sub478.pointer.toInt()
                var class282Sub1410: MusicPatch? = this.musicPatches.get(key.toLong())
                if (class282Sub1410 == null) {
                    class282Sub1410 = getMusicPatch(resource, key)
                    if (class282Sub1410 == null) {
                        ready = false
                        class282Sub478 = table.next() as? ObjectNode
                        continue
                    }

                    this.musicPatches.put(class282Sub1410, key.toLong())
                }

                val value = class282Sub478.value
                if (value is ByteArray && class282Sub1410 != null && !class282Sub1410.ready(cache, value, ints_7)) {
                    ready = false
                }
                class282Sub478 = table.next() as? ObjectNode
            }
        }

        if (ready) {
            track.clear()
        }

        return ready
    }

    fun setKeyPressure() {
    }

    fun setProgramChange(channel: Int, data1: Int) {
        if (data1 != this.programChange[channel]) {
            this.programChange[channel] = data1

            for (noteIndex in 0..127) {
                this.loopGroupNotes[channel][noteIndex] = null
            }
        }
    }

    fun turnAllNotesOff(i_1: Int) {
        var patchNode: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
        while (patchNode != null) {
            if ((i_1 < 0 || patchNode.currentTrack == i_1) && patchNode.releaseState < 0) {
                this.activeNotes[patchNode.currentTrack][patchNode.currentNotePitch] = null
                patchNode.releaseState = 0
            }
            patchNode = this.patchStream.queue.next() as? MusicPatchNode
        }
    }

    companion object {
        @JvmStatic
        fun getMusicPatch(resource: Js5ResourceProvider, fileId: Int): MusicPatch? {
            val fileData = resource.getFile(fileId)
            return if (fileData == null) null else MusicPatch(fileData)
        }
    }
}
