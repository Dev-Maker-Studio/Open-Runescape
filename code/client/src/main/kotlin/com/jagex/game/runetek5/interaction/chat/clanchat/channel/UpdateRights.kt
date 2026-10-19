package com.jagex.game.runetek5.interaction.chat.clanchat.channel

import com.jagex.game.runetek5.buffer.Packet

/**
 *
 * @author Brainpower - 14/01/2023
 *
 */
class UpdateRights(
    @JvmField val clanSettingsDelta: ClanChannelDelta
) : AbstractClanChannelUpdate() {

    @JvmField var clanName: String? = null
    @JvmField var guestsTalk: Byte = 0
    @JvmField var minRankToKick: Byte = 0

    override fun applyTo(channel: ClanChannel) {
        channel.clanName = this.clanName
        if (this.clanName != null) {
            channel.guestsTalk = this.guestsTalk
            channel.minRankToKick = this.minRankToKick
        }
    }

    override fun decode(buffer: Packet) {
        this.clanName = buffer.readNullString()
        if (this.clanName != null) {
            buffer.readUByte()
            this.guestsTalk = buffer.readByte()
            this.minRankToKick = buffer.readByte()
        }
    }
}
