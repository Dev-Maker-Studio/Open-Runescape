package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapHazeelSecretStairs() {
    onObjectClick(Loc.hazeelcultstairs) { (player, obj) -> player.useStairs(obj.tile.transform(0, 0, 1)) }
}
