package com.jagex.game.runetek5.config.material.texture.operation;

import java.util.Random;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureRaster;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.utils.ArrayUtils;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class TextureOpVoronoiNoise extends TextureOperation {

    public static int anInt4266;
    public static int anInt4777;
    public static int anInt10003;
    public byte[] aByteArray9965 = new byte[512];
    public short[] aShortArray9966 = new short[512];
    public int randomSeed;
    public int anInt9960 = 2048;
    public int anInt9961 = 5;
    public int anInt9962 = 5;
    public int anInt9963 = 2;
    public int anInt9949 = 1;

    public TextureOpVoronoiNoise() {
        super(0, true);
    }

    @Override
    public int[] getMonochromeOutput(int i_1) {
        int[] ints_3 = super.monochromeImageCache.getPalette(i_1);
        if (super.monochromeImageCache.dirty) {
            int i_4 = TextureRaster.verticalTable[i_1] * this.anInt9962 + 2048;
            int i_5 = i_4 >> 12;
            int i_6 = i_5 + 1;
            for (int i_7 = 0; i_7 < TextureRaster.currentWidth; i_7++) {
                anInt10003 = Integer.MAX_VALUE;
                anInt4777 = Integer.MAX_VALUE;
                StaticMethods.anInt118 = Integer.MAX_VALUE;
                anInt4266 = Integer.MAX_VALUE;
                int i_8 = TextureRaster.horizontalTable[i_7] * this.anInt9961 + 2048;
                int i_9 = i_8 >> 12;
                int i_10 = i_9 + 1;
                for (int i_11 = i_5 - 1; i_11 <= i_6; i_11++) {
                    int i_12 = this.aByteArray9965[(i_11 >= this.anInt9962 ? i_11 - this.anInt9962 : i_11) & 0xff] & 0xff;
                    for (int i_13 = i_9 - 1; i_13 <= i_10; i_13++) {
                        int i_14 = 2 * (this.aByteArray9965[i_12 + (i_13 >= this.anInt9961 ? i_13 - this.anInt9961 : i_13) & 0xff] & 0xff);
                        int i_15 = i_8 - (this.aShortArray9966[i_14++] + (i_13 << 12));
                        int i_16 = i_4 - (this.aShortArray9966[i_14] + (i_11 << 12));
                        int i_17;
                        switch (this.anInt9949) {
                            case 1:
                                i_17 = i_16 * i_16 + i_15 * i_15 >> 12;
                                break;
                            case 2:
                                i_17 = (i_15 < 0 ? -i_15 : i_15) + (i_16 < 0 ? -i_16 : i_16);
                                break;
                            case 3:
                                i_15 = i_15 < 0 ? -i_15 : i_15;
                                i_16 = i_16 < 0 ? -i_16 : i_16;
                                i_17 = Math.max(i_15, i_16);
                                break;
                            case 4:
                                i_15 = (int) (Math.sqrt((i_15 < 0 ? -i_15 : i_15) / 4096.0F) * 4096.0D);
                                i_16 = (int) (Math.sqrt((i_16 < 0 ? -i_16 : i_16) / 4096.0F) * 4096.0D);
                                i_17 = i_15 + i_16;
                                i_17 = i_17 * i_17 >> 12;
                                break;
                            case 5:
                                i_15 *= i_15;
                                i_16 *= i_16;
                                i_17 = (int) (Math.sqrt(Math.sqrt((i_16 + i_15) / 1.6777216E7F)) * 4096.0D);
                                break;
                            default:
                                i_17 = (int) (Math.sqrt((i_15 * i_15 + i_16 * i_16) / 1.6777216E7F) * 4096.0D);
                        }
                        if (i_17 < anInt4266) {
                            anInt10003 = anInt4777;
                            anInt4777 = StaticMethods.anInt118;
                            StaticMethods.anInt118 = anInt4266;
                            anInt4266 = i_17;
                        } else if (i_17 < StaticMethods.anInt118) {
                            anInt10003 = anInt4777;
                            anInt4777 = StaticMethods.anInt118;
                            StaticMethods.anInt118 = i_17;
                        } else if (i_17 < anInt4777) {
                            anInt10003 = anInt4777;
                            anInt4777 = i_17;
                        } else if (i_17 < anInt10003) {
                            anInt10003 = i_17;
                        }
                    }
                }
                switch (this.anInt9963) {
                    case 0:
                        ints_3[i_7] = anInt4266;
                        break;
                    case 1:
                        ints_3[i_7] = StaticMethods.anInt118;
                        break;
                    case 2:
                        ints_3[i_7] = StaticMethods.anInt118 - anInt4266;
                        break;
                    case 3:
                        ints_3[i_7] = anInt4777;
                        break;
                    case 4:
                        ints_3[i_7] = anInt10003;
                }
            }
        }
        return ints_3;
    }

    @Override
    public void postDecode() {
        this.aByteArray9965 = ArrayUtils.generateRandomlySeededBytearray(this.randomSeed);
        method15412();
    }

    public void method15412() {
        Random random2 = new Random(this.randomSeed);
        this.aShortArray9966 = new short[512];
        if (this.anInt9960 > 0) {
            for (int i_3 = 0; i_3 < 512; i_3++) {
                this.aShortArray9966[i_3] = (short) StaticMethods.method7931(random2, this.anInt9960);
            }
        }
    }

    @Override
    public void decodeOpcode(int i_1, Packet rsbytebuffer2) {
        switch (i_1) {
            case 0:
                this.anInt9961 = this.anInt9962 = rsbytebuffer2.readUByte();
                break;
            case 1:
                this.randomSeed = rsbytebuffer2.readUByte();
                break;
            case 2:
                this.anInt9960 = rsbytebuffer2.readUShort();
                break;
            case 3:
                this.anInt9963 = rsbytebuffer2.readUByte();
                break;
            case 4:
                this.anInt9949 = rsbytebuffer2.readUByte();
                break;
            case 5:
                this.anInt9961 = rsbytebuffer2.readUByte();
                break;
            case 6:
                this.anInt9962 = rsbytebuffer2.readUByte();
        }
    }
}
