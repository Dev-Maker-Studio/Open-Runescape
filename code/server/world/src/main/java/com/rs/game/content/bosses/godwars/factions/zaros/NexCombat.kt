package com.rs.game.content.bosses.godwars.factions.zaros

import com.rs.game.World
import com.rs.game.content.bosses.godwars.factions.zaros.attack.BloodSacrifice
import com.rs.game.content.bosses.godwars.factions.zaros.attack.ContainThis
import com.rs.game.content.bosses.godwars.factions.zaros.attack.Drag
import com.rs.game.content.bosses.godwars.factions.zaros.attack.EmbraceDarkness
import com.rs.game.content.bosses.godwars.factions.zaros.attack.IcePrison
import com.rs.game.content.bosses.godwars.factions.zaros.attack.NexAttack
import com.rs.game.content.bosses.godwars.factions.zaros.attack.NoEscape
import com.rs.game.content.bosses.godwars.factions.zaros.attack.ShadowTraps
import com.rs.game.content.bosses.godwars.factions.zaros.attack.Siphon
import com.rs.game.content.bosses.godwars.factions.zaros.attack.Virus
import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim

private const val MELEE_MAX = 360
private const val MAGIC_MAX = 250
private const val ZAROS_MAX = 350
private const val SHADOW_MAX = 800
private const val SHADOW_RANGE = 10
private const val POISON_DAMAGE = 88
private const val FREEZE_SECONDS = 18
private const val PROTECTED_FREEZE_SECONDS = 3
private const val PRAYER_SWITCH_INTERVAL = 3
private val PROJECTILE_HEIGHTS = 41 to 16

class NexCombat : CombatScript() {
    override fun getKeys() = arrayOf<Any>("Nex")

    override fun attack(npc: NPC, target: Entity): Int {
        val nex = npc as? Nex ?: return minionAttack(npc, target)
        if (nex.tempAttribs.getB("siphoning")) return 0
        specialAttack(nex)?.let { return nex.performAttack(target, it) }
        return autoAttack(nex, target)
    }

    /** The scripted attacks each phase mixes into its rotation, on the attack counts Nex uses them. */
    private fun specialAttack(nex: Nex): NexAttack? {
        val count = nex.attackCount
        return when (nex.phase) {
            NexPhase.SMOKE -> when {
                count % 12 == 0 -> if (Utils.random(4) == 0) Drag else NoEscape
                count % 6 == 0 -> Virus
                else -> null
            }
            NexPhase.SHADOW -> when {
                count == 0 -> EmbraceDarkness
                count % 5 == 0 -> ShadowTraps
                else -> null
            }
            NexPhase.BLOOD -> when {
                count % 8 == 0 -> Siphon
                count % 4 == 0 -> BloodSacrifice
                else -> null
            }
            NexPhase.ICE -> when {
                count % 8 == 0 -> ContainThis
                count % 4 == 0 -> IcePrison
                else -> null
            }
            NexPhase.ZAROS -> null
        }
    }

    private fun autoAttack(nex: Nex, target: Entity): Int {
        if (nex.isFollowTarget && !nex.inMeleeRange(target)) {
            nex.calcFollow(target, true)
            if (nex.shouldStopMeleeing()) nex.isFollowTarget = false
            return 0
        }
        val melee = nex.isFollowTarget
        nex.isFollowTarget = Utils.random(2) == 0
        if (melee) meleeAttack(nex, target) else spellAttack(nex, target)
        if (nex.phase == NexPhase.ZAROS && nex.attackCount % PRAYER_SWITCH_INTERVAL == 0) nex.switchPrayers()
        nex.incrementAttack()
        return nex.attackSpeed
    }

    private fun meleeAttack(nex: Nex, target: Entity) {
        delayHit(nex, 0, target, Hit.melee(nex, getMaxHit(nex, MELEE_MAX, CombatStyle.MELEE, target)))
        nex.anim(Seq.godwars2_zaros_attack)
    }

    private fun spellAttack(nex: Nex, target: Entity) {
        when (nex.phase) {
            NexPhase.SMOKE -> {
                nex.anim(Seq.godwars2_zaros_cast_attack)
                nex.spotAnim(Spotanim.zaros_magic_launch_spotanim)
                nex.possibleTargets.forEach { smokeHit(nex, it, MAGIC_MAX, poisons = true) }
            }
            NexPhase.SHADOW -> {
                nex.anim(Seq.godwars2_zaros_cast_attack)
                for (t in nex.possibleTargets) {
                    val distance = Utils.getDistance(t.x, t.y, nex.x, nex.y).toInt()
                    if (distance > SHADOW_RANGE) continue
                    val falloff = SHADOW_MAX - (distance * SHADOW_MAX / (SHADOW_RANGE + 1))
                    val delay = projectile(nex, t, Spotanim.spell_shadow_blitz_travel, impact = true)
                    delayHit(nex, delay, t, Hit.range(nex, getMaxHit(nex, falloff, CombatStyle.RANGE, t)))
                }
            }
            NexPhase.BLOOD -> {
                nex.anim(Seq.godwars2_zaros_alternate_cast_attack)
                val delay = projectile(nex, target, Spotanim.blood_blitz_travel, impact = false)
                delayHit(nex, delay, target, Hit.magic(nex, getMaxHit(nex, MAGIC_MAX, CombatStyle.MAGIC, target)))
            }
            NexPhase.ICE -> {
                nex.anim(Seq.godwars2_zaros_alternate_cast_attack)
                for (t in nex.possibleTargets) {
                    val damage = getMaxHit(nex, MAGIC_MAX, CombatStyle.MAGIC, t)
                    delayHit(nex, projectile(nex, t, Spotanim.ice_blitz_travel, impact = false), t, Hit.magic(nex, damage))
                    if (damage <= 0 || Utils.getRandomInclusive(5) != 0) continue
                    val protecting = (t as? Player)?.prayer?.isProtectingMage == true
                    t.freeze(Ticks.fromSeconds(if (protecting) PROTECTED_FREEZE_SECONDS else FREEZE_SECONDS), true)
                    t.spotAnim(Spotanim.ice_barrage_impact)
                }
            }
            NexPhase.ZAROS -> {
                nex.anim(Seq.godwars2_zaros_cast_attack)
                nex.possibleTargets.forEach { smokeHit(nex, it, ZAROS_MAX, poisons = false) }
            }
        }
    }

    private fun smokeHit(nex: NPC, target: Entity, maxHit: Int, poisons: Boolean) {
        val damage = getMaxHit(nex, maxHit, CombatStyle.MAGIC, target)
        delayHit(nex, projectile(nex, target, Spotanim.zaros_magic_spotanim, impact = true), target, Hit.magic(nex, damage))
        if (poisons && damage > 0 && Utils.getRandomInclusive(5) == 0) target.poison.makePoisoned(POISON_DAMAGE)
    }

    private fun projectile(nex: NPC, target: Entity, spotAnimId: Int, impact: Boolean): Int =
        World.sendProjectile(nex, target, spotAnimId, PROJECTILE_HEIGHTS, 41, 10, 16, 0) {
            if (impact) target.spotAnim(Spotanim.zaros_magic_impact_spotanim)
        }.taskDelay

    /** Nex's ancient mages fall back to her smoke attack when one of them is fought without her. */
    private fun minionAttack(npc: NPC, target: Entity): Int {
        npc.anim(Seq.godwars2_zaros_cast_attack)
        npc.spotAnim(Spotanim.zaros_magic_launch_spotanim)
        npc.possibleTargets.forEach { smokeHit(npc, it, MAGIC_MAX, poisons = true) }
        return npc.attackSpeed
    }
}
