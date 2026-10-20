package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.utils.Utils

class MouseMovementPacket : AbstractMousePacket() {

    fun getRecord(): MouseRecord? {
        return Client.MOUSE_MOVEMENTS.getFirst() as? MouseRecord
    }

    override fun canSubmit(): Boolean {
        return getRecord() != null || lastUpdate < Utils.time() - 2000L
    }

    override fun createPacketBase(): TcpPacket {
        return PacketEncoder.createPacket(ClientProt.MOVE_MOUSE_2, Client.GAME_CONNECTION_CONTEXT.isaac)
    }

    override fun writeConstantValue(packet: Packet, record: MouseRecord) {
    }

    override fun additionalPayloadLength(): Int = 0

    override fun queuePacket() {
        val record = getRecord() ?: return

        val timeDelta = getServerSubmissionDelta(record, 32767)

        var positionY = record.getY()
        if (positionY < 0) {
            positionY = 0
        } else if (positionY > 65535) {
            positionY = 65535
        }

        var positionX = record.getX()
        if (positionX < 0) {
            positionX = 0
        } else if (positionX > 65535) {
            positionX = 65535
        }

        val clickType: Byte = if (record.getClickType() == 2) 1 else 0

        val packet = PacketEncoder.createPacket(ClientProt.MOUSE_CLICK, Client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeIntLittle(positionX or (positionY shl 16))
        packet.buffer.writeShort(timeDelta or (clickType.toInt() shl 15))
        Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }
}
