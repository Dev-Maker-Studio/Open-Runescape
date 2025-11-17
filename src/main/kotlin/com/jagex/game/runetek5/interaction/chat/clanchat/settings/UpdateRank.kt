package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

/**
 *
 * @author Brainpower - 11/01/2022
 *
 */
class UpdateRank(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField var memberIndex: Int = -1
    @JvmField var rank: Byte = 0

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.updateRank(this.memberIndex, this.rank)
    }

    override fun decode(buffer: Packet) {
        this.memberIndex = buffer.readUShort()
        this.rank = buffer.readByte()
    }
}
