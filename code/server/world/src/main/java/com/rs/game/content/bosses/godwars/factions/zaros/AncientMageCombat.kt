package com.rs.game.content.bosses.godwars.factions.zaros

import com.rs.game.World
import com.rs.game.content.Effect
import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Spotanim

private const val HIT_DELAY = 2
private const val FREEZE_SECONDS = 10
private val PROJECTILE_HEIGHTS = 18 to 18

private enum class AncientSpell(val projectile: Int) {
    SMOKE(Spotanim.smoke_blitz_travel),
    SHADOW(Spotanim.spell_shadow_blitz_travel),
    BLOOD(Spotanim.blood_blitz_travel),
    ICE(Spotanim.ice_blitz_travel),
}

class AncientMageCombat : CombatScript() {
    override fun getKeys() = arrayOf<Any>(Npc.godwars2_ancient_zaros_mage)

    override fun attack(npc: NPC, target: Entity): Int {
        val defs = npc.combatDefinitions
        val spell = AncientSpell.entries[Utils.getRandomInclusive(AncientSpell.entries.size - 1)]
        val hit = Hit.magic(npc, getMaxHit(npc, defs.maxHit, CombatStyle.MAGIC, target))
        npc.anim(defs.attackEmote)
        World.sendProjectile(npc, target, spell.projectile, PROJECTILE_HEIGHTS, 50, 8, 0)
        delayHit(npc, HIT_DELAY, target, hit)
        when (spell) {
            AncientSpell.BLOOD -> npc.heal(hit.damage / 3)
            AncientSpell.ICE -> if (hit.damage > 0 && !target.hasEffect(Effect.FREEZE)) {
                target.spotAnim(Spotanim.ice_barrage_impact)
                target.freeze(Ticks.fromSeconds(FREEZE_SECONDS))
            }
            else -> {}
        }
        return npc.attackSpeed
    }
}
