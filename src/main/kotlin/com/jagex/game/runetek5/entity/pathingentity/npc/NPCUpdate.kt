package com.jagex.game.runetek5.entity.pathingentity.npc

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.sound.player.area.AmbientSound

/**
 * Handles NPC update packets from the server.
 * Manages NPC entity lifecycle, movement, animations, and state updates.
 */
object NPCUpdate {

    @JvmStatic
    fun decode(largeView: Boolean) {
        Client.localNpcCount = 0
        Client.npcListSize = 0
        Client.npcUpdateCycle++

        decodeAddRemove()
        decodeUpdate(largeView)
        decodeMasks()

        var isUnlinked = false
        for (index in 0 until Client.localNpcCount) {
            val npcIndice = Client.LOCAL_NPC_INDICES[index]
            val objectNode = Client.NPC_MAP?.get(npcIndice.toLong()) as? ObjectNode
            val npcEntity = objectNode?.value as? NpcEntity

            if (npcEntity != null && npcEntity.lastUpdate != Client.npcUpdateCycle) {
                if (MiniMenu.isOpen && MiniMenu.containsEntryForNpc(npcIndice)) {
                    MiniMenu.close()
                }
                if (npcEntity.definitions?.hasSoundEffects() == true) {
                    AmbientSound.unlinkEntitySound(npcEntity)
                }
                npcEntity.setNpcType(null)
                objectNode.unlink()
                isUnlinked = true
            }
        }

        if (isUnlinked) {
            val previousNpcCount = Client.npcCount
            Client.npcCount = Client.NPC_MAP?.size() ?: 0
            var npcArrayIndex = 0

            val iterator = Client.NPC_MAP?.iterator()
            if (iterator != null) {
                while (iterator.hasNext()) {
                    val node = iterator.next() as ObjectNode
                    Client.NPC_ARRAY[npcArrayIndex++] = node
                }
            }

            for (npcIndex in Client.npcCount until previousNpcCount) {
                Client.NPC_ARRAY[npcIndex] = null
            }
        }

        require(Client.GAME_CONNECTION_CONTEXT.receivedBuffer.currentPosition == Client.GAME_CONNECTION_CONTEXT.currentPacketSize) {
            "${Client.GAME_CONNECTION_CONTEXT.receivedBuffer.currentPosition} ${Client.GAME_CONNECTION_CONTEXT.currentPacketSize}"
        }

        for (updateIndex in 0 until Client.NPC_UPDATE_INDEX) {
            requireNotNull(Client.NPC_MAP?.get(Client.NPC_UPDATE_INDICES[updateIndex].toLong())) {
                "$updateIndex ${Client.NPC_UPDATE_INDEX}"
            }
        }

        require(Client.npcCount - Client.NPC_UPDATE_INDEX == 0) {
            "${Client.npcCount - Client.NPC_UPDATE_INDEX}"
        }

        for (npcIndex in 0 until Client.npcCount) {
            val pathingEntity = Client.NPC_ARRAY[npcIndex]?.value as? PathingEntity
            require(pathingEntity?.lastUpdate == Client.npcUpdateCycle) {
                "${pathingEntity?.playerIndex}"
            }
        }
    }

    @JvmStatic
    fun decodeAddRemove() {
        val buffer = Client.GAME_CONNECTION_CONTEXT.receivedBuffer
        buffer.initBitAccess()

        val size = buffer.readBits(8)
        if (size < Client.NPC_UPDATE_INDEX) {
            for (i in size until Client.NPC_UPDATE_INDEX) {
                Client.LOCAL_NPC_INDICES[++Client.localNpcCount - 1] = Client.NPC_UPDATE_INDICES[i]
            }
        }

        require(size <= Client.NPC_UPDATE_INDEX)

        Client.NPC_UPDATE_INDEX = 0
        for (i in 0 until size) {
            val updateIndice = Client.NPC_UPDATE_INDICES[i]
            val npc = (Client.NPC_MAP?.get(updateIndice.toLong()) as? ObjectNode)?.value as? NpcEntity
                ?: continue

            val updateRequired = buffer.readBits(1)
            if (updateRequired == 0) {
                Client.NPC_UPDATE_INDICES[++Client.NPC_UPDATE_INDEX - 1] = updateIndice
                npc.lastUpdate = Client.npcUpdateCycle
            } else {
                val moveSpeed = buffer.readBits(2)
                when (moveSpeed) {
                    0 -> {
                        // Standing still
                        Client.NPC_UPDATE_INDICES[++Client.NPC_UPDATE_INDEX - 1] = updateIndice
                        npc.lastUpdate = Client.npcUpdateCycle
                        Client.NPC_INDICES[++Client.npcListSize - 1] = updateIndice
                    }
                    1 -> {
                        Client.NPC_UPDATE_INDICES[++Client.NPC_UPDATE_INDEX - 1] = updateIndice
                        npc.lastUpdate = Client.npcUpdateCycle
                        val movementDirection = Identifiable.identify(NPCDirection.values(), buffer.readBits(3)) as NPCDirection
                        npc.move(movementDirection, MovementType.WALKING.id.toInt())
                        val hasUpdateMask = buffer.readBits(1)
                        if (hasUpdateMask == 1) {
                            Client.NPC_INDICES[++Client.npcListSize - 1] = updateIndice
                        }
                    }
                    2 -> {
                        // General movement
                        Client.NPC_UPDATE_INDICES[++Client.NPC_UPDATE_INDEX - 1] = updateIndice
                        npc.lastUpdate = Client.npcUpdateCycle
                        if (buffer.readBits(1) == 1) {
                            val firstMovementDirection = Identifiable.identify(NPCDirection.values(), buffer.readBits(3)) as NPCDirection
                            npc.move(firstMovementDirection, MovementType.RUNNING.id.toInt())
                            val secondaryMovementDirection = Identifiable.identify(NPCDirection.values(), buffer.readBits(3)) as NPCDirection
                            npc.move(secondaryMovementDirection, MovementType.RUNNING.id.toInt())
                        } else {
                            val movementDirection = Identifiable.identify(NPCDirection.values(), buffer.readBits(3)) as NPCDirection
                            npc.move(movementDirection, MovementType.HALF_WALK.id.toInt())
                        }
                        val hasUpdateMask = buffer.readBits(1)
                        if (hasUpdateMask == 1) {
                            Client.NPC_INDICES[++Client.npcListSize - 1] = updateIndice
                        }
                    }
                    3 -> {
                        // Teleport
                        Client.LOCAL_NPC_INDICES[++Client.localNpcCount - 1] = updateIndice
                    }
                }
            }
        }
    }

    @JvmStatic
    fun decodeUpdate(largeView: Boolean) {
        val bitBuffer = Client.GAME_CONNECTION_CONTEXT.receivedBuffer
        while (bitBuffer.readableBits(Client.GAME_CONNECTION_CONTEXT.currentPacketSize) >= 15) {
            val npcIndex = bitBuffer.readBits(15)
            if (npcIndex == 32767) {
                break
            }

            var requiresRotating = false
            var cachedNpc = Client.NPC_MAP?.get(npcIndex.toLong()) as? ObjectNode
            val npcEntity: NpcEntity

            if (cachedNpc == null) {
                npcEntity = NpcEntity(Resource.mapRegion?.getScene())
                npcEntity.playerIndex = npcIndex
                cachedNpc = ObjectNode(npcEntity)
                Client.NPC_MAP?.put(cachedNpc, npcIndex.toLong())
                Client.NPC_ARRAY[++Client.npcCount - 1] = cachedNpc
                requiresRotating = true
            } else {
                npcEntity = cachedNpc.value as NpcEntity
            }

            Client.NPC_UPDATE_INDICES[++Client.NPC_UPDATE_INDEX - 1] = npcIndex
            npcEntity.lastUpdate = Client.npcUpdateCycle

            val definitions = npcEntity.definitions
            if (definitions != null && definitions.hasSoundEffects()) {
                AmbientSound.unlinkEntitySound(npcEntity)
            }

            val add = bitBuffer.readBits(1)
            if (add == 1) {
                Client.NPC_INDICES[++Client.npcListSize - 1] = npcIndex
            }

            val offY = if (largeView) {
                val raw = bitBuffer.readBits(8)
                if (raw > 127) raw - 256 else raw
            } else {
                val raw = bitBuffer.readBits(5)
                if (raw > 15) raw - 32 else raw
            }

            val turnRotations = (bitBuffer.readBits(3) + 4 shl 11) and 0x3fff
            npcEntity.setNpcType(TypeLists.NPC?.list(bitBuffer.readBits(15)))

            val offX = if (largeView) {
                val raw = bitBuffer.readBits(8)
                if (raw > 127) raw - 256 else raw
            } else {
                val raw = bitBuffer.readBits(5)
                if (raw > 15) raw - 32 else raw
            }

            val isStationary = bitBuffer.readBits(1)
            val npcPlane = bitBuffer.readBits(2)

            npcEntity.setSize(npcEntity.definitions?.size ?: 1)
            npcEntity.turnDirection = (npcEntity.definitions?.turnDirection ?: 0) shl 3

            if (requiresRotating) {
                npcEntity.turn(turnRotations, true)
            }

            npcEntity.move(
                npcPlane,
                PlayerUpdate.MY_PLAYER!!.movementBaseX[0] + offX,
                PlayerUpdate.MY_PLAYER!!.movementBaseY[0] + offY,
                isStationary == 1,
                npcEntity.getSize()
            )

            if (npcEntity.definitions?.hasSoundEffects() == true) {
                AmbientSound.createAmbientSoundEffect(
                    npcEntity.plane.toInt(),
                    npcEntity.movementBaseX[0],
                    npcEntity.movementBaseY[0],
                    0,
                    null,
                    npcEntity,
                    null
                )
            }
        }
        bitBuffer.finishBitAccess()
    }

    @JvmStatic
    fun decodeMasks() {
        val buffer = Client.GAME_CONNECTION_CONTEXT.receivedBuffer

        for (listIndex in 0 until Client.npcListSize) {
            val npcIndice = Client.NPC_INDICES[listIndex]
            val npc = (Client.NPC_MAP?.get(npcIndice.toLong()) as? ObjectNode)?.value as? NpcEntity
                ?: continue

            var updateFlag = buffer.readUByte()
            if ((updateFlag and 0x40) != 0) {
                updateFlag += buffer.readUByte() shl 8
            }
            if ((updateFlag and 0x2000) != 0) {
                updateFlag += buffer.readUByte() shl 16
            }
            if ((updateFlag and 0x100000) != 0) {
                updateFlag += buffer.readUByte() shl 24
            }

            // Movement animations
            if ((updateFlag and 0x10) != 0) {
                val movementAnimations = IntArray(MovementType.values().size)
                for (index in movementAnimations.indices) {
                    movementAnimations[index] = buffer.readBigSmart()
                }
                val speed = buffer.readUByte()
                PlayerUpdate.animate(npc, movementAnimations, speed, true)
            }

            // Customize
            if ((updateFlag and UpdateFlag.CUSTOMIZE) != 0) {
                val modelCount = npc.definitions?.modelIds?.size ?: 0
                val colorAdjustments = npc.definitions?.recolorDst?.size ?: 0
                val textureAdjustments = npc.definitions?.retextureDst?.size ?: 0
                val actionFlags = buffer.readUByteSubtract()

                if ((actionFlags and 0x1) == 1) {
                    npc.customisation = null
                } else {
                    var models: IntArray? = null
                    if ((actionFlags and 0x2) == 2) {
                        models = IntArray(modelCount)
                        for (modelIndex in 0 until modelCount) {
                            models[modelIndex] = buffer.readBigSmart()
                        }
                    }

                    var colors: ShortArray? = null
                    if ((actionFlags and 0x4) == 4) {
                        colors = ShortArray(colorAdjustments)
                        for (colorIndex in 0 until colorAdjustments) {
                            colors[colorIndex] = buffer.readUShort().toShort()
                        }
                    }

                    var retextures: ShortArray? = null
                    if ((actionFlags and 0x8) == 8) {
                        retextures = ShortArray(textureAdjustments)
                        for (textureIndex in 0 until textureAdjustments) {
                            retextures[textureIndex] = buffer.readUShort().toShort()
                        }
                    }

                    val uid = npcIndice.toLong() or ((++npc.meshModifierCount - 1).toLong() shl 32)
                    npc.customisation = NPCTypeCustomisation(uid, models ?: IntArray(0), colors, retextures)
                }
            }

            // Force move
            if ((updateFlag and UpdateFlag.FORCE_MOVE) != 0) {
                npc.forceMovementT1XOff = buffer.readByteSubtract().toInt()
                npc.forceMovementT1YOff = buffer.readByteAdd().toInt()
                npc.forceMovementT2XOff = buffer.readByteInverse().toInt()
                npc.forceMovementT2YOff = buffer.readByteAdd().toInt()
                npc.forceMovementT1Delay = buffer.readUShortLittle() + Client.cycles
                npc.forceMovementT2Delay = buffer.readUShortLittle() + Client.cycles
                npc.forceMoveDirection = buffer.readUShortAddLittle()
                npc.forceMovementT1XOff = npc.forceMovementT1XOff + npc.movementBaseX[0]
                npc.forceMovementT1YOff = npc.forceMovementT1YOff + npc.movementBaseY[0]
                npc.forceMovementT2XOff = npc.forceMovementT2XOff + npc.movementBaseX[0]
                npc.forceMovementT2YOff = npc.forceMovementT2YOff + npc.movementBaseY[0]
                npc.movementStepsLeft = 1
                npc.previousMovementStepsLeft = 0
            }

            // Set BAS
            if ((updateFlag and UpdateFlag.SET_BAS) != 0) {
                npc.currRenderAnim = buffer.readUShort()
                if (npc.currRenderAnim == 0xffff) {
                    npc.currRenderAnim = -1
                }
            }

            // Set VARN
            if ((updateFlag and UpdateFlag.SET_VARN) != 0) {
                val count = buffer.buffer[++buffer.currentPosition - 1].toInt() and 0xff
                for (index in 0 until count) {
                    val id = buffer.readUShortAddLittle()
                    val value = buffer.readUIntMiddle()
                    npc.varns.setVarn(id, value)
                }
            }

            // Play GFX
            if ((updateFlag and UpdateFlag.PLAY_GFX) != 0) {
                var spotAnimId = buffer.readUShortAdd()
                val bitpackedSpeedHeight = buffer.readUIntInverseMiddle()
                if (spotAnimId == 0xffff) {
                    spotAnimId = -1
                }
                val spotAnimFlags = buffer.readUByteAdd()
                val spotAnimRotation = spotAnimFlags and 0x7
                val spotAnimSlot = (spotAnimFlags shr 3) and 0xf
                val slotId = if (spotAnimSlot == 15) -1 else spotAnimSlot
                val adjustReplaymode = ((spotAnimFlags shr 7) and 0x1) == 1
                npc.playSpotAnimation(spotAnimId, bitpackedSpeedHeight, spotAnimRotation, slotId, adjustReplaymode, 3)
            }

            // Face entity
            if ((updateFlag and UpdateFlag.FACE_ENTITY) != 0) {
                npc.faceEntity = buffer.readUShortLittle()
                if (npc.faceEntity == 65535) {
                    npc.faceEntity = -1
                }
            }

            // Animate
            if ((updateFlag and UpdateFlag.ANIMATE) != 0) {
                val count = buffer.readUByteInverse()
                val sequences = IntArray(count)
                val speeds = IntArray(count)
                val slotMasks = IntArray(count)
                for (index in 0 until count) {
                    sequences[index] = buffer.readBigSmart()
                    speeds[index] = buffer.readUByte()
                    slotMasks[index] = buffer.readUShortLittle()
                }
                PlayerUpdate.animate(npc, sequences, speeds, slotMasks)
            }

            // Spot anim (0x800)
            if ((updateFlag and 0x800) != 0) {
                var gfxId = buffer.readUShortLittle()
                val bitpackedSpeedHeight = buffer.readInt()
                if (gfxId == 0xffff) {
                    gfxId = -1
                }
                val spotAnimFlags = buffer.readUByteAdd()
                val spotAnimRotation = spotAnimFlags and 0x7
                val spotAnimSlot = (spotAnimFlags shr 3) and 0xf
                val slotId = if (spotAnimSlot == 15) -1 else spotAnimSlot
                val adjustReplaymode = ((spotAnimFlags shr 7) and 0x1) == 1
                npc.playSpotAnimation(gfxId, bitpackedSpeedHeight, spotAnimRotation, slotId, adjustReplaymode, 1)
            }

            // Add hitsplat
            if ((updateFlag and UpdateFlag.ADD_HITSPLAT) != 0) {
                val hitCount = buffer.readUByteAdd()
                if (hitCount > 0) {
                    for (index in 0 until hitCount) {
                        var soakHitsplatId = -1
                        var hitText = -1
                        var soakText = -1
                        var hitCustomText: String? = null
                        var soakCustomText: String? = null
                        var hitsplatId = buffer.readUSmart()

                        when (hitsplatId) {
                            32767 -> { // Soaking
                                hitsplatId = buffer.readUSmart()
                                hitText = buffer.readUSmart()
                                soakHitsplatId = buffer.readUSmart()
                                soakText = buffer.readUSmart()
                            }
                            32766 -> { // Missed
                                hitsplatId = -1
                                hitText = buffer.readUByte()
                            }
                            32764 -> { // Custom with soaking
                                hitsplatId = buffer.readUSmart()
                                hitCustomText = buffer.readString()
                                soakHitsplatId = buffer.readUSmart()
                                soakCustomText = buffer.readString()
                            }
                            32765 -> { // Custom without soaking
                                hitsplatId = buffer.readUSmart()
                                hitCustomText = buffer.readString()
                            }
                            else -> hitText = buffer.readUSmart()
                        }

                        val delay = buffer.readUSmart()
                        npc.applyHit(hitsplatId, hitText, hitCustomText, soakHitsplatId, soakText, soakCustomText, Client.cycles, delay)
                    }
                }

                val hitbarCount = buffer.readUByteAdd()
                if (hitbarCount > 0) {
                    for (index in 0 until hitbarCount) {
                        val hitbarId = buffer.readUSmart()
                        val lifetimeCycles = buffer.readUSmart()
                        if (lifetimeCycles != 32767) {
                            val delay = buffer.readUSmart()
                            val fromPercentage = buffer.readUByteSubtract()
                            val toPercentage = if (lifetimeCycles > 0) buffer.readUByteAdd() else fromPercentage
                            npc.displayHitbar(hitbarId, Client.cycles, lifetimeCycles, delay, fromPercentage, toPercentage)
                        } else {
                            npc.hideHitbar(hitbarId)
                        }
                    }
                }
            }

            // Transform (0x8)
            if ((updateFlag and 0x8) != 0) {
                if (npc.definitions?.hasSoundEffects() == true) {
                    AmbientSound.unlinkEntitySound(npc)
                }
                npc.setNpcType(TypeLists.NPC?.list(buffer.readBigSmart()))
                npc.setSize(npc.definitions?.size ?: 1)
                npc.turnDirection = (npc.definitions?.turnDirection ?: 0) shl 3
                if (npc.definitions?.hasSoundEffects() == true) {
                    AmbientSound.createAmbientSoundEffect(npc.plane.toInt(), npc.movementBaseX[0], npc.movementBaseY[0], 0, null, npc, null)
                }
            }

            // Spot anim (0x2000000)
            if ((updateFlag and 0x2000000) != 0) {
                var spotanimId = buffer.readUShortLittle()
                val flags = buffer.readUIntLittle()
                if (spotanimId == 65535) {
                    spotanimId = -1
                }
                val bitpacked = buffer.readUByte()
                val rotation = bitpacked and 0x7
                val spotAnimSlot = (bitpacked shr 3) and 0xf
                val slotId = if (spotAnimSlot == 15) -1 else spotAnimSlot
                val adjustReplaymode = ((bitpacked shr 7) and 0x1) == 1
                npc.playSpotAnimation(spotanimId, flags, rotation, slotId, adjustReplaymode, 2)
            }

            // Update stats
            if ((updateFlag and UpdateFlag.UPDATE_STATS) != 0) {
                val configCount = buffer.buffer[++buffer.currentPosition - 1].toInt() and 0xff
                for (index in 0 until configCount) {
                    val statIndex = buffer.readUByteAdd()
                    val max = buffer.readInt()
                    val current = buffer.readUMediumAlternateOrder()
                    npc.updateStats(statIndex, max, current)
                }
            }

            // Head customization (0x40000)
            if ((updateFlag and 0x40000) != 0) {
                val headmodelCount = npc.definitions?.headModels?.size ?: 0
                var adjustmentLength = npc.definitions?.recolorDst?.size ?: 0
                val total = npc.definitions?.retextureDst?.size ?: 0

                val actionFlag = buffer.readUByteAdd()
                if ((actionFlag and 0x1) != 1) {
                    var models: IntArray? = null
                    if ((actionFlag and 0x2) == 2) {
                        models = IntArray(headmodelCount)
                        for (modelIndex in 0 until headmodelCount) {
                            models[modelIndex] = buffer.readBigSmart()
                        }
                    }

                    var recolorDst: ShortArray? = null
                    if ((actionFlag and 0x4) == 4) {
                        recolorDst = ShortArray(adjustmentLength)
                        for (colorIndex in 0 until adjustmentLength) {
                            recolorDst[colorIndex] = buffer.readUShort().toShort()
                        }
                    }

                    var retextureDst: ShortArray? = null
                    if ((actionFlag and 0x8) == 8) {
                        retextureDst = ShortArray(total)
                    }

                    val uid = npcIndice.toLong() or ((++npc.headMeshModifierCount - 1).toLong() shl 32)
                    // Note: Creating object but not assigning - matches original Java code
                    NPCTypeCustomisation(uid, models ?: IntArray(0), recolorDst, retextureDst)
                }
            }

            // Update combat level
            if ((updateFlag and UpdateFlag.UPDATE_CB_LVL) != 0) {
                npc.modifiedCbLvl = buffer.readUShortAdd()
                if (npc.modifiedCbLvl == 0xffff) {
                    npc.modifiedCbLvl = npc.definitions?.combatLevel ?: 0
                }
            }

            // Clear varns (0x400000)
            if ((updateFlag and 0x400000) != 0) {
                npc.varns.clear()
                val varnCount = buffer.buffer[++buffer.currentPosition - 1].toInt() and 0xff
                for (varnIndex in 0 until varnCount) {
                    val varnId = buffer.readUShort()
                    val varnValue = buffer.readUIntMiddle()
                    npc.varns.setVarn(varnId, varnValue)
                }
            }

            // Face coordinates
            if ((updateFlag and 0x4) != 0) {
                npc.faceX = buffer.readUShortAddLittle()
                npc.faceY = buffer.readUShortLittle()
            }

            // Tint (0x20000)
            if ((updateFlag and 0x20000) != 0) {
                npc.hue = buffer.readByteSubtract()
                npc.saturation = buffer.readByte()
                npc.lightness = buffer.readByte()
                npc.opacity = buffer.readUByteSubtract().toByte()
                npc.tintStartCycle = Client.cycles + buffer.readUShortLittle()
                npc.tintEndCycle = Client.cycles + buffer.readUShort()
            }

            // Modified name
            if ((updateFlag and 0x800000) != 0) {
                npc.modifiedName = buffer.readString()
                check(npc.modifiedName != null)
                if (npc.modifiedName!!.isEmpty() || npc.modifiedName == npc.definitions?.name) {
                    npc.modifiedName = npc.definitions?.name
                }
            }

            // Say text (0x2)
            if ((updateFlag and 0x2) != 0) {
                npc.setOverheadText(buffer.readString())
            }

            // Unknown (0x4000)
            if ((updateFlag and 0x4000) != 0) {
                val equipCount = buffer.readUByteSubtract()
                val equipTargets = IntArray(equipCount)
                val equipSlotMasks = IntArray(equipCount)
                for (index in 0 until equipCount) {
                    val targetValue = buffer.readUShortAddLittle()
                    equipTargets[index] = if ((targetValue and 0xc000) == 49152) {
                        val extendedTarget = buffer.readUShort()
                        (targetValue shl 16) or extendedTarget
                    } else {
                        targetValue
                    }
                    equipSlotMasks[index] = buffer.readUShortAddLittle()
                }
                npc.setEquipmentOverrides(equipTargets, equipSlotMasks)
            }

            // Spot anim (0x20)
            if ((updateFlag and 0x20) != 0) {
                var spotAnimId = buffer.readUShort()
                val bitpackedSpeedHeight = buffer.readUIntInverseMiddle()
                if (spotAnimId == 65535) {
                    spotAnimId = -1
                }
                val spotAnimFlags = buffer.readUByteInverse()
                val spotAnimRotation = spotAnimFlags and 0x7
                val spotAnimSlot = (spotAnimFlags shr 3) and 0xf
                val slotId = if (spotAnimSlot == 15) -1 else spotAnimSlot
                val adjustReplaymode = ((spotAnimFlags shr 7) and 0x1) == 1
                npc.playSpotAnimation(spotAnimId, bitpackedSpeedHeight, spotAnimRotation, slotId, adjustReplaymode, 0)
            }
        }
    }

    @JvmStatic
    fun moveNpcs() {
        for (index in 0 until Client.NPC_UPDATE_INDEX) {
            val npcRef = Client.NPC_UPDATE_INDICES[index]
            val objectNode = Client.NPC_MAP?.get(npcRef.toLong()) as? ObjectNode
            if (objectNode != null) {
                val npc = objectNode.value as? NpcEntity
                if (npc != null) {
                    EntityUpdating.move(npc, false)
                }
            }
        }
    }
}
