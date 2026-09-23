package com.jagex.game.runetek5.config.defaults

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.DefaultsFile
import com.jagex.game.runetek5.config.UnknownOpcodes
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class EntityDefaults(defaultsResource: Js5ResourceProvider) {

    @JvmField var maximumHits = 4
    @JvmField var hitOffsetsX: IntArray? = null
    @JvmField var hitOffsetsY: IntArray? = null
    @JvmField var maxVisibleHitbars = 4
    @JvmField var maxHitbars = 4
    @JvmField var defaultHitbarHeight = 7
    @JvmField var profilingMesh = -1
    @JvmField var alwaysShowContextMenu = true
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
        decode(Packet(defaultsResource.getFile(DefaultsFile.ENTITY.fileId)!!))
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
                    for (i in hitOffsetsX!!.indices) {
                        hitOffsetsX!![i] = buffer.readShort()
                        hitOffsetsY!![i] = buffer.readShort()
                    }
                    loadedOffsets = true
                }
                2 -> profilingMesh = buffer.readBigSmart()
                3 -> {
                    maximumHits = buffer.readUByte()
                    hitOffsetsX = IntArray(maximumHits)
                    hitOffsetsY = IntArray(maximumHits)
                }
                4 -> alwaysShowContextMenu = false
                5 -> loginInterfaceId = buffer.readUMedium()
                6 -> lobbyWindow = buffer.readUMedium()
                7 -> {
                    recolorPaletteSrc = Array(10) { ShortArray(4) }
                    recolorPaletteDst = Array(10) { Array(4) { ShortArray(0) } }
                    for (paletteIndex in 0..9) {
                        for (colorIndex in 0..3) {
                            var srcColor = buffer.readUShort()
                            if (srcColor == 65535) {
                                srcColor = -1
                            }
                            recolorPaletteSrc!![paletteIndex][colorIndex] = srcColor.toShort()
                            val dstCount = buffer.readUShort()
                            recolorPaletteDst!![paletteIndex][colorIndex] = ShortArray(dstCount)
                            for (dstIndex in 0 until dstCount) {
                                var dstColor = buffer.readUShort()
                                if (dstColor == 65535) {
                                    dstColor = -1
                                }
                                recolorPaletteDst!![paletteIndex][colorIndex][dstIndex] = dstColor.toShort()
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
                14 -> maxVisibleHitbars = buffer.readUByte()
                15 -> defaultHitbarHeight = buffer.readUByte()
                else -> UnknownOpcodes.record(opcode)
            }
        }
    }
}
