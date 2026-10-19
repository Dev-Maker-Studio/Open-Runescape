package com.rs.engine.dialogue.statements

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface

class SimpleStatement(private vararg val texts: String) : Statement {

    override fun send(player: Player) {
        player.interfaceManager.sendChatBoxInterface(Interface.interface_1186)
        player.packets.setIFText(Component.interface_1186.component_1186_1, texts.joinToString("<br>", prefix = "<p=1>"))
    }

    override fun getOptionId(componentId: Int) = 0

    override fun close(player: Player) {}
}
