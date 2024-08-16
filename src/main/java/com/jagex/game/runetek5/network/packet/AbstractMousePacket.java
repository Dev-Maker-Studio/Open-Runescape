package com.jagex.game.runetek5.network.packet;

import java.util.Iterator;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;

public abstract class AbstractMousePacket {

    public static short aShort7079;

    public long lastServerSubmission = -1L;

    public long lastUpdate = -1L;

    public int lastMouseX = -1;

    public int lastMouseY = -1;

    public final NodeCollection nodeCollection = new NodeCollection();

    public int getServerSubmissionDelta(MouseRecord record, int delta) {
        long timeDelta;
        if (this.lastServerSubmission == -1L) {
            timeDelta = delta;
        } else {
            timeDelta = record.getTime() - this.lastServerSubmission;
            if (timeDelta > delta) {
                timeDelta = delta;
            }
        }
        this.lastServerSubmission = record.getTime();
        return (int) timeDelta;
    }

    public void reset() {
        this.nodeCollection.remove_all();
        this.lastServerSubmission = -1L;
        this.lastUpdate = -1L;
        this.lastMouseX = -1;
        this.lastMouseY = -1;
    }

    public void append(MouseRecord record) {
        this.nodeCollection.addFirst(record);
    }

    public abstract int additionalPayloadLength();

    public abstract void writeConstantValue(Packet var1, MouseRecord var2);

    public abstract void queuePacket();

    public abstract TcpPacket createPacketBase();

    public void submitAll() {
        if (canSubmit()) {
            TcpPacket packet = null;
            int startOffset = 0;
            int i_4 = 0;
            int count = 0;
            Iterator recordIterator = this.nodeCollection.iterator();
            label105:
            while (true) {
                while (true) {
                    if (!recordIterator.hasNext()) {
                        break label105;
                    }
                    MouseRecord record = (MouseRecord) recordIterator.next();
                    if (packet != null && packet.buffer.currentPosition - startOffset >= 252 - (6 + additionalPayloadLength())) {
                        break label105;
                    }
                    record.unlink();
                    int mouseY = record.getY();
                    if (mouseY < -1) {
                        mouseY = -1;
                    } else if (mouseY > 65534) {
                        mouseY = 65534;
                    }
                    int mouseX = record.getX();
                    if (mouseX < -1) {
                        mouseX = -1;
                    } else if (mouseX > 65534) {
                        mouseX = 65534;
                    }
                    if (mouseX != this.lastMouseX || mouseY != this.lastMouseY) {
                        if (packet == null) {
                            packet = createPacketBase();
                            packet.buffer.writeByte(0);
                            startOffset = packet.buffer.currentPosition;
                            packet.buffer.currentPosition += 2;
                            i_4 = 0;
                            count = 0;
                        }
                        int mouseDeltaX;
                        int mouseDeltaY;
                        int i_12;
                        if (this.lastUpdate != -1L) {
                            mouseDeltaX = mouseX - this.lastMouseX;
                            mouseDeltaY = mouseY - this.lastMouseY;
                            i_12 = (int) ((record.getTime() - this.lastUpdate) / 20L);
                            i_4 = (int) (i_4 + (record.getTime() - this.lastUpdate) % 20L);
                        } else {
                            mouseDeltaX = mouseX;
                            mouseDeltaY = mouseY;
                            i_12 = Integer.MAX_VALUE;
                        }
                        this.lastMouseX = mouseX;
                        this.lastMouseY = mouseY;
                        if (i_12 < 8 && mouseDeltaX >= -32 && mouseDeltaX <= 31 && mouseDeltaY >= -32 && mouseDeltaY <= 31) {
                            mouseDeltaX += 32;
                            mouseDeltaY += 32;
                            packet.buffer.writeShort((i_12 << 12) + mouseDeltaY + (mouseDeltaX << 6));
                        } else if (i_12 < 32 && mouseDeltaX >= -128 && mouseDeltaX <= 127 && mouseDeltaY >= -128 && mouseDeltaY <= 127) {
                            mouseDeltaX += 128;
                            mouseDeltaY += 128;
                            packet.buffer.writeByte(i_12 + 128);
                            packet.buffer.writeShort(mouseDeltaY + (mouseDeltaX << 8));
                        } else if (i_12 < 32) {
                            packet.buffer.writeByte(i_12 + 192);
                            if (mouseX != -1 && mouseY != -1) {
                                packet.buffer.writeInt(mouseX | mouseY << 16);
                            } else {
                                packet.buffer.writeInt(Integer.MIN_VALUE);
                            }
                        } else {
                            packet.buffer.writeShort((i_12 & 0x1fff) + 57344);
                            if (mouseX != -1 && mouseY != -1) {
                                packet.buffer.writeInt(mouseX | mouseY << 16);
                            } else {
                                packet.buffer.writeInt(Integer.MIN_VALUE);
                            }
                        }
                        ++count;
                        writeConstantValue(packet.buffer, record);
                        this.lastUpdate = record.getTime();
                    }
                    record.pushToTop();
                }
            }
            if (packet != null) {
                packet.buffer.writeIndex(packet.buffer.currentPosition - startOffset);
                int positionBeforeWriting = packet.buffer.currentPosition;
                packet.buffer.currentPosition = startOffset;
                packet.buffer.writeByte(i_4 / count);
                packet.buffer.writeByte(i_4 % count);
                packet.buffer.currentPosition = positionBeforeWriting;
                client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
            }
        }
        queuePacket();
    }

    public abstract boolean canSubmit();
}
