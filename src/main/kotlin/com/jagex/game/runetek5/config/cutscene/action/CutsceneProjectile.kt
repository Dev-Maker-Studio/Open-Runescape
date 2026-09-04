package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.impl.ProjectileSpawnRequest
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.sequence.Projectile

class CutsceneProjectile(buffer: Packet, sourceType: Int, targetType: Int) : CutsceneAction(buffer) {

    @JvmField val startTileX: Int
    @JvmField val startTileY: Int
    @JvmField val sourceEntityIndex: Int
    @JvmField val endTileX: Int
    @JvmField val endTileY: Int
    @JvmField val targetEntityIndex: Int
    @JvmField val fixedPlane: Int
    @JvmField val gfxId: Int
    @JvmField val startHeight: Int
    @JvmField val endHeight: Int
    @JvmField val duration: Int
    @JvmField val angle: Int
    @JvmField val slope: Int

    init {
        var packedTile: Int
        if (sourceType == 0) {
            packedTile = buffer.readInt()
            startTileX = packedTile ushr 16
            startTileY = packedTile and 0xffff
            sourceEntityIndex = -1
        } else {
            startTileX = -1
            startTileY = -1
            sourceEntityIndex = buffer.readUShort()
        }
        if (targetType == 0) {
            packedTile = buffer.readInt()
            endTileX = packedTile ushr 16
            endTileY = packedTile and 0xffff
            targetEntityIndex = -1
        } else {
            endTileX = -1
            endTileY = -1
            targetEntityIndex = buffer.readUShort()
        }
        if (sourceType == 0 && targetType == 0) {
            fixedPlane = buffer.readUByte()
        } else {
            fixedPlane = -1
        }
        gfxId = buffer.readUShort()
        startHeight = buffer.readUByte()
        endHeight = buffer.readUByte()
        duration = buffer.readUMedium()
        angle = buffer.readUShort()
        slope = buffer.readUByte()
    }

    override fun perform() {
        val srcX: Int
        val srcY: Int
        var plane: Int
        if (startTileX >= 0) {
            srcX = startTileX * 512 + 256
            srcY = startTileY * 512 + 256
            plane = fixedPlane
        } else {
            val sourceEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![sourceEntityIndex].getEntity()
            val sourcePos = sourceEntity.getAbsolutePosition().coords
            srcX = sourcePos.x.toInt()
            srcY = sourcePos.z.toInt()
            plane = sourceEntity.plane.toInt()
        }
        val dstX: Int
        val dstY: Int
        if (startTileY >= 0) {
            dstX = endTileX * 512 + 256
            dstY = endTileY * 512 + 256
        } else {
            val targetEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![targetEntityIndex].getEntity()
            val targetPos = targetEntity.getAbsolutePosition().coords
            dstX = targetPos.x.toInt()
            dstY = targetPos.z.toInt()
            if (plane < 0) {
                plane = targetEntity.plane.toInt()
            }
        }
        val scaledSlope = slope shl 2
        val projectile = Projectile(
            Resource.mapRegion!!.getScene()!!, gfxId, plane, plane, srcX, srcY,
            startHeight shl 2, Client.cycles, duration + Client.cycles, angle, scaledSlope, sourceEntityIndex + 1, targetEntityIndex + 1, endHeight shl 2, false, 0
        )
        projectile.start(dstX, dstY, endHeight shl 2, duration + Client.cycles)
        Client.PROJECTILES.addFirst(ProjectileSpawnRequest(projectile))
    }

    override fun isFullyCached(): Boolean {
        val gfxType = TypeLists.SPOT_ANIM!!.list(gfxId)
        var loaded = gfxType.isModelLoaded()
        val seqType = TypeLists.SEQ!!.list(gfxType.sequenceId)
        loaded = loaded and seqType.ready()
        return loaded
    }
}
