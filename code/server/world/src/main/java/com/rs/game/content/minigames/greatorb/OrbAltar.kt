package com.rs.game.content.minigames.greatorb

import com.rs.game.content.skills.runecrafting.Runecrafting.RCRune
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Model
import world.gregs.voidps.type.Tile

enum class OrbAltar(val rune: RCRune, val altar: Int, val anchor: Tile, val arrival: Tile, val exits: IntArray, val icon: Int, val spawnRadius: Int) {
    AIR(RCRune.AIR, Loc.loc_2478, Tile.of(2843, 4833, 0), Tile.of(2844, 4839, 0), intArrayOf(Loc.airtemple_exit_portal), Model.model_41489, 9),
    MIND(RCRune.MIND, Loc.loc_2479, Tile.of(2785, 4840, 0), Tile.of(2786, 4846, 0), intArrayOf(Loc.mindtemple_exit_portal), Model.model_41493, 14),
    WATER(RCRune.WATER, Loc.loc_2480, Tile.of(3483, 4835, 0), Tile.of(3484, 4842, 0), intArrayOf(Loc.watertemple_exit_portal), Model.model_41495, 11),
    EARTH(RCRune.EARTH, Loc.loc_2481, Tile.of(2657, 4840, 0), Tile.of(2658, 4846, 0), intArrayOf(Loc.earthtemple_exit_portal), Model.model_41485, 13),
    FIRE(RCRune.FIRE, Loc.loc_2482, Tile.of(2584, 4837, 0), Tile.of(2585, 4844, 0), intArrayOf(Loc.firetemple_exit_portal), Model.model_41492, 11),
    BODY(RCRune.BODY, Loc.loc_2483, Tile.of(2522, 4839, 0), Tile.of(2523, 4845, 0), intArrayOf(Loc.bodytemple_exit_portal), Model.model_41490, 11),
    CHAOS(RCRune.CHAOS, Loc.loc_2487, Tile.of(2270, 4841, 0), Tile.of(2271, 4848, 0), intArrayOf(Loc.chaostemple_exit_portal, Loc.rcguild_chaos_ladder), Model.model_41487, 9),
    NATURE(RCRune.NATURE, Loc.loc_2486, Tile.of(2399, 4840, 0), Tile.of(2400, 4846, 0), intArrayOf(Loc.naturetemple_exit_portal), Model.model_41484, 11);
}
