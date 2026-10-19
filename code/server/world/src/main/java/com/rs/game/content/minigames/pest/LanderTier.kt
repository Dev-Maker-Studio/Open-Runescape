package com.rs.game.content.minigames.pest

import com.rs.game.content.minigames.pest.npcs.PestFamily
import com.rs.game.content.minigames.pest.npcs.PestFamily.BRAWLER
import com.rs.game.content.minigames.pest.npcs.PestFamily.DEFILER
import com.rs.game.content.minigames.pest.npcs.PestFamily.RAVAGER
import com.rs.game.content.minigames.pest.npcs.PestFamily.SHIFTER
import com.rs.game.content.minigames.pest.npcs.PestFamily.SPINNER
import com.rs.game.content.minigames.pest.npcs.PestFamily.SPLATTER
import com.rs.game.content.minigames.pest.npcs.PestFamily.TORCHER
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

enum class LanderTier(
    val displayName: String,
    val combatLevel: Int,
    val commendations: Int,
    val portalHitpoints: Int,
    val activePortals: IntArray,
    val shieldedPortals: IntArray,
    val gangplank: Int,
    val ladder: Int,
    val squire: Int,
    val boardTile: Tile,
    val exitTile: Tile,
    val pests: Map<PestFamily, IntArray>,
) {
    NOVICE(
        "Novice", 40, 2, 2000,
        intArrayOf(Npc.pest_portal_1_active_bt1, Npc.pest_portal_2_active_bt1, Npc.pest_portal_3_active_bt1, Npc.pest_portal_4_active_bt1),
        intArrayOf(Npc.pest_portal_shield_1_active_bt1, Npc.pest_portal_shield_2_active_bt1, Npc.pest_portal_shield_3_active_bt1, Npc.pest_portal_shield_4_active_bt1),
        Loc.pest_lander_gangplank, Loc.pest_lander_ladder, Npc.pest_squire_lander,
        Tile.of(2661, 2639, 0), Tile.of(2657, 2639, 0),
        mapOf(
            SPLATTER to intArrayOf(Npc.pest_splatter_1, Npc.pest_splatter_2, Npc.pest_splatter_3),
            SHIFTER to intArrayOf(Npc.pest_shifter_1a, Npc.pest_shifter_1b, Npc.pest_shifter_2a, Npc.pest_shifter_2b),
            RAVAGER to intArrayOf(Npc.pest_ravager_1, Npc.pest_ravager_2, Npc.pest_ravager_3),
            SPINNER to intArrayOf(Npc.pest_spinner_1, Npc.pest_spinner_2, Npc.pest_spinner_3),
            TORCHER to intArrayOf(Npc.pest_torcher_1a, Npc.pest_torcher_1b, Npc.pest_torcher_2a, Npc.pest_torcher_2b),
            DEFILER to intArrayOf(Npc.pest_defiler_1a, Npc.pest_defiler_1b, Npc.pest_defiler_2a, Npc.pest_defiler_2b),
            BRAWLER to intArrayOf(Npc.pest_brawler_1, Npc.pest_brawler_2, Npc.pest_brawler_3),
        ),
    ),
    INTERMEDIATE(
        "Intermediate", 70, 3, 2500,
        intArrayOf(Npc.pest_portal_1_active, Npc.pest_portal_2_active, Npc.pest_portal_3_active, Npc.pest_portal_4_active),
        intArrayOf(Npc.pest_portal_shield_1_active, Npc.pest_portal_shield_2_active, Npc.pest_portal_shield_3_active, Npc.pest_portal_shield_4_active),
        Loc.pest_lander_gangplank_2, Loc.pest_lander_ladder_2, Npc.pest_squire_lander_2,
        Tile.of(2641, 2644, 0), Tile.of(2644, 2644, 0),
        mapOf(
            SPLATTER to intArrayOf(Npc.pest_splatter_2, Npc.pest_splatter_3, Npc.pest_splatter_4),
            SHIFTER to intArrayOf(Npc.pest_shifter_2a, Npc.pest_shifter_2b, Npc.pest_shifter_3a, Npc.pest_shifter_3b, Npc.pest_shifter_4a, Npc.pest_shifter_4b),
            RAVAGER to intArrayOf(Npc.pest_ravager_2, Npc.pest_ravager_3, Npc.pest_ravager_4),
            SPINNER to intArrayOf(Npc.pest_spinner_2, Npc.pest_spinner_3, Npc.pest_spinner_4, Npc.pest_spinner_5),
            TORCHER to intArrayOf(Npc.pest_torcher_2a, Npc.pest_torcher_2b, Npc.pest_torcher_3a, Npc.pest_torcher_3b, Npc.pest_torcher_4a, Npc.pest_torcher_4b),
            DEFILER to intArrayOf(Npc.pest_defiler_2a, Npc.pest_defiler_2b, Npc.pest_defiler_3a, Npc.pest_defiler_3b, Npc.pest_defiler_4a, Npc.pest_defiler_4b),
            BRAWLER to intArrayOf(Npc.pest_brawler_2, Npc.pest_brawler_3, Npc.pest_brawler_4),
        ),
    ),
    VETERAN(
        "Veteran", 100, 4, 2500,
        INTERMEDIATE.activePortals, INTERMEDIATE.shieldedPortals,
        Loc.pest_lander_gangplank_3, Loc.pest_lander_ladder_3, Npc.pest_squire_lander_3,
        Tile.of(2635, 2653, 0), Tile.of(2638, 2653, 0),
        mapOf(
            SPLATTER to intArrayOf(Npc.pest_splatter_3, Npc.pest_splatter_4, Npc.pest_splatter_5),
            SHIFTER to intArrayOf(Npc.pest_shifter_4a, Npc.pest_shifter_4b, Npc.pest_shifter_5a, Npc.pest_shifter_5b),
            RAVAGER to intArrayOf(Npc.pest_ravager_3, Npc.pest_ravager_4, Npc.pest_ravager_5),
            SPINNER to intArrayOf(Npc.pest_spinner_3, Npc.pest_spinner_4, Npc.pest_spinner_5),
            TORCHER to intArrayOf(Npc.pest_torcher_4a, Npc.pest_torcher_4b, Npc.pest_torcher_5a, Npc.pest_torcher_5b),
            DEFILER to intArrayOf(Npc.pest_defiler_4a, Npc.pest_defiler_4b, Npc.pest_defiler_5a, Npc.pest_defiler_5b),
            BRAWLER to intArrayOf(Npc.pest_brawler_3, Npc.pest_brawler_4),
        ),
    );

    companion object {
        private val byGangplank = entries.associateBy { it.gangplank }
        private val byLadder = entries.associateBy { it.ladder }
        private val bySquire = entries.associateBy { it.squire }

        fun forGangplank(loc: Int) = byGangplank[loc]
        fun forLadder(loc: Int) = byLadder[loc]
        fun forSquire(npc: Int) = bySquire[npc]
    }
}
