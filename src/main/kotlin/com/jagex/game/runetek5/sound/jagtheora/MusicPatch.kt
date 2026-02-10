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
    @JvmField val musicPatchNodes2 = arrayOfNulls<MusicPatchNode2>(128)
    @JvmField val loopMode = ByteArray(128)
    @JvmField val baseVelocity: Int

    init {
        val patchBuffer = Packet(fileData)

        var i3: Int
        var idx = patchBuffer.currentPosition
        while (patchBuffer.buffer[idx + patchBuffer.currentPosition] != 0.toByte()) {
            idx++
        }
        i3 = idx

        val bytes4 = ByteArray(i3)

        var i5 = 0
        while (i5 < i3) {
            bytes4[i5] = patchBuffer.readByte()
            i5++
        }

        ++patchBuffer.currentPosition
        ++i3
        var i5Pos = patchBuffer.currentPosition
        patchBuffer.currentPosition += i3

        var i6 = 0
        var idx2 = patchBuffer.currentPosition
        while (patchBuffer.buffer[idx2 + patchBuffer.currentPosition] != 0.toByte()) {
            idx2++
        }
        i6 = idx2

        val bytes7 = ByteArray(i6)

        var i8 = 0
        while (i8 < i6) {
            bytes7[i8] = patchBuffer.readByte()
            i8++
        }

        ++patchBuffer.currentPosition
        ++i6
        var i8Pos = patchBuffer.currentPosition
        patchBuffer.currentPosition += i6

        var i9 = 0
        var idx3 = patchBuffer.currentPosition
        while (patchBuffer.buffer[idx3 + patchBuffer.currentPosition] != 0.toByte()) {
            idx3++
        }
        i9 = idx3

        val bytes10 = ByteArray(i9)

        for (i11 in 0 until i9) {
            bytes10[i11] = patchBuffer.readByte()
        }

        ++patchBuffer.currentPosition
        ++i9
        val bytes36 = ByteArray(i9)
        var i12: Int
        var i14: Int
        if (i9 > 1) {
            bytes36[1] = 1
            var i13 = 1
            i12 = 2

            for (i14Tmp in 2 until i9) {
                i14 = i14Tmp
                val i41 = patchBuffer.readUByte()
                if (i41 == 0) {
                    i13 = i12++
                } else {
                    if (i41 <= i13) {
                        (i41 - 1).also { }
                    }
                    i13 = i41
                }

                bytes36[i14] = i13.toByte()
            }
        } else {
            i12 = i9
        }

        val arr37 = arrayOfNulls<MusicPatchNode2>(i12)

        for (i14Tmp in 0 until arr37.size) {
            i14 = i14Tmp
            val class11015 = MusicPatchNode2()
            arr37[i14] = class11015
            var i40 = patchBuffer.readUByte()
            if (i40 > 0) {
                class11015.volumeEnvelopeData = ByteArray(i40 * 2)
            }

            i40 = patchBuffer.readUByte()
            if (i40 > 0) {
                class11015.releaseEnvelopeData = ByteArray(i40 * 2 + 2)
                class11015.releaseEnvelopeData!![1] = 64
            }
        }

        var i14_2 = patchBuffer.readUByte()
        val bytes43 = if (i14_2 > 0) ByteArray(i14_2 * 2) else null
        i14_2 = patchBuffer.readUByte()
        val bytes16 = if (i14_2 > 0) ByteArray(i14_2 * 2) else null

        var i17 = 0
        var idx4 = patchBuffer.currentPosition
        while (patchBuffer.buffer[idx4 + patchBuffer.currentPosition] != 0.toByte()) {
            idx4++
        }
        i17 = idx4

        val bytes18 = ByteArray(i17)

        var i19 = 0
        for (i19Tmp in 0 until i17) {
            bytes18[i19Tmp] = patchBuffer.readByte()
        }

        ++patchBuffer.currentPosition
        ++i17
        i19 = 0

        var i20: Int
        for (i20Tmp in 0 until 128) {
            i20 = i20Tmp
            i19 += patchBuffer.readUByte()
            this.pitchOffset[i20] = i19.toShort()
        }

        i19 = 0

        for (i20Tmp in 0 until 128) {
            i20 = i20Tmp
            i19 += patchBuffer.readUByte()
            this.pitchOffset[i20] = (this.pitchOffset[i20] + (i19 shl 8)).toShort()
        }

        i20 = 0
        var i21 = 0
        var i22 = 0

        var i23: Int
        for (i23Tmp in 0 until 128) {
            i23 = i23Tmp
            if (i20 == 0) {
                if (i21 < bytes18.size) {
                    i20 = bytes18[i21++].toInt()
                } else {
                    i20 = -1
                }

                i22 = patchBuffer.readVarInt()
            }

            this.pitchOffset[i23] = (this.pitchOffset[i23] + ((i22 - 1 and 0x2) shl 14)).toShort()
            this.sampleOffsets[i23] = i22
            --i20
        }

        i20 = 0
        i21 = 0
        i23 = 0

        var i24: Int
        for (i24Tmp in 0 until 128) {
            i24 = i24Tmp
            if (this.sampleOffsets[i24] != 0) {
                if (i20 == 0) {
                    if (i21 < bytes4.size) {
                        i20 = bytes4[i21++].toInt()
                    } else {
                        i20 = -1
                    }

                    i23 = patchBuffer.buffer[i5Pos++].toInt() - 1
                }

                this.loopMode[i24] = i23.toByte()
                --i20
            }
        }

        i20 = 0
        i21 = 0
        i24 = 0

        for (i25 in 0 until 128) {
            if (this.sampleOffsets[i25] != 0) {
                if (i20 == 0) {
                    if (i21 < bytes7.size) {
                        i20 = bytes7[i21++].toInt()
                    } else {
                        i20 = -1
                    }

                    i24 = (patchBuffer.buffer[i8Pos++].toInt() + 16) shl 2
                }

                this.panOffset[i25] = i24.toByte()
                --i20
            }
        }

        i20 = 0
        i21 = 0
        var class11038: MusicPatchNode2? = null

        var i26: Int
        for (i26Tmp in 0 until 128) {
            i26 = i26Tmp
            if (this.sampleOffsets[i26] != 0) {
                if (i20 == 0) {
                    class11038 = arr37[bytes36[i21].toInt()]
                    if (i21 < bytes10.size) {
                        i20 = bytes10[i21++].toInt()
                    } else {
                        i20 = -1
                    }
                }

                this.musicPatchNodes2[i26] = class11038
                --i20
            }
        }

        i20 = 0
        i21 = 0
        i26 = 0

        var i27: Int
        for (i27Tmp in 0 until 128) {
            i27 = i27Tmp
            if (i20 == 0) {
                if (i21 < bytes18.size) {
                    i20 = bytes18[i21++].toInt()
                } else {
                    i20 = -1
                }

                if (this.sampleOffsets[i27] > 0) {
                    i26 = patchBuffer.readUByte() + 1
                }
            }

            this.volumeOffset[i27] = i26.toByte()
            --i20
        }

        this.baseVelocity = patchBuffer.readUByte() + 1

        var i29: Int
        var class11042: MusicPatchNode2?
        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            class11042 = arr37[i27]
            if (class11042?.volumeEnvelopeData != null) {
                i29 = 1
                while (i29 < class11042.volumeEnvelopeData!!.size) {
                    class11042.volumeEnvelopeData!![i29] = patchBuffer.readByte()
                    i29 += 2
                }
            }

            if (class11042?.releaseEnvelopeData != null) {
                i29 = 3
                while (i29 < class11042.releaseEnvelopeData!!.size - 2) {
                    class11042.releaseEnvelopeData!![i29] = patchBuffer.readByte()
                    i29 += 2
                }
            }
        }

        if (bytes43 != null) {
            for (i27Tmp in 0 until 128) {
                i27 = i27Tmp
                if (i27 >= 1 && i27 < bytes43.size) {
                    if (i27 % 2 == 1) {
                        bytes43[i27] = patchBuffer.readByte()
                    }
                }
            }
        }

        if (bytes16 != null) {
            for (i27Tmp in 0 until 128) {
                i27 = i27Tmp
                if (i27 >= 1 && i27 < bytes16.size) {
                    if (i27 % 2 == 1) {
                        bytes16[i27] = patchBuffer.readByte()
                    }
                }
            }
        }

        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            class11042 = arr37[i27]
            if (class11042?.releaseEnvelopeData != null) {
                i19 = 0

                i29 = 2
                while (i29 < class11042.releaseEnvelopeData!!.size) {
                    i19 = 1 + i19 + patchBuffer.readUByte()
                    class11042.releaseEnvelopeData!![i29] = i19.toByte()
                    i29 += 2
                }
            }
        }

        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            class11042 = arr37[i27]
            if (class11042?.volumeEnvelopeData != null) {
                i19 = 0

                i29 = 2
                while (i29 < class11042.volumeEnvelopeData!!.size) {
                    i19 += 1 + patchBuffer.readUByte()
                    class11042.volumeEnvelopeData!![i29] = i19.toByte()
                    i29 += 2
                }
            }
        }

        var b30: Byte
        var i32: Int
        var i33: Int
        var i34: Int
        var b47: Byte
        if (bytes43 != null) {
            i19 = patchBuffer.readUByte()
            bytes43[0] = i19.toByte()

            for (i27Tmp in 0 until 128) {
                i27 = i27Tmp
                if (i27 >= 2 && i27 < bytes43.size && i27 % 2 == 0) {
                    i19 = 1 + i19 + patchBuffer.readUByte()
                    bytes43[i27] = i19.toByte()
                }
            }

            b47 = bytes43[0]
            val b28 = bytes43[1]

            for (i29 in 0 until b47.toInt()) {
                this.volumeOffset[i29] = (b28.toInt() * this.volumeOffset[i29] + 32 shr 6).toByte()
            }

            for (i27Tmp in 0 until 128) {
                i27 = i27Tmp
                if (i27 >= 2 && i27 < bytes43.size && i27 % 2 == 0) {
                    b30 = bytes43[i27]
                    val b31 = bytes43[i27 + 1]
                    i32 = b28.toInt() * (b30.toInt() - b47.toInt()) + (b30.toInt() - b47.toInt()) / 2

                    for (i33 in b47.toInt() until b30.toInt()) {
                        i34 = divideRounded(i32, b30.toInt() - b47.toInt())
                        this.volumeOffset[i33] = (i34 * this.volumeOffset[i33] + 32 shr 6).toByte()
                        i32 += b31.toInt() - b28.toInt()
                    }

                    b47 = b30
                }
            }

            for (i29 in b47.toInt() until 128) {
                this.volumeOffset[i29] = (b28.toInt() * this.volumeOffset[i29] + 32 shr 6).toByte()
            }
        }

        if (bytes16 != null) {
            i19 = patchBuffer.readUByte()
            bytes16[0] = i19.toByte()

            for (i27Tmp in 0 until 128) {
                i27 = i27Tmp
                if (i27 >= 2 && i27 < bytes16.size && i27 % 2 == 0) {
                    i19 = 1 + i19 + patchBuffer.readUByte()
                    bytes16[i27] = i19.toByte()
                }
            }

            b47 = bytes16[0]
            var i44 = (bytes16[1].toInt() shl 1)

            var i45: Int
            for (i29 in 0 until b47.toInt()) {
                i45 = i44 + (this.panOffset[i29].toInt() and 0xff)
                if (i45 < 0) {
                    i45 = 0
                }

                if (i45 > 128) {
                    i45 = 128
                }

                this.panOffset[i29] = i45.toByte()
            }

            for (i27Tmp in 0 until 128) {
                i27 = i27Tmp
                if (i27 >= 2 && i27 < bytes16.size && i27 % 2 == 0) {
                    b30 = bytes16[i27]
                    val i46 = (bytes16[i27 + 1].toInt() shl 1)
                    i32 = i44 * (b30.toInt() - b47.toInt()) + (b30.toInt() - b47.toInt()) / 2

                    for (i33 in b47.toInt() until b30.toInt()) {
                        i34 = divideRounded(i32, b30.toInt() - b47.toInt())
                        var i35 = i34 + (this.panOffset[i33].toInt() and 0xff)
                        if (i35 < 0) {
                            i35 = 0
                        }

                        if (i35 > 128) {
                            i35 = 128
                        }

                        this.panOffset[i33] = i35.toByte()
                        i32 += i46 - i44
                    }

                    b47 = b30
                    i44 = i46
                }
            }

            for (i29 in b47.toInt() until 128) {
                i45 = i44 + (this.panOffset[i29].toInt() and 0xff)
                if (i45 < 0) {
                    i45 = 0
                }

                if (i45 > 128) {
                    i45 = 128
                }

                this.panOffset[i29] = i45.toByte()
            }
        }

        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            arr37[i27]?.let { it.volumeEnvelopeDecay = patchBuffer.readUByte() }
        }

        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            class11042 = arr37[i27]
            if (class11042?.volumeEnvelopeData != null) {
                class11042.volumeEnvelopeSustain = patchBuffer.readUByte()
            }

            if (class11042?.releaseEnvelopeData != null) {
                class11042.vibratoLfoDelay = patchBuffer.readUByte()
            }

            if (class11042?.volumeEnvelopeDecay ?: 0 > 0) {
                class11042!!.vibratoLfoFrequency = patchBuffer.readUByte()
            }
        }

        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            arr37[i27]?.let { it.volumeEnvelopeRelease = patchBuffer.readUByte() }
        }

        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            class11042 = arr37[i27]
            if (class11042?.volumeEnvelopeRelease ?: 0 > 0) {
                class11042?.let { it.vibratoLfoPitch = patchBuffer.readUByte() }
            }
        }

        for (i27Tmp in 0 until i12) {
            i27 = i27Tmp
            class11042 = arr37[i27]
            if (class11042?.vibratoLfoPitch ?: 0 > 0) {
                class11042?.let { it.vibratoDelay = patchBuffer.readUByte() }
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
        fun divideRounded(i0: Int, i1: Int): Int {
            val i3 = i0 ushr 31
            return (i0 + i3) / i1 - i3
        }
    }
}
