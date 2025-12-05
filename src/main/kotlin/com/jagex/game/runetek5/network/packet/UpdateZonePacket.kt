package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet
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

        when (this) {
            GROUND_ITEM_COUNT -> decodeGroundItemCount(buffer)
            OBJECT_PREFETCH -> decodeObjectPrefetch(buffer)
            SPOT_ANIM -> decodeSpotAnim(buffer)
            MAP_PROJANIM_HALFSQ -> decodeMapProjanimHalfsq(buffer)
            MAP_PROJANIM -> decodeMapProjanim(buffer)
            MIDI_SONG_LOCATION -> decodeMidiSongLocation(buffer)
            DESTROY_OBJECT -> decodeDestroyObject(buffer)
            REMOVE_GROUND_ITEM -> decodeRemoveGroundItem(buffer)
            CUSTOMIZE_OBJECT -> decodeCustomizeObject(buffer)
            TILE_MESSAGE -> decodeTileMessage(buffer)
            SOUND_AREA -> decodeSoundArea(buffer)
            OBJ_ANIM -> decodeObjAnim(buffer)
            GROUND_ITEM_REVEAL -> decodeGroundItemReveal(buffer)
            CREATE_GROUND_ITEM -> decodeCreateGroundItem(buffer)
            CREATE_OBJECT -> decodeCreateObject(buffer)
        }
    }

    private fun decodeGroundItemCount(buffer: Packet) {
        val coords = buffer.readUByte()
        val baseCoords = Resource.mapRegion!!.getBaseCoordinates()
        val localX = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val localY = (coords and 0x7) + UPDATE_ZONE_Y
        val x = localX + baseCoords.x
        val y = localY + baseCoords.y
        val itemId = buffer.readUShort()
        val oldAmount = buffer.readUShort()
        val newAmount = buffer.readUShort()

        client.GROUND_ITEMS?.get(createGroundItemKey(UPDATE_ZONE_PLANE, y, x))?.let { itemContainer ->
            var item = itemContainer.groundItems.getFirst() as GroundItemNode?
            while (item != null) {
                if ((itemId and 0x7fff) == item.id && oldAmount == item.amount) {
                    item.unlink()
                    item.amount = newAmount
                    GroundItemUpdating.addGroundItem(UPDATE_ZONE_PLANE, x, y, item)
                    break
                }
                item = itemContainer.groundItems.next() as GroundItemNode?
            }

            if (isWithinScene(localX, localY)) {
                GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, localX, localY)
            }
        }
    }

    private fun decodeObjectPrefetch(buffer: Packet) {
        val objectId = buffer.readInt()
        val typeIndex = buffer.readUByte()
        Resource.mapRegion!!.getLoctypeList().list(objectId).isTypeCached(typeIndex)
    }

    private fun decodeSpotAnim(buffer: Packet) {
        val bitpacked = buffer.readUByte()
        val x = (bitpacked shr 4 and 0x7) + UPDATE_ZONE_X
        val y = (bitpacked and 0x7) + UPDATE_ZONE_Y
        var spotAnimId = buffer.readUShort().let { if (it == 65535) -1 else it }
        val height = buffer.readUShort()
        val speed = buffer.readUShort()
        val rotation = buffer.readUByte()

        if (shouldProcessUpdate(x, y)) {
            if (spotAnimId == -1) {
                client.ACTIVE_GFX.get(x.toLong() shl 16 or y.toLong())?.apply {
                    objectGfx.resetCycle()
                    unlink()
                }
            } else {
                val scene = Resource.mapRegion!!.getScene()
                if (scene != null) {
                    val tileX = x * 512 + 256
                    val tileY = y * 512 + 256
                    var adjustedPlane = UPDATE_ZONE_PLANE

                    if (adjustedPlane < 3 && Resource.mapRegion!!.settingsBits.areRoofsHidden(x, y)) {
                        adjustedPlane++
                    }

                    val spotAnimation = SpotAnimation(
                        scene, spotAnimId, speed, UPDATE_ZONE_PLANE,
                        adjustedPlane, tileX,
                        MapRegion.tileheightForAbsolutePosition(tileX, tileY, UPDATE_ZONE_PLANE) - height,
                        tileY, x, x, y, y, rotation, false
                    )
                    client.ACTIVE_GFX.put(PositionedGfx(spotAnimation), x.toLong() shl 16 or y.toLong())
                }
            }
        }
    }

    private fun decodeMapProjanimHalfsq(buffer: Packet) {
        val localCoordsHash = buffer.readUByte()
        var localX = (localCoordsHash shr 4 and 0xf) + UPDATE_ZONE_X * 2
        var localY = UPDATE_ZONE_Y * 2 + (localCoordsHash and 0xf)
        val flags = buffer.readUByte()
        val adjustToTerrainHeight = (flags and 0x1) != 0
        val adjustToBASAnimFrameHeight = (flags and 0x2) != 0
        val basOffIdx = if (adjustToBASAnimFrameHeight) flags shr 2 else -1
        var xOff = localX + buffer.readByte()
        var yOff = localY + buffer.readByte()
        val source = buffer.readShort()
        val lockOn = buffer.readShort()
        val spotAnimId = buffer.readUShort()
        var startHeight = buffer.readUByte().let {
            if (adjustToBASAnimFrameHeight) it.toByte().toInt() else it * 4
        }
        var endHeight = buffer.readUByte() * 4
        val startTime = buffer.readUShort()
        val endTime = buffer.readUShort()
        var angle = buffer.readUByte().let { if (it == 255) -1 else it }
        var slope = buffer.readUShort()

        val sceneSize = Resource.mapRegion!!.getSceneSizeX()
        if (shouldProcessUpdate() &&
            localX in 0 until sceneSize * 2 && localY in 0 until sceneSize * 2 &&
            xOff in 0 until Resource.mapRegion!!.getSceneSizeY() * 2 &&
            yOff in 0 until Resource.mapRegion!!.getSceneSizeY() * 2 &&
            spotAnimId != 65535) {

            localX *= 256
            localY *= 256
            xOff *= 256
            yOff *= 256
            startHeight = startHeight shl 2
            endHeight = endHeight shl 2
            slope = slope shl 2

            ProjectileSpawnRequest.create(
                spotAnimId, source, lockOn, basOffIdx, startHeight, endHeight,
                localX, localY, xOff, yOff, startTime, endTime, angle, slope, adjustToTerrainHeight
            )
        }
    }

    private fun decodeMapProjanim(buffer: Packet) {
        val flags = buffer.readUByte()
        val useFloorHeight = (flags and 0x80) != 0
        var localX = (flags shr 3 and 0x7) + UPDATE_ZONE_X
        var localY = (flags and 0x7) + UPDATE_ZONE_Y
        var xOff = localX + buffer.readByte()
        var yOff = localY + buffer.readByte()
        val lockOn = buffer.readShort()
        val spotAnimId = buffer.readUShort()
        var startHeight = buffer.readUByte() * 4
        var endHeight = buffer.readUByte() * 4
        val startTime = buffer.readUShort()
        val endTime = buffer.readUShort()
        var angle = buffer.readUByte().let { if (it == 255) -1 else it }
        var slope = buffer.readUShort()

        val sceneSize = Resource.mapRegion!!.getSceneSizeX()
        if (shouldProcessUpdate() &&
            localX in 0 until sceneSize && localY in 0 until sceneSize &&
            xOff in 0 until sceneSize && yOff in 0 until sceneSize &&
            spotAnimId != 65535) {

            val scene = Resource.mapRegion!!.getScene()
            if (scene != null) {
                localX = localX * 512 + 256
                localY = localY * 512 + 256
                xOff = xOff * 512 + 256
                yOff = yOff * 512 + 256
                startHeight = startHeight shl 2
                endHeight = endHeight shl 2
                slope = slope shl 2

                val p = Projectile(
                    scene, spotAnimId, UPDATE_ZONE_PLANE, UPDATE_ZONE_PLANE,
                    localX, localY, startHeight, startTime + client.cycles, endTime + client.cycles,
                    angle, slope, 0, lockOn, endHeight, useFloorHeight, -1
                )
                p.start(xOff, yOff, MapRegion.tileheightForAbsolutePosition(xOff, yOff, UPDATE_ZONE_PLANE) - endHeight, startTime + client.cycles)
                client.PROJECTILES.addFirst(ProjectileSpawnRequest(p))
            }
        }
    }

    private fun decodeMidiSongLocation(buffer: Packet) {
        val coords = buffer.readUByte()
        val x = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = (coords and 0x7) + UPDATE_ZONE_Y
        var soundId = buffer.readUShort().let { if (it == 65535) -1 else it }
        val flags = buffer.readUByte()
        val range = flags shr 4 and 0xf
        val mode = flags and 0x7
        val delay = buffer.readUByte()
        val volume = buffer.readUByte()
        val rate = buffer.readUShort()

        if (shouldProcessUpdate(x, y)) {
            val checkRange = range + 1
            if (isPlayerWithinRange(x, y, checkRange)) {
                AreaSoundPlayer.playSoundSynth(
                    soundId, mode, delay, volume,
                    range + (UPDATE_ZONE_PLANE shl 24) + (y shl 8) + (x shl 16), rate
                )
            }
        }
    }

    private fun decodeDestroyObject(buffer: Packet) {
        val display = buffer.readUByteAdd()
        val shape = display shr 2
        val rotation = display and 0x3
        val type = client.OBJECT_TYPE_SLOTS[shape]
        val coords = buffer.readUByte()
        val x = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = (coords and 0x7) + UPDATE_ZONE_Y

        if (Resource.mapRegion!!.getLoadingType().ignoreUpdateZoneSizeLimitations() || isWithinScene(x, y)) {
            LocAction.destroyObject(UPDATE_ZONE_PLANE, x, y, type, -1, shape, rotation)
        }
    }

    private fun decodeRemoveGroundItem(buffer: Packet) {
        val coords = buffer.readUByteInverse()
        val baseCoords = Resource.mapRegion!!.getBaseCoordinates()
        val localY = (coords and 0x7) + UPDATE_ZONE_Y
        val localX = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = localY + baseCoords.y
        val x = localX + baseCoords.x
        val itemId = buffer.readUShortAddLittle()

        client.GROUND_ITEMS.get(createGroundItemKey(UPDATE_ZONE_PLANE, y, x))?.let { itemContainer ->
            var item = itemContainer.groundItems.getFirst() as GroundItemNode?
            while (item != null) {
                if ((itemId and 0x7fff) == item.id) {
                    item.unlink()
                    break
                }
                item = itemContainer.groundItems.next() as GroundItemNode?
            }

            if (itemContainer.groundItems.hasNext()) {
                itemContainer.unlink()
            }

            if (isWithinScene(localX, localY)) {
                GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, localX, localY)
            }
        }
    }

    private fun decodeCustomizeObject(buffer: Packet) {
        val locId = buffer.readInt()
        val locType = Resource.mapRegion!!.getLoctypeList().list(locId)
        val displayInfo = buffer.readUByteAdd()
        var locShape = displayInfo shr 2
        val type = client.OBJECT_TYPE_SLOTS[locShape]
        val transformFlags = buffer.readUByte()
        val coords = buffer.readUByteAdd()
        val x = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = (coords and 0x7) + UPDATE_ZONE_Y

        if (locShape == ObjectShapes.GROUND_INTERACT.shape) {
            locShape = ObjectShapes.SCENERY_INTERACT.shape
        }

        val modelCount = locType.shapes?.let { shapes ->
            shapes.indexOfFirst { it.toInt() == locShape }
                .takeIf { it != -1 }
                ?.let { locType.objectModelIds[it].size }
        } ?: 0

        val recolCount = locType.recolorDst?.size ?: 0
        val retexCount = locType.retextureDst?.size ?: 0

        if ((transformFlags and 0x1) == 1) {
            LocAction.customizeObject(UPDATE_ZONE_PLANE, x, y, type, locId, locShape, null)
        } else {
            val locModelTransform = if ((transformFlags and 0x2) == 2) {
                IntArray(modelCount) { buffer.readInt() }
            } else null

            val locColorTransform = if ((transformFlags and 0x4) == 4) {
                ShortArray(recolCount) { buffer.readUShort().toShort() }
            } else null

            val locTextureTransform = if ((transformFlags and 0x8) == 8) {
                ShortArray(retexCount) { buffer.readUShort().toShort() }
            } else null

            LocAction.customizeObject(
                UPDATE_ZONE_PLANE, x, y, type, locId, locShape,
                ObjectTypeCustomisation(
                    ObjectTypeCustomisation.locCustomisationId++,
                    locModelTransform, locColorTransform, locTextureTransform
                )
            )
        }
    }

    private fun decodeTileMessage(buffer: Packet) {
        buffer.readUByte() // Unused
        val coords = buffer.readUByte()
        val x = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = (coords and 0x7) + UPDATE_ZONE_Y
        val priority = buffer.readUShort()
        val duration = buffer.readUByte()
        val color = buffer.readUMedium()
        val message = buffer.readString()

        if (shouldProcessUpdate()) {
            TileMessage.offer(UPDATE_ZONE_PLANE, x, y, duration, priority, color, message)
        }
    }

    private fun decodeSoundArea(buffer: Packet) {
        val flags = buffer.readUByte()
        val localX = (flags shr 4 and 0x7) + UPDATE_ZONE_X
        val localY = (flags and 0x7) + UPDATE_ZONE_Y
        var soundId = buffer.readUShort().let { if (it == 65535) -1 else it }
        val soundFlags = buffer.readUByte()
        val range = soundFlags shr 4 and 0xf
        val timesRepeated = soundFlags and 0x7
        val delay = buffer.readUByte()
        val volume = buffer.readUByte()
        val sampleRate = buffer.readUShort()
        val voiceEffect = buffer.readUByte() == 1

        if (shouldProcessUpdate(localX, localY)) {
            val checkRange = range + 1
            if (isPlayerWithinRange(localX, localY, checkRange)) {
                AreaSoundPlayer.playSoundVorbis(
                    soundId, timesRepeated, delay, volume,
                    range + (UPDATE_ZONE_PLANE shl 24) + (localY shl 8) + (localX shl 16),
                    voiceEffect, sampleRate
                )
            }
        }
    }

    private fun decodeObjAnim(buffer: Packet) {
        val objInfo = buffer.readUByteSubtract()
        val type = objInfo shr 2
        val rotation = objInfo and 0x3
        val slot = client.OBJECT_TYPE_SLOTS[type]
        val coords = buffer.readUByteSubtract()
        val x = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = (coords and 0x7) + UPDATE_ZONE_Y
        val animationId = buffer.readUIntLittle()

        if (shouldProcessUpdate()) {
            SceneGraph.animateObject(UPDATE_ZONE_PLANE, x, y, slot, type, rotation, animationId)
        }
    }

    private fun decodeGroundItemReveal(buffer: Packet) {
        val playerId = buffer.readUShort()
        val coords = buffer.readUByte()
        val baseCoords = Resource.mapRegion!!.getBaseCoordinates()
        val localY = (coords and 0x7) + UPDATE_ZONE_Y
        val localX = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = localY + baseCoords.y
        val x = localX + baseCoords.x
        val amount = buffer.readUShortAddLittle()
        val id = buffer.readUShortAddLittle()

        if (playerId != PlayerUpdate.myPlayerIndex) {
            val withinScene = isWithinScene(localX, localY)
            if (withinScene || Resource.mapRegion!!.loadingType.ignoreUpdateZoneSizeLimitations()) {
                GroundItemUpdating.addGroundItem(UPDATE_ZONE_PLANE, x, y, GroundItemNode(id, amount))
                if (withinScene) {
                    GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, localX, localY)
                }
            }
        }
    }

    private fun decodeCreateGroundItem(buffer: Packet) {
        val amount = buffer.readUShortAddLittle()
        val id = buffer.readUShortAdd()
        val coords = buffer.readUByteInverse()
        val baseCoords = Resource.mapRegion!!.getBaseCoordinates()
        val localY = (coords and 0x7) + UPDATE_ZONE_Y
        val localX = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = localY + baseCoords.y
        val x = localX + baseCoords.x

        val withinScene = isWithinScene(localX, localY)
        if (withinScene || Resource.mapRegion!!.loadingType.ignoreUpdateZoneSizeLimitations()) {
            GroundItemUpdating.addGroundItem(UPDATE_ZONE_PLANE, x, y, GroundItemNode(id, amount))
            if (withinScene) {
                GroundItemUpdating.removeGroundItem(UPDATE_ZONE_PLANE, localX, localY)
            }
        }
    }

    private fun decodeCreateObject(buffer: Packet) {
        val objectId = buffer.readInt()
        val coords = buffer.readUByteSubtract()
        val x = (coords shr 4 and 0x7) + UPDATE_ZONE_X
        val y = (coords and 0x7) + UPDATE_ZONE_Y
        val displayInfo = buffer.readUByteSubtract()
        val shape = displayInfo shr 2
        val rotation = displayInfo and 0x3
        val type = client.OBJECT_TYPE_SLOTS[shape]

        if (Resource.mapRegion!!.loadingType.ignoreUpdateZoneSizeLimitations() || isWithinScene(x, y)) {
            LocAction.destroyObject(UPDATE_ZONE_PLANE, x, y, type, objectId, shape, rotation)
        }
    }

    private fun shouldProcessUpdate(x: Int = 0, y: Int = 0): Boolean {
        return Resource.mapRegion!!.loadingType !== RegionLoadingType.CUTSCENE &&
                (x == 0 && y == 0 || isWithinScene(x, y))
    }

    private fun isWithinScene(x: Int, y: Int): Boolean {
        return x in 0 until Resource.mapRegion!!.getSceneSizeX() &&
                y in 0 until Resource.mapRegion!!.getSceneSizeY()
    }

    private fun isPlayerWithinRange(x: Int, y: Int, range: Int): Boolean {
        val player = PlayerUpdate.MY_PLAYER
        return player.movementBaseX[0] in (x - range)..(x + range) &&
                player.movementBaseY[0] in (y - range)..(y + range)
    }

    private fun createGroundItemKey(plane: Int, y: Int, x: Int): Long {
        return plane.toLong() shl 28 or (y.toLong() shl 14) or x.toLong()
    }

    companion object {
        @JvmField
        var UPDATE_ZONE_PLANE: Int = 0
        var UPDATE_ZONE_X: Int = 0
        var UPDATE_ZONE_Y: Int = 0
    }
}