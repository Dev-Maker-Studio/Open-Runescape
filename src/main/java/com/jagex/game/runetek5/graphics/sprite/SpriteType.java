package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.utils.ArrayUtils;

public class SpriteType {

    public int minX;
    public int minY;
    public int width;
    public int height;
    public int borderX;
    public int borderY;
    public int[] pallete;
    public byte[] pixels;
    public byte[] alpha;
    
    public static SpriteType listSingle(Js5ResourceProvider resource, int archiveId, int fileId) {
        byte[] data = resource.getFile(archiveId, fileId);
        return data == null ? null : decode(data)[0];
    }

    public static SpriteType listSingle(Js5ResourceProvider resource, int fileId) {
        byte[] data = resource.getFile(fileId);
        return data == null ? null : decode(data)[0];
    }
    
    public static SpriteType[] listGroup(Js5ResourceProvider resource, int fileId) {
        byte[] data = resource.getFile(fileId);
        return data == null ? null : decode(data);
    }
    
    public static SpriteType[] decodeGroup(Js5ResourceProvider resource, int archive) {
        byte[] data = resource.getFile(archive, 0);
        return data == null ? null : decode(data);
    }

    public static SpriteType[] decode(byte[] data) {
        Packet stream = new Packet(data);
        stream.currentPosition = data.length - 2;
        int count = stream.readUShort();
        SpriteType[] sprites = new SpriteType[count];

        for (int i = 0; i < count; i++) {
            sprites[i] = new SpriteType();
        }

        stream.currentPosition = data.length - 7 - count * 8;
        int biggestWidth = stream.readUShort();
        int biggestHeight = stream.readUShort();
        int palleteLength = (stream.readUByte() & 0xff) + 1;

        for (int i = 0; i < count; i++) {
            sprites[i].minX = stream.readUShort();
        }

        for (int i = 0; i < count; i++) {
            sprites[i].minY = stream.readUShort();
        }

        for (int i = 0; i < count; i++) {
            sprites[i].width = stream.readUShort();
        }

        for (int i = 0; i < count; i++) {
            sprites[i].height = stream.readUShort();
        }

        for (int i = 0; i < count; i++) {
            SpriteType sprite = sprites[i];
            sprite.borderX = biggestWidth - sprite.width - sprite.minX;
            sprite.borderY = biggestHeight - sprite.height - sprite.minY;
        }

        stream.currentPosition = data.length - 7 - count * 8 - (palleteLength - 1) * 3;
        int[] pallete = new int[palleteLength];

        for (int i = 1; i < palleteLength; i++) {
            pallete[i] = stream.readUMedium();
            if (pallete[i] == 0) {
                pallete[i] = 1;
            }
        }

        for (int i = 0; i < count; i++) {
            sprites[i].pallete = pallete;
        }

        stream.currentPosition = 0;

        for (int i = 0; i < count; i++) {
            SpriteType sprite = sprites[i];
            int numPixels = sprite.width * sprite.height;
            sprite.pixels = new byte[numPixels];
            int flags = stream.readUByte();
            if ((flags & 0x2) == 0) {
                if ((flags & 0x1) == 0) {
                    for (int j = 0; j < numPixels; j++) {
                        sprite.pixels[j] = stream.readByte();
                    }
                } else {
                    for (int j = 0; j < sprite.width; j++) {
                        for (int k = 0; k < sprite.height; k++) {
                            sprite.pixels[j + k * sprite.width] = stream.readByte();
                        }
                    }
                }
            } else {
                boolean usesAlpha = false;
                sprite.alpha = new byte[numPixels];
                if ((flags & 0x1) == 0) {
                    for (int j = 0; j < numPixels; j++) {
                        sprite.pixels[j] = stream.readByte();
                    }

                    for (int j = 0; j < numPixels; j++) {
                        byte alpha = sprite.alpha[j] = stream.readByte();
                        usesAlpha |= alpha != -1;
                    }
                } else {
                    for (int j = 0; j < sprite.width; j++) {
                        for (int k = 0; k < sprite.height; k++) {
                            sprite.pixels[j + k * sprite.width] = stream.readByte();
                        }
                    }

                    for (int j = 0; j < sprite.width; j++) {
                        for (int k = 0; k < sprite.height; k++) {
                            byte alpha = sprite.alpha[j + k * sprite.width] = stream.readByte();
                            usesAlpha |= alpha != -1;
                        }
                    }
                }

                if (!usesAlpha) {
                    sprite.alpha = null;
                }
            }
        }

        return sprites;
    }

    public int widthSum() {
        return this.width + this.minX + this.borderX;
    }

    public int heightSum() {
        return this.height + this.minY + this.borderY;
    }

    public void method1523() {
        int w = widthSum();
        int h = heightSum();
        if (w != this.width || h != this.height) {
            byte[] replacementPixels = new byte[w * h];
            int currentH;
            int pixelIndex;
            int i_7;
            if (this.alpha != null) {
                byte[] replacementAlpha = new byte[w * h];
                currentH = 0;
                while (true) {
                    if (currentH >= this.height) {
                        this.alpha = replacementAlpha;
                        break;
                    }
                    pixelIndex = currentH * this.width;
                    i_7 = w * (currentH + this.minY) + this.minX;
                    for (int i_8 = 0; i_8 < this.width; i_8++) {
                        replacementPixels[i_7] = this.pixels[pixelIndex];
                        replacementAlpha[i_7++] = this.alpha[pixelIndex++];
                    }
                    ++currentH;
                }
            } else {
                for (int i_9 = 0; i_9 < this.height; i_9++) {
                    currentH = i_9 * this.width;
                    pixelIndex = w * (i_9 + this.minY) + this.minX;

                    for (i_7 = 0; i_7 < this.width; i_7++) {
                        replacementPixels[pixelIndex++] = this.pixels[currentH++];
                    }
                }
            }

            this.borderY = 0;
            this.minY = 0;
            this.borderX = 0;
            this.minX = 0;
            this.width = w;
            this.height = h;
            this.pixels = replacementPixels;
        }

    }

    public void outline(int outlineColor) {
        int finalPaletteIndex = -1;
        if (this.pallete.length < 255) {
            for (int paletteIndex = 0; paletteIndex < this.pallete.length; paletteIndex++) {
                if (this.pallete[paletteIndex] == outlineColor) {
                    finalPaletteIndex = paletteIndex;
                    break;
                }
            }
            if (finalPaletteIndex == -1) {
                finalPaletteIndex = this.pallete.length;
                int[] replacementPalette = new int[this.pallete.length + 1];
                ArrayUtils.arrayCopy(this.pallete, 0, replacementPalette, 0, this.pallete.length);
                this.pallete = replacementPalette;
                replacementPalette[finalPaletteIndex] = outlineColor;
            }
        } else {
            int minDeltaSum = Integer.MAX_VALUE;
            int red = outlineColor >> 16 & 0xff;
            int green = outlineColor >> 8 & 0xff;
            int blue = outlineColor & 0xff;
            for (int paletteIndex = 0; paletteIndex < this.pallete.length; paletteIndex++) {
                int currentColorRgb = this.pallete[paletteIndex];
                int currentRed = currentColorRgb >> 16 & 0xff;
                int currentGreen = currentColorRgb >> 8 & 0xff;
                int currentBlue = currentColorRgb & 0xff;
                int redDelta = red - currentRed;
                if (redDelta < 0) {
                    redDelta = -redDelta;
                }
                int greenDelta = green - currentGreen;
                if (greenDelta < 0) {
                    greenDelta = -greenDelta;
                }
                int blueDelta = blue - currentBlue;
                if (blueDelta < 0) {
                    blueDelta = -blueDelta;
                }
                int deltaSum = redDelta + greenDelta + blueDelta;
                if (deltaSum < minDeltaSum) {
                    minDeltaSum = deltaSum;
                    finalPaletteIndex = paletteIndex;
                }
            }
        }
        for (int h = this.height - 1; h > 0; --h) {
        	int basepos = h * this.width;
            for (int w = this.width - 1; w > 0; --w) {
                if (this.pallete[this.pixels[w + basepos] & 0xff] == 0 && this.pallete[this.pixels[w + basepos - 1 - this.width] & 0xff] != 0) {
                    this.pixels[w + basepos] = (byte) finalPaletteIndex;
                }
            }
        }
    }

    public void flipHorizontal() {
        byte[] bytes_1 = this.pixels;
        int i_2;
        int i_3;
        int i_4;
        if (this.alpha == null) {
            for (i_2 = this.height - 1; i_2 >= 0; --i_2) {
                i_3 = i_2 * this.width;

                for (i_4 = (i_2 + 1) * this.width; i_3 < i_4; i_3++) {
                    --i_4;
                    byte b_5 = bytes_1[i_3];
                    bytes_1[i_3] = bytes_1[i_4];
                    bytes_1[i_4] = b_5;
                }
            }
        } else {
            byte[] bytes_7 = this.alpha;

            for (i_3 = this.height - 1; i_3 >= 0; --i_3) {
                i_4 = i_3 * this.width;

                for (int i_8 = (i_3 + 1) * this.width; i_4 < i_8; i_4++) {
                    --i_8;
                    byte b_6 = bytes_1[i_4];
                    bytes_1[i_4] = bytes_1[i_8];
                    bytes_1[i_8] = b_6;
                    b_6 = bytes_7[i_4];
                    bytes_7[i_4] = bytes_7[i_8];
                    bytes_7[i_8] = b_6;
                }
            }
        }

        i_2 = this.minX;
        this.minX = this.borderX;
        this.borderX = i_2;
    }

    public void flipVertical() {
        byte[] bytes_1 = this.pixels;
        int i_2;
        int i_3;
        int i_4;
        int i_5;
        if (this.alpha == null) {
            for (i_2 = (this.height >> 1) - 1; i_2 >= 0; --i_2) {
                i_3 = i_2 * this.width;
                i_4 = (this.height - i_2 - 1) * this.width;

                for (i_5 = -this.width; i_5 < 0; i_5++) {
                    byte b_6 = bytes_1[i_3];
                    bytes_1[i_3] = bytes_1[i_4];
                    bytes_1[i_4] = b_6;
                    ++i_3;
                    ++i_4;
                }
            }
        } else {
            byte[] bytes_8 = this.alpha;

            for (i_3 = (this.height >> 1) - 1; i_3 >= 0; --i_3) {
                i_4 = i_3 * this.width;
                i_5 = (this.height - i_3 - 1) * this.width;

                for (int i_9 = -this.width; i_9 < 0; i_9++) {
                    byte b_7 = bytes_1[i_4];
                    bytes_1[i_4] = bytes_1[i_5];
                    bytes_1[i_5] = b_7;
                    b_7 = bytes_8[i_4];
                    bytes_8[i_4] = bytes_8[i_5];
                    bytes_8[i_5] = b_7;
                    ++i_4;
                    ++i_5;
                }
            }
        }

        i_2 = this.minY;
        this.minY = this.borderY;
        this.borderY = i_2;
    }

    public void rotate90DegLeft() {
        byte[] bytes_1 = new byte[this.width * this.height];
        int i_2 = 0;
        int i_3;
        int i_4;
        if (this.alpha == null) {
            for (i_3 = 0; i_3 < this.width; i_3++) {
                for (i_4 = this.height - 1; i_4 >= 0; --i_4) {
                    bytes_1[i_2++] = this.pixels[i_3 + i_4 * this.width];
                }
            }

            this.pixels = bytes_1;
        } else {
            byte[] bytes_6 = new byte[this.width * this.height];

            for (i_4 = 0; i_4 < this.width; i_4++) {
                for (int i_5 = this.height - 1; i_5 >= 0; --i_5) {
                    bytes_1[i_2] = this.pixels[i_4 + i_5 * this.width];
                    bytes_6[i_2++] = this.alpha[i_4 + i_5 * this.width];
                }
            }

            this.pixels = bytes_1;
            this.alpha = bytes_6;
        }

        i_3 = this.minY;
        this.minY = this.minX;
        this.minX = this.borderY;
        this.borderY = this.borderX;
        this.borderX = this.minY;
        i_3 = this.height;
        this.height = this.width;
        this.width = i_3;
    }
    
    public int[] getPixels() {
        int currentWidth = widthSum();
        int[] pixelBuffer = new int[currentWidth * heightSum()];
        if (this.alpha != null) {
            for (int y = 0; y < this.height; y++) {
            	int palettePixelIndex = y * this.width;
            	int pixelCount = currentWidth * (y + this.minY) + this.minX;
                for (int x = 0; x < this.width; x++) {
                    pixelBuffer[pixelCount++] = this.alpha[palettePixelIndex] << 24 | this.pallete[this.pixels[palettePixelIndex] & 0xff];
                    palettePixelIndex++;
                }
            }
        } else {
            for (int y = 0; y < this.height; y++) {
            	int palettePixelIndex = y * this.width;
            	int pixelCount = currentWidth * (y + this.minY) + this.minX;
                for (int x = 0; x < this.width; x++) {
                    int color = this.pallete[this.pixels[palettePixelIndex++] & 0xff];
                    if (color != 0) {
                        pixelBuffer[pixelCount++] = ~0xffffff | color;
                    } else {
                        pixelBuffer[pixelCount++] = 0;
                    }
                }
            }
        }
        return pixelBuffer;
    }

    public void offsetRgbColor(int redOffset, int greenOffset, int blueOffset) {
        for (int paletteIndex = 1; paletteIndex < this.pallete.length; paletteIndex++) {
            if (this.pallete[paletteIndex] != 1 && this.pallete[paletteIndex] != 16711935) {
                int r = this.pallete[paletteIndex] >> 16 & 0xff;
                r += redOffset;
                if (r < 0) {
                    r = 0;
                } else if (r > 255) {
                    r = 255;
                }

                int g = this.pallete[paletteIndex] >> 8 & 0xff;
                g += greenOffset;
                if (g < 0) {
                    g = 0;
                } else if (g > 255) {
                    g = 255;
                }

                int b = this.pallete[paletteIndex] & 0xff;
                b += blueOffset;
                if (b < 0) {
                    b = 0;
                } else if (b > 255) {
                    b = 255;
                }
                this.pallete[paletteIndex] = r << 16 | g << 8 | b;
            }
        }
    }

    public void drawThickBorder(int color) {
        int lowestRgbIndex = -1;
        if (this.pallete.length < 255) {
            for (int paletteIndex = 0; paletteIndex < this.pallete.length; paletteIndex++) {
                if (this.pallete[paletteIndex] == color) {
                    lowestRgbIndex = paletteIndex;
                    break;
                }
            }
            if (lowestRgbIndex == -1) {
                lowestRgbIndex = this.pallete.length;
                int[] copiedPalette = new int[this.pallete.length + 1];
                ArrayUtils.arrayCopy(this.pallete, 0, copiedPalette, 0, this.pallete.length);
                this.pallete = copiedPalette;
                copiedPalette[lowestRgbIndex] = color;
            }
        } else {
        	int lowestRgb = Integer.MAX_VALUE;
            int originalRed = color >> 16 & 0xff;
            int originalGreen = color >> 8 & 0xff;
            int originalBlue = color & 0xff;

            for (int index = 0; index < this.pallete.length; index++) {
                int paletteColor = this.pallete[index];
                int red = paletteColor >> 16 & 0xff;
                int green = paletteColor >> 8 & 0xff;
                int blue = paletteColor & 0xff;
                int redDelta = originalRed - red;
                if (redDelta < 0) {
                    redDelta = -redDelta;
                }

                int greenDelta = originalGreen - green;
                if (greenDelta < 0) {
                    greenDelta = -greenDelta;
                }

                int blueDelta = originalBlue - blue;
                if (blueDelta < 0) {
                    blueDelta = -blueDelta;
                }

                int rgbSum = redDelta + greenDelta + blueDelta;
                if (rgbSum < lowestRgb) {
                    lowestRgb = rgbSum;
                    lowestRgbIndex = index;
                }
            }
        }
        int replacementPixelIndex = 0;
        byte[] replacementPixels = new byte[this.width * this.height];
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int paletteIndex = this.pixels[replacementPixelIndex] & 0xff;
                if (this.pallete[paletteIndex] == 0) {
                    if (x > 0 && this.pallete[this.pixels[replacementPixelIndex - 1] & 0xff] != 0) {
                        paletteIndex = lowestRgbIndex;
                    } else if (y > 0 && this.pallete[this.pixels[replacementPixelIndex - this.width] & 0xff] != 0) {
                        paletteIndex = lowestRgbIndex;
                    } else if (x < this.width - 1 && this.pallete[this.pixels[replacementPixelIndex + 1] & 0xff] != 0) {
                        paletteIndex = lowestRgbIndex;
                    } else if (y < this.height - 1 && this.pallete[this.pixels[replacementPixelIndex + this.width] & 0xff] != 0) {
                        paletteIndex = lowestRgbIndex;
                    }
                }
                replacementPixels[replacementPixelIndex++] = (byte) paletteIndex;
            }
        }
        this.pixels = replacementPixels;
    }

    public void drawSmallBorder(int borderThickness) {
        int i_2 = widthSum();
        int i_3 = heightSum();
        if (i_2 != this.width || i_3 != this.height) {
            int i_4 = borderThickness;
            if (borderThickness > this.minX) {
                i_4 = this.minX;
            }

            int i_5 = borderThickness;
            if (borderThickness + this.minX + this.width > i_2) {
                i_5 = i_2 - this.minX - this.width;
            }

            int i_6 = borderThickness;
            if (borderThickness > this.minY) {
                i_6 = this.minY;
            }

            int i_7 = borderThickness;
            if (borderThickness + this.minY + this.height > i_3) {
                i_7 = i_3 - this.minY - this.height;
            }

            int i_8 = i_4 + i_5 + this.width;
            int i_9 = i_6 + i_7 + this.height;
            byte[] bytes_10 = new byte[i_8 * i_9];
            int i_12;
            int i_13;
            int i_14;
            if (this.alpha == null) {
                for (int i_11 = 0; i_11 < this.height; i_11++) {
                    i_12 = i_11 * this.width;
                    i_13 = i_8 * (i_11 + i_6) + i_4;

                    for (i_14 = 0; i_14 < this.width; i_14++) {
                        bytes_10[i_13++] = this.pixels[i_12++];
                    }
                }
            } else {
                byte[] bytes_16 = new byte[i_8 * i_9];
                i_12 = 0;

                while (true) {
                    if (i_12 >= this.height) {
                        this.alpha = bytes_16;
                        break;
                    }

                    i_13 = i_12 * this.width;
                    i_14 = i_8 * (i_12 + i_6) + i_4;

                    for (int i_15 = 0; i_15 < this.width; i_15++) {
                        bytes_16[i_14] = this.alpha[i_13];
                        bytes_10[i_14++] = this.pixels[i_13++];
                    }

                    ++i_12;
                }
            }

            this.minX -= i_4;
            this.minY -= i_6;
            this.borderX -= i_5;
            this.borderY -= i_7;
            this.width = i_8;
            this.height = i_9;
            this.pixels = bytes_10;
        }

    }

}
