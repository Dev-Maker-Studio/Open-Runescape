package com.rs.game.content.bosses

import com.rs.game.World
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile

private val BURROWS = arrayOf(
    -23 to 42, -9 to 47, 18 to 51, -24 to 41, 20 to -34, -2 to -24, -15 to -17, 0 to -3
)

private val DUST_RING = arrayOf(
    0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0
)

class GiantMole(id: Int, tile: Tile, spawned: Boolean) : NPC(id, tile, spawned) {
    private val hpPercent: Double get() = hitpoints.toDouble() / maxHitpoints.toDouble() * 100.0

    override fun handlePostHit(hit: Hit) {
        if (hpPercent in 5.0..50.0 && Utils.random(4) == 0) burrow(hit)
        super.handlePostHit(hit)
    }

    private fun burrow(hit: Hit) {
        anim(Seq.seq_3314)
        setCantInteract(true)
        combat.removeTarget()
        val watcher = hit.source as? Player
        watcher?.interfaceManager?.sendOverlay(Interface.interface_226)
        val middle = middleTile
        schedule {
            wait(2)
            watcher?.interfaceManager?.removeOverlay()
            setCantInteract(false)
            if (isDead) return@schedule
            World.sendSpotAnim(middle, SpotAnim(Spotanim.spotanim_572))
            DUST_RING.forEach { (dx, dy) -> World.sendSpotAnim(middle.transform(dx, dy), SpotAnim(Spotanim.mole_mud_cloud)) }
            val (dx, dy) = BURROWS.random()
            tele(respawnTile.transform(dx, dy))
            anim(Seq.seq_3315)
        }
    }
}

@ServerStartupEvent
fun mapGiantMole() {
    instantiateNpc(Npc.npc_3340) { npcId, tile -> GiantMole(npcId, tile, false) }
}
