package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

/**
 * @author Brainpower - 11/01/2022
 */
class UnbanUser(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField
    var bannedUserIndex: Int = -1

    override fun decode(buffer: Packet) {
        bannedUserIndex = buffer.readUShort()
    }

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.unbanUser(bannedUserIndex)
    }
}
