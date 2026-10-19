package com.rs.game.content.world.areas.dungeons

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapUndergroundPassDungeon() {
    onObjectClick(Loc.loc_3220, Loc.loc_3221) { (player) -> player.useStairs(Tile.of(2173, 4725, 1)) }
    onObjectClick(Loc.loc_34288, Loc.loc_34289) { (player) -> player.useStairs(Tile.of(2369, 9718, 0)) }
}
