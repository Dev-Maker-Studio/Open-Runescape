package com.rs.game.content.world.areas.minigames

import com.rs.game.content.minigames.creations.StealingCreation
import com.rs.game.content.minigames.creations.StealingCreationLobbyController
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile

private val LOBBY_PEN_MIN = Tile.of(2959, 9690, 0)
private val LOBBY_PEN_MAX = Tile.of(2965, 9706, 0)

@ServerStartupEvent
fun mapGamersGrotto() {
    onObjectClick(Loc.activity_cave_entrance) { (player) -> player.useStairs(-1, Tile.of(2969, 9672, 0), 1, 1) }
    onObjectClick(Loc.activity_cave_exit) { (player) -> player.useStairs(-1, Tile.of(3018, 3404, 0), 0, 1) }
    onObjectClick(Loc.sc_team_a_stile, Loc.sc_team_b_stile) { (player, obj) -> StealingCreationLobbyController.climbOverStile(player, obj, true) }
    onLogin { (player) ->
        if (player.tile.withinArea(LOBBY_PEN_MIN.x, LOBBY_PEN_MIN.y, LOBBY_PEN_MAX.x, LOBBY_PEN_MAX.y) && player.controllerManager.controller !is StealingCreationLobbyController)
            player.tele(StealingCreation.EXIT)
    }
}
