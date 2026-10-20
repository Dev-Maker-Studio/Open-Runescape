package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.utils.Utils
import java.util.LinkedList
import java.util.Queue

class MouseClickPacket : AbstractMousePacket() {

    @JvmField val records: Queue<MouseActionServer> = LinkedList()

    fun addToQueue(record: MouseActionServer) {
        records.add(record)
        if (records.size > 10) {
            records.poll()
        }
    }

    override fun queuePacket() {
        val record = records.poll()
        if (record != null) {
            val packet = PacketEncoder.createPacket(ClientProt.MOUSE_BUTTON_CLICK, Client.GAME_CONNECTION_CONTEXT.isaac)
            packet.buffer.writeIntLittle(record.getX() or (record.getY() shl 16))
            packet.buffer.writeByteAdd((record.getClickMeta() shl 1) or (record.getConstant() and 0x1))
            packet.buffer.writeShortLittle(getServerSubmissionDelta(record, 0xFFFF))
            Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
            record.pushToTop()
        }
    }

    fun writeConstant(packet: Packet, record: MouseActionServer) {
        packet.writeByte(record.getConstant())
    }

    override fun canSubmit(): Boolean {
        return records.isNotEmpty() || lastUpdate < Utils.time() - 2000L
    }

    override fun additionalPayloadLength(): Int {
        return 1
    }

    override fun createPacketBase(): TcpPacket {
        return PacketEncoder.createPacket(ClientProt.MOVE_MOUSE, Client.GAME_CONNECTION_CONTEXT.isaac)
    }

    override fun writeConstantValue(packet: Packet, record: MouseRecord) {
        writeConstant(packet, record as MouseActionServer)
    }
}
