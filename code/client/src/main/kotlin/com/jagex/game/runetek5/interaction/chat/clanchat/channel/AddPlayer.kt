package com.jagex.game.runetek5.interaction.chat.clanchat.channel

import com.jagex.game.runetek5.buffer.Packet

class AddPlayer(
    @JvmField val clanChannelDelta: ClanChannelDelta
) : AbstractClanChannelUpdate() {

    @JvmField var name: String? = null

    @JvmField var world: Int = 0

    @JvmField var rank: Byte = 0

    override fun applyTo(channel: ClanChannel) {
        val player = ClanChannelPlayer()
        player.name = this.name
        player.world = this.world
        player.rank = this.rank
        channel.addPlayer(player)
    }

    override fun decode(buffer: Packet) {
        if (buffer.readUByte() != 255) {
            buffer.currentPosition--
            buffer.readLong()
        }
        this.name = buffer.readNullString()
        this.world = buffer.readUShort()
        this.rank = buffer.readByte()
        buffer.readLong()
    }
}
