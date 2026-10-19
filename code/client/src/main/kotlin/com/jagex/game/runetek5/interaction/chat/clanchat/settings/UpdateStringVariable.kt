package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

/**
 * @author Steven - 18/09/2023
 */
class UpdateStringVariable(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField
    var key: Int = 0

    @JvmField
    var value: String? = null

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.putStringValue(key, value)
    }

    override fun decode(buffer: Packet) {
        key = buffer.readInt()
        value = buffer.readString()
    }
}
