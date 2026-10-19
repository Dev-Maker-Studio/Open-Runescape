package com.rs.game.content.quests.deserttreasure

import com.rs.game.content.combat.CombatSpell
import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.npcCombat
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

class Damis(owner: Player, id: Int, tile: Tile) : OwnedNPC(owner, id, tile, true) {
    override fun handlePreHit(hit: Hit) {
        if (hit.look == Hit.HitLook.MAGIC_DAMAGE &&
            (hit.getData("combatSpell") as? CombatSpell)?.isEarthSpell == true)
            hit.damage *= 2
        super.handlePreHit(hit)
    }

    override fun processNPC() {
        if (id == Npc.fd_damis_tougher && !isDead && !hasFinished() && !owner.isDead &&
            combat.target === owner && withinDistance(owner, 1))
            owner.prayer.drainPrayer(owner.prayer.points * 0.05 + 10.0)
        super.processNPC()
    }
}

@ServerStartupEvent
fun mapDamisCombat() {
    npcCombat(Npc.fd_damis_normal, Npc.fd_damis_tougher) { npc, target ->
        npc.anim(Seq.human_sword_stab)
        CombatScript.delayHit(npc, 0, target, Hit.melee(npc, CombatScript.getMaxHit(npc, CombatStyle.MELEE, target)))
        if (npc.id == Npc.fd_damis_tougher) 3 else 4
    }
}
