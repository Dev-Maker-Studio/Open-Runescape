package com.rs.game.content.skills.firemaking

import com.rs.engine.pathfinder.RouteEvent
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onGroundItemClick
import com.rs.plugin.kts.onInterfaceOnGroundItem
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapGroundLogs() {
    val logs = Firemaking.Fire.entries.map { it.logId }.toIntArray()
    onGroundItemClick(*logs, options = arrayOf("Light")) { (player, item) ->
        val fire = Firemaking.Fire.forId(item.id) ?: return@onGroundItemClick
        player.actionManager.setAction(Firemaking(fire, item))
    }
    onInterfaceOnGroundItem(fromInterfaceIds = intArrayOf(Interface.inventory), itemIds = logs) { e ->
        if (e.player.inventory.getItem(e.slotId)?.id != Obj.tinderbox) return@onInterfaceOnGroundItem
        val fire = Firemaking.Fire.forId(e.item.id) ?: return@onInterfaceOnGroundItem
        e.player.setRouteEvent(RouteEvent(e.item) { e.player.actionManager.setAction(Firemaking(fire, e.item)) })
    }
    onItemOnItem(logs, Firemaking.bowIds) { event ->
        val item1 = event.item1.id
        val item2 = event.item2.id
        val bowId = if (Firemaking.bowLightAnimation(item1) != -1) item1 else item2
        val logId = if (bowId == item1) item2 else item1
        Firemaking.bowLight(event.player, Firemaking.Fire.forId(logId) ?: return@onItemOnItem, bowId)
    }
}
