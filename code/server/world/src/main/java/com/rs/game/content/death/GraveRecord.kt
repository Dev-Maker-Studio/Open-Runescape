package com.rs.game.content.death

import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

class GraveRecord(
    val tile: Tile,
    val graveStone: Int,
    val ticks: Int,
    val blessed: Boolean,
    val items: List<Item>,
)
