package org.darkan.world.server.packet

import com.rs.game.model.entity.player.Player
import com.rs.plugin.events.InputStringEvent
import org.darkan.core.net.prot.packet.ResumeTextDialog
import org.darkan.core.net.prot.packet.ResumeNameDialog
import org.darkan.core.net.prot.packet.ResumeClanForumQFCDialog
import org.darkan.core.net.prot.handler.PacketHandler

class ResumeTextDialogHandler : PacketHandler<Player, ResumeTextDialog> {
    override suspend fun handle(player: Player, packet: ResumeTextDialog) {
        if (!player.isRunning || !player.hasStarted()) return
        val e = player.tempAttribs.removeO<InputStringEvent>("pluginEnterLongText")
        e?.run(packet.text)
    }
}

class ResumeNameDialogHandler : PacketHandler<Player, ResumeNameDialog> {
    override suspend fun handle(player: Player, packet: ResumeNameDialog) {
        if (!player.isRunning || !player.hasStarted()) return
        val e = player.tempAttribs.removeO<InputStringEvent>("pluginEnterName")
        e?.run(packet.name)
    }
}

class ResumeClanForumQFCDialogHandler : PacketHandler<Player, ResumeClanForumQFCDialog> {
    override suspend fun handle(player: Player, packet: ResumeClanForumQFCDialog) {
        if (!player.isRunning || !player.hasStarted()) return
        val e = player.tempAttribs.removeO<InputStringEvent>("pluginQFCD")
        e?.run(packet.forumQfc)
    }
}
