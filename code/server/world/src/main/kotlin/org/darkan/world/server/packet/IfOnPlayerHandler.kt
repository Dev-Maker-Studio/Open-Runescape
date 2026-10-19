package org.darkan.world.server.packet

import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.IFOnPlayerEvent
import org.darkan.core.net.prot.packet.IfOnPlayer
import org.darkan.core.net.prot.handler.PacketHandler

class IfOnPlayerHandler : PacketHandler<Player, IfOnPlayer> {
    override suspend fun handle(player: Player, packet: IfOnPlayer) {
        if (!player.hasStarted() || !player.isRunning || player.isDead || player.isLocked)
            return
        if (!player.interfaceManager.topOpen(packet.interfaceId))
            return
        val target = World.players[packet.playerIndex] ?: return
        if (target.isDead || target.hasFinished() || !player.mapChunkIds.contains(target.chunkId))
            return
        if (packet.forceRun)
            player.setRun(true)
        player.stopAll(false)
        if (PluginManager.handle(IFOnPlayerEvent(player, target, packet.interfaceId, packet.componentId, packet.slotId, packet.itemId, false)))
            return
        player.setRouteEvent(RouteEvent(target) {
            PluginManager.handle(IFOnPlayerEvent(player, target, packet.interfaceId, packet.componentId, packet.slotId, packet.itemId, true))
        })
    }
}
