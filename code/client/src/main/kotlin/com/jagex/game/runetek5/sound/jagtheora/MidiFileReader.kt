package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet

class MidiFileReader {

    @JvmField
    var division: Int = 0

    @JvmField
    var microsecondsPerBeat: Int = 0

    @JvmField
    var trackStarts: IntArray? = null

    @JvmField
    var tempoTimeOffset: Long = 0

    @JvmField
    var startPositionTrackBuffer: IntArray? = null

    @JvmField
    var trackLengths: IntArray? = null

    @JvmField
    var runningStatus: IntArray? = null

    @JvmField
    val buffer = Packet()

    constructor(bytes: ByteArray) {
        decode(bytes)
    }

    constructor()

    fun getTrackLength(track: Int) {
        val delta = buffer.readVarInt()
        trackLengths!![track] += delta
    }

    fun getPrioritizedTrack(): Int {
        val trackCount = startPositionTrackBuffer!!.size
        var bestTrack = -1
        var earliestLength = Int.MAX_VALUE

        for (track in 0 until trackCount) {
            if (startPositionTrackBuffer!![track] >= 0 && trackLengths!![track] < earliestLength) {
                bestTrack = track
                earliestLength = trackLengths!![track]
            }
        }

        return bestTrack
    }

    fun decode(data: ByteArray) {
        buffer.buffer = data
        buffer.currentPosition = 10
        val count = buffer.readUShort()
        division = buffer.readUShort()
        microsecondsPerBeat = 500000
        trackStarts = IntArray(count)

        var index = 0
        while (index < count) {
            val chunkType = buffer.readInt()
            val chunkLength = buffer.readInt()
            if (chunkType == 1297379947) {
                trackStarts!![index++] = buffer.currentPosition
            }
            buffer.currentPosition += chunkLength
        }

        tempoTimeOffset = 0L
        startPositionTrackBuffer = IntArray(count)

        System.arraycopy(trackStarts, 0, startPositionTrackBuffer, 0, count)

        trackLengths = IntArray(count)
        runningStatus = IntArray(count)
    }

    fun clear() {
        buffer.buffer = ByteArray(0)
        trackStarts = null
        startPositionTrackBuffer = null
        trackLengths = null
        runningStatus = null
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

    fun getTickTimestamp(tickLength: Int): Long {
        return tempoTimeOffset + tickLength.toLong() * microsecondsPerBeat
    }

    fun markTracePosition(track: Int) {
        startPositionTrackBuffer!![track] = buffer.currentPosition
    }

    fun isDone(): Boolean {
        val trackCount = startPositionTrackBuffer!!.size

        for (track in 0 until trackCount) {
            if (startPositionTrackBuffer!![track] >= 0) {
                return false
            }
        }

        return true
    }

    fun resetTracksFromTimestamp(timestamp: Long) {
        tempoTimeOffset = timestamp
        val trackCount = startPositionTrackBuffer!!.size

        for (track in 0 until trackCount) {
            trackLengths!![track] = 0
            runningStatus!![track] = 0
            buffer.currentPosition = trackStarts!![track]
            getTrackLength(track)
            startPositionTrackBuffer!![track] = buffer.currentPosition
        }
    }

    fun dataLoaded(): Boolean = buffer.buffer.isNotEmpty()

    fun decodeTrackFlags(track: Int, statusByte: Int): Int {
        if (statusByte == 255) {
            val metaType = buffer.readUByte()
            var metaLength = buffer.readVarInt()
            if (metaType == 47) {
                buffer.currentPosition += metaLength
                return 1
            } else if (metaType == 81) {
                val newTempo = buffer.readUMedium()
                metaLength -= 3
                val currentLength = trackLengths!![track]
                tempoTimeOffset += currentLength.toLong() * (microsecondsPerBeat - newTempo)
                microsecondsPerBeat = newTempo
                buffer.currentPosition += metaLength
                return 2
            } else {
                buffer.currentPosition += metaLength
                return 3
            }
        }

        val dataByteCount = messageDataByteCount[statusByte - 128]
        var flags = statusByte

        if (dataByteCount >= 1) {
            flags = statusByte or (buffer.readUByte() shl 8)
        }

        if (dataByteCount >= 2) {
            flags = flags or (buffer.readUByte() shl 16)
        }

        return flags
    }

    fun decodeTrackFlags(track: Int): Int {
        val peekByte = buffer.buffer[buffer.currentPosition]
        val statusByte: Int

        if (peekByte < 0) {
            statusByte = peekByte.toInt() and 0xff
            runningStatus!![track] = statusByte
            buffer.currentPosition++
        } else {
            statusByte = runningStatus!![track]
        }

        if (statusByte != 240 && statusByte != 247) {
            return decodeTrackFlags(track, statusByte)
        }

        val sysexLength = buffer.readVarInt()
        if (statusByte == 247 && sysexLength > 0) {
            val flags = buffer.buffer[buffer.currentPosition].toInt() and 0xff
            if (flags in 241..243 || flags == 246 || flags == 248 || flags in 250..252 || flags == 254) {
                buffer.currentPosition++
                runningStatus!![track] = flags
                return decodeTrackFlags(track, flags)
            }
        }

        buffer.currentPosition += sysexLength
        return 0
    }

    companion object {
        @JvmField
        val messageDataByteCount = byteArrayOf(
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
