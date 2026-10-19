package org.darkan.world.server.packet

import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.IFOnGroundItemEvent
import org.darkan.core.Utils
import org.darkan.core.net.prot.handler.PacketHandler
import org.darkan.core.net.prot.packet.IfOnGroundItem
import world.gregs.voidps.type.Tile

class IfOnGroundItemHandler : PacketHandler<Player, IfOnGroundItem> {
    override suspend fun handle(player: Player, packet: IfOnGroundItem) {
        val known = Utils.getInterfaceDefinitionsSize()
        if ((known > 0 && known <= packet.interfaceId) || player.isDead || player.isLocked || !player.interfaceManager.topOpen(packet.interfaceId))
            return
        val components = Utils.getInterfaceDefinitionsComponentsSize(packet.interfaceId)
        if (packet.componentId != -1 && components > 0 && components <= packet.componentId)
            return
        val tile = Tile.of(packet.x, packet.y, player.plane)
        if (!player.mapChunkIds.contains(tile.getChunkId()))
            return
        val groundItem = ChunkManager.getChunk(tile.getChunkId()).getGroundItem(packet.itemId, tile, player) ?: return

        player.stopAll()
        if (packet.forceRun)
            player.setRun(true)
        PluginManager.handle(IFOnGroundItemEvent(player, packet.interfaceId, packet.componentId, packet.slotId, groundItem))
    }
}
