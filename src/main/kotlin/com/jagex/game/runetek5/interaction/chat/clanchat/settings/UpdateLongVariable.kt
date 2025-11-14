package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

/**
 * @author Brainpower - 11/01/2022
 */
class UpdateLongVariable(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField
    var key: Int = 0

    @JvmField
    var value: Long = 0

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.setLongVariable(key, value)
    }

    override fun decode(buffer: Packet) {
        key = buffer.readInt()
        value = buffer.readLong()
    }
}
