package com.jagex.game.runetek5.network.packet;

import java.util.LinkedList;
import java.util.Queue;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer;
import com.jagex.game.runetek5.utils.Utils;

public class MouseClickPacket extends AbstractMousePacket {

    public final Queue<MouseActionServer> records = new LinkedList<MouseActionServer>();

    public void addToQueue(MouseActionServer record) {
        this.records.add(record);
        if (this.records.size() > 10) {
            this.records.poll();
        }
    }

    @Override
    public void queuePacket() {
        MouseActionServer record = this.records.poll();
        if (record != null) {
            TcpPacket packet = PacketEncoder.createPacket(ClientProt.MOUSE_BUTTON_CLICK, client.GAME_CONNECTION_CONTEXT.isaac);
            packet.buffer.writeIntLE(record.getX() | record.getY() << 16);
            packet.buffer.writeByte128(record.getClickMeta() << 1 | record.getConstant() & 0x1);
            packet.buffer.writeShortLittle(getServerSubmissionDelta(record, 0xFFFF));
            client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
            record.pushToTop();
        }
    }

    public void writeConstant(Packet packet, MouseActionServer record) {
        packet.writeByte(record.getConstant());
    }

    @Override
    public boolean canSubmit() {
        return !this.records.isEmpty() || super.lastUpdate < Utils.time() - 2000L;
    }

    @Override
    public int additionalPayloadLength() {
        return 1;
    }


    @Override
    public TcpPacket createPacketBase() {
        return PacketEncoder.createPacket(ClientProt.MOVE_MOUSE, client.GAME_CONNECTION_CONTEXT.isaac);
    }


    @Override
    public void writeConstantValue(Packet packet, MouseRecord record) {
        writeConstant(packet, (MouseActionServer) record);
    }
}
