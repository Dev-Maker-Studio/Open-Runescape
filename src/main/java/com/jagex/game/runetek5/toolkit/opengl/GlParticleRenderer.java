package com.jagex.game.runetek5.toolkit.opengl;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;
import com.jagex.game.runetek5.graphics.model.particle.ParticleNode;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.graphics.model.particle.PointEntity;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer;
import com.jagex.game.runetek5.utils.BufferUtils;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class GlParticleRenderer {

    public static float sunBrightness;
    public ArrayBufferGL arrayBuffer;
    public OpenGlArrayDrawBuffer textureCoordinates;
    public OpenGlArrayDrawBuffer vertices;
    public OpenGlArrayDrawBuffer colors;
    public int anInt1616;
    public final float[] modelviewMatrix = new float[16];
    public final GlBitBuffer textureBuffer = new GlBitBuffer(786336);
    public final int anInt1609;
    public final int[] particleDepths;
    public final int[] anIntArray1615;
    public final int[] anIntArray1604;
    public final PointEntity[][] aEntityNodeSub1Sub1ArrayArray1614;
    public final PointEntity[][] aEntityNodeSub1Sub1ArrayArray1607;

    public GlParticleRenderer() {
        this.anInt1609 = BufferUtils.ilog(1600);
        this.particleDepths = new int[8191];
        this.anIntArray1615 = new int[1600];
        this.anIntArray1604 = new int[64];
        this.aEntityNodeSub1Sub1ArrayArray1614 = new PointEntity[1600][64];
        this.aEntityNodeSub1Sub1ArrayArray1607 = new PointEntity[64][768];
        this.anInt1616 = 0;
    }

    public void initialize(OpenGlToolkit toolkit) {
        this.arrayBuffer = toolkit.createArrayBuffer(24, null, 196584, true);
        this.textureCoordinates = new OpenGlArrayDrawBuffer(this.arrayBuffer, GL_FLOAT, 2, 0);
        this.vertices = new OpenGlArrayDrawBuffer(this.arrayBuffer, GL_FLOAT, 3, 8);
        this.colors = new OpenGlArrayDrawBuffer(this.arrayBuffer, GL_UNSIGNED_BYTE, 4, 20);
    }

    public void drawParticles(OpenGlToolkit toolkit, ParticleWrapper particleWrapper) {
        disableLights(toolkit);
        float vertexZNearA = toolkit.aClass3848348.faceList[2];
        float vertexZNearB = toolkit.aClass3848348.faceList[6];
        float vertexZNearC = toolkit.aClass3848348.faceList[10];
        float vertexZNearD = toolkit.aClass3848348.faceList[14];

        try {
            int totalParticles = 0;
            int minDepth = Integer.MAX_VALUE;
            int maxDepth = 0;
            ParticleNode tree = particleWrapper.particleBinaryTree.current;

            for (ParticleNode particle = tree.child; particle != tree; particle = particle.child) {
                PointEntity particleEntity = (PointEntity) particle;
                int depth = (int) (vertexZNearA * (particleEntity.x >> 12) + vertexZNearB * (particleEntity.y >> 12) + vertexZNearC * (particleEntity.z >> 12) + vertexZNearD);
                if (depth > maxDepth) {
                    maxDepth = depth;
                }

                if (depth < minDepth) {
                    minDepth = depth;
                }

                this.particleDepths[totalParticles++] = depth;
            }

            int depthRange = maxDepth - minDepth;
            int i_20;
            if (depthRange + 2 > 1600) {
                i_20 = 1 + BufferUtils.ilog(depthRange) - this.anInt1609;
                depthRange = (depthRange >> i_20) + 2;
            } else {
                i_20 = 0;
                depthRange += 2;
            }

            ParticleNode particles = tree.child;
            int particlePtr = 0;
            int materialId = -2;
            boolean brightnessUpdate = true;

            for (boolean updateRequired = true; particles != tree; draw(toolkit, depthRange)) {
                this.anInt1616 = 0;

                for (int i_17 = 0; i_17 < depthRange; i_17++) {
                    this.anIntArray1615[i_17] = 0;
                }

                for (int i_17 = 0; i_17 < 64; i_17++) {
                    this.anIntArray1604[i_17] = 0;
                }

                for (; particles != tree; particles = particles.child) {
                    PointEntity particleEntity = (PointEntity) particles;
                    if (updateRequired) {
                        materialId = particleEntity.materialId;
                        brightnessUpdate = particleEntity.lightIntensityAdjustment;
                        updateRequired = false;
                    }

                    if (particlePtr > 0 && (materialId != particleEntity.materialId || brightnessUpdate != particleEntity.lightIntensityAdjustment)) {
                        updateRequired = true;
                        break;
                    }

                    int depth = this.particleDepths[particlePtr++] - minDepth >> i_20;
                    if (depth < 1600) {
                        if (this.anIntArray1615[depth] < 64) {
                            this.aEntityNodeSub1Sub1ArrayArray1614[depth][this.anIntArray1615[depth]++] = particleEntity;
                        } else {
                            if (this.anIntArray1615[depth] == 64) {
                                if (this.anInt1616 == 64) {
                                    continue;
                                }

                                this.anIntArray1615[depth] += 1 + this.anInt1616++;
                            }

                            PointEntity[] arr_10000 = this.aEntityNodeSub1Sub1ArrayArray1607[this.anIntArray1615[depth] - 64 - 1];
                            int i_10002 = this.anIntArray1615[depth] - 64 - 1;
                            int i_10004 = this.anIntArray1604[this.anIntArray1615[depth] - 64 - 1];
                            this.anIntArray1604[i_10002] = this.anIntArray1604[this.anIntArray1615[depth] - 64 - 1] + 1;
                            arr_10000[i_10004] = particleEntity;
                        }
                    }
                }

                if (materialId >= 0) {
                    toolkit.renderTexture(materialId);
                } else {
                    toolkit.renderTexture(-1);
                }

                if (brightnessUpdate && toolkit.sunBrightness != sunBrightness) {
                    toolkit.updateSun(sunBrightness);
                } else if (toolkit.sunBrightness != 1.0F) {
                    toolkit.updateSun(1.0F);
                }
            }
        } catch (Exception ignored) {
        	// NOOP
        }

        enableLights(toolkit);

    }

    public void draw(OpenGlToolkit toolkit, int depthRange) {
        OpenGL.glGetFloatv(GL_MODELVIEW_MATRIX, this.modelviewMatrix, 0);
        float vertexXA = this.modelviewMatrix[0];
        float vertexXB = this.modelviewMatrix[4];
        float vertexXC = this.modelviewMatrix[8];
        float vertexYA = this.modelviewMatrix[1];
        float vertexYB = this.modelviewMatrix[5];
        float vertexYC = this.modelviewMatrix[9];
        float offX1 = vertexXA + vertexYA;
        float offY1 = vertexXB + vertexYB;
        float offZ1 = vertexXC + vertexYC;
        float offX3 = vertexXA - vertexYA;
        float offY3 = vertexXB - vertexYB;
        float offZ3 = vertexXC - vertexYC;
        float offX2 = vertexYA - vertexXA;
        float offY2 = vertexYB - vertexXB;
        float offZ2 = vertexYC - vertexXC;
        this.textureBuffer.currentPosition = 0;
        if (toolkit.bigEndianess) {
            for (int i_18 = depthRange - 1; i_18 >= 0; --i_18) {
            	int i_19 = Math.min(64, this.anIntArray1615[i_18]);
                if (i_19 > 0) {
                    for (int i_20 = i_19 - 1; i_20 >= 0; --i_20) {
                    	PointEntity pointEntity = this.aEntityNodeSub1Sub1ArrayArray1614[i_18][i_20];
                        int color = pointEntity.colorArgb;
                        byte red = (byte) (color >> 16);
                        byte green = (byte) (color >> 8);
                        byte blue = (byte) color;
                        byte alpha = (byte) (color >>> 24);
                        float x = (pointEntity.x >> 12);
                        float y = (pointEntity.y >> 12);
                        float z = (pointEntity.z >> 12);
                        int size = pointEntity.size >> 12;
                        this.textureBuffer.writeFloatBigEndian(0.0F);
                        this.textureBuffer.writeFloatBigEndian(0.0F);
                        this.textureBuffer.writeFloatBigEndian(x + offX1 * (-size));
                        this.textureBuffer.writeFloatBigEndian(y + offY1 * (-size));
                        this.textureBuffer.writeFloatBigEndian(z + offZ1 * (-size));
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                        this.textureBuffer.writeFloatBigEndian(0.0F);
                        this.textureBuffer.writeFloatBigEndian(1.0F);
                        this.textureBuffer.writeFloatBigEndian(x + offX2 * size);
                        this.textureBuffer.writeFloatBigEndian(y + offY2 * size);
                        this.textureBuffer.writeFloatBigEndian(z + offZ2 * size);
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                        this.textureBuffer.writeFloatBigEndian(1.0F);
                        this.textureBuffer.writeFloatBigEndian(1.0F);
                        this.textureBuffer.writeFloatBigEndian(x + offX1 * size);
                        this.textureBuffer.writeFloatBigEndian(y + offY1 * size);
                        this.textureBuffer.writeFloatBigEndian(z + offZ1 * size);
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                        this.textureBuffer.writeFloatBigEndian(1.0F);
                        this.textureBuffer.writeFloatBigEndian(0.0F);
                        this.textureBuffer.writeFloatBigEndian(x + offX3 * size);
                        this.textureBuffer.writeFloatBigEndian(y + offY3 * size);
                        this.textureBuffer.writeFloatBigEndian(z + offZ3 * size);
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                    }

                    if (this.anIntArray1615[i_18] > 64) {
                    	int i_20 = this.anIntArray1615[i_18] - 64 - 1;

                        for (int i_21 = this.anIntArray1604[i_20] - 1; i_21 >= 0; --i_21) {
                        	PointEntity entity = this.aEntityNodeSub1Sub1ArrayArray1607[i_20][i_21];
                            int color = entity.colorArgb;
                            byte red = (byte) (color >> 16);
                            byte green = (byte) (color >> 8);
                            byte blue = (byte) color;
                            byte alpha = (byte) (color >>> 24);
                            float x = (entity.x >> 12);
                            float y = (entity.y >> 12);
                            float z = (entity.z >> 12);
                            int size = entity.size >> 12;
                            this.textureBuffer.writeFloatBigEndian(0.0F);
                            this.textureBuffer.writeFloatBigEndian(0.0F);
                            this.textureBuffer.writeFloatBigEndian(x + offX1 * (-size));
                            this.textureBuffer.writeFloatBigEndian(y + offY1 * (-size));
                            this.textureBuffer.writeFloatBigEndian(z + offZ1 * (-size));
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                            this.textureBuffer.writeFloatBigEndian(0.0F);
                            this.textureBuffer.writeFloatBigEndian(1.0F);
                            this.textureBuffer.writeFloatBigEndian(x + offX2 * size);
                            this.textureBuffer.writeFloatBigEndian(y + offY2 * size);
                            this.textureBuffer.writeFloatBigEndian(z + offZ2 * size);
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                            this.textureBuffer.writeFloatBigEndian(1.0F);
                            this.textureBuffer.writeFloatBigEndian(1.0F);
                            this.textureBuffer.writeFloatBigEndian(x + offX1 * size);
                            this.textureBuffer.writeFloatBigEndian(y + offY1 * size);
                            this.textureBuffer.writeFloatBigEndian(z + offZ1 * size);
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                            this.textureBuffer.writeFloatBigEndian(1.0F);
                            this.textureBuffer.writeFloatBigEndian(0.0F);
                            this.textureBuffer.writeFloatBigEndian(x + offX3 * size);
                            this.textureBuffer.writeFloatBigEndian(y + offY3 * size);
                            this.textureBuffer.writeFloatBigEndian(z + offZ3 * size);
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                        }
                    }
                }
            }
        } else {
            for (int i_18 = depthRange - 1; i_18 >= 0; --i_18) {
            	int i_19 = Math.min(64, this.anIntArray1615[i_18]);
                if (i_19 > 0) {
                    for (int i_20 = i_19 - 1; i_20 >= 0; --i_20) {
                    	PointEntity pointEntity = this.aEntityNodeSub1Sub1ArrayArray1614[i_18][i_20];
                        int color = pointEntity.colorArgb;
                        byte red = (byte) (color >> 16);
                        byte green = (byte) (color >> 8);
                        byte blue = (byte) color;
                        byte alpha = (byte) (color >>> 24);
                        float x = (pointEntity.x >> 12);
                        float y = (pointEntity.y >> 12);
                        float z = (pointEntity.z >> 12);
                        int size = pointEntity.size >> 12;
                        this.textureBuffer.writeFloatLittleEndian(0.0F);
                        this.textureBuffer.writeFloatLittleEndian(0.0F);
                        this.textureBuffer.writeFloatLittleEndian(x + offX1 * (-size));
                        this.textureBuffer.writeFloatLittleEndian(y + offY1 * (-size));
                        this.textureBuffer.writeFloatLittleEndian(z + offZ1 * (-size));
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                        this.textureBuffer.writeFloatLittleEndian(0.0F);
                        this.textureBuffer.writeFloatLittleEndian(1.0F);
                        this.textureBuffer.writeFloatLittleEndian(x + offX2 * size);
                        this.textureBuffer.writeFloatLittleEndian(y + offY2 * size);
                        this.textureBuffer.writeFloatLittleEndian(z + offZ2 * size);
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                        this.textureBuffer.writeFloatLittleEndian(1.0F);
                        this.textureBuffer.writeFloatLittleEndian(1.0F);
                        this.textureBuffer.writeFloatLittleEndian(x + offX1 * size);
                        this.textureBuffer.writeFloatLittleEndian(y + offY1 * size);
                        this.textureBuffer.writeFloatLittleEndian(z + offZ1 * size);
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                        this.textureBuffer.writeFloatLittleEndian(1.0F);
                        this.textureBuffer.writeFloatLittleEndian(0.0F);
                        this.textureBuffer.writeFloatLittleEndian(x + offX3 * size);
                        this.textureBuffer.writeFloatLittleEndian(y + offY3 * size);
                        this.textureBuffer.writeFloatLittleEndian(z + offZ3 * size);
                        this.textureBuffer.writeByte(red);
                        this.textureBuffer.writeByte(green);
                        this.textureBuffer.writeByte(blue);
                        this.textureBuffer.writeByte(alpha);
                    }

                    if (this.anIntArray1615[i_18] > 64) {
                    	int main = this.anIntArray1615[i_18] - 64 - 1;

                        for (int sub = this.anIntArray1604[main] - 1; sub >= 0; --sub) {
                        	PointEntity entity = this.aEntityNodeSub1Sub1ArrayArray1607[main][sub];
                            int color = entity.colorArgb;
                            byte red = (byte) (color >> 16);
                            byte green = (byte) (color >> 8);
                            byte blue = (byte) color;
                            byte alpha = (byte) (color >>> 24);
                            float x = (entity.x >> 12);
                            float y = (entity.y >> 12);
                            float z = (entity.z >> 12);
                            int size = entity.size >> 12;
                            this.textureBuffer.writeFloatLittleEndian(0.0F);
                            this.textureBuffer.writeFloatLittleEndian(0.0F);
                            this.textureBuffer.writeFloatLittleEndian(x + offX1 * (-size));
                            this.textureBuffer.writeFloatLittleEndian(y + offY1 * (-size));
                            this.textureBuffer.writeFloatLittleEndian(z + offZ1 * (-size));
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                            this.textureBuffer.writeFloatLittleEndian(0.0F);
                            this.textureBuffer.writeFloatLittleEndian(1.0F);
                            this.textureBuffer.writeFloatLittleEndian(x + offX2 * size);
                            this.textureBuffer.writeFloatLittleEndian(y + offY2 * size);
                            this.textureBuffer.writeFloatLittleEndian(z + offZ2 * size);
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                            this.textureBuffer.writeFloatLittleEndian(1.0F);
                            this.textureBuffer.writeFloatLittleEndian(1.0F);
                            this.textureBuffer.writeFloatLittleEndian(x + offX1 * size);
                            this.textureBuffer.writeFloatLittleEndian(y + offY1 * size);
                            this.textureBuffer.writeFloatLittleEndian(z + offZ1 * size);
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                            this.textureBuffer.writeFloatLittleEndian(1.0F);
                            this.textureBuffer.writeFloatLittleEndian(0.0F);
                            this.textureBuffer.writeFloatLittleEndian(x + offX3 * size);
                            this.textureBuffer.writeFloatLittleEndian(y + offY3 * size);
                            this.textureBuffer.writeFloatLittleEndian(z + offZ3 * size);
                            this.textureBuffer.writeByte(red);
                            this.textureBuffer.writeByte(green);
                            this.textureBuffer.writeByte(blue);
                            this.textureBuffer.writeByte(alpha);
                        }
                    }
                }
            }
        }

        if (this.textureBuffer.currentPosition != 0) {
            this.arrayBuffer.putArray(24, this.textureBuffer.buffer, this.textureBuffer.currentPosition);
            toolkit.toggleDrawingClientstates(this.vertices, null, this.colors, this.textureCoordinates);
            toolkit.drawArrays(this.textureBuffer.currentPosition / 24);
        }
    }

    public void disableLights(OpenGlToolkit toolkit) {
        sunBrightness = toolkit.sunBrightness;
        toolkit.setupMatrixRendering();
        OpenGL.glDisable(GL_LIGHT0);
        OpenGL.glDisable(GL_LIGHT1);
        toolkit.setDepthMask(false);
        OpenGL.glNormal3f(0.0F, -1.0f, 0.0F);
    }

    public void enableLights(OpenGlToolkit toolkit) {
        toolkit.setDepthMask(true);
        OpenGL.glEnable(GL_LIGHT0);
        OpenGL.glEnable(GL_LIGHT1);
        if (toolkit.sunBrightness != sunBrightness) {
            toolkit.updateSun(sunBrightness);
        }
    }
}