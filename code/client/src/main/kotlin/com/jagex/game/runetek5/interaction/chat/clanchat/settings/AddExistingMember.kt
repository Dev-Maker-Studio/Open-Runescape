package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

class AddExistingMember(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField var memberHash: Long = -1L
    @JvmField var memberName: String? = null

    @JvmField var joinRuneday: Int = 0

    override fun decode(buffer: Packet) {
        if (buffer.readUByte() != 255) {
            buffer.currentPosition--
            this.memberHash = buffer.readLong()
        }
        this.memberName = buffer.readNullString()
        this.joinRuneday = buffer.readUShort()
    }

    override fun apply(clanSettings: ClanSettings) {
        clanSettings.addMember(this.memberHash, this.memberName, this.joinRuneday)
    }
}
