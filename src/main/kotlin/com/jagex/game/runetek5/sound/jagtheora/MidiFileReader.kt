package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet

/**
 * @author Brainpower - 28/03/2022
 * Naming from OpenOSRS
 *
 */
class MidiFileReader {

    @JvmField
    var division: Int = 0

    @JvmField
    var anInt790: Int = 0

    @JvmField
    var trackStarts: IntArray? = null

    @JvmField
    var aLong786: Long = 0

    @JvmField
    var startPositionTrackBuffer: IntArray? = null

    @JvmField
    var trackLengths: IntArray? = null

    @JvmField
    var anIntArray796: IntArray? = null

    @JvmField
    val buffer = Packet()

    constructor(bytes: ByteArray) {
        decode(bytes)
    }

    constructor()

    fun getTrackLength(i_1: Int) {
        val i_2 = buffer.readVarInt()
        trackLengths!![i_1] += i_2
    }

    fun getPrioritizedTrack(): Int {
        val i_1 = startPositionTrackBuffer!!.size
        var i_2 = -1
        var i_3 = Int.MAX_VALUE

        for (i_4 in 0 until i_1) {
            if (startPositionTrackBuffer!![i_4] >= 0 && trackLengths!![i_4] < i_3) {
                i_2 = i_4
                i_3 = trackLengths!![i_4]
            }
        }

        return i_2
    }

    fun decode(data: ByteArray) {
        buffer.buffer = data
        buffer.currentPosition = 10
        val count = buffer.readUShort()
        division = buffer.readUShort()
        anInt790 = 500000
        trackStarts = IntArray(count)

        var index = 0
        while (index < count) {
            val action = buffer.readInt()
            val offset = buffer.readInt()
            if (action == 1297379947) {
                trackStarts!![index++] = buffer.currentPosition
            }
            buffer.currentPosition += offset
        }

        aLong786 = 0L
        startPositionTrackBuffer = IntArray(count)

        System.arraycopy(trackStarts, 0, startPositionTrackBuffer, 0, count)

        trackLengths = IntArray(count)
        anIntArray796 = IntArray(count)
    }

    fun clear() {
        buffer.buffer = ByteArray(0)
        trackStarts = null
        startPositionTrackBuffer = null
        trackLengths = null
        anIntArray796 = null
    }

    fun getTrackCount(): Int = startPositionTrackBuffer!!.size

    fun getGotoTrack(track: Int) {
        buffer.currentPosition = startPositionTrackBuffer!![track]
    }

    fun setTrackDone() {
        buffer.currentPosition = -1
    }

    fun readMessage(track: Int): Int {
        return decodeTrackFlags(track)
    }

    fun method1403(i_1: Int): Long {
        return aLong786 + i_1.toLong() * anInt790
    }

    fun markTracePosition(i_1: Int) {
        startPositionTrackBuffer!![i_1] = buffer.currentPosition
    }

    fun isDone(): Boolean {
        val i_1 = startPositionTrackBuffer!!.size

        for (i_2 in 0 until i_1) {
            if (startPositionTrackBuffer!![i_2] >= 0) {
                return false
            }
        }

        return true
    }

    fun method1406(long_1: Long) {
        aLong786 = long_1
        val i_3 = startPositionTrackBuffer!!.size

        for (i_4 in 0 until i_3) {
            trackLengths!![i_4] = 0
            anIntArray796!![i_4] = 0
            buffer.currentPosition = trackStarts!![i_4]
            getTrackLength(i_4)
            startPositionTrackBuffer!![i_4] = buffer.currentPosition
        }
    }

    fun dataLoaded(): Boolean = buffer.buffer.isNotEmpty()

    fun decodeTrackFlags(track: Int, initialFlags: Int): Int {
        if (initialFlags == 255) {
            val i_7 = buffer.readUByte()
            var offset = buffer.readVarInt()
            if (i_7 == 47) {
                buffer.currentPosition += offset
                return 1
            } else if (i_7 == 81) {
                val i_5 = buffer.readUMedium()
                offset -= 3
                val length = trackLengths!![track]
                aLong786 += length.toLong() * (anInt790 - i_5)
                anInt790 = i_5
                buffer.currentPosition += offset
                return 2
            } else {
                buffer.currentPosition += offset
                return 3
            }
        }

        val b_3 = aByteArray784[initialFlags - 128]
        var flags = initialFlags

        if (b_3 >= 1) {
            flags = initialFlags or (buffer.readUByte() shl 8)
        }

        if (b_3 >= 2) {
            flags = flags or (buffer.readUByte() shl 16)
        }

        return flags
    }

    fun decodeTrackFlags(track: Int): Int {
        val b_2 = buffer.buffer[buffer.currentPosition]
        val i_5: Int

        if (b_2 < 0) {
            i_5 = b_2.toInt() and 0xff
            anIntArray796!![track] = i_5
            buffer.currentPosition++
        } else {
            i_5 = anIntArray796!![track]
        }

        if (i_5 != 240 && i_5 != 247) {
            return decodeTrackFlags(track, i_5)
        }

        val offset = buffer.readVarInt()
        if (i_5 == 247 && offset > 0) {
            val flags = buffer.buffer[buffer.currentPosition].toInt() and 0xff
            if (flags in 241..243 || flags == 246 || flags == 248 || flags in 250..252 || flags == 254) {
                buffer.currentPosition++
                anIntArray796!![track] = flags
                return decodeTrackFlags(track, flags)
            }
        }

        buffer.currentPosition += offset
        return 0
    }

    companion object {
        @JvmField
        val aByteArray784 = byteArrayOf(
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
    }
}
