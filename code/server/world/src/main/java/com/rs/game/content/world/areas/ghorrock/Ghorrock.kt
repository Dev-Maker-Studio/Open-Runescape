package com.rs.game.content.world.areas.ghorrock

import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

private val DUNGEON_EXITS = listOf(
    Triple(Loc.mah4_frozen_fort_cellar_trapdoor_large, Tile.of(2914, 3942, 0), Tile.of(2909, 10417, 1)),
    Triple(Loc.mah4_frozen_fort_cellar_stairs_up_thawed, Tile.of(2908, 10418, 1), Tile.of(2914, 3941, 0)),
    Triple(Loc.mah4_frozen_fort_cellar_trapdoor_thawed, Tile.of(2893, 10417, 1), Tile.of(2893, 10354, 0)),
    Triple(Loc.mah4_frozen_fort_cellar_trapdoor_ladder, Tile.of(2893, 10353, 0), Tile.of(2894, 10419, 1)),
    Triple(Loc.mah4_frozen_fort_cellar_trapdoor_thawed, Tile.of(2929, 10381, 1), Tile.of(2929, 10318, 0)),
    Triple(Loc.mah4_frozen_fort_cellar_trapdoor_ladder, Tile.of(2929, 10317, 0), Tile.of(2929, 10380, 1))
)

@ServerStartupEvent
fun mapGhorrock() {
    onObjectClick(Loc.loc_47130) { (player, obj) ->
        if (!player.isQuestComplete(Quest.RITUAL_OF_MAHJARRAT)) {
            player.sendMessage("You do not meet the requirements for Ritual of Mahjarrat.")
            return@onObjectClick
        }
        val distance = if (obj.rotation == 1 || obj.rotation == 3) 4 else 0
        if (player.x < obj.x)
            player.tele(player.transform(distance, 0, 0))
        if (player.x > obj.x)
            player.tele(player.transform(-distance, 0, 0))
    }

    onObjectClick(Loc.mah4_frozen_lower_stairs, Loc.mah4_frozen_upper_stairs) { e ->
        val (player, obj) = e
        if (obj.id == Loc.mah4_frozen_lower_stairs) {
            player.tele(if (obj.rotation == 0) player.transform(-1, -4, 1) else player.transform(0, 0, 1))
            return@onObjectClick
        }
        if (e.objectAt(2920, 3932, 1)) {
            player.tele(Tile.of(2920, 3935, 0))
            return@onObjectClick
        }
        player.tele(if (obj.rotation == 0) player.transform(1, 4, -1) else player.transform(0, 0, -1))
    }

    onObjectClick(Loc.mah4_frozen_lower_stairs_mirror) { it.player.tele(Tile.of(2921, 3931, 1)) }

    onLogin { it.player.vars.setVarBit(Varbit.mah1_canoe_state, 1) }

    onObjectClick(Loc.loc_42886) { (player) ->
        teleIfQuestComplete(player, Quest.TALE_OF_MUSPAH, Tile.of(2856, 3933, 0), "Ritual of Mahjarrat")
    }

    onObjectClick(Loc.mah1_return_canoe) { (player) ->
        teleIfQuestComplete(player, Quest.TALE_OF_MUSPAH, Tile.of(2692, 3793, 0), "Ritual of Mahjarrat")
    }

    onObjectClick(Loc.mah1_burial_cave_entrance) { (player) ->
        teleIfQuestComplete(player, Quest.TALE_OF_MUSPAH, Tile.of(2843, 10364, 0), "The Tale of the Muspah")
    }

    onObjectClick(Loc.mah1_burial_cave_exit) { (player) ->
        teleIfQuestComplete(player, Quest.TALE_OF_MUSPAH, Tile.of(2866, 3927, 0), "The Tale of the Muspah")
    }

    onObjectClick(Loc.mah3_fort_stairs_up, Loc.mah3_fort_stairs_down) { (player, obj) ->
        if (obj.id == Loc.mah3_fort_stairs_up)
            player.tele(player.transform(if (obj.rotation == 3) -3 else 0, if (obj.rotation == 0) 3 else 0, 1))
        else
            player.tele(player.transform(if (obj.rotation == 3) 3 else 0, if (obj.rotation == 0) -3 else 0, -1))
    }

    onObjectClick(Loc.mah3_fort_stairs_up_mirror) { it.player.tele(Tile.of(2836, 3868, 1)) }

    onObjectClick(*DUNGEON_EXITS.map { it.first }.distinct().toTypedArray<Any>()) { (player, obj) ->
        DUNGEON_EXITS.firstOrNull { (loc, at) -> obj.id == loc && obj.tile.matches(at) }
            ?.let { (_, _, destination) -> player.tele(destination) }
    }

    onObjectClick(Loc.mah4_frozen_trapdoor) {
        it.player.sendMessage("Many centuries of rust have fused the hinges in place. There's no hope of opening it.")
    }

    onObjectClick(Loc.mah5_fortress_pillar_jump_over) { (player, obj) ->
        val destination = if (player.y < obj.y) Tile.of(2913, 3918, 0) else Tile.of(2913, 3916, 0)
        player.forceMove(destination, Seq.human_crossing_stile, 5, 60)
    }

    onObjectClick(Loc.mah4_frozen_inner_wall_lower_climb, Loc.mah4_frozen_inner_wall_rampart_smashed) { e ->
        when {
            e.objectAt(2921, 3937, 0) -> e.player.forceMove(Tile.of(2920, 3937, 0), Seq.tog_climb_slope_up, 25, 60)
            e.objectAt(2909, 3942, 1) -> e.player.forceMove(Tile.of(2908, 3942, 1), Seq.tog_climb_slope_up, 25, 60)
            e.objectAt(2909, 3942, 0) -> e.player.forceMove(Tile.of(2910, 3942, 0), Seq.tog_climb_slope_down, 25, 60)
        }
    }
}

private fun teleIfQuestComplete(player: Player, quest: Quest, destination: Tile, requirement: String) {
    if (player.isQuestComplete(quest))
        player.tele(destination)
    else
        player.sendMessage("You do not meet the requirements for $requirement.")
}
