package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

import jagtheora.ogg.OggPacket;
import jagtheora.ogg.OggStreamState;

public class Node_Sub41_Sub2 extends Node_Sub41 {

    public int anInt9464;
    public int anInt9467;
    public String languageAbbreviation;
    public String aString9469;
    public float aFloat9462;
    public float aFloat9465;
    public String aString9470;

    public Node_Sub41_Sub2(OggStreamState oggstreamstate1) {
        super(oggstreamstate1);
    }

    @Override
    void decode(OggPacket oggpacket1) {
        if (super.anInt8031 <= 0 || "SUB".equals(this.aString9469)) {
            Packet rsbytebuffer3 = new Packet(oggpacket1.getData());
            int i_4 = rsbytebuffer3.readUByte();
            if (super.anInt8031 <= 8) {
                if ((i_4 | 0x80) == 0) {
                    throw new IllegalStateException();
                }

                if (super.anInt8031 == 0) {
                    rsbytebuffer3.currentPosition += 23;
                    this.anInt9464 = rsbytebuffer3.readUIntLittle();
                    this.anInt9467 = rsbytebuffer3.readUIntLittle();
                    if (this.anInt9464 == 0 || this.anInt9467 == 0) {
                        throw new IllegalStateException();
                    }

                    Packet rsbytebuffer12 = new Packet(16);
                    rsbytebuffer3.readBytes(rsbytebuffer12.buffer, 0, 16);
                    this.languageAbbreviation = rsbytebuffer12.readString();
                    rsbytebuffer12.currentPosition = 0;
                    rsbytebuffer3.readBytes(rsbytebuffer12.buffer, 0, 16);
                    this.aString9469 = rsbytebuffer12.readString();
                }
            } else {
                if (i_4 == 0) {
                    label64:
                    {
                        long long_5 = rsbytebuffer3.readULongLittle();
                        long long_7 = rsbytebuffer3.readULongLittle();
                        long long_9 = rsbytebuffer3.readULongLittle();
                        if (long_7 >= 0L && long_9 >= 0L && long_9 <= long_5) {
                            this.aFloat9462 = (float) (this.anInt9467 * long_5) / this.anInt9464;
                            this.aFloat9465 = (float) (this.anInt9467 * (long_7 + long_5)) / this.anInt9464;
                            int i_11 = rsbytebuffer3.readUIntLittle();
                            if (i_11 >= 0 && i_11 <= rsbytebuffer3.buffer.length - rsbytebuffer3.currentPosition) {
                                this.aString9470 = StaticMethods.method4824(rsbytebuffer3.buffer, rsbytebuffer3.currentPosition, i_11);
                                break label64;
                            }

                            throw new IllegalStateException();
                        }

                        throw new IllegalStateException();
                    }
                }

                if ((i_4 | 0x80) != 0) {
                }
            }
        }

    }

    public float method14702() {
        return this.aFloat9462;
    }

    public String getLanguageAbbreviation() {
        return this.languageAbbreviation;
    }

    public String method14704() {
        return this.aString9470;
    }

    @Override
    public void reset() {
    }

    public float method14705() {
        return this.aFloat9465;
    }
}
