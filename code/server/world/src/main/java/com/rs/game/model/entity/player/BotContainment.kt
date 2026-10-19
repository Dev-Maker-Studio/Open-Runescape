package com.rs.game.model.entity.player

import org.darkan.core.model.Item

object BotContainment {
    @JvmStatic
    fun inTransferZone(player: Player) = player.controllerManager.controller?.allowsBotItemTransfer() == true

    @JvmStatic
    fun holdsItems(player: Player) = player.isBot && !inTransferZone(player)

    @JvmStatic
    fun destroyDropped(player: Player, slot: Int, item: Item): Boolean {
        if (!holdsItems(player)) return false
        player.inventory.deleteItem(slot, item)
        return true
    }

    @JvmStatic
    fun refuseExchange(player: Player, other: Player): Boolean {
        if (!player.isBot && !other.isBot) return false
        if (inTransferZone(player) && inTransferZone(other)) return false
        player.sendMessage("You can't exchange items with a bot.")
        return true
    }

    @JvmStatic
    fun refuseEconomy(player: Player): Boolean {
        if (!player.isBot) return false
        player.sendMessage("Bots can't take part in the economy.")
        return true
    }
}
