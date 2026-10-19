package com.rs.game.content.minigames.pest.npcs

import com.rs.game.World
import com.rs.game.content.minigames.pest.Fortification
import com.rs.game.content.minigames.pest.PestControlGame
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private const val STRUCTURE_SEARCH = 12

class Ravager(id: Int, tile: Tile, game: PestControlGame, portal: PestPortal) : Pest(id, tile, game, portal, PestFamily.RAVAGER) {
    private var structure: Fortification? = null
    private var nextSwipe = 0L

    override fun processNPC() {
        if (isDead || hasFinished() || isLocked) return
        val target = structure?.takeIf { it.standing } ?: game.fortifications.nearestStanding(this, STRUCTURE_SEARCH)
        structure = target
        if (target == null) {
            super.processNPC()
            return
        }
        val piece = target.closestPiece(this)
        if (!withinDistance(piece, 1)) {
            if (!hasWalkSteps()) walkTowards(piece)
            return
        }
        resetWalkSteps()
        faceTile(piece)
        if (World.getServerTicks() < nextSwipe) return
        nextSwipe = World.getServerTicks() + attackSpeed
        anim(Seq.ravager_attack)
        target.damage()
    }
}
