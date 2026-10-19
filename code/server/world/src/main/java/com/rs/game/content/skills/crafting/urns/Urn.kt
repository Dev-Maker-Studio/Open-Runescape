package com.rs.game.content.skills.crafting.urns

import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Animation

enum class Urn(
    val unfired: Int,
    val noRune: Int,
    val empty: Int,
    val started: Int,
    val full: Int,
    runeInSeq: Int,
    teleportSeq: Int,
    val skill: Int,
    val level: Int,
    val capacity: Double,
    val wheelXp: Double,
    val fireXp: Double,
    val activator: Int
) {
    CRACKED_SMITHING(Obj.urns_smithing1_unfired, Obj.urns_smithing1_norune, Obj.urns_smithing1_rune, Obj.urns_smithing1_started, Obj.urns_smithing1_full, Seq.human_rune_in_urn_smithing_01, Seq.human_urn_teleport_smithing_01, Constants.SMITHING, 4, 200.0, 14.0, 23.1, Obj.firerune),
    FRAGILE_SMITHING(Obj.urns_smithing2_unfired, Obj.urns_smithing2_norune, Obj.urns_smithing2_rune, Obj.urns_smithing2_started, Obj.urns_smithing2_full, Seq.human_rune_in_urn_smithing_02, Seq.human_urn_teleport_smithing_02, Constants.SMITHING, 17, 312.5, 20.0, 31.8, Obj.firerune),
    PLAIN_SMITHING(Obj.urns_smithing3_unfired, Obj.urns_smithing3_norune, Obj.urns_smithing3_rune, Obj.urns_smithing3_started, Obj.urns_smithing3_full, Seq.human_rune_in_urn_smithing_03, Seq.human_urn_teleport_smithing_03, Constants.SMITHING, 35, 750.0, 29.0, 42.0, Obj.firerune),
    STRONG_SMITHING(Obj.urns_smithing4_unfired, Obj.urns_smithing4_norune, Obj.urns_smithing4_rune, Obj.urns_smithing4_started, Obj.urns_smithing4_full, Seq.human_rune_in_urn_smithing_04, Seq.human_urn_teleport_smithing_04, Constants.SMITHING, 49, 1250.0, 36.0, 52.0, Obj.firerune),
    DECORATED_SMITHING(Obj.urns_smithing5_unfired, Obj.urns_smithing5_norune, Obj.urns_smithing5_rune, Obj.urns_smithing5_started, Obj.urns_smithing5_full, Seq.human_rune_in_urn_smithing_05, Seq.human_urn_teleport_smithing_05, Constants.SMITHING, 80, 1625.0, 52.0, 78.0, Obj.firerune),
    CRACKED_WOODCUTTING(Obj.urns_woodcutting1_unfired, Obj.urns_woodcutting1_norune, Obj.urns_woodcutting1_rune, Obj.urns_woodcutting1_started, Obj.urns_woodcutting1_full, Seq.human_rune_in_urn_woodcutting_01, Seq.human_urn_teleport_woodcutting_01, Constants.WOODCUTTING, 4, 800.0, 14.0, 23.1, Obj.earthrune),
    FRAGILE_WOODCUTTING(Obj.urns_woodcutting2_unfired, Obj.urns_woodcutting2_norune, Obj.urns_woodcutting2_rune, Obj.urns_woodcutting2_started, Obj.urns_woodcutting2_full, Seq.human_rune_in_urn_woodcutting_02, Seq.human_urn_teleport_woodcutting_02, Constants.WOODCUTTING, 15, 2125.0, 19.0, 30.0, Obj.earthrune),
    PLAIN_WOODCUTTING(Obj.urns_woodcutting3_unfired, Obj.urns_woodcutting3_norune, Obj.urns_woodcutting3_rune, Obj.urns_woodcutting3_started, Obj.urns_woodcutting3_full, Seq.human_rune_in_urn_woodcutting_03, Seq.human_urn_teleport_woodcutting_03, Constants.WOODCUTTING, 44, 4125.0, 34.0, 48.0, Obj.earthrune),
    STRONG_WOODCUTTING(Obj.urns_woodcutting4_unfired, Obj.urns_woodcutting4_norune, Obj.urns_woodcutting4_rune, Obj.urns_woodcutting4_started, Obj.urns_woodcutting4_full, Seq.human_rune_in_urn_woodcutting_04, Seq.human_urn_teleport_woodcutting_04, Constants.WOODCUTTING, 61, 8312.5, 42.0, 58.2, Obj.earthrune),
    DECORATED_WOODCUTTING(Obj.urns_woodcutting5_unfired, Obj.urns_woodcutting5_norune, Obj.urns_woodcutting5_rune, Obj.urns_woodcutting5_started, Obj.urns_woodcutting5_full, Seq.human_rune_in_urn_woodcutting_05, Seq.human_urn_teleport_woodcutting_05, Constants.WOODCUTTING, 76, 9500.0, 50.0, 72.0, Obj.earthrune),
    CRACKED_FISHING(Obj.urns_fishing1_unfired, Obj.urns_fishing1_norune, Obj.urns_fishing1_rune, Obj.urns_fishing1_started, Obj.urns_fishing1_full, Seq.human_rune_in_urn_fishing_01, Seq.human_urn_teleport_fishing_01, Constants.FISHING, 1, 750.0, 13.0, 18.0, Obj.waterrune),
    FRAGILE_FISHING(Obj.urns_fishing2_unfired, Obj.urns_fishing2_norune, Obj.urns_fishing2_rune, Obj.urns_fishing2_started, Obj.urns_fishing2_full, Seq.human_rune_in_urn_fishing_02, Seq.human_urn_teleport_fishing_02, Constants.FISHING, 15, 1750.0, 19.0, 30.0, Obj.waterrune),
    PLAIN_FISHING(Obj.urns_fishing3_unfired, Obj.urns_fishing3_norune, Obj.urns_fishing3_rune, Obj.urns_fishing3_started, Obj.urns_fishing3_full, Seq.human_rune_in_urn_fishing_03, Seq.human_urn_teleport_fishing_03, Constants.FISHING, 41, 2500.0, 32.0, 46.8, Obj.waterrune),
    STRONG_FISHING(Obj.urns_fishing4_unfired, Obj.urns_fishing4_norune, Obj.urns_fishing4_rune, Obj.urns_fishing4_started, Obj.urns_fishing4_full, Seq.human_rune_in_urn_fishing_04, Seq.human_urn_teleport_fishing_04, Constants.FISHING, 53, 3000.0, 38.0, 54.0, Obj.waterrune),
    DECORATED_FISHING(Obj.urns_fishing5_unfired, Obj.urns_fishing5_norune, Obj.urns_fishing5_rune, Obj.urns_fishing5_started, Obj.urns_fishing5_full, Seq.human_rune_in_urn_fishing_05, Seq.human_urn_teleport_fishing_05, Constants.FISHING, 76, 9500.0, 50.0, 72.0, Obj.waterrune),
    CRACKED_COOKING(Obj.urns_cooking1_unfired, Obj.urns_cooking1_norune, Obj.urns_cooking1_rune, Obj.urns_cooking1_started, Obj.urns_cooking1_full, Seq.human_rune_in_urn_cooking_01, Seq.human_urn_teleport_cooking_01, Constants.COOKING, 1, 2000.0, 13.0, 18.0, Obj.firerune),
    FRAGILE_COOKING(Obj.urns_cooking2_unfired, Obj.urns_cooking2_norune, Obj.urns_cooking2_rune, Obj.urns_cooking2_started, Obj.urns_cooking2_full, Seq.human_rune_in_urn_cooking_02, Seq.human_urn_teleport_cooking_02, Constants.COOKING, 12, 2750.0, 18.0, 24.0, Obj.firerune),
    PLAIN_COOKING(Obj.urns_cooking3_unfired, Obj.urns_cooking3_norune, Obj.urns_cooking3_rune, Obj.urns_cooking3_started, Obj.urns_cooking3_full, Seq.human_rune_in_urn_cooking_03, Seq.human_urn_teleport_cooking_03, Constants.COOKING, 36, 4750.0, 30.0, 42.9, Obj.firerune),
    STRONG_COOKING(Obj.urns_cooking4_unfired, Obj.urns_cooking4_norune, Obj.urns_cooking4_rune, Obj.urns_cooking4_started, Obj.urns_cooking4_full, Seq.human_rune_in_urn_cooking_04, Seq.human_urn_teleport_cooking_04, Constants.COOKING, 51, 5250.0, 37.0, 52.5, Obj.firerune),
    DECORATED_COOKING(Obj.urns_cooking5_unfired, Obj.urns_cooking5_norune, Obj.urns_cooking5_rune, Obj.urns_cooking5_started, Obj.urns_cooking5_full, Seq.human_rune_in_urn_cooking_05, Seq.human_urn_teleport_cooking_05, Constants.COOKING, 81, 7737.5, 52.0, 78.0, Obj.firerune),
    CRACKED_MINING(Obj.urns_mining1_unfired, Obj.urns_mining1_norune, Obj.urns_mining1_rune, Obj.urns_mining1_started, Obj.urns_mining1_full, Seq.human_rune_in_urn_mining_01, Seq.human_urn_teleport_mining_01, Constants.MINING, 1, 437.5, 13.0, 16.8, Obj.earthrune),
    FRAGILE_MINING(Obj.urns_mining2_unfired, Obj.urns_mining2_norune, Obj.urns_mining2_rune, Obj.urns_mining2_started, Obj.urns_mining2_full, Seq.human_rune_in_urn_mining_02, Seq.human_urn_teleport_mining_02, Constants.MINING, 17, 1000.0, 20.0, 31.8, Obj.earthrune),
    PLAIN_MINING(Obj.urns_mining3_unfired, Obj.urns_mining3_norune, Obj.urns_mining3_rune, Obj.urns_mining3_started, Obj.urns_mining3_full, Seq.human_rune_in_urn_mining_03, Seq.human_urn_teleport_mining_03, Constants.MINING, 32, 1625.0, 28.0, 40.8, Obj.earthrune),
    STRONG_MINING(Obj.urns_mining4_unfired, Obj.urns_mining4_norune, Obj.urns_mining4_rune, Obj.urns_mining4_started, Obj.urns_mining4_full, Seq.human_rune_in_urn_mining_04, Seq.human_urn_teleport_mining_04, Constants.MINING, 48, 2000.0, 36.0, 49.2, Obj.earthrune),
    DECORATED_MINING(Obj.urns_mining5_unfired, Obj.urns_mining5_norune, Obj.urns_mining5_rune, Obj.urns_mining5_started, Obj.urns_mining5_full, Seq.human_rune_in_urn_mining_05, Seq.human_urn_teleport_mining_05, Constants.MINING, 78, 3125.0, 51.0, 57.0, Obj.earthrune),
    IMPIOUS(Obj.urns_prayer1_unfired, Obj.urns_prayer1_norune, Obj.urns_prayer1_rune, Obj.urns_prayer1_started, Obj.urns_prayer1_full, Seq.human_rune_in_urn_prayer_01, Seq.human_urn_teleport_prayer_01, Constants.PRAYER, 1, 100.0, 12.0, 18.0, Obj.airrune),
    ACCURSED(Obj.urns_prayer2_unfired, Obj.urns_prayer2_norune, Obj.urns_prayer2_rune, Obj.urns_prayer2_started, Obj.urns_prayer2_full, Seq.human_rune_in_urn_prayer_02, Seq.human_urn_teleport_prayer_02, Constants.PRAYER, 26, 312.5, 25.0, 37.5, Obj.airrune),
    INFERNAL(Obj.urns_prayer3_unfired, Obj.urns_prayer3_norune, Obj.urns_prayer3_rune, Obj.urns_prayer3_started, Obj.urns_prayer3_full, Seq.human_rune_in_urn_prayer_03, Seq.human_urn_teleport_prayer_03, Constants.PRAYER, 62, 1562.5, 40.0, 60.0, Obj.airrune),
    CRACKED_FARMING(Obj.urns_farming1_unfired, Obj.urns_farming1_norune, Obj.urns_farming1_rune, Obj.urns_farming1_started, Obj.urns_farming1_full, Seq.human_rune_in_urn_farming_01, Seq.human_urn_teleport_farming_01, Constants.FARMING, 5, 1100.0, 14.0, 18.0, Obj.naturerune),
    FRAGILE_FARMING(Obj.urns_farming2_unfired, Obj.urns_farming2_norune, Obj.urns_farming2_rune, Obj.urns_farming2_started, Obj.urns_farming2_full, Seq.human_rune_in_urn_farming_02, Seq.human_urn_teleport_farming_02, Constants.FARMING, 18, 2500.0, 21.0, 24.0, Obj.naturerune),
    PLAIN_FARMING(Obj.urns_farming3_unfired, Obj.urns_farming3_norune, Obj.urns_farming3_rune, Obj.urns_farming3_started, Obj.urns_farming3_full, Seq.human_rune_in_urn_farming_03, Seq.human_urn_teleport_farming_03, Constants.FARMING, 40, 4500.0, 32.0, 42.9, Obj.naturerune),
    STRONG_FARMING(Obj.urns_farming4_unfired, Obj.urns_farming4_norune, Obj.urns_farming4_rune, Obj.urns_farming4_started, Obj.urns_farming4_full, Seq.human_rune_in_urn_farming_04, Seq.human_urn_teleport_farming_04, Constants.FARMING, 58, 5500.0, 41.0, 52.5, Obj.naturerune),
    DECORATED_FARMING(Obj.urns_farming5_unfired, Obj.urns_farming5_norune, Obj.urns_farming5_rune, Obj.urns_farming5_started, Obj.urns_farming5_full, Seq.human_rune_in_urn_farming_05, Seq.human_urn_teleport_farming_05, Constants.FARMING, 84, 7000.0, 54.0, 78.0, Obj.naturerune),
    CRACKED_HUNTER(Obj.urns_hunter1_unfired, Obj.urns_hunter1_norune, Obj.urns_hunter1_rune, Obj.urns_hunter1_started, Obj.urns_hunter1_full, Seq.human_rune_in_urn_hunter_01, Seq.human_urn_teleport_hunter_01, Constants.HUNTER, 3, 1100.0, 13.0, 18.0, Obj.mudrune),
    FRAGILE_HUNTER(Obj.urns_hunter2_unfired, Obj.urns_hunter2_norune, Obj.urns_hunter2_rune, Obj.urns_hunter2_started, Obj.urns_hunter2_full, Seq.human_rune_in_urn_hunter_02, Seq.human_urn_teleport_hunter_02, Constants.HUNTER, 17, 2500.0, 20.0, 24.0, Obj.mudrune),
    PLAIN_HUNTER(Obj.urns_hunter3_unfired, Obj.urns_hunter3_norune, Obj.urns_hunter3_rune, Obj.urns_hunter3_started, Obj.urns_hunter3_full, Seq.human_rune_in_urn_hunter_03, Seq.human_urn_teleport_hunter_03, Constants.HUNTER, 34, 5000.0, 29.0, 42.9, Obj.mudrune),
    STRONG_HUNTER(Obj.urns_hunter4_unfired, Obj.urns_hunter4_norune, Obj.urns_hunter4_rune, Obj.urns_hunter4_started, Obj.urns_hunter4_full, Seq.human_rune_in_urn_hunter_04, Seq.human_urn_teleport_hunter_04, Constants.HUNTER, 55, 7000.0, 39.0, 52.5, Obj.mudrune),
    DECORATED_HUNTER(Obj.urns_hunter5_unfired, Obj.urns_hunter5_norune, Obj.urns_hunter5_rune, Obj.urns_hunter5_started, Obj.urns_hunter5_full, Seq.human_rune_in_urn_hunter_05, Seq.human_urn_teleport_hunter_05, Constants.HUNTER, 79, 8000.0, 51.0, 78.0, Obj.mudrune),
    CRACKED_RUNECRAFTING(Obj.urns_runecrafting1_unfired, Obj.urns_runecrafting1_norune, Obj.urns_runecrafting1_rune, Obj.urns_runecrafting1_started, Obj.urns_runecrafting1_full, Seq.human_rune_in_urn_runecrafting_01, Seq.human_urn_teleport_runecrafting_01, Constants.RUNECRAFTING, 4, 1050.0, 14.0, 18.0, Obj.blankrune_high),
    FRAGILE_RUNECRAFTING(Obj.urns_runecrafting2_unfired, Obj.urns_runecrafting2_norune, Obj.urns_runecrafting2_rune, Obj.urns_runecrafting2_started, Obj.urns_runecrafting2_full, Seq.human_rune_in_urn_runecrafting_02, Seq.human_urn_teleport_runecrafting_02, Constants.RUNECRAFTING, 18, 1750.0, 21.0, 24.0, Obj.blankrune_high),
    PLAIN_RUNECRAFTING(Obj.urns_runecrafting3_unfired, Obj.urns_runecrafting3_norune, Obj.urns_runecrafting3_rune, Obj.urns_runecrafting3_started, Obj.urns_runecrafting3_full, Seq.human_rune_in_urn_runecrafting_03, Seq.human_urn_teleport_runecrafting_03, Constants.RUNECRAFTING, 33, 3500.0, 28.0, 42.9, Obj.blankrune_high),
    STRONG_RUNECRAFTING(Obj.urns_runecrafting4_unfired, Obj.urns_runecrafting4_norune, Obj.urns_runecrafting4_rune, Obj.urns_runecrafting4_started, Obj.urns_runecrafting4_full, Seq.human_rune_in_urn_runecrafting_04, Seq.human_urn_teleport_runecrafting_04, Constants.RUNECRAFTING, 52, 5000.0, 38.0, 52.5, Obj.blankrune_high),
    DECORATED_RUNECRAFTING(Obj.urns_runecrafting5_unfired, Obj.urns_runecrafting5_norune, Obj.urns_runecrafting5_rune, Obj.urns_runecrafting5_started, Obj.urns_runecrafting5_full, Seq.human_rune_in_urn_runecrafting_05, Seq.human_urn_teleport_runecrafting_05, Constants.RUNECRAFTING, 77, 7000.0, 50.0, 78.0, Obj.blankrune_high);

    val runeIn = Animation(runeInSeq)
    val teleport = Animation(teleportSeq)

    val teleportXp get() = capacity * if (skill == Constants.PRAYER) 1.2 else 0.2

    companion object {
        @JvmField val byNoRune = entries.associateBy { it.noRune }
        @JvmField val byEmpty = entries.associateBy { it.empty }
        @JvmField val byStarted = entries.associateBy { it.started }
        @JvmField val byFull = entries.associateBy { it.full }

        fun forSkill(skill: Int) = entries.filter { it.skill == skill }
    }
}
