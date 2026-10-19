package com.rs.game.content.minigames.treasuretrails

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onPickupItem

@ServerStartupEvent
fun mapClueScrollPickup() {
    onPickupItem(*TreasureTrailsManager.CLUE_SCROLLS.map { it as Any }.toTypedArray()) { e ->
        if (e.isTelegrabbed || !e.player.treasureTrailsManager.hasClueScrollItem()) return@onPickupItem
        e.player.sendMessage("You should finish the clue you are currently doing first.")
        e.cancelPickup()
    }
}
