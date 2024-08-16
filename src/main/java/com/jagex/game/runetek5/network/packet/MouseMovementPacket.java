package com.jagex.game.runetek5.network.packet;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.utils.Utils;

public class MouseMovementPacket extends AbstractMousePacket {

    public MouseRecord getRecord() {
        return (MouseRecord) client.MOUSE_MOVEMENTS.getFirst();
    }


    @Override
    public boolean canSubmit() {
        return getRecord() != null || super.lastUpdate < Utils.time() - 2000L;
    }

    @Override
    public TcpPacket createPacketBase() {
        return PacketEncoder.createPacket(ClientProt.MOVE_MOUSE_2, client.GAME_CONNECTION_CONTEXT.isaac);
    }

    @Override
    public void writeConstantValue(Packet packet, MouseRecord record) {
    }

    @Override
    public int additionalPayloadLength() {
        return 0;
    }
    @Override
    public void queuePacket() {
        MouseRecord record = getRecord();
        if (record != null) {
            int timeDelta = getServerSubmissionDelta(record, 32767);
            int positionY = record.getY();
            if (positionY < 0) {
                positionY = 0;
            } else if (positionY > 65535) {
                positionY = 65535;
            }
            int positionX = record.getX();
            if (positionX < 0) {
                positionX = 0;
            } else if (positionX > 65535) {
                positionX = 65535;
            }
            byte clickType = 0;
            if (record.getClickType() == 2) {
                clickType = 1;
            }
            TcpPacket packet = PacketEncoder.createPacket(ClientProt.MOUSE_CLICK, client.GAME_CONNECTION_CONTEXT.isaac);
            packet.buffer.writeIntLE(positionX | positionY << 16);
            packet.buffer.writeShort(timeDelta | clickType << 15);
            client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
        }
    }
}
