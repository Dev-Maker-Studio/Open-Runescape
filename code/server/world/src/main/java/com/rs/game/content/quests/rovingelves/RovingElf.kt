package com.rs.game.content.quests.rovingelves

import com.rs.game.model.entity.npc.NPC
import world.gregs.voidps.type.Tile

internal class RovingElf(id: Int, tile: Tile) : NPC(id, tile) {
    init { setRandomWalk(true) }

    fun moveToCamp(tile: Tile) {
        resetWalkSteps()
        setRespawnTile(tile)
        tele(tile)
        setRandomWalk(true)
    }
}
