package com.rs.game.content.bosses.godwars.factions.zaros

import com.rs.game.content.bosses.HostedMinion
import com.rs.game.content.bosses.MinionHost
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.NPC
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

class NexMinion(id: Int, tile: Tile, spawned: Boolean) : NPC(id, tile, spawned), HostedMinion {
    override var host: MinionHost? = null

    init {
        setCantFollowUnderCombat(true)
        capDamage = 0
        isIgnoreDocile = true
    }

    fun breakBarrier() {
        capDamage = -1
    }

    override fun ignoreWallsWhenMeleeing() = true

    override fun processNPC() {
        if (isDead) return
        if (!combat.process()) checkAggressivity()
    }

    override fun handlePreHit(hit: Hit) {
        super.handlePreHit(hit)
        if (hit.look != HitLook.RANGE_DAMAGE) hit.damage = 0
        if (capDamage != -1) spotAnim(Spotanim.dsd_surok_defend_spotanim)
    }

    override fun sendDeath(source: Entity?) {
        super.sendDeath(source)
        (host as? Nex)?.nextPhase()
    }
}

@ServerStartupEvent
fun mapNexMinions() {
    instantiateNpc(Npc.godwars2_smokemage, Npc.godwars2_shadowmage, Npc.godwars2_bloodmage, Npc.godwars2_icemage) { npcId, tile ->
        NexMinion(npcId, tile, true)
    }
}
