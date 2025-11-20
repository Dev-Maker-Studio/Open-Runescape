package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.utils.Utils
import java.util.LinkedList
import java.util.Queue

/**
 * Packet handler for mouse click events sent to the server.
 */
class MouseClickPacket : AbstractMousePacket() {

    @JvmField val records: Queue<MouseActionServer> = LinkedList()

    /**
     * Adds a mouse action to the queue for sending to the server.
     *
     * @param record The mouse action to queue
     */
    fun addToQueue(record: MouseActionServer) {
        records.add(record)
        if (records.size > 10) {
            records.poll()
        }
    }

    override fun queuePacket() {
        val record = records.poll()
        if (record != null) {
            val packet = PacketEncoder.createPacket(ClientProt.MOUSE_BUTTON_CLICK, client.GAME_CONNECTION_CONTEXT.isaac)
            packet.buffer.writeIntLittle(record.getX() or (record.getY() shl 16))
            packet.buffer.writeByteAdd((record.getClickMeta() shl 1) or (record.getConstant() and 0x1))
            packet.buffer.writeShortLittle(getServerSubmissionDelta(record, 0xFFFF))
            client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
            record.pushToTop()
        }
    }

    /**
     * Writes the constant value for a mouse action to the packet.
     *
     * @param packet The packet to write to
     * @param record The mouse action record
     */
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
        return PacketEncoder.createPacket(ClientProt.MOVE_MOUSE, client.GAME_CONNECTION_CONTEXT.isaac)
    }

    override fun writeConstantValue(packet: Packet, record: MouseRecord) {
        writeConstant(packet, record as MouseActionServer)
    }
}
