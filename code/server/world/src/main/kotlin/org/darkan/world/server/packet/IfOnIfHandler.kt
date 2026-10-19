package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.IFOnIFEvent
import org.darkan.core.Logger.logTrace
import org.darkan.core.net.prot.packet.IfOnIf
import org.darkan.core.net.prot.handler.PacketHandler

class IfOnIfHandler : PacketHandler<Player, IfOnIf> {
    override suspend fun handle(player: Player, packet: IfOnIf) {
        if (!player.hasStarted() || !player.isRunning || player.isDead || player.isLocked)
            return
        if (!player.interfaceManager.topOpen(packet.fromInter) || !player.interfaceManager.topOpen(packet.toInter))
            return
        logTrace("IfOnIf: from=${packet.fromInter}:${packet.fromComp}[${packet.fromSlot}] to=${packet.toInter}:${packet.toComp}[${packet.toSlot}]")
        val event = IFOnIFEvent(player, packet.fromInter, packet.fromComp, packet.fromSlot, packet.fromItemId, packet.toInter, packet.toComp, packet.toSlot, packet.toItemId)
        if (!event.intercepted()) PluginManager.handle(event)
    }
}
