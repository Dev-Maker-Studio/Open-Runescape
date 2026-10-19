package com.rs.game.content.quests.observatoryquest.objects

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapObservatoryOrrery() {
    onObjectClick(Loc.qip_obs_planets, tiles = arrayOf(Tile.of(2442, 3185, 0))) { (player) ->
        player.sendMessage("You look at the brass clockwork planetary model. It tracks the strange orbits of the celestial bodies.")
    }
}
