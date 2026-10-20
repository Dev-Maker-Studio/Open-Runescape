package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

class BanUser(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField var bannedUserHash: Long = -1L
    @JvmField var bannedUserName: String? = null

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.banUser(this.bannedUserHash, this.bannedUserName)
    }

    override fun decode(buffer: Packet) {
        if (buffer.readUByte() != 255) {
            buffer.currentPosition--
            this.bannedUserHash = buffer.readLong()
        }

        this.bannedUserName = buffer.readNullString()
    }
}
