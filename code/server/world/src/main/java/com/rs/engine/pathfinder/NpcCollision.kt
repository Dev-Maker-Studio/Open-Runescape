package com.rs.engine.pathfinder

import com.rs.game.model.entity.npc.NPC
import org.darkan.core.map.WorldCollision
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile

object NpcCollision {
    @JvmStatic
    fun clipNPC(npc: NPC) {
        if (!npc.blocksOtherNpcs()) return
        val lastTile = if (npc.lastTile == null) npc.tile else npc.lastTile
        fillNPCClip(lastTile, npc.size, false)
        if (!npc.hasFinished()) fillNPCClip(npc.tile, npc.size, true)
    }

    @JvmStatic
    fun fillNPCClip(tile: Tile, size: Int, blocks: Boolean) {
        for (x in 0 until size) for (y in 0 until size) {
            val local = tile.transform(x, y)
            if (blocks) WorldCollision.addClipNPC(local)
            else WorldCollision.removeClipNPC(local)
        }
    }

    @JvmStatic
    fun getClipNPC(tile: Tile): Boolean {
        return WorldCollision.getClipNPC(tile)
    }

    @JvmStatic
    fun checkNPCClip(npc: NPC, dir: Direction): Boolean {
        val size = npc.size
        val toX = npc.x + dir.dx
        val toY = npc.y + dir.dy
        val eastMostX = npc.x + (size - 1)
        val northMostY = npc.y + (size - 1)
        for (x in toX until (toX + size)) for (y in toY until (toY + size)) {
            if (x >= npc.x && x <= eastMostX && y >= npc.y && y <= northMostY)
                continue
            if (getClipNPC(Tile.of(x, y, npc.plane))) return false
        }
        return true
    }
}
