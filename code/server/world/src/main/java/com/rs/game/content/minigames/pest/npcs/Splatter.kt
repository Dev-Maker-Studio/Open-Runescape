package com.rs.game.content.minigames.pest.npcs

import com.rs.game.content.minigames.pest.PestControlGame
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

private const val BLAST_RADIUS = 1
private val BLASTS = mapOf(
    Npc.pest_splatter_1 to Spotanim.splatter_exploding,
    Npc.pest_splatter_2 to Spotanim.splatter_exploding_2,
    Npc.pest_splatter_3 to Spotanim.splatter_exploding_3,
    Npc.pest_splatter_4 to Spotanim.splatter_exploding_4,
    Npc.pest_splatter_5 to Spotanim.splatter_exploding_5,
)

class Splatter(id: Int, tile: Tile, game: PestControlGame, portal: PestPortal) : Pest(id, tile, game, portal, PestFamily.SPLATTER) {
    private var exploded = false

    fun explode() {
        if (exploded) return
        exploded = true
        resetWalkSteps()
        combat.removeTarget()
        anim(Seq.splatter_exploding)
        spotAnim(BLASTS[id] ?: Spotanim.splatter_exploding)
        WorldTasks.schedule(1) {
            for (victim in game.blastVictims(this, BLAST_RADIUS))
                victim.applyHit(Hit(this, Utils.random(maxHit + 1), HitLook.TRUE_DAMAGE))
            game.fortifications.damageNear(tile, BLAST_RADIUS)
            finish()
        }
    }

    override fun sendDeath(source: Entity) = explode()
}
