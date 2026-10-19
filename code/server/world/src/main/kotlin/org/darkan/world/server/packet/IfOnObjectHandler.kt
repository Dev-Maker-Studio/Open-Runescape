package org.darkan.world.server.packet

import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.net.decoders.handlers.ObjectHandler
import com.rs.plugin.PluginManager
import com.rs.plugin.events.InterfaceOnObjectEvent
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.net.prot.packet.IfOnObject
import org.darkan.core.net.prot.handler.PacketHandler
import world.gregs.voidps.type.Tile

class IfOnObjectHandler : PacketHandler<Player, IfOnObject> {
    override suspend fun handle(player: Player, packet: IfOnObject) {
        if (!player.hasStarted() || !player.isRunning || player.isDead || player.isLocked)
            return
        if (player.emotesManager.isAnimating)
            return
        if (!player.interfaceManager.topOpen(packet.interfaceId))
            return
        val tile = Tile.of(packet.x, packet.y, player.plane)
        if (!player.mapChunkIds.contains(tile.getChunkId()))
            return
        val obj = World.getObjectWithId(tile, packet.objectId) ?: return

        player.stopAll(false)
        if (packet.forceRun)
            player.setRun(true)

        when (packet.interfaceId) {
            // Item on object. The item has to come out of the player's own container and be
            // checked against the id the client claims - trusting packet.itemId here both routed
            // every "use X on Y" to the wrong event and let a client name an item it does not hold.
            Interface.inventory -> {
                val item = player.inventory.getItem(packet.slotId) ?: return
                if (item.id != packet.itemId)
                    return
                ObjectHandler.handleItemOnObject(player, obj, packet.interfaceId, item, packet.slotId)
            }
            // Familiar special attack on an object.
            662, 747 -> {
                val familiar = player.familiar ?: return
                player.resetWalkSteps()
                if ((packet.interfaceId == 662 && packet.componentId == 74) ||
                    (packet.interfaceId == 747 && packet.componentId == 18)
                )
                    familiar.castSpecial(obj)
            }
            // Anything else (spells, scrolls, ...): once before walking, once on arrival.
            else -> {
                PluginManager.handle(
                    InterfaceOnObjectEvent(player, obj, packet.interfaceId, packet.componentId, packet.slotId, false)
                )
                player.setRouteEvent(RouteEvent(obj) {
                    player.faceObject(obj)
                    PluginManager.handle(
                        InterfaceOnObjectEvent(player, obj, packet.interfaceId, packet.componentId, packet.slotId, true)
                    )
                })
            }
        }
    }
}
