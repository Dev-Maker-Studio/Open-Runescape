package com.jagex.game.runetek5.toolkit.directx;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.graphics.model.particle.ParticleNode;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.graphics.model.particle.PointEntity;
import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent;
import com.jagex.game.runetek5.utils.BufferUtils;

public class DirectXParticleRenderer {

    public static float currentLightIntensity;
    public int anInt509;
    int anInt497 = 64;
    int anInt501 = 64;
    int anInt502 = 768;
    int anInt503 = 1600;
    public final int anInt504 = BufferUtils.ilog(1600);
    public final int[] anIntArray506;
    public final int[] pointEntityIndices;
    public final int[] anIntArray508;
    public final PointEntity[][] aEntityNodeSub1Sub1ArrayArray498;
    public final PointEntity[][] pointEntities;
    public final VertexDeclaration aClass70499;
    public final NativeStream anInterface4500;
    public final NativeStream anInterface4510;
    public final DirectXBuffer anInterface32511;

    public DirectXParticleRenderer(AbstractNativeToolkit class505Sub21) {
        this.anIntArray506 = new int[8191];
        this.pointEntityIndices = new int[1600];
        this.anIntArray508 = new int[64];
        this.aEntityNodeSub1Sub1ArrayArray498 = new PointEntity[1600][64];
        this.pointEntities = new PointEntity[64][768];
        this.anInt509 = 0;
        this.aClass70499 = class505Sub21.method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69690, VertexDeclarationAction.aClass69692}), new VertexDeclarationComponent(VertexDeclarationAction.NORMAL_POINTER_ACTION)});
        this.anInterface4500 = class505Sub21.createStream(true);
        this.anInterface4510 = class505Sub21.createStream(false);
        this.anInterface4510.create(393168, 12);
        this.anInterface32511 = class505Sub21.method13993(false);
        this.anInterface32511.create(49146);
        ByteBuffer bytebuffer2 = class505Sub21.directBuffer;
        bytebuffer2.clear();

        int i_3;
        for (i_3 = 0; i_3 < 8191; i_3++) {
            int i_4 = i_3 * 4;
            bytebuffer2.putShort((short) i_4);
            bytebuffer2.putShort((short) (i_4 + 1));
            bytebuffer2.putShort((short) (i_4 + 2));
            bytebuffer2.putShort((short) (i_4 + 2));
            bytebuffer2.putShort((short) (i_4 + 3));
            bytebuffer2.putShort((short) i_4);
        }

        this.anInterface32511.upload(0, bytebuffer2.position(), class505Sub21.directBufferAddress);
        bytebuffer2.clear();

        for (i_3 = 0; i_3 < 8191; i_3++) {
            bytebuffer2.putFloat(0.0F);
            bytebuffer2.putFloat(-1.0f);
            bytebuffer2.putFloat(0.0F);
            bytebuffer2.putFloat(0.0F);
            bytebuffer2.putFloat(-1.0f);
            bytebuffer2.putFloat(0.0F);
            bytebuffer2.putFloat(0.0F);
            bytebuffer2.putFloat(-1.0f);
            bytebuffer2.putFloat(0.0F);
            bytebuffer2.putFloat(0.0F);
            bytebuffer2.putFloat(-1.0f);
            bytebuffer2.putFloat(0.0F);
        }

        this.anInterface4510.upload(0, bytebuffer2.position(), class505Sub21.directBufferAddress);
    }

    public void method1089() {
        this.anInterface4500.create(786336, 24);
    }

    public void deallocate() {
        this.anInterface4500.deallocate();
    }

    public void render(AbstractNativeToolkit nativeToolkit, ParticleWrapper particleWrapper) {
        nativeToolkit.setDepthBuffering(false);
        currentLightIntensity = nativeToolkit.lightIntensity;
        float zNearA = nativeToolkit.particleMatrix.faceList[2];
        float zNearB = nativeToolkit.particleMatrix.faceList[6];
        float zNearC = nativeToolkit.particleMatrix.faceList[10];
        float zNearD = nativeToolkit.particleMatrix.faceList[14];
        int i_7 = 0;
        int i_8 = Integer.MAX_VALUE;
        int i_9 = 0;
        ParticleNode particleTree = particleWrapper.particleBinaryTree.current;

        for (ParticleNode particleNode = particleTree.child; particleNode != particleTree; particleNode = particleNode.child) {
            PointEntity pointEntity = (PointEntity) particleNode;
            int i_22 = (int) (zNearA * (pointEntity.x >> 12) + zNearB * (pointEntity.y >> 12) + zNearC * (pointEntity.z >> 12) + zNearD);
            if (i_22 > i_9) {
                i_9 = i_22;
            }

            if (i_22 < i_8) {
                i_8 = i_22;
            }

            this.anIntArray506[i_7++] = i_22;
        }

        int i_20 = i_9 - i_8;
        int i_12;
        if (i_20 + 2 > 1600) {
            i_12 = 1 + BufferUtils.ilog(i_20) - this.anInt504;
            i_20 = (i_20 >> i_12) + 2;
        } else {
            i_12 = 0;
            i_20 += 2;
        }

        ParticleNode child = particleTree.child;
        i_7 = 0;
        nativeToolkit.method13997(this.anInterface32511);
        AbstractParticleShader class10314 = nativeToolkit.abstractParticleShader;
        class10314.method1788(AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX);
        class10314.textureCoordinateMatrix.identity();
        class10314.diffuseColor = -1;
        int materialId = -2;
        boolean hasLightAdjustment = true;

        for (boolean hasAdjustments = true; child != particleTree; method1096(nativeToolkit, i_20)) {
            this.anInt509 = 0;

            int i_18;
            for (i_18 = 0; i_18 < i_20; i_18++) {
                this.pointEntityIndices[i_18] = 0;
            }

            for (i_18 = 0; i_18 < 64; i_18++) {
                this.anIntArray508[i_18] = 0;
            }

            for (; child != particleTree; child = child.child) {
                PointEntity pointEntity = (PointEntity) child;
                if (hasAdjustments) {
                    materialId = pointEntity.materialId;
                    hasLightAdjustment = pointEntity.lightIntensityAdjustment;
                    hasAdjustments = false;
                }

                if (i_7 > 0 && (materialId != pointEntity.materialId || hasLightAdjustment != pointEntity.lightIntensityAdjustment)) {
                    hasAdjustments = true;
                    break;
                }

                int i_19 = this.anIntArray506[i_7++] - i_8 >> i_12;
                if (i_19 < 1600) {
                    if (this.pointEntityIndices[i_19] < 64) {
                        this.aEntityNodeSub1Sub1ArrayArray498[i_19][this.pointEntityIndices[i_19]++] = pointEntity;
                    } else {
                        if (this.pointEntityIndices[i_19] == 64) {
                            if (this.anInt509 == 64) {
                                continue;
                            }

                            this.pointEntityIndices[i_19] += 1 + this.anInt509++;
                        }

                        PointEntity[] arr_10000 = this.pointEntities[this.pointEntityIndices[i_19] - 64 - 1];
                        int i_10002 = this.pointEntityIndices[i_19] - 64 - 1;
                        int i_10004 = this.anIntArray508[this.pointEntityIndices[i_19] - 64 - 1];
                        this.anIntArray508[i_10002] = this.anIntArray508[this.pointEntityIndices[i_19] - 64 - 1] + 1;
                        arr_10000[i_10004] = pointEntity;
                    }
                }
            }

            class10314.diffuseSampler = nativeToolkit.materialCache.getTextureSampler(materialId >= 0 ? materialId : -1);
            if (hasLightAdjustment && nativeToolkit.lightIntensity != currentLightIntensity) {
                nativeToolkit.updateSun(currentLightIntensity);
            } else if (nativeToolkit.lightIntensity != 1.0F) {
                nativeToolkit.updateSun(1.0F);
            }
        }

        if (nativeToolkit.lightIntensity != currentLightIntensity) {
            nativeToolkit.updateSun(currentLightIntensity);
        }

        nativeToolkit.setDepthBuffering(true);
    }

    public void method1096(AbstractNativeToolkit nativeToolkit, int i_2) {
        int i_3 = 0;
        Matrix44 particleMatrix = nativeToolkit.particleMatrix;
        float xA = particleMatrix.faceList[0];
        float xB = particleMatrix.faceList[4];
        float xC = particleMatrix.faceList[8];
        float yA = particleMatrix.faceList[1];
        float yB = particleMatrix.faceList[5];
        float yC = particleMatrix.faceList[9];
        float scaleXA = xA + yA;
        float scaleYA = xB + yB;
        float scaleZA = xC + yC;
        float scaleXB = xA - yA;
        float scaleYB = xB - yB;
        float scakeZB = xC - yC;
        float scaleXC = yA - xA;
        float scaleYC = yB - xB;
        float scaleZC = yC - xC;
        ByteBuffer directBuffer = nativeToolkit.directBuffer;
        directBuffer.clear();

        for (int i_21 = i_2 - 1; i_21 >= 0; --i_21) {
            int i_22 = Math.min(64, this.pointEntityIndices[i_21]);
            if (i_22 > 0) {
                int i_23;
                for (i_23 = i_22 - 1; i_23 >= 0; --i_23) {
                    PointEntity pointEntity = this.aEntityNodeSub1Sub1ArrayArray498[i_21][i_23];
                    int colorRgba = pointEntity.colorArgb;
                    byte red = (byte) (colorRgba >> 16);
                    byte green = (byte) (colorRgba >> 8);
                    byte blue = (byte) colorRgba;
                    byte alpha = (byte) (colorRgba >>> 24);
                    float downscaledX = (pointEntity.x >> 12);
                    float downscaledY = (pointEntity.y >> 12);
                    float downscaledZ = (pointEntity.z >> 12);
                    int downscaledSize = pointEntity.size >> 12;
                    directBuffer.putFloat(downscaledX + scaleXA * (-downscaledSize));
                    directBuffer.putFloat(downscaledY + scaleYA * (-downscaledSize));
                    directBuffer.putFloat(downscaledZ + scaleZA * (-downscaledSize));
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue);
                        directBuffer.put(green);
                        directBuffer.put(red);
                        directBuffer.put(alpha);
                    } else {
                        directBuffer.put(red);
                        directBuffer.put(green);
                        directBuffer.put(blue);
                        directBuffer.put(alpha);
                    }

                    directBuffer.putFloat(0.0F);
                    directBuffer.putFloat(0.0F);
                    directBuffer.putFloat(downscaledX + scaleXC * downscaledSize);
                    directBuffer.putFloat(downscaledY + scaleYC * downscaledSize);
                    directBuffer.putFloat(downscaledZ + scaleZC * downscaledSize);
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue);
                        directBuffer.put(green);
                        directBuffer.put(red);
                        directBuffer.put(alpha);
                    } else {
                        directBuffer.put(red);
                        directBuffer.put(green);
                        directBuffer.put(blue);
                        directBuffer.put(alpha);
                    }

                    directBuffer.putFloat(0.0F);
                    directBuffer.putFloat(1.0F);
                    directBuffer.putFloat(downscaledX + scaleXA * downscaledSize);
                    directBuffer.putFloat(downscaledY + scaleYA * downscaledSize);
                    directBuffer.putFloat(downscaledZ + scaleZA * downscaledSize);
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue);
                        directBuffer.put(green);
                        directBuffer.put(red);
                        directBuffer.put(alpha);
                    } else {
                        directBuffer.put(red);
                        directBuffer.put(green);
                        directBuffer.put(blue);
                        directBuffer.put(alpha);
                    }

                    directBuffer.putFloat(1.0F);
                    directBuffer.putFloat(1.0F);
                    directBuffer.putFloat(downscaledX + scaleXB * downscaledSize);
                    directBuffer.putFloat(downscaledY + scaleYB * downscaledSize);
                    directBuffer.putFloat(downscaledZ + scakeZB * downscaledSize);
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue);
                        directBuffer.put(green);
                        directBuffer.put(red);
                        directBuffer.put(alpha);
                    } else {
                        directBuffer.put(red);
                        directBuffer.put(green);
                        directBuffer.put(blue);
                        directBuffer.put(alpha);
                    }

                    directBuffer.putFloat(1.0F);
                    directBuffer.putFloat(0.0F);
                    ++i_3;
                }

                if (this.pointEntityIndices[i_21] > 64) {
                    i_23 = this.pointEntityIndices[i_21] - 64 - 1;

                    for (int i_24 = this.anIntArray508[i_23] - 1; i_24 >= 0; --i_24) {
                        PointEntity pointEntity = this.pointEntities[i_23][i_24];
                        int color = pointEntity.colorArgb;
                        byte red = (byte) (color >> 16);
                        byte green = (byte) (color >> 8);
                        byte blue = (byte) color;
                        byte alpha = (byte) (color >>> 24);
                        float downscaledX = (pointEntity.x >> 12);
                        float downscaledY = (pointEntity.y >> 12);
                        float downscaledZ = (pointEntity.z >> 12);
                        int downscaledSize = pointEntity.size >> 12;
                        directBuffer.putFloat(downscaledX + scaleXA * (-downscaledSize));
                        directBuffer.putFloat(downscaledY + scaleYA * (-downscaledSize));
                        directBuffer.putFloat(downscaledZ + scaleZA * (-downscaledSize));
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue);
                            directBuffer.put(green);
                            directBuffer.put(red);
                            directBuffer.put(alpha);
                        } else {
                            directBuffer.put(red);
                            directBuffer.put(green);
                            directBuffer.put(blue);
                            directBuffer.put(alpha);
                        }

                        directBuffer.putFloat(0.0F);
                        directBuffer.putFloat(0.0F);
                        directBuffer.putFloat(downscaledX + scaleXC * downscaledSize);
                        directBuffer.putFloat(downscaledY + scaleYC * downscaledSize);
                        directBuffer.putFloat(downscaledZ + scaleZC * downscaledSize);
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue);
                            directBuffer.put(green);
                            directBuffer.put(red);
                            directBuffer.put(alpha);
                        } else {
                            directBuffer.put(red);
                            directBuffer.put(green);
                            directBuffer.put(blue);
                            directBuffer.put(alpha);
                        }

                        directBuffer.putFloat(0.0F);
                        directBuffer.putFloat(1.0F);
                        directBuffer.putFloat(downscaledX + scaleXA * downscaledSize);
                        directBuffer.putFloat(downscaledY + scaleYA * downscaledSize);
                        directBuffer.putFloat(downscaledZ + scaleZA * downscaledSize);
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue);
                            directBuffer.put(green);
                            directBuffer.put(red);
                            directBuffer.put(alpha);
                        } else {
                            directBuffer.put(red);
                            directBuffer.put(green);
                            directBuffer.put(blue);
                            directBuffer.put(alpha);
                        }

                        directBuffer.putFloat(1.0F);
                        directBuffer.putFloat(1.0F);
                        directBuffer.putFloat(downscaledX + scaleXB * downscaledSize);
                        directBuffer.putFloat(downscaledY + scaleYB * downscaledSize);
                        directBuffer.putFloat(downscaledZ + scakeZB * downscaledSize);
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue);
                            directBuffer.put(green);
                            directBuffer.put(red);
                            directBuffer.put(alpha);
                        } else {
                            directBuffer.put(red);
                            directBuffer.put(green);
                            directBuffer.put(blue);
                            directBuffer.put(alpha);
                        }

                        directBuffer.putFloat(1.0F);
                        directBuffer.putFloat(0.0F);
                        ++i_3;
                    }
                }
            }
        }

        this.anInterface4500.upload(0, directBuffer.position(), nativeToolkit.directBufferAddress);
        nativeToolkit.setStreamSource(0, this.anInterface4500);
        nativeToolkit.setStreamSource(1, this.anInterface4510);
        nativeToolkit.setVertexDeclaration(this.aClass70499);
        AbstractParticleShader particleShader = nativeToolkit.abstractParticleShader;
        particleShader.method1787(i_3);
    }

}
