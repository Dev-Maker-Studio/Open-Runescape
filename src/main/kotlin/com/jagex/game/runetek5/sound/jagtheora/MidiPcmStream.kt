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
    fun setChannelVolume(channel: Int, volume: Int) {
        if (channel < 0) {
            for (channelIndex in 0..15) {
                this.channelVolume[channelIndex] = volume
            }
        } else {
            this.channelVolume[channel] = volume
        }
    }

    @Synchronized
    override fun fill(mixBuffer: IntArray, offset: Int, length: Int) {
        var outputOffset = offset
        var remaining = length
        if (this.fileReader.dataLoaded()) {
            val ticksPerSample = this.fileReader.division * this.ticks / PcmPlayerManager.sampleRate

            do {
                val projectedTickTime = (ticksPerSample.toLong()) * remaining + this.currentTickTime
                if (this.nextEventTickTime - projectedTickTime >= 0L) {
                    this.currentTickTime = projectedTickTime
                    break
                }

                val samplesToProcess = ((this.nextEventTickTime - this.currentTickTime + ticksPerSample - 1L) / ticksPerSample).toInt()
                this.currentTickTime += (samplesToProcess.toLong()) * ticksPerSample
                this.patchStream.fill(mixBuffer, outputOffset, samplesToProcess)
                outputOffset += samplesToProcess
                remaining -= samplesToProcess
                processNextMidiEvents()
            } while (this.fileReader.dataLoaded())
        }

        this.patchStream.fill(mixBuffer, outputOffset, remaining)
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
    fun setMusicTrack(musicTrack: MusicTrack, looping: Boolean) {
        loadAndPlayTrack(musicTrack, looping, true)
    }

    fun applyInitialSampleOffset(patchNode: MusicPatchNode, reverseLoop: Boolean) {
        val audioFile = patchNode.audioFile ?: return
        val stream = patchNode.stream ?: return
        var samples = audioFile.getTotalSamples()
        val sampleOffset: Int
        if (reverseLoop && audioFile.enableLoop) {
            val bounceLength = samples + samples - audioFile.start
            sampleOffset = ((this.generalPurpose1[patchNode.currentTrack].toLong()) * bounceLength shr 6).toInt()
            samples = samples shl 8
            if (sampleOffset >= samples) {
                stream.reversePlaybackDirection()
            }
        } else {
            sampleOffset = ((this.generalPurpose1[patchNode.currentTrack].toLong()) * samples shr 6).toInt()
        }

        stream.setPlaybackPosition(sampleOffset)
    }

    @Synchronized
    fun setMusicTrackAtPosition(musicTrack: MusicTrack, looping: Boolean, tickPosition: Long) {
        loadAndPlayTrack(musicTrack, looping, true)
        advanceToTick(this.fileReader.division * tickPosition)
    }

    @Synchronized
    fun turnOffSound() {
        clearTrack(true)
    }

    @Synchronized
    fun clearTrack(allSoundOff: Boolean) {
        this.fileReader.clear()
        this.queuedTrack = null
        sendSystemResetMessage(allSoundOff)
    }

    @Synchronized
    fun dataLoaded(): Boolean {
        return this.fileReader.dataLoaded()
    }

    fun setEffectSpeed(channel: Int, rawSpeed: Int) {
        this.effectSpeedRaw[channel] = rawSpeed
        this.effectSpeedScaled[channel] = (2097152.0 * (2.0).pow((rawSpeed.toDouble()) * (5.4931640625E-4)) + 0.5).toInt()
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
                    val portamentoPitch = (patchNode.portamentoFactor * patchNode.portamentoPitchOffset shr 12) + patchNode.notePitchAdjustment
                    patchNode.notePitchAdjustment += (note - patchNode.currentNotePitch) shl 8
                    patchNode.portamentoPitchOffset = portamentoPitch - patchNode.notePitchAdjustment
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
                    val existingNode = this.loopGroupNotes[channel][musicPatchNode.loopVariable]
                    if (existingNode != null && existingNode.releaseState < 0) {
                        this.activeNotes[channel][existingNode.currentNotePitch] = null
                        existingNode.releaseState = 0
                    }

                    this.loopGroupNotes[channel][musicPatchNode.loopVariable] = musicPatchNode
                }

                this.patchStream.queue.addFirst(musicPatchNode)
                this.activeNotes[channel][note] = musicPatchNode
            }
        }
    }

    fun calculateVolume(patchNode: MusicPatchNode): Int {
        if (this.channelVolume[patchNode.currentTrack] == 0) {
            return 0
        } else {
            val patchInfo = patchNode.musicPatchInfo ?: return 0
            var vol = this.expression[patchNode.currentTrack] * this.volume[patchNode.currentTrack] + 4096 shr 13
            vol = vol * vol + 16384 shr 15
            vol = vol * patchNode.maxVolumeLevel + 16384 shr 15
            vol = vol * this.musicVolume + 128 shr 8
            vol = vol * this.volumeMultiplier shr 8
            vol = vol * this.channelVolume[patchNode.currentTrack] + 128 shr 8
            if (patchInfo.volumeEnvelopeDecay > 0) {
                vol = (vol * (0.5).pow((patchNode.volumeEnvelopeTime.toDouble()) * (1.953125E-5) * (patchInfo.volumeEnvelopeDecay.toDouble())) + 0.5).toInt()
            }

            var envelopePosition: Int
            var envelopeValue: Int
            var envelopeKeyStart: Int
            var envelopeKeyEnd: Int
            patchInfo.volumeEnvelopeData?.let {
                envelopePosition = patchNode.volumeEnvelopePosition
                envelopeValue = (it[patchNode.volumeEnvelopeIndex + 1].toInt() and 0xff)
                if (patchNode.volumeEnvelopeIndex < it.size - 2) {
                    envelopeKeyStart = (it[patchNode.volumeEnvelopeIndex].toInt() and 0xff) shl 8
                    envelopeKeyEnd = (it[patchNode.volumeEnvelopeIndex + 2].toInt() and 0xff) shl 8
                    envelopeValue += (envelopePosition - envelopeKeyStart) * (it[patchNode.volumeEnvelopeIndex + 3].toInt() - envelopeValue) / (envelopeKeyEnd - envelopeKeyStart)
                }

                vol = vol * envelopeValue + 32 shr 6
            }

            patchInfo.releaseEnvelopeData?.let {
                if (patchNode.releaseState > 0) {
                    envelopePosition = patchNode.releaseState
                    envelopeValue = (it[patchNode.releaseEnvelopeIndex + 1].toInt() and 0xff)
                    if (patchNode.releaseEnvelopeIndex < it.size - 2) {
                        envelopeKeyStart = (it[patchNode.releaseEnvelopeIndex].toInt() and 0xff) shl 8
                        envelopeKeyEnd = (it[patchNode.releaseEnvelopeIndex + 2].toInt() and 0xff) shl 8
                        envelopeValue += (it[patchNode.releaseEnvelopeIndex + 3].toInt() - envelopeValue) * (envelopePosition - envelopeKeyStart) / (envelopeKeyEnd - envelopeKeyStart)
                    }

                    vol = envelopeValue * vol + 32 shr 6
                }
            }

            return vol
        }
    }

    fun setNoteOff(channel: Int, note: Int) {
        val activeNote = this.activeNotes[channel][note]
        if (activeNote != null) {
            this.activeNotes[channel][note] = null
            if ((this.sustain[channel] and 0x2) != 0) {
                var streamedPatch: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
                while (streamedPatch != null) {
                    if (activeNote.currentTrack == streamedPatch.currentTrack && streamedPatch.releaseState < 0 && streamedPatch != activeNote) {
                        activeNote.releaseState = 0
                        break
                    }
                    streamedPatch = this.patchStream.queue.next() as? MusicPatchNode
                }
            } else {
                activeNote.releaseState = 0
            }
        }
    }

    fun turnAllSoundOff(channel: Int) {
        var patchNode: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
        while (patchNode != null) {
            if (channel < 0 || patchNode.currentTrack == channel) {
                patchNode.stream?.let { stream ->
                    stream.fadeOutAndStop(PcmPlayerManager.sampleRate / 100)
                    if (stream.isFading()) {
                        this.patchStream.fadeOutScheduler.addSubStream(stream)
                    }
                }

                patchNode.clearMusicPatchNode()

                if (patchNode.releaseState < 0) {
                    this.activeNotes[patchNode.currentTrack][patchNode.currentNotePitch] = null
                }

                patchNode.unlink()
            }
            patchNode = this.patchStream.queue.next() as? MusicPatchNode
        }
    }

    fun resetAllControllers(channel: Int) {
        var ch = channel
        if (ch < 0) {
            for (channelIndex in 0..15) {
                resetAllControllers(channelIndex)
            }
        } else {
            this.volume[ch] = 12800
            this.pan[ch] = 8192
            this.expression[ch] = 16383
            this.pitchBend[ch] = 8192
            this.modulation[ch] = 0
            this.portamento[ch] = 8192
            resetSustainPedal(ch)
            resetEffectSwitch(ch, 41.toByte())
            this.sustain[ch] = 0
            this.nonRegisteredParameter[ch] = 32767
            this.dataEntry[ch] = 256
            this.generalPurpose1[ch] = 0
            setEffectSpeed(ch, 8192)
        }
    }

    fun isPatchNodeDead(patchNode: MusicPatchNode): Boolean {
        if (patchNode.stream == null) {
            if (patchNode.releaseState >= 0) {
                patchNode.unlink()
                if (patchNode.loopVariable > 0 && patchNode == this.loopGroupNotes[patchNode.currentTrack][patchNode.loopVariable]) {
                    this.loopGroupNotes[patchNode.currentTrack][patchNode.loopVariable] = null
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

    fun resetEffectSwitch(index: Int, threshold: Byte) {
        if ((this.sustain[index] and 0x4) != 0 && threshold > 1) {
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

            var paramNumber: Int
            if (data1 == 6) {
                paramNumber = this.nonRegisteredParameter[channel]
                if (paramNumber == 16384) {
                    this.dataEntry[channel] = (data2 shl 7) + (this.dataEntry[channel] and -16257)
                }
            }

            if (data1 == 38) {
                paramNumber = this.nonRegisteredParameter[channel]
                if (paramNumber == 16384) {
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

    fun calculatePitch(patchNode: MusicPatchNode): Int {
        var pitch = (patchNode.portamentoPitchOffset * patchNode.portamentoFactor shr 12) + patchNode.notePitchAdjustment
        pitch += (this.pitchBend[patchNode.currentTrack] - 8192) * this.dataEntry[patchNode.currentTrack] shr 12
        val patchInfo = patchNode.musicPatchInfo
        var scaledPitch: Int
        if (patchInfo != null && patchInfo.volumeEnvelopeRelease > 0 && (patchInfo.vibratoLfoPitch > 0 || this.modulation[patchNode.currentTrack] > 0)) {
            scaledPitch = patchInfo.vibratoLfoPitch shl 2
            val vibratoDelay = patchInfo.vibratoDelay shl 1
            if (patchNode.tickCounter < vibratoDelay) {
                scaledPitch = scaledPitch * patchNode.tickCounter / vibratoDelay
            }

            scaledPitch += this.modulation[patchNode.currentTrack] shr 7
            val vibratoWave = sin(0.01227184630308513 * (patchNode.vibratoPhase and 0x1ff))
            pitch += (vibratoWave * scaledPitch).toInt()
        }

        val audioFile = patchNode.audioFile
        scaledPitch = if (audioFile != null) {
            ((audioFile.sampleRate * 256) * (2.0).pow((3.255208333333333E-4) * (pitch.toDouble())) / PcmPlayerManager.sampleRate + 0.5).toInt()
        } else {
            0
        }
        return max(1, scaledPitch)
    }

    fun setAfterTouch() {
    }

    fun calculatePan(patchNode: MusicPatchNode): Int {
        val channelPan = this.pan[patchNode.currentTrack]
        return if (channelPan < 8192) channelPan * patchNode.currentPanValue + 32 shr 6 else 16384 - ((128 - patchNode.currentPanValue) * (16384 - channelPan) + 32 shr 6)
    }

    @Synchronized
    override fun getFirstStream(): PcmStream {
        return this.patchStream
    }

    fun getMusicVolume(): Int {
        return this.musicVolume
    }

    @Synchronized
    override fun skip(sampleCount: Int) {
        var remaining = sampleCount
        if (this.fileReader.dataLoaded()) {
            val ticksPerSample = this.fileReader.division * this.ticks / PcmPlayerManager.sampleRate

            do {
                val projectedTickTime = (remaining.toLong()) * ticksPerSample + this.currentTickTime
                if (this.nextEventTickTime - projectedTickTime >= 0L) {
                    this.currentTickTime = projectedTickTime
                    break
                }

                val samplesToProcess = ((this.nextEventTickTime - this.currentTickTime + ticksPerSample - 1L) / ticksPerSample).toInt()
                this.currentTickTime += (samplesToProcess.toLong()) * ticksPerSample
                this.patchStream.skip(samplesToProcess)
                remaining -= samplesToProcess
                processNextMidiEvents()
            } while (this.fileReader.dataLoaded())
        }

        this.patchStream.skip(remaining)
    }

    fun advanceToTick(targetTick: Long) {
        while (this.nextEventTickTime <= targetTick) {
            var track = this.prioritizedTrack
            var length = this.prioritizedTrackLength

            var tickTime: Long
            while (true) {
                tickTime = this.nextEventTickTime
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

                            this.fileReader.resetTracksFromTimestamp(tickTime)
                        }
                        break
                    }

                    if ((flags and 0x80) != 0 && (flags and 0xf0) != 144) {
                        parseMessage(flags)
                    }

                    this.fileReader.getTrackLength(track)
                    this.fileReader.markTracePosition(track)
                }

                this.currentTickTime = tickTime
                track = this.fileReader.getPrioritizedTrack()
                val trackLengths2 = this.fileReader.trackLengths
                length = if (trackLengths2 != null) trackLengths2[track] else 0
            }

            this.prioritizedTrack = track
            this.prioritizedTrackLength = length
            this.nextEventTickTime = tickTime
        }
    }

    fun processNextMidiEvents() {
        var track = this.prioritizedTrack
        var length = this.prioritizedTrackLength
        var tickTime = this.nextEventTickTime
        if (this.queuedTrack != null && length == this.queuedTrackTick) {
            loadAndPlayTrack(this.queuedTrack!!, this.isLooping, this.queuedAllSoundOff)
            processNextMidiEvents()
        } else {
            while (true) {
                if (length != this.prioritizedTrackLength) {
                    this.prioritizedTrack = track
                    this.prioritizedTrackLength = length
                    this.nextEventTickTime = tickTime
                    if (this.queuedTrack != null && this.queuedTrackTick < length) {
                        this.prioritizedTrack = -1
                        this.prioritizedTrackLength = this.queuedTrackTick
                        this.nextEventTickTime = this.fileReader.getTickTimestamp(this.prioritizedTrackLength)
                    }
                    break
                }

                val trackLengths = this.fileReader.trackLengths
                while (trackLengths != null && length == trackLengths[track]) {
                    this.fileReader.getGotoTrack(track)
                    val midiMessage = this.fileReader.readMessage(track)
                    if (midiMessage == 1) {
                        this.fileReader.setTrackDone()
                        this.fileReader.markTracePosition(track)
                        if (this.fileReader.isDone()) {
                            if (this.queuedTrack != null) {
                                setMusicTrack(this.queuedTrack!!, this.isLooping)
                                processNextMidiEvents()
                                return
                            }

                            if (!this.isLooping || length == 0) {
                                sendSystemResetMessage(true)
                                this.fileReader.clear()
                                return
                            }

                            this.fileReader.resetTracksFromTimestamp(tickTime)
                        }
                        break
                    }

                    if ((midiMessage and 0x80) != 0) {
                        parseMessage(midiMessage)
                    }

                    this.fileReader.getTrackLength(track)
                    this.fileReader.markTracePosition(track)
                }

                track = this.fileReader.getPrioritizedTrack()
                val trackLengths2 = this.fileReader.trackLengths
                length = if (trackLengths2 != null) trackLengths2[track] else 0
                tickTime = this.fileReader.getTickTimestamp(length)
            }
        }
    }

    @Synchronized
    override fun getActiveVoiceCount(): Int {
        return 0
    }

    fun updatePatchNode(patchNode: MusicPatchNode, mixBuffer: IntArray?, outputOffset: Int, sampleLength: Int): Boolean {
        val stream = patchNode.stream ?: return true
        patchNode.tickSampleLength = PcmPlayerManager.sampleRate / 100
        if (patchNode.releaseState < 0 || !stream.isFinished()) {
            var portamentoFactor = patchNode.portamentoFactor
            if (portamentoFactor > 0) {
                portamentoFactor -= (16.0 * (2.0).pow((this.portamento[patchNode.currentTrack].toDouble()) * (4.921259842519685E-4)) + 0.5).toInt()
                if (portamentoFactor < 0) {
                    portamentoFactor = 0
                }

                patchNode.portamentoFactor = portamentoFactor
            }

            stream.setPlaybackRate(calculatePitch(patchNode))
            val patchInfo = patchNode.musicPatchInfo ?: return true
            var isDead = false
            ++patchNode.tickCounter
            patchNode.vibratoPhase += patchInfo.volumeEnvelopeRelease
            val pitchScalar = 5.086263020833333E-6 * ((patchNode.currentNotePitch - 60 shl 8) + (patchNode.portamentoPitchOffset * patchNode.portamentoFactor shr 12))
            if (patchInfo.volumeEnvelopeDecay > 0) {
                if (patchInfo.decayKeyScaling > 0) {
                    patchNode.volumeEnvelopeTime += (128.0 * (2.0).pow((pitchScalar) * (patchInfo.decayKeyScaling.toDouble())) + 0.5).toInt()
                } else {
                    patchNode.volumeEnvelopeTime += 128
                }

                if (patchInfo.volumeEnvelopeDecay * patchNode.volumeEnvelopeTime >= 819200) {
                    isDead = true
                }
            }

            patchInfo.volumeEnvelopeData?.let { array ->
                if (patchInfo.volumeEnvelopeKeyScaling > 0) {
                    patchNode.volumeEnvelopePosition += (128.0 * (2.0).pow((pitchScalar) * (patchInfo.volumeEnvelopeKeyScaling.toDouble())) + 0.5).toInt()
                } else {
                    patchNode.volumeEnvelopePosition += 128
                }

                while (patchNode.volumeEnvelopeIndex < array.size - 2 && patchNode.volumeEnvelopePosition > (array[patchNode.volumeEnvelopeIndex + 2].toInt() and 0xff) shl 8) {
                    patchNode.volumeEnvelopeIndex += 2
                }

                if (array.size - 2 == patchNode.volumeEnvelopeIndex && array[patchNode.volumeEnvelopeIndex + 1].toInt() == 0) {
                    isDead = true
                }
            }

            if (patchNode.releaseState >= 0 && (this.sustain[patchNode.currentTrack] and 0x1) == 0 && (patchNode.loopVariable < 0 || patchNode != this.loopGroupNotes[patchNode.currentTrack][patchNode.loopVariable])) {
                patchInfo.releaseEnvelopeData?.let { array ->
                    if (patchInfo.releaseEnvelopeKeyScaling > 0) {
                        patchNode.releaseState += (128.0 * (2.0).pow((pitchScalar) * (patchInfo.releaseEnvelopeKeyScaling.toDouble())) + 0.5).toInt()
                    } else {
                        patchNode.releaseState += 128
                    }

                    while (patchNode.releaseEnvelopeIndex < array.size - 2 && patchNode.releaseState > (array[patchNode.releaseEnvelopeIndex + 2].toInt() and 0xff) shl 8) {
                        patchNode.releaseEnvelopeIndex += 2
                    }

                    if (array.size - 2 == patchNode.releaseEnvelopeIndex) {
                        isDead = true
                    }
                }
            }

            if (isDead) {
                stream.fadeOutAndStop(patchNode.tickSampleLength)
                if (mixBuffer != null) {
                    stream.fill(mixBuffer, outputOffset, sampleLength)
                } else {
                    stream.skip(sampleLength)
                }

                if (stream.isFading()) {
                    this.patchStream.fadeOutScheduler.addSubStream(stream)
                }

                patchNode.clearMusicPatchNode()
                if (patchNode.releaseState >= 0) {
                    patchNode.unlink()
                    if (patchNode.loopVariable > 0 && patchNode == this.loopGroupNotes[patchNode.currentTrack][patchNode.loopVariable]) {
                        this.loopGroupNotes[patchNode.currentTrack][patchNode.loopVariable] = null
                    }
                }

                return true
            } else {
                stream.fadeToVolumeAndPan(patchNode.tickSampleLength, calculateVolume(patchNode), calculatePan(patchNode))
                return false
            }
        } else {
            patchNode.clearMusicPatchNode()
            patchNode.unlink()
            if (patchNode.loopVariable > 0 && patchNode == this.loopGroupNotes[patchNode.currentTrack][patchNode.loopVariable]) {
                this.loopGroupNotes[patchNode.currentTrack][patchNode.loopVariable] = null
            }

            return true
        }
    }

    @Synchronized
    fun loadAndPlayTrack(musicTrack: MusicTrack, looping: Boolean, allSoundOff: Boolean) {
        clearTrack(allSoundOff)
        val midi = musicTrack.midi
        if (midi != null) {
            this.fileReader.decode(midi)
            this.isLooping = looping
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
        var isReady = true
        val sampleRates = intArrayOf(22050)

        val table = track.table
        if (table != null) {
            var patchNode: ObjectNode? = table.first() as? ObjectNode
            while (patchNode != null) {
                val key = patchNode.pointer.toInt()
                var patch: MusicPatch? = this.musicPatches.get(key.toLong())
                if (patch == null) {
                    patch = getMusicPatch(resource, key)
                    if (patch == null) {
                        isReady = false
                        patchNode = table.next() as? ObjectNode
                        continue
                    }

                    this.musicPatches.put(patch, key.toLong())
                }

                val value = patchNode.value
                if (value is ByteArray && patch != null && !patch.ready(cache, value, sampleRates)) {
                    isReady = false
                }
                patchNode = table.next() as? ObjectNode
            }
        }

        if (isReady) {
            track.clear()
        }

        return isReady
    }

    fun setKeyPressure() {
    }

    fun setProgramChange(channel: Int, program: Int) {
        if (program != this.programChange[channel]) {
            this.programChange[channel] = program

            for (noteIndex in 0..127) {
                this.loopGroupNotes[channel][noteIndex] = null
            }
        }
    }

    fun turnAllNotesOff(channel: Int) {
        var patchNode: MusicPatchNode? = this.patchStream.queue.getFirst() as? MusicPatchNode
        while (patchNode != null) {
            if ((channel < 0 || patchNode.currentTrack == channel) && patchNode.releaseState < 0) {
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
