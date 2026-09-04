package com.darkan.bot.scripts

import com.darkan.bot.Bot

suspend fun Script.findAndPickupItems(vararg items: String): Boolean {
    for (i in items.indices) {
        while (Bot.findGroundItem(items[i]) != null) {
            if (Bot.inventory.freeSlots() <= 0) break
            val drop = Bot.findGroundItem(items[i])
            if (drop == null) continue
            Bot.takeGroundItem(drop.z, drop)
            waitThenDelayUntil(1200, 5000) { !Bot.isWalking() }
            return true
        }
    }
    return false
}

suspend fun Script.findAndPickupItems(distance: Int, vararg items: String): Boolean {
    for (i in items.indices) {
        while (Bot.findGroundItem(items[i], distance) != null) {
            if (Bot.inventory.freeSlots() <= 0) break
            val drop = Bot.findGroundItem(items[i], distance) ?: continue
            Bot.takeGroundItem(drop.z, drop)
            waitThenDelayUntil(1200, 5000) { !Bot.isWalking() }
            return true
        }
    }
    return false
}

suspend fun Script.findAndPickupItems(vararg items: Int) {
    for (i in items.indices) {
        while (Bot.findGroundItem(items[i]) != null) {
            if (Bot.inventory.freeSlots() <= 0) break
            val drop = Bot.findGroundItem(items[i]) ?: continue
            Bot.takeGroundItem(items[i], drop)
            waitThenDelayUntil(1200, 5000) { !Bot.isWalking() }
        }
    }
}