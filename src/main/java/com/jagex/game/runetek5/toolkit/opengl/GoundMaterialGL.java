package com.jagex.game.runetek5.toolkit.opengl;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer;
import com.jagex.game.runetek5.scene.ground.GroundFunctionMask;
import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer;

import jaclib.memory.Stream;
import jaclib.memory.heap.NativeHeapBuffer;
import jaggl.OpenGL;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;

public class GoundMaterialGL extends Node {

    public OpenGlArrayDrawBuffer arrayBufferPointer;
    public NativeHeapBuffer nativeHeapBuffer;
    public Stream nativeStream;
    public final GroundGL ground;
    public final OpenGlToolkit toolkit;
    public final int materialId;
    public final float textureScale;
    public final WaterTile waterTile;
    public final int[] groundFunctionMask;
    public final GlElementsArrayBuffer texture2d;

    public GoundMaterialGL(GroundGL ground, int materialId, int scaling, WaterTile waterTile) {
        this.ground = ground;
        this.toolkit = ground.activeToolkit;
        this.materialId = materialId;
        this.textureScale = scaling;
        this.waterTile = waterTile;
        this.groundFunctionMask = new int[this.ground.width * this.ground.length];
        this.texture2d = new GlElementsArrayBuffer(this.toolkit, null, 1);
    }

    public void appendToNativeStream(int i_1) {
        this.nativeStream.method2919(i_1 * 4 + 3);
        this.nativeStream.putByte(-1);
    }

    public void generateFunctionMask(int x, int y, int faceCount) {
        this.groundFunctionMask[y * this.ground.width + x] |= 1 << faceCount;
    }

    public void method12083(int i_1, int color, int lightness, float colorMultiplier) {
        if (this.materialId != -1) {
            MaterialType material = this.toolkit.materialProvider.getMaterial(this.materialId);
            int contrast = material.alpha & 0xff;
            if (contrast != 0 && material.effectId != 4) {
            	int i_7;
                if (lightness < 0) {
                    i_7 = 0;
                } else if (lightness > 127) {
                    i_7 = 16777215;
                } else {
                    i_7 = lightness * 131586;
                }

                int blendContrast = 256 - contrast;
                color = ((i_7 & 0xff00ff) * contrast + blendContrast * (color & 0xff00ff) & -16711936) + (blendContrast * (color & 0xff00) + contrast * (i_7 & 0xff00) & 0xff0000) >> 8;
            }

            int brightness = material.brightness & 0xff;
            if (brightness != 0) {
                brightness += 256;
                int red = ((color & 0xff0000) >> 16) * brightness;
                if (red > 65535) {
                    red = 65535;
                }
                int green = ((color & 0xff00) >> 8) * brightness;
                if (green > 65535) {
                    green = 65535;
                }
                int blue = brightness * (color & 0xff);
                if (blue > 65535) {
                    blue = 65535;
                }
                color = (blue >> 8) + (green & 0xff00) + (red << 8 & 0xff0000);
            }
        }

        if (colorMultiplier != 1.0F) {
            int red = color >> 16 & 0xff;
            int green = color >> 8 & 0xff;
            int blue = color & 0xff;
            red = (int) (red * colorMultiplier);
            if (red < 0) {
                red = 0;
            } else if (red > 255) {
                red = 255;
            }

            green = (int) (green * colorMultiplier);
            if (green < 0) {
                green = 0;
            } else if (green > 255) {
                green = 255;
            }

            blue = (int) (blue * colorMultiplier);
            if (blue < 0) {
                blue = 0;
            } else if (blue > 255) {
                blue = 255;
            }
            color = red << 16 | green << 8 | blue;
        }
        this.nativeStream.method2919(i_1 * 4);
        this.nativeStream.putByte((byte) (color >> 16));
        this.nativeStream.putByte((byte) (color >> 8));
        this.nativeStream.putByte((byte) color);
    }

    public void method12085(int[] ints_1, int i_2) {
        int addedTextures = 0;
        GlBitBuffer textureBuffer = this.toolkit.textureBuffer;
        textureBuffer.currentPosition = 0;
        if (this.toolkit.bigEndianess) {
            for (int i_5 = 0; i_5 < i_2; i_5++) {
            	int i_6 = ints_1[i_5];
            	short[] materialsByTriangle = this.ground.materialIndices[i_6];
                int functionMask = this.groundFunctionMask[i_6];
                if (functionMask != 0 && materialsByTriangle != null) {
                	int loops = 0;
                	int verticeCount = 0;
                    while (verticeCount < materialsByTriangle.length) {
                        if ((functionMask & 1 << loops++) != 0) {
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++] & 0xffff);
                            addedTextures++;
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++] & 0xffff);
                            addedTextures++;
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++] & 0xffff);
                            addedTextures++;
                        } else {
                            verticeCount += 3;
                        }
                    }
                }
            }
        } else {
            for (int i_5 = 0; i_5 < i_2; i_5++) {
            	int i_6 = ints_1[i_5];
            	short[] materialsByTriangle = this.ground.materialIndices[i_6];
                int functionMask = this.groundFunctionMask[i_6];
                if (functionMask != 0 && materialsByTriangle != null) {
                	int loops = 0;
                	int verticeCount = 0;

                    while (verticeCount < materialsByTriangle.length) {
                        if ((functionMask & 1 << loops++) != 0) {
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++] & 0xffff);
                            addedTextures++;
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++] & 0xffff);
                            addedTextures++;
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++] & 0xffff);
                            addedTextures++;
                        } else {
                            verticeCount += 3;
                        }
                    }
                }
            }
        }
        if (addedTextures > 0) {
            this.texture2d.putElements(textureBuffer.buffer, textureBuffer.currentPosition);
            this.toolkit.toggleDrawingClientstates(this.ground.vertexBuffer, this.ground.normalsBuffer, this.arrayBufferPointer, this.ground.textureBuffer);
            this.toolkit.renderTexture(this.materialId, (this.ground.waterFunctionMask & GroundFunctionMask.HD_LIGHT) != 0, (this.ground.waterFunctionMask & 0x8) != 0);
            if (this.toolkit.stillWaterToggled) {
                this.toolkit.addWaterTile(Integer.MAX_VALUE, this.waterTile);
            }

            OpenGL.glMatrixMode(GL_TEXTURE);
            OpenGL.glPushMatrix();
            OpenGL.glScalef(1.0F / this.textureScale, 1.0F / this.textureScale, 1.0F);
            OpenGL.glMatrixMode(GL_MODELVIEW);
            this.toolkit.toggleDrawingClientstates(this.ground.vertexBuffer, this.ground.normalsBuffer, this.arrayBufferPointer, this.ground.textureBuffer);
            this.toolkit.drawElements(this.texture2d, 0, addedTextures);
            OpenGL.glMatrixMode(GL_TEXTURE);
            OpenGL.glPopMatrix();
            OpenGL.glMatrixMode(GL_MODELVIEW);
        }

    }

    public void createDataArrayBuffer(int totalTiles) {
        this.nativeStream.method2925();
        ArrayBufferGL pointer = this.toolkit.createDataArrayBuffer(4, this.nativeHeapBuffer, totalTiles * 4);
        if (pointer instanceof GlVboArrayBuffer) {
            this.nativeHeapBuffer.deallocateBuffer();
        }
        this.arrayBufferPointer = new OpenGlArrayDrawBuffer(pointer, GL_UNSIGNED_BYTE, 4, 0);
        this.nativeHeapBuffer = null;
        this.nativeStream = null;
    }

    public void generateNativeBuffer(int colorCount) {
        this.nativeHeapBuffer = this.toolkit.nativeHeap.createBuffer(colorCount * 4, true);
        this.nativeStream = new Stream(this.nativeHeapBuffer);
    }

}
