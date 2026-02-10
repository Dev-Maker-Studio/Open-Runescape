package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 *
 * @author Brainpower - 28/03/2022
 * Naming from OpenOSRS
 * Reference: https://www.rune-server.ee/runescape-development/rs-503-client-server/help/650307-742-music-midi-decoding.html
 * Reference: https://www.rune-server.ee/runescape-development/rs-503-client-server/snippets/311669-rs-music-file-structure-conversion.html
 * Reference: https://github.com/open-osrs/runelite
 *
 */
class MusicTrack(rsbytebuffer1: Packet) : Node<MusicTrack>() {

    @JvmField
    var midi: ByteArray? = null
    @JvmField
    var table: IterableNodeMap<ObjectNode>? = null

    init {
        rsbytebuffer1.currentPosition = rsbytebuffer1.buffer.size - 3
        val i_2 = rsbytebuffer1.readUByte()
        val i_3 = rsbytebuffer1.readUShort()
        var i_4 = i_2 * 10 + 14
        rsbytebuffer1.currentPosition = 0
        var i_5 = 0
        var i_6 = 0
        var i_7 = 0
        var i_8 = 0
        var i_9 = 0
        var i_10 = 0
        var i_11 = 0
        var i_12 = 0

        var i_13: Int
        var i_14: Int
        var i_15: Int
        i_13 = 0
        while (i_13 < i_2) {
            i_14 = -1

            while (true) {
                i_15 = rsbytebuffer1.readUByte()
                if (i_15 != i_14) {
                    ++i_4
                }

                i_14 = i_15 and 0xf
                if (i_15 == 7) {
                    break
                }

                if (i_15 == 23) {
                    ++i_5
                } else if (i_14 == 0) {
                    ++i_7
                } else if (i_14 == 1) {
                    ++i_8
                } else if (i_14 == 2) {
                    ++i_6
                } else if (i_14 == 3) {
                    ++i_9
                } else if (i_14 == 4) {
                    ++i_10
                } else if (i_14 == 5) {
                    ++i_11
                } else {
                    if (i_14 != 6) {
                        throw RuntimeException()
                    }

                    ++i_12
                }
            }
            ++i_13
        }

        i_4 += i_5 * 5
        i_4 += (i_7 + i_8 + i_6 + i_9 + i_11) * 2
        i_4 += i_10 + i_12
        i_13 = rsbytebuffer1.currentPosition
        i_14 = i_2 + i_5 + i_6 + i_7 + i_8 + i_9 + i_10 + i_11 + i_12

        for (i_15 in 0 until i_14) {
            rsbytebuffer1.readVarInt()
        }

        i_4 += rsbytebuffer1.currentPosition - i_13
        i_15 = rsbytebuffer1.currentPosition
        var i_16 = 0
        var i_17 = 0
        var i_18 = 0
        var i_19 = 0
        var i_20 = 0
        var i_21 = 0
        var i_22 = 0
        var i_23 = 0
        var i_24 = 0
        var i_25 = 0
        var i_26 = 0
        var i_27 = 0
        var i_28 = 0

        var i_29 = 0
        while (i_29 < i_6) {
            i_28 = i_28 + rsbytebuffer1.readUByte() and 0x7f
            if (i_28 != 0 && i_28 != 32) {
                if (i_28 == 1) {
                    ++i_16
                } else if (i_28 == 33) {
                    ++i_17
                } else if (i_28 == 7) {
                    ++i_18
                } else if (i_28 == 39) {
                    ++i_19
                } else if (i_28 == 10) {
                    ++i_20
                } else if (i_28 == 42) {
                    ++i_21
                } else if (i_28 == 99) {
                    ++i_22
                } else if (i_28 == 98) {
                    ++i_23
                } else if (i_28 == 101) {
                    ++i_24
                } else if (i_28 == 100) {
                    ++i_25
                } else if (i_28 != 64 && i_28 != 65 && i_28 != 120 && i_28 != 121 && i_28 != 123) {
                    ++i_27
                } else {
                    ++i_26
                }
            } else {
                ++i_12
            }
            i_29++
        }

        var i_30 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_26
        var i_31 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_11
        var i_32 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_10
        var i_33 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_9
        var i_34 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_16
        var i_35 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_18
        var i_36 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_20
        var i_37 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_7 + i_8 + i_11
        var i_38 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_7
        var i_39 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_27
        var i_40 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_8
        var i_41 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_17
        var i_42 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_19
        var i_43 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_21
        var i_44 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_12
        var i_45 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_9
        var i_46 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_22
        var i_47 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_23
        var i_48 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_24
        var i_49 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_25
        var i_50 = rsbytebuffer1.currentPosition
        rsbytebuffer1.currentPosition += i_5 * 3
        this.midi = ByteArray(i_4)
        val rsbytebuffer51 = Packet(this.midi!!)
        rsbytebuffer51.writeInt(1297377380)
        rsbytebuffer51.writeInt(6)
        rsbytebuffer51.writeShort(if (i_2 > 1) 1 else 0)
        rsbytebuffer51.writeShort(i_2)
        rsbytebuffer51.writeShort(i_3)
        rsbytebuffer1.currentPosition = i_13
        var i_52 = 0
        var i_53 = 0
        var i_54 = 0
        var i_55 = 0
        var i_56 = 0
        var i_57 = 0
        var i_58 = 0
        var ints_59 = IntArray(128)
        i_28 = 0

        var i_60 = 0
        outerLoop@ while (i_60 < i_2) {
            rsbytebuffer51.writeInt(1297379947)
            rsbytebuffer51.currentPosition += 4
            val i_61 = rsbytebuffer51.currentPosition
            var i_62 = -1

            while (true) {
                while (true) {
                    val i_63 = rsbytebuffer1.readVarInt()
                    rsbytebuffer51.writeFlags(i_63)
                    val i_64 = rsbytebuffer1.buffer[i_29++].toInt() and 0xff
                    val bool65 = i_64 != i_62
                    i_62 = i_64 and 0xf
                    if (i_64 == 7) {
                        if (bool65) {
                            rsbytebuffer51.writeByte(255)
                        }

                        rsbytebuffer51.writeByte(47)
                        rsbytebuffer51.writeByte(0)
                        rsbytebuffer51.writeIntLength(rsbytebuffer51.currentPosition - i_61)
                        i_60++
                        continue@outerLoop
                    }

                    if (i_64 == 23) {
                        if (bool65) {
                            rsbytebuffer51.writeByte(255)
                        }

                        rsbytebuffer51.writeByte(81)
                        rsbytebuffer51.writeByte(3)
                        rsbytebuffer51.writeByte((rsbytebuffer1.buffer[i_50++].toInt() and 0xff))
                        rsbytebuffer51.writeByte((rsbytebuffer1.buffer[i_50++].toInt() and 0xff))
                        rsbytebuffer51.writeByte((rsbytebuffer1.buffer[i_50++].toInt() and 0xff))
                    } else {
                        i_52 = i_52 xor (i_64 shr 4)
                        if (i_62 == 0) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 144)
                            }

                            i_53 += (rsbytebuffer1.buffer[i_37++].toInt() and 0xff)
                            i_54 += (rsbytebuffer1.buffer[i_38++].toInt() and 0xff)
                            rsbytebuffer51.writeByte(i_53 and 0x7f)
                            rsbytebuffer51.writeByte(i_54 and 0x7f)
                        } else if (i_62 == 1) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 128)
                            }

                            i_53 += (rsbytebuffer1.buffer[i_37++].toInt() and 0xff)
                            i_55 += (rsbytebuffer1.buffer[i_40++].toInt() and 0xff)
                            rsbytebuffer51.writeByte(i_53 and 0x7f)
                            rsbytebuffer51.writeByte(i_55 and 0x7f)
                        } else if (i_62 == 2) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 176)
                            }

                            i_28 = i_28 + (rsbytebuffer1.buffer[i_15++].toInt() and 0xff) and 0x7f
                            rsbytebuffer51.writeByte(i_28)
                            val b_66: Byte
                            if (i_28 != 0 && i_28 != 32) {
                                b_66 = when (i_28) {
                                    1 -> rsbytebuffer1.buffer[i_34++]
                                    33 -> rsbytebuffer1.buffer[i_41++]
                                    7 -> rsbytebuffer1.buffer[i_35++]
                                    39 -> rsbytebuffer1.buffer[i_42++]
                                    10 -> rsbytebuffer1.buffer[i_36++]
                                    42 -> rsbytebuffer1.buffer[i_43++]
                                    99 -> rsbytebuffer1.buffer[i_46++]
                                    98 -> rsbytebuffer1.buffer[i_47++]
                                    101 -> rsbytebuffer1.buffer[i_48++]
                                    100 -> rsbytebuffer1.buffer[i_49++]
                                    else -> if (i_28 != 64 && i_28 != 65 && i_28 != 120 && i_28 != 121 && i_28 != 123) {
                                        rsbytebuffer1.buffer[i_39++]
                                    } else {
                                        rsbytebuffer1.buffer[i_30++]
                                    }
                                }
                            } else {
                                b_66 = rsbytebuffer1.buffer[i_44++]
                            }

                            val i_67 = b_66.toInt() + ints_59[i_28]
                            ints_59[i_28] = i_67
                            rsbytebuffer51.writeByte(i_67 and 0x7f)
                        } else if (i_62 == 3) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 224)
                            }

                            i_56 += (rsbytebuffer1.buffer[i_45++].toInt() and 0xff)
                            i_56 += (rsbytebuffer1.buffer[i_33++].toInt() and 0xff) shl 7
                            rsbytebuffer51.writeByte(i_56 and 0x7f)
                            rsbytebuffer51.writeByte(i_56 shr 7 and 0x7f)
                        } else if (i_62 == 4) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 208)
                            }

                            i_57 += (rsbytebuffer1.buffer[i_32++].toInt() and 0xff)
                            rsbytebuffer51.writeByte(i_57 and 0x7f)
                        } else if (i_62 == 5) {
                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 160)
                            }

                            i_53 += (rsbytebuffer1.buffer[i_37++].toInt() and 0xff)
                            i_58 += (rsbytebuffer1.buffer[i_31++].toInt() and 0xff)
                            rsbytebuffer51.writeByte(i_53 and 0x7f)
                            rsbytebuffer51.writeByte(i_58 and 0x7f)
                        } else {
                            if (i_62 != 6) {
                                throw RuntimeException()
                            }

                            if (bool65) {
                                rsbytebuffer51.writeByte(i_52 + 192)
                            }

                            rsbytebuffer51.writeByte((rsbytebuffer1.buffer[i_44++].toInt() and 0xff))
                        }
                    }
                }
            }
        }
    }

    fun buildPatchTable() {
        if (this.table == null) {
            this.table = IterableNodeMap(16)
            val ints_1 = IntArray(16)
            val ints_2 = IntArray(16)
            ints_2[9] = 128
            ints_1[9] = 128
            val class804 = MidiFileReader(this.midi!!)
            val i_5 = class804.getTrackCount()

            var i_6: Int
            for (i_6 in 0 until i_5) {
                class804.getGotoTrack(i_6)
                class804.getTrackLength(i_6)
                class804.markTracePosition(i_6)
            }

            outerLoop@ do {
                while (true) {
                    i_6 = class804.getPrioritizedTrack()
                    val i_7 = class804.trackLengths!![i_6]

                    while (i_7 == class804.trackLengths!![i_6]) {
                        class804.getGotoTrack(i_6)
                        val i_8 = class804.readMessage(i_6)
                        if (i_8 == 1) {
                            class804.setTrackDone()
                            class804.markTracePosition(i_6)
                            continue@outerLoop
                        }

                        val i_9 = i_8 and 0xf0
                        var i_10: Int
                        var i_11: Int
                        var i_12: Int
                        if (i_9 == 176) {
                            i_10 = i_8 and 0xf
                            i_11 = i_8 shr 8 and 0x7f
                            i_12 = i_8 shr 16 and 0x7f
                            if (i_11 == 0) {
                                ints_1[i_10] = (i_12 shl 14) + (ints_1[i_10] and -2080769)
                            }

                            if (i_11 == 32) {
                                ints_1[i_10] = (ints_1[i_10] and -16257) + (i_12 shl 7)
                            }
                        }

                        if (i_9 == 192) {
                            i_10 = i_8 and 0xf
                            i_11 = i_8 shr 8 and 0x7f
                            ints_2[i_10] = i_11 + ints_1[i_10]
                        }

                        if (i_9 == 144) {
                            i_10 = i_8 and 0xf
                            i_11 = i_8 shr 8 and 0x7f
                            i_12 = i_8 shr 16 and 0x7f
                            if (i_12 > 0) {
                                val i_13 = ints_2[i_10]
                                var class282Sub4714 = this.table!!.get(i_13.toLong())
                                if (class282Sub4714 == null) {
                                    class282Sub4714 = ObjectNode(ByteArray(128))
                                    this.table!!.put(class282Sub4714, i_13.toLong())
                                }

                                (class282Sub4714.value as ByteArray)[i_11] = 1
                            }
                        }

                        class804.getTrackLength(i_6)
                        class804.markTracePosition(i_6)
                    }
                }
            } while (!class804.isDone())
        }
    }

    fun clear() {
        this.table = null
    }

    companion object {
        @JvmStatic
        fun loadMusictrack(resource: Js5ResourceProvider, archiveId: Int, fileId: Int): MusicTrack? {
            val data = resource.getFile(archiveId, fileId)
            return if (data == null) null else MusicTrack(Packet(data))
        }
    }
}
