package com.rs.game.content.minigames.pest

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Varbit

const val COMMENDATION_CAP = 500

var Player.commendations: Int
    get() = pestPoints
    set(value) {
        pestPoints = value.coerceIn(0, COMMENDATION_CAP)
        refreshCommendations()
    }

fun Player.refreshCommendations() = vars.setVarBit(Varbit.pest_points_old2, pestPoints)

fun Player.awardCommendations(amount: Int): Int {
    val credited = amount.coerceAtMost(COMMENDATION_CAP - commendations).coerceAtLeast(0)
    commendations += credited
    return credited
}
