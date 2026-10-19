package com.rs.game.content.skills.slayer.npcs.combat

import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.combat.CombatScript

class SpiritualMage : CombatScript() {
    override fun getKeys() = arrayOf<Any>(6257, 6221, 6278)

    override fun attack(npc: NPC, target: Entity): Int {
        val defs = npc.combatDefinitions
        npc.anim(defs.attackEmote)
        val damage = getMaxHit(npc, defs.maxHit, CombatStyle.MAGIC, target)
        npc.spotAnim(defs.attackGfx)
        delayHit(npc, 2, target, Hit.magic(npc, damage))
        if (damage > 0)
            npc.schedule {
                wait(2)
                target.spotAnim(defs.attackProjectile)
            }
        return npc.attackSpeed + 2
    }
}
