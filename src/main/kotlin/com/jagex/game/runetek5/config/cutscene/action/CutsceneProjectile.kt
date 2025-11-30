package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.impl.ProjectileSpawnRequest
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.sequence.Projectile
import com.jagex.game.runetek5.config.sequence.SeqType
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.math.Vector3

class CutsceneProjectile(rsbytebuffer1: Packet, i_2: Int, i_3: Int) : CutsceneAction(rsbytebuffer1) {

    @JvmField val anInt9403: Int
    @JvmField val anInt9404: Int
    @JvmField val anInt9402: Int
    @JvmField val anInt9407: Int
    @JvmField val anInt9408: Int
    @JvmField val anInt9400: Int
    @JvmField val anInt9401: Int
    @JvmField val gfxId: Int
    @JvmField val anInt9405: Int
    @JvmField val endHeight: Int
    @JvmField val anInt9409: Int
    @JvmField val anInt9412: Int
    @JvmField val anInt9399: Int

    init {
        var i_4: Int
        if (i_2 == 0) {
            i_4 = rsbytebuffer1.readInt()
            anInt9403 = i_4 ushr 16
            anInt9404 = i_4 and 0xffff
            anInt9402 = -1
        } else {
            anInt9403 = -1
            anInt9404 = -1
            anInt9402 = rsbytebuffer1.readUShort()
        }
        if (i_3 == 0) {
            i_4 = rsbytebuffer1.readInt()
            anInt9407 = i_4 ushr 16
            anInt9408 = i_4 and 0xffff
            anInt9400 = -1
        } else {
            anInt9407 = -1
            anInt9408 = -1
            anInt9400 = rsbytebuffer1.readUShort()
        }
        if (i_2 == 0 && i_3 == 0) {
            anInt9401 = rsbytebuffer1.readUByte()
        } else {
            anInt9401 = -1
        }
        gfxId = rsbytebuffer1.readUShort()
        anInt9405 = rsbytebuffer1.readUByte()
        endHeight = rsbytebuffer1.readUByte()
        anInt9409 = rsbytebuffer1.readUMedium()
        anInt9412 = rsbytebuffer1.readUShort()
        anInt9399 = rsbytebuffer1.readUByte()
    }

    override fun perform() {
        val dstX: Int
        val dstY: Int
        var plane: Int
        if (anInt9403 >= 0) {
            dstX = anInt9403 * 512 + 256
            dstY = anInt9404 * 512 + 256
            plane = anInt9401
        } else {
            val animable9 = CutsceneTypeList.CUTSCENE_ENTITIES[anInt9402].getEntity()
            val abs = animable9.getAbsolutePosition().coords
            dstX = abs.x.toInt()
            dstY = abs.z.toInt()
            plane = animable9.plane.toInt()
        }
        val srcX: Int
        val srcY: Int
        if (anInt9404 >= 0) {
            srcX = anInt9407 * 512 + 256
            srcY = anInt9408 * 512 + 256
        } else {
            val entity = CutsceneTypeList.CUTSCENE_ENTITIES[anInt9400].getEntity()
            val abs = entity.getAbsolutePosition().coords
            srcX = abs.x.toInt()
            srcY = abs.z.toInt()
            if (plane < 0) {
                plane = entity.plane.toInt()
            }
        }
        val i_7 = anInt9399 shl 2
        val projectile = Projectile(
            Resource.mapRegion!!.getScene(), gfxId, plane, plane, dstX, dstY,
            anInt9405 shl 2, client.cycles, anInt9409 + client.cycles, anInt9412, i_7, anInt9402 + 1, anInt9400 + 1, endHeight shl 2, false, 0
        )
        projectile.start(srcX, srcY, endHeight shl 2, anInt9409 + client.cycles)
        client.PROJECTILES.addFirst(ProjectileSpawnRequest(projectile))
    }

    override fun isFullyCached(): Boolean {
        val gfxType = TypeLists.SPOT_ANIM!!.list(gfxId)
        var loaded = gfxType.isModelLoaded()
        val seqType = TypeLists.SEQ!!.list(gfxType.sequenceId)
        loaded = loaded and seqType.ready()
        return loaded
    }
}
