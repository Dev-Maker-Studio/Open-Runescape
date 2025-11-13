package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.cache.node.impl.ProjectileSpawnRequest
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.sequence.PositionedGfx
import com.jagex.game.runetek5.config.sequence.Projectile
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode
import com.jagex.game.runetek5.entity.grounditem.GroundItemUpdating
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation
import com.jagex.game.runetek5.scene.LocAction
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.RegionLoadingType
import com.jagex.game.runetek5.scene.SceneGraph
import com.jagex.game.runetek5.scene.tile.TileMessage
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer

enum class UpdateZonePacket(val size: Int) {
    CUSTOMIZE_OBJECT(-1),
    REMOVE_GROUND_ITEM(3),
    CREATE_GROUND_ITEM(5),
    DESTROY_OBJECT(2),
    CREATE_OBJECT(6),
    MAP_PROJANIM(16),
    MAP_PROJANIM_HALFSQ(19),
    OBJECT_PREFETCH(5),
    GROUND_ITEM_REVEAL(7),
    GROUND_ITEM_COUNT(7),
    MIDI_SONG_LOCATION(8),
    TILE_MESSAGE(-1),
    OBJ_ANIM(6),
    SOUND_AREA(9),
    SPOT_ANIM(8);

    fun decode() {
        val buffer = client.GAME_CONNECTION_CONTEXT.receivedBuffer
        if (this == GROUND_ITEM_COUNT) {
            val i_3 = buffer.readUByte()
            val coordgrid4 = Resource.mapRegion.getBaseCoordinates()
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val y = i_5 + coordgrid4.y
            val i_7 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val x = i_7 + coordgrid4.x
            val i_9 = buffer.readUShort()
            val i_10 = buffer.readUShort()
            val amount = buffer.readUShort()
            if (client.GROUND_ITEMS != null) {
                val class282Sub2912 = client.GROUND_ITEMS.get(UpdateZonePacket.UPDATE_ZONE_PLANE.toLong() shl 28 or (y.toLong() shl 14) or x.toLong())
                if (class282Sub2912 != null) {
                    var item = class282Sub2912.groundItems.getFirst() as GroundItemNode?
                    while (item != null) {
                        if ((i_9 and 0x7fff) == item.id && i_10 == item.amount) {
                            item.unlink()
                            item.amount = amount
                            GroundItemUpdating.addGroundItem(UPDATE_ZONE_PLANE, x, y, item)
                            break
                        }
                        item = class282Sub2912.groundItems.next() as GroundItemNode?
                    }
                    if (i_7 >= 0 && i_5 >= 0 && i_7 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                        GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, i_7, i_5)
                    }
                }
            }
        } else if (this == OBJECT_PREFETCH) {
            val i_3 = buffer.readInt()
            val i_21 = buffer.readUByte()
            Resource.mapRegion.getLoctypeList().list(i_3).isTypeCached(i_21)
        } else if (this == SPOT_ANIM) {
            val bitpacked = buffer.readUByte()
            val x = (bitpacked shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (bitpacked and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var spotAnimId = buffer.readUShort()
            if (spotAnimId == 65535) {
                spotAnimId = -1
            }
            val height = buffer.readUShort()
            val speed = buffer.readUShort()
            val rotation = buffer.readUByte()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                if (spotAnimId == -1) {
                    val class282Sub50Sub1022 = client.ACTIVE_GFX.get(x.toLong() shl 16 or y.toLong())
                    if (class282Sub50Sub1022 != null) {
                        class282Sub50Sub1022.objectGfx.resetCycle()
                        class282Sub50Sub1022.unlink()
                    }
                } else {
                    val tileX = x * 512 + 256
                    val tileY = y * 512 + 256
                    var adjustedPlane = UpdateZonePacket.UPDATE_ZONE_PLANE
                    if (adjustedPlane < 3 && Resource.mapRegion.getSettingsBits().areRoofsHidden(x, y)) {
                        adjustedPlane++
                    }
                    val spotAnimation = SpotAnimation(Resource.mapRegion.getScene(), spotAnimId, speed, UPDATE_ZONE_PLANE, adjustedPlane, tileX, MapRegion.tileheightForAbsolutePosition(tileX, tileY, UPDATE_ZONE_PLANE) - height, tileY, x, x, y, y, rotation, false)
                    client.ACTIVE_GFX.put(PositionedGfx(spotAnimation), x.toLong() shl 16 or y.toLong())
                }
            }
        } else if (this == MAP_PROJANIM_HALFSQ) {
            val localCoordsHash = buffer.readUByte()
            var localX = (localCoordsHash shr 4 and 0xf) + UpdateZonePacket.UPDATE_ZONE_X * 2
            var localY = UpdateZonePacket.UPDATE_ZONE_Y * 2 + (localCoordsHash and 0xf)
            val flags = buffer.readUByte()
            val adjustToTerrainHeight = (flags and 0x1) != 0
            val adjustToBASAnimFrameHeight = (flags and 0x2) != 0
            val basOffIdx = if (adjustToBASAnimFrameHeight) flags shr 2 else -1
            var xOff = localX + buffer.readByte()
            var yOff = localY + buffer.readByte()
            val source = buffer.readShort()
            val lockOn = buffer.readShort()
            val spotAnimId = buffer.readUShort()
            var startHeight = buffer.readUByte()
            if (adjustToBASAnimFrameHeight) {
                startHeight = startHeight.toByte().toInt()
            } else {
                startHeight *= 4
            }
            var endHeight = buffer.readUByte() * 4
            val startTime = buffer.readUShort()
            val endTime = buffer.readUShort()
            var angle = buffer.readUByte()
            var slope = buffer.readUShort()
            if (angle == 255) {
                angle = -1
            }
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() * 2 && localY < Resource.mapRegion.getSceneSizeX() * 2 && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeY() * 2 && yOff < Resource.mapRegion.getSceneSizeY() * 2 && spotAnimId != 65535) {
                localX *= 256
                localY *= 256
                xOff *= 256
                yOff *= 256
                startHeight = startHeight shl 2
                endHeight = endHeight shl 2
                slope = slope shl 2
                ProjectileSpawnRequest.create(spotAnimId, source, lockOn, basOffIdx, startHeight, endHeight, localX, localY, xOff, yOff, startTime, endTime, angle, slope, adjustToTerrainHeight)
            }
        } else if (this == MAP_PROJANIM) {
            val flags = buffer.readUByte()
            val useFloorHeight = (flags and 0x80) != 0
            var localX = (flags shr 3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            var localY = (flags and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var xOff = localX + buffer.readByte()
            var yOff = localY + buffer.readByte()
            val lockOn = buffer.readShort()
            val spotAnimId = buffer.readUShort()
            var startHeight = buffer.readUByte() * 4
            var endHeight = buffer.readUByte() * 4
            val startTime = buffer.readUShort()
            val endTime = buffer.readUShort()
            var angle = buffer.readUByte()
            var slope = buffer.readUShort()
            if (angle == 255) {
                angle = -1
            }
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY() && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeX() && yOff < Resource.mapRegion.getSceneSizeY() && spotAnimId != 65535) {
                localX = localX * 512 + 256
                localY = localY * 512 + 256
                xOff = xOff * 512 + 256
                yOff = yOff * 512 + 256
                startHeight = startHeight shl 2
                endHeight = endHeight shl 2
                slope = slope shl 2
                val p = Projectile(Resource.mapRegion.getScene(), spotAnimId, UPDATE_ZONE_PLANE, UPDATE_ZONE_PLANE, localX, localY, startHeight, startTime + client.cycles, endTime + client.cycles, angle, slope, 0, lockOn, endHeight, useFloorHeight, -1)
                p.start(xOff, yOff, MapRegion.tileheightForAbsolutePosition(xOff, yOff, UPDATE_ZONE_PLANE) - endHeight, startTime + client.cycles)
                client.PROJECTILES.addFirst(ProjectileSpawnRequest(p))
            }
        } else if (this == MIDI_SONG_LOCATION) {
            val i_3 = buffer.readUByte()
            val i_21 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var i_6 = buffer.readUShort()
            if (i_6 == 65535) {
                i_6 = -1
            }
            val i_7 = buffer.readUByte()
            val i_8 = i_7 shr 4 and 0xf
            val i_9 = i_7 and 0x7
            val i_10 = buffer.readUByte()
            val i_11 = buffer.readUByte()
            val i_23 = buffer.readUShort()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && i_21 >= 0 && i_5 >= 0 && i_21 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                val i_24 = i_8 + 1
                if (PlayerUpdate.MY_PLAYER.movementBaseX[0] >= i_21 - i_24 && PlayerUpdate.MY_PLAYER.movementBaseX[0] <= i_21 + i_24 && PlayerUpdate.MY_PLAYER.movementBaseY[0] >= i_5 - i_24 && PlayerUpdate.MY_PLAYER.movementBaseY[0] <= i_24 + i_5) {
                    AreaSoundPlayer.playSoundSynth(i_6, i_9, i_10, i_11, i_8 + (UPDATE_ZONE_PLANE shl 24) + (i_5 shl 8) + (i_21 shl 16), i_23)
                }
            }
        } else if (this == DESTROY_OBJECT) {
            val display = buffer.readUByteAdd()
            val shape = display shr 2
            val rotation = display and 0x3
            val type = client.OBJECT_TYPE_SLOTS[shape]
            val coords = buffer.readUByte()
            val x = (coords shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (coords and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            if (Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations() || x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                LocAction.destroyObject(UPDATE_ZONE_PLANE, x, y, type, -1, shape, rotation)
            }
        } else if (this == REMOVE_GROUND_ITEM) {
            val i_3 = buffer.readUByteInverse()
            val coordgrid = Resource.mapRegion.getBaseCoordinates()
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val i_6 = i_5 + coordgrid.y
            val i_7 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_8 = i_7 + coordgrid.x
            val i_9 = buffer.readUShortAddLittle()
            val class282Sub2939 = client.GROUND_ITEMS.get(UpdateZonePacket.UPDATE_ZONE_PLANE.toLong() shl 28 or (i_6.toLong() shl 14) or i_8.toLong())
            if (class282Sub2939 != null) {
                var class282Sub3426 = class282Sub2939.groundItems.getFirst() as GroundItemNode?
                while (class282Sub3426 != null) {
                    if ((i_9 and 0x7fff) == class282Sub3426.id) {
                        class282Sub3426.unlink()
                        break
                    }
                    class282Sub3426 = class282Sub2939.groundItems.next() as GroundItemNode?
                }
                if (class282Sub2939.groundItems.hasNext()) {
                    class282Sub2939.unlink()
                }
                if (i_7 >= 0 && i_5 >= 0 && i_7 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                    GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, i_7, i_5)
                }
            }
        } else if (this == CUSTOMIZE_OBJECT) {
            val locId = buffer.readInt()
            val locType = Resource.mapRegion.getLoctypeList().list(locId)
            val i_5 = buffer.readUByteAdd()
            var locShape = i_5 shr 2
            val type = client.OBJECT_TYPE_SLOTS[locShape]
            val transformFlags = buffer.readUByte()
            val coords = buffer.readUByteAdd()
            val x = (coords shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (coords and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            if (locShape == ObjectShapes.GROUND_INTERACT.shape) {
                locShape = ObjectShapes.SCENERY_INTERACT.shape
            }
            var modelCount = 0
            if (locType.shapes != null) {
                var i_24 = -1
                for (i_14 in locType.shapes.indices) {
                    if (locShape == locType.shapes[i_14].toInt()) {
                        i_24 = i_14
                        break
                    }
                }
                modelCount = locType.objectModelIds[i_24].size
            }
            var recolCount = 0
            if (locType.recolorDst != null) {
                recolCount = locType.recolorDst.size
            }
            var retexCount = 0
            if (locType.retextureDst != null) {
                retexCount = locType.retextureDst.size
            }
            if ((transformFlags and 0x1) == 1) {
                LocAction.customizeObject(UPDATE_ZONE_PLANE, x, y, type, locId, locShape, null)
            } else {
                var locModelTransform: IntArray? = null
                if ((transformFlags and 0x2) == 2) {
                    locModelTransform = IntArray(modelCount)
                    for (index in 0..<modelCount) {
                        locModelTransform[index] = buffer.readInt()
                    }
                }
                var locColorTransform: ShortArray? = null
                if ((transformFlags and 0x4) == 4) {
                    locColorTransform = ShortArray(recolCount)
                    for (index in 0..<recolCount) {
                        locColorTransform[index] = buffer.readUShort().toShort()
                    }
                }
                var locTextureTransform: ShortArray? = null
                if ((transformFlags and 0x8) == 8) {
                    locTextureTransform = ShortArray(retexCount)
                    for (index in 0..<retexCount) {
                        locTextureTransform[index] = buffer.readUShort().toShort()
                    }
                }
                LocAction.customizeObject(UPDATE_ZONE_PLANE, x, y, type, locId, locShape, ObjectTypeCustomisation(ObjectTypeCustomisation.locCustomisationId++, locModelTransform, locColorTransform, locTextureTransform))
            }
        } else if (this == TILE_MESSAGE) {
            buffer.readUByte()
            val i_3 = buffer.readUByte()
            val i_21 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val i_6 = buffer.readUShort()
            val i_7 = buffer.readUByte()
            val i_8 = buffer.readUMedium()
            val string_27 = buffer.readString()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE) {
                TileMessage.offer(UPDATE_ZONE_PLANE, i_21, i_5, i_7, i_6, i_8, string_27)
            }
        } else if (this == SOUND_AREA) { //sound related again
            val flags = buffer.readUByte()
            val localX = (flags shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val localY = (flags and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var soundId = buffer.readUShort()
            if (soundId == 65535) {
                soundId = -1
            }
            val i_7 = buffer.readUByte()
            val i_8 = i_7 shr 4 and 0xf
            val timesRepeated = i_7 and 0x7
            val delay = buffer.readUByte()
            val volume = buffer.readUByte()
            val sampleRate = buffer.readUShort()
            val voiceEffect = buffer.readUByte() == 1
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY()) {
                val i_14 = i_8 + 1
                if (PlayerUpdate.MY_PLAYER.movementBaseX[0] >= localX - i_14 && PlayerUpdate.MY_PLAYER.movementBaseX[0] <= localX + i_14 && PlayerUpdate.MY_PLAYER.movementBaseY[0] >= localY - i_14 && PlayerUpdate.MY_PLAYER.movementBaseY[0] <= i_14 + localY) {
                    AreaSoundPlayer.playSoundVorbis(soundId, timesRepeated, delay, volume, i_8 + (UPDATE_ZONE_PLANE shl 24) + (localY shl 8) + (localX shl 16), voiceEffect, sampleRate)
                }
            }
        } else if (this == OBJ_ANIM) {
            val i_3 = buffer.readUByteSubtract()
            val type = i_3 shr 2
            val rotation = i_3 and 0x3
            val slot = client.OBJECT_TYPE_SLOTS[type]
            val i_7 = buffer.readUByteSubtract()
            val x = (i_7 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (i_7 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val animationId = buffer.readUIntLittle()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE) {
                SceneGraph.animateObject(UPDATE_ZONE_PLANE, x, y, slot, type, rotation, animationId)
            }
        } else if (this == GROUND_ITEM_REVEAL) {
            val playerId = buffer.readUShort()
            val i_21 = buffer.readUByte()
            val coordgrid25 = Resource.mapRegion.getBaseCoordinates()
            val localY = (i_21 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val y = localY + coordgrid25.y
            val localX = (i_21 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val x = localX + coordgrid25.x
            val amount = buffer.readUShortAddLittle()
            val id = buffer.readUShortAddLittle()
            if (playerId != PlayerUpdate.myPlayerIndex) {
                val withinSceneRange = localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY()
                if (withinSceneRange || Resource.mapRegion.loadingType.ignoreUpdateZoneSizeLimitations()) {
                    GroundItemUpdating.addGroundItem(UPDATE_ZONE_PLANE, x, y, GroundItemNode(id, amount))
                    if (withinSceneRange) {
                        GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, localX, localY)
                    }
                }
            }
        } else if (this == CREATE_GROUND_ITEM) {
            val amount = buffer.readUShortAddLittle()
            val id = buffer.readUShortAdd()
            val i_5 = buffer.readUByteInverse()
            val coordgrid31 = Resource.mapRegion.getBaseCoordinates()
            val localY = (i_5 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val y = localY + coordgrid31.y
            val localX = (i_5 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val x = localX + coordgrid31.x
            val bool36 = localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY()
            if (bool36 || Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations()) {
                GroundItemUpdating.addGroundItem(UPDATE_ZONE_PLANE, x, y, GroundItemNode(id, amount))
                if (bool36) {
                    GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, localX, localY)
                }
            }
        } else if (this == CREATE_OBJECT) {
            val i_3 = buffer.readInt()
            val i_21 = buffer.readUByteSubtract()
            val i_5 = (i_21 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_6 = (i_21 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val i_7 = buffer.readUByteSubtract()
            val i_8 = i_7 shr 2
            val i_9 = i_7 and 0x3
            val i_10 = client.OBJECT_TYPE_SLOTS[i_8]
            if (Resource.mapRegion.loadingType.ignoreUpdateZoneSizeLimitations() || i_5 >= 0 && i_6 >= 0 && i_5 < Resource.mapRegion.getSceneSizeX() && i_6 < Resource.mapRegion.getSceneSizeY()) {
                LocAction.destroyObject(UPDATE_ZONE_PLANE, i_5, i_6, i_10, i_3, i_8, i_9)
            }
        } else {
            PacketEncoder.sendErrorToServer("" + this, RuntimeException())
            client.logout(false)
        }
    }

    companion object {
        @JvmField
        var UPDATE_ZONE_PLANE: Int = 0
        var UPDATE_ZONE_X: Int = 0
        var UPDATE_ZONE_Y: Int = 0
    }
}