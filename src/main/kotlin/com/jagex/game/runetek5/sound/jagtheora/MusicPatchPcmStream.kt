package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.sound.player.PcmPlayerManager

/**
 * PCM stream processor for music patches with sustain and looping support.
 */
class MusicPatchPcmStream(@JvmField val midiPcmStream: MidiPcmStream) : PcmStream() {

    @JvmField val queue: NodeCollection = NodeCollection()
    @JvmField val fadeOutScheduler: PcmStreamMixer = PcmStreamMixer()

    override fun getFirstStream(): PcmStream? {
        val musicPatch = queue.getFirst() as? MusicPatchNode ?: return null
        return musicPatch.stream ?: getNextStream()
    }

    override fun fill(buffer: IntArray, offset: Int, length: Int) {
        fadeOutScheduler.fill(buffer, offset, length)

        var patchNode = queue.getFirst() as? MusicPatchNode
        while (patchNode != null) {
            if (!midiPcmStream.isPatchNodeDead(patchNode)) {
                var currentOffset = offset
                var remainingLength = length

                do {
                    if (remainingLength <= patchNode.tickSampleLength) {
                        fillPatchNode(patchNode, buffer, currentOffset, remainingLength, remainingLength + currentOffset)
                        patchNode.tickSampleLength -= remainingLength
                        break
                    }

                    fillPatchNode(patchNode, buffer, currentOffset, patchNode.tickSampleLength, currentOffset + remainingLength)
                    currentOffset += patchNode.tickSampleLength
                    remainingLength -= patchNode.tickSampleLength
                } while (!midiPcmStream.updatePatchNode(patchNode, buffer, currentOffset, remainingLength))
            }

            patchNode = queue.next() as? MusicPatchNode
        }
    }

    fun skipPatchNode(patchNode: MusicPatchNode, amount: Int) {
        var skipAmount = amount
        if ((midiPcmStream.sustain[patchNode.currentTrack] and 0x4) != 0 && patchNode.releaseState < 0) {
            val step = midiPcmStream.effectSpeedScaled[patchNode.currentTrack] / PcmPlayerManager.sampleRate
            val loopCount = (step + 1048575 - patchNode.retriggerPhase) / step
            patchNode.retriggerPhase = (step * skipAmount + patchNode.retriggerPhase) and 0xfffff
            if (loopCount <= skipAmount) {
                val currentStream = patchNode.stream!!
                if (midiPcmStream.generalPurpose1[patchNode.currentTrack] == 0) {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.getAbsolutePlaybackRate(),
                        currentStream.getTargetVolume(),
                        currentStream.getPanPosition()
                    )
                } else {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.getAbsolutePlaybackRate(),
                        0,
                        currentStream.getPanPosition()
                    )
                    midiPcmStream.applyInitialSampleOffset(patchNode, patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0)
                }

                if (patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0) {
                    patchNode.stream!!.setNumLoops(-1)
                }

                skipAmount = patchNode.retriggerPhase / step
            }
        }

        patchNode.stream!!.skip(skipAmount)
    }

    fun fillPatchNode(patchNode: MusicPatchNode, buffer: IntArray, offset: Int, length: Int, endOffset: Int) {
        if ((midiPcmStream.sustain[patchNode.currentTrack] and 0x4) != 0 && patchNode.releaseState < 0) {
            val step = midiPcmStream.effectSpeedScaled[patchNode.currentTrack] / PcmPlayerManager.sampleRate
            var currentOffset = offset
            var remainingLength = length

            while (true) {
                val loopCount = (step + 1048575 - patchNode.retriggerPhase) / step
                if (loopCount > remainingLength) {
                    patchNode.retriggerPhase += remainingLength * step
                    break
                }

                patchNode.stream!!.fill(buffer, currentOffset, loopCount)
                currentOffset += loopCount
                remainingLength -= loopCount
                patchNode.retriggerPhase += step * loopCount - 1048576
                val fadeLength = PcmPlayerManager.sampleRate / 100
                val maxFadeLength = 262144 / step
                val actualFadeLength = if (maxFadeLength < fadeLength) maxFadeLength else fadeLength

                val currentStream = patchNode.stream!!
                if (midiPcmStream.generalPurpose1[patchNode.currentTrack] == 0) {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.getAbsolutePlaybackRate(),
                        currentStream.getTargetVolume(),
                        currentStream.getPanPosition()
                    )
                } else {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.getAbsolutePlaybackRate(),
                        0,
                        currentStream.getPanPosition()
                    )
                    midiPcmStream.applyInitialSampleOffset(patchNode, patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0)
                    patchNode.stream!!.fadeToVolume(actualFadeLength, currentStream.getTargetVolume())
                }

                if (patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0) {
                    patchNode.stream!!.setNumLoops(-1)
                }

                currentStream.fadeOutAndStop(actualFadeLength)
                currentStream.fill(buffer, currentOffset, endOffset - currentOffset)
                if (currentStream.isFading()) {
                    fadeOutScheduler.addSubStream(currentStream)
                }
            }
        }

        patchNode.stream!!.fill(buffer, offset, length)
    }

    override fun getNextStream(): PcmStream? {
        var musicPatch: MusicPatchNode?
        do {
            musicPatch = queue.next() as? MusicPatchNode ?: return null
        } while (musicPatch.stream == null)

        return musicPatch.stream
    }

    override fun getActiveVoiceCount(): Int = 0

    override fun skip(amount: Int) {
        fadeOutScheduler.skip(amount)

        var patchNode = queue.getFirst() as? MusicPatchNode
        while (patchNode != null) {
            if (!midiPcmStream.isPatchNodeDead(patchNode)) {
                var remainingAmount = amount

                do {
                    if (remainingAmount <= patchNode.tickSampleLength) {
                        skipPatchNode(patchNode, remainingAmount)
                        patchNode.tickSampleLength -= remainingAmount
                        break
                    }

                    skipPatchNode(patchNode, patchNode.tickSampleLength)
                    remainingAmount -= patchNode.tickSampleLength
                } while (!midiPcmStream.updatePatchNode(patchNode, null, 0, remainingAmount))
            }

            patchNode = queue.next() as? MusicPatchNode
        }
    }
}
