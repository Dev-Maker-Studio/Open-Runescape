package com.jagex.game.runetek5.interaction.chat.clanchat.channel

import com.jagex.game.runetek5.buffer.Packet

/**
 *
 * @author Brainpower - 14/01/2023
 *
 */
class UpdatePlayer(
    @JvmField val clanChannelDelta: ClanChannelDelta
) : AbstractClanChannelUpdate() {

    @JvmField var playerIndex: Int = -1
    @JvmField var rank: Byte = 0
    @JvmField var world: Int = 0
    @JvmField var name: String? = null

    override fun applyTo(clanChannel: ClanChannel) {
        val player = clanChannel.players!![this.playerIndex]!!
        player.rank = this.rank
        player.world = this.world
        player.name = this.name
    }

    override fun decode(buffer: Packet) {
        this.playerIndex = buffer.readUShort()
        this.rank = buffer.readByte()
        this.world = buffer.readUShort()
        buffer.readLong()
        this.name = buffer.readString()
    }
}
