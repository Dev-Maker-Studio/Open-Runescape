package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.net.prot.packet.RegionLoadedConfirm
import org.darkan.core.net.prot.handler.PacketHandler

class RegionLoadedHandler : PacketHandler<Player, RegionLoadedConfirm> {
    override suspend fun handle(player: Player, packet: RegionLoadedConfirm) {
        if (!player.clientHasLoadedMapRegionFinished()) {
            player.setClientHasLoadedMapRegion()
            player.initNewChunks()
        }
    }
}
