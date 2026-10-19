package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
abstract class AbstractClanSettingUpdate : Node<Any?>() {
    abstract fun decode(buffer: Packet)

    abstract fun apply(clansettings: ClanSettings)
}
