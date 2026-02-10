package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.sound.player.PcmPlayerManager

/**
 * PCM stream processor for music patches with sustain and looping support.
 */
class MusicPatchPcmStream(@JvmField val aNodeSub15Sub29753: MidiPcmStream) : PcmStream() {

    @JvmField val queue: NodeCollection = NodeCollection()
    @JvmField val aNodeSub15Sub49755: PcmStreamScheduler = PcmStreamScheduler()

    override fun getFirstStream(): PcmStream? {
        val musicPatch = queue.getFirst() as? MusicPatchNode ?: return null
        return musicPatch.stream ?: getNextStream()
    }

    override fun fill(buffer: IntArray, offset: Int, length: Int) {
        aNodeSub15Sub49755.fill(buffer, offset, length)

        var patchNode = queue.getFirst() as? MusicPatchNode
        while (patchNode != null) {
            if (!aNodeSub15Sub29753.method15115(patchNode)) {
                var currentOffset = offset
                var remainingLength = length

                do {
                    if (remainingLength <= patchNode.anInt7584) {
                        fillPatchNode(patchNode, buffer, currentOffset, remainingLength, remainingLength + currentOffset)
                        patchNode.anInt7584 -= remainingLength
                        break
                    }

                    fillPatchNode(patchNode, buffer, currentOffset, patchNode.anInt7584, currentOffset + remainingLength)
                    currentOffset += patchNode.anInt7584
                    remainingLength -= patchNode.anInt7584
                } while (!aNodeSub15Sub29753.method15128(patchNode, buffer, currentOffset, remainingLength))
            }

            patchNode = queue.next() as? MusicPatchNode
        }
    }

    fun skipPatchNode(patchNode: MusicPatchNode, amount: Int) {
        var skipAmount = amount
        if ((aNodeSub15Sub29753.sustain[patchNode.currentTrack] and 0x4) != 0 && patchNode.anInt7579 < 0) {
            val step = aNodeSub15Sub29753.anIntArray9712[patchNode.currentTrack] / PcmPlayerManager.sampleRate
            val loopCount = (step + 1048575 - patchNode.anInt7583) / step
            patchNode.anInt7583 = (step * skipAmount + patchNode.anInt7583) and 0xfffff
            if (loopCount <= skipAmount) {
                val currentStream = patchNode.stream!!
                if (aNodeSub15Sub29753.generalPurpose1[patchNode.currentTrack] == 0) {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.method15323(),
                        currentStream.method15315(),
                        currentStream.method15316()
                    )
                } else {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.method15323(),
                        0,
                        currentStream.method15316()
                    )
                    aNodeSub15Sub29753.method15099(patchNode, patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0)
                }

                if (patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0) {
                    patchNode.stream!!.setNumLoops(-1)
                }

                skipAmount = patchNode.anInt7583 / step
            }
        }

        patchNode.stream!!.skip(skipAmount)
    }

    fun fillPatchNode(patchNode: MusicPatchNode, buffer: IntArray, offset: Int, length: Int, endOffset: Int) {
        if ((aNodeSub15Sub29753.sustain[patchNode.currentTrack] and 0x4) != 0 && patchNode.anInt7579 < 0) {
            val step = aNodeSub15Sub29753.anIntArray9712[patchNode.currentTrack] / PcmPlayerManager.sampleRate
            var currentOffset = offset
            var remainingLength = length

            while (true) {
                val loopCount = (step + 1048575 - patchNode.anInt7583) / step
                if (loopCount > remainingLength) {
                    patchNode.anInt7583 += remainingLength * step
                    break
                }

                patchNode.stream!!.fill(buffer, currentOffset, loopCount)
                currentOffset += loopCount
                remainingLength -= loopCount
                patchNode.anInt7583 += step * loopCount - 1048576
                val fadeLength = PcmPlayerManager.sampleRate / 100
                val maxFadeLength = 262144 / step
                val actualFadeLength = if (maxFadeLength < fadeLength) maxFadeLength else fadeLength

                val currentStream = patchNode.stream!!
                if (aNodeSub15Sub29753.generalPurpose1[patchNode.currentTrack] == 0) {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.method15323(),
                        currentStream.method15315(),
                        currentStream.method15316()
                    )
                } else {
                    patchNode.stream = patchNode.audioFile!!.getStream(
                        currentStream.method15323(),
                        0,
                        currentStream.method15316()
                    )
                    aNodeSub15Sub29753.method15099(patchNode, patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0)
                    patchNode.stream!!.method15320(actualFadeLength, currentStream.method15315())
                }

                if (patchNode.patch!!.pitchOffset[patchNode.currentNotePitch] < 0) {
                    patchNode.stream!!.setNumLoops(-1)
                }

                currentStream.method15322(actualFadeLength)
                currentStream.fill(buffer, currentOffset, endOffset - currentOffset)
                if (currentStream.method15311()) {
                    aNodeSub15Sub49755.addSubStream(currentStream)
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

    override fun method12228(): Int = 0

    override fun skip(amount: Int) {
        aNodeSub15Sub49755.skip(amount)

        var patchNode = queue.getFirst() as? MusicPatchNode
        while (patchNode != null) {
            if (!aNodeSub15Sub29753.method15115(patchNode)) {
                var remainingAmount = amount

                do {
                    if (remainingAmount <= patchNode.anInt7584) {
                        skipPatchNode(patchNode, remainingAmount)
                        patchNode.anInt7584 -= remainingAmount
                        break
                    }

                    skipPatchNode(patchNode, patchNode.anInt7584)
                    remainingAmount -= patchNode.anInt7584
                } while (!aNodeSub15Sub29753.method15128(patchNode, null, 0, remainingAmount))
            }

            patchNode = queue.next() as? MusicPatchNode
        }
    }
}
