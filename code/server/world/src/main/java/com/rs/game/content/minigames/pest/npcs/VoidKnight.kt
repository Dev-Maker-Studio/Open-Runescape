package com.rs.game.content.minigames.pest.npcs

import com.rs.game.content.minigames.pest.PestControlGame
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile

class VoidKnight(id: Int, tile: Tile, private val game: PestControlGame) : NPC(id, tile, true) {

    init {
        setForceMultiArea(true)
        setCantFollowUnderCombat(true)
        setNoDistanceCheck(true)
        setRandomWalk(false)
    }

    override fun processNPC() {}

    override fun setCombatTarget(entity: Entity?) {}

    override fun canBeAttackedBy(player: Player) = false

    override fun sendDeath(source: Entity) {
        resetWalkSteps()
        anim(combatDefinitions.deathEmote)
        game.knightKilled()
    }
}
