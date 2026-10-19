package com.rs.engine.dialogue.statements

import com.rs.engine.dialogue.HeadE
import com.rs.game.model.entity.player.Player
import org.darkan.core.cache.NPCDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface

class NPCStatement(private val nameOverride: String?, private val npcId: Int, private val emote: HeadE?, private vararg val texts: String) : Statement {

    constructor(npcId: Int, emote: HeadE?, vararg texts: String) : this(null, npcId, emote, *texts)

    override fun send(player: Player) {
        player.interfaceManager.sendChatBoxInterface(Interface.interface_1184)
        player.packets.setIFText(Component.interface_1184.component_1184_17, nameOverride ?: NPCDefinitions.getDefs(npcId, player.vars).name)
        player.packets.setIFText(Component.interface_1184.component_1184_13, texts.joinToString("") { " $it" })
        player.packets.setIFNPCHead(Component.interface_1184.component_1184_11, npcId)
        emote?.emoteId?.takeIf { it != -1 }?.let { player.packets.setIFAnimation(it, Component.interface_1184.component_1184_11) }
    }

    override fun getOptionId(componentId: Int) = 0

    override fun close(player: Player) {}
}
