package com.jagex.game.runetek5.graphics.light;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;
import com.jagex.game.runetek5.buffer.ElementsBufferGL;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.HashTable;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ShortNode;
import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer;
import com.jagex.game.runetek5.utils.MathUtils;

public class LightGL extends Node<LightGL> {

    public int anInt7646;
    public final OpenGlToolkit aGraphicalRendererSub17650;
    public final PointLight aNodeSub247641;
    public final GroundGL aGroundSub27654;
    public final int anInt7642;
    public final int anInt7653;
    public final int anInt7644;
    public final int anInt7656;
    public float[][] aFloatArrayArray7652;
    public float[][] aFloatArrayArray7648;
    public float[][] aFloatArrayArray7645;
    public final ElementsBufferGL anInterface157639;
    public final ArrayBufferGL anInterface147649;
    public final OpenGlArrayDrawBuffer aClass1437647;
    public final OpenGlArrayDrawBuffer aClass1437643;
    public Packet aNodeSub357655;
    public GlBitBuffer bitBuffer;
    public HashTable aClass4537657;
    public int anInt7640;

    public LightGL(OpenGlToolkit class505Sub11, GroundGL class390Sub22, PointLight class282Sub243, int[] ints_4) {
        this.aGraphicalRendererSub17650 = class505Sub11;
        this.aNodeSub247641 = class282Sub243;
        this.aGroundSub27654 = class390Sub22;
        int i_5 = this.aNodeSub247641.getSize2d() - (class390Sub22.tileUnits >> 1);
        this.anInt7642 = this.aNodeSub247641.getX() - i_5 >> class390Sub22.tileScale;
        this.anInt7653 = this.aNodeSub247641.getX() + i_5 >> class390Sub22.tileScale;
        this.anInt7644 = this.aNodeSub247641.getZ() - i_5 >> class390Sub22.tileScale;
        this.anInt7656 = this.aNodeSub247641.getZ() + i_5 >> class390Sub22.tileScale;
        int i_6 = this.anInt7653 - this.anInt7642 + 1;
        int i_7 = this.anInt7656 - this.anInt7644 + 1;
        this.aFloatArrayArray7652 = new float[i_6 + 1][i_7 + 1];
        this.aFloatArrayArray7648 = new float[i_6 + 1][i_7 + 1];
        this.aFloatArrayArray7645 = new float[i_6 + 1][i_7 + 1];

        int i_8;
        int i_9;
        int i_10;
        int i_11;
        int i_12;
        int i_13;
        for (i_8 = 0; i_8 <= i_7; i_8++) {
            i_9 = i_8 + this.anInt7644;
            if (i_9 > 0 && i_9 < this.aGroundSub27654.length - 1) {
                for (i_10 = 0; i_10 <= i_6; i_10++) {
                    i_11 = i_10 + this.anInt7642;
                    if (i_11 > 0 && i_11 < this.aGroundSub27654.width - 1) {
                        i_12 = class390Sub22.getHeight(i_11 + 1, i_9) - class390Sub22.getHeight(i_11 - 1, i_9);
                        i_13 = class390Sub22.getHeight(i_11, i_9 + 1) - class390Sub22.getHeight(i_11, i_9 - 1);
                        float f_14 = (float) (1.0D / Math.sqrt(i_12 * i_12 + i_13 * i_13 + 65536));
                        this.aFloatArrayArray7652[i_10][i_8] = i_12 * f_14;
                        this.aFloatArrayArray7648[i_10][i_8] = -256.0f * f_14;
                        this.aFloatArrayArray7645[i_10][i_8] = i_13 * f_14;
                    }
                }
            }
        }

        i_8 = 0;

        for (i_9 = this.anInt7644; i_9 <= this.anInt7656; i_9++) {
            if (i_9 >= 0 && i_9 < class390Sub22.length) {
                for (i_10 = this.anInt7642; i_10 <= this.anInt7653; i_10++) {
                    if (i_10 >= 0 && i_10 < class390Sub22.width) {
                        i_11 = ints_4[i_8];
                        int[] ints_19 = class390Sub22.tileColors[i_10][i_9];
                        if (ints_19 != null && i_11 != 0) {
                            if (i_11 == 1) {
                                i_13 = 0;

                                while (i_13 < ints_19.length) {
                                    if (ints_19[i_13++] != -1 && ints_19[i_13++] != -1 && ints_19[i_13++] != -1) {
                                        this.anInt7640 += 3;
                                    }
                                }
                            } else {
                                this.anInt7640 += 3;
                            }
                        }
                    }

                    ++i_8;
                }
            } else {
                i_8 += this.anInt7653 - this.anInt7642;
            }
        }

        if (this.anInt7640 > 0) {
            this.aNodeSub357655 = new Packet(this.anInt7640 * 2);
            this.bitBuffer = new GlBitBuffer(this.anInt7640 * 16);
            this.aClass4537657 = new HashTable(MathUtils.nextPowerOfTwo(this.anInt7640));
            i_9 = 0;
            i_8 = 0;

            for (i_10 = this.anInt7644; i_10 <= this.anInt7656; i_10++) {
                if (i_10 >= 0 && i_10 < class390Sub22.length) {
                    i_11 = 0;

                    for (i_12 = this.anInt7642; i_12 <= this.anInt7653; i_12++) {
                        if (i_12 >= 0 && i_12 < class390Sub22.width) {
                            i_13 = ints_4[i_8];
                            int[] ints_18 = class390Sub22.tileColors[i_12][i_10];
                            if (ints_18 != null && i_13 != 0) {
                                if (i_13 != 1) {
                                    if (i_13 == 3) {
                                        method12306(i_11, i_9, i_12, i_10, 0, 0);
                                        method12306(i_11, i_9, i_12, i_10, class390Sub22.tileUnits, 0);
                                        method12306(i_11, i_9, i_12, i_10, 0, class390Sub22.tileUnits);
                                    } else if (i_13 == 2) {
                                        method12306(i_11, i_9, i_12, i_10, class390Sub22.tileUnits, 0);
                                        method12306(i_11, i_9, i_12, i_10, class390Sub22.tileUnits, class390Sub22.tileUnits);
                                        method12306(i_11, i_9, i_12, i_10, 0, 0);
                                    } else if (i_13 == 5) {
                                        method12306(i_11, i_9, i_12, i_10, class390Sub22.tileUnits, class390Sub22.tileUnits);
                                        method12306(i_11, i_9, i_12, i_10, 0, class390Sub22.tileUnits);
                                        method12306(i_11, i_9, i_12, i_10, class390Sub22.tileUnits, 0);
                                    } else if (i_13 == 4) {
                                        method12306(i_11, i_9, i_12, i_10, 0, class390Sub22.tileUnits);
                                        method12306(i_11, i_9, i_12, i_10, 0, 0);
                                        method12306(i_11, i_9, i_12, i_10, class390Sub22.tileUnits, class390Sub22.tileUnits);
                                    }
                                } else {
                                    int[] ints_15 = class390Sub22.tileOffsetsX[i_12][i_10];
                                    int[] ints_16 = class390Sub22.tileOffsetsZ[i_12][i_10];
                                    int i_17 = 0;

                                    label108:
                                    while (true) {
                                        while (true) {
                                            if (i_17 >= ints_18.length) {
                                                break label108;
                                            }

                                            if (ints_18[i_17] != -1 && ints_18[i_17 + 1] != -1 && ints_18[i_17 + 2] != -1) {
                                                method12306(i_11, i_9, i_12, i_10, ints_15[i_17], ints_16[i_17]);
                                                ++i_17;
                                                method12306(i_11, i_9, i_12, i_10, ints_15[i_17], ints_16[i_17]);
                                                ++i_17;
                                                method12306(i_11, i_9, i_12, i_10, ints_15[i_17], ints_16[i_17]);
                                                ++i_17;
                                            } else {
                                                i_17 += 3;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ++i_8;
                        ++i_11;
                    }
                } else {
                    i_8 += this.anInt7653 - this.anInt7642;
                }

                ++i_9;
            }

            this.anInterface157639 = this.aGraphicalRendererSub17650.createElementsArrayBuffer(this.aNodeSub357655.buffer, this.aNodeSub357655.currentPosition, false);
            this.anInterface147649 = this.aGraphicalRendererSub17650.createArrayBuffer(16, this.bitBuffer.buffer, this.bitBuffer.currentPosition, false);
            this.aClass1437647 = new OpenGlArrayDrawBuffer(this.anInterface147649, 5126, 3, 0);
            this.aClass1437643 = new OpenGlArrayDrawBuffer(this.anInterface147649, 5121, 4, 12);
        } else {
            this.anInterface157639 = null;
            this.anInterface147649 = null;
            this.aClass1437647 = null;
            this.aClass1437643 = null;
        }

        this.bitBuffer = null;
        this.aNodeSub357655 = null;
        this.aClass4537657 = null;
        this.aFloatArrayArray7645 = null;
        this.aFloatArrayArray7648 = null;
        this.aFloatArrayArray7652 = null;
    }

    public void method12305(short s_1) {
        if (this.aGraphicalRendererSub17650.bigEndianess) {
            this.aNodeSub357655.writeShort(s_1);
        } else {
            this.aNodeSub357655.writeShortLittle(s_1);
        }

    }

    public void method12306(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        long long_7 = -1L;
        int i_9 = i_5 + (i_3 << this.aGroundSub27654.tileScale);
        int i_10 = (i_4 << this.aGroundSub27654.tileScale) + i_6;
        int i_11 = this.aGroundSub27654.getAverageHeight(i_9, i_10);
        if ((i_5 & 0x7f) == 0 || (i_6 & 0x7f) == 0) {
            long_7 = (i_10 & 0xffffL) << 16 | i_9 & 0xffffL;
            Node node29 = this.aClass4537657.get(long_7);
            if (node29 != null) {
                method12305(((ShortNode) node29).aShort8067);
                return;
            }
        }

        short s_12 = (short) (this.anInt7646++);
        if (long_7 != -1L) {
            this.aClass4537657.put(new ShortNode(s_12), long_7);
        }

        float f_13;
        float f_14;
        float f_15;
        float f_16;
        float f_17;
        float f_18;
        float f_19;
        float f_20;
        float f_21;
        float f_22;
        float f_23;
        if (i_5 == 0 && i_6 == 0) {
            f_13 = this.aFloatArrayArray7652[i_1][i_2];
            f_14 = this.aFloatArrayArray7648[i_1][i_2];
            f_15 = this.aFloatArrayArray7645[i_1][i_2];
        } else if (i_5 == this.aGroundSub27654.tileUnits && i_6 == 0) {
            f_13 = this.aFloatArrayArray7652[i_1 + 1][i_2];
            f_14 = this.aFloatArrayArray7648[i_1 + 1][i_2];
            f_15 = this.aFloatArrayArray7645[i_1 + 1][i_2];
        } else if (i_5 == this.aGroundSub27654.tileUnits && this.aGroundSub27654.tileUnits == i_6) {
            f_13 = this.aFloatArrayArray7652[i_1 + 1][i_2 + 1];
            f_14 = this.aFloatArrayArray7648[i_1 + 1][i_2 + 1];
            f_15 = this.aFloatArrayArray7645[i_1 + 1][i_2 + 1];
        } else if (i_5 == 0 && this.aGroundSub27654.tileUnits == i_6) {
            f_13 = this.aFloatArrayArray7652[i_1][i_2 + 1];
            f_14 = this.aFloatArrayArray7648[i_1][i_2 + 1];
            f_15 = this.aFloatArrayArray7645[i_1][i_2 + 1];
        } else {
            f_16 = (float) i_5 / this.aGroundSub27654.tileUnits;
            f_17 = (float) i_6 / this.aGroundSub27654.tileUnits;
            f_18 = this.aFloatArrayArray7652[i_1][i_2];
            f_19 = this.aFloatArrayArray7648[i_1][i_2];
            f_20 = this.aFloatArrayArray7645[i_1][i_2];
            f_21 = this.aFloatArrayArray7652[i_1 + 1][i_2];
            f_22 = this.aFloatArrayArray7648[i_1 + 1][i_2];
            f_23 = this.aFloatArrayArray7645[i_1 + 1][i_2];
            f_18 += (this.aFloatArrayArray7652[i_1][i_2 + 1] - f_18) * f_16;
            f_19 += (this.aFloatArrayArray7648[i_1][i_2 + 1] - f_19) * f_16;
            f_20 += (this.aFloatArrayArray7645[i_1][i_2 + 1] - f_20) * f_16;
            f_21 += (this.aFloatArrayArray7652[i_1 + 1][i_2 + 1] - f_21) * f_16;
            f_22 += (this.aFloatArrayArray7648[i_1 + 1][i_2 + 1] - f_22) * f_16;
            f_23 += (this.aFloatArrayArray7645[i_1 + 1][i_2 + 1] - f_23) * f_16;
            f_13 = f_18 + (f_21 - f_18) * f_17;
            f_14 = f_19 + (f_22 - f_19) * f_17;
            f_15 = f_20 + (f_23 - f_20) * f_17;
        }

        f_16 = (this.aNodeSub247641.getX() - i_9);
        f_17 = (this.aNodeSub247641.getY() - i_11);
        f_18 = (this.aNodeSub247641.getZ() - i_10);
        f_19 = (float) Math.sqrt(f_16 * f_16 + f_17 * f_17 + f_18 * f_18);
        f_20 = 1.0F / f_19;
        f_16 *= f_20;
        f_17 *= f_20;
        f_18 *= f_20;
        f_21 = f_19 / this.aNodeSub247641.getSize2d();
        f_22 = 1.0F - f_21 * f_21;
        if (f_22 < 0.0F) {
            f_22 = 0.0F;
        }

        f_23 = f_16 * f_13 + f_17 * f_14 + f_18 * f_15;
        if (f_23 < 0.0F) {
            f_23 = 0.0F;
        }

        float f_24 = f_23 * f_22 * 2.0F;
        if (f_24 > 1.0F) {
            f_24 = 1.0F;
        }

        int i_25 = this.aNodeSub247641.getColor();
        int i_26 = (int) (f_24 * (i_25 >> 16 & 0xff));
        if (i_26 > 255) {
            i_26 = 255;
        }

        int i_27 = (int) (f_24 * (i_25 >> 8 & 0xff));
        if (i_27 > 255) {
            i_27 = 255;
        }

        int i_28 = (int) (f_24 * (i_25 & 0xff));
        if (i_28 > 255) {
            i_28 = 255;
        }

        if (this.aGraphicalRendererSub17650.bigEndianess) {
            this.bitBuffer.writeFloatBigEndian(i_9);
            this.bitBuffer.writeFloatBigEndian(i_11);
            this.bitBuffer.writeFloatBigEndian(i_10);
        } else {
            this.bitBuffer.writeFloatLittleEndian(i_9);
            this.bitBuffer.writeFloatLittleEndian(i_11);
            this.bitBuffer.writeFloatLittleEndian(i_10);
        }

        this.bitBuffer.writeByte(i_26);
        this.bitBuffer.writeByte(i_27);
        this.bitBuffer.writeByte(i_28);
        this.bitBuffer.writeByte(255);
        method12305(s_12);
    }

    public void method12311(int i_1, int i_2, int i_3, boolean[][] bools_4) {
        if (this.anInterface157639 != null && this.anInt7642 <= i_3 + i_1 && this.anInt7653 >= i_1 - i_3 && this.anInt7644 <= i_3 + i_2 && this.anInt7656 >= i_2 - i_3) {
            for (int i_5 = this.anInt7644; i_5 <= this.anInt7656; i_5++) {
                for (int i_6 = this.anInt7642; i_6 <= this.anInt7653; i_6++) {
                    int i_7 = i_6 - i_1;
                    int i_8 = i_5 - i_2;
                    if (i_7 > -i_3 && i_7 < i_3 && i_8 > -i_3 && i_8 < i_3 && bools_4[i_3 + i_7][i_3 + i_8]) {
                        this.aGraphicalRendererSub17650.setTextureEnvColor((int) (this.aNodeSub247641.getAlpha() * 255.0F) << 24);
                        this.aGraphicalRendererSub17650.toggleDrawingClientstates(this.aClass1437647, null, this.aClass1437643, null);
                        this.aGraphicalRendererSub17650.drawElements(this.anInterface157639, 0, this.anInt7640);
                        return;
                    }
                }
            }
        }

    }

}
