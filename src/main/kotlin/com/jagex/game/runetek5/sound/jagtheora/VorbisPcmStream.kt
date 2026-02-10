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
    override fun fill(ints_1: IntArray, i_2: Int, i_3: Int) {
        var i_2_var = i_2
        if (!isMuted) {
            if (getFirst() == null) {
                if (isStopping) {
                    unlink()
                    TIMED_CACHE.clear()
                }
            } else {
                var i_4 = i_3 + i_2_var
                if (PcmPlayerManager.stereo) {
                    i_4 = i_4 shl 1
                }
                val b_5: Byte = 0
                val b_6: Byte = if (channelCount == 2) 1 else 0

                while (i_2_var < i_4) {
                    val class282Sub397 = getFirst() ?: break
                    val shorts_8 = class282Sub397.samples

                    while (i_2_var < i_4 && sampleOffset < shorts_8[0].size) {
                        if (PcmPlayerManager.stereo) {
                            ints_1[i_2_var++] = shorts_8[b_5.toInt()][sampleOffset] * leftVolume
                            ints_1[i_2_var++] = shorts_8[b_6.toInt()][sampleOffset] * rightVolume
                        } else {
                            ints_1[i_2_var++] += shorts_8[b_6.toInt()][sampleOffset] * rightVolume + shorts_8[b_5.toInt()][sampleOffset] * leftVolume
                        }
                        sampleOffset++
                    }

                    if (sampleOffset >= shorts_8[0].size) {
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
        val class282Sub392 = getFirst()
        if (class282Sub392 != null) {
            class282Sub392.unlink()
            sampleOffset = 0
            capacityUsed--
            TIMED_CACHE.put(class282Sub392.getCacheKey(), class282Sub392)
        }
    }

    fun allocateSampleData(i_1: Int, d_2: Double): MonoSterioData {
        val long_4 = channelCount.toLong() or i_1.toLong()
        var class282Sub396 = TIMED_CACHE.get(long_4)
        if (class282Sub396 != null) {
            class282Sub396.timestamp = d_2
            TIMED_CACHE.pop(long_4)
        } else {
            class282Sub396 = MonoSterioData(Array(channelCount) { ShortArray(i_1) }, d_2)
        }
        return class282Sub396
    }

    @Synchronized
    fun enqueueSampleData(class282Sub391: MonoSterioData) {
        while (capacityUsed >= 100) {
            nodeCollection.popHead()
            capacityUsed--
        }
        nodeCollection.addFirst(class282Sub391)
        capacityUsed++
    }

    @Synchronized
    fun setMuted(bool1: Boolean) {
        isMuted = bool1
    }

    override fun getActiveVoiceCount(): Int {
        return 1
    }

    @Synchronized
    override fun skip(i_1: Int) {
        var i_1_var = i_1
        if (!isMuted) {
            while (true) {
                val class282Sub392 = getFirst()
                if (class282Sub392 == null) {
                    if (isStopping) {
                        unlink()
                        TIMED_CACHE.clear()
                    }
                    break
                }
                if (class282Sub392.samples[0].size - sampleOffset > i_1_var) {
                    sampleOffset += i_1_var
                    break
                }
                i_1_var -= class282Sub392.samples[0].size - sampleOffset
                advanceBuffer()
            }
        }
    }

    @Synchronized
    fun getPlaybackTime(): Double {
        if (capacityUsed < 1) {
            return -1.0
        }
        val class282Sub392 = nodeCollection.getFirst() as MonoSterioData?
        return if (class282Sub392 == null) {
            -1.0
        } else {
            class282Sub392.timestamp - (class282Sub392.samples[0].size.toFloat() / PcmPlayerManager.sampleRate)
        }
    }

    fun setVolume(i_1: Int) {
        leftVolume = i_1
        rightVolume = i_1
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
