package com.jagex.game.runetek5.config.midi.instrument

import java.io.IOException
import java.util.LinkedList

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer
import com.jagex.game.runetek5.utils.BufferUtils
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 *
 * @author Brainpower - 19/04/2022
 * @autor lequietriot - 13/08/2021
 * Reference: https://github.com/lequietriot/RuneScape-MIDI-Player-Suite/blob/master/src/main/MusicSample.java
 *
 */
class MusicSample @Throws(IOException::class) constructor(data: ByteArray) : Node<MusicSample>() {

    @JvmField var sampleRate: Int = 0
    @JvmField var sampleCount: Int = 0
    @JvmField var start: Int = 0
    @JvmField var end: Int = 0
    @JvmField var loopConsistency: Boolean = false
    @JvmField var packets: Array<ByteArray>? = null
    @JvmField var T: FloatArray? = null
    @JvmField var Y: Int = 0
    @JvmField var H: Int = 0
    @JvmField var blockFlag: Boolean = false
    @JvmField var samples: FloatArray? = null
    @JvmField var sampleLength: Int = 0
    @JvmField var soundIndices: Int = 0
    @JvmField var audioBufferLength: Int = 0

    companion object {
        @JvmStatic
        var fullyCached: Boolean = false
        @JvmStatic
        var blockSize0: Int = 0
        @JvmStatic
        var blockSize1: Int = 0
        @JvmStatic
        var window: FloatArray? = null
        @JvmStatic
        var codebooks: Array<MusicSampleCodebook>? = null
        @JvmStatic
        var floors: Array<MusicSampleFloor>? = null
        @JvmStatic
        var residues: Array<MusicSampleResidue>? = null
        @JvmStatic
        var mappings: Array<MusicSamepleMapping>? = null
        @JvmStatic
        var noResidues: BooleanArray? = null
        @JvmStatic
        var modes: IntArray? = null
        @JvmStatic
        var CdR: FloatArray? = null
        @JvmStatic
        var CdP: FloatArray? = null
        @JvmStatic
        var CdV: FloatArray? = null
        @JvmStatic
        var CdAv: IntArray? = null
        @JvmStatic
        var CdAg: FloatArray? = null
        @JvmStatic
        var CdAq: FloatArray? = null
        @JvmStatic
        var CdAj: FloatArray? = null
        @JvmStatic
        var CdAr: IntArray? = null
        @JvmStatic
        var instrumentData: ByteArray? = null
        @JvmStatic
        var byteIndex: Int = 0
        @JvmStatic
        var root: Int = 0
        @JvmField
        val MUSIC_SAMPLES: LinkedList<MusicSample> = LinkedList()

        @JvmStatic
        fun initializeData(data: ByteArray) {
            setData(data)
            blockSize0 = 1 shl getBits(4)
            blockSize1 = 1 shl getBits(4)
            window = FloatArray(blockSize1)

            for (blockType in 0 until 2) {
                val blockSize = if (blockType != 0) blockSize1 else blockSize0
                val halfBlock = blockSize shr 1
                val quarterBlock = blockSize shr 2
                val eightBlock = blockSize shr 3
                val trig1 = FloatArray(halfBlock)
                for (mapping in 0 until quarterBlock) {
                    trig1[mapping * 2] = cos((mapping * 4) * Math.PI / blockSize).toFloat()
                    trig1[mapping * 2 + 1] = -(sin((mapping * 4) * Math.PI / blockSize).toFloat())
                }
                val trig2 = FloatArray(halfBlock)
                for (mapping in 0 until quarterBlock) {
                    trig2[mapping * 2] = cos((mapping * 2 + 1) * Math.PI / (blockSize * 2)).toFloat()
                    trig2[mapping * 2 + 1] = sin((mapping * 2 + 1) * Math.PI / (blockSize * 2)).toFloat()
                }
                val trig3 = FloatArray(quarterBlock)
                for (time in 0 until eightBlock) {
                    trig3[time * 2] = cos((time * 4 + 2) * Math.PI / blockSize).toFloat()
                    trig3[time * 2 + 1] = -(sin((time * 4 + 2) * Math.PI / blockSize).toFloat())
                }
                val ints_15 = IntArray(eightBlock)
                val i_10 = BufferUtils.ilog(eightBlock - 1)
                for (i_11 in 0 until eightBlock) {
                    ints_15[i_11] = method285(i_11, i_10)
                }
                if (blockType != 0) {
                    CdAg = trig1
                    CdAq = trig2
                    CdAj = trig3
                    CdAr = ints_15
                } else {
                    CdR = trig1
                    CdP = trig2
                    CdV = trig3
                    CdAv = ints_15
                }
            }
            val codeBookCount = getBits(8) + 1
            codebooks = Array(codeBookCount) { MusicSampleCodebook() }
            val skip = getBits(6) + 1
            for (residueCount in 0 until skip) {
                getBits(16)
            }
            val floorCount = getBits(6) + 1
            floors = Array(floorCount) { MusicSampleFloor() }
            val residueCount = getBits(6) + 1
            residues = Array(residueCount) { MusicSampleResidue() }
            val mappingCount = getBits(6) + 1
            mappings = Array(mappingCount) { MusicSamepleMapping() }
            val noResidueCount = getBits(6) + 1
            noResidues = BooleanArray(noResidueCount)
            modes = IntArray(noResidueCount)
            for (index in 0 until noResidueCount) {
                noResidues!![index] = getBit() != 0
                getBits(16)
                getBits(16)
                modes!![index] = getBits(8)
            }
            fullyCached = true
        }

        @JvmStatic
        fun setData(bytes_0: ByteArray) {
            instrumentData = bytes_0
            byteIndex = 0
            root = 0
        }

        @JvmStatic
        fun getBit(): Int {
            val bit = instrumentData!![byteIndex].toInt() shr root and 0x1
            ++root
            byteIndex += root shr 3
            root = root and 0x7
            return bit
        }

        @JvmStatic
        fun getBits(bits: Int): Int {
            var bitsRemaining = bits
            var res = 0
            var index = 0
            var bitIndex: Int
            while (bitsRemaining >= 8 - root) {
                bitIndex = 8 - root
                val mask = (1 shl bitIndex) - 1
                res += (instrumentData!![byteIndex].toInt() shr root and mask) shl index
                root = 0
                byteIndex++
                index += bitIndex
                bitsRemaining -= bitIndex
            }

            if (bitsRemaining > 0) {
                bitIndex = (1 shl bitsRemaining) - 1
                res += (instrumentData!![byteIndex].toInt() shr root and bitIndex) shl index
                root += bitsRemaining
            }

            return res
        }

        @JvmStatic
        fun isLoaded(resource: Js5ResourceProvider): Boolean {
            if (!fullyCached) {
                val data = resource.getFile(0, 0)
                if (data == null) {
                    return false
                }
                initializeData(data)
            }
            return true
        }

        @JvmStatic
        fun list(resource: Js5ResourceProvider, fileId: Int): MusicSample? {
            if (!isLoaded(resource)) {
                resource.isFileCached(fileId)
                return null
            }
            val data = resource.getFile(fileId) ?: return null
            var instrument: MusicSample? = null
            try {
                instrument = MusicSample(data)
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
            return instrument
        }

        @JvmStatic
        fun list(resource: Js5ResourceProvider, archiveId: Int, fileId: Int): MusicSample? {
            if (!isLoaded(resource)) {
                resource.isCached(archiveId, fileId)
                return null
            }
            val data = resource.getFile(archiveId, fileId) ?: return null
            var musicSample: MusicSample? = null
            try {
                musicSample = MusicSample(data)
            } catch (ioexception: IOException) {
                ioexception.printStackTrace()
            }
            return musicSample
        }

        /**
         * https://www.cs.cmu.edu/afs/club/usr/cort/code/img2ktx/third_party/stb/stb_vorbis.c
         */
        @JvmStatic
        fun float32Unpack(input: Int): Float {
            var mantissa = input and 0x1fffff
            val sign = input and -2147483648
            val exponent = (input and 0x7fe00000) shr 21
            if (sign != 0) {
                mantissa = -mantissa
            }

            return (mantissa * 2.0.pow(exponent - 788)).toFloat()
        }

        @JvmStatic
        fun method7042() {
            val numSamples = MUSIC_SAMPLES.size
            val sampleIt = MUSIC_SAMPLES.iterator()
            while (sampleIt.hasNext()) {
                val sample = sampleIt.next()
                sample.calculateAudioBufferLength(sample.getSampleLength() + sample.getSampleRate() / (2 * numSamples))
                if (!sample.method12276()) {
                    sampleIt.remove()
                }
            }
        }

        @JvmStatic
        fun method285(i_0: Int, i_1: Int): Int {
            var value = i_0
            var bits = i_1
            var i_3 = 0
            while (bits > 0) {
                i_3 = i_3 shl 1 or (value and 0x1)
                value = value ushr 1
                --bits
            }
            return i_3
        }

        @JvmStatic
        fun addIfAbsent(musicSample: MusicSample) {
            if (!MUSIC_SAMPLES.contains(musicSample)) {
                MUSIC_SAMPLES.add(musicSample)
            }
        }
    }

    init {
        decode(data)
    }

    @Throws(IOException::class)
    fun decode(data: ByteArray) {
        val stream = Packet(data)
        this.sampleRate = stream.readInt()
        this.sampleCount = stream.readInt()
        this.start = stream.readInt()
        this.end = stream.readInt()
        if (this.end < 0) {
            this.end = this.end.inv()
            this.loopConsistency = true
        }
        val packetCount = stream.readInt()
        if (packetCount < 0) {
            throw IOException()
        }
        this.packets = Array(packetCount) { ByteArray(0) }
        for (packet in 0 until packetCount) {
            var size = 0
            var offset: Int
            do {
                offset = stream.readUByte()
                size += offset
            } while (offset >= 255)
            val packetData = ByteArray(size)
            stream.readBytes(packetData, 0, size)
            this.packets!![packet] = packetData
        }
    }

    fun mdctFloatCompute(packetIndex: Int): FloatArray? {
        setData(this.packets!![packetIndex])
        getBit()
        val modeNumber = getBits(BufferUtils.ilog(modes!!.size - 1))
        val blockSizeFlag = noResidues!![modeNumber]
        val blocksize = if (blockSizeFlag) blockSize1 else blockSize0
        var previousWindowFlag = false
        var nextWindowFlag = false
        if (blockSizeFlag) {
            previousWindowFlag = getBit() != 0
            nextWindowFlag = getBit() != 0
        }

        val windowCenter = blocksize shr 1
        val leftWindowStart: Int
        val leftWindowEnd: Int
        val leftN: Int
        if (blockSizeFlag && !previousWindowFlag) {
            leftWindowStart = (blocksize shr 2) - (blockSize0 shr 2)
            leftWindowEnd = (blockSize0 shr 2) + (blocksize shr 2)
            leftN = blockSize0 shr 1
        } else {
            leftWindowStart = 0
            leftWindowEnd = windowCenter
            leftN = blocksize shr 1
        }

        val rightWindowStart: Int
        val rightWindowEnd: Int
        val rightN: Int
        if (blockSizeFlag && !nextWindowFlag) {
            rightWindowStart = blocksize - (blocksize shr 2) - (blockSize0 shr 2)
            rightWindowEnd = (blockSize0 shr 2) + (blocksize - (blocksize shr 2))
            rightN = blockSize0 shr 1
        } else {
            rightWindowStart = windowCenter
            rightWindowEnd = blocksize
            rightN = blocksize shr 1
        }

        val mapping = mappings!![modes!![modeNumber]]
        val submapNumber = mapping.mux
        val floorNumber = mapping.submapFloors[submapNumber]
        val allFloorsEmpty = !floors!![floorNumber].decodeFloor()

        for (submap in 0 until mapping.submaps) {
            val residue = residues!![mapping.submapResidues[submap]]
            val pcm = window
            residue.decodeResidue(pcm!!, blocksize shr 1, allFloorsEmpty)
        }

        if (!allFloorsEmpty) {
            val floor = mapping.mux
            val floorIndex = mapping.submapFloors[floor]
            floors!![floorIndex].computeFloor(window!!, blocksize shr 1)
        }

        if (allFloorsEmpty) {
            for (w in (blocksize shr 1) until blocksize) {
                window!![w] = 0.0f
            }
        } else {
            val halfBlocksize = blocksize shr 1
            val quarterBlocksize = blocksize shr 2
            val eightBlocksize = blocksize shr 3
            val pcm = window!!

            for (i in 0 until halfBlocksize) {
                pcm[i] *= 0.5f
            }

            for (i in halfBlocksize until blocksize) {
                pcm[i] = -pcm[blocksize - i - 1]
            }

            val floats_44 = if (blockSizeFlag) CdAg!! else CdR!!
            val floats_23 = if (blockSizeFlag) CdAq!! else CdP!!
            val floats_24 = if (blockSizeFlag) CdAj!! else CdV!!
            val ints_25 = if (blockSizeFlag) CdAr!! else CdAv!!

            for (i in 0 until quarterBlocksize) {
                val f_27 = pcm[i * 4] - pcm[blocksize - i * 4 - 1]
                val f_28 = pcm[i * 4 + 2] - pcm[blocksize - i * 4 - 3]
                val f_29 = floats_44[i * 2]
                val f_30 = floats_44[i * 2 + 1]
                pcm[blocksize - i * 4 - 1] = f_27 * f_29 - f_28 * f_30
                pcm[blocksize - i * 4 - 3] = f_27 * f_30 + f_28 * f_29
            }

            for (i in 0 until eightBlocksize) {
                val f_27 = pcm[halfBlocksize + i * 4 + 3]
                val f_28 = pcm[halfBlocksize + i * 4 + 1]
                val f_29 = pcm[i * 4 + 3]
                val f_30 = pcm[i * 4 + 1]
                pcm[halfBlocksize + i * 4 + 3] = f_27 + f_29
                pcm[halfBlocksize + i * 4 + 1] = f_28 + f_30
                val f_31 = floats_44[halfBlocksize - 4 - i * 4]
                val f_32 = floats_44[halfBlocksize - 3 - i * 4]
                pcm[i * 4 + 3] = (f_27 - f_29) * f_31 - (f_28 - f_30) * f_32
                pcm[i * 4 + 1] = (f_28 - f_30) * f_31 + (f_27 - f_29) * f_32
            }

            val i_26 = BufferUtils.ilog(blocksize - 1)

            for (i_45 in 0 until i_26 - 3) {
                val i_46 = blocksize shr i_45 + 2
                val i_47 = 8 shl i_45

                for (i_48 in 0 until (2 shl i_45)) {
                    val i_49 = blocksize - i_46 * i_48 * 2
                    val i_50 = blocksize - i_46 * (i_48 * 2 + 1)

                    for (i_33 in 0 until (blocksize shr i_45 + 4)) {
                        val i_34 = i_33 * 4
                        val f_35 = pcm[i_49 - 1 - i_34]
                        val f_36 = pcm[i_49 - 3 - i_34]
                        val f_37 = pcm[i_50 - 1 - i_34]
                        val f_38 = pcm[i_50 - 3 - i_34]
                        pcm[i_49 - 1 - i_34] = f_35 + f_37
                        pcm[i_49 - 3 - i_34] = f_36 + f_38
                        val f_39 = floats_44[i_33 * i_47]
                        val f_40 = floats_44[i_33 * i_47 + 1]
                        pcm[i_50 - 1 - i_34] = (f_35 - f_37) * f_39 - (f_36 - f_38) * f_40
                        pcm[i_50 - 3 - i_34] = (f_36 - f_38) * f_39 + (f_35 - f_37) * f_40
                    }
                }
            }

            for (i_45 in 1 until eightBlocksize - 1) {
                val i_46 = ints_25[i_45]
                if (i_45 < i_46) {
                    val i_47 = i_45 * 8
                    val i_48 = i_46 * 8
                    var tmp = pcm[i_47 + 1]
                    pcm[i_47 + 1] = pcm[i_48 + 1]
                    pcm[i_48 + 1] = tmp
                    tmp = pcm[i_47 + 3]
                    pcm[i_47 + 3] = pcm[i_48 + 3]
                    pcm[i_48 + 3] = tmp
                    tmp = pcm[i_47 + 5]
                    pcm[i_47 + 5] = pcm[i_48 + 5]
                    pcm[i_48 + 5] = tmp
                    tmp = pcm[i_47 + 7]
                    pcm[i_47 + 7] = pcm[i_48 + 7]
                    pcm[i_48 + 7] = tmp
                }
            }

            for (i_45 in 0 until halfBlocksize) {
                pcm[i_45] = pcm[i_45 * 2 + 1]
            }

            for (i_45 in 0 until eightBlocksize) {
                pcm[blocksize - 1 - i_45 * 2] = pcm[i_45 * 4]
                pcm[blocksize - 2 - i_45 * 2] = pcm[i_45 * 4 + 1]
                pcm[blocksize - quarterBlocksize - 1 - i_45 * 2] = pcm[i_45 * 4 + 2]
                pcm[blocksize - quarterBlocksize - 2 - i_45 * 2] = pcm[i_45 * 4 + 3]
            }

            for (i_45 in 0 until eightBlocksize) {
                val f_28 = floats_24[i_45 * 2]
                val f_29 = floats_24[i_45 * 2 + 1]
                val f_30 = pcm[halfBlocksize + i_45 * 2]
                val f_31 = pcm[halfBlocksize + i_45 * 2 + 1]
                val f_32 = pcm[blocksize - 2 - i_45 * 2]
                val f_51 = pcm[blocksize - 1 - i_45 * 2]
                var f_52 = f_29 * (f_30 - f_32) + f_28 * (f_31 + f_51)
                pcm[halfBlocksize + i_45 * 2] = (f_30 + f_32 + f_52) * 0.5f
                pcm[blocksize - 2 - i_45 * 2] = (f_30 + f_32 - f_52) * 0.5f
                f_52 = f_29 * (f_31 + f_51) - f_28 * (f_30 - f_32)
                pcm[halfBlocksize + i_45 * 2 + 1] = (f_31 - f_51 + f_52) * 0.5f
                pcm[blocksize - 1 - i_45 * 2] = (-f_31 + f_51 + f_52) * 0.5f
            }

            for (pos in 0 until quarterBlocksize) {
                pcm[pos] = pcm[halfBlocksize + pos * 2] * floats_23[pos * 2] + pcm[halfBlocksize + pos * 2 + 1] * floats_23[pos * 2 + 1]
                pcm[halfBlocksize - 1 - pos] = pcm[halfBlocksize + pos * 2] * floats_23[pos * 2 + 1] - pcm[halfBlocksize + pos * 2 + 1] * floats_23[pos * 2]
            }

            for (pos in 0 until quarterBlocksize) {
                pcm[pos + (blocksize - quarterBlocksize)] = -pcm[pos]
            }

            for (pos in 0 until quarterBlocksize) {
                pcm[pos] = pcm[quarterBlocksize + pos]
            }

            for (pos in 0 until quarterBlocksize) {
                pcm[quarterBlocksize + pos] = -pcm[quarterBlocksize - pos - 1]
            }

            for (pos in 0 until quarterBlocksize) {
                pcm[halfBlocksize + pos] = pcm[blocksize - pos - 1]
            }

            for (index in leftWindowStart until leftWindowEnd) {
                val f_28 = sin(((index - leftWindowStart) + 0.5) / leftN * 0.5 * Math.PI).toFloat()
                window!![index] *= sin(1.5707963267948966 * f_28 * f_28).toFloat()
            }

            for (index in rightWindowStart until rightWindowEnd) {
                val f_28 = sin(((index - rightWindowStart) + 0.5) / rightN * 0.5 * Math.PI + 1.5707963267948966).toFloat()
                window!![index] *= sin(1.5707963267948966 * f_28 * f_28).toFloat()
            }
        }

        var floats_53: FloatArray? = null
        if (this.Y > 0) {
            val i_20 = blocksize + this.Y shr 2
            floats_53 = FloatArray(i_20)
            var i_21: Int
            if (!this.blockFlag) {
                i_21 = 0
                while (i_21 < this.H) {
                    val i_22 = i_21 + (this.Y shr 1)
                    floats_53[i_21] += this.T!![i_22]
                    i_21++
                }
            }

            if (!allFloorsEmpty) {
                i_21 = leftWindowStart
                while (i_21 < blocksize shr 1) {
                    val i_22 = floats_53.size - (blocksize shr 1) + i_21
                    floats_53[i_22] += window!![i_21]
                    i_21++
                }
            }
        }

        val tmp = this.T
        this.T = window
        window = tmp
        this.Y = blocksize
        this.H = rightWindowEnd - (blocksize shr 1)
        this.blockFlag = allFloorsEmpty
        return floats_53
    }

    fun getAudioBuffer(): AudioBuffer? {
        addIfAbsent(this)
        return if (!method12276() || this.sampleLength > this.sampleRate && this.audioBufferLength > this.sampleRate / GameShell.gameCyclesPerSecond())
            AudioBuffer(this.sampleRate, this.samples!!, this.start, this.end, this.loopConsistency) else null
    }

    fun getAudiobufferLength(len: Int): Int {
        var length = 0
        if (this.samples == null) {
            this.Y = 0
            this.T = FloatArray(blockSize1)
            this.samples = FloatArray(this.sampleCount)
            this.sampleLength = 0
            this.soundIndices = 0
        }

        while (len > this.sampleLength && this.soundIndices < this.packets!!.size) {
            val computed = mdctFloatCompute(this.soundIndices)
            if (computed != null) {
                var count = this.sampleLength
                var total = computed.size
                if (total > this.sampleCount - count) {
                    total = this.sampleCount - count
                }

                for (src in 0 until total) {
                    this.samples!![count++] = computed[src]
                }

                length += count - this.sampleLength
                this.sampleLength = count
            }
            this.soundIndices++
        }
        return length
    }

    fun getSampleLength(): Int {
        return this.sampleLength
    }

    fun method12276(): Boolean {
        return this.sampleLength < this.sampleCount - 1
    }

    fun getSampleRate(): Int {
        return this.sampleRate
    }

    fun calculateAudioBufferLength(len: Int) {
        var length = len
        if (length > this.sampleCount - 1) {
            length = this.sampleCount - 1
        }

        if (length > this.sampleLength) {
            this.audioBufferLength = getAudiobufferLength(length)
        } else {
            this.audioBufferLength = 0
        }

        if (!method12276()) {
            this.T = null
        }
    }
}
