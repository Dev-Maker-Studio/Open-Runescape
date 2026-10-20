package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

class UpdateIntVariable(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField
    var key: Int = 0

    @JvmField
    var value: Int = 0

    override fun decode(buffer: Packet) {
        key = buffer.readInt()
        value = buffer.readInt()
    }

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.putIntVariable(key, value)
    }
}
