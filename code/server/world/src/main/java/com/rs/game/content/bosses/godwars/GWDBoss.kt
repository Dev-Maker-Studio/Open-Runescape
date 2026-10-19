package com.rs.game.content.bosses.godwars

import com.rs.game.content.bosses.MinionHost
import com.rs.game.content.bosses.MinionSpawn
import world.gregs.voidps.type.Tile

open class GWDBoss(id: Int, tile: Tile, spawned: Boolean, minions: List<MinionSpawn>) : MinionHost(id, tile, spawned, minions) {
    init {
        isIntelligentRouteFinder = true
        isIgnoreDocile = true
        setForceAggroDistance(64)
    }
}
