package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.net.prot.packet.WorldMapClick
import org.darkan.core.net.prot.handler.PacketHandler

class WorldMapClickHandler : PacketHandler<Player, WorldMapClick> {
    override suspend fun handle(player: Player, packet: WorldMapClick) {
        val tile = packet.tile
        val lastHash = player.tempAttribs.getI("worldMapTileHash", -1)
        if (lastHash == tile.id) {
            player.hintIconsManager.addHintIcon(tile.x, tile.y, tile.plane, -1, 2, 0, -1, false)
            player.vars.setVar(Varp.varp_1159, tile.id)
        }
        player.tempAttribs.setI("worldMapTileHash", tile.id)
    }
}
