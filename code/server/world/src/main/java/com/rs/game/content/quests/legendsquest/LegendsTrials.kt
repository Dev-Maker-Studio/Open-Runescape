package com.rs.game.content.quests.legendsquest

import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils

// Source guides describe failures but omit exact rolls. These rates are server tuning.
internal fun Player.legendsTrial(skill: Int, minimum: Int, baseChance: Int = 50): Boolean =
    Utils.random(100) < (baseChance + (skills.getLevel(skill) - minimum) * 2).coerceIn(0, 95)
