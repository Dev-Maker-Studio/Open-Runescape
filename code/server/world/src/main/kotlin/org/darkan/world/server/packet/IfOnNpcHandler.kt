package org.darkan.world.server.packet

import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.IFOnNPCEvent
import org.darkan.core.net.prot.packet.IfOnNpc
import org.darkan.core.net.prot.handler.PacketHandler

class IfOnNpcHandler : PacketHandler<Player, IfOnNpc> {
    override suspend fun handle(player: Player, packet: IfOnNpc) {
        if (!player.hasStarted() || !player.isRunning || player.isDead || player.isLocked)
            return
        if (!player.interfaceManager.topOpen(packet.interfaceId))
            return
        val npc = World.NPCs[packet.npcIndex] ?: return
        if (npc.isDead || npc.hasFinished() || !player.mapChunkIds.contains(npc.chunkId))
            return
        if (packet.forceRun)
            player.setRun(true)
        player.stopAll(false)
        if (PluginManager.handle(IFOnNPCEvent(player, npc, packet.interfaceId, packet.componentId, packet.slotId, packet.itemId, false)))
            return
        player.setRouteEvent(RouteEvent(npc) {
            PluginManager.handle(IFOnNPCEvent(player, npc, packet.interfaceId, packet.componentId, packet.slotId, packet.itemId, true))
        })
    }
}
