package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet

class AddNewMember(
    @JvmField val clanSettingsDelta: ClanSettingsDelta
) : AbstractClanSettingUpdate() {

    @JvmField var memberHash: Long = -1L
    @JvmField var memberName: String? = null

    override fun decode(buffer: Packet) {
        if (buffer.readUByte() != 255) {
            buffer.currentPosition--
            this.memberHash = buffer.readLong()
        }
        this.memberName = buffer.readNullString()
    }

    override fun apply(clansettings: ClanSettings) {
        clansettings.addMember(this.memberHash, this.memberName, 0)
    }
}
