package com.jagex.game.runetek5.config.sequence.frameset;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;
import com.jagex.game.runetek5.buffer.Packet;

public class AnimFrame {

    private static final short[] INDICES = new short[500];
    private static final short[] TRANSFORM_X = new short[500];
    private static final short[] TRANSFORM_Y = new short[500];
    private static final short[] TRANSFORM_Z = new short[500];
    private static final short[] SKIPPED = new short[500];
    private static final byte[] FLAGS = new byte[500];
    public final AnimBase base;
    public int numTransformations;
    public boolean modifiesAlpha;
    public boolean modifiesColor;
    public boolean modifiesBillboard;
    public short[] baseIndices;
    public short[] transformationX;
    public short[] transformationY;
    public short[] transformationZ;
    public short[] labels;
    public byte[] tweeningProperties;

    public AnimFrame(byte[] data, AnimBase frameList) {
        this.base = frameList;
        try {
            Packet attribBuffer = new Packet(data);
            Packet transformationBuffer = new Packet(data);
            attribBuffer.readUnsignedByte();
            attribBuffer.currentPosition += 2;
            int count = attribBuffer.readUnsignedByte();
            int used = 0;
            int last = -1;
            int lastUsed = -1;
            transformationBuffer.currentPosition = count + attribBuffer.currentPosition;
            int index;
            for (index = 0; index < count; index++) {
                int type = this.base.transformationType[index];
                if (type == 0) {
                    last = index;
                }
                int attribute = attribBuffer.readUnsignedByte();
                if (attribute > 0) {
                    if (type == 0) {
                        lastUsed = index;
                    }
                    INDICES[used] = (short) index;
                    short value = 0;
                    if (type == 3 || type == 10) {
                        value = 128;
                    }

                    if ((attribute & DISPLACE_X) != 0) {
                        TRANSFORM_X[used] = (short) transformationBuffer.readSignedSmart();
                    } else {
                        TRANSFORM_X[used] = value;
                    }

                    if ((attribute & DISPLACE_Y) != 0) {
                        TRANSFORM_Y[used] = (short) transformationBuffer.readSignedSmart();
                    } else {
                        TRANSFORM_Y[used] = value;
                    }

                    if ((attribute & DISPLACE_Z) != 0) {
                        TRANSFORM_Z[used] = (short) transformationBuffer.readSignedSmart();
                    } else {
                        TRANSFORM_Z[used] = value;
                    }

                    FLAGS[used] = (byte) (attribute >>> 3 & 0x3);
                    if (type == 2 || type == 9) {
                        TRANSFORM_X[used] = (short) (TRANSFORM_X[used] << 2 & 0x3fff);
                        TRANSFORM_Y[used] = (short) (TRANSFORM_Y[used] << 2 & 0x3fff);
                        TRANSFORM_Z[used] = (short) (TRANSFORM_Z[used] << 2 & 0x3fff);
                    }

                    SKIPPED[used] = -1;
                    if (type != 1 && type != 2 && type != 3) {
                        if (type == 5) {
                            this.modifiesAlpha = true;
                        } else if (type == 7) {
                            this.modifiesColor = true;
                        } else if (type == 9 || type == 10 || type == 8) {
                            this.modifiesBillboard = true;
                        }
                    } else if (last > lastUsed) {
                        SKIPPED[used] = (short) last;
                        lastUsed = last;
                    }
                    used++;
                }
            }

            if (data.length != transformationBuffer.currentPosition) {
                throw new RuntimeException();
            }

            this.numTransformations = used;
            this.baseIndices = new short[used];
            this.transformationX = new short[used];
            this.transformationY = new short[used];
            this.transformationZ = new short[used];
            this.labels = new short[used];
            this.tweeningProperties = new byte[used];

            for (index = 0; index < used; index++) {
                this.baseIndices[index] = INDICES[index];
                this.transformationX[index] = TRANSFORM_X[index];
                this.transformationY[index] = TRANSFORM_Y[index];
                this.transformationZ[index] = TRANSFORM_Z[index];
                this.labels[index] = SKIPPED[index];
                this.tweeningProperties[index] = FLAGS[index];
            }
        } catch (Exception ex) {
            this.numTransformations = 0;
            this.modifiesAlpha = false;
            this.modifiesColor = false;
        }
    }
}