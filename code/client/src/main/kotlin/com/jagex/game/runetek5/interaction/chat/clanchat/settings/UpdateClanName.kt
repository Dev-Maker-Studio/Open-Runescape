package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

class UpdateClanName(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField
    var clanName: String? = null

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.clanName = clanName
    }

    override fun decode(buffer: Packet) {
        clanName = buffer.readString()
        buffer.readInt()
    }
}
