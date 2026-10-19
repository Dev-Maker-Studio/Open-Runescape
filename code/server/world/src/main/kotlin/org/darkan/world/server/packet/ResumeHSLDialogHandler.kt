package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import com.rs.plugin.events.InputHSLEvent
import org.darkan.core.net.prot.packet.ResumeHSLDialog
import org.darkan.core.net.prot.handler.PacketHandler

class ResumeHSLDialogHandler : PacketHandler<Player, ResumeHSLDialog> {
    override suspend fun handle(player: Player, packet: ResumeHSLDialog) {
        if (!player.isRunning || !player.hasStarted()) return
        val e = player.tempAttribs.removeO<InputHSLEvent>("pluginHSL")
        e?.run(packet.data)
    }
}
