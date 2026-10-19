package org.darkan.world.server.packet

import com.rs.engine.Shop
import com.rs.game.model.entity.player.Inventory
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.net.prot.packet.IfDragOntoIf
import org.darkan.core.net.prot.handler.PacketHandler

/**
 * Dragging one interface component onto another: rearranging the inventory, the bank, the notes
 * tab, and drag-to-buy in shops.
 */
class IfDragOntoIfHandler : PacketHandler<Player, IfDragOntoIf> {
    override suspend fun handle(player: Player, packet: IfDragOntoIf) {
        if (!player.hasStarted() || !player.isRunning || player.isDead || player.isLocked)
            return
        if (!player.interfaceManager.topOpen(packet.fromInter) || !player.interfaceManager.topOpen(packet.toInter))
            return

        val invSize = player.inventory.itemsContainerSize
        when {
            // Inventory tab. The client numbers this interface's slots from 28.
            packet.fromInter == Interface.inventory && packet.fromComp == 0 &&
                packet.toInter == Interface.inventory && packet.toComp == 0 -> {
                player.closeInterfaces()
                val to = packet.toSlot - 28
                if (to < 0 || to >= invSize || packet.fromSlot < 0 || packet.fromSlot >= invSize)
                    return
                player.inventory.switchItem(packet.fromSlot, to)
            }
            // The inventory as drawn inside the bank screen.
            packet.fromInter == 763 && packet.fromComp == 0 && packet.toInter == 763 && packet.toComp == 0 -> {
                if (packet.toSlot < 0 || packet.toSlot >= invSize || packet.fromSlot < 0 || packet.fromSlot >= invSize)
                    return
                player.inventory.switchItem(packet.fromSlot, packet.toSlot)
            }
            packet.fromInter == Interface.interface_762 && packet.toInter == Interface.interface_762 -> {
                val bankSize = player.bank.bankSize
                if (packet.fromSlot !in 0 until bankSize || (packet.toSlot != -1 && packet.toSlot !in 0 until bankSize))
                    return
                player.bank.switchItem(packet.fromSlot, packet.toSlot, packet.toInter shl 16 or packet.toComp)
            }
            // Drag a shop stock slot onto the inventory to buy one.
            packet.fromInter == 1265 && packet.toInter == 1266 && player.tempAttribs.getB("shop_buying") -> {
                val shop = player.tempAttribs.getO<Shop>("Shop") ?: return
                shop.buy(player, packet.fromSlot, 1)
            }
            packet.fromInter == 34 && packet.toInter == 34 ->
                player.notes.switchNotes(packet.fromSlot, packet.toSlot)
        }
    }
}
