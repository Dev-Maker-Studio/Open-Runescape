package com.rs.game.content.minigames.pest.npcs

import com.rs.game.World
import com.rs.game.content.minigames.pest.PestControlGame
import com.rs.game.tasks.WorldTasks
import com.rs.utils.WorldUtil
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

private const val TELEPORT_COOLDOWN = 8
private const val CHASE_DISTANCE = 3
private const val TELEPORT_CHANCE = 6

class Shifter(id: Int, tile: Tile, game: PestControlGame, portal: PestPortal) : Pest(id, tile, game, portal, PestFamily.SHIFTER) {
    private var nextTeleport = 0L

    override val reachesTowers = true

    override fun processNPC() {
        super.processNPC()
        val target = combat.target ?: return
        if (isDead || hasFinished() || World.getServerTicks() < nextTeleport) return
        if (target.plane != plane || !WorldUtil.isInRange(this, target, CHASE_DISTANCE) || Utils.random(TELEPORT_CHANCE) == 0 && !WorldUtil.isInRange(this, target, 1))
            teleportTo(target.tile)
    }

    private fun teleportTo(destination: Tile) {
        val landing = World.findAdjacentFreeTile(destination) ?: return
        nextTeleport = World.getServerTicks() + TELEPORT_COOLDOWN
        resetWalkSteps()
        anim(Seq.pest_shifter_teleportattack)
        spotAnim(Spotanim.pest_shifter_portal)
        WorldTasks.schedule(1) {
            if (isDead || hasFinished()) return@schedule
            tele(landing)
            spotAnim(Spotanim.pest_shifter_portal)
        }
    }
}
