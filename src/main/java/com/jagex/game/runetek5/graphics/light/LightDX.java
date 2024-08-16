package com.jagex.game.runetek5.graphics.light;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.cache.node.HashTable;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ShortNode;
import com.jagex.game.runetek5.scene.ground.GroundDX;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.utils.MathUtils;

public class LightDX extends Node<LightDX> {

    public int anInt7530;
    public final AbstractNativeToolkit toolkit;
    public final PointLight lightSource;
    public final GroundDX ground;
    public final int minX;
    public final int maxX;
    public final int minZ;
    public final int maxZ;
    public float[][] heightsXZ;
    public float[][] normalsXZ;
    public float[][] shadowRatioXZ;
    public final NativeStream anInterface47525;
    public final DirectXBuffer anInterface327523;
    public HashTable aClass4537537;
    public int numTriangles;
    
    public void method12175(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, ByteBuffer bytebuffer7, ByteBuffer bytebuffer8) {
        long hash = -1L;
        int x = i_5 + (i_3 << this.ground.tileScale);
        int y = (i_4 << this.ground.tileScale) + i_6;
        int i_13 = this.ground.getAverageHeight(x, y);
        if ((i_5 & 0x7f) == 0 || (i_6 & 0x7f) == 0) {
            hash = (y & 0xffffL) << 16 | x & 0xffffL;
            Node node31 = this.aClass4537537.get(hash);
            if (node31 != null) {
                bytebuffer7.putShort(((ShortNode) node31).aShort8067);
                return;
            }
        }

        short s_14 = (short) (this.anInt7530++);
        if (hash != -1L) {
            this.aClass4537537.put(new ShortNode(s_14), hash);
        }

        float f_15;
        float f_16;
        float f_17;
        float f_18;
        float f_19;
        float f_20;
        float f_21;
        float f_22;
        float f_23;
        float f_24;
        float f_25;
        if (i_5 == 0 && i_6 == 0) {
            f_15 = this.heightsXZ[i_1][i_2];
            f_16 = this.normalsXZ[i_1][i_2];
            f_17 = this.shadowRatioXZ[i_1][i_2];
        } else if (i_5 == this.ground.tileUnits && i_6 == 0) {
            f_15 = this.heightsXZ[i_1 + 1][i_2];
            f_16 = this.normalsXZ[i_1 + 1][i_2];
            f_17 = this.shadowRatioXZ[i_1 + 1][i_2];
        } else if (i_5 == this.ground.tileUnits && this.ground.tileUnits == i_6) {
            f_15 = this.heightsXZ[i_1 + 1][i_2 + 1];
            f_16 = this.normalsXZ[i_1 + 1][i_2 + 1];
            f_17 = this.shadowRatioXZ[i_1 + 1][i_2 + 1];
        } else if (i_5 == 0 && this.ground.tileUnits == i_6) {
            f_15 = this.heightsXZ[i_1][i_2 + 1];
            f_16 = this.normalsXZ[i_1][i_2 + 1];
            f_17 = this.shadowRatioXZ[i_1][i_2 + 1];
        } else {
            f_18 = (float) i_5 / this.ground.tileUnits;
            f_19 = (float) i_6 / this.ground.tileUnits;
            f_20 = this.heightsXZ[i_1][i_2];
            f_21 = this.normalsXZ[i_1][i_2];
            f_22 = this.shadowRatioXZ[i_1][i_2];
            f_23 = this.heightsXZ[i_1 + 1][i_2];
            f_24 = this.normalsXZ[i_1 + 1][i_2];
            f_25 = this.shadowRatioXZ[i_1 + 1][i_2];
            f_20 += (this.heightsXZ[i_1][i_2 + 1] - f_20) * f_18;
            f_21 += (this.normalsXZ[i_1][i_2 + 1] - f_21) * f_18;
            f_22 += (this.shadowRatioXZ[i_1][i_2 + 1] - f_22) * f_18;
            f_23 += (this.heightsXZ[i_1 + 1][i_2 + 1] - f_23) * f_18;
            f_24 += (this.normalsXZ[i_1 + 1][i_2 + 1] - f_24) * f_18;
            f_25 += (this.shadowRatioXZ[i_1 + 1][i_2 + 1] - f_25) * f_18;
            f_15 = f_20 + (f_23 - f_20) * f_19;
            f_16 = f_21 + (f_24 - f_21) * f_19;
            f_17 = f_22 + (f_25 - f_22) * f_19;
        }

        f_18 = (this.lightSource.getX() - x);
        f_19 = (this.lightSource.getY() - i_13);
        f_20 = (this.lightSource.getZ() - y);
        f_21 = (float) Math.sqrt(f_18 * f_18 + f_19 * f_19 + f_20 * f_20);
        f_22 = 1.0F / f_21;
        f_18 *= f_22;
        f_19 *= f_22;
        f_20 *= f_22;
        f_23 = f_21 / this.lightSource.getSize2d();
        f_24 = 1.0F - f_23 * f_23;
        if (f_24 < 0.0F) {
            f_24 = 0.0F;
        }

        f_25 = f_18 * f_15 + f_19 * f_16 + f_20 * f_17;
        if (f_25 < 0.0F) {
            f_25 = 0.0F;
        }

        float f_26 = f_25 * f_24 * 2.0F;
        if (f_26 > 1.0F) {
            f_26 = 1.0F;
        }

        int i_27 = this.lightSource.getColor();
        int i_28 = (int) (f_26 * (i_27 >> 16 & 0xff));
        if (i_28 > 255) {
            i_28 = 255;
        }

        int i_29 = (int) (f_26 * (i_27 >> 8 & 0xff));
        if (i_29 > 255) {
            i_29 = 255;
        }

        int i_30 = (int) (f_26 * (i_27 & 0xff));
        if (i_30 > 255) {
            i_30 = 255;
        }

        bytebuffer8.putFloat(x);
        bytebuffer8.putFloat(i_13);
        bytebuffer8.putFloat(y);
        if (this.toolkit.endiannessType == 0) {
            bytebuffer8.put((byte) i_30);
            bytebuffer8.put((byte) i_29);
            bytebuffer8.put((byte) i_28);
        } else {
            bytebuffer8.put((byte) i_28);
            bytebuffer8.put((byte) i_29);
            bytebuffer8.put((byte) i_30);
        }

        bytebuffer8.put((byte) -1);
        bytebuffer7.putShort(s_14);
    }

    public LightDX(AbstractNativeToolkit class505Sub21, GroundDX class390Sub12, PointLight light, int[] ints_4) {
        this.toolkit = class505Sub21;
        this.lightSource = light;
        this.ground = class390Sub12;
        int size = this.lightSource.getSize2d() - (class390Sub12.tileUnits >> 1);
        this.minX = this.lightSource.getX() - size >> class390Sub12.tileScale;
        this.maxX = this.lightSource.getX() + size >> class390Sub12.tileScale;
        this.minZ = this.lightSource.getZ() - size >> class390Sub12.tileScale;
        this.maxZ = this.lightSource.getZ() + size >> class390Sub12.tileScale;
        int sizeX = this.maxX - this.minX + 1;
        int sizeZ = this.maxZ - this.minZ + 1;
        this.heightsXZ = new float[sizeX + 1][sizeZ + 1];
        this.normalsXZ = new float[sizeX + 1][sizeZ + 1];
        this.shadowRatioXZ = new float[sizeX + 1][sizeZ + 1];


        for (int i_8 = 0; i_8 <= sizeZ; i_8++) {
            int i_9 = i_8 + this.minZ;
            if (i_9 > 0 && i_9 < this.ground.length - 1) {
                for (int i_21 = 0; i_21 <= sizeX; i_21++) {
                    int i_11 = i_21 + this.minX;
                    if (i_11 > 0 && i_11 < this.ground.width - 1) {
                        int i_12 = class390Sub12.getHeight(i_11 + 1, i_9) - class390Sub12.getHeight(i_11 - 1, i_9);
                        int i_13 = class390Sub12.getHeight(i_11, i_9 + 1) - class390Sub12.getHeight(i_11, i_9 - 1);
                        float f_14 = (float) (1.0D / Math.sqrt(i_12 * i_12 + i_13 * i_13 + 65536));
                        this.heightsXZ[i_21][i_8] = i_12 * f_14;
                        this.normalsXZ[i_21][i_8] = -256.0f * f_14;
                        this.shadowRatioXZ[i_21][i_8] = i_13 * f_14;
                    }
                }
            }
        }

        int i_8 = 0;

        for (int z = this.minZ; z <= this.maxZ; z++) {
            if (z >= 0 && z < class390Sub12.length) {
                for (int x = this.minX; x <= this.maxX; x++) {
                    if (x >= 0 && x < class390Sub12.width) {
                        int i_11 = ints_4[i_8];
                        int[] ints_22 = class390Sub12.anIntArrayArrayArray8538[x][z];
                        if (ints_22 != null && i_11 != 0) {
                            if (i_11 == 1) {
                                int i_13 = 0;

                                while (i_13 < ints_22.length) {
                                    if (ints_22[i_13++] != -1 && ints_22[i_13++] != -1 && ints_22[i_13++] != -1) {
                                        this.numTriangles += 3;
                                    }
                                }
                            } else {
                                this.numTriangles += 3;
                            }
                        }
                    }

                    ++i_8;
                }
            } else {
                i_8 += this.maxX - this.minX;
            }
        }

        if (this.numTriangles > 0) {
            this.aClass4537537 = new HashTable(MathUtils.nextPowerOfTwo(this.numTriangles));
            ByteBuffer bytebuffer20 = this.toolkit.directBuffer;
            bytebuffer20.clear();
            bytebuffer20.position(2097152);
            ByteBuffer bytebuffer10 = bytebuffer20.slice();
            bytebuffer20.position(0);
            bytebuffer20.limit(2097152);
            int i_11 = 0;
            i_8 = 0;

            for (int z = this.minZ; z <= this.maxZ; z++) {
                if (z >= 0 && z < class390Sub12.length) {
                    int i_13 = 0;

                    for (int i_23 = this.minX; i_23 <= this.maxX; i_23++) {
                        if (i_23 >= 0 && i_23 < class390Sub12.width) {
                            int i_15 = ints_4[i_8];
                            int[] ints_16 = class390Sub12.anIntArrayArrayArray8538[i_23][z];
                            if (ints_16 != null && i_15 != 0) {
                                if (i_15 != 1) {
                                    if (i_15 == 3) {
                                        method12175(i_13, i_11, i_23, z, 0, 0, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, 0, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, 0, class390Sub12.tileUnits, bytebuffer10, bytebuffer20);
                                    } else if (i_15 == 2) {
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, 0, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, class390Sub12.tileUnits, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, 0, 0, bytebuffer10, bytebuffer20);
                                    } else if (i_15 == 5) {
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, class390Sub12.tileUnits, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, 0, class390Sub12.tileUnits, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, 0, bytebuffer10, bytebuffer20);
                                    } else if (i_15 == 4) {
                                        method12175(i_13, i_11, i_23, z, 0, class390Sub12.tileUnits, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, 0, 0, bytebuffer10, bytebuffer20);
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, class390Sub12.tileUnits, bytebuffer10, bytebuffer20);
                                    }
                                } else {
                                    int[] ints_17 = class390Sub12.anIntArrayArrayArray8540[i_23][z];
                                    int[] ints_18 = class390Sub12.anIntArrayArrayArray8533[i_23][z];
                                    int i_19 = 0;

                                    label108:
                                    while (true) {
                                        while (true) {
                                            if (i_19 >= ints_16.length) {
                                                break label108;
                                            }

                                            if (ints_16[i_19] != -1 && ints_16[i_19 + 1] != -1 && ints_16[i_19 + 2] != -1) {
                                                method12175(i_13, i_11, i_23, z, ints_17[i_19], ints_18[i_19], bytebuffer10, bytebuffer20);
                                                ++i_19;
                                                method12175(i_13, i_11, i_23, z, ints_17[i_19], ints_18[i_19], bytebuffer10, bytebuffer20);
                                                ++i_19;
                                                method12175(i_13, i_11, i_23, z, ints_17[i_19], ints_18[i_19], bytebuffer10, bytebuffer20);
                                                ++i_19;
                                            } else {
                                                i_19 += 3;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ++i_8;
                        ++i_13;
                    }
                } else {
                    i_8 += this.maxX - this.minX;
                }

                ++i_11;
            }

            this.anInterface327523 = this.toolkit.method13993(false);
            this.anInterface327523.create(this.numTriangles);
            this.anInterface47525 = this.toolkit.createStream(false);
            this.anInterface47525.create(this.anInt7530 * 16, 16);
            this.anInterface47525.upload(0, bytebuffer20.position(), this.toolkit.directBufferAddress);
            this.anInterface327523.upload(0, bytebuffer10.position(), this.toolkit.directBufferAddress + 2097152L);
        } else {
            this.anInterface47525 = null;
            this.anInterface327523 = null;
        }

        this.aClass4537537 = null;
        this.shadowRatioXZ = null;
        this.normalsXZ = null;
        this.heightsXZ = null;
    }

}
