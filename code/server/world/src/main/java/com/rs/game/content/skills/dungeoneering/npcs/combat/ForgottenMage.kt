package com.rs.game.content.skills.dungeoneering.npcs.combat

import com.rs.game.World
import com.rs.game.content.combat.CombatStyle
import com.rs.game.content.skills.dungeoneering.npcs.SkeletalAdventurer
import com.rs.game.content.skills.dungeoneering.npcs.YkLagorMage
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.SpotAnim

class ForgottenMage : CombatScript() {

    private enum class Debuff(val stat: Int, val drainRate: Double, val freezeTicks: Int) {
        CONFUSE(Constants.ATTACK, 0.95, 0),
        WEAKEN(Constants.STRENGTH, 0.95, 0),
        CURSE(Constants.DEFENSE, 0.95, 0),
        BIND(-1, 0.0, 8),
        SNARE(-1, 0.0, 12),
        ENTANGLE(-1, 0.0, 16),
        VULNERABILITY(Constants.DEFENSE, 0.90, 0),
        ENFEEBLE(Constants.STRENGTH, 0.90, 0),
        STUN(Constants.ATTACK, 0.90, 0);

        val freezes get() = freezeTicks > 0
    }

    override fun getKeys() = arrayOf<Any>("Forgotten mage")

    override fun attack(npc: NPC, target: Entity): Int {
        val tier = when {
            npc is YkLagorMage -> 3
            npc is SkeletalAdventurer -> 8
            npc.id == Npc.rand_human_magic_lvl1_1 -> 0
            else -> ((npc.id - Npc.rand_human_magic_lvl1_1) / 5 - 1).coerceAtLeast(0)
        }
        val possibleAttacks = ATTACK_TYPES[tier]
        val attack = possibleAttacks[Utils.random(possibleAttacks.size)]
        val boss = npc is YkLagorMage || npc is SkeletalAdventurer
        if (boss && attack < DEBUFF_COUNT) return 0
        if (!boss && target is Player && target.equipment.containsOneItem(Obj.rand_shadow_silk_hood, Obj.rand_shadow_silk_hood_b) && !target.tempAttribs.getB(SHADOW_SILK_DISABLED)) {
            sendAntiSilkHoodSpell(npc, target)
            return 5
        }
        if (attack < DEBUFF_COUNT)
            sendDebuff(npc, target, ATTACK_ANIMATIONS[attack], START_GRAPHICS[attack], HIT_GRAPHICS[attack], PROJECTILES[attack], Debuff.entries[attack])
        else
            sendCombatSpell(npc, target, ATTACK_ANIMATIONS[attack], START_GRAPHICS[attack], HIT_GRAPHICS[attack], PROJECTILES[attack])
        return 5
    }

    private fun sendCombatSpell(npc: NPC, target: Entity, attackAnim: Int, startGfx: Int, hitGfx: Int, projectileId: Int) {
        npc.anim(attackAnim)
        if (startGfx != -1) npc.spotAnim(startGfx)
        if (projectileId == Spotanim.spell_update_earth_proj_5) {
            World.sendProjectile(npc, target, Spotanim.spell_update_fire_proj_7, 18 to 18, 50, 5, 3)
            World.sendProjectile(npc, target, Spotanim.spell_update_fire_proj_8, 18 to 18, 50, 5, 20)
            World.sendProjectile(npc, target, Spotanim.spell_update_fire_proj_8, 18 to 18, 50, 5, 110)
        } else
            World.sendProjectile(npc, target, projectileId, 18 to 18, 50, 5, 3)
        delayHit(npc, 2, target, Hit.magic(npc, getMaxHit(npc, npc.maxHit, CombatStyle.MAGIC, target)))
        if (hitGfx == -1) return
        WorldTasks.schedule(2) { target.spotAnim(SpotAnim(hitGfx, 0, 85)) }
    }

    private fun sendDebuff(npc: NPC, target: Entity, attackAnim: Int, startGfx: Int, hitGfx: Int, projectileId: Int, debuff: Debuff) {
        npc.anim(attackAnim)
        npc.spotAnim(SpotAnim(startGfx, 0, 50))
        World.sendProjectile(npc, target, projectileId, 39 to 18, 55, 5, 5)
        if (hitGfx <= 0) return
        WorldTasks.schedule(2) {
            val player = target as? Player ?: return@schedule
            if (player.prayer.isProtectingMage) return@schedule
            if (debuff.freezes)
                player.freeze(debuff.freezeTicks, true)
            else if (player.skills.getLevel(debuff.stat) > player.skills.getLevelForXp(debuff.stat) * 0.93)
                player.skills.set(debuff.stat, (player.skills.getLevel(debuff.stat) * debuff.drainRate).toInt())
        }
        target.spotAnim(SpotAnim(hitGfx, 140, 85))
    }

    private fun sendAntiSilkHoodSpell(npc: NPC, player: Player) {
        npc.anim(Seq.dream_player_monsterexam_statspy)
        npc.spotAnim(Spotanim.dream_spell_monsterexam_statspy_spotanim)
        WorldTasks.schedule(2) {
            player.spotAnim(SpotAnim(Spotanim.quest_lunar_spell_energy_exchange_2, 0, 50))
            player.tempAttribs.setB(SHADOW_SILK_DISABLED, true)
            player.sendMessage("<col=ff6f69>Your shadow silk hood loses its power...")
            WorldTasks.delay(Ticks.fromMinutes(2)) {
                if (!player.hasStarted()) return@delay
                if (player.tempAttribs.getB(SHADOW_SILK_DISABLED))
                    player.sendMessage("<col=96ceb4>Your shadow silk hood returns its power...")
                player.tempAttribs.setB(SHADOW_SILK_DISABLED, false)
            }
        }
    }

    private companion object {
        const val SHADOW_SILK_DISABLED = "ShadowSilkSpellDisable"
        const val DEBUFF_COUNT = 9

        val ATTACK_TYPES = arrayOf(
            intArrayOf(0, 9, 10),
            intArrayOf(10, 11, 0, 12, 1),
            intArrayOf(12, 13, 0, 1, 3, 2, 14),
            intArrayOf(14, 15, 0, 3, 16),
            intArrayOf(17, 18, 0, 1, 2, 3, 19),
            intArrayOf(20, 21, 0, 1, 2, 22, 4),
            intArrayOf(22, 23, 0, 1, 4, 6, 24),
            intArrayOf(24, 25, 0, 6, 7, 5, 26),
            intArrayOf(26, 27, 6, 7, 8, 5, 28),
            intArrayOf(28, 29, 6, 7, 8, 5, 30),
            intArrayOf(30, 6, 7, 8, 5),
        )

        val ATTACK_ANIMATIONS = intArrayOf(
            Seq.human_caststrike, Seq.human_castconfuse, Seq.human_castcrumbleundead,
            Seq.human_castentangle, Seq.human_castentangle, Seq.human_castentangle,
            Seq.human_caststun, Seq.human_caststun, Seq.human_caststun,
            Seq.seq_14221, Seq.seq_14222, Seq.seq_14221, Seq.seq_14221,
            Seq.seq_14220, Seq.seq_14220, Seq.seq_14222, Seq.seq_14223,
            Seq.seq_14221, Seq.seq_14220, Seq.seq_14222, Seq.seq_14223,
            Seq.seq_14221, Seq.seq_14220, Seq.seq_14222, Seq.seq_14223,
            Seq.spells_update_player_air_launch, Seq.seq_10542, Seq.seq_14209, Seq.spells_update_player_fire_launch,
        )

        val START_GRAPHICS = intArrayOf(
            Spotanim.confuse_casting, Spotanim.weaken_casting, Spotanim.curse_casting,
            Spotanim.entangle_casting, Spotanim.entangle_casting, Spotanim.entangle_casting,
            Spotanim.vulnerability_casting, Spotanim.enfeeble_casting, Spotanim.stun_casting,
            -1, Spotanim.spotanim_2701, Spotanim.spotanim_2713, Spotanim.spotanim_2728,
            -1, Spotanim.spell_update_water_proj_5, Spotanim.spotanim_2709, Spotanim.spotanim_2714, Spotanim.spotanim_2728,
            -1, Spotanim.spotanim_2701, Spotanim.spotanim_2715, Spotanim.spotanim_2728,
            -1, Spotanim.spotanim_2702, Spotanim.spotanim_2716, Spotanim.spotanim_2728,
            Spotanim.spells_update_air_spotanim, Spotanim.spotanim_2701, Spotanim.spotanim_2717, Spotanim.spotanim_2728,
        )

        val HIT_GRAPHICS = intArrayOf(
            Spotanim.confuse_impact, Spotanim.weaken_impact, Spotanim.curse_impact,
            Spotanim.spotanim_181, Spotanim.spotanim_180, Spotanim.entangle_impact,
            Spotanim.vulnerability_impact, Spotanim.enfeeble_impact, Spotanim.weaken_impact,
            Spotanim.spotanim_2700, Spotanim.spotanim_2708, Spotanim.spotanim_2723, Spotanim.spotanim_2737,
            Spotanim.spotanim_2700, Spotanim.spell_update_water_proj_2, Spotanim.spotanim_2724, Spotanim.spotanim_2738,
            Spotanim.spotanim_2700, Spotanim.spotanim_2710, Spotanim.spotanim_2725, Spotanim.spotanim_2739,
            Spotanim.spotanim_2700, Spotanim.spotanim_2710, Spotanim.spotanim_2726, Spotanim.spotanim_2740,
            Spotanim.spotanim_2700, Spotanim.spotanim_2712, Spotanim.spotanim_2727, Spotanim.spotanim_2741,
        )

        val PROJECTILES = intArrayOf(
            Spotanim.confuse_travel, Spotanim.weaken_travel, Spotanim.curse_travel,
            Spotanim.entangle_travel, Spotanim.entangle_travel, Spotanim.entangle_travel,
            Spotanim.curse_travel_2, Spotanim.enfeeble_travel, Spotanim.stun_travel,
            Spotanim.spotanim_2699, Spotanim.spell_update_water_proj, Spotanim.spell_update_earth_proj, Spotanim.spell_update_fire_proj,
            Spotanim.spotanim_2699, Spotanim.spell_update_water_proj_2, Spotanim.spell_update_earth_proj_2, Spotanim.spell_update_fire_proj_3,
            Spotanim.spotanim_2699, Spotanim.spell_update_water_proj_3, Spotanim.spell_update_earth_proj_3, Spotanim.spell_update_fire_proj_5,
            Spotanim.spotanim_2699, Spotanim.spell_update_water_proj_4, Spotanim.spell_update_earth_proj_4, Spotanim.spell_update_fire_proj_7,
            Spotanim.spell_update_air_proj_5, Spotanim.spell_update_water_proj_5, Spotanim.spell_update_earth_proj_5, -1,
        )
    }
}
