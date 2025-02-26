package com.jagex.game.runetek5.config.material.texture;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class TextureType extends CacheableNode {

    public final TextureOperation[] textureOperations;
    public final int[] textureFileIds;
    public final int[] materialFileIds;
    public final TextureOperation opaqueOperation;
    public final TextureOperation opacityOperation;
    public final TextureOperation hdrOperation;
    
	public TextureType(Packet stream) {
        int totalOperations = stream.readUByte();
        int textureFileCount = 0;
        int materialFileCount = 0;
        int[][] operationIndices = new int[totalOperations][];
        this.textureOperations = new TextureOperation[totalOperations];
        for (int index = 0; index < totalOperations; index++) {
        	TextureOperation textureOperation = TextureTypeList.listOperation(stream);
            if (textureOperation.getTextureId() >= 0) {
                textureFileCount++;
            }
            if (textureOperation.getMaterialId() >= 0) {
                materialFileCount++;
            }
            int operationCount = textureOperation.operations.length;
            operationIndices[index] = new int[operationCount];
            for (int sub = 0; sub < operationCount; sub++) {
                operationIndices[index][sub] = stream.readUByte();
            }
            this.textureOperations[index] = textureOperation;
        }
        this.textureFileIds = new int[textureFileCount];
        textureFileCount = 0;
        this.materialFileIds = new int[materialFileCount];
        materialFileCount = 0;
        for (int operationIndex = 0; operationIndex < totalOperations; operationIndex++) {
        	TextureOperation textureOperation = this.textureOperations[operationIndex];
            int operationCount = textureOperation.operations.length;
            for (int sub = 0; sub < operationCount; sub++) {
                textureOperation.operations[sub] = this.textureOperations[operationIndices[operationIndex][sub]];
            }
            int textureFileId = textureOperation.getTextureId();
            int materialFileId = textureOperation.getMaterialId();
            if (textureFileId > 0) {
                this.textureFileIds[textureFileCount++] = textureFileId;
            }
            if (materialFileId > 0) {
                this.materialFileIds[materialFileCount++] = materialFileId;
            }
            operationIndices[operationIndex] = null;
        }
        this.opaqueOperation = this.textureOperations[stream.readUByte()];
        this.opacityOperation = this.textureOperations[stream.readUByte()];
        this.hdrOperation = this.textureOperations[stream.readUByte()];
    }

    public int[] getPixelsRgb(Js5ResourceProvider textureSpriteResource, MaterialProvider materialProvider, double brightness, int width, int height, boolean readReversed, boolean writeReversed) {
        TextureTypeList.textureSpriteResource = textureSpriteResource;
        TextureTypeList.materialProvider = materialProvider;
        for (int index = 0; index < this.textureOperations.length; index++) {
            this.textureOperations[index].createImageCache(width, height);
        }
        TextureTypeList.setBrightness(brightness);
        TextureRaster.setDimensions(width, height);
        int[] texturePixels = new int[width * height];
        int startReadingFrom;
        int pixelCount;
        byte pixelOffset;
        if (readReversed) {
            startReadingFrom = width - 1;
            pixelCount = -1;
            pixelOffset = -1;
        } else {
            startReadingFrom = 0;
            pixelCount = width;
            pixelOffset = 1;
        }
        int writePosition = 0;
        for (int row = 0; row < height; row++) {
            int[] colorsRed;
            int[] colorsGreen;
            int[] colorsBlue;
            if (this.opaqueOperation.monochrome) {
                int[] palette = this.opaqueOperation.getMonochromeOutput(row);
                colorsRed = palette;
                colorsGreen = palette;
                colorsBlue = palette;
            } else {
                int[][] palette = this.opaqueOperation.getColorOutput(row);
                colorsRed = palette[0];
                colorsGreen = palette[1];
                colorsBlue = palette[2];
            }
            if (writeReversed) {
                writePosition = row;
            }
            for (int pixelIndex = startReadingFrom; pixelIndex != pixelCount; pixelIndex += pixelOffset) {
                int red = colorsRed[pixelIndex] >> 4;
                if (red > 255) {
                    red = 255;
                }
                if (red < 0) {
                    red = 0;
                }
                int green = colorsGreen[pixelIndex] >> 4;
                if (green > 255) {
                    green = 255;
                }
                if (green < 0) {
                    green = 0;
                }
                int blue = colorsBlue[pixelIndex] >> 4;
                if (blue > 255) {
                    blue = 255;
                }
                if (blue < 0) {
                    blue = 0;
                }
                red = TextureTypeList.BRIGHTNESS_TABLE[red];
                green = TextureTypeList.BRIGHTNESS_TABLE[green];
                blue = TextureTypeList.BRIGHTNESS_TABLE[blue];
                int pixel = blue + (green << 8) + (red << 16);
                if (pixel != 0) {
                    pixel |= -16777216;
                }
                texturePixels[writePosition++] = pixel;
                if (writeReversed) {
                    writePosition += width - 1;
                }
            }
        }
        for (int index = 0; index < this.textureOperations.length; index++) {
            this.textureOperations[index].clearImageCache();
        }
        return texturePixels;
    }

    public int[] getPixelsArgb(Js5ResourceProvider textureSprites, MaterialProvider materialProvider, int rasterWidth, int rasterHeight, boolean writeReversed) {
        TextureTypeList.textureSpriteResource = textureSprites;
        TextureTypeList.materialProvider = materialProvider;
        for (int index = 0; index < this.textureOperations.length; index++) {
            this.textureOperations[index].createImageCache(rasterWidth, rasterHeight);
        }
        TextureTypeList.setBrightness(0.7);
        TextureRaster.setDimensions(rasterWidth, rasterHeight);
        int[] pixelColorBuffer = new int[rasterWidth * rasterHeight];
        int pixelIndex = 0;
        for (int row = 0; row < rasterHeight; row++) {
            int[] redBuffer;
            int[] greenBuffer;
            int[] blueBuffer;
            if (this.opaqueOperation.monochrome) {
                int[] buffer = this.opaqueOperation.getMonochromeOutput(row);
                redBuffer = buffer;
                greenBuffer = buffer;
                blueBuffer = buffer;
            } else {
                int[][] palette2d = this.opaqueOperation.getColorOutput(row);
                redBuffer = palette2d[0];
                greenBuffer = palette2d[1];
                blueBuffer = palette2d[2];
            }
            int[] opacity;
            if (this.opacityOperation.monochrome) {
                opacity = this.opacityOperation.getMonochromeOutput(row);
            } else {
                opacity = this.opacityOperation.getColorOutput(row)[0];
            }
            if (writeReversed) {
                pixelIndex = row;
            }
            for (int x = rasterWidth - 1; x >= 0; --x) {
                int red = redBuffer[x] >> 4;
                if (red > 255) {
                    red = 255;
                }
                if (red < 0) {
                    red = 0;
                }
                int green = greenBuffer[x] >> 4;
                if (green > 255) {
                    green = 255;
                }
                if (green < 0) {
                    green = 0;
                }
                int blue = blueBuffer[x] >> 4;
                if (blue > 255) {
                    blue = 255;
                }
                if (blue < 0) {
                    blue = 0;
                }
                red = TextureTypeList.BRIGHTNESS_TABLE[red];
                green = TextureTypeList.BRIGHTNESS_TABLE[green];
                blue = TextureTypeList.BRIGHTNESS_TABLE[blue];
                int alpha;
                if (red == 0 && green == 0 && blue == 0) {
                    alpha = 0;
                } else {
                    alpha = opacity[x] >> 4;
                    if (alpha > 255) {
                        alpha = 255;
                    }
                    if (alpha < 0) {
                        alpha = 0;
                    }
                }
                pixelColorBuffer[pixelIndex++] = blue + (green << 8) + (alpha << 24) + (red << 16);
                if (writeReversed) {
                    pixelIndex += rasterWidth - 1;
                }
            }
        }
        for (int index = 0; index < this.textureOperations.length; index++) {
            this.textureOperations[index].clearImageCache();
        }
        return pixelColorBuffer;
    }

    public float[] getPixelsHdr(Js5ResourceProvider textureSpriteResource, MaterialProvider materialProvider, int width, int height, boolean writeReversed) {
        TextureTypeList.textureSpriteResource = textureSpriteResource;
        TextureTypeList.materialProvider = materialProvider;
        for (int index = 0; index < this.textureOperations.length; index++) {
            this.textureOperations[index].createImageCache(width, height);
        }
        TextureRaster.setDimensions(width, height);
        float[] pixelsHdr = new float[width * height * 4];
        int bufferPosition = 0;
        for (int row = 0; row < height; row++) {
            int[] colorsRed;
            int[] colorsGreen;
            int[] colorsBlue;
            if (this.opaqueOperation.monochrome) {
            	int[] uniform = this.opaqueOperation.getMonochromeOutput(row);
                colorsRed = uniform;
                colorsGreen = uniform;
                colorsBlue = uniform;
            } else {
                int[][] spectrum = this.opaqueOperation.getColorOutput(row);
                colorsRed = spectrum[0];
                colorsGreen = spectrum[1];
                colorsBlue = spectrum[2];
            }
            int[] opacityBuffer;
            if (this.opacityOperation.monochrome) {
                opacityBuffer = this.opacityOperation.getMonochromeOutput(row);
            } else {
                opacityBuffer = this.opacityOperation.getColorOutput(row)[0];
            }
            int[] hdrBuffer;
            if (this.hdrOperation.monochrome) {
                hdrBuffer = this.hdrOperation.getMonochromeOutput(row);
            } else {
                hdrBuffer = this.hdrOperation.getColorOutput(row)[0];
            }
            if (writeReversed) {
                bufferPosition = row << 2;
            }
            for (int x = width - 1; x >= 0; --x) {
                float opacity = opacityBuffer[x] / 4096.0F;
                float intensity = (1.0F + hdrBuffer[x] * 31.0F / 4096.0F) / 4096.0F;
                if (opacity < 0.0F) {
                    opacity = 0.0F;
                } else if (opacity > 1.0F) {
                    opacity = 1.0F;
                }
                pixelsHdr[bufferPosition++] = colorsRed[x] * intensity;
                pixelsHdr[bufferPosition++] = colorsGreen[x] * intensity;
                pixelsHdr[bufferPosition++] = colorsBlue[x] * intensity;
                pixelsHdr[bufferPosition++] = opacity;
                if (writeReversed) {
                    bufferPosition += (width << 2) - 4;
                }
            }
        }
        for (int index = 0; index < this.textureOperations.length; index++) {
            this.textureOperations[index].clearImageCache();
        }
        return pixelsHdr;
    }
    
	public boolean isCached(Js5ResourceProvider spriteResource, MaterialProvider materialprovider) {
        int fileIndex;
        if (TextureTypeList.TEXTURE_GROUP_ID >= 0) {
            for (fileIndex = 0; fileIndex < this.textureFileIds.length; fileIndex++) {
                if (!spriteResource.isCached(TextureTypeList.TEXTURE_GROUP_ID, this.textureFileIds[fileIndex])) {
                    return false;
                }
            }
        } else {
            for (fileIndex = 0; fileIndex < this.textureFileIds.length; fileIndex++) {
                if (!spriteResource.isFileCached(this.textureFileIds[fileIndex])) {
                    return false;
                }
            }
        }
        for (fileIndex = 0; fileIndex < this.materialFileIds.length; fileIndex++) {
            if (!materialprovider.isCached(this.materialFileIds[fileIndex])) {
                return false;
            }
        }
        return true;
    }

}
