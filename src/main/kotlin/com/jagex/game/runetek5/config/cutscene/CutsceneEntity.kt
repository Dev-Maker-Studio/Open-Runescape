package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity

class CutsceneEntity(buffer: Packet, val index: Int) {

    @JvmField var initialized: Boolean = false
    @JvmField val id: Int
    @JvmField var player: PlayerEntity? = null
    @JvmField var npc: NpcEntity? = null
    @JvmField var animations: IntArray? = null

    init {
        val type = buffer.readUByte()
        id = when (type) {
            0 -> buffer.readBigSmart()
            else -> -1
        }
        buffer.readString()
    }

    fun move(x: Int, y: Int, plane: Int, direction: Int) {
        if (!initialized) {
            initialized = true
            if (id >= 0) {
                npc = NpcEntity(Resource.mapRegion!!.getScene())
                npc!!.playerIndex = index
                npc!!.lastUpdate = client.cycles
                npc!!.setDirection(TypeLists.NPC!!.list(id))
                npc!!.size = npc!!.definitions.size
                npc!!.turnDirection = npc!!.definitions.turnDirection shl 3
                npc!!.worldPriority = ++drawPriority - 1
            } else {
                player = PlayerEntity(Resource.mapRegion!!.getScene())
                player!!.decodeAppearance(CutsceneTypeList.CUTSCENE_BUFFER)
                player!!.playerIndex = index
                player!!.lastUpdate = client.cycles
                player!!.worldPriority = ++drawPriority - 1
            }
        }
        if (id >= 0) {
            npc!!.move(plane, x, y, true, npc!!.getSize())
            npc!!.turn(direction, true)
        } else {
            player!!.plane = plane.toByte()
            player!!.collisionPlane = plane.toByte()
            player!!.move(x, y)
            player!!.turn(direction, true)
        }
    }

    fun reset() {
        npc = null
        player = null
        initialized = false
    }

    fun move(collisionPlane: Int, baseX: Int, baseY: Int) {
        if (npc != null) {
            npc!!.move(collisionPlane, baseX, baseY, true, npc!!.getSize())
        } else {
            player!!.plane = collisionPlane.toByte()
            player!!.collisionPlane = collisionPlane.toByte()
            player!!.move(baseX, baseY)
        }
    }

    fun isFullyCached(): Boolean {
        if (id < 0) {
            return true
        }
        val npcType = TypeLists.NPC!!.list(id)
        var cached = npcType.isBodyCached()
        if (animations == null) {
            val basType = TypeLists.BAS!!.list(npcType.npcBasId)
            animations = basType.getAnimations()
        }
        for (index in animations!!.indices) {
            val seqType = animations!![index]
            cached = cached and TypeLists.SEQ!!.list(seqType).ready()
        }
        return cached
    }

    fun getEntity(): PathingEntity {
        return npc ?: player!!
    }

    companion object {
        @JvmField
        var drawPriority = 1
    }
}
