package com.jagex.game.runetek5.interaction.chat.clanchat.channel

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node

abstract class AbstractClanChannelUpdate : Node<AbstractClanChannelUpdate>() {
    abstract fun decode(buffer: Packet)

    abstract fun applyTo(channel: ClanChannel)
}
