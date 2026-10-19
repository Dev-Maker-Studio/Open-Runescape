package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

/**
 *
 * @author Brainpower - 11/01/2022
 *
 */
class UpdatePermission(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField var allowGuests: Boolean = false
    @JvmField var talkRank: Byte = 0
    @JvmField var kickRank: Byte = 0
    @JvmField var lootshareRank: Byte = 0
    @JvmField var coinShare: Byte = 0

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.allowGuests = this.allowGuests
        clanSettings.talkRank = this.talkRank
        clanSettings.kickRank = this.kickRank
        clanSettings.lootshareRank = this.lootshareRank
        clanSettings.coinShare = this.coinShare
    }

    override fun decode(buffer: Packet) {
        this.allowGuests = buffer.readUByte() == 1
        this.talkRank = buffer.readByte()
        this.kickRank = buffer.readByte()
        this.lootshareRank = buffer.readByte()
        this.coinShare = buffer.readByte()
    }
}
