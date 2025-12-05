package com.jagex.game.runetek5.config.defaults

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.DefaultsFile
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class EntityDefaults(index1: Js5ResourceProvider) {

    @JvmField var maximumHits = 4
    @JvmField var hitOffsetsX: IntArray? = null
    @JvmField var hitOffsetsY: IntArray? = null
    @JvmField var anInt7030 = 4
    @JvmField var maxHitbars = 4
    @JvmField var anInt7028 = 7
    @JvmField var profilingMesh = -1
    @JvmField var aBool7044 = true
    @JvmField var npcMessagesEnabled = true
    @JvmField var npcMessageDuration = 2
    @JvmField var enablePlayerMessages = true
    @JvmField var playerMessageDuration = 3
    @JvmField var gameWidthDefault = -1
    @JvmField var gameHeightDefault = -1
    @JvmField var loginInterfaceId: Int = 0
    @JvmField var lobbyWindow: Int = 0
    @JvmField var recolorPaletteSrc: Array<ShortArray>? = null
    @JvmField var recolorPaletteDst: Array<Array<ShortArray>>? = null

    init {
        decode(Packet(index1.getFile(DefaultsFile.ENTITY.fileId)!!))
    }

    fun decode(buffer: Packet) {
        var loadedOffsets = false
        while (true) {
            var opcode = buffer.readUByte()
            if (opcode == 0) {
                if (!loadedOffsets) {
                    if (hitOffsetsX == null) {
                        maximumHits = 4
                        hitOffsetsX = IntArray(4)
                        hitOffsetsY = IntArray(4)
                    }
                    for (i in hitOffsetsX!!.indices) {
                        hitOffsetsX!![i] = 0
                        hitOffsetsY!![i] = i * 20
                    }
                }
                return
            }
            when (opcode) {
                1 -> {
                    if (hitOffsetsX == null) {
                        maximumHits = 4
                        hitOffsetsX = IntArray(4)
                        hitOffsetsY = IntArray(4)
                    }
                    for (i_5 in hitOffsetsX!!.indices) {
                        hitOffsetsX!![i_5] = buffer.readShort()
                        hitOffsetsY!![i_5] = buffer.readShort()
                    }
                    loadedOffsets = true
                }
                2 -> profilingMesh = buffer.readBigSmart()
                3 -> {
                    maximumHits = buffer.readUByte()
                    hitOffsetsX = IntArray(maximumHits)
                    hitOffsetsY = IntArray(maximumHits)
                }
                4 -> aBool7044 = false
                5 -> loginInterfaceId = buffer.readUMedium()
                6 -> lobbyWindow = buffer.readUMedium()
                7 -> {
                    recolorPaletteSrc = Array(10) { ShortArray(4) }
                    recolorPaletteDst = Array(10) { Array(4) { ShortArray(0) } }
                    for (i_5 in 0..9) {
                        for (i_6 in 0..3) {
                            var i_7 = buffer.readUShort()
                            if (i_7 == 65535) {
                                i_7 = -1
                            }
                            recolorPaletteSrc!![i_5][i_6] = i_7.toShort()
                            val i_8 = buffer.readUShort()
                            recolorPaletteDst!![i_5][i_6] = ShortArray(i_8)
                            for (i_9 in 0 until i_8) {
                                var i_10 = buffer.readUShort()
                                if (i_10 == 65535) {
                                    i_10 = -1
                                }
                                recolorPaletteDst!![i_5][i_6][i_9] = i_10.toShort()
                            }
                        }
                    }
                }
                8 -> npcMessagesEnabled = false
                9 -> npcMessageDuration = buffer.readUByte()
                10 -> enablePlayerMessages = false
                11 -> playerMessageDuration = buffer.readUByte()
                12 -> {
                    gameWidthDefault = buffer.readUShort()
                    gameHeightDefault = buffer.readUShort()
                }
                13 -> maxHitbars = buffer.readUByte()
                14 -> anInt7030 = buffer.readUByte()
                15 -> anInt7028 = buffer.readUByte()
            }
        }
    }
}
