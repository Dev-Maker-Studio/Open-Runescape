package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.sound.player.PcmPlayerManager

class VorbisPcmStream(
    @JvmField val channelCount: Int
) : PcmStream() {

    @JvmField var isStopping: Boolean = false
    @JvmField var sampleOffset: Int = 0
    @JvmField var capacityUsed: Int = 0
    @JvmField val nodeCollection: NodeCollection = NodeCollection()
    @JvmField var leftVolume: Int = 256
    @JvmField var rightVolume: Int = 256
    @JvmField var isMuted: Boolean = false

    @Synchronized
    fun stop() {
        isStopping = true
    }

    @Synchronized
    override fun fill(mixBuffer: IntArray, offset: Int, length: Int) {
        var outputIndex = offset
        if (!isMuted) {
            if (getFirst() == null) {
                if (isStopping) {
                    unlink()
                    TIMED_CACHE.clear()
                }
            } else {
                var endIndex = length + outputIndex
                if (PcmPlayerManager.stereo) {
                    endIndex = endIndex shl 1
                }
                val leftChannel: Byte = 0
                val rightChannel: Byte = if (channelCount == 2) 1 else 0

                while (outputIndex < endIndex) {
                    val sampleData = getFirst() ?: break
                    val samples = sampleData.samples

                    while (outputIndex < endIndex && sampleOffset < samples[0].size) {
                        if (PcmPlayerManager.stereo) {
                            mixBuffer[outputIndex++] = samples[leftChannel.toInt()][sampleOffset] * leftVolume
                            mixBuffer[outputIndex++] = samples[rightChannel.toInt()][sampleOffset] * rightVolume
                        } else {
                            mixBuffer[outputIndex++] += samples[rightChannel.toInt()][sampleOffset] * rightVolume + samples[leftChannel.toInt()][sampleOffset] * leftVolume
                        }
                        sampleOffset++
                    }

                    if (sampleOffset >= samples[0].size) {
                        advanceBuffer()
                    }
                }
            }
        }
    }

    @Synchronized
    fun getBufferedCount(): Int {
        return capacityUsed
    }

    @Synchronized
    fun getFirst(): MonoSterioData? {
        return nodeCollection.getFirst() as MonoSterioData?
    }

    @Synchronized
    fun advanceBuffer() {
        val sampleData = getFirst()
        if (sampleData != null) {
            sampleData.unlink()
            sampleOffset = 0
            capacityUsed--
            TIMED_CACHE.put(sampleData.getCacheKey(), sampleData)
        }
    }

    fun allocateSampleData(sampleCount: Int, timestamp: Double): MonoSterioData {
        val cacheKey = channelCount.toLong() or sampleCount.toLong()
        var data = TIMED_CACHE.get(cacheKey)
        if (data != null) {
            data.timestamp = timestamp
            TIMED_CACHE.pop(cacheKey)
        } else {
            data = MonoSterioData(Array(channelCount) { ShortArray(sampleCount) }, timestamp)
        }
        return data
    }

    @Synchronized
    fun enqueueSampleData(sampleData: MonoSterioData) {
        while (capacityUsed >= 100) {
            nodeCollection.popHead()
            capacityUsed--
        }
        nodeCollection.addFirst(sampleData)
        capacityUsed++
    }

    @Synchronized
    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    override fun getActiveVoiceCount(): Int {
        return 1
    }

    @Synchronized
    override fun skip(sampleCount: Int) {
        var remaining = sampleCount
        if (!isMuted) {
            while (true) {
                val sampleData = getFirst()
                if (sampleData == null) {
                    if (isStopping) {
                        unlink()
                        TIMED_CACHE.clear()
                    }
                    break
                }
                if (sampleData.samples[0].size - sampleOffset > remaining) {
                    sampleOffset += remaining
                    break
                }
                remaining -= sampleData.samples[0].size - sampleOffset
                advanceBuffer()
            }
        }
    }

    @Synchronized
    fun getPlaybackTime(): Double {
        if (capacityUsed < 1) {
            return -1.0
        }
        val sampleData = nodeCollection.getFirst() as MonoSterioData?
        return if (sampleData == null) {
            -1.0
        } else {
            sampleData.timestamp - (sampleData.samples[0].size.toFloat() / PcmPlayerManager.sampleRate)
        }
    }

    fun setVolume(volume: Int) {
        leftVolume = volume
        rightVolume = volume
    }

    override fun getNextStream(): PcmStream? {
        return null
    }

    override fun getFirstStream(): PcmStream? {
        return null
    }

    companion object {
        @JvmField
        val TIMED_CACHE: MonoSterioAudioCache = MonoSterioAudioCache(MonoSterioCacheType.TIMED)
    }
}
