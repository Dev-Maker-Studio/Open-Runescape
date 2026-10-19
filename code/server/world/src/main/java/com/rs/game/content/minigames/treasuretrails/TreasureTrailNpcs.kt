package com.rs.game.content.minigames.treasuretrails

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.interceptNpcClick

@ServerStartupEvent
fun mapTreasureTrailNpcs() {
    interceptNpcClick { player, npc -> player.treasureTrailsManager.useNPC(npc) }
}
