package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.midi.SoundBankCache

class MusicPatch(fileData: ByteArray) : Node<Any?>() {

    @JvmField
    var sampleOffsets = IntArray(128)

    @JvmField val audioFiles = arrayOfNulls<AbstractAudioFile>(128)
    @JvmField val pitchOffset = ShortArray(128)
    @JvmField val volumeOffset = ByteArray(128)
    @JvmField val panOffset = ByteArray(128)
    @JvmField val musicPatchNodes2 = arrayOfNulls<MusicPatchInfo>(128)
    @JvmField val loopMode = ByteArray(128)
    @JvmField val baseVelocity: Int

    init {
        val patchBuffer = Packet(fileData)

        var loopModeRunCount: Int
        var loopModeScanPos = 0
        while (patchBuffer.buffer[loopModeScanPos + patchBuffer.currentPosition] != 0.toByte()) {
            loopModeScanPos++
        }
        loopModeRunCount = loopModeScanPos

        val loopModeRuns = ByteArray(loopModeRunCount)

        var loopModeRunIdx = 0
        while (loopModeRunIdx < loopModeRunCount) {
            loopModeRuns[loopModeRunIdx] = patchBuffer.readByte()
            loopModeRunIdx++
        }

        ++patchBuffer.currentPosition
        ++loopModeRunCount
        var loopModeValuePos = patchBuffer.currentPosition
        patchBuffer.currentPosition += loopModeRunCount

        var panRunCount = 0
        var panScanPos = 0
        while (patchBuffer.buffer[panScanPos + patchBuffer.currentPosition] != 0.toByte()) {
            panScanPos++
        }
        panRunCount = panScanPos

        val panRuns = ByteArray(panRunCount)

        var panRunIdx = 0
        while (panRunIdx < panRunCount) {
            panRuns[panRunIdx] = patchBuffer.readByte()
            panRunIdx++
        }

        ++patchBuffer.currentPosition
        ++panRunCount
        var panValuePos = patchBuffer.currentPosition
        patchBuffer.currentPosition += panRunCount

        var patchInfoRunCount = 0
        var patchInfoScanPos = 0
        while (patchBuffer.buffer[patchInfoScanPos + patchBuffer.currentPosition] != 0.toByte()) {
            patchInfoScanPos++
        }
        patchInfoRunCount = patchInfoScanPos

        val patchInfoRuns = ByteArray(patchInfoRunCount)

        for (i in 0 until patchInfoRunCount) {
            patchInfoRuns[i] = patchBuffer.readByte()
        }

        ++patchBuffer.currentPosition
        ++patchInfoRunCount
        val runPatchInfoIndices = ByteArray(patchInfoRunCount)
        var patchInfoCount: Int
        var entryIdx: Int
        if (patchInfoRunCount > 1) {
            runPatchInfoIndices[1] = 1
            var patchInfoIndex = 1
            patchInfoCount = 2

            for (run in 2 until patchInfoRunCount) {
                entryIdx = run
                val patchInfoRef = patchBuffer.readUByte()
                if (patchInfoRef == 0) {
                    patchInfoIndex = patchInfoCount++
                } else {
                    patchInfoIndex = if (patchInfoRef <= patchInfoIndex) patchInfoRef - 1 else patchInfoRef
                }

                runPatchInfoIndices[entryIdx] = patchInfoIndex.toByte()
            }
        } else {
            patchInfoCount = patchInfoRunCount
        }

        val patchInfos = arrayOfNulls<MusicPatchInfo>(patchInfoCount)

        for (i in 0 until patchInfos.size) {
            entryIdx = i
            val patchInfo = MusicPatchInfo()
            patchInfos[entryIdx] = patchInfo
            var segmentCount = patchBuffer.readUByte()
            if (segmentCount > 0) {
                patchInfo.volumeEnvelopeData = ByteArray(segmentCount * 2)
            }

            segmentCount = patchBuffer.readUByte()
            if (segmentCount > 0) {
                patchInfo.releaseEnvelopeData = ByteArray(segmentCount * 2 + 2)
                patchInfo.releaseEnvelopeData!![1] = 64
            }
        }

        var curvePointCount = patchBuffer.readUByte()
        val volumeCurve = if (curvePointCount > 0) ByteArray(curvePointCount * 2) else null
        curvePointCount = patchBuffer.readUByte()
        val panCurve = if (curvePointCount > 0) ByteArray(curvePointCount * 2) else null

        var keyRunCount = 0
        var keyScanPos = 0
        while (patchBuffer.buffer[keyScanPos + patchBuffer.currentPosition] != 0.toByte()) {
            keyScanPos++
        }
        keyRunCount = keyScanPos

        val keyRuns = ByteArray(keyRunCount)

        var accum = 0
        for (i in 0 until keyRunCount) {
            keyRuns[i] = patchBuffer.readByte()
        }

        ++patchBuffer.currentPosition
        ++keyRunCount
        accum = 0

        var n: Int
        for (key in 0 until 128) {
            n = key
            accum += patchBuffer.readUByte()
            this.pitchOffset[n] = accum.toShort()
        }

        accum = 0

        for (key in 0 until 128) {
            n = key
            accum += patchBuffer.readUByte()
            this.pitchOffset[n] = (this.pitchOffset[n] + (accum shl 8)).toShort()
        }

        n = 0
        var runIdx = 0
        var sampleOffset = 0

        var m: Int
        for (key in 0 until 128) {
            m = key
            if (n == 0) {
                if (runIdx < keyRuns.size) {
                    n = keyRuns[runIdx++].toInt()
                } else {
                    n = -1
                }

                sampleOffset = patchBuffer.readVarInt()
            }

            this.pitchOffset[m] = (this.pitchOffset[m] + ((sampleOffset - 1 and 0x2) shl 14)).toShort()
            this.sampleOffsets[m] = sampleOffset
            --n
        }

        n = 0
        runIdx = 0
        m = 0

        var p: Int
        for (key in 0 until 128) {
            p = key
            if (this.sampleOffsets[p] != 0) {
                if (n == 0) {
                    if (runIdx < loopModeRuns.size) {
                        n = loopModeRuns[runIdx++].toInt()
                    } else {
                        n = -1
                    }

                    m = patchBuffer.buffer[loopModeValuePos++].toInt() - 1
                }

                this.loopMode[p] = m.toByte()
                --n
            }
        }

        n = 0
        runIdx = 0
        p = 0

        for (key in 0 until 128) {
            if (this.sampleOffsets[key] != 0) {
                if (n == 0) {
                    if (runIdx < panRuns.size) {
                        n = panRuns[runIdx++].toInt()
                    } else {
                        n = -1
                    }

                    p = (patchBuffer.buffer[panValuePos++].toInt() + 16) shl 2
                }

                this.panOffset[key] = p.toByte()
                --n
            }
        }

        n = 0
        runIdx = 0
        var runPatchInfo: MusicPatchInfo? = null

        var q: Int
        for (key in 0 until 128) {
            q = key
            if (this.sampleOffsets[q] != 0) {
                if (n == 0) {
                    runPatchInfo = patchInfos[runPatchInfoIndices[runIdx].toInt()]
                    if (runIdx < patchInfoRuns.size) {
                        n = patchInfoRuns[runIdx++].toInt()
                    } else {
                        n = -1
                    }
                }

                this.musicPatchNodes2[q] = runPatchInfo
                --n
            }
        }

        n = 0
        runIdx = 0
        q = 0

        var idx: Int
        for (key in 0 until 128) {
            idx = key
            if (n == 0) {
                if (runIdx < keyRuns.size) {
                    n = keyRuns[runIdx++].toInt()
                } else {
                    n = -1
                }

                if (this.sampleOffsets[idx] > 0) {
                    q = patchBuffer.readUByte() + 1
                }
            }

            this.volumeOffset[idx] = q.toByte()
            --n
        }

        this.baseVelocity = patchBuffer.readUByte() + 1

        var envIdx: Int
        var patchInfo: MusicPatchInfo?
        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfo = patchInfos[idx]
            if (patchInfo?.volumeEnvelopeData != null) {
                envIdx = 1
                while (envIdx < patchInfo.volumeEnvelopeData!!.size) {
                    patchInfo.volumeEnvelopeData!![envIdx] = patchBuffer.readByte()
                    envIdx += 2
                }
            }

            if (patchInfo?.releaseEnvelopeData != null) {
                envIdx = 3
                while (envIdx < patchInfo.releaseEnvelopeData!!.size - 2) {
                    patchInfo.releaseEnvelopeData!![envIdx] = patchBuffer.readByte()
                    envIdx += 2
                }
            }
        }

        if (volumeCurve != null) {
            for (curveIdx in 1 until volumeCurve.size step 2) {
                volumeCurve[curveIdx] = patchBuffer.readByte()
            }
        }

        if (panCurve != null) {
            for (curveIdx in 1 until panCurve.size step 2) {
                panCurve[curveIdx] = patchBuffer.readByte()
            }
        }

        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfo = patchInfos[idx]
            if (patchInfo?.releaseEnvelopeData != null) {
                accum = 0

                envIdx = 2
                while (envIdx < patchInfo.releaseEnvelopeData!!.size) {
                    accum = 1 + accum + patchBuffer.readUByte()
                    patchInfo.releaseEnvelopeData!![envIdx] = accum.toByte()
                    envIdx += 2
                }
            }
        }

        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfo = patchInfos[idx]
            if (patchInfo?.volumeEnvelopeData != null) {
                accum = 0

                envIdx = 2
                while (envIdx < patchInfo.volumeEnvelopeData!!.size) {
                    accum += 1 + patchBuffer.readUByte()
                    patchInfo.volumeEnvelopeData!![envIdx] = accum.toByte()
                    envIdx += 2
                }
            }
        }

        if (volumeCurve != null) {
            accum = patchBuffer.readUByte()
            volumeCurve[0] = accum.toByte()
            for (curveIdx in 2 until volumeCurve.size step 2) {
                accum = 1 + accum + patchBuffer.readUByte()
                volumeCurve[curveIdx] = accum.toByte()
            }

            var startKey = volumeCurve[0].toInt()
            var startVolume = volumeCurve[1].toInt()
            for (key in 0 until startKey) {
                this.volumeOffset[key] = (startVolume * this.volumeOffset[key] + 32 shr 6).toByte()
            }

            for (curveIdx in 2 until volumeCurve.size step 2) {
                val endKey = volumeCurve[curveIdx].toInt()
                val endVolume = volumeCurve[curveIdx + 1].toInt()
                var interpAcc = startVolume * (endKey - startKey) + (endKey - startKey) / 2
                for (key in startKey until endKey) {
                    val interpValue = divideRounded(interpAcc, endKey - startKey)
                    this.volumeOffset[key] = (interpValue * this.volumeOffset[key] + 32 shr 6).toByte()
                    interpAcc += endVolume - startVolume
                }
                startKey = endKey
                startVolume = endVolume
            }

            for (key in startKey until 128) {
                this.volumeOffset[key] = (startVolume * this.volumeOffset[key] + 32 shr 6).toByte()
            }
        }

        if (panCurve != null) {
            accum = patchBuffer.readUByte()
            panCurve[0] = accum.toByte()
            for (curveIdx in 2 until panCurve.size step 2) {
                accum = 1 + accum + patchBuffer.readUByte()
                panCurve[curveIdx] = accum.toByte()
            }

            var startKey = panCurve[0].toInt()
            var startPan = panCurve[1].toInt() shl 1
            for (key in 0 until startKey) {
                this.panOffset[key] = (startPan + (this.panOffset[key].toInt() and 0xff)).coerceIn(0, 128).toByte()
            }

            for (curveIdx in 2 until panCurve.size step 2) {
                val endKey = panCurve[curveIdx].toInt()
                val endPan = panCurve[curveIdx + 1].toInt() shl 1
                var interpAcc = (endKey - startKey) * startPan + (endKey - startKey) / 2
                for (key in startKey until endKey) {
                    val interpValue = divideRounded(interpAcc, endKey - startKey)
                    this.panOffset[key] = ((this.panOffset[key].toInt() and 0xff) + interpValue).coerceIn(0, 128).toByte()
                    interpAcc += endPan - startPan
                }
                startKey = endKey
                startPan = endPan
            }

            for (key in startKey until 128) {
                this.panOffset[key] = ((this.panOffset[key].toInt() and 0xff) + startPan).coerceIn(0, 128).toByte()
            }
        }

        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfos[idx]?.let { it.volumeEnvelopeDecay = patchBuffer.readUByte() }
        }

        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfo = patchInfos[idx]
            if (patchInfo?.volumeEnvelopeData != null) {
                patchInfo.volumeEnvelopeKeyScaling = patchBuffer.readUByte()
            }

            if (patchInfo?.releaseEnvelopeData != null) {
                patchInfo.releaseEnvelopeKeyScaling = patchBuffer.readUByte()
            }

            if (patchInfo?.volumeEnvelopeDecay ?: 0 > 0) {
                patchInfo!!.decayKeyScaling = patchBuffer.readUByte()
            }
        }

        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfos[idx]?.let { it.volumeEnvelopeRelease = patchBuffer.readUByte() }
        }

        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfo = patchInfos[idx]
            if (patchInfo?.volumeEnvelopeRelease ?: 0 > 0) {
                patchInfo?.let { it.vibratoLfoPitch = patchBuffer.readUByte() }
            }
        }

        for (i in 0 until patchInfoCount) {
            idx = i
            patchInfo = patchInfos[idx]
            if (patchInfo?.vibratoLfoPitch ?: 0 > 0) {
                patchInfo?.let { it.vibratoDelay = patchBuffer.readUByte() }
            }
        }
    }

    fun ready(cache: SoundBankCache, data: ByteArray?, sampleRates: IntArray): Boolean {
        var ready = true
        var file = 0
        var audioFile: AbstractAudioFile? = null

        for (index in 0 until 128) {
            if (data == null || data[index] != 0.toByte()) {
                val sampleOffset = this.sampleOffsets[index]
                if (sampleOffset != 0) {
                    if (sampleOffset != file) {
                        var sampleOffsetVar = sampleOffset - 1
                        file = sampleOffset
                        if ((sampleOffsetVar and 0x1) == 0) {
                            audioFile = cache.listEffect(sampleOffsetVar shr 2, sampleRates)
                        } else {
                            audioFile = cache.listMusic(sampleOffsetVar shr 2, sampleRates)
                        }

                        if (audioFile == null) {
                            ready = false
                        }
                    }

                    if (audioFile != null) {
                        this.audioFiles[index] = audioFile
                        this.sampleOffsets[index] = 0
                    }
                }
            }
        }

        return ready
    }

    fun reset() {
        this.sampleOffsets = IntArray(0)
    }

    companion object {
        @JvmStatic
        fun divideRounded(numerator: Int, denominator: Int): Int {
            val signBit = numerator ushr 31
            return (numerator + signBit) / denominator - signBit
        }
    }
}
