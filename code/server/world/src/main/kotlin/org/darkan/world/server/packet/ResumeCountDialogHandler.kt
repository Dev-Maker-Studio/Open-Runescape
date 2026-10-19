package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import com.rs.plugin.events.InputIntegerEvent
import org.darkan.core.net.prot.packet.ResumeCountDialog
import org.darkan.core.net.prot.handler.PacketHandler

class ResumeCountDialogHandler : PacketHandler<Player, ResumeCountDialog> {
    override suspend fun handle(player: Player, packet: ResumeCountDialog) {
        if (!player.isRunning || !player.hasStarted())
            return
        val e = player.tempAttribs.removeO<InputIntegerEvent>("pluginInteger")
        e?.run(packet.count)
    }
}
