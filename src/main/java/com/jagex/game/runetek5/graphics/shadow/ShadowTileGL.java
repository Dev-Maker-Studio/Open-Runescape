package com.jagex.game.runetek5.graphics.shadow;

import com.jagex.game.runetek5.buffer.ElementsBufferGL;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture;
import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class ShadowTileGL {

	public boolean needsTextureRegeneration = true;
    public int lastModifiedPixelCount = -1;
    public final OpenGlToolkit toolkit;
    public final ShadowMapperGL shadowMapper;
    public final int tileWidth;
    public final int tileLength;
    public final int materialCount;
    public OpenGl2DTexture cachedTexture;
    public ElementsBufferGL elementsBuffer;
    public GlElementsArrayBuffer aClass131Sub21567;

    public ShadowTileGL(OpenGlToolkit toolkit, ShadowMapperGL shadowMapper, GroundGL ground, int x, int y, int sizeShift, int width, int length) {
        this.toolkit = toolkit;
        this.shadowMapper = shadowMapper;
        this.tileWidth = width;
        this.tileLength = length;
        int size = 1 << sizeShift;
        int materialCount = 0;
        int upscaledX = x << sizeShift;
        int upscaledY = y << sizeShift;

        for (int px = 0; px < size; px++) {
            int i_14 = ground.width * (upscaledY + px) + upscaledX;

            for (int py = 0; py < size; py++) {
                short[] shorts_20 = ground.materialIndices[i_14++];
                if (shorts_20 != null) {
                    materialCount += shorts_20.length;
                }
            }
        }

        this.materialCount = materialCount;
        if (materialCount > 0) {
            Packet buffer = new Packet(materialCount * 2);
            short[] materialIndices;
            if (this.toolkit.bigEndianess) {
                for (int px = 0; px < size; px++) {
                    int indicePtr = ground.width * (upscaledY + px) + upscaledX;

                    for (int py = 0; py < size; py++) {
                        materialIndices = ground.materialIndices[indicePtr++];
                        if (materialIndices != null) {
                            for (int i_18 = 0; i_18 < materialIndices.length; i_18++) {
                                buffer.writeShort(materialIndices[i_18] & 0xffff);
                            }
                        }
                    }
                }
            } else {
                for (int px = 0; px < size; px++) {
                    int indicePtr = ground.width * (upscaledY + px) + upscaledX;
                    for (int py = 0; py < size; py++) {
                        materialIndices = ground.materialIndices[indicePtr++];
                        if (materialIndices != null) {
                            for (int idx = 0; idx < materialIndices.length; idx++) {
                                buffer.writeLEShort(materialIndices[idx] & 0xffff);
                            }
                        }
                    }
                }
            }

            this.elementsBuffer = this.toolkit.createElementsArrayBuffer(buffer.buffer, buffer.currentPosition, false);
            this.aClass131Sub21567 = new GlElementsArrayBuffer(toolkit, null, 1);
        } else {
        	this.cachedTexture = null;
        }

    }

    public void method2155() {
        method2165(this.elementsBuffer, this.materialCount);
    }

    public void method2156(byte[] bytes_1, int i_2) {
        this.aClass131Sub21567.putElements(bytes_1, i_2 * 2);
        method2165(this.aClass131Sub21567, i_2);
    }

    public void regenerateTexture() {
        if (this.needsTextureRegeneration) {
            this.needsTextureRegeneration = false;
            byte[] shadowMapperData = this.shadowMapper.shadowMapperData;
            byte[] shadowTextureData = this.toolkit.shadowTextureData;
            int modifiedPixelCount = 0;
            int shadowMapperWidth = this.shadowMapper.width;
            int totalOffset = this.tileWidth + this.shadowMapper.width * this.tileLength;

            for (int x = -128; x < 0; x++) {
                modifiedPixelCount = (modifiedPixelCount << 8) - modifiedPixelCount;

                for (int y = -128; y < 0; y++) {
                    if (shadowMapperData[totalOffset++] != 0) {
                        modifiedPixelCount++;
                    }
                }

                totalOffset += shadowMapperWidth - 128;
            }

            if (this.cachedTexture != null && modifiedPixelCount == this.lastModifiedPixelCount) {
            } else {
                this.lastModifiedPixelCount = modifiedPixelCount;
                int dataIndex = 0;
                totalOffset = shadowMapperWidth * this.tileLength + this.tileWidth;

                for (int x = -128; x < 0; x++) {
                    for (int y = -128; y < 0; y++) {
                        if (shadowMapperData[totalOffset] != 0) {
                            shadowTextureData[dataIndex++] = 68;
                        } else {
                            int neighborCount = 0;
                            if (shadowMapperData[totalOffset - 1] != 0) {
                                neighborCount++;
                            }

                            if (shadowMapperData[totalOffset + 1] != 0) {
                                neighborCount++;
                            }

                            if (shadowMapperData[totalOffset - shadowMapperWidth] != 0) {
                                neighborCount++;
                            }

                            if (shadowMapperData[totalOffset + shadowMapperWidth] != 0) {
                                neighborCount++;
                            }

                            shadowTextureData[dataIndex++] = (byte) (neighborCount * 17);
                        }

                        totalOffset++;
                    }

                    totalOffset += this.shadowMapper.width - 128;
                }

                if (this.cachedTexture == null) {
                    this.cachedTexture = new OpenGl2DTexture(this.toolkit, 3553, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, 128, 128, false, this.toolkit.shadowTextureData, GLFormat.RGBA, false);
                    this.cachedTexture.setRepeat(false, false);
                    this.cachedTexture.setLinearTextureFilter(true);
                } else {
                    this.cachedTexture.uploadTextureData(0, 128, 128, this.toolkit.shadowTextureData, GLFormat.RGBA, false);
                }
            }
        }

    }

    public void method2165(ElementsBufferGL interface151, int i_2) {
        if (i_2 != 0) {
            regenerateTexture();
            this.toolkit.setActiveTexture(this.cachedTexture);
            this.toolkit.drawElements(interface151, 0, i_2);
        }

    }

}
