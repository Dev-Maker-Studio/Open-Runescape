package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node

class TcpPacket : Node<TcpPacket>() {
    @JvmField var capacity: Int = 0
    @JvmField var buffer: Packet.Bit = Packet.Bit(0)
    @JvmField var position: Int = 0
    @JvmField var packet: ClientProt? = null

    fun push() {
        if (index < OUTGOING_PACKETS.size) {
            OUTGOING_PACKETS[index++] = this
        }
    }

    companion object {
        @JvmField val OUTGOING_PACKETS = arrayOfNulls<TcpPacket>(300)
        @JvmField var index: Int = 0

        @JvmStatic
        fun createLoginPacket(): TcpPacket {
            val tcpPacket = createPacket()
            tcpPacket.packet = null
            tcpPacket.position = 0
            tcpPacket.buffer = Packet.Bit(5000)
            return tcpPacket
        }

        @JvmStatic
        fun createPacket(): TcpPacket {
            return if (index == 0) TcpPacket() else OUTGOING_PACKETS[--index]!!
        }
    }
}
