package com.rs.engine.dialogue.statements

import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import java.security.InvalidParameterException

private const val MAX_OPTIONS = 5
private const val DEFAULT_TITLE = "Select an Option"
private val FIRST_OPTION = Utils.componentIdFromHash(Component.interface_1188.component_1188_11)
private val OPTION_OFFSET = Utils.componentIdFromHash(Component.interface_1188.component_1188_12)

class OptionStatement(private val title: String?, private vararg val options: String?) : Statement {

    init {
        if (options.size > MAX_OPTIONS) throw InvalidParameterException("The max options length is $MAX_OPTIONS.")
    }

    override fun send(player: Player) {
        val shown = options.filterNotNull()
        val padded = List(MAX_OPTIONS) { shown.getOrElse(it) { "" } }
        player.interfaceManager.sendChatBoxInterface(Interface.interface_1188)
        player.packets.setIFText(Component.interface_1188.component_1188_20, title ?: DEFAULT_TITLE)
        player.packets.sendRunScriptReverse(Cs2.choice_v2_align, padded[4], padded[3], padded[2], padded[1], padded[0], options.size)
    }

    override fun getOptionId(componentId: Int) = if (componentId == FIRST_OPTION) 0 else componentId - OPTION_OFFSET

    override fun close(player: Player) {}
}
