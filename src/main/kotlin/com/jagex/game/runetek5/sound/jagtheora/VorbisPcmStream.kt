package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.sound.player.PcmPlayerManager

class VorbisPcmStream(
    @JvmField val anInt9567: Int
) : PcmStream() {

    @JvmField var aBool9571: Boolean = false
    @JvmField var anInt9570: Int = 0
    @JvmField var capacityUsed: Int = 0
    @JvmField val nodeCollection: NodeCollection = NodeCollection()
    @JvmField var anInt9565: Int = 256
    @JvmField var anInt9572: Int = 256
    @JvmField var aBool9574: Boolean = false

    @Synchronized
    fun method14808() {
        aBool9571 = true
    }

    @Synchronized
    override fun fill(ints_1: IntArray, i_2: Int, i_3: Int) {
        var i_2_var = i_2
        if (!aBool9574) {
            if (getFirst() == null) {
                if (aBool9571) {
                    unlink()
                    TIMED_CACHE.clear()
                }
            } else {
                var i_4 = i_3 + i_2_var
                if (PcmPlayerManager.stereo) {
                    i_4 = i_4 shl 1
                }
                val b_5: Byte = 0
                val b_6: Byte = if (anInt9567 == 2) 1 else 0

                while (i_2_var < i_4) {
                    val class282Sub397 = getFirst() ?: break
                    val shorts_8 = class282Sub397.aShortArrayArray8003

                    while (i_2_var < i_4 && anInt9570 < shorts_8[0].size) {
                        if (PcmPlayerManager.stereo) {
                            ints_1[i_2_var++] = shorts_8[b_5.toInt()][anInt9570] * anInt9565
                            ints_1[i_2_var++] = shorts_8[b_6.toInt()][anInt9570] * anInt9572
                        } else {
                            ints_1[i_2_var++] += shorts_8[b_6.toInt()][anInt9570] * anInt9572 + shorts_8[b_5.toInt()][anInt9570] * anInt9565
                        }
                        anInt9570++
                    }

                    if (anInt9570 >= shorts_8[0].size) {
                        method14813()
                    }
                }
            }
        }
    }

    @Synchronized
    fun method14811(): Int {
        return capacityUsed
    }

    @Synchronized
    fun getFirst(): MonoSterioData? {
        return nodeCollection.getFirst() as MonoSterioData?
    }

    @Synchronized
    fun method14813() {
        val class282Sub392 = getFirst()
        if (class282Sub392 != null) {
            class282Sub392.unlink()
            anInt9570 = 0
            capacityUsed--
            TIMED_CACHE.put(class282Sub392.method13294(), class282Sub392)
        }
    }

    fun method14814(i_1: Int, d_2: Double): MonoSterioData {
        val long_4 = anInt9567.toLong() or i_1.toLong()
        var class282Sub396 = TIMED_CACHE.get(long_4)
        if (class282Sub396 != null) {
            class282Sub396.aDouble8004 = d_2
            TIMED_CACHE.pop(long_4)
        } else {
            class282Sub396 = MonoSterioData(Array(anInt9567) { ShortArray(i_1) }, d_2)
        }
        return class282Sub396
    }

    @Synchronized
    fun method14815(class282Sub391: MonoSterioData) {
        while (capacityUsed >= 100) {
            nodeCollection.popHead()
            capacityUsed--
        }
        nodeCollection.addFirst(class282Sub391)
        capacityUsed++
    }

    @Synchronized
    fun method14816(bool1: Boolean) {
        aBool9574 = bool1
    }

    override fun method12228(): Int {
        return 1
    }

    @Synchronized
    override fun skip(i_1: Int) {
        var i_1_var = i_1
        if (!aBool9574) {
            while (true) {
                val class282Sub392 = getFirst()
                if (class282Sub392 == null) {
                    if (aBool9571) {
                        unlink()
                        TIMED_CACHE.clear()
                    }
                    break
                }
                if (class282Sub392.aShortArrayArray8003[0].size - anInt9570 > i_1_var) {
                    anInt9570 += i_1_var
                    break
                }
                i_1_var -= class282Sub392.aShortArrayArray8003[0].size - anInt9570
                method14813()
            }
        }
    }

    @Synchronized
    fun method14819(): Double {
        if (capacityUsed < 1) {
            return -1.0
        }
        val class282Sub392 = nodeCollection.getFirst() as MonoSterioData?
        return if (class282Sub392 == null) {
            -1.0
        } else {
            class282Sub392.aDouble8004 - (class282Sub392.aShortArrayArray8003[0].size.toFloat() / PcmPlayerManager.sampleRate)
        }
    }

    fun method14820(i_1: Int) {
        anInt9565 = i_1
        anInt9572 = i_1
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
