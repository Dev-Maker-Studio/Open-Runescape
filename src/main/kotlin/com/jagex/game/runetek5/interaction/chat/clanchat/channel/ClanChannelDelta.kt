package com.jagex.game.runetek5.interaction.chat.clanchat.channel

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.NodeCollection

class ClanChannelDelta(buffer: Packet) {

    @JvmField var pointer: Long = 0L
    @JvmField var updateNumber: Long = -1L
    @JvmField val updates: NodeCollection = NodeCollection()

    init {
        decode(buffer)
    }

    fun decode(buffer: Packet) {
        this.pointer = buffer.readLong()
        this.updateNumber = buffer.readLong()
        var opcode = buffer.readUByte()
        while (opcode != 0) {
            val channelUpdate: AbstractClanChannelUpdate = when (opcode) {
                1 -> AddPlayer(this)
                2 -> UpdatePlayer(this)
                3 -> KickPlayer(this)
                4 -> UpdateRights(this)
                else -> throw RuntimeException("")
            }
            channelUpdate.decode(buffer)
            this.updates.addFirst(channelUpdate)
            opcode = buffer.readUByte()
        }
    }

    fun apply(clanChannel: ClanChannel) {
        if (clanChannel.pointer == this.pointer && clanChannel.nextUpdateNumber == this.updateNumber) {
            var update = this.updates.getFirst() as AbstractClanChannelUpdate?
            while (update != null) {
                update.applyTo(clanChannel)
                update = this.updates.next() as AbstractClanChannelUpdate?
            }
            clanChannel.nextUpdateNumber++
        } else {
            throw RuntimeException("")
        }
    }
}
