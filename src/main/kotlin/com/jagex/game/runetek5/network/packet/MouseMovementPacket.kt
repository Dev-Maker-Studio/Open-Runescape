package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.utils.Utils

/**
 * Network packet for transmitting mouse movement and click data to the server.
 * Batches mouse events and sends them periodically.
 */
class MouseMovementPacket : AbstractMousePacket() {

    /**
     * Gets the next mouse record to send
     * @return The first mouse record in the queue, or null if empty
     */
    fun getRecord(): MouseRecord? {
        return client.MOUSE_MOVEMENTS.getFirst() as? MouseRecord
    }

    override fun canSubmit(): Boolean {
        return getRecord() != null || lastUpdate < Utils.time() - 2000L
    }

    override fun createPacketBase(): TcpPacket {
        return PacketEncoder.createPacket(ClientProt.MOVE_MOUSE_2, client.GAME_CONNECTION_CONTEXT.isaac)
    }

    override fun writeConstantValue(packet: Packet, record: MouseRecord) {
        // No constant values to write
    }

    override fun additionalPayloadLength(): Int = 0

    override fun queuePacket() {
        val record = getRecord() ?: return

        val timeDelta = getServerSubmissionDelta(record, 32767)

        // Clamp Y position to valid range
        var positionY = record.getY()
        if (positionY < 0) {
            positionY = 0
        } else if (positionY > 65535) {
            positionY = 65535
        }

        // Clamp X position to valid range
        var positionX = record.getX()
        if (positionX < 0) {
            positionX = 0
        } else if (positionX > 65535) {
            positionX = 65535
        }

        // Encode click type (0 = move, 1 = click)
        val clickType: Byte = if (record.getClickType() == 2) 1 else 0

        // Create and send packet
        val packet = PacketEncoder.createPacket(ClientProt.MOUSE_CLICK, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeIntLittle(positionX or (positionY shl 16))
        packet.buffer.writeShort(timeDelta or (clickType.toInt() shl 15))
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }
}
