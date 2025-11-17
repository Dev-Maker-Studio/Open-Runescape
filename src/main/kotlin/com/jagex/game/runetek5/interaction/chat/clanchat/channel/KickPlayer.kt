package com.jagex.game.runetek5.interaction.chat.clanchat.channel

import com.jagex.game.runetek5.buffer.Packet

class KickPlayer(
    @JvmField val clanChannelDelta: ClanChannelDelta
) : AbstractClanChannelUpdate() {

    @JvmField var playerIndex: Int = -1

    override fun decode(buffer: Packet) {
        this.playerIndex = buffer.readUShort()
        buffer.readUByte()
        if (buffer.readUByte() != 255) {
            buffer.currentPosition--
            buffer.readLong()
        }
    }

    override fun applyTo(clanChannel: ClanChannel) {
        clanChannel.removePlayer(this.playerIndex)
    }
}
