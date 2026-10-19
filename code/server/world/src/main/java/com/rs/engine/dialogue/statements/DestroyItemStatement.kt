package com.rs.engine.dialogue.statements

import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.model.Item

private val CONFIRM_OPTION = Utils.componentIdFromHash(Component.interface_1183.component_1183_9)

class DestroyItemStatement(private val item: Item, private val message: String?) : Statement {

    override fun send(player: Player) {
        player.interfaceManager.sendChatBoxInterface(Interface.interface_1183)
        player.packets.setIFText(Component.interface_1183.component_1183_7, item.name)
        message?.let { player.packets.setIFText(Component.interface_1183.component_1183_12, it) }
        player.packets.setIFItem(Component.interface_1183.component_1183_13, item.id, 1)
    }

    override fun getOptionId(componentId: Int) = if (componentId == CONFIRM_OPTION) 0 else 1

    override fun close(player: Player) {}
}
