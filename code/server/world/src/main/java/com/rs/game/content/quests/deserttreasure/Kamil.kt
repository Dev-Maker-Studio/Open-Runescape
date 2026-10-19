package com.rs.game.content.quests.deserttreasure

import com.rs.game.World
import com.rs.game.content.combat.CombatSpell
import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.npcCombat
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

class Kamil(owner: Player, tile: Tile) : OwnedNPC(owner, Npc.icediamond_icewarrior, tile, true) {
    init {
        setAutoDespawnAtDistance(false)
        setForceMultiAttacked(true)
        setAttackRange(8)
        val opponent = owner.attackedBy as? NPC
        if (opponent != null) {
            opponent.removeCombatTarget()
            owner.resetCombat()
            owner.interactionManager.forceStop()
        }
        setCombatTarget(owner)
    }

    override fun processNPC() {
        if (!isDead && !hasFinished() && !owner.isDead && !owner.hasFinished() && combat.target == null)
            setCombatTarget(owner)
        super.processNPC()
    }

    override fun handlePreHit(hit: Hit) {
        if (hit.look == HitLook.MAGIC_DAMAGE) {
            val spell = hit.getData("combatSpell") as? CombatSpell
            if (spell?.isFireSpell != true) {
                hit.damage = 0
                return
            }
            hit.damage *= 2
        }
        super.handlePreHit(hit)
    }

    override fun drop() {
        owner.sendNPCKill(definitions.name)
        World.addGroundItem(Item(Obj._3dose2restore), tile, owner)
        World.addGroundItem(Item(Obj.chocolate_cake, 2), tile, owner)
    }
}

@ServerStartupEvent
fun mapKamilCombat() {
    npcCombat(Npc.icediamond_icewarrior) { npc, target ->
        if (npc.withinDistance(target.tile, 1) && Utils.random(2) == 0) {
            npc.anim(Seq.human_sword_stab)
            CombatScript.delayHit(npc, 0, target, Hit(npc, CombatScript.getMaxHit(npc, CombatStyle.MELEE, target), HitLook.MELEE_DAMAGE))
        } else {
            npc.anim(Seq.seq_1979)
            CombatScript.delayHit(npc, 1, target, Hit(npc, 50, HitLook.MAGIC_DAMAGE)) {
                target.spotAnim(Spotanim.ice_barrage_impact)
                target.freeze(8)
                target.stun(8)
            }
        }
        4
    }
}
