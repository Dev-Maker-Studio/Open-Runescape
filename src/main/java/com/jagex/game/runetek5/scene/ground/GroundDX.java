package com.jagex.game.runetek5.scene.ground;

import jaclib.memory.Stream;
import jaclib.memory.heap.NativeHeapBuffer;

import static com.jagex.game.runetek5.scene.tile.TileFunctionMask.*;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.cache.node.HashTable;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ShortNode;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.graphics.light.LightDX;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeEvtWaterMapper;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.graphics.shadow.ShadowDX;
import com.jagex.game.runetek5.graphics.shadow.ShadowMapperDX;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.scene.tile.TileFunctionMask;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.toolkit.directx.DirectXWater;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent;
import com.jagex.game.runetek5.utils.ArrayUtils;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.MathUtils;

public class GroundDX extends Ground {

    public static int[] anIntArray8558 = new int[1];
    public static int[] anIntArray8559 = new int[1];
    public static int[] anIntArray8561 = new int[1];
    public static int anInt8560;
    public int[][][] anIntArrayArrayArray8543;
    public int[][][] anIntArrayArrayArray8532;
    public int anInt8552;
    public int anInt8542;
    public ShadowMapperDX shadowMapper;
    public int[][][] anIntArrayArrayArray8533;
    public int[][][] anIntArrayArrayArray8540;
    public int[][][] anIntArrayArrayArray8538;
    public int[][][] anIntArrayArrayArray8556;
    public DirectXWater[][][] directxWater;
    public byte[][] aByteArrayArray8553;
    public HashTable tileMap;
    public float[][] aFloatArrayArray8549;
    public float[][] aFloatArrayArray8551;
    public float[][] aFloatArrayArray8554;
    public NativeStream anInterface48557;
    public NativeStream anInterface48548;
    public VertexDeclaration aClass708550;
    public Node[] aNodeArray8547;
    public int anInt8527;
    public int anInt8525;
    public float aFloat8535 = Float.MAX_VALUE;
    public float aFloat8544 = -3.4028235E38f;
    public final LinkedList aClass4738546 = new LinkedList();
    public final AbstractNativeToolkit activeToolkit;
    public final int anInt8536;
    public final int anInt8529;
    public final int anInt8530;
    public final int flags;
    public final short[][] tileVertices;
    public final byte[][] groundFlags;

    public GroundDX(AbstractNativeToolkit class505Sub21, int i_2, int i_3, int i_4, int i_5, int[][] tileHeight, int[][] ints_7) {
        super(i_4, i_5, 512, tileHeight);
        this.activeToolkit = class505Sub21;
        this.anInt8536 = super.tileScale - 2;
        this.anInt8529 = 1 << this.anInt8536;
        this.anInt8530 = i_2;
        this.flags = i_3;
        this.anIntArrayArrayArray8532 = new int[i_4][i_5][];
        this.directxWater = new DirectXWater[i_4][i_5][];
        this.anIntArrayArrayArray8540 = new int[i_4][i_5][];
        this.anIntArrayArrayArray8533 = new int[i_4][i_5][];
        this.anIntArrayArrayArray8538 = new int[i_4][i_5][];
        this.anIntArrayArrayArray8556 = new int[i_4][i_5][];
        this.tileVertices = new short[i_5 * i_4][];
        this.groundFlags = new byte[i_4][i_5];
        this.aByteArrayArray8553 = new byte[i_4 + 1][i_5 + 1];
        this.aFloatArrayArray8554 = new float[super.width + 1][super.length + 1];
        this.aFloatArrayArray8551 = new float[super.width + 1][super.length + 1];
        this.aFloatArrayArray8549 = new float[super.width + 1][super.length + 1];

        for (int i_9 = 0; i_9 <= super.length; i_9++) {
            for (int i_10 = 0; i_10 <= super.width; i_10++) {
                int i_11 = super.tileHeights[i_10][i_9];
                if (i_11 < this.aFloat8535) {
                    this.aFloat8535 = i_11;
                }

                if (i_11 > this.aFloat8544) {
                    this.aFloat8544 = i_11;
                }

                if (i_10 > 0 && i_9 > 0 && i_10 < super.width && i_9 < super.length) {
                    int i_12 = ints_7[i_10 + 1][i_9] - ints_7[i_10 - 1][i_9];
                    int i_13 = ints_7[i_10][i_9 + 1] - ints_7[i_10][i_9 - 1];
                    float f_14 = (float) (1.0D / Math.sqrt(512 * 512 * 4 + i_12 * i_12 + i_13 * i_13));
                    this.aFloatArrayArray8554[i_10][i_9] = i_12 * f_14;
                    this.aFloatArrayArray8551[i_10][i_9] = (-512 * 2) * f_14;
                    this.aFloatArrayArray8549[i_10][i_9] = i_13 * f_14;
                }
            }
        }

        --this.aFloat8535;
        ++this.aFloat8544;
        this.tileMap = new HashTable(128);
        if ((this.flags & 0x10) != 0) {
            this.shadowMapper = new ShadowMapperDX(this.activeToolkit, this);
        }

    }

  
    @Override
    public void setTileShadow(int x, int y, int i_3) {
        if ((this.aByteArrayArray8553[x][y] & 0xff) < i_3) {
            this.aByteArrayArray8553[x][y] = (byte) i_3;
        }
    }

    @Override
    public void addBlendedTile(int x, int y, int[] ints_3, int[] ints_4, int[] ints_5, int[] ints_6, int[] ints_7, int[] ints_8, int[] ints_9, int[] waterScales, WaterTile hdWaterTile, boolean flat) {
        MaterialProvider interface2213 = this.activeToolkit.materialProvider;
        if (ints_6 != null && this.anIntArrayArrayArray8543 == null) {
            this.anIntArrayArrayArray8543 = new int[super.width][super.length][];
        }

        if (ints_4 != null && this.anIntArrayArrayArray8532 == null) {
            this.anIntArrayArrayArray8532 = new int[super.width][super.length][];
        }

        this.anIntArrayArrayArray8540[x][y] = ints_3;
        this.anIntArrayArrayArray8533[x][y] = ints_5;
        this.anIntArrayArrayArray8538[x][y] = ints_7;
        this.anIntArrayArrayArray8556[x][y] = ints_8;
        if (this.anIntArrayArrayArray8543 != null) {
            this.anIntArrayArrayArray8543[x][y] = ints_6;
        }

        if (this.anIntArrayArrayArray8532 != null) {
            this.anIntArrayArrayArray8532[x][y] = ints_4;
        }

        DirectXWater[] waterNodes = this.directxWater[x][y] = new DirectXWater[ints_7.length];

        for (int i_15 = 0; i_15 < ints_7.length; i_15++) {
            int i_16 = ints_9[i_15];
            int waterScale = waterScales[i_15];
            if ((this.flags & 0x20) != 0 && i_16 != -1 && interface2213.getMaterial(i_16).detailsOnly) {
                waterScale = 128;
                i_16 = -1;
            }

            long key = (long) hdWaterTile.intensity << 48 | (long) hdWaterTile.scale << 42 | (long) hdWaterTile.color << 28 | ((long) waterScale << 14) | i_16;

            Node node;
            for (node = this.tileMap.get(key); node != null; node = this.tileMap.nextInBucket()) {
                DirectXWater class282Sub621 = (DirectXWater) node;
                if (i_16 == class282Sub621.textureId && class282Sub621.waterScale == waterScale && class282Sub621.hdWaterTile.equals(hdWaterTile)) {
                    break;
                }
            }

            if (node == null) {
                waterNodes[i_15] = new DirectXWater(this, i_16, waterScale, hdWaterTile);
                this.tileMap.put(waterNodes[i_15], key);
            } else {
                waterNodes[i_15] = (DirectXWater) node;
            }
        }

        if (flat) {
            this.groundFlags[x][y] |= COMPLETELY_FLAT;
        }

        if (ints_7.length > this.anInt8552) {
            this.anInt8552 = ints_7.length;
        }

        this.anInt8542 += ints_7.length;
    }



    @Override
    public boolean hasRenderingShadows(Shadow shadow, int i_2, int i_3, int i_4) {
        if (this.shadowMapper != null && shadow != null) {
            int x = i_2 - (i_3 * this.activeToolkit.shadowOffsetX >> 8) >> this.activeToolkit.shadowSizeShift;
            int z = i_4 - (i_3 * this.activeToolkit.shadowOffsetZ >> 8) >> this.activeToolkit.shadowSizeShift;
            return this.shadowMapper.hasShadow(shadow, x, z);
        } 
        return false;
    }



    @Override
    public void renderMinimapFloor(int i_1, int i_2, int i_4, int i_5, int i_6, int i_7, boolean[][] bools_8) {
        if (this.anInt8542 > 0) {
            DirectXBuffer interface329 = this.activeToolkit.method13911(this.anInt8527);
            int i_10 = 0;
            int i_11 = 32767;
            int i_12 = -32768;
            ByteBuffer bytebuffer13 = this.activeToolkit.directBuffer;
            bytebuffer13.clear();

            for (int i_14 = i_5; i_14 < i_7; i_14++) {
                int i_15 = i_14 * super.width + i_4;

                for (int i_16 = i_4; i_16 < i_6; i_16++) {
                    if (bools_8[i_16 - i_4][i_14 - i_5]) {
                        short[] tileVertices = this.tileVertices[i_15];
                        if (tileVertices != null) {
                            for (int i_21 = 0; i_21 < tileVertices.length; i_21++) {
                                int i_19 = tileVertices[i_21] & 0xffff;
                                if (i_19 > i_12) {
                                    i_12 = i_19;
                                }

                                if (i_19 < i_11) {
                                    i_11 = i_19;
                                }

                                bytebuffer13.putShort((short) i_19);
                                ++i_10;
                            }
                        }
                    }

                    ++i_15;
                }
            }

            interface329.upload(0, bytebuffer13.position(), this.activeToolkit.directBufferAddress);
            if (i_10 > 0) {
                this.activeToolkit.method14004();
                AbstractEnvironmentShader shader = this.activeToolkit.environmentShader;
                this.activeToolkit.setStreamSource(0, this.anInterface48548);
                this.activeToolkit.setStreamSource(1, this.anInterface48557);
                this.activeToolkit.setVertexDeclaration(this.aClass708550);
                this.activeToolkit.method13997(interface329);
                this.activeToolkit.setSunPosition(Matrix43.aClass2943518);
                float width = this.activeToolkit.getDisplay().getWidth();
                float height = this.activeToolkit.getDisplay().getHeight();
                Matrix43 matrix44var24 = new Matrix43();
                Matrix43 matrix44var18 = new Matrix43();
                matrix44var24.method5214();
                matrix44var18.setScale(1024 / (256.0F * super.tileUnits), (-1024) / (256.0F * super.tileUnits), 1.0F / (this.aFloat8544 - this.aFloat8535));
                matrix44var18.translate(i_1 - (1024 * i_4) / 256.0F, i_2 + (1024 * i_7) / 256.0F, -this.aFloat8535 / (this.aFloat8544 - this.aFloat8535));
                matrix44var18.scale(2.0F / width, 2.0F / height);
                matrix44var18.translate(-1.0f, -1.0f, 0.0F);
                this.activeToolkit.unsure.multiply(matrix44var24, matrix44var18);
                this.activeToolkit.aClass3848683.fromMatrix(this.activeToolkit.unsure);
                this.activeToolkit.setModelMatrix(this.activeToolkit.aClass3848683);
                shader.loadWorldViewProjection(AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX);
                shader.distanceFogPlane.set(0.0F, 0.0F, 0.0F, 0.0F);
                shader.distanceFogColor.set(0.0F, 0.0F, 0.0F);
                shader.heightFogPlane.set(0.0F, 0.0F, 0.0F, 0.0F);
                shader.heightFogColor.set(0.0F, 0.0F, 0.0F);
                shader.textureSampler = this.activeToolkit.activeTextureSampler;
                shader.textureCoordinateMatrix.identity();
                shader.anInt467 = i_11;
                shader.anInt468 = i_12 - i_11 + 1;
                shader.anInt469 = 0;
                shader.anInt470 = i_10 / 3;
                shader.loadUnlitProgram(false);
            }
        }

    }

    @Override
    public Shadow createShadowAt(int x, int z, Shadow shadow) {
        if ((this.groundFlags[x][z] & COMPLETELY_FLAT) == 0) {
            return null;
        } 
        int size = super.tileUnits >> this.activeToolkit.shadowSizeShift;
        ShadowDX dxShadow = (ShadowDX) shadow;
        ShadowDX adjustedShadow;
        if (dxShadow != null && dxShadow.isClean(size, size)) {
            adjustedShadow = dxShadow;
            dxShadow.clearPixels();
        } else {
            adjustedShadow = new ShadowDX(size, size);
        }

        adjustedShadow.setSize(0, 0, size, size);
        method13791(adjustedShadow, x, z);
        return adjustedShadow;
    }

    public void method13791(ShadowDX class282Sub50Sub17Sub21, int i_2, int i_3) {
        int[] ints_4 = this.anIntArrayArrayArray8540[i_2][i_3];
        int[] ints_5 = this.anIntArrayArrayArray8533[i_2][i_3];
        int i_6 = ints_4.length;
        if (anIntArray8558.length < i_6) {
            anIntArray8558 = new int[i_6];
            anIntArray8559 = new int[i_6];
        }

        int i_7;
        for (i_7 = 0; i_7 < i_6; i_7++) {
            anIntArray8558[i_7] = ints_4[i_7] >> this.activeToolkit.shadowSizeShift;
            anIntArray8559[i_7] = ints_5[i_7] >> this.activeToolkit.shadowSizeShift;
        }

        i_7 = 0;

        while (i_7 < i_6) {
            int i_8 = anIntArray8558[i_7];
            int i_9 = anIntArray8559[i_7++];
            int i_10 = anIntArray8558[i_7];
            int i_11 = anIntArray8559[i_7++];
            int i_12 = anIntArray8558[i_7];
            int i_13 = anIntArray8559[i_7++];
            if ((i_8 - i_10) * (i_11 - i_13) - (i_11 - i_9) * (i_12 - i_10) > 0) {
                class282Sub50Sub17Sub21.calculateShadowTriangle(i_9, i_11, i_13, i_8, i_10, i_12);
            }
        }

    }



    @Override
    public void removeShadow(Shadow class282Sub50Sub171, int i_2, int i_3, int i_4) {
        if (this.shadowMapper != null && class282Sub50Sub171 != null) {
            int i_7 = i_2 - (i_3 * this.activeToolkit.shadowOffsetX >> 8) >> this.activeToolkit.shadowSizeShift;
            int i_8 = i_4 - (i_3 * this.activeToolkit.shadowOffsetZ >> 8) >> this.activeToolkit.shadowSizeShift;
            this.shadowMapper.remove(class282Sub50Sub171, i_7, i_8);
        }

    }

    @Override
    public void resetLight() {
        if (this.anInt8542 > 0) {
            byte[][] bytes_1 = new byte[super.width + 1][super.length + 1];

            int i_3;
            for (int i_2 = 1; i_2 < super.width; i_2++) {
                for (i_3 = 1; i_3 < super.length; i_3++) {
                    bytes_1[i_2][i_3] = (byte) ((this.aByteArrayArray8553[i_2][i_3 - 1] >> 2) + (this.aByteArrayArray8553[i_2][i_3] >> 1) + (this.aByteArrayArray8553[i_2 - 1][i_3] >> 2) + (this.aByteArrayArray8553[i_2][i_3 + 1] >> 3) + (this.aByteArrayArray8553[i_2 + 1][i_3] >> 3));
                }
            }

            Node[] arr_67 = new Node[this.tileMap.nodeCount()];
            this.tileMap.values(arr_67);

            for (i_3 = 0; i_3 < arr_67.length; i_3++) {
                ((DirectXWater) arr_67[i_3]).method12150(this.anInt8542);
            }

            i_3 = 20;
            if (this.anIntArrayArrayArray8543 != null) {
                i_3 += 4;
            }

            if ((this.flags & 0x7) != 0) {
                i_3 += 12;
            }

            NativeHeapBuffer nativeheapbuffer4 = this.activeToolkit.nativeHeap.createBuffer(this.anInt8542 * 4, false);
            NativeHeapBuffer nativeheapbuffer5 = this.activeToolkit.nativeHeap.createBuffer(i_3 * this.anInt8542, false);
            Stream stream6 = new Stream(nativeheapbuffer5);
            Stream stream7 = new Stream(nativeheapbuffer4);
            DirectXWater[] arr_8 = new DirectXWater[this.anInt8542];
            int i_9 = MathUtils.furtestBitValue(this.anInt8542 / 4);
            if (i_9 < 1) {
                i_9 = 1;
            }

            HashTable class45310 = new HashTable(i_9);
            DirectXWater[] arr_11 = new DirectXWater[this.anInt8552];

            int i_12;
            int i_13;
            for (i_12 = 0; i_12 < super.width; i_12++) {
                for (i_13 = 0; i_13 < super.length; i_13++) {
                    if (this.anIntArrayArrayArray8538[i_12][i_13] != null) {
                        DirectXWater[] arr_14 = this.directxWater[i_12][i_13];
                        int[] ints_70 = this.anIntArrayArrayArray8540[i_12][i_13];
                        int[] ints_71 = this.anIntArrayArrayArray8533[i_12][i_13];
                        int[] ints_17 = this.anIntArrayArrayArray8556[i_12][i_13];
                        int[] ints_18 = this.anIntArrayArrayArray8538[i_12][i_13];
                        int[] ints_19 = this.anIntArrayArrayArray8532 != null ? this.anIntArrayArrayArray8532[i_12][i_13] : null;
                        int[] ints_20 = this.anIntArrayArrayArray8543 != null ? this.anIntArrayArrayArray8543[i_12][i_13] : null;
                        if (ints_17 == null) {
                            ints_17 = ints_18;
                        }

                        float f_21 = this.aFloatArrayArray8554[i_12][i_13];
                        float f_22 = this.aFloatArrayArray8551[i_12][i_13];
                        float f_23 = this.aFloatArrayArray8549[i_12][i_13];
                        float f_24 = this.aFloatArrayArray8554[i_12][i_13 + 1];
                        float f_25 = this.aFloatArrayArray8551[i_12][i_13 + 1];
                        float f_26 = this.aFloatArrayArray8549[i_12][i_13 + 1];
                        float f_27 = this.aFloatArrayArray8554[i_12 + 1][i_13 + 1];
                        float f_28 = this.aFloatArrayArray8551[i_12 + 1][i_13 + 1];
                        float f_29 = this.aFloatArrayArray8549[i_12 + 1][i_13 + 1];
                        float f_30 = this.aFloatArrayArray8554[i_12 + 1][i_13];
                        float f_31 = this.aFloatArrayArray8551[i_12 + 1][i_13];
                        float f_32 = this.aFloatArrayArray8549[i_12 + 1][i_13];
                        int i_33 = bytes_1[i_12][i_13] & 0xff;
                        int i_34 = bytes_1[i_12][i_13 + 1] & 0xff;
                        int i_35 = bytes_1[i_12 + 1][i_13 + 1] & 0xff;
                        int i_36 = bytes_1[i_12 + 1][i_13] & 0xff;
                        int i_37 = 0;

                        int i_40;
                        label459:
                        for (int i_38 = 0; i_38 < ints_18.length; i_38++) {
                            DirectXWater class282Sub673 = arr_14[i_38];

                            for (i_40 = 0; i_40 < i_37; i_40++) {
                                if (class282Sub673 == arr_11[i_40]) {
                                    continue label459;
                                }
                            }

                            arr_11[i_37++] = class282Sub673;
                        }

                        short[] shorts_72 = this.tileVertices[i_12 + i_13 * super.width] = new short[ints_18.length];

                        for (int i_39 = 0; i_39 < ints_18.length; i_39++) {
                            i_40 = (i_12 << super.tileScale) + ints_70[i_39];
                            int i_41 = (i_13 << super.tileScale) + ints_71[i_39];
                            int i_42 = i_40 >> this.anInt8536;
                            int i_43 = i_41 >> this.anInt8536;
                            int i_44 = ints_18[i_39];
                            int i_45 = ints_17[i_39];
                            int i_46 = ints_19 != null ? ints_19[i_39] : 0;
                            long long_47 = (long) i_45 << 48 | (long) i_44 << 32 | (i_42 << 16) | i_43;
                            int i_49 = ints_70[i_39];
                            int i_50 = ints_71[i_39];
                            byte b_51 = 74;
                            int i_52 = 0;
                            float f_53 = 1.0F;
                            float f_54;
                            float f_55;
                            float f_56;
                            float f_61;
                            int i_84;
                            if (i_49 == 0 && i_50 == 0) {
                                f_54 = f_21;
                                f_55 = f_22;
                                f_56 = f_23;
                                i_84 = b_51 - i_33;
                            } else if (i_49 == 0 && i_50 == super.tileUnits) {
                                f_54 = f_24;
                                f_55 = f_25;
                                f_56 = f_26;
                                i_84 = b_51 - i_34;
                            } else if (i_49 == super.tileUnits && i_50 == super.tileUnits) {
                                f_54 = f_27;
                                f_55 = f_28;
                                f_56 = f_29;
                                i_84 = b_51 - i_35;
                            } else if (i_49 == super.tileUnits && i_50 == 0) {
                                f_54 = f_30;
                                f_55 = f_31;
                                f_56 = f_32;
                                i_84 = b_51 - i_36;
                            } else {
                                float f_57 = (float) i_49 / super.tileUnits;
                                float f_58 = (float) i_50 / super.tileUnits;
                                float f_59 = f_21 + (f_30 - f_21) * f_57;
                                float f_60 = f_22 + (f_31 - f_22) * f_57;
                                f_61 = f_23 + (f_32 - f_23) * f_57;
                                float f_62 = f_24 + (f_27 - f_24) * f_57;
                                float f_63 = f_25 + (f_28 - f_25) * f_57;
                                float f_64 = f_26 + (f_29 - f_26) * f_57;
                                f_54 = f_59 + (f_62 - f_59) * f_58;
                                f_55 = f_60 + (f_63 - f_60) * f_58;
                                f_56 = f_61 + (f_64 - f_61) * f_58;
                                int i_65 = i_33 + (i_49 * (i_36 - i_33) >> super.tileScale);
                                int i_66 = i_34 + (i_49 * (i_35 - i_34) >> super.tileScale);
                                i_84 = b_51 - (i_65 + (i_50 * (i_66 - i_65) >> super.tileScale));
                            }

                            if (i_44 != -1) {
                                int i_85 = i_84 * (i_44 & 0x7f) >> 7;
                                if (i_85 < 2) {
                                    i_85 = 2;
                                } else if (i_85 > 126) {
                                    i_85 = 126;
                                }

                                i_52 = ColorUtil.hslToRgb[i_44 & 0xff80 | i_85];
                                if ((this.flags & 0x7) == 0) {
                                    f_53 = this.activeToolkit.sunPosition[0] * f_54 + this.activeToolkit.sunPosition[1] * f_55 + this.activeToolkit.sunPosition[2] * f_56;
                                    f_53 = this.activeToolkit.lightIntensity + f_53 * (f_53 > 0.0F ? this.activeToolkit.sunColorIntensity : this.activeToolkit.antiSunColorIntensity);
                                }
                            }

                            Node node80 = null;
                            if ((i_40 & this.anInt8529 - 1) == 0 && (i_41 & this.anInt8529 - 1) == 0) {
                                node80 = class45310.get(long_47);
                            }

                            int i_86;
                            int i_87;
                            if (node80 == null) {
                                if (i_45 != i_44) {
                                    int i_88 = i_84 * (i_45 & 0x7f) >> 7;
                                    if (i_88 < 2) {
                                        i_88 = 2;
                                    } else if (i_88 > 126) {
                                        i_88 = 126;
                                    }

                                    i_87 = ColorUtil.hslToRgb[i_45 & 0xff80 | i_88];
                                    if ((this.flags & 0x7) == 0) {
                                        float f_10000 = this.activeToolkit.sunPosition[0] * f_54 + this.activeToolkit.sunPosition[1] * f_55 + this.activeToolkit.sunPosition[2] * f_56;
                                        f_61 = this.activeToolkit.lightIntensity + f_53 * (f_53 > 0.0F ? this.activeToolkit.sunColorIntensity : this.activeToolkit.antiSunColorIntensity);
                                        int i_89 = i_87 >> 16 & 0xff;
                                        int i_90 = i_87 >> 8 & 0xff;
                                        int i_91 = i_87 & 0xff;
                                        i_89 = (int) (i_89 * f_61);
                                        if (i_89 < 0) {
                                            i_89 = 0;
                                        } else if (i_89 > 255) {
                                            i_89 = 255;
                                        }

                                        i_90 = (int) (i_90 * f_61);
                                        if (i_90 < 0) {
                                            i_90 = 0;
                                        } else if (i_90 > 255) {
                                            i_90 = 255;
                                        }

                                        i_91 = (int) (i_91 * f_61);
                                        if (i_91 < 0) {
                                            i_91 = 0;
                                        } else if (i_91 > 255) {
                                            i_91 = 255;
                                        }

                                        i_87 = i_89 << 16 | i_90 << 8 | i_91;
                                    }
                                } else {
                                    i_87 = i_52;
                                }

                                if (Stream.bigEndianessEnabled()) {
                                    stream6.putFloatBigEndian(i_40);
                                    stream6.putFloatBigEndian((getAverageHeight(i_40, i_41) + i_46));
                                    stream6.putFloatBigEndian(i_41);
                                    stream6.putFloatBigEndian(i_40);
                                    stream6.putFloatBigEndian(i_41);
                                    if (this.anIntArrayArrayArray8543 != null) {
                                        stream6.putFloatBigEndian(ints_20 != null ? (ints_20[i_39] - 1) : 0.0F);
                                    }

                                    if ((this.flags & 0x7) != 0) {
                                        stream6.putFloatBigEndian(f_54);
                                        stream6.putFloatBigEndian(f_55);
                                        stream6.putFloatBigEndian(f_56);
                                    }
                                } else {
                                    stream6.putFloatLittleEndian(i_40);
                                    stream6.putFloatLittleEndian((getAverageHeight(i_40, i_41) + i_46));
                                    stream6.putFloatLittleEndian(i_41);
                                    stream6.putFloatLittleEndian(i_40);
                                    stream6.putFloatLittleEndian(i_41);
                                    if (this.anIntArrayArrayArray8543 != null) {
                                        stream6.putFloatLittleEndian(ints_20 != null ? (ints_20[i_39] - 1) : 0.0F);
                                    }

                                    if ((this.flags & 0x7) != 0) {
                                        stream6.putFloatLittleEndian(f_54);
                                        stream6.putFloatLittleEndian(f_55);
                                        stream6.putFloatLittleEndian(f_56);
                                    }
                                }

                                if (this.activeToolkit.endiannessType == 0) {
                                    stream7.method2921(~0xffffff | i_87);
                                } else {
                                    stream7.method2922(~0xffffff | i_87);
                                }

                                i_86 = this.anInt8525++;
                                shorts_72[i_39] = (short) i_86;
                                if (i_44 != -1) {
                                    arr_8[i_86] = arr_14[i_39];
                                }

                                class45310.put(new ShortNode(shorts_72[i_39]), long_47);
                            } else {
                                shorts_72[i_39] = ((ShortNode) node80).aShort8067;
                                i_86 = shorts_72[i_39] & 0xffff;
                                if (i_44 != -1 && arr_14[i_39].pointer < arr_8[i_86].pointer) {
                                    arr_8[i_86] = arr_14[i_39];
                                }
                            }

                            for (i_87 = 0; i_87 < i_37; i_87++) {
                                arr_11[i_87].method12145(i_86, i_52, i_84, f_53);
                            }

                            ++this.anInt8527;
                        }
                    }
                }
            }

            for (i_12 = 0; i_12 < this.anInt8525; i_12++) {
                DirectXWater class282Sub668 = arr_8[i_12];
                if (class282Sub668 != null) {
                    class282Sub668.method12143(i_12);
                }
            }

            for (i_12 = 0; i_12 < super.width; i_12++) {
                for (i_13 = 0; i_13 < super.length; i_13++) {
                    short[] shorts_81 = this.tileVertices[i_12 + i_13 * super.width];
                    if (shorts_81 != null) {
                        int i_15 = 0;

                        for (int i_16 = 0; i_16 < shorts_81.length; i_15++) {
                            int i_74 = shorts_81[i_16++] & 0xffff;
                            int i_75 = shorts_81[i_16++] & 0xffff;
                            int i_76 = shorts_81[i_16++] & 0xffff;
                            DirectXWater class282Sub683 = arr_8[i_74];
                            DirectXWater class282Sub677 = arr_8[i_75];
                            DirectXWater class282Sub678 = arr_8[i_76];
                            DirectXWater class282Sub679 = null;
                            if (class282Sub683 != null) {
                                class282Sub683.method12152(i_12, i_13, i_15);
                                class282Sub679 = class282Sub683;
                            }

                            if (class282Sub677 != null) {
                                class282Sub677.method12152(i_12, i_13, i_15);
                                if (class282Sub679 == null || class282Sub677.pointer < class282Sub679.pointer) {
                                    class282Sub679 = class282Sub677;
                                }
                            }

                            if (class282Sub678 != null) {
                                class282Sub678.method12152(i_12, i_13, i_15);
                                if (class282Sub679 == null || class282Sub678.pointer < class282Sub679.pointer) {
                                    class282Sub679 = class282Sub678;
                                }
                            }

                            if (class282Sub679 != null) {
                                if (class282Sub683 != null) {
                                    class282Sub679.method12143(i_74);
                                }

                                if (class282Sub677 != null) {
                                    class282Sub679.method12143(i_75);
                                }

                                if (class282Sub678 != null) {
                                    class282Sub679.method12143(i_76);
                                }

                                class282Sub679.method12152(i_12, i_13, i_15);
                            }
                        }
                    }
                }
            }

            stream6.method2925();
            stream7.method2925();
            this.anInterface48557 = this.activeToolkit.createStream(false);
            this.anInterface48557.createFromSource(this.anInt8525 * 4, 4, nativeheapbuffer4);
            this.anInterface48548 = this.activeToolkit.createStream(false);
            this.anInterface48548.createFromSource(i_3 * this.anInt8525, i_3, nativeheapbuffer5);
            if ((this.flags & 0x7) != 0) {
                if (this.anIntArrayArrayArray8543 != null) {
                    this.aClass708550 = this.activeToolkit.method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69692, VertexDeclarationAction.aClass69691, VertexDeclarationAction.NORMAL_POINTER_ACTION}), new VertexDeclarationComponent(VertexDeclarationAction.aClass69690)});
                } else {
                    this.aClass708550 = this.activeToolkit.method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69692, VertexDeclarationAction.NORMAL_POINTER_ACTION}), new VertexDeclarationComponent(VertexDeclarationAction.aClass69690)});
                }
            } else if (this.anIntArrayArrayArray8543 != null) {
                this.aClass708550 = this.activeToolkit.method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69692, VertexDeclarationAction.aClass69691}), new VertexDeclarationComponent(VertexDeclarationAction.aClass69690)});
            } else {
                this.aClass708550 = this.activeToolkit.method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69692}), new VertexDeclarationComponent(VertexDeclarationAction.aClass69690)});
            }

            i_12 = 0;

            for (i_13 = 0; i_13 < arr_67.length; i_13++) {
                DirectXWater class282Sub682 = (DirectXWater) arr_67[i_13];
                if (class282Sub682.anInt7508 > 0) {
                    arr_67[i_12++] = class282Sub682;
                }
            }

            this.aNodeArray8547 = new Node[i_12];
            long[] longs_92 = new long[i_12];

            for (int i_69 = 0; i_69 < i_12; i_69++) {
                DirectXWater class282Sub693 = (DirectXWater) arr_67[i_69];
                longs_92[i_69] = class282Sub693.pointer;
                this.aNodeArray8547[i_69] = class282Sub693;
                class282Sub693.method12146(this.anInt8525);
            }

            ArrayUtils.quicksort(longs_92, this.aNodeArray8547);
            if (this.shadowMapper != null) {
                this.shadowMapper.method1312();
            }
        } else {
            this.shadowMapper = null;
        }

        if ((this.anInt8530 & 0x2) == 0) {
            this.anIntArrayArrayArray8533 = null;
            this.anIntArrayArrayArray8540 = null;
            this.anIntArrayArrayArray8538 = null;
        }

        this.anIntArrayArrayArray8543 = null;
        this.anIntArrayArrayArray8556 = null;
        this.anIntArrayArrayArray8532 = null;
        this.directxWater = null;
        this.aByteArrayArray8553 = null;
        this.tileMap = null;
        this.aFloatArrayArray8549 = null;
        this.aFloatArrayArray8551 = null;
        this.aFloatArrayArray8554 = null;
    }

    @Override
    public void renderFloor(int i_1, int i_2, int i_3, boolean[][] bools_4, boolean bool5) {
        if (this.aNodeArray8547 != null) {
            int i_7 = i_3 + i_3 + 1;
            i_7 *= i_7;
            if (anIntArray8561.length < i_7) {
                anIntArray8561 = new int[i_7];
            }

            int i_8 = i_1 - i_3;
            int i_9 = i_8;
            if (i_8 < 0) {
                i_8 = 0;
            }

            int i_10 = i_2 - i_3;
            int i_11 = i_10;
            if (i_10 < 0) {
                i_10 = 0;
            }

            int i_12 = i_3 + i_1;
            if (i_12 > super.width - 1) {
                i_12 = super.width - 1;
            }

            int i_13 = i_3 + i_2;
            if (i_13 > super.length - 1) {
                i_13 = super.length - 1;
            }

            anInt8560 = 0;

            for (int i_14 = i_8; i_14 <= i_12; i_14++) {
                boolean[] bools_25 = bools_4[i_14 - i_9];

                for (int i_26 = i_10; i_26 <= i_13; i_26++) {
                    if (bools_25[i_26 - i_11]) {
                        anIntArray8561[anInt8560++] = i_14 + i_26 * super.width;
                    }
                }
            }

            ByteBuffer bytebuffer24 = this.activeToolkit.directBuffer;
            bytebuffer24.clear();

            int i_15;
            for (i_15 = 0; i_15 < this.aNodeArray8547.length; i_15++) {
                DirectXWater class282Sub616 = (DirectXWater) this.aNodeArray8547[i_15];
                class282Sub616.method12147(anIntArray8561, anInt8560);
            }

            i_15 = bytebuffer24.position();
            AbstractEnvironmentShader envShader = this.activeToolkit.environmentShader;
            if (i_15 != 0) {
                DirectXBuffer interface3217 = this.activeToolkit.method13911(i_15 / 2);
                interface3217.upload(0, i_15, this.activeToolkit.directBufferAddress);
                this.activeToolkit.method14004();
                this.activeToolkit.setStreamSource(0, this.anInterface48548);
                this.activeToolkit.method13997(interface3217);
                envShader.loadWorldViewProjection(AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX);
                if (this.activeToolkit.fogDistance > 0) {
                    envShader.distanceFogPlane.set(0.0F, 0.0F, 1.0F, -this.activeToolkit.fogStart);
                    envShader.distanceFogColor.set((this.activeToolkit.fogColorRgb >> 16 & 0xff) / 255.0F, (this.activeToolkit.fogColorRgb >> 8 & 0xff) / 255.0F, (this.activeToolkit.fogColorRgb & 0xff) / 255.0F);
                    this.activeToolkit.aClass3848683.copyFrom(this.activeToolkit.particleMatrix);
                    this.activeToolkit.aClass3848683.rotate();
                    envShader.distanceFogPlane.concat(this.activeToolkit.aClass3848683);
                    envShader.distanceFogPlane.scale(1.0F / (this.activeToolkit.fogEnd - this.activeToolkit.fogStart));
                } else {
                    envShader.distanceFogPlane.set(0.0F, 0.0F, 0.0F, 0.0F);
                    envShader.distanceFogColor.set(0.0F, 0.0F, 0.0F);
                }

                int i_18;
                int i_19;
                DirectXWater directxWater;
                MaterialType waterMaterial;
                if ((this.flags & 0x37) == 0) {
                    i_18 = 0;

                    for (i_19 = 0; i_19 < this.aNodeArray8547.length; i_19++) {
                        directxWater = (DirectXWater) this.aNodeArray8547[i_19];
                        if (directxWater.anInt7514 != 0) {
                            if (this.activeToolkit.missingWaterColor) {
                                this.activeToolkit.addWaterTile(0, directxWater.hdWaterTile);
                                envShader.heightFogPlane.set(0.0F, 1.0F, 0.0F, this.activeToolkit.renderedWaterColor + directxWater.hdWaterTile.intensity / 255.0F * directxWater.hdWaterTile.scale);
                                envShader.heightFogPlane.scale(1.0F / directxWater.hdWaterTile.scale);
                                envShader.heightFogColor.set((directxWater.hdWaterTile.color >> 16 & 0xff) / 255.0F, (directxWater.hdWaterTile.color >> 8 & 0xff) / 255.0F, (directxWater.hdWaterTile.color & 0xff) / 255.0F);
                            } else {
                                envShader.heightFogPlane.set(0.0F, 0.0F, 0.0F, 0.0F);
                                envShader.heightFogColor.set(0.0F, 0.0F, 0.0F);
                            }

                            boolean bool30 = false;
                            if (directxWater.textureId != -1) {
                                envShader.textureSampler = this.activeToolkit.materialCache.getTextureSampler(directxWater.textureId);
                                waterMaterial = this.activeToolkit.materialProvider.getMaterial(directxWater.textureId);
                                bool30 = !MaterialTypeList.isOpaqueEffect(waterMaterial.effectId);
                            } else {
                                envShader.textureSampler = this.activeToolkit.activeTextureSampler;
                            }

                            this.activeToolkit.setStreamSource(1, directxWater.directxWaterBuffer);
                            this.activeToolkit.setVertexDeclaration(this.aClass708550);
                            envShader.textureCoordinateMatrix.method6525(1.0F / directxWater.waterScale, 1.0F / directxWater.waterScale, 1.0F, 1.0F);
                            envShader.anInt467 = directxWater.anInt7519;
                            envShader.anInt468 = directxWater.anInt7517 - directxWater.anInt7519 + 1;
                            envShader.anInt469 = i_18;
                            envShader.anInt470 = directxWater.anInt7514 / 3;
                            envShader.loadUnlitProgram(bool30);
                            i_18 += directxWater.anInt7514;
                        }
                    }
                } else {
                    envShader.sunPosition.set(this.activeToolkit.sunPosition[0], this.activeToolkit.sunPosition[1], this.activeToolkit.sunPosition[2]);
                    envShader.sunColor.set(this.activeToolkit.sunColorIntensity * this.activeToolkit.baseLightColorRed, this.activeToolkit.sunColorIntensity * this.activeToolkit.baseLightColorGreen, this.activeToolkit.sunColorIntensity * this.activeToolkit.baseLightColorBlue);
                    envShader.antiSunColor.set(-this.activeToolkit.antiSunColorIntensity * this.activeToolkit.baseLightColorRed, -this.activeToolkit.antiSunColorIntensity * this.activeToolkit.baseLightColorGreen, -this.activeToolkit.antiSunColorIntensity * this.activeToolkit.baseLightColorBlue);
                    envShader.lightColor.set(this.activeToolkit.lightIntensity * this.activeToolkit.baseLightColorRed, this.activeToolkit.lightIntensity * this.activeToolkit.baseLightColorGreen, this.activeToolkit.lightIntensity * this.activeToolkit.baseLightColorBlue);
                    i_18 = 0;

                    for (i_19 = 0; i_19 < this.aNodeArray8547.length; i_19++) {
                        directxWater = (DirectXWater) this.aNodeArray8547[i_19];
                        if (directxWater.anInt7514 > 0) {
                            if (this.activeToolkit.missingWaterColor) {
                                this.activeToolkit.addWaterTile(0, directxWater.hdWaterTile);
                                float f_21 = 0.15F;
                                envShader.heightFogPlane.set(0.0F, 1.0F / (directxWater.hdWaterTile.scale * f_21), 0.0F, 256.0F / (directxWater.hdWaterTile.scale * f_21));
                                envShader.heightFogColor.set((directxWater.hdWaterTile.color >> 16 & 0xff) / 255.0F, (directxWater.hdWaterTile.color >> 8 & 0xff) / 255.0F, (directxWater.hdWaterTile.color & 0xff) / 255.0F);
                            } else {
                                envShader.heightFogPlane.set(0.0F, 0.0F, 0.0F, 0.0F);
                                envShader.heightFogColor.set(0.0F, 0.0F, 0.0F);
                            }

                            byte b_29 = 11;
                            if (directxWater.textureId != -1) {
                                waterMaterial = this.activeToolkit.materialProvider.getMaterial(directxWater.textureId);
                                b_29 = waterMaterial.effectId;
                                envShader.textureSampler = this.activeToolkit.materialCache.getTextureSampler(directxWater.textureId);
                                envShader.indentifyMaterialEffect(waterMaterial);
                            } else {
                                envShader.textureSampler = this.activeToolkit.activeTextureSampler;
                            }

                            this.activeToolkit.setStreamSource(1, directxWater.directxWaterBuffer);
                            this.activeToolkit.setVertexDeclaration(this.aClass708550);
                            envShader.textureCoordinateMatrix.method6525(1.0F / directxWater.waterScale, 1.0F / directxWater.waterScale, 1.0F, 1.0F);
                            envShader.anInt467 = directxWater.anInt7519;
                            envShader.anInt468 = directxWater.anInt7517 - directxWater.anInt7519 + 1;
                            envShader.anInt469 = i_18;
                            envShader.anInt470 = directxWater.anInt7514 / 3;
                            switch (b_29) {
                                case 1:
                                    envShader.eyePos.set(this.activeToolkit.aClass3848814.faceList[12], this.activeToolkit.aClass3848814.faceList[13], this.activeToolkit.aClass3848814.faceList[14]);
                                    envShader.loadSpecularPointlight(0);
                                    break;
                                case 2:
                                case 4:
                                case 8:
                                case 9:
                                    // render shaders/ripples water
                                    if (!this.activeToolkit.enableLights && (this.flags & TileFunctionMask.HD_WATER) != 0) {
                                        NativeEvtWaterMapper evtWaterMapper = this.activeToolkit.nativeEvtWaterMapper;
                                        evtWaterMapper.waterViewingPlaneMatrix.copyFrom(this.activeToolkit.aClass3848729);
                                        evtWaterMapper.textureCoordinateMatrix.method6525(1.0F / (directxWater.waterScale * directxWater.hdWaterTile.floOpcode20), 1.0F / (directxWater.waterScale * directxWater.hdWaterTile.floOpcode20), 1.0F, 1.0F);
                                        evtWaterMapper.eye.set(this.activeToolkit.aClass3848814.faceList[12], this.activeToolkit.aClass3848814.faceList[13], this.activeToolkit.aClass3848814.faceList[14]);
                                        MaterialType class16923 = this.activeToolkit.materialProvider.getMaterial(directxWater.textureId);
                                        evtWaterMapper.effectParam1 = class16923.effectParam1;
                                        evtWaterMapper.anInt10110 = directxWater.anInt7519;
                                        evtWaterMapper.anInt10111 = directxWater.anInt7517 - directxWater.anInt7519 + 1;
                                        evtWaterMapper.anInt10094 = i_18;
                                        evtWaterMapper.anInt10112 = directxWater.anInt7514 / 3;
                                        evtWaterMapper.heightFogPlaneVector.copy(envShader.heightFogPlane);
                                        evtWaterMapper.heightFogColorVector.copy(envShader.heightFogColor);
                                        evtWaterMapper.distanceFogPlaneVector.copy(envShader.distanceFogPlane);
                                        evtWaterMapper.distanceFogColorVector.copy(envShader.distanceFogColor);
                                        evtWaterMapper.method15520();
                                    } else {
                                        envShader.loadStandartPointlight(0);
                                    }
                                    break;
                                case 3:
                                case 5:
                                default:
                                    if (this.activeToolkit.missingWaterColor) {
                                        envShader.loadUnderwaterGround();
                                    } else {
                                        envShader.loadStandartPointlight(0);
                                    }
                                    break;
                                case 6:
                                    envShader.loadUnlitProgram(!MaterialTypeList.isOpaqueEffect(b_29));
                                    break;
                                case 7:
                                    envShader.eyePos.set(this.activeToolkit.aClass3848814.faceList[12], this.activeToolkit.aClass3848814.faceList[13], this.activeToolkit.aClass3848814.faceList[14]);
                                    envShader.aClass384465.identity();
                                    envShader.anInterface31451 = this.activeToolkit.getEnvironmentMapSampler();
                                    envShader.loadPointlightEnvironmentMapping(0);
                            }

                            i_18 += directxWater.anInt7514;
                        }
                    }
                }
            }

            if (this.shadowMapper != null) {
                this.activeToolkit.setStreamSource(0, this.anInterface48548);
                this.activeToolkit.setStreamSource(1, this.anInterface48557);
                this.activeToolkit.setVertexDeclaration(this.aClass708550);
                Matrix44 matrix4428 = this.activeToolkit.aClass3848683;
                matrix4428.identity();
                matrix4428.faceList[13] = -1.0f;
                envShader.loadWorldViewProjection(matrix4428);
                this.shadowMapper.method1314(envShader, i_1, i_2, i_3, bools_4, bool5);
            }
        }

    }


    @Override
    public void calculateBlockTiles(int x, int y, int[] ints_3, int[] ints_4, int[] ints_5, int[] ints_6, int[] ints_7, int[] ints_8, int[] ints_9, int[] ints_10, int[] ints_11, int[] ints_12, int[] ints_13, WaterTile hdWaterTile) {
        int i_16 = ints_10.length;
        int[] ints_17 = new int[i_16 * 3];
        int[] ints_18 = new int[i_16 * 3];
        int[] ints_19 = new int[i_16 * 3];
        int[] ints_20 = new int[i_16 * 3];
        int[] ints_21 = new int[i_16 * 3];
        int[] ints_22 = new int[i_16 * 3];
        int[] ints_23 = ints_4 != null ? new int[i_16 * 3] : null;
        int[] ints_24 = ints_6 != null ? new int[i_16 * 3] : null;
        int i_25 = 0;

        for (int i_26 = 0; i_26 < i_16; i_26++) {
            int i_27 = ints_7[i_26];
            int i_28 = ints_8[i_26];
            int i_29 = ints_9[i_26];
            ints_17[i_25] = ints_3[i_27];
            ints_18[i_25] = ints_5[i_27];
            ints_19[i_25] = ints_10[i_26];
            ints_21[i_25] = ints_12[i_26];
            ints_22[i_25] = ints_13[i_26];
            ints_20[i_25] = ints_11 != null ? ints_11[i_26] : ints_10[i_26];
            if (ints_4 != null) {
                ints_23[i_25] = ints_4[i_27];
            }

            if (ints_6 != null) {
                ints_24[i_25] = ints_6[i_27];
            }

            ++i_25;
            ints_17[i_25] = ints_3[i_28];
            ints_18[i_25] = ints_5[i_28];
            ints_19[i_25] = ints_10[i_26];
            ints_21[i_25] = ints_12[i_26];
            ints_22[i_25] = ints_13[i_26];
            ints_20[i_25] = ints_11 != null ? ints_11[i_26] : ints_10[i_26];
            if (ints_4 != null) {
                ints_23[i_25] = ints_4[i_28];
            }

            if (ints_6 != null) {
                ints_24[i_25] = ints_6[i_28];
            }

            ++i_25;
            ints_17[i_25] = ints_3[i_29];
            ints_18[i_25] = ints_5[i_29];
            ints_19[i_25] = ints_10[i_26];
            ints_21[i_25] = ints_12[i_26];
            ints_22[i_25] = ints_13[i_26];
            ints_20[i_25] = ints_11 != null ? ints_11[i_26] : ints_10[i_26];
            if (ints_4 != null) {
                ints_23[i_25] = ints_4[i_29];
            }

            if (ints_6 != null) {
                ints_24[i_25] = ints_6[i_29];
            }

            ++i_25;
        }

        addBlendedTile(x, y, ints_17, ints_23, ints_18, ints_24, ints_19, ints_20, ints_21, ints_22, hdWaterTile, false);
    }

    @Override
    public void addLight(PointLight class282Sub241, int[] ints_2) {
        this.aClass4738546.insertTail(new LightDX(this.activeToolkit, this, class282Sub241, ints_2));
    }

    @Override
    public void addShadow(Shadow shadow, int i_2, int i_3, int i_4) {
        if (this.shadowMapper != null && shadow != null) {
            int x = i_2 - (i_3 * this.activeToolkit.shadowOffsetX >> 8) >> this.activeToolkit.shadowSizeShift;
            int z = i_4 - (i_3 * this.activeToolkit.shadowOffsetZ >> 8) >> this.activeToolkit.shadowSizeShift;
            this.shadowMapper.add(shadow, x, z);
        }

    }

}
