package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import com.rs.utils.ReportsManager
import org.darkan.core.net.prot.packet.ReportAbuse
import org.darkan.core.net.prot.handler.PacketHandler

class ReportAbuseHandler : PacketHandler<Player, ReportAbuse> {
    override suspend fun handle(player: Player, packet: ReportAbuse) {
        if (!player.hasStarted()) return
        ReportsManager.report(player, packet.username, packet.type, packet.mute)
    }
}
