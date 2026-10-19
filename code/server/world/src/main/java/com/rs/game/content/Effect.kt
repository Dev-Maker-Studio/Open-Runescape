package com.rs.game.content

import com.rs.game.content.combat.BakriminelBolts
import com.rs.game.content.combat.CombatDefinitions
import com.rs.game.content.minigames.herblorehabitat.JadinkoType
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Graphic
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim

enum class Effect(
    val displayName: String? = null,
    val isRemoveOnDeath: Boolean = true,
    val buffBar: BuffBarEntry? = null,
) {
    SKULL(buffBar = spriteIcon(Graphic.headicons_pk_interface_0, "You will not keep any items if you die.")) {
        override fun apply(entity: Entity) {
            if (entity is Player) entity.appearance.generateAppearanceData()
        }

        override fun expire(entity: Entity) {
            if (entity is Player) entity.appearance.generateAppearanceData()
        }
    },
    ANTIPOISON("poison immunity", buffBar = objIcon(Obj._4doseantipoison, "You are immune to poison.")) {
        override fun apply(entity: Entity) {
            entity.poison.reset()
        }
    },
    STUN(buffBar = spriteIcon(Graphic.buff_bar_stunned, "You are held in place and cannot move.")),
    FREEZE(buffBar = spriteIcon(Graphic.buff_bar_bound, "You are frozen in place.")),
    FREEZE_BLOCK,
    MIASMIC_SLOWDOWN(buffBar = spriteIcon(Graphic.magicon2_42, "Your attacks are slower than normal.")),
    MIASMIC_BLOCK,
    TELEBLOCK("teleblock", buffBar = spriteIcon(Graphic.buff_bar_teleport_block, "You cannot teleport out of the Wilderness.")),
    LIVID_FISH_COOKING_BOOST("Reduce The Fish I Burn Wish",
        buffBar = objIcon(Obj.gauntlets_of_cooking, "You are less likely to burn food.")),
    LIVID_PLANKS_BOOST("More Planks Please Wish",
        buffBar = spriteIcon(Graphic.graphic_581, "Plank Make sometimes produces an extra plank.")),
    LUNARS_SPELLBOOK_SWAP("Spellbook Swap",
        buffBar = spriteIcon(Graphic.graphic_582, "Your spellbook returns to Lunar when this ends.")) {
        override fun sendWarnings() = false

        override fun expire(entity: Entity) {
            if (entity !is Player) return
            entity.combatDefinitions.setSpellbook(CombatDefinitions.Spellbook.LUNAR)
            entity.combatDefinitions.setAutoCastSpell(null)
            entity.sendMessage("Your spellbook has returned to Lunar Spellbook.")
        }
    },
    LUNARS_DISRUPTION_SHIELD("disruption shield") {
        override fun sendWarnings() = false

        override fun apply(entity: Entity) {
            if (entity is Player) entity.sendMessage("You are protected by a Disruption Shield.")
        }
    },
    LUNARS_DREAM("dream", buffBar = spriteIcon(Graphic.graphic_580, "You are dreaming, and regain life points as you rest.")) {
        override fun sendWarnings() = false
    },
    DOUBLE_XP("double xp", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.lmm_xp_lamp, "You earn double experience.")),
    ANTIFIRE("antifire", buffBar = spriteIcon(Graphic.buff_bar_anti_dragonfire, "Dragonfire does less damage to you.")),
    SUPER_ANTIFIRE("super-antifire", buffBar = spriteIcon(Graphic.buff_bar_anti_dragonfire, "Dragonfire does far less damage to you.")),
    PRAYER_RENEWAL("prayer renewal", buffBar = objIcon(Obj._4doseprayerrenew, "Your Prayer points are steadily restored.")) {
        override fun tick(entity: Entity, tickNumber: Long) {
            if (entity !is Player || entity.prayer.hasFullPoints()) return
            entity.prayer.restorePrayer(entity.skills.getLevelForXp(Constants.PRAYER) * 4.3 / 600.0 + 0.2)
            if (tickNumber % 25 == 0L) entity.spotAnim(Spotanim.lore_boost_receive)
        }
    },
    BARON_SHARK(buffBar = objIcon(Obj.golden_shark, "You are steadily regaining life points.")) {
        override fun tick(entity: Entity, tickNumber: Long) {
            if (tickNumber % 2 == 0L) entity.heal(10)
        }
    },
    STAFF_OF_LIGHT_SPEC("staff of light protection",
        buffBar = objIcon(Obj.staff_of_light, "Melee damage against you is halved.")),
    JUJU_MINING("juju mining potion", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.mining_potion4, "Mined ore is sometimes sent to your bank.")),
    JUJU_MINE_BANK,
    JUJU_WOODCUTTING("juju woodcutting potion", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.woodcutting_potion4, "Logs you cut are sometimes sent to your bank.")),
    JUJU_WC_BANK,
    JUJU_FARMING("juju farming potion", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.farming_potion4, "Harvesting herbs sometimes yields an extra one.")),
    JUJU_FISHING("juju fishing potion", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.advanced_fishing_potion4, "Sharks you catch are sometimes raw baron sharks.")),
    SCENTLESS("scentless potion", isRemoveOnDeath = false),
    JUJU_HUNTER("juju hunter potion", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.advanced_hunter_potion4, "Jadinkos that need juju can be attracted to the habitat.")),
    SARA_BLESSING("Saradomin's blessing", isRemoveOnDeath = false),
    GUTHIX_GIFT("Guthix's gift", isRemoveOnDeath = false),
    ZAMMY_FAVOR("Zamorak's favour", isRemoveOnDeath = false),
    MAMBO_WARDING("Papa Mambo's Warding", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.ecosystem_striped_vine, "Papa Mambo is keeping one type of jadinko away.")) {
        override fun sendWarnings() = false

        override fun expire(entity: Entity) {
            if (entity !is Player) return
            val type = entity.savingAttributes.remove("MAMBO_WARDING_TYPE") as? JadinkoType ?: return
            entity.sendMessage("<col=FF0000>Papa Mambo has stopped warding off ${type.name.lowercase().replace('_', ' ')} jadinkos!")
        }
    },
    REV_IMMUNE("immunity to revenants", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.obj_11095, "Revenants cannot damage you.")),
    REV_AGGRO_IMMUNE("revenant aggression immunity", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.obj_11095, "Revenants will not attack you.")),
    CHARGED("god charge", isRemoveOnDeath = false,
        buffBar = spriteIcon(Graphic.magicon2_3, "God spells cast with the matching god cape hit harder.")),
    DUNG_HS_SCROLL_BOOST("hoardstalker boost",
        buffBar = objIcon(Obj.rand_loreskill_01_spell, "Your skilling levels are invisibly boosted.")),
    AGGRESSION_POTION("aggression potion", isRemoveOnDeath = false),
    OVERLOAD_PVP_REDUCTION,
    MELEE_IMMUNE(buffBar = objIcon(Obj.pvpw_spear, "Melee attacks cannot damage you.")),
    BONFIRE("bonfire boost", isRemoveOnDeath = false,
        buffBar = spriteIcon(Graphic.buff_bar_firemaking, "The bonfire's warmth has raised your maximum life points.")) {
        override fun apply(entity: Entity) {
            if (entity is Player) entity.equipment.refreshConfigs(false)
        }

        override fun expire(entity: Entity) {
            if (entity is Player) entity.equipment.refreshConfigs(false)
        }
    },
    BLOOD_NECKLACE("blood necklace") {
        override fun sendWarnings() = false

        override fun tick(entity: Entity, tickNumber: Long) {
            if (tickNumber % Ticks.fromSeconds(10) != 0L || entity !is Player || !entity.dungManager.isInsideDungeon) return
            val targets = entity.queryNearbyNPCsByTileRange(1) { npc ->
                !npc.isDead && npc.withinDistance(entity, 1) && npc.definitions.hasAttackOption() &&
                    entity.controllerManager.canHit(npc) && npc.combatTarget is Player
            }
            for (npc in targets) {
                val damage = 40 * entity.skills.combatLevelWithSummoning / 138
                npc.applyHit(Hit(entity, damage, Hit.HitLook.TRUE_DAMAGE))
                entity.heal(damage)
            }
        }
    },
    STONE_OF_JAS("Stone of Jas") {
        override fun apply(entity: Entity) {
            entity.sync(Seq.mah5_player_receive_boost, Spotanim.mah5_player_receive_boost_spotanim)
            if (entity is Player) entity.sendMessage("The residual energy from the Stone of Jas empowers you, increasing your damage.")
        }

        override fun tick(entity: Entity, tickNumber: Long) {
            if (tickNumber % 50 == 0L) entity.spotAnim(Spotanim.mah5_player_receive_boost_spotanim)
        }
    },
    EXCALIBUR_HEAL(buffBar = objIcon(Obj.obj_14632, "The Enhanced Excalibur is restoring your life points.")) {
        override fun tick(entity: Entity, tickNumber: Long) {
            if (tickNumber % 7 != 0L) return
            entity.heal(40)
            entity.spotAnim(Spotanim.lore_spinner_heal_spot)
        }
    },
    OVERLOAD("overload", buffBar = objIcon(Obj._4doseoverload, "Your combat stats are boosted.")) {
        override fun apply(entity: Entity) {
            if (entity is Player) Potion.applyOverloadEffect(entity)
        }

        override fun tick(entity: Entity, tickNumber: Long) {
            if (tickNumber % 25 == 0L && entity is Player) Potion.applyOverloadEffect(entity)
        }

        override fun expire(entity: Entity) {
            if (entity !is Player) return
            if (!entity.isDead) {
                for (stat in intArrayOf(Constants.ATTACK, Constants.STRENGTH, Constants.DEFENSE, Constants.MAGIC, Constants.RANGE)) {
                    val base = entity.skills.getLevelForXp(stat)
                    if (entity.skills.getLevel(stat) > base) entity.skills.set(stat, base)
                }
                entity.heal(500)
            }
            entity.soundEffect(Sound.sound_2607, false)
            entity.sendMessage("<col=480000>The effects of overload have worn off and you feel normal again.")
        }
    },
    OOG_BANDOS_POOL("Bandos pool"),
    OOG_SALTWATER_POOL("saltwater spring", buffBar = spriteIcon(Graphic.buff_bar_saltwater_flask, "Your run energy is steadily restored.")) {
        override fun apply(entity: Entity) {
            if (entity is Player) entity.restoreRunEnergy(entity.skills.getLevel(Constants.AGILITY).toDouble())
        }

        override fun tick(entity: Entity, tickNumber: Long) {
            if (tickNumber % Ticks.fromSeconds(3) == 0L && entity is Player)
                entity.restoreRunEnergy(entity.skills.getLevel(Constants.AGILITY).toDouble())
        }
    },
    OOG_THERMAL_POOL("thermal bath", buffBar = spriteIcon(Graphic.buff_bar_thermal_flask, "Your maximum life points are raised, and you are immune to poison.")) {
        override fun apply(entity: Entity) {
            if (entity !is Player) return
            entity.refreshHitPoints()
            entity.addEffect(ANTIPOISON, Ticks.fromMinutes(15).toLong())
        }

        override fun expire(entity: Entity) {
            if (entity is Player) entity.refreshHitPoints()
        }
    },
    FARMERS_AFFINITY("farmer's affinity",
        buffBar = spriteIcon(Graphic.buff_bar_hunter, "You push through Puro-Puro's wheat more quickly.")),
    PATCH_BOMB("patch bomb", buffBar = spriteIcon(Graphic.buff_bar_farming, "Your farming patches are being tended.")),
    SHOOTING_STAR_MINING_BUFF("star sprite's power", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.star_dust, "Mining sometimes yields an extra ore.")),
    EVIL_TREE_WOODCUTTING_BUFF("evil tree magic", isRemoveOnDeath = false,
        buffBar = objIcon(Obj.eviltree_kindling, "Logs you cut are sent to your bank.")),
    BAKRIMINEL_MAGIC_SLOW("bakriminel magic slow",
        buffBar = spriteIcon(Graphic.buff_bar_stunned, "Your magic casting is slowed.")) {
        override fun expire(entity: Entity) {
            if (entity is Player) BakriminelBolts.clearMagicSlow(entity)
        }
    },
    BAKRIMINEL_BLEED("bleeding",
        buffBar = spriteIcon(Graphic.buff_bar_bound, "You are bleeding.")) {
        override fun tick(entity: Entity, tickNumber: Long) {
            if (entity is Player) BakriminelBolts.bleedTick(entity, tickNumber)
        }

        override fun expire(entity: Entity) {
            if (entity is Player) BakriminelBolts.clearBleedStacks(entity)
        }
    };

    open fun apply(entity: Entity) {}

    open fun tick(entity: Entity, tickNumber: Long) {}

    open fun expire(entity: Entity) {}

    open fun sendWarnings() = displayName != null

    fun get30SecWarning() = "<col=FF0000>Your $displayName is going to run out in 30 seconds!"

    fun getExpiryMessage() = "<col=FF0000>Your $displayName has run out!"

    val isOnBuffBar get() = buffBar != null
}
