package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.impl.ProjectileSpawnRequest
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.sequence.Projectile

class CutsceneProjectile(rsbytebuffer1: Packet, i_2: Int, i_3: Int) : CutsceneAction(rsbytebuffer1) {

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
        var i_4: Int
        if (i_2 == 0) {
            i_4 = rsbytebuffer1.readInt()
            startTileX = i_4 ushr 16
            startTileY = i_4 and 0xffff
            sourceEntityIndex = -1
        } else {
            startTileX = -1
            startTileY = -1
            sourceEntityIndex = rsbytebuffer1.readUShort()
        }
        if (i_3 == 0) {
            i_4 = rsbytebuffer1.readInt()
            endTileX = i_4 ushr 16
            endTileY = i_4 and 0xffff
            targetEntityIndex = -1
        } else {
            endTileX = -1
            endTileY = -1
            targetEntityIndex = rsbytebuffer1.readUShort()
        }
        if (i_2 == 0 && i_3 == 0) {
            fixedPlane = rsbytebuffer1.readUByte()
        } else {
            fixedPlane = -1
        }
        gfxId = rsbytebuffer1.readUShort()
        startHeight = rsbytebuffer1.readUByte()
        endHeight = rsbytebuffer1.readUByte()
        duration = rsbytebuffer1.readUMedium()
        angle = rsbytebuffer1.readUShort()
        slope = rsbytebuffer1.readUByte()
    }

    override fun perform() {
        val dstX: Int
        val dstY: Int
        var plane: Int
        if (startTileX >= 0) {
            dstX = startTileX * 512 + 256
            dstY = startTileY * 512 + 256
            plane = fixedPlane
        } else {
            val animable9 = CutsceneTypeList.CUTSCENE_ENTITIES!![sourceEntityIndex].getEntity()
            val abs = animable9.getAbsolutePosition().coords
            dstX = abs.x.toInt()
            dstY = abs.z.toInt()
            plane = animable9.plane.toInt()
        }
        val srcX: Int
        val srcY: Int
        if (startTileY >= 0) {
            srcX = endTileX * 512 + 256
            srcY = endTileY * 512 + 256
        } else {
            val entity = CutsceneTypeList.CUTSCENE_ENTITIES!![targetEntityIndex].getEntity()
            val abs = entity.getAbsolutePosition().coords
            srcX = abs.x.toInt()
            srcY = abs.z.toInt()
            if (plane < 0) {
                plane = entity.plane.toInt()
            }
        }
        val i_7 = slope shl 2
        val projectile = Projectile(
            Resource.mapRegion!!.getScene()!!, gfxId, plane, plane, dstX, dstY,
            startHeight shl 2, Client.cycles, duration + Client.cycles, angle, i_7, sourceEntityIndex + 1, targetEntityIndex + 1, endHeight shl 2, false, 0
        )
        projectile.start(srcX, srcY, endHeight shl 2, duration + Client.cycles)
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
