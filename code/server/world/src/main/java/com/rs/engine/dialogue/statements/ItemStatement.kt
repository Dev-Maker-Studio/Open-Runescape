package com.rs.engine.dialogue.statements

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface

private const val DEFAULT_ZOOM = 500

class ItemStatement(private val itemId: Int, private val zoom: Int, private vararg val text: String) : Statement {

    constructor(itemId: Int, vararg text: String) : this(itemId, DEFAULT_ZOOM, *text)

    override fun send(player: Player) {
        player.interfaceManager.sendChatBoxInterface(Interface.interface_1189)
        player.packets.sendRunScript(Cs2.objbox_set_zoom, itemId, zoom)
        player.packets.setIFText(Component.interface_1189.component_1189_4, text.joinToString("") { "$it<br>" })
    }

    override fun getOptionId(componentId: Int) = 0

    override fun close(player: Player) {}
}
