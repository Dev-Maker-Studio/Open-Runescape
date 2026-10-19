package com.rs.game.content.quests.deserttreasure

import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.npcCombat
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

class Dessous(owner: Player, tile: Tile, private val blessedLure: Boolean) :
    OwnedNPC(owner, Npc.blooddiamond_vampirewarrior, tile, true) {
    private var teleports = 0
    private var nextTeleport = 0L

    init {
        setAutoDespawnAtDistance(false)
    }

    override fun processNPC() {
        if (!isDead && !hasFinished() && combat.target === owner && tickCounter >= nextTeleport &&
            !withinDistance(owner, 4)) {
            tele(owner.transform(0, 1, 0))
            teleports++
            if (teleports >= 3) {
                forceTalk("I'm bored of toying with you.")
                removeCombatTarget()
                finish()
                return
            }
            nextTeleport = tickCounter + 5
            setCombatTarget(owner)
        }
        super.processNPC()
    }

    override fun removeHitpoints(hit: Hit) {
        if (!blessedLure && hit.damage >= hitpoints) {
            hit.damage = 0
            setHitpoints(maxHitpoints)
            forceTalk("You will never defeat me")
        }
        super.removeHitpoints(hit)
    }
}

@ServerStartupEvent
fun mapDessousCombat() {
    npcCombat(Npc.blooddiamond_vampirewarrior) { npc, target ->
        if (Utils.random(3) == 0) {
            npc.anim(Seq.vampire_summonbats)
            target.spotAnim(Spotanim.bat_cluster_attack)
            CombatScript.delayHit(npc, 2, target, Hit.magic(npc, 50))
            CombatScript.delayHit(npc, 2, target, Hit.range(npc, 50))
        } else {
            npc.anim(Seq.human_sword_slash)
            CombatScript.delayHit(npc, 0, target, Hit.melee(npc, CombatScript.getMaxHit(npc, CombatStyle.MELEE, target)))
        }
        npc.attackSpeed
    }
}
