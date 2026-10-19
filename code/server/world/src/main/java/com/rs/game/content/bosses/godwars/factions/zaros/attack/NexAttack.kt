package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.Entity

fun interface NexAttack {
    fun attack(nex: Nex, target: Entity): Int
}
