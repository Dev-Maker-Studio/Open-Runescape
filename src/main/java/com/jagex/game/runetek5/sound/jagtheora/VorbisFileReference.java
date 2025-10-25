package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class VorbisFileReference extends JagTheoraManager {

    public int[] values;
    public int anInt8115;
    public final byte[][] aByteArrayArray8111 = new byte[10][];
    public final Packet aNodeSub358112 = new Packet();
    public final Packet byteBuffer = new Packet();
    public final Js5ResourceProvider resourceProvider;
    public final int fileId;

    public VorbisFileReference(Js5ResourceProvider resourceProvider, int fileId) {
        this.resourceProvider = resourceProvider;
        this.fileId = fileId;
    }

    @Override
    public int decode(byte[] bytes_1) {
        if (this.values == null) {
            if (!this.resourceProvider.isCached(this.fileId, 0)) {
                return 0;
            }
            byte[] data = this.resourceProvider.getFile(this.fileId, 0);
            if (data == null) {
                throw new IllegalStateException("");
            }
            this.byteBuffer.buffer = data;
            this.byteBuffer.currentPosition = 0;
            int len = data.length >> 1;
            this.values = new int[len];
            for (int i_5 = 0; i_5 < len; i_5++) {
                this.values[i_5] = this.byteBuffer.readUShort();
            }
        }
        if (this.anInt8115 >= this.values.length) {
            return -1;
        }
        method13447();
        this.byteBuffer.buffer = bytes_1;
        this.byteBuffer.currentPosition = 0;
        do {
            if (this.byteBuffer.currentPosition >= this.byteBuffer.buffer.length) {
                this.byteBuffer.buffer = new byte[0];
                return bytes_1.length;
            }
            if (this.aNodeSub358112.buffer.length == 0) {
                if (this.aByteArrayArray8111[0] == null) {
                    this.byteBuffer.buffer = new byte[0];
                    return this.byteBuffer.currentPosition;
                }
                this.aNodeSub358112.buffer = this.aByteArrayArray8111[0];
            }
            int i_6 = this.byteBuffer.buffer.length - this.byteBuffer.currentPosition;
            int len = this.aNodeSub358112.buffer.length - this.aNodeSub358112.currentPosition;
            if (i_6 < len) {
                this.aNodeSub358112.readBytes(this.byteBuffer.buffer, this.byteBuffer.currentPosition, i_6);
                this.byteBuffer.buffer = new byte[0];
                return bytes_1.length;
            }
            this.byteBuffer.writeBytes(this.aNodeSub358112.buffer, this.aNodeSub358112.currentPosition, len);
            this.aNodeSub358112.buffer = new byte[0];
            this.aNodeSub358112.currentPosition = 0;
            ++this.anInt8115;
            System.arraycopy(this.aByteArrayArray8111, 1, this.aByteArrayArray8111, 0, 9);
            this.aByteArrayArray8111[9] = null;
        } while (this.anInt8115 < this.values.length);
        this.byteBuffer.buffer = new byte[0];
        return this.byteBuffer.currentPosition;
    }

    public void method13447() {
        if (this.values != null) {
            for (int i_2 = 0; i_2 < 10 && i_2 + this.anInt8115 < this.values.length; i_2++) {
                if (this.aByteArrayArray8111[i_2] == null && this.resourceProvider.isCached(this.values[i_2 + this.anInt8115], 0)) {
                    this.aByteArrayArray8111[i_2] = this.resourceProvider.getFile(this.values[i_2 + this.anInt8115], 0);
                }
            }
        }
    }
}
