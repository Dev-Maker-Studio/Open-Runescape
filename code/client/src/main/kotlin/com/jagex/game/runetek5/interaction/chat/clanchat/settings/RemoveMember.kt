package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

/**
 * @author Steven - 18/09/2023
 */
class RemoveMember(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField
    var memberIndex: Int = -1

    override fun decode(buffer: Packet) {
        memberIndex = buffer.readUShort()
    }

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.removeMember(memberIndex)
    }
}
