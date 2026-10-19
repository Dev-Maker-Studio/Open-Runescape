package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.game.content.quests.the_grand_tree.*
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val GRAND_TREE_SURFACE = Tile.of(2465, 3497, 0)

@ServerStartupEvent
fun mapGrandTreeUndergroundLadders() {
    onObjectClick(Loc.di2_ladder_base, tiles = arrayOf(Tile.of(2463, 9897, 0))) { (player) -> player.useStairs(GRAND_TREE_SURFACE) }
    onObjectClick(Loc.di2_ladder_base_broken, tiles = arrayOf(Tile.of(2492, 9864, 0))) { (player) -> player.sendMessage("The ladder is broken. You can't climb it.") }
}
