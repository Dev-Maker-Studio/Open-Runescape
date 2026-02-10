package com.jagex.game.runetek5.entity.pathingentity.player

import com.darkan.bot.Bot
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.ChatMessageType
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.config.sequence.PathingEntitySequence
import com.jagex.game.runetek5.config.sequence.SeqType
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.graphics.hint.HintArrow
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.sound.player.area.AmbientSound
import com.jagex.game.runetek5.vardomain.VarProviders
import java.util.*

object PlayerUpdate {

    @JvmField val HINT_ARROWS: Array<HintArrow?> = arrayOfNulls(8)
    @JvmField val OUT_PLAYER_INDICES: IntArray = IntArray(2048)
    @JvmField val OUT_PLAYER_UPDATE_STATE: ByteArray = ByteArray(2048)
    @JvmField val PLAYER_MOVEMENT_TYPES: ByteArray = ByteArray(2048)
    @JvmField val APPEARANCE_BUFFERS: Array<Packet?> = arrayOfNulls(2048)
    @JvmField val anIntArray2426: IntArray = IntArray(2048)
    @JvmField val anIntArray2435: IntArray = IntArray(2048)
    @JvmField val PLAYER_UPDATES: Array<PlayerUpdateEntry?> = arrayOfNulls(2048)
    @JvmField var MY_PLAYER: PlayerEntity? = null
    @JvmField var MY_PLAYER_PLANE: Int = 0
    @JvmField var players: Array<PlayerEntity?> = arrayOfNulls(2048)
    @JvmField var NUM_OUT_PLAYER_INDICES: Int = 0
    @JvmField var myPlayerIndex: Int = 0
    @JvmField var anInt2431: Int = 0
    @JvmField var anInt2434: Int = 0
    @JvmField var MY_PLAYER_USERNAME: String? = null

    @JvmStatic
    fun decode(buffer: Packet.Bit, size: Int) {
        anInt2434 = 0
        decodeUpdate(buffer)
        decodeUpdateMasks(buffer)
        if (buffer.currentPosition != size) {
            throw RuntimeException("${buffer.currentPosition} $size")
        }
    }

    @JvmStatic
    fun decodeUpdate(bitBuffer: Packet.Bit) {
        var skipCount = 0
        bitBuffer.initBitAccess()
        for (index in 0 until NUM_OUT_PLAYER_INDICES) {
            val playerPtr = OUT_PLAYER_INDICES[index]
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() and 0x1) == 0) {
                if (skipCount > 0) {
                    skipCount--
                    OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                } else {
                    val hasUpdate = bitBuffer.readBits(1)
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer)
                        OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                    } else {
                        decodePositionUpdate(bitBuffer, playerPtr)
                    }
                }
            }
        }
        bitBuffer.finishBitAccess()
        if (skipCount != 0) {
            throw RuntimeException()
        }
        bitBuffer.initBitAccess()
        for (index in 0 until NUM_OUT_PLAYER_INDICES) {
            val playerPtr = OUT_PLAYER_INDICES[index]
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() and 0x1) != 0) {
                if (skipCount > 0) {
                    skipCount--
                    OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                } else {
                    val hasUpdate = bitBuffer.readBits(1)
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer)
                        OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                    } else {
                        decodePositionUpdate(bitBuffer, playerPtr)
                    }
                }
            }
        }
        bitBuffer.finishBitAccess()
        if (skipCount != 0) {
            throw RuntimeException()
        }
        bitBuffer.initBitAccess()
        for (index in 0 until anInt2431) {
            val playerPtr = anIntArray2426[index]
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() and 0x1) != 0) {
                if (skipCount > 0) {
                    skipCount--
                    OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                } else {
                    val hasUpdate = bitBuffer.readBits(1)
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer)
                        OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                    } else if (decodeMovementUpdate(bitBuffer, playerPtr)) {
                        OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                    }
                }
            }
        }
        bitBuffer.finishBitAccess()
        if (skipCount != 0) {
            throw RuntimeException()
        }
        bitBuffer.initBitAccess()
        for (i_3 in 0 until anInt2431) {
            val playerPtr = anIntArray2426[i_3]
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() and 0x1) == 0) {
                if (skipCount > 0) {
                    --skipCount
                    OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                } else {
                    val hasUpdate = bitBuffer.readBits(1)
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer)
                        OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                    } else if (decodeMovementUpdate(bitBuffer, playerPtr)) {
                        OUT_PLAYER_UPDATE_STATE[playerPtr] = (OUT_PLAYER_UPDATE_STATE[playerPtr].toInt() or 0x2).toByte()
                    }
                }
            }
        }
        bitBuffer.finishBitAccess()
        if (skipCount != 0) {
            throw RuntimeException()
        }
        NUM_OUT_PLAYER_INDICES = 0
        anInt2431 = 0
        for (index in 1 until 2048) {
            OUT_PLAYER_UPDATE_STATE[index] = (OUT_PLAYER_UPDATE_STATE[index].toInt() shr 1).toByte()
            val player = players[index]
            if (player != null) {
                OUT_PLAYER_INDICES[++NUM_OUT_PLAYER_INDICES - 1] = index
            } else {
                anIntArray2426[++anInt2431 - 1] = index
            }
        }
    }

    @JvmStatic
    fun decodeUpdateMasks(buffer: Packet.Bit) {
        for (i_2 in 0 until anInt2434) {
            val playerPtr = anIntArray2435[i_2]
            val player = players[playerPtr]
            var updateMasks = buffer.readUByte()
            if ((updateMasks and 0x8) != 0) {
                updateMasks += buffer.readUByte() shl 8
            }
            if ((updateMasks and 0x100) != 0) {
                updateMasks += buffer.readUByte() shl 16
            }
            decodePlayerUpdateMask(buffer, playerPtr, player!!, updateMasks)
        }
    }

    @JvmStatic
    fun decodePlayerUpdateMask(stream: Packet.Bit, slot: Int, player: PlayerEntity, flags: Int) {
        var defaultMovespeed = MovementType.STATIONARY.id

        if ((flags and 0x20000) != 0) {
            player.hue = stream.readByte()
            player.saturation = stream.readByteInverse()
            player.lightness = stream.readByteSubtract()
            player.opacity = stream.readUByteSubtract().toByte()
            player.tintStartCycle = Client.cycles + stream.readUShortAdd()
            player.tintEndCycle = Client.cycles + stream.readUShort()
        }

        if ((flags and 0x400000) != 0) {
            val count = stream.buffer[++stream.currentPosition - 1].toInt() and 0xff
            for (index in 0 until count) {
                val key = stream.readUShortLittle()
                val value = stream.readInt()
                player.playerConfigs.put(key, value)
            }
        }

        if ((flags and 0x200) != 0) {
            var gfxId = stream.readUShort()
            val i_7 = stream.readUIntInverseMiddle()
            if (gfxId == 65535) {
                gfxId = -1
            }

            val i_8 = stream.readUByteSubtract()
            val i_9 = i_8 and 0x7
            var i_10 = i_8 shr 3 and 0xf
            if (i_10 == 15) {
                i_10 = -1
            }

            val bool11 = (i_8 shr 7 and 0x1) == 1
            player.playSpotAnimation(gfxId, i_7, i_9, i_10, bool11, 1)
        }

        if ((flags and 0x20) != 0) {
            player.faceDirection = stream.readUShortAdd()
            if (player.movementStepsLeft == 0) {
                player.turn(player.faceDirection)
                player.faceDirection = -1
            }
        }

        if ((flags and 0x800000) != 0) {
            player.playerConfigs.clear()
            val i_6 = stream.buffer[++stream.currentPosition - 1].toInt() and 0xff

            for (i_7 in 0 until i_6) {
                val i_8 = stream.readUShortLittle()
                val i_9 = stream.readInt()
                player.playerConfigs.put(i_8, i_9)
            }
        }

        if ((flags and 0x10000) != 0) {
            player.reduceAttackPriority = stream.readUByteSubtract() == 1
        }

        if ((flags and 0x100000) != 0) {
            val size = stream.readUByteSubtract()
            val ints_14 = IntArray(size)
            val ints_15 = IntArray(size)

            for (i_9 in 0 until size) {
                var i_10 = stream.readUShortAdd()
                if ((i_10 and 0xc000) == 49152) {
                    val i_19 = stream.readUShortAdd()
                    ints_14[i_9] = i_10 shl 16 or i_19
                } else {
                    ints_14[i_9] = i_10
                }

                ints_15[i_9] = stream.readUShort()
            }

            player.method15797(ints_14, ints_15)
        }

        if ((flags and 0x4000) != 0) {
            val message = stream.readString()
            if (player == MY_PLAYER) {
                ChatBox.writeMessage(ChatMessageType.PUBLIC_CHAT, 0, player.getUsernameWithTitle()!!, player.getDisplayName()!!, player.username!!, message)
            }

            player.sendChat(message, 0, 0)
        }

        if ((flags and 0x80000) != 0) {
            val message = stream.readString()
            val chatFlags = stream.readUByteAdd()
            if ((chatFlags and 0x1) != 0) {
                ChatBox.writeMessage(ChatMessageType.PUBLIC_CHAT, chatFlags, player.getUsernameWithTitle()!!, player.getDisplayName()!!, player.username!!, message)
            }

            player.sendChat(message, 0, 0)
        }

        if ((flags and 0x40000) != 0) {
            var gfxId = stream.readUShortAddLittle()
            val i_7 = stream.readUIntInverseMiddle()
            if (gfxId == 65535) {
                gfxId = -1
            }

            val i_8 = stream.readUByteAdd()
            val i_9 = i_8 and 0x7
            var i_10 = i_8 shr 3 and 0xf
            if (i_10 == 15) {
                i_10 = -1
            }

            val bool11 = (i_8 shr 7 and 0x1) == 1
            player.playSpotAnimation(gfxId, i_7, i_9, i_10, bool11, 2)
        }

        if ((flags and 0x8000) != 0) {
            player.pMapdot = stream.readUByteSubtract() == 1
        }

        if ((flags and 0x40) != 0) {
            val numHits = stream.readUByteSubtract()
            if (numHits > 0) {
                for (i_7 in 0 until numHits) {
                    var soakHitsplatId = -1
                    var soakText = -1
                    var hitText = -1
                    var hitCustomText: String? = null
                    var soakCustomText: String? = null
                    var hitsplatId = stream.readUSmart()
                    when (hitsplatId) {
                        32767 -> {
                            hitsplatId = stream.readUSmart()
                            hitText = stream.readUSmart()
                            soakHitsplatId = stream.readUSmart()
                            soakText = stream.readUSmart()
                        }
                        32766 -> {
                            hitsplatId = -1
                            hitText = stream.readUByteInverse()
                        }
                        32764 -> { // Custom with soaking
                            hitsplatId = stream.readUSmart()
                            hitCustomText = stream.readString()
                            soakHitsplatId = stream.readUSmart()
                            soakCustomText = stream.readString()
                        }
                        32765 -> { // Custom without soaking
                            hitsplatId = stream.readUSmart()
                            hitCustomText = stream.readString()
                        }
                        else -> hitText = stream.readUSmart()
                    }

                    val delay = stream.readUSmart()
                    player.applyHit(hitsplatId, hitText, hitCustomText, soakHitsplatId, soakText, soakCustomText, Client.cycles, delay)
                }
            }

            val hitbarCount = stream.readUByte()
            if (hitbarCount > 0) {
                for (index in 0 until hitbarCount) {
                    val hitbarId = stream.readUSmart()
                    val lifetimeCycles = stream.readUSmart()
                    if (lifetimeCycles != 32767) {
                        val delay = stream.readUSmart()
                        val fromPercentage = stream.readUByteSubtract()
                        val toPercentage = if (lifetimeCycles > 0) stream.readUByteAdd() else fromPercentage
                        player.displayHitbar(hitbarId, Client.cycles, lifetimeCycles, delay, fromPercentage, toPercentage)
                    } else {
                        player.hideHitbar(hitbarId)
                    }
                }
            }
        }

        if ((flags and 0x1) != 0) {
            val appearanceByteCount = stream.readUByteInverse()
            val appearanceData = ByteArray(appearanceByteCount)
            val appearanceBuffer = Packet(appearanceData)
            stream.readBytes(appearanceData, 0, appearanceByteCount)
            APPEARANCE_BUFFERS[slot] = appearanceBuffer
            player.decodeAppearance(appearanceBuffer)
        }

        if ((flags and 0x10) != 0) {
            val movementAnimations = IntArray(MovementType.values().size)

            for (moveType in MovementType.values().indices) {
                movementAnimations[moveType] = stream.readBigSmart()
            }
            Bot.refreshLastAnimated()
            val speed = stream.readUByte()
            animate(player, movementAnimations, speed, false)
        }

        if ((flags and 0x2) != 0) {
            var faceEntity = stream.readUShortAdd()
            if (faceEntity == 65535) {
                faceEntity = -1
            }

            player.faceEntity = faceEntity
        }

        if ((flags and 0x1000) != 0) {
            defaultMovespeed = stream.readByteInverse()
        }

        if ((flags and 0x2000) != 0) {
            val i_6 = stream.readUByteSubtract()
            val animationIds = IntArray(i_6)
            val ints_15 = IntArray(i_6)
            val ints_17 = IntArray(i_6)

            for (i_10 in 0 until i_6) {
                animationIds[i_10] = stream.readBigSmart()
                ints_15[i_10] = stream.readUByteInverse()
                ints_17[i_10] = stream.readUShortAddLittle()
            }

            animate(player, animationIds, ints_15, ints_17)
        }

        if ((flags and 0x4) != 0) {
            PLAYER_MOVEMENT_TYPES[slot] = stream.readByteSubtract()
        }

        if ((flags and 0x200000) != 0) {
            var i_6 = stream.readUShortLittle()
            val i_7 = stream.readUIntLittle()
            if (i_6 == 65535) {
                i_6 = -1
            }

            val i_8 = stream.readUByteSubtract()
            val i_9 = i_8 and 0x7
            var i_10 = i_8 shr 3 and 0xf
            if (i_10 == 15) {
                i_10 = -1
            }

            val bool11 = (i_8 shr 7 and 0x1) == 1
            player.playSpotAnimation(i_6, i_7, i_9, i_10, bool11, 3)
        }

        if ((flags and 0x800) != 0) {
            player.forceMovementT1XOff = stream.readByteInverse().toInt()
            player.forceMovementT1YOff = stream.readByteSubtract().toInt()
            player.forceMovementT2XOff = stream.readByteAdd().toInt()
            player.forceMovementT2YOff = stream.readByteInverse().toInt()
            player.forceMovementT1Delay = stream.readUShortAddLittle() + Client.cycles
            player.forceMovementT2Delay = stream.readUShortLittle() + Client.cycles
            player.forceMoveDirection = stream.readUShortAdd()
            if (player.aBool10568) {
                player.forceMovementT1XOff = player.forceMovementT1XOff + player.basePositionX
                player.forceMovementT1YOff = player.forceMovementT1YOff + player.basePositionY
                player.forceMovementT2XOff = player.forceMovementT2XOff + player.basePositionX
                player.forceMovementT2YOff = player.forceMovementT2YOff + player.basePositionY
                player.movementStepsLeft = 0
            } else {
                player.forceMovementT1XOff = player.forceMovementT1XOff + player.movementBaseX[0]
                player.forceMovementT1YOff = player.forceMovementT1YOff + player.movementBaseY[0]
                player.forceMovementT2XOff = player.forceMovementT2XOff + player.movementBaseX[0]
                player.forceMovementT2YOff = player.forceMovementT2YOff + player.movementBaseY[0]
                player.movementStepsLeft = 1
            }

            player.previousMovementStepsLeft = 0
        }

        if ((flags and 0x80) != 0) {
            var spotAnimId = stream.readUShortLittle()
            val flags1 = stream.readInt()
            if (spotAnimId == 65535) {
                spotAnimId = -1
            }

            val flags2 = stream.readUByteAdd()
            val rotation = flags2 and 0x7
            var i_10 = flags2 shr 3 and 0xf
            if (i_10 == 15) {
                i_10 = -1
            }

            val bool11 = (flags2 shr 7 and 0x1) == 1
            player.playSpotAnimation(spotAnimId, flags1, rotation, i_10, bool11, 0)
        }

        if (player.aBool10568) {
            if (defaultMovespeed == 127.toByte()) {
                player.move(player.basePositionX, player.basePositionY)
            } else {
                val speed: Byte
                speed = if (defaultMovespeed != MovementType.STATIONARY.id) {
                    defaultMovespeed
                } else {
                    PLAYER_MOVEMENT_TYPES[slot]
                }

                animateMovement(player, speed)
                player.move(player.basePositionX, player.basePositionY, speed)
            }
        }

    }

    @JvmStatic
    fun decodePositionUpdate(bitBuffer: Packet.Bit, updateIndex: Int) {
        val baseCoordinates = Resource.mapRegion!!.getBaseCoordinates()
        val bool4 = bitBuffer.readBits(1) == 1
        if (bool4) {
            anIntArray2435[++anInt2434 - 1] = updateIndex
        }
        val size = bitBuffer.readBits(2)
        val player = players[updateIndex]!!
        if (size == 0) {
            if (bool4) {
                player.aBool10568 = false
            } else {
                if (myPlayerIndex == updateIndex) {
                    throw RuntimeException()
                }
                val update = PlayerUpdateEntry().also { PLAYER_UPDATES[updateIndex] = it }
                update.packedCoordinates = (player.plane.toInt() shl 28) +
                        (baseCoordinates.y + player.movementBaseY[0] shr 6) +
                        (baseCoordinates.x + player.movementBaseX[0] shr 6 shl 14)
                if (player.faceDirection != -1) {
                    update.rotation = player.faceDirection
                } else {
                    update.rotation = player.yaw.getRotation()
                }
                update.faceEntity = player.faceEntity
                update.pMapdot = player.pMapdot
                update.reduceAttackPriority = player.reduceAttackPriority
                if (player.isNpc > 0) {
                    AmbientSound.unlinkPlayerSound(player)
                }
                players[updateIndex] = null
                if (bitBuffer.readBits(1) != 0) {
                    decodeMovementUpdate(bitBuffer, updateIndex)
                }
            }
        } else {
            if (size == 1) {
                val direction = bitBuffer.readBits(3)
                var x = player.movementBaseX[0]
                var y = player.movementBaseY[0]
                when (direction) {
                    0 -> {
                        x--
                        y--
                    }
                    1 -> y--
                    2 -> {
                        x++
                        y--
                    }
                    3 -> x--
                    4 -> x++
                    5 -> {
                        x--
                        y++
                    }
                    6 -> y++
                    7 -> {
                        x++
                        y++
                    }
                }
                if (bool4) {
                    player.basePositionX = x
                    player.basePositionY = y
                    player.aBool10568 = true
                } else {
                    player.move(x, y, PLAYER_MOVEMENT_TYPES[updateIndex])
                }
            } else if (size == 2) {
                val direction = bitBuffer.readBits(4)
                var x = player.movementBaseX[0]
                var y = player.movementBaseY[0]
                when (direction) {
                    0 -> {
                        x -= 2
                        y -= 2
                    }
                    1 -> {
                        x--
                        y -= 2
                    }
                    2 -> y -= 2
                    3 -> {
                        x++
                        y -= 2
                    }
                    4 -> {
                        x += 2
                        y -= 2
                    }
                    5 -> {
                        x -= 2
                        y--
                    }
                    6 -> {
                        x += 2
                        y--
                    }
                    7 -> x -= 2
                    8 -> x += 2
                    9 -> {
                        x -= 2
                        y++
                    }
                    10 -> {
                        x += 2
                        y++
                    }
                    11 -> {
                        x -= 2
                        y += 2
                    }
                    12 -> {
                        x--
                        y += 2
                    }
                    13 -> y += 2
                    14 -> {
                        x++
                        y += 2
                    }
                    15 -> {
                        x += 2
                        y += 2
                    }
                }
                if (bool4) {
                    player.basePositionX = x
                    player.basePositionY = y
                    player.aBool10568 = true
                } else {
                    player.move(x, y, PLAYER_MOVEMENT_TYPES[updateIndex])
                }
            } else {
                val direction = bitBuffer.readBits(1)
                if (direction == 0) {
                    val i_8 = bitBuffer.readBits(12)
                    val i_9 = i_8 shr 10
                    var i_10 = i_8 shr 5 and 0x1f
                    if (i_10 > 15) {
                        i_10 -= 32
                    }
                    var i_11 = i_8 and 0x1f
                    if (i_11 > 15) {
                        i_11 -= 32
                    }
                    val baseX = i_10 + player.movementBaseX[0]
                    val baseY = i_11 + player.movementBaseY[0]
                    if (bool4) {
                        player.basePositionX = baseX
                        player.basePositionY = baseY
                        player.aBool10568 = true
                    } else {
                        player.move(baseX, baseY, PLAYER_MOVEMENT_TYPES[updateIndex])
                    }
                    player.plane = ((i_9 + player.plane) and 0x3).toByte()
                    player.collisionPlane = player.plane
                    if (Resource.mapRegion!!.settingsBits.areRoofsHidden(baseX, baseY)) {
                        player.collisionPlane = (player.collisionPlane.toInt() + 1).toByte()
                    }
                    if (myPlayerIndex == updateIndex && player.plane != MY_PLAYER_PLANE.toByte()) {
                        MY_PLAYER_PLANE = player.plane.toInt()
                    }
                } else {
                    val coordinates = bitBuffer.readBits(30)
                    val playerPlane = coordinates shr 28
                    val playerX = coordinates shr 14 and 0x3fff
                    val playerY = coordinates and 0x3fff
                    val baseX = (playerX + baseCoordinates.x + player.movementBaseX[0] and 0x3fff) - baseCoordinates.x
                    val baseY = (playerY + baseCoordinates.y + player.movementBaseY[0] and 0x3fff) - baseCoordinates.y
                    if (bool4) {
                        player.basePositionX = baseX
                        player.basePositionY = baseY
                        player.aBool10568 = true
                    } else {
                        player.move(baseX, baseY, PLAYER_MOVEMENT_TYPES[updateIndex])
                    }
                    player.plane = ((playerPlane + player.plane) and 0x3).toByte()
                    player.collisionPlane = player.plane
                    if (Resource.mapRegion!!.settingsBits.areRoofsHidden(baseX, baseY)) {
                        player.collisionPlane = (player.collisionPlane.toInt() + 1).toByte()
                    }
                    if (myPlayerIndex == updateIndex) {
                        MY_PLAYER_PLANE = player.plane.toInt()
                    }
                }
            }
        }
    }

    @JvmStatic
    fun decodeSkipcount(bitBuffer: Packet.Bit): Int {
        val skipType = bitBuffer.readBits(2)
        val skipCount: Int
        skipCount = when (skipType) {
            0 -> 0
            1 -> bitBuffer.readBits(5)
            2 -> bitBuffer.readBits(8)
            else -> bitBuffer.readBits(11)
        }
        return skipCount
    }

    @JvmStatic
    fun decodeMovementUpdate(bitBuffer: Packet.Bit, playerPtr: Int): Boolean {
        val updateType = bitBuffer.readBits(2)
        if (updateType == 0) {
            if (bitBuffer.readBits(1) != 0) {
                decodeMovementUpdate(bitBuffer, playerPtr)
            }
            val xInRegion = bitBuffer.readBits(6)
            val yInRegion = bitBuffer.readBits(6)
            val bool18 = bitBuffer.readBits(1) == 1
            if (bool18) {
                anIntArray2435[++anInt2434 - 1] = playerPtr
            }
            if (players[playerPtr] != null) {
                throw RuntimeException()
            }
            val update = PLAYER_UPDATES[playerPtr]!!
            val player = PlayerEntity(Resource.mapRegion!!.getScene()).also { players[playerPtr] = it }
            player.playerIndex = playerPtr
            if (APPEARANCE_BUFFERS[playerPtr] != null) {
                player.decodeAppearance(APPEARANCE_BUFFERS[playerPtr]!!)
            }
            player.turn(update.rotation, true)
            player.faceEntity = update.faceEntity
            val coordinates = update.packedCoordinates
            val playerHeight = coordinates shr 28
            val playerX = coordinates shr 14 and 0xff
            val playerY = coordinates and 0xff
            val basePosition = Resource.mapRegion!!.getBaseCoordinates()
            val baseX = xInRegion + (playerX shl 6) - basePosition.x
            val baseY = yInRegion + (playerY shl 6) - basePosition.y
            player.pMapdot = update.pMapdot
            player.reduceAttackPriority = update.reduceAttackPriority
            player.movementTypes[0] = PLAYER_MOVEMENT_TYPES[playerPtr]
            player.plane = playerHeight.toByte()
            player.collisionPlane = playerHeight.toByte()
            if (Resource.mapRegion!!.settingsBits.areRoofsHidden(baseX, baseY)) {
                player.collisionPlane = (player.collisionPlane.toInt() + 1).toByte()
            }
            player.move(baseX, baseY)
            player.aBool10568 = false
            PLAYER_UPDATES[playerPtr] = null
            return true
        } else if (updateType == 1) {
            val playerHeight = bitBuffer.readBits(2)
            val coordinates = PLAYER_UPDATES[playerPtr]!!.packedCoordinates
            PLAYER_UPDATES[playerPtr]!!.packedCoordinates = (coordinates and 0xfffffff) + (((coordinates shr 28) + playerHeight and 0x3) shl 28)
            return false
        } else if (updateType == 2) {
            val bitpacked = bitBuffer.readBits(5)
            val heightOffset = bitpacked shr 3
            val direction = bitpacked and 0x7
            val coordinates = PLAYER_UPDATES[playerPtr]!!.packedCoordinates
            val playerHeight = (coordinates shr 28) + heightOffset and 0x3
            var playerX = coordinates shr 14 and 0xff
            var playerY = coordinates and 0xff
            when (direction) {
                0 -> {
                    playerX--
                    playerY--
                }
                1 -> playerY--
                2 -> {
                    playerX++
                    playerY--
                }
                3 -> playerX--
                4 -> playerX++
                5 -> {
                    playerX--
                    playerY++
                }
                6 -> playerY++
                7 -> {
                    playerX++
                    playerY++
                }
            }
            PLAYER_UPDATES[playerPtr]!!.packedCoordinates = (playerX shl 14) + playerY + (playerHeight shl 28)
        } else {
            val bitpackedPositionOffset = bitBuffer.readBits(18)
            val heightOffset = bitpackedPositionOffset shr 16
            val offsetX = bitpackedPositionOffset shr 8 and 0xff
            val offsetY = bitpackedPositionOffset and 0xff
            val coordinates = PLAYER_UPDATES[playerPtr]!!.packedCoordinates
            val playerHeight = (coordinates shr 28) + heightOffset and 0x3
            val playerX = offsetX + (coordinates shr 14) and 0xff
            val playerY = offsetY + coordinates and 0xff
            PLAYER_UPDATES[playerPtr]!!.packedCoordinates = (playerX shl 14) + playerY + (playerHeight shl 28)
        }
        return false
    }

    @JvmStatic
    fun animate(animable: PathingEntity, animationIds: IntArray, speed: Int, bool3: Boolean) {
        if (animable.currentAnimations != null) {
            var sameSequence = true
            for (index in animable.currentAnimations!!.indices) {
                if (animable.currentAnimations!![index] != animationIds[index]) {
                    sameSequence = false
                    break
                }
            }
            val currSeq: Sequence = animable.currentSequence
            if (sameSequence && currSeq.hasType()) {
                val currSeqType: SeqType = animable.currentSequence.getType()!!
                val speedType = currSeqType.loopType
                if (speedType == 1) {
                    currSeq.reset(speed)
                }
                if (speedType == 2) {
                    currSeq.resetLoops()
                }
            }
        }
        var isDefault = true
        for (index in animationIds.indices) {
            if (animationIds[index] != -1) {
                isDefault = false
            }
            if (animable.currentAnimations == null || animable.currentAnimations!![index] == -1
                    || TypeLists.SEQ!!.list(animationIds[index])!!.priority >= TypeLists.SEQ!!.list(animable.currentAnimations!![index])!!.priority) {
                animable.currentAnimations = animationIds
                animable.currentSequence.setSpeed(speed)
                if (bool3) {
                    animable.previousMovementStepsLeft = animable.movementStepsLeft
                }
            }
        }
        if (isDefault) {
            animable.currentAnimations = animationIds
            animable.currentSequence.setSpeed(speed)
            if (bool3) {
                animable.previousMovementStepsLeft = animable.movementStepsLeft
            }
        }
    }

    @JvmStatic
    fun animateMovement(entity: PathingEntity, movementType: Byte) {
        if (entity.currentAnimations != null) {
            val moveSpeed = Identifiable.identify(MovementType.values(), movementType.toInt()) as MovementType?
            assert(moveSpeed != null)
            val sequence = entity.currentAnimations!![moveSpeed!!.getValueIncr()]
            if (sequence != entity.currentSequence.getId()) {
                entity.currentSequence.animateSequence(sequence, entity.currentSequence.getSpeed())
                entity.previousMovementStepsLeft = entity.movementStepsLeft
            }
        }
    }

    @JvmStatic
    fun animate(animable: PathingEntity, animations: IntArray, ints_2: IntArray, animationFlags: IntArray) {
        for (index in animations.indices) {
            val seqId = animations[index]
            var flags = animationFlags[index]
            val i_8 = ints_2[index]
            var seq = 0
            while (flags != 0 && seq < animable.sequences.size) {
                if ((flags and 0x1) != 0) {
                    if (seqId == -1) {
                        animable.sequences[seq] = null
                    } else {
                        val seqtype = TypeLists.SEQ!!.list(seqId)!!
                        val loopType = seqtype.loopType
                        var entitySequence = animable.sequences[seq]
                        if (entitySequence != null && entitySequence.hasType()) {
                            if (seqId == entitySequence.getId()) {
                                when (loopType) {
                                    0 -> {
                                        animable.sequences[seq] = null
                                        entitySequence = null
                                    }
                                    1 -> {
                                        entitySequence.reset()
                                        entitySequence.anInt10065 = i_8
                                    }
                                    2 -> entitySequence.resetLoops()
                                }
                            } else if (seqtype.priority >= entitySequence.getType()!!.priority) {
                                animable.sequences[seq] = null
                                entitySequence = null
                            }
                        }
                        if (entitySequence == null || !entitySequence.hasType()) {
                            entitySequence = PathingEntitySequence(animable).also { animable.sequences[seq] = it }
                            entitySequence.update(seqId)
                            entitySequence.anInt10065 = i_8
                        }
                    }
                }
                ++seq
                flags = flags ushr 1
            }
        }
    }

    @JvmStatic
    fun movePlayers() {
        val numPlayers = NUM_OUT_PLAYER_INDICES
        val playerIndices = OUT_PLAYER_INDICES
        for (i in 0 until numPlayers) {
            val player = players[playerIndices[i]]
            if (player != null) {
                EntityUpdating.move(player, false)
            }
        }
    }

    @JvmStatic
    fun reset() {
        NUM_OUT_PLAYER_INDICES = 0
        for (index in 0 until 2048) {
            APPEARANCE_BUFFERS[index] = null
            PLAYER_MOVEMENT_TYPES[index] = MovementType.WALKING.id
            PLAYER_UPDATES[index] = null
        }
    }

    @JvmStatic
    fun decodeOwnPlayer(packet: Packet.Bit) {
        packet.initBitAccess()
        val myPlayer = myPlayerIndex
        val player = PlayerEntity(Resource.mapRegion!!.getScene()).also {
            MY_PLAYER = it
            players[myPlayer] = it
        }
        player.playerIndex = myPlayer
        val coordinates = packet.readBits(30)
        val playerPlane = (coordinates shr 28).toByte()
        val playerX = coordinates shr 14 and 0x3fff
        val playerY = coordinates and 0x3fff
        val baseCoordinates = Resource.mapRegion!!.getBaseCoordinates()
        player.movementBaseX[0] = playerX - baseCoordinates.x
        player.movementBaseY[0] = playerY - baseCoordinates.y
        player.setBasePosition(((player.movementBaseX[0] shl 9) + (player.getSize() shl 8)).toFloat(), player.basePosition.coords.y, ((player.movementBaseY[0] shl 9) + (player.getSize() shl 8)).toFloat())
        MY_PLAYER_PLANE = player.plane.toInt()
        player.plane = playerPlane
        player.collisionPlane = playerPlane
        if (Resource.mapRegion!!.settingsBits.areRoofsHidden(player.movementBaseX[0], player.movementBaseY[0])) {
            player.collisionPlane = (player.collisionPlane.toInt() + 1).toByte()
        }
        if (APPEARANCE_BUFFERS[myPlayer] != null) {
            player.decodeAppearance(APPEARANCE_BUFFERS[myPlayer]!!)
        }
        NUM_OUT_PLAYER_INDICES = 0
        OUT_PLAYER_INDICES[++NUM_OUT_PLAYER_INDICES - 1] = myPlayer
        OUT_PLAYER_UPDATE_STATE[myPlayer] = 0
        anInt2431 = 0
        for (updateIndex in 1 until 2048) {
            if (myPlayer != updateIndex) {
                val otherCoordinates = packet.readBits(18)
                val otherPlane = otherCoordinates shr 16
                val otherX = otherCoordinates shr 8 and 0xff
                val otherY = otherCoordinates and 0xff
                val update = PlayerUpdateEntry().also { PLAYER_UPDATES[updateIndex] = it }
                update.packedCoordinates = (otherX shl 14) + otherY + (otherPlane shl 28)
                update.rotation = 0
                update.faceEntity = -1
                update.pMapdot = false
                update.reduceAttackPriority = false
                anIntArray2426[++anInt2431 - 1] = updateIndex
                OUT_PLAYER_UPDATE_STATE[updateIndex] = 0
            }
        }
        packet.finishBitAccess()
    }

    @JvmStatic
    fun method6233() {
        val ints_1 = OUT_PLAYER_INDICES
        var i_2: Int
        i_2 = 0
        while (i_2 < NUM_OUT_PLAYER_INDICES) {
            val player3 = players[ints_1[i_2]]
            if (player3 != null) {
                player3.updateHintdetailsOntick()
            }
            i_2++
        }
        i_2 = 0
        while (i_2 < Client.NPC_UPDATE_INDEX) {
            val long_6 = Client.NPC_UPDATE_INDICES[i_2].toLong()
            val class282Sub475 = Client.NPC_MAP[long_6]
            if (class282Sub475 != null) {
                (class282Sub475.value as PathingEntity).updateHintdetailsOntick()
            }
            i_2++
        }
        if (Client.MAP_LOADING_STAGE == 4) {
            i_2 = 0
            while (i_2 < CutsceneTypeList.CUTSCENE_ENTITIES!!.size) {
                val class758 = CutsceneTypeList.CUTSCENE_ENTITIES!![i_2]
                if (class758.initialized) {
                    class758.getEntity().updateHintdetailsOntick()
                }
                i_2++
            }
        }
    }

    @JvmStatic
    fun method8333(i_0: Int) {
        AmbientSound.update()
        val i_2 = TypeLists.VAR!!.list(i_0)!!.anInt4327
        if (i_2 != 0) {
            val i_3 = VarProviders.PLAYER_VAR_PROVIDER.activeVars[i_0]
            if (i_2 == 5) {
                Client.menuOpenMode = i_3
            }

            if (i_2 == 6) {
                Client.overheadChatFilter = i_3
            }
        }

    }

    @JvmStatic
    fun findRoute(player: PlayerEntity, destX: Int, destY: Int, moveType: Byte) {
        val rbx = player.movementBaseX[0]
        val rby = player.movementBaseY[0]
        if (rbx >= 0 && rbx < Resource.mapRegion!!.sceneSizeX && rby >= 0 && rby < Resource.mapRegion!!.sceneSizeY && destX >= 0 && destX < Resource.mapRegion!!.sceneSizeX && destY >= 0 && destY < Resource.mapRegion!!.sceneSizeY) {
            val i_7 = RouteFinder.findRoute(rbx, rby, player.getSize(), RouteStrategies.forFixedTile(destX, destY), Resource.mapRegion!!.getCollisionMap(player.plane.toInt()), true, Client.WALKING_STEPS_X, Client.WALKING_STEPS_Y)
            if (i_7 in 1..3) {
                for (index in 0 until i_7 - 1) {
                    player.addMovementStep(Client.WALKING_STEPS_X[index], Client.WALKING_STEPS_Y[index], moveType)
                }
            }
        }
    }

    @JvmStatic
    fun animateZero(player: PlayerEntity, size: Int, speed: Int) {
        val animations = IntArray(MovementType.values().size)
        Arrays.fill(animations, 0, animations.size, size)
        animate(player, animations, speed, false)
    }

    @JvmStatic
    fun method11626() {
        val count = NUM_OUT_PLAYER_INDICES
        val playerIndices = OUT_PLAYER_INDICES
        val idleAnimationsSetting = PreferenceManager.preferences!!.idleAnimations!!.getValue()
        val animate = idleAnimationsSetting == 1 && count > 200 || idleAnimationsSetting == 0 && count > 50

        for (i_4 in 0 until count) {
            val player5 = players[playerIndices[i_4]]!!
            if (!player5.hasAppearance()) {
                player5.worldPriority = -1
            } else if (player5.hidden) {
                player5.worldPriority = -1
            } else {
                player5.calculatePosition()
                if (player5.startX >= 0 && player5.startZ >= 0 && player5.endX < Resource.mapRegion!!.sceneSizeX && player5.endZ < Resource.mapRegion!!.sceneSizeY) {
                    player5.aBool10573 = player5.movingSequence.hasType && animate
                    if (player5 == MY_PLAYER) {
                        player5.worldPriority = Integer.MAX_VALUE
                    } else {
                        var priority = 0

                        priority += if (player5.isHidden) 0 else 1
                        priority += if (player5.entityHitbars.isEmpty()) 0 else 2
                        priority += (5 - player5.getSize()) shl 2

                        if (player5.pMapdot || player5.reduceAttackPriority) {
                            priority += 512
                        } else {
                            priority += if (Client.prioritizePlayerDraw) 128 else 32
                            priority += 256
                        }

                        player5.worldPriority = priority + 1
                    }
                } else {
                    player5.worldPriority = -1
                }
            }
        }

        for (npcIndex in 0 until Client.NPC_UPDATE_INDEX) {
            val npc = (Client.NPC_MAP[Client.NPC_UPDATE_INDICES[npcIndex].toLong()]!!.value as NpcEntity)
            if (npc.hasDefinition() && npc.definitions!!.hasTransformations(VarProviders.PLAYER_VAR_PROVIDER)) {
                npc.calculatePosition()
                if (npc.startX >= 0 && npc.startZ >= 0 && npc.endX < Resource.mapRegion!!.sceneSizeX && npc.endZ < Resource.mapRegion!!.sceneSizeY) {
                    var drawPriority = 0
                    if (!npc.isHidden) {
                        drawPriority++
                    }

                    if (!npc.entityHitbars.isEmpty()) {
                        drawPriority += 2
                    }

                    drawPriority += 5 - npc.getSize() shl 2

                    var baseValue = if (Client.prioritizePlayerDraw) 64 else 128
                    if (npc.definitions!!.lowPriority)
                        baseValue /= 2
                    drawPriority += baseValue

                    if (npc.definitions!!.highPriority) {
                        drawPriority += 1024
                    } else if (!npc.definitions!!.mediumPriority) {
                        drawPriority += 256
                    }

                    npc.worldPriority = drawPriority + 1
                } else {
                    npc.worldPriority = -1
                }
            } else {
                npc.worldPriority = -1
            }
        }

        for (arrowIndex in HINT_ARROWS.indices) {
            val arrow = HINT_ARROWS[arrowIndex]
            if (arrow != null) {
                if (arrow.targetType == 1) {
                    val npcObject = Client.NPC_MAP[arrow.targetIndex.toLong()]
                    if (npcObject != null) {
                        val npc = npcObject.value as NpcEntity
                        if (npc.worldPriority >= 0) {
                            npc.worldPriority += 2048
                        }
                    }
                } else if (arrow.targetType == 10) {
                    val player = players[arrow.targetIndex]
                    if (player != null && player != MY_PLAYER && player.worldPriority >= 0) {
                        player.worldPriority += 2048
                    }
                }
            }
        }

    }

    @JvmStatic
    fun updateDrawPriority(plane: Int) {
        val numPlayerIndices = NUM_OUT_PLAYER_INDICES
        val playerIndices = OUT_PLAYER_INDICES
        val drawPriority = Resource.mapRegion!!.drawPriority
        val drawPrioritySharedby = Resource.mapRegion!!.drawPrioritySharedby
        val totalIndices: Int
        totalIndices = if (Client.MAP_LOADING_STAGE == 4) {
            CutsceneTypeList.CUTSCENE_ENTITIES!!.size
        } else {
            numPlayerIndices + Client.NPC_UPDATE_INDEX
        }

        for (index in 0 until totalIndices) {
            val entity: PathingEntity
            if (Client.MAP_LOADING_STAGE == 4) {
                val cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![index]
                if (!cutsceneEntity.initialized) {
                    continue
                }

                entity = cutsceneEntity.getEntity()
            } else {
                entity = if (index < numPlayerIndices) {
                    players[playerIndices[index]]!!
                } else {
                    (Client.NPC_MAP[Client.NPC_UPDATE_INDICES[index - numPlayerIndices].toLong()]!!.value as PathingEntity)
                }

                if (plane != entity.plane.toInt() || entity.worldPriority < 0) {
                    continue
                }
            }

            val size = entity.getSize()
            val abs = entity.getAbsolutePosition().coords
            if ((size and 0x1) == 0) {
                if ((abs.x.toInt() and 0x1ff) != 0 || (abs.z.toInt() and 0x1ff) != 0) {
                    continue
                }
            } else if ((abs.x.toInt() and 0x1ff) != 256 || (abs.z.toInt() and 0x1ff) != 256) {
                continue
            }

            if (size == 1) {
                val x = abs.x.toInt() shr 9
                val z = abs.z.toInt() shr 9
                if (entity.worldPriority > drawPriority[x][z]) {
                    drawPriority[x][z] = entity.worldPriority
                    drawPrioritySharedby[x][z] = 1
                } else if (drawPriority[x][z] == entity.worldPriority) {
                    drawPrioritySharedby[x][z]++
                }
            } else {
                val absSize = (size - 1) * 256 + 60
                val startX = (abs.x.toInt() - absSize) shr 9
                val startZ = (abs.z.toInt() - absSize) shr 9
                val endX = (absSize + abs.x.toInt()) shr 9
                val endZ = (absSize + abs.z.toInt()) shr 9

                for (x in startX..endX) {
                    for (z in startZ..endZ) {
                        if (entity.worldPriority > drawPriority[x][z]) {
                            drawPriority[x][z] = entity.worldPriority
                            drawPrioritySharedby[x][z] = 1
                        } else if (drawPriority[x][z] == entity.worldPriority) {
                            drawPrioritySharedby[x][z]++
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun method11629(plane: Int) {
        val numPlayerIndices = NUM_OUT_PLAYER_INDICES
        val playerIndices = OUT_PLAYER_INDICES
        val drawPriority = Resource.mapRegion!!.drawPriority
        val drawPrioritySharedby = Resource.mapRegion!!.drawPrioritySharedby
        val totalIndices: Int
        totalIndices = if (Client.MAP_LOADING_STAGE == 4) {
            CutsceneTypeList.CUTSCENE_ENTITIES!!.size
        } else {
            if (Client.aBool7241) numPlayerIndices else numPlayerIndices + Client.NPC_UPDATE_INDEX
        }

        for (index in 0 until totalIndices) {
            val entity: PathingEntity
            if (Client.MAP_LOADING_STAGE == 4) {
                val cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![index]
                if (!cutsceneEntity.initialized) {
                    continue
                }

                entity = cutsceneEntity.getEntity()
            } else {
                entity = if (index < numPlayerIndices) {
                    players[playerIndices[index]]!!
                } else {
                    (Client.NPC_MAP[Client.NPC_UPDATE_INDICES[index - numPlayerIndices].toLong()]!!.value as PathingEntity)
                }

                if (plane != entity.plane.toInt()) {
                    continue
                }

                if (entity.worldPriority < 0) {
                    entity.isHidden = false
                    continue
                }
            }

            entity.anInt10319 = 0
            val size = entity.getSize()
            val abs = entity.getAbsolutePosition().coords
            if ((size and 0x1) == 0) {
                if ((abs.x.toInt() and 0x1ff) != 0 || (abs.z.toInt() and 0x1ff) != 0) {
                    entity.isHidden = false
                    continue
                }
            } else if ((abs.x.toInt() and 0x1ff) != 256 || (abs.z.toInt() and 0x1ff) != 256) {
                entity.isHidden = false
                continue
            }

            if (Client.MAP_LOADING_STAGE != 4) {
                if (size == 1) {
                    val x = abs.x.toInt() shr 9
                    val z = abs.z.toInt() shr 9
                    if (drawPriority[x][z] != entity.worldPriority) {
                        entity.isHidden = true
                        continue
                    }

                    if (drawPrioritySharedby[x][z] > 1) {
                        drawPrioritySharedby[x][z]--
                        entity.isHidden = true
                        continue
                    }
                } else {
                    val absSize = (size - 1) * 256 + 252
                    val startX = (abs.x.toInt() - absSize) shr 9
                    val startZ = (abs.z.toInt() - absSize) shr 9
                    val endX = (absSize + abs.x.toInt()) shr 9
                    val endZ = (absSize + abs.z.toInt()) shr 9
                    if (!entityOnTop(drawPriority, drawPrioritySharedby, entity.worldPriority, startX, startZ, endX, endZ)) {
                        for (x in startX..endX) {
                            for (z in startZ..endZ) {
                                if (drawPriority[x][z] == entity.worldPriority) {
                                    drawPrioritySharedby[x][z]--
                                }
                            }
                        }

                        entity.isHidden = true
                        continue
                    }
                }
            }

            entity.isHidden = false
            entity.setBasePosition(abs.x, MapRegion.tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(), entity.plane.toInt()).toFloat(), abs.z)
            Resource.mapRegion!!.getScene()!!.addInteractive(entity, true)
        }

    }

    @JvmStatic
    fun entityOnTop(maxDrawPriority: Array<IntArray>, maxDrawPrioritySharedby: Array<IntArray>, entityDrawPriority: Int, startX: Int, startZ: Int, endX: Int, endZ: Int): Boolean {
        for (x in startX..endX) {
            for (z in startZ..endZ) {
                if (entityDrawPriority == maxDrawPriority[x][z] && maxDrawPrioritySharedby[x][z] <= 1) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun method11619() {
        val numIndices = NUM_OUT_PLAYER_INDICES
        val indices = OUT_PLAYER_INDICES
        val entities: Int
        entities = if (Client.MAP_LOADING_STAGE == 4) {
            CutsceneTypeList.CUTSCENE_ENTITIES!!.size
        } else {
            if (Client.aBool7241) numIndices else numIndices + Client.NPC_UPDATE_INDEX
        }

        for (entityIndex in 0 until entities) {
            val entity: Any
            if (Client.MAP_LOADING_STAGE == 4) {
                val cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![entityIndex]
                if (!cutsceneEntity.initialized) {
                    continue
                }

                entity = cutsceneEntity.getEntity()
            } else {
                entity = if (entityIndex < numIndices) {
                    players[indices[entityIndex]]!!
                } else {
                    (Client.NPC_MAP[Client.NPC_UPDATE_INDICES[entityIndex - numIndices].toLong()]!!.value)!!
                }

                if ((entity as PathingEntity).worldPriority < 0) {
                    continue
                }
            }

            val size = (entity as PathingEntity).getSize()
            val abs = entity.getAbsolutePosition().coords
            if ((size and 0x1) == 0) {
                if ((abs.x.toInt() and 0x1ff) == 0 && (abs.z.toInt() and 0x1ff) == 0) {
                    continue
                }
            } else if ((abs.x.toInt() and 0x1ff) == 256 && (abs.z.toInt() and 0x1ff) == 256) {
                continue
            }

            entity.setBasePosition(abs.x, MapRegion.tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(), entity.plane.toInt()).toFloat(), abs.z)
            Resource.mapRegion!!.getScene()!!.addInteractive(entity as AbstractInteractiveEntity, true)
        }

    }
}
