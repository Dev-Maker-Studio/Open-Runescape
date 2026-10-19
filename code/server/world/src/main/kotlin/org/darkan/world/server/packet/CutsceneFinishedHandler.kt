package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import org.darkan.core.net.prot.packet.CutsceneFinished
import org.darkan.core.net.prot.handler.PacketHandler

class CutsceneFinishedHandler : PacketHandler<Player, CutsceneFinished> {
    override suspend fun handle(player: Player, packet: CutsceneFinished) {
        player.loadMapRegions()
        player.onPacketCutsceneFinish?.run()
        player.onPacketCutsceneFinish = null
    }
}
