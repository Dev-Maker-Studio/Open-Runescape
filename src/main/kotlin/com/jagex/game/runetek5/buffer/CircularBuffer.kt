package com.jagex.game.runetek5.buffer

/**
 * Circular buffer pool for reusing byte arrays to reduce allocations.
 */
object CircularBuffer {

    /**
     * 0 = not in use
     * 1 = in use
     */
    @JvmField var SLOT_IN_USE: IntArray? = null
    @JvmField var STASH_FOR_BUFFER_SIZE: IntArray? = null
    @JvmField var UNUSED_1_CURSOR_POS: Int = 0
    @JvmField var LOGIN_PACKET_CURSOR_POS: Int = 0
    @JvmField var UNUSED_2_CURSOR_POS: Int = 0
    @JvmField val UNUSED_1 = arrayOfNulls<ByteArray>(1000)
    @JvmField val LOGIN_PACKET_BUFFERS = arrayOfNulls<ByteArray>(250)
    @JvmField val UNUSED_2 = arrayOfNulls<ByteArray>(50)
    @JvmField var STASH: Array<Array<ByteArray?>>? = null

    @JvmStatic
    fun initialize(sizeStashes: IntArray?, maxStashCapacities: IntArray?) {
        if (sizeStashes != null && maxStashCapacities != null) {
            STASH_FOR_BUFFER_SIZE = sizeStashes
            SLOT_IN_USE = IntArray(sizeStashes.size)
            STASH = Array(sizeStashes.size) { arrayOfNulls(maxStashCapacities[it]) }
        } else {
            STASH_FOR_BUFFER_SIZE = null
            SLOT_IN_USE = null
            STASH = null
        }
    }

    @JvmStatic
    @Synchronized
    fun createBuffer(size: Int): ByteArray {
        if (size == 100 && UNUSED_1_CURSOR_POS > 0) {
            val data = UNUSED_1[--UNUSED_1_CURSOR_POS]
            UNUSED_1[UNUSED_1_CURSOR_POS] = null
            return data!!
        } else if (size == 5000 && LOGIN_PACKET_CURSOR_POS > 0) {
            val data = LOGIN_PACKET_BUFFERS[--LOGIN_PACKET_CURSOR_POS]
            LOGIN_PACKET_BUFFERS[LOGIN_PACKET_CURSOR_POS] = null
            return data!!
        } else if (size == 30000 && UNUSED_2_CURSOR_POS > 0) {
            val data = UNUSED_2[--UNUSED_2_CURSOR_POS]
            UNUSED_2[UNUSED_2_CURSOR_POS] = null
            return data!!
        } else if (STASH != null) {
            for (index in STASH_FOR_BUFFER_SIZE!!.indices) {
                if (STASH_FOR_BUFFER_SIZE!![index] == size && SLOT_IN_USE!![index] > 0) {
                    val data = STASH!![index][--SLOT_IN_USE!![index]]
                    STASH!![index][SLOT_IN_USE!![index]] = null
                    return data!!
                }
            }
        }
        return ByteArray(size)
    }

    @JvmStatic
    @Synchronized
    fun stash(data: ByteArray) {
        if (data.size == 100 && UNUSED_1_CURSOR_POS < 1000) {
            UNUSED_1[UNUSED_1_CURSOR_POS++] = data
        } else if (data.size == 5000 && LOGIN_PACKET_CURSOR_POS < 250) {
            LOGIN_PACKET_BUFFERS[LOGIN_PACKET_CURSOR_POS++] = data
        } else if (data.size == 30000 && UNUSED_2_CURSOR_POS < 50) {
            UNUSED_2[UNUSED_2_CURSOR_POS++] = data
        } else if (STASH != null) {
            for (index in STASH_FOR_BUFFER_SIZE!!.indices) {
                if (data.size == STASH_FOR_BUFFER_SIZE!![index] && SLOT_IN_USE!![index] < STASH!![index].size) {
                    STASH!![index][SLOT_IN_USE!![index]++] = data
                    break
                }
            }
        }
    }
}
