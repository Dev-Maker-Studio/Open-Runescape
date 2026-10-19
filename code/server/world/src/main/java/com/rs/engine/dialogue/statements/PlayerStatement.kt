package com.rs.engine.dialogue.statements

import com.rs.engine.dialogue.HeadE
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface

class PlayerStatement(private val emote: HeadE?, private vararg val texts: String) : Statement {

    override fun send(player: Player) {
        player.interfaceManager.sendChatBoxInterface(Interface.interface_1191)
        player.packets.setIFText(Component.interface_1191.component_1191_8, player.displayName)
        player.packets.setIFText(Component.interface_1191.component_1191_17, texts.joinToString("") { " $it" })
        player.packets.setIFPlayerHead(Component.interface_1191.component_1191_15)
        emote?.emoteId?.takeIf { it != -1 }?.let { player.packets.setIFAnimation(it, Component.interface_1191.component_1191_15) }
    }

    override fun getOptionId(componentId: Int) = 0

    override fun close(player: Player) {}
}
