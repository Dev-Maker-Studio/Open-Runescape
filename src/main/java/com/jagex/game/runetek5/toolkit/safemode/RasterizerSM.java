package com.jagex.game.runetek5.toolkit.safemode;

import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;

/**
 * Only used for safemode
 * @author Brainpower
 *
 */
public class RasterizerSM {

    public float centerX;
    public float offsetX;
    public float centerY;
    public int clipWidth;
    public int clipHeight;
    public float offsetY;
    
    /**
     * Draws the lines around the characters, triangles
     * flawed code though
     */
    public boolean wireframeMode;
    public int fogColour;
    public int[] texturePixelsA;
    public float textureScaleA;
    public int blendingAlpha;
    public int[] texturePixelsB;
    public float textureScaleB;
    public int[] texturePixelsC;
    public float textureScaleC;
    public int textureSizeA;
    public int textureMaskA;
    public int alphaMode;
    public int textureSizeB;
    public int textureMaskB;
    public int textureSizeC;
    public int textureMaskC;
    
    /**
     * Always false
     */
    public boolean blending;
    
    public boolean smoothEdges = true;
    public boolean aBool1676;
    public float aFloat1682;
    public float aFloat1683 = 1.0F;
    public final int[] scanlineOffsets = new int[4096];
    public final int textureB = -1;
    public final int textureA = -1;
    public boolean textureRepeat = true;
    public final int textureC = -1;
    public boolean clipEdges;
    public final SafeModeToolkit safemodeToolkit;
    public final ThreadResourceSM threadResource;
    public final int scanlineWidth;
    public final int[] colourBuffer;
    public final float[] depthBuffer;

    public RasterizerSM(SafeModeToolkit hardwarerenderer1, ThreadResourceSM class1852) {
        this.safemodeToolkit = hardwarerenderer1;
        this.threadResource = class1852;
        this.scanlineWidth = this.safemodeToolkit.viewportWidth;
        this.colourBuffer = this.safemodeToolkit.colourBuffer;
        this.depthBuffer = this.safemodeToolkit.depthBuffer;
    }

    public int getCurrentColumn() {
        return this.scanlineOffsets[0] % this.scanlineWidth;
    }

    public int getCurrentRow() {
        return this.scanlineOffsets[0] / this.scanlineWidth;
    }

    public void setWireframeMode(boolean wireframeMode) {
        this.wireframeMode = wireframeMode;
    }

    
    public void drawFlatScanline(final boolean colorWrite, final boolean depthWrite, final boolean transparent, final int[] pixels, int bufferOffset, int color, int xA, int xB, float depth, final float depthOffset) {
        int x = 0;
        if (this.clipEdges) {
            if (xB > this.clipWidth) {
                xB = this.clipWidth;
            }
            if (xA < 0) {
                xA = 0;
            }
        }
        if (xA < xB) {
            bufferOffset += xA - 1;
            x = xB - xA >> 2;
            depth += depthOffset * xA;
            if (this.threadResource.zbuffering) {
                if (this.blendingAlpha == 0) {
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color;
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color;
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color;
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color;
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                    }
                    x = (xB - xA & 0x3);
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color;
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                    }
                    return;
                }
                if (this.blendingAlpha != 254) {
                    final int alpha = this.blendingAlpha;
                    final int invertedAlpha = 256 - this.blendingAlpha;
                    color = ((color & 0xFF00FF) * invertedAlpha >> 8 & 0xFF00FF) + (invertedAlpha * (color & 0xFF00) >> 8 & 0xFF00);
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                final int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                                }
                                else {
                                    pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                                }
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                final int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                                }
                                else {
                                    pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                                }
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                final int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                                }
                                else {
                                    pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                                }
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                final int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                                }
                                else {
                                    pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                                }
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                    }
                    x = (xB - xA & 0x3);
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            if (colorWrite) {
                                final int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                                }
                                else {
                                    pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                                }
                            }
                            if (depthWrite) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthOffset;
                    }
                    return;
                }
                if (xA != 0 && xB <= this.clipWidth - 1) {
                    while (--x >= 0) {
                        bufferOffset++;
                        if ((!depthWrite || depth < this.depthBuffer[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if ((!depthWrite || depth < this.depthBuffer[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if ((!depthWrite || depth < this.depthBuffer[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if ((!depthWrite || depth < this.depthBuffer[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                    }
                    x = (xB - xA & 0x3);
                    while (--x >= 0) {
                        bufferOffset++;
                        if ((!depthWrite || depth < this.depthBuffer[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                    }
                }
            }
            else if (colorWrite) {
                if (this.blendingAlpha == 0) {
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = color;
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = color;
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = color;
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = color;
                        }
                        depth += depthOffset;
                    }
                    x = (xB - xA & 0x3);
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = color;
                        }
                        depth += depthOffset;
                    }
                    return;
                }
                if (this.blendingAlpha != 254) {
                    final int alpha = this.blendingAlpha;
                    final int invertedAlpha = 256 - this.blendingAlpha;
                    color = ((color & 0xFF00FF) * invertedAlpha >> 8 & 0xFF00FF) + (invertedAlpha * (color & 0xFF00) >> 8 & 0xFF00);
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            final int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                            }
                            else {
                                pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            final int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                            }
                            else {
                                pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            final int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                            }
                            else {
                                pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                            }
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            final int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                            }
                            else {
                                pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                            }
                        }
                        depth += depthOffset;
                    }
                    x = (xB - xA & 0x3);
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            final int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = ((invertedAlpha | pixel >> 24) << 24 | ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color);
                            }
                            else {
                                pixels[bufferOffset] = ((pixel & 0xFF00FF) * alpha >> 8 & 0xFF00FF) + (alpha * (pixel & 0xFF00) >> 8 & 0xFF00) + color;
                            }
                        }
                        depth += depthOffset;
                    }
                    return;
                }
                if (xA != 0 && xB <= this.clipWidth - 1) {
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                    }
                    x = (xB - xA & 0x3);
                    while (--x >= 0) {
                        bufferOffset++;
                        if (!depthWrite || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset];
                        }
                        depth += depthOffset;
                    }
                }
            }
        }
    }

    
    public void drawMultiMaterializedTriangle(float yA, float yB, float yC, float xA, float xB, float xC, float zNearA, float zNearB, float zNearC, float zFarA, float zFarB, float zFarC, float uA, float uB, float uC, float vA, float vB, float vC, int floorColorA, int floorColorB, int floorColorC, int fogColor, float fogDensityA, float fogDensityB, float fogDensityC, int materialA, float scaleA, int materialB, float scaleB, int materialC, float scaleC) {
        this.fogColour = fogColor;
        if (yA > yB || yA > yC) {
            if (yB <= yC) {
            	float tmpFloat = xA;
                xA = xB;
                xB = tmpFloat;
                tmpFloat = yA;
                yA = yB;
                yB = tmpFloat;
                tmpFloat = zNearA;
                zNearA = zNearB;
                zNearB = tmpFloat;
                tmpFloat = uA;
                uA = uB;
                uB = tmpFloat;
                tmpFloat = vA;
                vA = vB;
                vB = tmpFloat;
                tmpFloat = zFarA;
                zFarA = zFarB;
                zFarB = tmpFloat;
                tmpFloat = fogDensityA;
                fogDensityA = fogDensityB;
                fogDensityB = tmpFloat;
                tmpFloat = scaleA;
                scaleA = scaleB;
                scaleB = tmpFloat;
                int tmpInt = floorColorA;
                floorColorA = floorColorB;
                floorColorB = tmpInt;
                tmpInt = materialA;
                materialA = materialB;
                materialB = tmpInt;
            } else {
            	float tmpFloat = xA;
                xA = xC;
                xC = tmpFloat;
                tmpFloat = yA;
                yA = yC;
                yC = tmpFloat;
                tmpFloat = zNearA;
                zNearA = zNearC;
                zNearC = tmpFloat;
                tmpFloat = uA;
                uA = uC;
                uC = tmpFloat;
                tmpFloat = vA;
                vA = vC;
                vC = tmpFloat;
                tmpFloat = zFarA;
                zFarA = zFarC;
                zFarC = tmpFloat;
                tmpFloat = fogDensityA;
                fogDensityA = fogDensityC;
                fogDensityC = tmpFloat;
                tmpFloat = scaleA;
                scaleA = scaleC;
                scaleC = tmpFloat;
                int tmpInt = floorColorA;
                floorColorA = floorColorC;
                floorColorC = tmpInt;
                tmpInt = materialA;
                materialA = materialC;
                materialC = tmpInt;
            }
        }
        
        /** Texture a **/
        if (materialA != this.textureA) {
            this.texturePixelsA = this.safemodeToolkit.getAnimatedMaterialArgb(materialA);
            if (this.texturePixelsA == null) {
                this.blendingAlpha = 255 - (floorColorA >> 24 & 0xff);
                int materialColorRgb = ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(this.safemodeToolkit.getMaterialColor(materialA)) & 0xffff];
                int tmp2 = ~0xffffff | (materialColorRgb >> 16 & 0xff) * (floorColorA >> 16 & 0xff) << 8 & 0xff0000 | (floorColorA >> 8 & 0xff) * (materialColorRgb >> 8 & 0xff) & 0xff00 | (materialColorRgb & 0xff) * (floorColorA & 0xff) >> 8;
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityC));
                return;
            }

            this.textureSizeA = this.safemodeToolkit.isSmallTexture(materialA) ? 64 : this.safemodeToolkit.materialSize;
            this.textureMaskA = this.textureSizeA - 1;
            this.alphaMode = this.safemodeToolkit.getMaterialEffectCombiner(materialA);
        }
        this.textureScaleA = scaleA;
        
        /** Texture b **/
        if (this.textureB != materialB) {
            this.texturePixelsB = this.safemodeToolkit.getAnimatedMaterialArgb(materialB);
            if (this.texturePixelsB == null) {
                this.blendingAlpha = 255 - (floorColorA >> 24 & 0xff);
                int materialColorRgb = ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(this.safemodeToolkit.getMaterialColor(materialB)) & 0xffff];
                int tmp2 = ~0xffffff | (materialColorRgb >> 16 & 0xff) * (floorColorA >> 16 & 0xff) << 8 & 0xff0000 | (floorColorA >> 8 & 0xff) * (materialColorRgb >> 8 & 0xff) & 0xff00 | (materialColorRgb & 0xff) * (floorColorA & 0xff) >> 8;
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityC));
                return;
            }

            this.textureSizeB = this.safemodeToolkit.isSmallTexture(materialB) ? 64 : this.safemodeToolkit.materialSize;
            this.textureMaskB = this.textureSizeB - 1;
        }
        this.textureScaleB = scaleB;
        
        /** Texture c **/
        if (materialC != this.textureC) {
            this.texturePixelsC = this.safemodeToolkit.getAnimatedMaterialArgb(materialC);
            if (this.texturePixelsC == null) {
                this.blendingAlpha = 255 - (floorColorA >> 24 & 0xff);
                int materialColorRgb = ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(this.safemodeToolkit.getMaterialColor(materialC)) & 0xffff];
                int tmp2 = ~0xffffff | (materialColorRgb >> 16 & 0xff) * (floorColorA >> 16 & 0xff) << 8 & 0xff0000 | (floorColorA >> 8 & 0xff) * (materialColorRgb >> 8 & 0xff) & 0xff00 | (materialColorRgb & 0xff) * (floorColorA & 0xff) >> 8;
                drawTriangle(true, true, false, ((int) yA), ((int) yB), ((int) yC), ((int) xA), ((int) xB), ((int) xC), ((int) zNearA), ((int) zNearB), ((int) zNearC), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityC));
                return;
            }

            this.textureSizeC = this.safemodeToolkit.isSmallTexture(materialC) ? 64 : this.safemodeToolkit.materialSize;
            this.textureMaskC = this.textureSizeC - 1;
        }
        this.textureScaleC = scaleC;
        
        uA /= zFarA;
        uB /= zFarB;
        uC /= zFarC;
        vA /= zFarA;
        vB /= zFarB;
        vC /= zFarC;
        zNearA = 1.0F / zNearA;
        zNearB = 1.0F / zNearB;
        zNearC = 1.0F / zNearC;
        zFarA = 1.0F / zFarA;
        zFarB = 1.0F / zFarB;
        zFarC = 1.0F / zFarC;
        float colorAAlpha = (floorColorA >> 24 & 0xff);
        float colorBAlpha = (floorColorB >> 24 & 0xff);
        float colorCAlpha = (floorColorC >> 24 & 0xff);
        float colorARed = (floorColorA >> 16 & 0xff);
        float colorBRed = (floorColorB >> 16 & 0xff);
        float colorCRed = (floorColorC >> 16 & 0xff);
        float colorAGreen = (floorColorA >> 8 & 0xff);
        float colorBGreen = (floorColorB >> 8 & 0xff);
        float colorCGreen = (floorColorC >> 8 & 0xff);
        float colorABlue = (floorColorA & 0xff);
        float colorBBlue = (floorColorB & 0xff);
        float colorCBlue = (floorColorC & 0xff);
        float textureAPercentageA = 1.0F;
        float textureAPercentageB = 0.0F;
        float textureAPercentageC = 0.0F;
        float textureBPercentageA = 0.0F;
        float textureBPercentageB = 1.0F;
        float textureBPercentageC = 0.0F;
        float slopeXBa = 0.0F;
        float slopeZNearBa = 0.0F;
        float slopeZFarBa = 0.0F;
        float slopeUBa = 0.0F;
        float slopeVBa = 0.0F;
        float slopeFogDensityBa = 0.0F;
        float slopeAlphaBa = 0.0F;
        float slopeRedBa = 0.0F;
        float slopeGreenBa = 0.0F;
        float slopeBlueBa = 0.0F;
        float slopeTextureAPercentageBa = 0.0F;
        float slopeTextureBPercentageBa = 0.0F;
        if (yB != yA) {
        	float lengthY = yB - yA;
            slopeXBa = (xB - xA) / lengthY;
            slopeZNearBa = (zNearB - zNearA) / lengthY;
            slopeZFarBa = (zFarB - zFarA) / lengthY;
            slopeUBa = (uB - uA) / lengthY;
            slopeVBa = (vB - vA) / lengthY;
            slopeFogDensityBa = (fogDensityB - fogDensityA) / lengthY;
            slopeAlphaBa = (colorBAlpha - colorAAlpha) / lengthY;
            slopeRedBa = (colorBRed - colorARed) / lengthY;
            slopeGreenBa = (colorBGreen - colorAGreen) / lengthY;
            slopeBlueBa = (colorBBlue - colorABlue) / lengthY;
            slopeTextureAPercentageBa = (textureAPercentageB - textureAPercentageA) / lengthY;
            slopeTextureBPercentageBa = (textureBPercentageB - textureBPercentageA) / lengthY;
        }
        float slopeXCb = 0.0F;
        float slopeZNearCb = 0.0F;
        float slopeZFarCb = 0.0F;
        float slopeUCb = 0.0F;
        float slopeVCb = 0.0F;
        float slopeFogDensityCb = 0.0F;
        float slopeAlphaCb = 0.0F;
        float slopeRedCb = 0.0F;
        float slopeGreenCb = 0.0F;
        float slopeBlueCb = 0.0F;
        float slopeTextureAPercentageCb = 0.0F;
        float slopeTextureBPercentageCb = 0.0F;
        if (yC != yB) {
            float lengthY = yC - yB;
            slopeXCb = (xC - xB) / lengthY;
            slopeZNearCb = (zNearC - zNearB) / lengthY;
            slopeZFarCb = (zFarC - zFarB) / lengthY;
            slopeUCb = (uC - uB) / lengthY;
            slopeVCb = (vC - vB) / lengthY;
            slopeFogDensityCb = (fogDensityC - fogDensityB) / lengthY;
            slopeAlphaCb = (colorCAlpha - colorBAlpha) / lengthY;
            slopeRedCb = (colorCRed - colorBRed) / lengthY;
            slopeGreenCb = (colorCGreen - colorBGreen) / lengthY;
            slopeBlueCb = (colorCBlue - colorBBlue) / lengthY;
            slopeTextureAPercentageCb = (textureAPercentageC - textureAPercentageB) / lengthY;
            slopeTextureBPercentageCb = (textureBPercentageC - textureBPercentageB) / lengthY;
        }
        float slopeXAc = 0.0F;
        float slopeZNearAc = 0.0F;
        float slopeZFarAc = 0.0F;
        float slopeUAc = 0.0F;
        float slopeVAc = 0.0F;
        float slopeFogDensityAc = 0.0F;
        float slopeAlphaAc = 0.0F;
        float slopeRedAc = 0.0F;
        float slopeGreenAc = 0.0F;
        float slopeBlueAc = 0.0F;
        float slopeTextureAPercentageAc = 0.0F;
        float slopeTextureBPercentageAc = 0.0F;
        if (yA != yC) {
            float lengthY = yA - yC;
            slopeXAc = (xA - xC) / lengthY;
            slopeZNearAc = (zNearA - zNearC) / lengthY;
            slopeZFarAc = (zFarA - zFarC) / lengthY;
            slopeUAc = (uA - uC) / lengthY;
            slopeVAc = (vA - vC) / lengthY;
            slopeFogDensityAc = (fogDensityA - fogDensityC) / lengthY;
            slopeAlphaAc = (colorAAlpha - colorCAlpha) / lengthY;
            slopeRedAc = (colorARed - colorCRed) / lengthY;
            slopeGreenAc = (colorAGreen - colorCGreen) / lengthY;
            slopeBlueAc = (colorABlue - colorCBlue) / lengthY;
            slopeTextureAPercentageAc = (textureAPercentageA - textureAPercentageC) / lengthY;
            slopeTextureBPercentageAc = (textureBPercentageA - textureBPercentageC) / lengthY;
        }
        if (yA < this.clipHeight) {
            if (yB > this.clipHeight) {
                yB = this.clipHeight;
            }
            if (yC > this.clipHeight) {
                yC = this.clipHeight;
            }
            if (yB < yC) {
                xC = xA;
                zNearC = zNearA;
                zFarC = zFarA;
                uC = uA;
                vC = vA;
                fogDensityC = fogDensityA;
                colorCAlpha = colorAAlpha;
                colorCRed = colorARed;
                colorCGreen = colorAGreen;
                colorCBlue = colorABlue;
                textureAPercentageC = textureAPercentageA;
                textureBPercentageC = textureBPercentageA;
                if (yA < 0.0F) {
                    xA -= slopeXBa * yA;
                    xC -= slopeXAc * yA;
                    zNearA -= slopeZNearBa * yA;
                    zNearC -= slopeZNearAc * yA;
                    zFarA -= slopeZFarBa * yA;
                    zFarC -= slopeZFarAc * yA;
                    uA -= slopeUBa * yA;
                    uC -= slopeUAc * yA;
                    vA -= slopeVBa * yA;
                    vC -= slopeVAc * yA;
                    fogDensityA -= slopeFogDensityBa * yA;
                    fogDensityC -= slopeFogDensityAc * yA;
                    colorAAlpha -= slopeAlphaBa * yA;
                    colorCAlpha -= slopeAlphaAc * yA;
                    colorARed -= slopeRedBa * yA;
                    colorCRed -= slopeRedAc * yA;
                    colorAGreen -= slopeGreenBa * yA;
                    colorCGreen -= slopeGreenAc * yA;
                    colorABlue -= slopeBlueBa * yA;
                    colorCBlue -= slopeBlueAc * yA;
                    textureAPercentageA -= slopeTextureAPercentageBa * yA;
                    textureAPercentageC -= slopeTextureAPercentageAc * yA;
                    textureBPercentageA -= slopeTextureBPercentageBa * yA;
                    textureBPercentageC -= slopeTextureBPercentageAc * yA;
                    yA = 0.0F;
                }
                if (yB < 0.0F) {
                    xB -= slopeXCb * yB;
                    zNearB -= slopeZNearCb * yB;
                    zFarB -= slopeZFarCb * yB;
                    uB -= slopeUCb * yB;
                    vB -= slopeVCb * yB;
                    fogDensityB -= slopeFogDensityCb * yB;
                    colorBAlpha -= slopeAlphaCb * yB;
                    colorBRed -= slopeRedCb * yB;
                    colorBGreen -= slopeGreenCb * yB;
                    colorBBlue -= slopeBlueCb * yB;
                    textureAPercentageB -= slopeTextureAPercentageCb * yB;
                    textureBPercentageB -= slopeTextureBPercentageCb * yB;
                    yB = 0.0F;
                }
                if ((yA == yB || slopeXAc >= slopeXBa) && (yA != yB || slopeXAc <= slopeXCb)) {
                    yA = ((int) (yA + 0.5F));
                    yB = ((int) (yB + 0.5F));
                    yC = ((int) (yC + 0.5F)) - yB;
                    yB -= yA;
                    for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xA, (int) xC, zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue, textureAPercentageA, textureAPercentageC, textureBPercentageA, textureBPercentageC);
                        xA += slopeXBa;
                        xC += slopeXAc;
                        zNearA += slopeZNearBa;
                        zNearC += slopeZNearAc;
                        zFarA += slopeZFarBa;
                        zFarC += slopeZFarAc;
                        uA += slopeUBa;
                        uC += slopeUAc;
                        vA += slopeVBa;
                        vC += slopeVAc;
                        fogDensityA += slopeFogDensityBa;
                        fogDensityC += slopeFogDensityAc;
                        colorAAlpha += slopeAlphaBa;
                        colorCAlpha += slopeAlphaAc;
                        colorARed += slopeRedBa;
                        colorCRed += slopeRedAc;
                        colorAGreen += slopeGreenBa;
                        colorCGreen += slopeGreenAc;
                        colorABlue += slopeBlueBa;
                        colorCBlue += slopeBlueAc;
                        textureAPercentageA += slopeTextureAPercentageBa;
                        textureAPercentageC += slopeTextureAPercentageAc;
                        textureBPercentageA += slopeTextureBPercentageBa;
                        textureBPercentageC += slopeTextureBPercentageAc;
                    }
                    while (--yC >= 0.0F) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xB, (int) xC, zNearB, zNearC, zFarB, zFarC, uB, uC, vB, vC, fogDensityB, fogDensityC, colorBAlpha, colorCAlpha, colorBRed, colorCRed, colorBGreen, colorCGreen, colorBBlue, colorCBlue, textureAPercentageB, textureAPercentageC, textureBPercentageB, textureBPercentageC);
                        xB += slopeXCb;
                        xC += slopeXAc;
                        zNearB += slopeZNearCb;
                        zNearC += slopeZNearAc;
                        zFarB += slopeZFarCb;
                        zFarC += slopeZFarAc;
                        uB += slopeUCb;
                        uC += slopeUAc;
                        vB += slopeVCb;
                        vC += slopeVAc;
                        fogDensityB += slopeFogDensityCb;
                        fogDensityC += slopeFogDensityAc;
                        colorBAlpha += slopeAlphaCb;
                        colorCAlpha += slopeAlphaAc;
                        colorBRed += slopeRedCb;
                        colorCRed += slopeRedAc;
                        colorBGreen += slopeGreenCb;
                        colorCGreen += slopeGreenAc;
                        colorBBlue += slopeBlueCb;
                        colorCBlue += slopeBlueAc;
                        textureAPercentageB += slopeTextureAPercentageCb;
                        textureAPercentageC += slopeTextureAPercentageAc;
                        textureBPercentageB += slopeTextureBPercentageCb;
                        textureBPercentageC += slopeTextureBPercentageAc;
                        yA += this.scanlineWidth;
                    }
                } else {
                    yA = ((int) (yA + 0.5F));
                    yB = ((int) (yB + 0.5F));
                    yC = ((int) (yC + 0.5F)) - yB;
                    yB -= yA;
                    for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xC, (int) xA, zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue, textureAPercentageC, textureAPercentageA, textureBPercentageC, textureBPercentageA);
                        xA += slopeXBa;
                        xC += slopeXAc;
                        zNearA += slopeZNearBa;
                        zNearC += slopeZNearAc;
                        zFarA += slopeZFarBa;
                        zFarC += slopeZFarAc;
                        uA += slopeUBa;
                        uC += slopeUAc;
                        vA += slopeVBa;
                        vC += slopeVAc;
                        fogDensityA += slopeFogDensityBa;
                        fogDensityC += slopeFogDensityAc;
                        colorAAlpha += slopeAlphaBa;
                        colorCAlpha += slopeAlphaAc;
                        colorARed += slopeRedBa;
                        colorCRed += slopeRedAc;
                        colorAGreen += slopeGreenBa;
                        colorCGreen += slopeGreenAc;
                        colorABlue += slopeBlueBa;
                        colorCBlue += slopeBlueAc;
                        textureAPercentageA += slopeTextureAPercentageBa;
                        textureAPercentageC += slopeTextureAPercentageAc;
                        textureBPercentageA += slopeTextureBPercentageBa;
                        textureBPercentageC += slopeTextureBPercentageAc;
                    }
                    while (--yC >= 0.0F) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xC, (int) xB, zNearC, zNearB, zFarC, zFarB, uC, uB, vC, vB, fogDensityC, fogDensityB, colorCAlpha, colorBAlpha, colorCRed, colorBRed, colorCGreen, colorBGreen, colorCBlue, colorBBlue, textureAPercentageC, textureAPercentageB, textureBPercentageC, textureBPercentageB);
                        xB += slopeXCb;
                        xC += slopeXAc;
                        zNearB += slopeZNearCb;
                        zNearC += slopeZNearAc;
                        zFarB += slopeZFarCb;
                        zFarC += slopeZFarAc;
                        uB += slopeUCb;
                        uC += slopeUAc;
                        vB += slopeVCb;
                        vC += slopeVAc;
                        fogDensityB += slopeFogDensityCb;
                        fogDensityC += slopeFogDensityAc;
                        colorBAlpha += slopeAlphaCb;
                        colorCAlpha += slopeAlphaAc;
                        colorBRed += slopeRedCb;
                        colorCRed += slopeRedAc;
                        colorBGreen += slopeGreenCb;
                        colorCGreen += slopeGreenAc;
                        colorBBlue += slopeBlueCb;
                        colorCBlue += slopeBlueAc;
                        textureAPercentageB += slopeTextureAPercentageCb;
                        textureAPercentageC += slopeTextureAPercentageAc;
                        textureBPercentageB += slopeTextureBPercentageCb;
                        textureBPercentageC += slopeTextureBPercentageAc;
                        yA += this.scanlineWidth;
                    }
                }
            } else {
                xB = xA;
                zNearB = zNearA;
                zFarB = zFarA;
                uB = uA;
                vB = vA;
                fogDensityB = fogDensityA;
                colorBAlpha = colorAAlpha;
                colorBRed = colorARed;
                colorBGreen = colorAGreen;
                colorBBlue = colorABlue;
                textureAPercentageB = textureAPercentageA;
                textureBPercentageB = textureBPercentageA;
                if (yA < 0.0F) {
                    xA -= slopeXBa * yA;
                    xB -= slopeXAc * yA;
                    zNearA -= slopeZNearBa * yA;
                    zNearB -= slopeZNearAc * yA;
                    zFarA -= slopeZFarBa * yA;
                    zFarB -= slopeZFarAc * yA;
                    uA -= slopeUBa * yA;
                    uB -= slopeUAc * yA;
                    vA -= slopeVBa * yA;
                    vB -= slopeVAc * yA;
                    fogDensityA -= slopeFogDensityBa * yA;
                    fogDensityB -= slopeFogDensityAc * yA;
                    colorAAlpha -= slopeAlphaBa * yA;
                    colorBAlpha -= slopeAlphaAc * yA;
                    colorARed -= slopeRedBa * yA;
                    colorBRed -= slopeRedAc * yA;
                    colorAGreen -= slopeGreenBa * yA;
                    colorBGreen -= slopeGreenAc * yA;
                    colorABlue -= slopeBlueBa * yA;
                    colorBBlue -= slopeBlueAc * yA;
                    textureAPercentageA -= slopeTextureAPercentageBa * yA;
                    textureAPercentageB -= slopeTextureAPercentageAc * yA;
                    textureBPercentageA -= slopeTextureBPercentageBa * yA;
                    textureBPercentageB -= slopeTextureBPercentageAc * yA;
                    yA = 0.0F;
                }
                if (yC < 0.0F) {
                    xC -= slopeXCb * yC;
                    zNearC -= slopeZNearCb * yC;
                    zFarC -= slopeZFarCb * yC;
                    uC -= slopeUCb * yC;
                    vC -= slopeVCb * yC;
                    fogDensityC -= slopeFogDensityCb * yC;
                    colorCAlpha -= slopeAlphaCb * yC;
                    colorCRed -= slopeRedCb * yC;
                    colorCGreen -= slopeGreenCb * yC;
                    colorCBlue -= slopeBlueCb * yC;
                    textureAPercentageC -= slopeTextureAPercentageCb * yC;
                    textureBPercentageC -= slopeTextureBPercentageCb * yC;
                    yC = 0.0F;
                }
                if (yA != yC && slopeXAc < slopeXBa || yA == yC && slopeXCb > slopeXBa) {
                    yA = ((int) (yA + 0.5F));
                    yC = ((int) (yC + 0.5F));
                    yB = ((int) (yB + 0.5F)) - yC;
                    yC -= yA;
                    for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xB, (int) xA, zNearB, zNearA, zFarB, zFarA, uB, uA, vB, vA, fogDensityB, fogDensityA, colorBAlpha, colorAAlpha, colorBRed, colorARed, colorBGreen, colorAGreen, colorBBlue, colorABlue, textureAPercentageB, textureAPercentageA, textureBPercentageB, textureBPercentageA);
                        xA += slopeXBa;
                        xB += slopeXAc;
                        zNearA += slopeZNearBa;
                        zNearB += slopeZNearAc;
                        zFarA += slopeZFarBa;
                        zFarB += slopeZFarAc;
                        uA += slopeUBa;
                        uB += slopeUAc;
                        vA += slopeVBa;
                        vB += slopeVAc;
                        fogDensityA += slopeFogDensityBa;
                        fogDensityB += slopeFogDensityAc;
                        colorAAlpha += slopeAlphaBa;
                        colorBAlpha += slopeAlphaAc;
                        colorARed += slopeRedBa;
                        colorBRed += slopeRedAc;
                        colorAGreen += slopeGreenBa;
                        colorBGreen += slopeGreenAc;
                        colorABlue += slopeBlueBa;
                        colorBBlue += slopeBlueAc;
                        textureAPercentageA += slopeTextureAPercentageBa;
                        textureAPercentageB += slopeTextureAPercentageAc;
                        textureBPercentageA += slopeTextureBPercentageBa;
                        textureBPercentageB += slopeTextureBPercentageAc;
                    }
                    while (--yB >= 0.0F) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xC, (int) xA, zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue, textureAPercentageC, textureAPercentageA, textureBPercentageC, textureBPercentageA);
                        xC += slopeXCb;
                        xA += slopeXBa;
                        zNearC += slopeZNearCb;
                        zNearA += slopeZNearBa;
                        zFarC += slopeZFarCb;
                        zFarA += slopeZFarBa;
                        uC += slopeUCb;
                        uA += slopeUBa;
                        vC += slopeVCb;
                        vA += slopeVBa;
                        fogDensityC += slopeFogDensityCb;
                        fogDensityA += slopeFogDensityBa;
                        colorCAlpha += slopeAlphaCb;
                        colorAAlpha += slopeAlphaBa;
                        colorCRed += slopeRedCb;
                        colorARed += slopeRedBa;
                        colorCGreen += slopeGreenCb;
                        colorAGreen += slopeGreenBa;
                        colorCBlue += slopeBlueCb;
                        colorABlue += slopeBlueBa;
                        textureAPercentageC += slopeTextureAPercentageCb;
                        textureAPercentageA += slopeTextureAPercentageBa;
                        textureBPercentageC += slopeTextureBPercentageCb;
                        textureBPercentageA += slopeTextureBPercentageBa;
                        yA += this.scanlineWidth;
                    }
                } else {
                    yA = ((int) (yA + 0.5F));
                    yC = ((int) (yC + 0.5F));
                    yB = ((int) (yB + 0.5F)) - yC;
                    yC -= yA;
                    for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xA, (int) xB, zNearA, zNearB, zFarA, zFarB, uA, uB, vA, vB, fogDensityA, fogDensityB, colorAAlpha, colorBAlpha, colorARed, colorBRed, colorAGreen, colorBGreen, colorABlue, colorBBlue, textureAPercentageA, textureAPercentageB, textureBPercentageA, textureBPercentageB);
                        xB += slopeXAc;
                        xA += slopeXBa;
                        zNearB += slopeZNearAc;
                        zNearA += slopeZNearBa;
                        zFarB += slopeZFarAc;
                        zFarA += slopeZFarBa;
                        uB += slopeUAc;
                        uA += slopeUBa;
                        vB += slopeVAc;
                        vA += slopeVBa;
                        fogDensityB += slopeFogDensityAc;
                        fogDensityA += slopeFogDensityBa;
                        colorBAlpha += slopeAlphaAc;
                        colorAAlpha += slopeAlphaBa;
                        colorBRed += slopeRedAc;
                        colorARed += slopeRedBa;
                        colorBGreen += slopeGreenAc;
                        colorAGreen += slopeGreenBa;
                        colorBBlue += slopeBlueAc;
                        colorABlue += slopeBlueBa;
                        textureAPercentageB += slopeTextureAPercentageAc;
                        textureAPercentageA += slopeTextureAPercentageBa;
                        textureBPercentageB += slopeTextureBPercentageAc;
                        textureBPercentageA += slopeTextureBPercentageBa;
                    }
                    while (--yB >= 0.0F) {
                        drawMultiMaterialzedScanline(this.colourBuffer, (int) yA, (int) xA, (int) xC, zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue, textureAPercentageA, textureAPercentageC, textureBPercentageA, textureBPercentageC);
                        xA += slopeXBa;
                        xC += slopeXCb;
                        zNearA += slopeZNearBa;
                        zNearC += slopeZNearCb;
                        zFarA += slopeZFarBa;
                        zFarC += slopeZFarCb;
                        uA += slopeUBa;
                        uC += slopeUCb;
                        vA += slopeVBa;
                        vC += slopeVCb;
                        fogDensityA += slopeFogDensityBa;
                        fogDensityC += slopeFogDensityCb;
                        colorAAlpha += slopeAlphaBa;
                        colorCAlpha += slopeAlphaCb;
                        colorARed += slopeRedBa;
                        colorCRed += slopeRedCb;
                        colorAGreen += slopeGreenBa;
                        colorCGreen += slopeGreenCb;
                        colorABlue += slopeBlueBa;
                        colorCBlue += slopeBlueCb;
                        textureAPercentageA += slopeTextureAPercentageBa;
                        textureAPercentageC += slopeTextureAPercentageCb;
                        textureBPercentageA += slopeTextureBPercentageBa;
                        textureBPercentageC += slopeTextureBPercentageCb;
                        yA += this.scanlineWidth;
                    }
                }
            }
        }
    }

    public void drawScanline(boolean writeDepth, boolean transparent, int[] pixels, int bufferOffset, int xA, int xB, float depth, float slopeDepth, float red, float slopeRed, float green, float slopeGreen, float blue, float slopeBlue) {
        int x = 0;
        int colorRgb = 0;
        if (this.clipEdges) {
            if (xB > this.clipWidth) {
                xB = this.clipWidth;
            }

            if (xA < 0) {
                xA = 0;
            }
        }

        if (xA < xB) {
            int alpha;
            int pixel;
            int invertedAlpha;
            if (this.aBool1676) {
                bufferOffset += xA;
                red += slopeRed * xA;
                green += slopeGreen * xA;
                blue += slopeBlue * xA;
                if (this.smoothEdges) {
                    x = xB - xA >> 2;
                    slopeRed *= 4.0F;
                    slopeGreen *= 4.0F;
                    slopeBlue *= 4.0F;
                    if (this.blendingAlpha == 0) {
                        if (x > 0) {
                            do {
                                colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                                pixels[bufferOffset++] = colorRgb;
                                pixels[bufferOffset++] = colorRgb;
                                pixels[bufferOffset++] = colorRgb;
                                pixels[bufferOffset++] = colorRgb;
                            } while (--x > 0);
                        }
                        x = xB - xA & 0x3;
                        if (x > 0) {
                            colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            do {
                                pixels[bufferOffset++] = colorRgb;
                            } while (--x > 0);
                        }
                    } else if (!this.blending) {
                        alpha = this.blendingAlpha;
                        invertedAlpha = 256 - this.blendingAlpha;
                        if (x > 0) {
                            do {
                                colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                                colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                                pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset++] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    pixel = pixels[bufferOffset];
                                    pixels[bufferOffset++] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    pixel = pixels[bufferOffset];
                                    pixels[bufferOffset++] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    pixel = pixels[bufferOffset];
                                    pixels[bufferOffset++] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                } else {
                                    pixels[bufferOffset++] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    pixel = pixels[bufferOffset];
                                    pixels[bufferOffset++] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    pixel = pixels[bufferOffset];
                                    pixels[bufferOffset++] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    pixel = pixels[bufferOffset];
                                    pixels[bufferOffset++] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                }
                            } while (--x > 0);
                        }
                        x = xB - xA & 0x3;
                        if (x > 0) {
                            colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                            do {
                                pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset++] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                } else {
                                    pixels[bufferOffset++] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                }
                            } while (--x > 0);
                        }
                    } else {
                        if (x > 0) {
                            do {
                                colorRgb = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                                invertedAlpha = bufferOffset++;
                                int i_21 = pixels[invertedAlpha];
                                int i_20 = colorRgb + i_21;
                                int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                pixels[invertedAlpha] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                i_20 = bufferOffset++;
                                int i_23 = pixels[i_20];
                                int i_24 = colorRgb + i_23;
                                int i_25 = (i_23 & 0xff00ff) + (colorRgb & 0xff00ff);
                                i_23 = (i_25 & 0x1000100) + (i_24 - i_25 & 0x10000);
                                pixels[i_20] = ~0xffffff | i_24 - i_23 | i_23 - (i_23 >>> 8);
                                i_24 = bufferOffset++;
                                int i_26 = pixels[i_24];
                                int i_27 = colorRgb + i_26;
                                int i_28 = (i_26 & 0xff00ff) + (colorRgb & 0xff00ff);
                                i_26 = (i_28 & 0x1000100) + (i_27 - i_28 & 0x10000);
                                pixels[i_24] = ~0xffffff | i_27 - i_26 | i_26 - (i_26 >>> 8);
                                i_27 = bufferOffset++;
                                int i_29 = pixels[i_27];
                                int i_30 = colorRgb + i_29;
                                int i_31 = (i_29 & 0xff00ff) + (colorRgb & 0xff00ff);
                                i_29 = (i_31 & 0x1000100) + (i_30 - i_31 & 0x10000);
                                pixels[i_27] = ~0xffffff | i_30 - i_29 | i_29 - (i_29 >>> 8);
                            } while (--x > 0);
                        }
                        x = xB - xA & 0x3;
                        if (x > 0) {
                            colorRgb = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            do {
                                invertedAlpha = bufferOffset++;
                                int i_21 = pixels[invertedAlpha];
                                int i_20 = colorRgb + i_21;
                                int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                pixels[invertedAlpha] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                            } while (--x > 0);
                        }
                    }
                } else {
                    x = xB - xA;
                    if (this.blendingAlpha == 0) {
                        do {
                            pixels[bufferOffset++] = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            red += slopeRed;
                            green += slopeGreen;
                            blue += slopeBlue;
                        } while (--x > 0);
                    } else if (!this.blending) {
                        alpha = this.blendingAlpha;
                        invertedAlpha = 256 - this.blendingAlpha;
                        do {
                            colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            red += slopeRed;
                            green += slopeGreen;
                            blue += slopeBlue;
                            colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                            pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset++] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            } else {
                                pixels[bufferOffset++] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            }
                        } while (--x > 0);
                    } else {
                        do {
                            invertedAlpha = bufferOffset++;
                            pixel = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            int i_21 = pixels[invertedAlpha];
                            int i_20 = pixel + i_21;
                            int i_22 = (pixel & 0xff00ff) + (i_21 & 0xff00ff);
                            i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                            pixels[invertedAlpha] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                            red += slopeRed;
                            green += slopeGreen;
                            blue += slopeBlue;
                        } while (--x > 0);
                    }
                }
            } else {
                bufferOffset += xA - 1;
                depth += slopeDepth * xA;
                red += slopeRed * xA;
                green += slopeGreen * xA;
                blue += slopeBlue * xA;
                if (this.threadResource.zbuffering) {
                    if (this.smoothEdges) {
                        x = xB - xA >> 2;
                        slopeRed *= 4.0F;
                        slopeGreen *= 4.0F;
                        slopeBlue *= 4.0F;
                        if (this.blendingAlpha == 0) {
                            if (x > 0) {
                                do {
                                    colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                    red += slopeRed;
                                    green += slopeGreen;
                                    blue += slopeBlue;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb;
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb;
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb;
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb;
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                } while (--x > 0);
                            }
                            x = xB - xA & 0x3;
                            if (x > 0) {
                                colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                do {
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb;
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                } while (--x > 0);
                            }
                        } else if (!this.blending) {
                            alpha = this.blendingAlpha;
                            invertedAlpha = 256 - this.blendingAlpha;
                            if (x > 0) {
                                do {
                                    colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                    red += slopeRed;
                                    green += slopeGreen;
                                    blue += slopeBlue;
                                    colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixel = pixels[bufferOffset];
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        }
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixel = pixels[bufferOffset];
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        }
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixel = pixels[bufferOffset];
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        }
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixel = pixels[bufferOffset];
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        }
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                } while (--x > 0);
                            }
                            x = xB - xA & 0x3;
                            if (x > 0) {
                                colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                                do {
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixel = pixels[bufferOffset];
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        }
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                } while (--x > 0);
                            }
                        } else {
                            if (x > 0) {
                                do {
                                    colorRgb = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                    red += slopeRed;
                                    green += slopeGreen;
                                    blue += slopeBlue;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    	int i_21 = pixels[bufferOffset];
                                    	int i_20 = colorRgb + i_21;
                                    	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                        i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                        pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    	int i_21 = pixels[bufferOffset];
                                    	int i_20 = colorRgb + i_21;
                                    	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                        i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                        pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    	int i_21 = pixels[bufferOffset];
                                    	int i_20 = colorRgb + i_21;
                                    	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                        i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                        pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    	int i_21 = pixels[bufferOffset];
                                    	int i_20 = colorRgb + i_21;
                                    	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                        i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                        pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                } while (--x > 0);
                            }
                            x = xB - xA & 0x3;
                            if (x > 0) {
                                colorRgb = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                do {
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    	int i_21 = pixels[bufferOffset];
                                    	int i_20 = colorRgb + i_21;
                                    	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                        i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                        pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                } while (--x > 0);
                            }
                        }
                    } else {
                        x = xB - xA;
                        if (this.blendingAlpha != 0) {
                            if (this.blending) {
                                do {
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        pixel = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                        int i_21 = pixels[bufferOffset];
                                        int i_20 = pixel + i_21;
                                        int i_22 = (pixel & 0xff00ff) + (i_21 & 0xff00ff);
                                        i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                        pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    red += slopeRed;
                                    green += slopeGreen;
                                    blue += slopeBlue;
                                } while (--x > 0);
                            } else {
                                alpha = this.blendingAlpha;
                                invertedAlpha = 256 - this.blendingAlpha;
                                do {
                                    bufferOffset++;
                                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                        colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                        colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                                        pixel = pixels[bufferOffset];
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                        }

                                        if (writeDepth) {
                                            this.depthBuffer[bufferOffset] = depth;
                                        }
                                    }
                                    depth += slopeDepth;
                                    red += slopeRed;
                                    green += slopeGreen;
                                    blue += slopeBlue;
                                } while (--x > 0);
                            }
                        } else {
                            do {
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixels[bufferOffset] = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                    if (writeDepth) {
                                        this.depthBuffer[bufferOffset] = depth;
                                    }
                                }
                                depth += slopeDepth;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                            } while (--x > 0);
                        }
                    }
                } else if (this.smoothEdges) {
                    x = xB - xA >> 2;
                    slopeRed *= 4.0F;
                    slopeGreen *= 4.0F;
                    slopeBlue *= 4.0F;
                    if (this.blendingAlpha == 0) {
                        if (x > 0) {
                            do {
                                colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb;
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb;
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb;
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb;
                                }
                                depth += slopeDepth;
                            } while (--x > 0);
                        }
                        x = xB - xA & 0x3;
                        if (x > 0) {
                            colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            do {
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb;
                                }
                                depth += slopeDepth;
                            } while (--x > 0);
                        }
                    } else if (!this.blending) {
                        alpha = this.blendingAlpha;
                        invertedAlpha = 256 - this.blendingAlpha;
                        if (x > 0) {
                            do {
                                colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                                colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixel = pixels[bufferOffset];
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    }
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixel = pixels[bufferOffset];
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    }
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixel = pixels[bufferOffset];
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    }
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixel = pixels[bufferOffset];
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    }
                                }
                                depth += slopeDepth;
                            } while (--x > 0);
                        }
                        x = xB - xA & 0x3;
                        if (x > 0) {
                            colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                            do {
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixel = pixels[bufferOffset];
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    }
                                }
                                depth += slopeDepth;
                            } while (--x > 0);
                        }
                    } else {
                        if (x > 0) {
                            do {
                                colorRgb = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                	int i_21 = pixels[bufferOffset];
                                	int i_20 = colorRgb + i_21;
                                	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                    i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                    pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                	int i_21 = pixels[bufferOffset];
                                	int i_20 = colorRgb + i_21;
                                	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                    i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                    pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                	int i_21 = pixels[bufferOffset];
                                	int i_20 = colorRgb + i_21;
                                	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                    i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                    pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                }
                                depth += slopeDepth;
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                	int i_21 = pixels[bufferOffset];
                                	int i_20 = colorRgb + i_21;
                                	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                    i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                    pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                }
                                depth += slopeDepth;
                            } while (--x > 0);
                        }
                        x = xB - xA & 0x3;
                        if (x > 0) {
                            colorRgb = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            do {
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                	int i_21 = pixels[bufferOffset];
                                	int i_20 = colorRgb + i_21;
                                	int i_22 = (i_21 & 0xff00ff) + (colorRgb & 0xff00ff);
                                    i_21 = (i_22 & 0x1000100) + (i_20 - i_22 & 0x10000);
                                    pixels[bufferOffset] = ~0xffffff | i_20 - i_21 | i_21 - (i_21 >>> 8);
                                }

                                depth += slopeDepth;
                            } while (--x > 0);
                        }
                    }
                } else {
                    x = xB - xA;
                    if (this.blendingAlpha != 0) {
                        if (this.blending) {
                            do {
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    pixel = (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                    int currentPixel = pixels[bufferOffset];
                                    int pixelSum = pixel + currentPixel;
                                    int i_22 = (pixel & 0xff00ff) + (currentPixel & 0xff00ff);
                                    currentPixel = (i_22 & 0x1000100) + (pixelSum - i_22 & 0x10000);
                                    pixels[bufferOffset] = ~0xffffff | pixelSum - currentPixel | currentPixel - (currentPixel >>> 8);
                                }
                                depth += slopeDepth;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                            } while (--x > 0);
                        } else {
                            alpha = this.blendingAlpha;
                            invertedAlpha = 256 - this.blendingAlpha;
                            do {
                                bufferOffset++;
                                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                    colorRgb = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                                    colorRgb = (invertedAlpha * (colorRgb & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (colorRgb & 0xff00) >> 8 & 0xff00);
                                    pixel = pixels[bufferOffset];
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                    }
                                }
                                depth += slopeDepth;
                                red += slopeRed;
                                green += slopeGreen;
                                blue += slopeBlue;
                            } while (--x > 0);
                        }
                    } else {
                        do {
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                pixels[bufferOffset] = ~0xffffff | (int) red & 0xff0000 | (int) green & 0xff00 | (int) blue & 0xff;
                            }
                            depth += slopeDepth;
                            red += slopeRed;
                            green += slopeGreen;
                            blue += slopeBlue;
                        } while (--x > 0);
                    }
                }
            }
        }
    }

    public void drawMaterializedTriangle(boolean colorWrite, boolean depthWrite, boolean transparent, float yA, float yB, float yC, float xA, float xB, float xC, float zNearA, float zNearB, float zNearC, float zFarA, float zFarB, float zFarC, float uA, float uB, float uC, float vA, float vB, float vC, int colorA, int colorB, int colorC, int fogColor, float fogDensityA, float fogDensityB, float fogDensityC, int materialId) {
        if (!colorWrite) {
            drawFlatTriangle(false, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, 0);
        } else {
            if (materialId != this.textureA) {
                this.texturePixelsA = this.safemodeToolkit.getAnimatedMaterialArgb(materialId);
                if (this.texturePixelsA == null) {
                    this.blendingAlpha = 255 - (colorA >> 24 & 0xff);
                    int materialARgb = ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(this.safemodeToolkit.getMaterialColor(materialId)) & 0xffff];
                    int centerColour = ~0xffffff | (materialARgb >> 16 & 0xff) * (colorA >> 16 & 0xff) << 8 & 0xff0000 | (colorA >> 8 & 0xff) * (materialARgb >> 8 & 0xff) & 0xff00 | (materialARgb & 0xff) * (colorA & 0xff) >> 8;
                    drawTriangle(true, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(centerColour, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(centerColour, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(centerColour, fogColor, fogDensityC));
                    return;
                }
                this.textureSizeA = this.safemodeToolkit.isSmallTexture(materialId) ? 64 : this.safemodeToolkit.materialSize;
                this.textureMaskA = this.textureSizeA - 1;
                this.alphaMode = this.safemodeToolkit.getMaterialEffectCombiner(materialId);
                this.textureRepeat = this.safemodeToolkit.hasRepeat(materialId);
            }
            this.fogColour = fogColor;
            if (yA > yB || yA > yC) {
                if (yB <= yC) {
                	float tmp1 = xA;
                    xA = xB;
                    xB = tmp1;
                    tmp1 = yA;
                    yA = yB;
                    yB = tmp1;
                    tmp1 = zNearA;
                    zNearA = zNearB;
                    zNearB = tmp1;
                    tmp1 = uA;
                    uA = uB;
                    uB = tmp1;
                    tmp1 = vA;
                    vA = vB;
                    vB = tmp1;
                    tmp1 = zFarA;
                    zFarA = zFarB;
                    zFarB = tmp1;
                    tmp1 = fogDensityA;
                    fogDensityA = fogDensityB;
                    fogDensityB = tmp1;
                    int tmp2 = colorA;
                    colorA = colorB;
                    colorB = tmp2;
                } else {
                	float tmp1 = xA;
                    xA = xC;
                    xC = tmp1;
                    tmp1 = yA;
                    yA = yC;
                    yC = tmp1;
                    tmp1 = zNearA;
                    zNearA = zNearC;
                    zNearC = tmp1;
                    tmp1 = uA;
                    uA = uC;
                    uC = tmp1;
                    tmp1 = vA;
                    vA = vC;
                    vC = tmp1;
                    tmp1 = zFarA;
                    zFarA = zFarC;
                    zFarC = tmp1;
                    tmp1 = fogDensityA;
                    fogDensityA = fogDensityC;
                    fogDensityC = tmp1;
                    int tmp = colorA;
                    colorA = colorC;
                    colorC = tmp;
                }
            }
            uA /= zFarA;
            uB /= zFarB;
            uC /= zFarC;
            vA /= zFarA;
            vB /= zFarB;
            vC /= zFarC;
            zNearA = 1.0F / zNearA;
            zNearB = 1.0F / zNearB;
            zNearC = 1.0F / zNearC;
            zFarA = 1.0F / zFarA;
            zFarB = 1.0F / zFarB;
            zFarC = 1.0F / zFarC;
            float colorAAlpha = (colorA >> 24 & 0xff);
            float colorBAlpha = (colorB >> 24 & 0xff);
            float colorCAlpha = (colorC >> 24 & 0xff);
            float colorARed = (colorA >> 16 & 0xff);
            float colorBRed = (colorB >> 16 & 0xff);
            float colorCRed = (colorC >> 16 & 0xff);
            float colorAGreen = (colorA >> 8 & 0xff);
            float colorBGreen = (colorB >> 8 & 0xff);
            float colorCGreen = (colorC >> 8 & 0xff);
            float colorABlue = (colorA & 0xff);
            float colorBBlue = (colorB & 0xff);
            float colorCBlue = (colorC & 0xff);
            float slopeXBa = 0.0F;
            float slopeZnearBa = 0.0F;
            float slopeZfarBa = 0.0F;
            float slopeUBa = 0.0F;
            float slopeVBa = 0.0F;
            float slopeFogDensityBa = 0.0F;
            float slopeAlphaBa = 0.0F;
            float slopeRedBa = 0.0F;
            float slopeGreenBa = 0.0F;
            float slopeBlueBa = 0.0F;
            if (yB != yA) {
                float lengthDeltaYAb = yB - yA;
                slopeXBa = (xB - xA) / lengthDeltaYAb;
                slopeZnearBa = (zNearB - zNearA) / lengthDeltaYAb;
                slopeZfarBa = (zFarB - zFarA) / lengthDeltaYAb;
                slopeUBa = (uB - uA) / lengthDeltaYAb;
                slopeVBa = (vB - vA) / lengthDeltaYAb;
                slopeFogDensityBa = (fogDensityB - fogDensityA) / lengthDeltaYAb;
                slopeAlphaBa = (colorBAlpha - colorAAlpha) / lengthDeltaYAb;
                slopeRedBa = (colorBRed - colorARed) / lengthDeltaYAb;
                slopeGreenBa = (colorBGreen - colorAGreen) / lengthDeltaYAb;
                slopeBlueBa = (colorBBlue - colorABlue) / lengthDeltaYAb;
            }
            float slopeXCb = 0.0F;
            float slopeZnearCb = 0.0F;
            float slopeZfarCb = 0.0F;
            float slopeUCb = 0.0F;
            float slopeVCb = 0.0F;
            float slopeFogDensityCb = 0.0F;
            float slopeAlphaCb = 0.0F;
            float slopeRedCb = 0.0F;
            float slopeGreenCb = 0.0F;
            float slopeBlueCb = 0.0F;
            if (yC != yB) {
            	float lengthDeltaYCb = yC - yB;
                slopeXCb = (xC - xB) / lengthDeltaYCb;
                slopeZnearCb = (zNearC - zNearB) / lengthDeltaYCb;
                slopeZfarCb = (zFarC - zFarB) / lengthDeltaYCb;
                slopeUCb = (uC - uB) / lengthDeltaYCb;
                slopeVCb = (vC - vB) / lengthDeltaYCb;
                slopeFogDensityCb = (fogDensityC - fogDensityB) / lengthDeltaYCb;
                slopeAlphaCb = (colorCAlpha - colorBAlpha) / lengthDeltaYCb;
                slopeRedCb = (colorCRed - colorBRed) / lengthDeltaYCb;
                slopeGreenCb = (colorCGreen - colorBGreen) / lengthDeltaYCb;
                slopeBlueCb = (colorCBlue - colorBBlue) / lengthDeltaYCb;
            }
            float slopeXAc = 0.0F;
            float slopeZnearAc = 0.0F;
            float slopeZfarAc = 0.0F;
            float slopeUAc = 0.0F;
            float slopeVAc = 0.0F;
            float slopeFogDensityAc = 0.0F;
            float slopeAlphaAc = 0.0F;
            float slopeRedAc = 0.0F;
            float slopeGreenAc = 0.0F;
            float slopeBlueAc = 0.0F;
            if (yA != yC) {
                float lengthDeltaYAc = yA - yC;
                slopeXAc = (xA - xC) / lengthDeltaYAc;
                slopeZnearAc = (zNearA - zNearC) / lengthDeltaYAc;
                slopeZfarAc = (zFarA - zFarC) / lengthDeltaYAc;
                slopeUAc = (uA - uC) / lengthDeltaYAc;
                slopeVAc = (vA - vC) / lengthDeltaYAc;
                slopeFogDensityAc = (fogDensityA - fogDensityC) / lengthDeltaYAc;
                slopeAlphaAc = (colorAAlpha - colorCAlpha) / lengthDeltaYAc;
                slopeRedAc = (colorARed - colorCRed) / lengthDeltaYAc;
                slopeGreenAc = (colorAGreen - colorCGreen) / lengthDeltaYAc;
                slopeBlueAc = (colorABlue - colorCBlue) / lengthDeltaYAc;
            }
            if (yA < this.clipHeight) {
                if (yB > this.clipHeight) {
                    yB = this.clipHeight;
                }
                if (yC > this.clipHeight) {
                    yC = this.clipHeight;
                }
                if (yB < yC) {
                    xC = xA;
                    zNearC = zNearA;
                    zFarC = zFarA;
                    uC = uA;
                    vC = vA;
                    fogDensityC = fogDensityA;
                    colorCAlpha = colorAAlpha;
                    colorCRed = colorARed;
                    colorCGreen = colorAGreen;
                    colorCBlue = colorABlue;
                    if (yA < 0.0F) {
                        xA -= slopeXBa * yA;
                        xC -= slopeXAc * yA;
                        zNearA -= slopeZnearBa * yA;
                        zNearC -= slopeZnearAc * yA;
                        zFarA -= slopeZfarBa * yA;
                        zFarC -= slopeZfarAc * yA;
                        uA -= slopeUBa * yA;
                        uC -= slopeUAc * yA;
                        vA -= slopeVBa * yA;
                        vC -= slopeVAc * yA;
                        fogDensityA -= slopeFogDensityBa * yA;
                        fogDensityC -= slopeFogDensityAc * yA;
                        colorAAlpha -= slopeAlphaBa * yA;
                        colorCAlpha -= slopeAlphaAc * yA;
                        colorARed -= slopeAlphaBa * yA;
                        colorCRed -= slopeAlphaAc * yA;
                        colorAGreen -= slopeAlphaBa * yA;
                        colorCGreen -= slopeAlphaAc * yA;
                        colorABlue -= slopeAlphaBa * yA;
                        colorCBlue -= slopeAlphaAc * yA;
                        yA = 0.0F;
                    }
                    if (yB < 0.0F) {
                        xB -= slopeXCb * yB;
                        zNearB -= slopeZnearCb * yB;
                        zFarB -= slopeZfarCb * yB;
                        uB -= slopeUCb * yB;
                        vB -= slopeVCb * yB;
                        fogDensityB -= slopeFogDensityCb * yB;
                        colorBAlpha -= slopeAlphaCb * yB;
                        colorBRed -= slopeRedCb * yB;
                        colorBGreen -= slopeGreenCb * yB;
                        colorBBlue -= slopeBlueCb * yB;
                        yB = 0.0F;
                    }
                    if (yA != yB && slopeXAc < slopeXBa || yA == yB && slopeXAc > slopeXCb) {
                        yA = ((int) (yA + 0.5F));
                        yB = ((int) (yB + 0.5F));
                        yC = ((int) (yC + 0.5F)) - yB;
                        yB -= yA;
                        for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xC, (int) xA, zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue);
                            xA += slopeXBa;
                            xC += slopeXAc;
                            zNearA += slopeZnearBa;
                            zNearC += slopeZnearAc;
                            zFarA += slopeZfarBa;
                            zFarC += slopeZfarAc;
                            uA += slopeUBa;
                            uC += slopeUAc;
                            vA += slopeVBa;
                            vC += slopeVAc;
                            fogDensityA += slopeFogDensityBa;
                            fogDensityC += slopeFogDensityAc;
                            colorAAlpha += slopeAlphaBa;
                            colorCAlpha += slopeAlphaAc;
                            colorARed += slopeRedBa;
                            colorCRed += slopeRedAc;
                            colorAGreen += slopeGreenBa;
                            colorCGreen += slopeGreenAc;
                            colorABlue += slopeBlueBa;
                            colorCBlue += slopeBlueAc;
                        }
                        while (--yC >= 0.0F) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xC, (int) xB, zNearC, zNearB, zFarC, zFarB, uC, uB, vC, vB, fogDensityC, fogDensityB, colorCAlpha, colorBAlpha, colorCRed, colorBRed, colorCGreen, colorBGreen, colorCBlue, colorBBlue);
                            xB += slopeXCb;
                            xC += slopeXAc;
                            zNearB += slopeZnearCb;
                            zNearC += slopeZnearAc;
                            zFarB += slopeZfarCb;
                            zFarC += slopeZfarAc;
                            uB += slopeUCb;
                            uC += slopeUAc;
                            vB += slopeVCb;
                            vC += slopeVAc;
                            fogDensityB += slopeFogDensityCb;
                            fogDensityC += slopeFogDensityAc;
                            colorBAlpha += slopeAlphaCb;
                            colorCAlpha += slopeAlphaAc;
                            colorBRed += slopeRedCb;
                            colorCRed += slopeRedAc;
                            colorBGreen += slopeGreenCb;
                            colorCGreen += slopeGreenAc;
                            colorBBlue += slopeBlueCb;
                            colorCBlue += slopeBlueAc;
                            yA += this.scanlineWidth;
                        }
                    } else {
                        yA = ((int) (yA + 0.5F));
                        yB = ((int) (yB + 0.5F));
                        yC = ((int) (yC + 0.5F)) - yB;
                        yB -= yA;
                        for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xA, (int) xC, zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue);
                            xA += slopeXBa;
                            xC += slopeXAc;
                            zNearA += slopeZnearBa;
                            zNearC += slopeZnearAc;
                            zFarA += slopeZfarBa;
                            zFarC += slopeZfarAc;
                            uA += slopeUBa;
                            uC += slopeUAc;
                            vA += slopeVBa;
                            vC += slopeVAc;
                            fogDensityA += slopeFogDensityBa;
                            fogDensityC += slopeFogDensityAc;
                            colorAAlpha += slopeAlphaBa;
                            colorCAlpha += slopeAlphaAc;
                            colorARed += slopeRedBa;
                            colorCRed += slopeRedAc;
                            colorAGreen += slopeGreenBa;
                            colorCGreen += slopeGreenAc;
                            colorABlue += slopeBlueBa;
                            colorCBlue += slopeBlueAc;
                        }
                        while (--yC >= 0.0F) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xB, (int) xC, zNearB, zNearC, zFarB, zFarC, uB, uC, vB, vC, fogDensityB, fogDensityC, colorBAlpha, colorCAlpha, colorBRed, colorCRed, colorBGreen, colorCGreen, colorBBlue, colorCBlue);
                            xB += slopeXCb;
                            xC += slopeXAc;
                            zNearB += slopeZnearCb;
                            zNearC += slopeZnearAc;
                            zFarB += slopeZfarCb;
                            zFarC += slopeZfarAc;
                            uB += slopeUCb;
                            uC += slopeUAc;
                            vB += slopeVCb;
                            vC += slopeVAc;
                            fogDensityB += slopeFogDensityCb;
                            fogDensityC += slopeFogDensityAc;
                            colorBAlpha += slopeAlphaCb;
                            colorCAlpha += slopeAlphaAc;
                            colorBRed += slopeRedCb;
                            colorCRed += slopeRedAc;
                            colorBGreen += slopeGreenCb;
                            colorCGreen += slopeGreenAc;
                            colorBBlue += slopeBlueCb;
                            colorCBlue += slopeBlueAc;
                            yA += this.scanlineWidth;
                        }
                    }
                } else {
                    xB = xA;
                    zNearB = zNearA;
                    zFarB = zFarA;
                    uB = uA;
                    vB = vA;
                    fogDensityB = fogDensityA;
                    colorBAlpha = colorAAlpha;
                    colorBRed = colorARed;
                    colorBGreen = colorAGreen;
                    colorBBlue = colorABlue;
                    if (yA < 0.0F) {
                        xA -= slopeXBa * yA;
                        xB -= slopeXAc * yA;
                        zNearA -= slopeZnearBa * yA;
                        zNearB -= slopeZnearAc * yA;
                        zFarA -= slopeZfarBa * yA;
                        zFarB -= slopeZfarAc * yA;
                        uA -= slopeUBa * yA;
                        uB -= slopeUAc * yA;
                        vA -= slopeVBa * yA;
                        vB -= slopeVAc * yA;
                        fogDensityA -= slopeFogDensityBa * yA;
                        fogDensityB -= slopeFogDensityAc * yA;
                        colorAAlpha -= slopeAlphaBa * yA;
                        colorBAlpha -= slopeAlphaAc * yA;
                        colorARed -= slopeAlphaBa * yA;
                        colorBRed -= slopeAlphaAc * yA;
                        colorAGreen -= slopeAlphaBa * yA;
                        colorBGreen -= slopeAlphaAc * yA;
                        colorABlue -= slopeAlphaBa * yA;
                        colorBBlue -= slopeAlphaAc * yA;
                        yA = 0.0F;
                    }
                    if (yC < 0.0F) {
                        xC -= slopeXCb * yC;
                        zNearC -= slopeZnearCb * yC;
                        zFarC -= slopeZfarCb * yC;
                        uC -= slopeUCb * yC;
                        vC -= slopeVCb * yC;
                        fogDensityC -= slopeFogDensityCb * yC;
                        colorCAlpha -= slopeAlphaCb * yC;
                        colorCRed -= slopeRedCb * yC;
                        colorCGreen -= slopeGreenCb * yC;
                        colorCBlue -= slopeBlueCb * yC;
                        yC = 0.0F;
                    }
                    if ((yA == yC || slopeXAc >= slopeXBa) && (yA != yC || slopeXCb <= slopeXBa)) {
                        yA = ((int) (yA + 0.5F));
                        yC = ((int) (yC + 0.5F));
                        yB = ((int) (yB + 0.5F)) - yC;
                        yC -= yA;
                        for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xA, (int) xB, zNearA, zNearB, zFarA, zFarB, uA, uB, vA, vB, fogDensityA, fogDensityB, colorAAlpha, colorBAlpha, colorARed, colorBRed, colorAGreen, colorBGreen, colorABlue, colorBBlue);
                            xB += slopeXAc;
                            xA += slopeXBa;
                            zNearB += slopeZnearAc;
                            zNearA += slopeZnearBa;
                            zFarB += slopeZfarAc;
                            zFarA += slopeZfarBa;
                            uB += slopeUAc;
                            uA += slopeUBa;
                            vB += slopeVAc;
                            vA += slopeVBa;
                            fogDensityB += slopeFogDensityAc;
                            fogDensityA += slopeFogDensityBa;
                            colorBAlpha += slopeAlphaAc;
                            colorAAlpha += slopeAlphaBa;
                            colorBRed += slopeRedAc;
                            colorARed += slopeRedBa;
                            colorBGreen += slopeGreenAc;
                            colorAGreen += slopeGreenBa;
                            colorBBlue += slopeBlueAc;
                            colorABlue += slopeBlueBa;
                        }
                        while (--yB >= 0.0F) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xA, (int) xC, zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue);
                            xA += slopeXBa;
                            xC += slopeXCb;
                            zNearA += slopeZnearBa;
                            zNearC += slopeZnearCb;
                            zFarA += slopeZfarBa;
                            zFarC += slopeZfarCb;
                            uA += slopeUBa;
                            uC += slopeUCb;
                            vA += slopeVBa;
                            vC += slopeVCb;
                            fogDensityA += slopeFogDensityBa;
                            fogDensityC += slopeFogDensityCb;
                            colorAAlpha += slopeAlphaBa;
                            colorCAlpha += slopeAlphaCb;
                            colorARed += slopeRedBa;
                            colorCRed += slopeRedCb;
                            colorAGreen += slopeGreenBa;
                            colorCGreen += slopeGreenCb;
                            colorABlue += slopeBlueBa;
                            colorCBlue += slopeBlueCb;
                            yA += this.scanlineWidth;
                        }
                    } else {
                        yA = ((int) (yA + 0.5F));
                        yC = ((int) (yC + 0.5F));
                        yB = ((int) (yB + 0.5F)) - yC;
                        yC -= yA;
                        for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xB, (int) xA, zNearB, zNearA, zFarB, zFarA, uB, uA, vB, vA, fogDensityB, fogDensityA, colorBAlpha, colorAAlpha, colorBRed, colorARed, colorBGreen, colorAGreen, colorBBlue, colorABlue);
                            xA += slopeXBa;
                            xB += slopeXAc;
                            zNearA += slopeZnearBa;
                            zNearB += slopeZnearAc;
                            zFarA += slopeZfarBa;
                            zFarB += slopeZfarAc;
                            uA += slopeUBa;
                            uB += slopeUAc;
                            vA += slopeVBa;
                            vB += slopeVAc;
                            fogDensityA += slopeFogDensityBa;
                            fogDensityB += slopeFogDensityAc;
                            colorAAlpha += slopeAlphaBa;
                            colorBAlpha += slopeAlphaAc;
                            colorARed += slopeRedBa;
                            colorBRed += slopeRedAc;
                            colorAGreen += slopeGreenBa;
                            colorBGreen += slopeGreenAc;
                            colorABlue += slopeBlueBa;
                            colorBBlue += slopeBlueAc;
                        }
                        while (--yB >= 0.0F) {
                            drawMaterialisedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xC, (int) xA, zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue);
                            xC += slopeXCb;
                            xA += slopeXBa;
                            zNearC += slopeZnearCb;
                            zNearA += slopeZnearBa;
                            zFarC += slopeZfarCb;
                            zFarA += slopeZfarBa;
                            uC += slopeUCb;
                            uA += slopeUBa;
                            vC += slopeVCb;
                            vA += slopeVBa;
                            fogDensityC += slopeFogDensityCb;
                            fogDensityA += slopeFogDensityBa;
                            colorCAlpha += slopeAlphaCb;
                            colorAAlpha += slopeAlphaBa;
                            colorCRed += slopeRedCb;
                            colorARed += slopeRedBa;
                            colorCGreen += slopeGreenCb;
                            colorAGreen += slopeGreenBa;
                            colorCBlue += slopeBlueCb;
                            colorABlue += slopeBlueBa;
                            yA += this.scanlineWidth;
                        }
                    }
                }
            }
        }
    }

    public void drawTriangle(boolean colorWrite, boolean depthWrite, boolean transparency, float yA, float yB, float yC, float xA, float xB, float xC, float zA, float zB, float zC, int colorA, int colorB, int colorC) {
        if (!colorWrite) {
            drawFlatTriangle(false, depthWrite, transparency, yA, yB, yC, xA, xB, xC, zA, zB, zC, 0);
        } else if (this.wireframeMode) {
            this.safemodeToolkit.drawOpaqueLine((int) xA, (int) yA, (int) xB, (int) yB, ~0xffffff | colorA);
            this.safemodeToolkit.drawOpaqueLine((int) xB, (int) yB, (int) xC, (int) yC, ~0xffffff | colorA);
            this.safemodeToolkit.drawOpaqueLine((int) xC, (int) yC, (int) xA, (int) yA, ~0xffffff | colorA);
        } else {
            float slopeXBa = xB - xA;
            float slopeYBa = yB - yA;
            float slopeXCa = xC - xA;
            float slopeYCa = yC - yA;
            float slopeZBa = zB - zA;
            float slopeZCa = zC - zA;
            float colorRedBa = ((colorB & 0xff0000) - (colorA & 0xff0000));
            float colorRedCa = ((colorC & 0xff0000) - (colorA & 0xff0000));
            float colorGreenBa = ((colorB & 0xff00) - (colorA & 0xff00));
            float colorGreenCa = ((colorC & 0xff00) - (colorA & 0xff00));
            float colorBlueBa = ((colorB & 0xff) - (colorA & 0xff));
            float colorBlueCa = ((colorC & 0xff) - (colorA & 0xff));
            float scaleCb;
            if (yC != yB) {
                scaleCb = (xC - xB) / (yC - yB);
            } else {
                scaleCb = 0.0F;
            }

            float scaleBa;
            if (yB != yA) {
                scaleBa = slopeXBa / slopeYBa;
            } else {
                scaleBa = 0.0F;
            }

            float scaleCa;
            if (yC != yA) {
                scaleCa = slopeXCa / slopeYCa;
            } else {
                scaleCa = 0.0F;
            }

            float f_31 = slopeXBa * slopeYCa - slopeXCa * slopeYBa;
            if (f_31 != 0.0F) {
                float f_32 = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / f_31;
                float f_33 = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / f_31;
                float f_34 = (colorRedBa * slopeYCa - colorRedCa * slopeYBa) / f_31;
                float f_35 = (colorRedCa * slopeXBa - colorRedBa * slopeXCa) / f_31;
                float f_36 = (colorGreenBa * slopeYCa - colorGreenCa * slopeYBa) / f_31;
                float f_37 = (colorGreenCa * slopeXBa - colorGreenBa * slopeXCa) / f_31;
                float f_38 = (colorBlueBa * slopeYCa - colorBlueCa * slopeYBa) / f_31;
                float f_39 = (colorBlueCa * slopeXBa - colorBlueBa * slopeXCa) / f_31;
                float f_40;
                float f_41;
                float f_42;
                if (yA <= yB && yA <= yC) {
                    if (yA < this.clipHeight) {
                        if (yB > this.clipHeight) {
                            yB = this.clipHeight;
                        }

                        if (yC > this.clipHeight) {
                            yC = this.clipHeight;
                        }

                        zA = zA - f_32 * xA + f_32;
                        f_41 = (colorA & 0xff0000) - f_34 * xA + f_34;
                        f_42 = (colorA & 0xff00) - f_36 * xA + f_36;
                        f_40 = (colorA & 0xff) - f_38 * xA + f_38;
                        if (yB < yC) {
                            xC = xA;
                            if (yA < 0.0F) {
                                xC = xA - scaleCa * yA;
                                xA -= scaleBa * yA;
                                zA -= f_33 * yA;
                                f_41 -= f_35 * yA;
                                f_42 -= f_37 * yA;
                                f_40 -= f_39 * yA;
                                yA = 0.0F;
                            }

                            if (yB < 0.0F) {
                                xB -= scaleCb * yB;
                                yB = 0.0F;
                            }

                            if ((yA == yB || scaleCa >= scaleBa) && (yA != yB || scaleCa <= scaleCb)) {
                                yA = ((int) (yA + 0.5F));
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yB;
                                yB -= yA;

                                for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xA, (int) xC, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleCa;
                                    xA += scaleBa;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yC >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xB, (int) xC, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleCa;
                                    xB += scaleCb;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yA += this.scanlineWidth;
                                }
                            } else {
                                yA = ((int) (yA + 0.5F));
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yB;
                                yB -= yA;

                                for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xC, (int) xA, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleCa;
                                    xA += scaleBa;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yC >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xC, (int) xB, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleCa;
                                    xB += scaleCb;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yA += this.scanlineWidth;
                                }
                            }
                        } else {
                            xB = xA;
                            if (yA < 0.0F) {
                                xB = xA - scaleCa * yA;
                                xA -= scaleBa * yA;
                                zA -= f_33 * yA;
                                f_41 -= f_35 * yA;
                                f_42 -= f_37 * yA;
                                f_40 -= f_39 * yA;
                                yA = 0.0F;
                            }

                            if (yC < 0.0F) {
                                xC -= scaleCb * yC;
                                yC = 0.0F;
                            }

                            if (yA != yC && scaleCa < scaleBa || yA == yC && scaleCb > scaleBa) {
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yB = ((int) (yB + 0.5F)) - yC;
                                yC -= yA;

                                for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xB, (int) xA, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xB += scaleCa;
                                    xA += scaleBa;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yB >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xC, (int) xA, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleCb;
                                    xA += scaleBa;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yA += this.scanlineWidth;
                                }
                            } else {
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yB = ((int) (yB + 0.5F)) - yC;
                                yC -= yA;

                                for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xA, (int) xB, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xB += scaleCa;
                                    xA += scaleBa;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yB >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yA, (int) xA, (int) xC, zA, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleCb;
                                    xA += scaleBa;
                                    zA += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yA += this.scanlineWidth;
                                }
                            }
                        }
                    }
                } else if (yB <= yC) {
                    if (yB < this.clipHeight) {
                        if (yC > this.clipHeight) {
                            yC = this.clipHeight;
                        }

                        if (yA > this.clipHeight) {
                            yA = this.clipHeight;
                        }

                        zB = zB - f_32 * xB + f_32;
                        f_41 = (colorB & 0xff0000) - f_34 * xB + f_34;
                        f_42 = (colorB & 0xff00) - f_36 * xB + f_36;
                        f_40 = (colorB & 0xff) - f_38 * xB + f_38;
                        if (yC < yA) {
                            xA = xB;
                            if (yB < 0.0F) {
                                xA = xB - scaleBa * yB;
                                xB -= scaleCb * yB;
                                zB -= f_33 * yB;
                                f_41 -= f_35 * yB;
                                f_42 -= f_37 * yB;
                                f_40 -= f_39 * yB;
                                yB = 0.0F;
                            }

                            if (yC < 0.0F) {
                                xC -= scaleCa * yC;
                                yC = 0.0F;
                            }

                            if ((yB == yC || scaleBa >= scaleCb) && (yB != yC || scaleBa <= scaleCa)) {
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yA = ((int) (yA + 0.5F)) - yC;
                                yC -= yB;

                                for (yB = this.scanlineOffsets[(int) yB]; --yC >= 0.0F; yB += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xB, (int) xA, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xA += scaleBa;
                                    xB += scaleCb;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yA >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xC, (int) xA, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xA += scaleBa;
                                    xC += scaleCa;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yB += this.scanlineWidth;
                                }
                            } else {
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yA = ((int) (yA + 0.5F)) - yC;
                                yC -= yB;

                                for (yB = this.scanlineOffsets[(int) yB]; --yC >= 0.0F; yB += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xA, (int) xB, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xA += scaleBa;
                                    xB += scaleCb;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yA >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xA, (int) xC, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xA += scaleBa;
                                    xC += scaleCa;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yB += this.scanlineWidth;
                                }
                            }
                        } else {
                            xC = xB;
                            if (yB < 0.0F) {
                                xC = xB - scaleBa * yB;
                                xB -= scaleCb * yB;
                                zB -= f_33 * yB;
                                f_41 -= f_35 * yB;
                                f_42 -= f_37 * yB;
                                f_40 -= f_39 * yB;
                                yB = 0.0F;
                            }

                            if (yA < 0.0F) {
                                xA -= scaleCa * yA;
                                yA = 0.0F;
                            }

                            if (scaleBa < scaleCb) {
                                yB = ((int) (yB + 0.5F));
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yA;
                                yA -= yB;

                                for (yB = this.scanlineOffsets[(int) yB]; --yA >= 0.0F; yB += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xC, (int) xB, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleBa;
                                    xB += scaleCb;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yC >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xA, (int) xB, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xA += scaleCa;
                                    xB += scaleCb;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yB += this.scanlineWidth;
                                }
                            } else {
                                yB = ((int) (yB + 0.5F));
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yA;
                                yA -= yB;

                                for (yB = this.scanlineOffsets[(int) yB]; --yA >= 0.0F; yB += this.scanlineWidth) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xB, (int) xC, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xC += scaleBa;
                                    xB += scaleCb;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                }

                                while (--yC >= 0.0F) {
                                    drawScanline(depthWrite, transparency, this.colourBuffer, (int) yB, (int) xB, (int) xA, zB, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                    xA += scaleCa;
                                    xB += scaleCb;
                                    zB += f_33;
                                    f_41 += f_35;
                                    f_42 += f_37;
                                    f_40 += f_39;
                                    yB += this.scanlineWidth;
                                }
                            }
                        }
                    }
                } else if (yC < this.clipHeight) {
                    if (yA > this.clipHeight) {
                        yA = this.clipHeight;
                    }

                    if (yB > this.clipHeight) {
                        yB = this.clipHeight;
                    }

                    zC = zC - f_32 * xC + f_32;
                    f_41 = (colorC & 0xff0000) - f_34 * xC + f_34;
                    f_42 = (colorC & 0xff00) - f_36 * xC + f_36;
                    f_40 = (colorC & 0xff) - f_38 * xC + f_38;
                    if (yA < yB) {
                        xB = xC;
                        if (yC < 0.0F) {
                            xB = xC - scaleCb * yC;
                            xC -= scaleCa * yC;
                            zC -= f_33 * yC;
                            f_41 -= f_35 * yC;
                            f_42 -= f_37 * yC;
                            f_40 -= f_39 * yC;
                            yC = 0.0F;
                        }

                        if (yA < 0.0F) {
                            xA -= scaleBa * yA;
                            yA = 0.0F;
                        }

                        if (scaleCb < scaleCa) {
                            yC = ((int) (yC + 0.5F));
                            yA = ((int) (yA + 0.5F));
                            yB = ((int) (yB + 0.5F)) - yA;
                            yA -= yC;

                            for (yC = this.scanlineOffsets[(int) yC]; --yA >= 0.0F; yC += this.scanlineWidth) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xB, (int) xC, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xB += scaleCb;
                                xC += scaleCa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                            }

                            while (--yB >= 0.0F) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xB, (int) xA, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xB += scaleCb;
                                xA += scaleBa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                                yC += this.scanlineWidth;
                            }
                        } else {
                            yC = ((int) (yC + 0.5F));
                            yA = ((int) (yA + 0.5F));
                            yB = ((int) (yB + 0.5F)) - yA;
                            yA -= yC;

                            for (yC = this.scanlineOffsets[(int) yC]; --yA >= 0.0F; yC += this.scanlineWidth) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xC, (int) xB, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xB += scaleCb;
                                xC += scaleCa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                            }

                            while (--yB >= 0.0F) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xA, (int) xB, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xB += scaleCb;
                                xA += scaleBa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                                yC += this.scanlineWidth;
                            }
                        }
                    } else {
                        xA = xC;
                        if (yC < 0.0F) {
                            xA = xC - scaleCb * yC;
                            xC -= scaleCa * yC;
                            zC -= f_33 * yC;
                            f_41 -= f_35 * yC;
                            f_42 -= f_37 * yC;
                            f_40 -= f_39 * yC;
                            yC = 0.0F;
                        }

                        if (yB < 0.0F) {
                            xB -= scaleBa * yB;
                            yB = 0.0F;
                        }

                        if (scaleCb < scaleCa) {
                            yC = ((int) (yC + 0.5F));
                            yB = ((int) (yB + 0.5F));
                            yA = ((int) (yA + 0.5F)) - yB;
                            yB -= yC;

                            for (yC = this.scanlineOffsets[(int) yC]; --yB >= 0.0F; yC += this.scanlineWidth) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xA, (int) xC, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xA += scaleCb;
                                xC += scaleCa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                            }

                            while (--yA >= 0.0F) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xB, (int) xC, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xB += scaleBa;
                                xC += scaleCa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                                yC += this.scanlineWidth;
                            }
                        } else {
                            yC = ((int) (yC + 0.5F));
                            yB = ((int) (yB + 0.5F));
                            yA = ((int) (yA + 0.5F)) - yB;
                            yB -= yC;

                            for (yC = this.scanlineOffsets[(int) yC]; --yB >= 0.0F; yC += this.scanlineWidth) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xC, (int) xA, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xA += scaleCb;
                                xC += scaleCa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                            }

                            while (--yA >= 0.0F) {
                                drawScanline(depthWrite, transparency, this.colourBuffer, (int) yC, (int) xC, (int) xB, zC, f_32, f_41, f_34, f_42, f_36, f_40, f_38);
                                xB += scaleBa;
                                xC += scaleCa;
                                zC += f_33;
                                f_41 += f_35;
                                f_42 += f_37;
                                f_40 += f_39;
                                yC += this.scanlineWidth;
                            }
                        }
                    }
                }
            }
        }

    }

    public void drawShadedTriangle(boolean colorWrite, boolean depthWrite, boolean transparent, float yA, float yB, float yC, float xA, float xB, float xC, float zA, float zB, float zC, float colorA, float colorB, float colorC) {
        if (!colorWrite) {
            drawFlatTriangle(false, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zA, zB, zC, 0);
        } else if (this.wireframeMode) {
            this.safemodeToolkit.drawOpaqueLine((int) xA, (int) yA, (int) xB, (int) yB, ColorUtil.hsvToRgbTable[(int) colorA & 0xffff]);
            this.safemodeToolkit.drawOpaqueLine((int) xB, (int) yB, (int) xC, (int) yC, ColorUtil.hsvToRgbTable[(int) colorA & 0xffff]);
            this.safemodeToolkit.drawOpaqueLine((int) xC, (int) yC, (int) xA, (int) yA, ColorUtil.hsvToRgbTable[(int) colorA & 0xffff]);
        } else {
            float slopeXBa = xB - xA;
            float slopeYBa = yB - yA;
            float slopeXCa = xC - xA;
            float slopeYCa = yC - yA;
            float slopeColorABa = colorB - colorA;
            float slopeColorBBa = colorC - colorA;
            float slopeZBa = zB - zA;
            float slopeZCa = zC - zA;
            float scaleXCb;
            if (yC != yB) {
                scaleXCb = (xC - xB) / (yC - yB);
            } else {
                scaleXCb = 0.0F;
            }

            float scaleXBa;
            if (yB != yA) {
                scaleXBa = slopeXBa / slopeYBa;
            } else {
                scaleXBa = 0.0F;
            }

            float scaleXCa;
            if (yC != yA) {
                scaleXCa = slopeXCa / slopeYCa;
            } else {
                scaleXCa = 0.0F;
            }

            float f_27 = slopeXBa * slopeYCa - slopeXCa * slopeYBa;
            if (f_27 != 0.0F) {
                float scaleColorB = (slopeColorABa * slopeYCa - slopeColorBBa * slopeYBa) / f_27;
                float scaleColorA = (slopeColorBBa * slopeXBa - slopeColorABa * slopeXCa) / f_27;
                float scaleZB = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / f_27;
                float scaleZA = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / f_27;
                if (yA <= yB && yA <= yC) {
                    if (yA < this.clipHeight) {
                        if (yB > this.clipHeight) {
                            yB = this.clipHeight;
                        }
                        if (yC > this.clipHeight) {
                            yC = this.clipHeight;
                        }
                        colorA = colorA - scaleColorB * xA + scaleColorB;
                        zA = zA - scaleZB * xA + scaleZB;
                        if (yB < yC) {
                            xC = xA;
                            if (yA < 0.0F) {
                                xC = xA - scaleXCa * yA;
                                xA -= scaleXBa * yA;
                                colorA -= scaleColorA * yA;
                                zA -= scaleZA * yA;
                                yA = 0.0F;
                            }
                            if (yB < 0.0F) {
                                xB -= scaleXCb * yB;
                                yB = 0.0F;
                            }
                            if (yA != yB && scaleXCa < scaleXBa || yA == yB && scaleXCa > scaleXCb) {
                                yA = ((int) (yA + 0.5F));
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yB;
                                yB -= yA;
                                for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xC, (int) xA, colorA, scaleColorB, zA, scaleZB);
                                    xC += scaleXCa;
                                    xA += scaleXBa;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                }
                                while (--yC >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xC, (int) xB, colorA, scaleColorB, zA, scaleZB);
                                    xC += scaleXCa;
                                    xB += scaleXCb;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                    yA += this.scanlineWidth;
                                }
                            } else {
                                yA = ((int) (yA + 0.5F));
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yB;
                                yB -= yA;
                                for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xA, (int) xC, colorA, scaleColorB, zA, scaleZB);
                                    xC += scaleXCa;
                                    xA += scaleXBa;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                }
                                while (--yC >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xB, (int) xC, colorA, scaleColorB, zA, scaleZB);
                                    xC += scaleXCa;
                                    xB += scaleXCb;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                    yA += this.scanlineWidth;
                                }
                            }
                        } else {
                            xB = xA;
                            if (yA < 0.0F) {
                                xB = xA - scaleXCa * yA;
                                xA -= scaleXBa * yA;
                                colorA -= scaleColorA * yA;
                                zA -= scaleZA * yA;
                                yA = 0.0F;
                            }
                            if (yC < 0.0F) {
                                xC -= scaleXCb * yC;
                                yC = 0.0F;
                            }
                            if ((yA == yC || scaleXCa >= scaleXBa) && (yA != yC || scaleXCb <= scaleXBa)) {
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yB = ((int) (yB + 0.5F)) - yC;
                                yC -= yA;
                                for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xA, (int) xB, colorA, scaleColorB, zA, scaleZB);
                                    xB += scaleXCa;
                                    xA += scaleXBa;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                }
                                while (--yB >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xA, (int) xC, colorA, scaleColorB, zA, scaleZB);
                                    xC += scaleXCb;
                                    xA += scaleXBa;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                    yA += this.scanlineWidth;
                                }
                            } else {
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yB = ((int) (yB + 0.5F)) - yC;
                                yC -= yA;

                                for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xB, (int) xA, colorA, scaleColorB, zA, scaleZB);
                                    xB += scaleXCa;
                                    xA += scaleXBa;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                }
                                while (--yB >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yA, (int) xC, (int) xA, colorA, scaleColorB, zA, scaleZB);
                                    xC += scaleXCb;
                                    xA += scaleXBa;
                                    colorA += scaleColorA;
                                    zA += scaleZA;
                                    yA += this.scanlineWidth;
                                }
                            }
                        }
                    }
                } else if (yB <= yC) {
                    if (yB < this.clipHeight) {
                        if (yC > this.clipHeight) {
                            yC = this.clipHeight;
                        }
                        if (yA > this.clipHeight) {
                            yA = this.clipHeight;
                        }
                        colorB = colorB - scaleColorB * xB + scaleColorB;
                        zB = zB - scaleZB * xB + scaleZB;
                        if (yC < yA) {
                            xA = xB;
                            if (yB < 0.0F) {
                                xA = xB - scaleXBa * yB;
                                xB -= scaleXCb * yB;
                                colorB -= scaleColorA * yB;
                                zB -= scaleZA * yB;
                                yB = 0.0F;
                            }
                            if (yC < 0.0F) {
                                xC -= scaleXCa * yC;
                                yC = 0.0F;
                            }
                            if (yB != yC && scaleXBa < scaleXCb || yB == yC && scaleXBa > scaleXCa) {
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yA = ((int) (yA + 0.5F)) - yC;
                                yC -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yC >= 0.0F; yB += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xA, (int) xB, colorB, scaleColorB, zB, scaleZB);
                                    xA += scaleXBa;
                                    xB += scaleXCb;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                }
                                while (--yA >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xA, (int) xC, colorB, scaleColorB, zB, scaleZB);
                                    xA += scaleXBa;
                                    xC += scaleXCa;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                    yB += this.scanlineWidth;
                                }
                            } else {
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yA = ((int) (yA + 0.5F)) - yC;
                                yC -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yC >= 0.0F; yB += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xB, (int) xA, colorB, scaleColorB, zB, scaleZB);
                                    xA += scaleXBa;
                                    xB += scaleXCb;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                }
                                while (--yA >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xC, (int) xA, colorB, scaleColorB, zB, scaleZB);
                                    xA += scaleXBa;
                                    xC += scaleXCa;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                    yB += this.scanlineWidth;
                                }
                            }
                        } else {
                            xC = xB;
                            if (yB < 0.0F) {
                                xC = xB - scaleXBa * yB;
                                xB -= scaleXCb * yB;
                                colorB -= scaleColorA * yB;
                                zB -= scaleZA * yB;
                                yB = 0.0F;
                            }
                            if (yA < 0.0F) {
                                xA -= scaleXCa * yA;
                                yA = 0.0F;
                            }
                            if (scaleXBa < scaleXCb) {
                                yB = ((int) (yB + 0.5F));
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yA;
                                yA -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yA >= 0.0F; yB += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xC, (int) xB, colorB, scaleColorB, zB, scaleZB);
                                    xC += scaleXBa;
                                    xB += scaleXCb;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                }
                                while (--yC >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xA, (int) xB, colorB, scaleColorB, zB, scaleZB);
                                    xA += scaleXCa;
                                    xB += scaleXCb;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                    yB += this.scanlineWidth;
                                }
                            } else {
                                yB = ((int) (yB + 0.5F));
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yA;
                                yA -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yA >= 0.0F; yB += this.scanlineWidth) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xB, (int) xC, colorB, scaleColorB, zB, scaleZB);
                                    xC += scaleXBa;
                                    xB += scaleXCb;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                }
                                while (--yC >= 0.0F) {
                                    drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yB, (int) xB, (int) xA, colorB, scaleColorB, zB, scaleZB);
                                    xA += scaleXCa;
                                    xB += scaleXCb;
                                    colorB += scaleColorA;
                                    zB += scaleZA;
                                    yB += this.scanlineWidth;
                                }
                            }
                        }
                    }
                } else if (yC < this.clipHeight) {
                    if (yA > this.clipHeight) {
                        yA = this.clipHeight;
                    }
                    if (yB > this.clipHeight) {
                        yB = this.clipHeight;
                    }
                    colorC = colorC - scaleColorB * xC + scaleColorB;
                    zC = zC - scaleZB * xC + scaleZB;
                    if (yA < yB) {
                        xB = xC;
                        if (yC < 0.0F) {
                            xB = xC - scaleXCb * yC;
                            xC -= scaleXCa * yC;
                            colorC -= scaleColorA * yC;
                            zC -= scaleZA * yC;
                            yC = 0.0F;
                        }
                        if (yA < 0.0F) {
                            xA -= scaleXBa * yA;
                            yA = 0.0F;
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = ((int) (yC + 0.5F));
                            yA = ((int) (yA + 0.5F));
                            yB = ((int) (yB + 0.5F)) - yA;
                            yA -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yA >= 0.0F; yC += this.scanlineWidth) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xB, (int) xC, colorC, scaleColorB, zC, scaleZB);
                                xB += scaleXCb;
                                xC += scaleXCa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                            }
                            while (--yB >= 0.0F) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xB, (int) xA, colorC, scaleColorB, zC, scaleZB);
                                xB += scaleXCb;
                                xA += scaleXBa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                                yC += this.scanlineWidth;
                            }
                        } else {
                            yC = ((int) (yC + 0.5F));
                            yA = ((int) (yA + 0.5F));
                            yB = ((int) (yB + 0.5F)) - yA;
                            yA -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yA >= 0.0F; yC += this.scanlineWidth) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xC, (int) xB, colorC, scaleColorB, zC, scaleZB);
                                xB += scaleXCb;
                                xC += scaleXCa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                            }
                            while (--yB >= 0.0F) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xA, (int) xB, colorC, scaleColorB, zC, scaleZB);
                                xB += scaleXCb;
                                xA += scaleXBa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                                yC += this.scanlineWidth;
                            }
                        }
                    } else {
                        xA = xC;
                        if (yC < 0.0F) {
                            xA = xC - scaleXCb * yC;
                            xC -= scaleXCa * yC;
                            colorC -= scaleColorA * yC;
                            zC -= scaleZA * yC;
                            yC = 0.0F;
                        }
                        if (yB < 0.0F) {
                            xB -= scaleXBa * yB;
                            yB = 0.0F;
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = ((int) (yC + 0.5F));
                            yB = ((int) (yB + 0.5F));
                            yA = ((int) (yA + 0.5F)) - yB;
                            yB -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yB >= 0.0F; yC += this.scanlineWidth) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xA, (int) xC, colorC, scaleColorB, zC, scaleZB);
                                xA += scaleXCb;
                                xC += scaleXCa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                            }
                            while (--yA >= 0.0F) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xB, (int) xC, colorC, scaleColorB, zC, scaleZB);
                                xB += scaleXBa;
                                xC += scaleXCa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                                yC += this.scanlineWidth;
                            }
                        } else {
                            yC = ((int) (yC + 0.5F));
                            yB = ((int) (yB + 0.5F));
                            yA = ((int) (yA + 0.5F)) - yB;
                            yB -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yB >= 0.0F; yC += this.scanlineWidth) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xC, (int) xA, colorC, scaleColorB, zC, scaleZB);
                                xA += scaleXCb;
                                xC += scaleXCa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                            }
                            while (--yA >= 0.0F) {
                                drawShadedScanline(depthWrite, transparent, this.colourBuffer, (int) yC, (int) xC, (int) xB, colorC, scaleColorB, zC, scaleZB);
                                xB += scaleXBa;
                                xC += scaleXCa;
                                colorC += scaleColorA;
                                zC += scaleZA;
                                yC += this.scanlineWidth;
                            }
                        }
                    }
                }
            }
        }
    }

    
    public void drawMultiMaterialzedScanline(int[] pixels, int y, int xA, int xB, float zNearA, float zNearB, float zFarA, float zFarB, float uA, float uB, float vA, float vB, float fogDensityA, float fogDensityB, float alphaA, float alphaB, float redA, float redB, float greenA, float greenB, float blueA, float blueB, float textureAPercentageA, float textureAPercentageB, float textureBPercentageA, float textureBPercentageB) {
        int lengthX = xB - xA;
        float scaleX = 1.0F / lengthX;
        float slopeZNearBa = (zNearB - zNearA) * scaleX;
        float slopeZFarBa = (zFarB - zFarA) * scaleX;
        float slopeUBa = (uB - uA) * scaleX;
        float slopeVBa = (vB - vA) * scaleX;
        float slopeFogDensityBa = (fogDensityB - fogDensityA) * scaleX;
        float slopeAlphaBa = (alphaB - alphaA) * scaleX;
        float slopeRedBa = (redB - redA) * scaleX;
        float slopeGreenBa = (greenB - greenA) * scaleX;
        float slopeBlueBa = (blueB - blueA) * scaleX;
        float slopeTextureAPercentageBa = (textureAPercentageB - textureAPercentageA) * scaleX;
        float slopeTextureBPercentageBa = (textureBPercentageB - textureBPercentageA) * scaleX;
        if (this.clipEdges) {
            if (xB > this.clipWidth) {
                xB = this.clipWidth;
            }
            if (xA < 0) {
                zNearA -= slopeZNearBa * xA;
                zFarA -= slopeZFarBa * xA;
                uA -= slopeUBa * xA;
                vA -= slopeVBa * xA;
                fogDensityA -= slopeFogDensityBa * xA;
                alphaA -= slopeAlphaBa * xA;
                redA -= slopeRedBa * xA;
                greenA -= slopeGreenBa * xA;
                blueA -= slopeBlueBa * xA;
                textureAPercentageA -= slopeTextureAPercentageBa * xA;
                textureBPercentageA -= slopeTextureBPercentageBa * xA;
                xA = 0;
            }
        }
        if (xA < xB) {
            lengthX = xB - xA;
            for (y += xA; lengthX-- > 0; textureBPercentageA += slopeTextureBPercentageBa) {
                float depthNear = 1.0F / zNearA;
                float depthFar = 1.0F / zFarA;
                if (depthNear < this.depthBuffer[y]) {
                	/** Calculate base material colors **/
                    float u = uA * depthFar;
                    float v = vA * depthFar;
                    int textureUA = (int) (u * this.textureSizeA * this.textureScaleA) & this.textureMaskA;
                    int textureVA = (int) (v * this.textureSizeA * this.textureScaleA) & this.textureMaskA;
                    int texturePixelA = this.texturePixelsA[textureUA + textureVA * this.textureSizeA];
                    textureUA = (int) (u * this.textureSizeB * this.textureScaleB) & this.textureMaskB;
                    textureVA = (int) (v * this.textureSizeB * this.textureScaleB) & this.textureMaskB;
                    int texturePixelB = this.texturePixelsB[textureUA + textureVA * this.textureSizeB];
                    textureUA = (int) (u * this.textureSizeC * this.textureScaleC) & this.textureMaskC;
                    textureVA = (int) (v * this.textureSizeC * this.textureScaleC) & this.textureMaskC;
                    int texturePixelC = this.texturePixelsC[textureUA + textureVA * this.textureSizeC];
                    /** Mix texture colors **/
                    float texturePercentC = 1.0F - (textureAPercentageA + textureBPercentageA);
                    texturePixelA = ~0xffffff | (int) (textureAPercentageA * (texturePixelA >> 16 & 0xff)) << 16 | (int) (textureAPercentageA * (texturePixelA >> 8 & 0xff)) << 8 | (int) (textureAPercentageA * (texturePixelA & 0xff));
                    texturePixelB = ~0xffffff | (int) (textureBPercentageA * (texturePixelB >> 16 & 0xff)) << 16 | (int) (textureBPercentageA * (texturePixelB >> 8 & 0xff)) << 8 | (int) (textureBPercentageA * (texturePixelB & 0xff));
                    texturePixelC = ~0xffffff | (int) (texturePercentC * (texturePixelC >> 16 & 0xff)) << 16 | (int) (texturePercentC * (texturePixelC >> 8 & 0xff)) << 8 | (int) (texturePercentC * (texturePixelC & 0xff));
                    int colorSum = texturePixelA + texturePixelB + texturePixelC;
                    int blendColor = ~0xffffff | (int) (redA * (colorSum >> 16 & 0xff)) << 8 & 0xff0000 | (int) (greenA * (colorSum >> 8 & 0xff)) & 0xff00 | (int) (blueA * (colorSum & 0xff)) >> 8;
                    /** Blend fog into final color **/
                    if (fogDensityA != 0.0F) {
                        int fogTransparency = (int) (255.0F - fogDensityA);
                        int fogBlendedColor = ((this.fogColour & 0xff00ff) * (int) fogDensityA & -16711936 | (int) fogDensityA * (this.fogColour & 0xff00) & 0xff0000) >>> 8;
                        blendColor = fogBlendedColor + (((blendColor & 0xff00ff) * fogTransparency & -16711936 | fogTransparency * (blendColor & 0xff00) & 0xff0000) >>> 8);
                    }
                    pixels[y] = blendColor;
                    this.depthBuffer[y] = depthNear;
                }
                y++;
                zNearA += slopeZNearBa;
                zFarA += slopeZFarBa;
                uA += slopeUBa;
                vA += slopeVBa;
                fogDensityA += slopeFogDensityBa;
                alphaA += slopeAlphaBa;
                redA += slopeRedBa;
                greenA += slopeGreenBa;
                blueA += slopeBlueBa;
                textureAPercentageA += slopeTextureAPercentageBa;
            }
        }
    }

    public void drawFlatTriangle(boolean colorWrite, boolean depthWrite, boolean transparent, float yA, float yB, float yC, float xA, float xB, float xC, float zA, float zB, float zC, int color) {
        if (this.wireframeMode) {
            this.safemodeToolkit.drawOpaqueLine((int) xA, (int) yA, (int) xB, (int) yB, color);
            this.safemodeToolkit.drawOpaqueLine((int) xB, (int) yB, (int) xC, (int) yC, color);
            this.safemodeToolkit.drawOpaqueLine((int) xC, (int) yC, (int) xA, (int) yA, color);
        } else {
            float slopeXBa = xB - xA;
            float slopeYBa = yB - yA;
            float slopeXCa = xC - xA;
            float slopeYCa = yC - yA;
            float slopeZBa = zB - zA;
            float slopeZCa = zC - zA;
            float scaleXBa = 0.0F;
            if (yB != yA) {
                scaleXBa = (xB - xA) / (yB - yA);
            }
            float scaleXCb = 0.0F;
            if (yC != yB) {
                scaleXCb = (xC - xB) / (yC - yB);
            }
            float scaleXCa = 0.0F;
            if (yC != yA) {
                scaleXCa = (xA - xC) / (yA - yC);
            }
            float length = slopeXBa * slopeYCa - slopeXCa * slopeYBa;
            if (length != 0.0F) {
                float deltaA = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / length;
                float scaleZAvg = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / length;
                if (yA <= yB && yA <= yC) {
                    if (yA < this.clipHeight) {
                        if (yB > this.clipHeight) {
                            yB = this.clipHeight;
                        }
                        if (yC > this.clipHeight) {
                            yC = this.clipHeight;
                        }
                        zA = zA - deltaA * xA + deltaA;
                        if (yB < yC) {
                            xC = xA;
                            if (yA < 0.0F) {
                                xC = xA - scaleXCa * yA;
                                xA -= scaleXBa * yA;
                                zA -= scaleZAvg * yA;
                                yA = 0.0F;
                            }
                            if (yB < 0.0F) {
                                xB -= scaleXCb * yB;
                                yB = 0.0F;
                            }
                            if (yA != yB && scaleXCa < scaleXBa || yA == yB && scaleXCa > scaleXCb) {
                                yA = ((int) (yA + 0.5F));
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yB;
                                yB -= yA;
                                for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xC, (int) xA, zA, deltaA);
                                    xC += scaleXCa;
                                    xA += scaleXBa;
                                    zA += scaleZAvg;
                                }
                                while (--yC >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xC, (int) xB, zA, deltaA);
                                    xC += scaleXCa;
                                    xB += scaleXCb;
                                    zA += scaleZAvg;
                                    yA += this.scanlineWidth;
                                }
                            } else {
                                yA = ((int) (yA + 0.5F));
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yB;
                                yB -= yA;
                                for (yA = this.scanlineOffsets[(int) yA]; --yB >= 0.0F; yA += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xA, (int) xC, zA, deltaA);
                                    xC += scaleXCa;
                                    xA += scaleXBa;
                                    zA += scaleZAvg;
                                }
                                while (--yC >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xB, (int) xC, zA, deltaA);
                                    xC += scaleXCa;
                                    xB += scaleXCb;
                                    zA += scaleZAvg;
                                    yA += this.scanlineWidth;
                                }
                            }
                        } else {
                            xB = xA;
                            if (yA < 0.0F) {
                                xB = xA - scaleXCa * yA;
                                xA -= scaleXBa * yA;
                                zA -= scaleZAvg * yA;
                                yA = 0.0F;
                            }
                            if (yC < 0.0F) {
                                xC -= scaleXCb * yC;
                                yC = 0.0F;
                            }
                            if ((yA == yC || scaleXCa >= scaleXBa) && (yA != yC || scaleXCb <= scaleXBa)) {
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yB = ((int) (yB + 0.5F)) - yC;
                                yC -= yA;
                                for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xA, (int) xB, zA, deltaA);
                                    xB += scaleXCa;
                                    xA += scaleXBa;
                                    zA += scaleZAvg;
                                }
                                while (--yB >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xA, (int) xC, zA, deltaA);
                                    xC += scaleXCb;
                                    xA += scaleXBa;
                                    zA += scaleZAvg;
                                    yA += this.scanlineWidth;
                                }
                            } else {
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yB = ((int) (yB + 0.5F)) - yC;
                                yC -= yA;
                                for (yA = this.scanlineOffsets[(int) yA]; --yC >= 0.0F; yA += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xB, (int) xA, zA, deltaA);
                                    xB += scaleXCa;
                                    xA += scaleXBa;
                                    zA += scaleZAvg;
                                }
                                while (--yB >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yA, color, (int) xC, (int) xA, zA, deltaA);
                                    xC += scaleXCb;
                                    xA += scaleXBa;
                                    zA += scaleZAvg;
                                    yA += this.scanlineWidth;
                                }
                            }
                        }
                    }
                } else if (yB <= yC) {
                    if (yB < this.clipHeight) {
                        if (yC > this.clipHeight) {
                            yC = this.clipHeight;
                        }
                        if (yA > this.clipHeight) {
                            yA = this.clipHeight;
                        }
                        zB = zB - deltaA * xB + deltaA;
                        if (yC < yA) {
                            xA = xB;
                            if (yB < 0.0F) {
                                xA = xB - scaleXBa * yB;
                                xB -= scaleXCb * yB;
                                zB -= scaleZAvg * yB;
                                yB = 0.0F;
                            }
                            if (yC < 0.0F) {
                                xC -= scaleXCa * yC;
                                yC = 0.0F;
                            }
                            if ((yB == yC || scaleXBa >= scaleXCb) && (yB != yC || scaleXBa <= scaleXCa)) {
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yA = ((int) (yA + 0.5F)) - yC;
                                yC -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yC >= 0.0F; yB += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xB, (int) xA, zB, deltaA);
                                    xA += scaleXBa;
                                    xB += scaleXCb;
                                    zB += scaleZAvg;
                                }
                                while (--yA >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xC, (int) xA, zB, deltaA);
                                    xA += scaleXBa;
                                    xC += scaleXCa;
                                    zB += scaleZAvg;
                                    yB += this.scanlineWidth;
                                }
                            } else {
                                yB = ((int) (yB + 0.5F));
                                yC = ((int) (yC + 0.5F));
                                yA = ((int) (yA + 0.5F)) - yC;
                                yC -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yC >= 0.0F; yB += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xA, (int) xB, zB, deltaA);
                                    xA += scaleXBa;
                                    xB += scaleXCb;
                                    zB += scaleZAvg;
                                }
                                while (--yA >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xA, (int) xC, zB, deltaA);
                                    xA += scaleXBa;
                                    xC += scaleXCa;
                                    zB += scaleZAvg;
                                    yB += this.scanlineWidth;
                                }
                            }
                        } else {
                            xC = xB;
                            if (yB < 0.0F) {
                                xC = xB - scaleXBa * yB;
                                xB -= scaleXCb * yB;
                                zB -= scaleZAvg * yB;
                                yB = 0.0F;
                            }
                            if (yA < 0.0F) {
                                xA -= scaleXCa * yA;
                                yA = 0.0F;
                            }
                            if (scaleXBa < scaleXCb) {
                                yB = ((int) (yB + 0.5F));
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yA;
                                yA -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yA >= 0.0F; yB += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xC, (int) xB, zB, deltaA);
                                    xC += scaleXBa;
                                    xB += scaleXCb;
                                    zB += scaleZAvg;
                                }
                                while (--yC >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xA, (int) xB, zB, deltaA);
                                    xA += scaleXCa;
                                    xB += scaleXCb;
                                    zB += scaleZAvg;
                                    yB += this.scanlineWidth;
                                }
                            } else {
                                yB = ((int) (yB + 0.5F));
                                yA = ((int) (yA + 0.5F));
                                yC = ((int) (yC + 0.5F)) - yA;
                                yA -= yB;
                                for (yB = this.scanlineOffsets[(int) yB]; --yA >= 0.0F; yB += this.scanlineWidth) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xB, (int) xC, zB, deltaA);
                                    xC += scaleXBa;
                                    xB += scaleXCb;
                                    zB += scaleZAvg;
                                }
                                while (--yC >= 0.0F) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yB, color, (int) xB, (int) xA, zB, deltaA);
                                    xA += scaleXCa;
                                    xB += scaleXCb;
                                    zB += scaleZAvg;
                                    yB += this.scanlineWidth;
                                }
                            }
                        }
                    }
                } else if (yC < this.clipHeight) {
                    if (yA > this.clipHeight) {
                        yA = this.clipHeight;
                    }
                    if (yB > this.clipHeight) {
                        yB = this.clipHeight;
                    }
                    zC = zC - deltaA * xC + deltaA;
                    if (yA < yB) {
                        xB = xC;
                        if (yC < 0.0F) {
                            xB = xC - scaleXCb * yC;
                            xC -= scaleXCa * yC;
                            zC -= scaleZAvg * yC;
                            yC = 0.0F;
                        }
                        if (yA < 0.0F) {
                            xA -= scaleXBa * yA;
                            yA = 0.0F;
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = ((int) (yC + 0.5F));
                            yA = ((int) (yA + 0.5F));
                            yB = ((int) (yB + 0.5F)) - yA;
                            yA -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yA >= 0.0F; yC += this.scanlineWidth) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xB, (int) xC, zC, deltaA);
                                xB += scaleXCb;
                                xC += scaleXCa;
                                zC += scaleZAvg;
                            }
                            while (--yB >= 0.0F) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xB, (int) xA, zC, deltaA);
                                xB += scaleXCb;
                                xA += scaleXBa;
                                zC += scaleZAvg;
                                yC += this.scanlineWidth;
                            }
                        } else {
                            yC = ((int) (yC + 0.5F));
                            yA = ((int) (yA + 0.5F));
                            yB = ((int) (yB + 0.5F)) - yA;
                            yA -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yA >= 0.0F; yC += this.scanlineWidth) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xC, (int) xB, zC, deltaA);
                                xB += scaleXCb;
                                xC += scaleXCa;
                                zC += scaleZAvg;
                            }
                            while (--yB >= 0.0F) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xA, (int) xB, zC, deltaA);
                                xB += scaleXCb;
                                xA += scaleXBa;
                                zC += scaleZAvg;
                                yC += this.scanlineWidth;
                            }
                        }
                    } else {
                        xA = xC;
                        if (yC < 0.0F) {
                            xA = xC - scaleXCb * yC;
                            xC -= scaleXCa * yC;
                            zC -= scaleZAvg * yC;
                            yC = 0.0F;
                        }
                        if (yB < 0.0F) {
                            xB -= scaleXBa * yB;
                            yB = 0.0F;
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = ((int) (yC + 0.5F));
                            yB = ((int) (yB + 0.5F));
                            yA = ((int) (yA + 0.5F)) - yB;
                            yB -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yB >= 0.0F; yC += this.scanlineWidth) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xA, (int) xC, zC, deltaA);
                                xA += scaleXCb;
                                xC += scaleXCa;
                                zC += scaleZAvg;
                            }
                            while (--yA >= 0.0F) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xB, (int) xC, zC, deltaA);
                                xB += scaleXBa;
                                xC += scaleXCa;
                                zC += scaleZAvg;
                                yC += this.scanlineWidth;
                            }
                        } else {
                            yC = ((int) (yC + 0.5F));
                            yB = ((int) (yB + 0.5F));
                            yA = ((int) (yA + 0.5F)) - yB;
                            yB -= yC;
                            for (yC = this.scanlineOffsets[(int) yC]; --yB >= 0.0F; yC += this.scanlineWidth) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xC, (int) xA, zC, deltaA);
                                xA += scaleXCb;
                                xC += scaleXCa;
                                zC += scaleZAvg;
                            }
                            while (--yA >= 0.0F) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, this.colourBuffer, (int) yC, color, (int) xC, (int) xB, zC, deltaA);
                                xB += scaleXBa;
                                xC += scaleXCa;
                                zC += scaleZAvg;
                                yC += this.scanlineWidth;
                            }
                        }
                    }
                }
            }
        }
    }

    public void drawMaterialisedScanline(boolean writeDepth, boolean transparent, int[] pixels, int bufferOffset, int xA, int xB, float zNearA, float zNearB, float zFarA, float zFarB, float uA, float uB, float vA, float vB, float fogDensityA, float fogDensityB, float alphaA, float alphaB, float redA, float redB, float greenA, float greenB, float blueA, float blueB) {
        int lengthX = xB - xA;
        float scaleX = 1.0F / lengthX;
        float slopeZnarBa = (zNearB - zNearA) * scaleX;
        float slopeZfarBa = (zFarB - zFarA) * scaleX;
        float slopeUBa = (uB - uA) * scaleX;
        float slopeVBa = (vB - vA) * scaleX;
        float slopeFogDensityBa = (fogDensityB - fogDensityA) * scaleX;
        float slopeAlphaBa = (alphaB - alphaA) * scaleX;
        float slopeRedBa = (redB - redA) * scaleX;
        float slopeGreenBa = (greenB - greenA) * scaleX;
        float slopeBlueBa = (blueB - blueA) * scaleX;
        if (this.clipEdges) {
            if (xB > this.clipWidth) {
                xB = this.clipWidth;
            }
            if (xA < 0) {
                zNearA -= slopeZnarBa * xA;
                zFarA -= slopeZfarBa * xA;
                uA -= slopeUBa * xA;
                vA -= slopeVBa * xA;
                fogDensityA -= slopeFogDensityBa * xA;
                alphaA -= slopeAlphaBa * xA;
                redA -= slopeRedBa * xA;
                greenA -= slopeGreenBa * xA;
                blueA -= slopeBlueBa * xA;
                xA = 0;
            }
        }
        if (xA < xB) {
            lengthX = xB - xA;
            for (bufferOffset += xA; lengthX-- > 0; blueA += slopeBlueBa) {
                float depth = 1.0F / zNearA;
                float aUvScale = 1.0F / zFarA;
                if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                    int v = (int) (uA * aUvScale * this.textureSizeA);
                    if (this.textureRepeat) {
                        v &= this.textureMaskA;
                    } else if (v < 0) {
                        v = 0;
                    } else if (v > this.textureMaskA) {
                        v = this.textureMaskA;
                    }
                    
                    int u = (int) (vA * aUvScale * this.textureSizeA);
                    if (this.textureRepeat) {
                        u &= this.textureMaskA;
                    } else if (u < 0) {
                        u = 0;
                    } else if (u > this.textureMaskA) {
                        u = this.textureMaskA;
                    }
                    
                    int texel = this.texturePixelsA[v + u * this.textureSizeA];
                    int alpha;
                    if (this.alphaMode == 2) {
                        alpha = texel >> 24 & 0xff;
                    } else if (this.alphaMode == 1) {
                        alpha = texel == 0 ? 0 : 255;
                    } else {
                        alpha = (int) alphaA;
                    }
                    
                    if (alpha != 0) {
                        if (alpha != 255) {
                        	int pixel = ~0xffffff | (int) (redA * (texel >> 16 & 0xff)) << 8 & 0xff0000 | (int) (greenA * (texel >> 8 & 0xff)) & 0xff00 | (int) (blueA * (texel & 0xff)) >> 8;
                            if (fogDensityA != 0.0F) {
                            	int fogTransparency = (int) (255.0F - fogDensityA);
                            	int blendedFogColor = ((this.fogColour & 0xff00ff) * (int) fogDensityA & -16711936 | (this.fogColour & 0xff00) * (int) fogDensityA & 0xff0000) >>> 8;
                                pixel = blendedFogColor + (((pixel & 0xff00ff) * fogTransparency & -16711936 | fogTransparency * (pixel & 0xff00) & 0xff0000) >>> 8);
                            }
                            int currentPixel = pixels[bufferOffset];
                            int invertedAlpha = 255 - alpha;
                            pixel = ((currentPixel & 0xff00ff) * invertedAlpha + (pixel & 0xff00ff) * alpha & -16711936) + (invertedAlpha * (currentPixel & 0xff00) + alpha * (pixel & 0xff00) & 0xff0000) >> 8;
                            if (transparent) {
                                pixels[bufferOffset] = (alpha | pixels[bufferOffset] >> 24) << 24 | pixel;
                            } else {
                                pixels[bufferOffset] = pixel;
                            }
                        } else {
                        	int pixel = ~0xffffff | (int) (redA * (texel >> 16 & 0xff)) << 8 & 0xff0000 | (int) (greenA * (texel >> 8 & 0xff)) & 0xff00 | (int) (blueA * (texel & 0xff)) >> 8;
                            if (fogDensityA != 0.0F) {
                            	int fogTransparency = (int) (255.0F - fogDensityA);
                            	int blendedFogColor = ((this.fogColour & 0xff00ff) * (int) fogDensityA & -16711936 | (this.fogColour & 0xff00) * (int) fogDensityA & 0xff0000) >>> 8;
                                pixel = blendedFogColor + (((pixel & 0xff00ff) * fogTransparency & -16711936 | fogTransparency * (pixel & 0xff00) & 0xff0000) >>> 8);
                            }
                            if (transparent) {
                                pixels[bufferOffset] = alpha << 24 | pixel;
                            } else {
                                pixels[bufferOffset] = pixel;
                            }
                        }
                        if (writeDepth) {
                            this.depthBuffer[bufferOffset] = depth;
                        }
                    }
                }
                bufferOffset++;
                zNearA += slopeZnarBa;
                zFarA += slopeZfarBa;
                uA += slopeUBa;
                vA += slopeVBa;
                fogDensityA += slopeFogDensityBa;
                alphaA += slopeAlphaBa;
                redA += slopeRedBa;
                greenA += slopeGreenBa;
            }
        }
    }

    public void drawShadedScanline(boolean writeDepth, boolean transparent, int[] pixels, int bufferOffset, int startX, int endX, float colour, float colourSlope, float depth, float depthSlope) {
        if (this.clipEdges) {
            if (endX > this.clipWidth) {
                endX = this.clipWidth;
            }
            if (startX < 0) {
                startX = 0;
            }
        }
        
		if (startX >= endX) {
			return;
		}
        
        bufferOffset += startX - 1;
        colour += colourSlope * startX;
        depth += depthSlope * startX;
        if (this.threadResource.zbuffering) {
            if (this.smoothEdges) {
            	int x = endX - startX >> 2;
                colourSlope *= 4.0F;
                if (this.blendingAlpha == 0) {
                    if (x > 0) {
                        do {
                        	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                            colour += colourSlope;
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                pixels[bufferOffset] = background;
                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                pixels[bufferOffset] = background;
                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                pixels[bufferOffset] = background;
                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                pixels[bufferOffset] = background;
                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                        } while (--x > 0);
                    }
                    x = endX - startX & 0x3;
                    if (x > 0) {
                    	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                        do {
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                                pixels[bufferOffset] = background;
                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                        } while (--x > 0);
                    }
                } else {
                	int alpha = this.blendingAlpha;
                	int invertedAlpha = 256 - this.blendingAlpha;
                    if (x > 0) {
                        do {
                        	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                            colour += colourSlope;
                            background = (invertedAlpha * (background & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (background & 0xff00) >> 8 & 0xff00);
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            	int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                } else {
                                    pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                }

                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            	int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                } else {
                                    pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                }

                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            	int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                } else {
                                    pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                }

                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            	int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                } else {
                                    pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                }
                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                        } while (--x > 0);
                    }
                    x = endX - startX & 0x3;
                    if (x > 0) {
                    	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                        background = (invertedAlpha * (background & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (background & 0xff00) >> 8 & 0xff00);
                        do {
                            bufferOffset++;
                            if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            	int pixel = pixels[bufferOffset];
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                } else {
                                    pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                                }
                                if (writeDepth) {
                                    this.depthBuffer[bufferOffset] = depth;
                                }
                            }
                            depth += depthSlope;
                        } while (--x > 0);
                    }
                }
            } else {
            	int x = endX - startX;
                if (this.blendingAlpha != 0) {
                	int alpha = this.blendingAlpha;
                	int invertedAlpha = 256 - this.blendingAlpha;
                    do {
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                        	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                            background = (invertedAlpha * (background & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (background & 0xff00) >> 8 & 0xff00);
                            int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            } else {
                                pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            }
                            if (writeDepth) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        colour += colourSlope;
                        depth += depthSlope;
                    } while (--x > 0);
                } else {
                    do {
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                            if (writeDepth) {
                                this.depthBuffer[bufferOffset] = depth;
                            }
                        }
                        depth += depthSlope;
                        colour += colourSlope;
                    } while (--x > 0);
                }
            }
        } else if (this.smoothEdges) {
        	int x = endX - startX >> 2;
            colourSlope *= 4.0F;
            if (this.blendingAlpha == 0) {
                if (x > 0) {
                    do {
                    	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                        colour += colourSlope;
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = background;
                        }
                        depth += depthSlope;
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = background;
                        }
                        depth += depthSlope;
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = background;
                        }
                        depth += depthSlope;
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = background;
                        }
                        depth += depthSlope;
                    } while (--x > 0);
                }
                x = endX - startX & 0x3;
                if (x > 0) {
                	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                    do {
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                            pixels[bufferOffset] = background;
                        }
                        depth += depthSlope;
                    } while (--x > 0);
                }
            } else {
            	int alpha = this.blendingAlpha;
            	int invertedAlpha = 256 - this.blendingAlpha;
                if (x > 0) {
                    do {
                    	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                        colour += colourSlope;
                        background = (invertedAlpha * (background & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (background & 0xff00) >> 8 & 0xff00);
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                        	int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            } else {
                                pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            }
                        }
                        depth += depthSlope;
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                        	int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            } else {
                                pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            }
                        }

                        depth += depthSlope;
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                        	int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            } else {
                                pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            }
                        }

                        depth += depthSlope;
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                        	int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            } else {
                                pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            }
                        }
                        depth += depthSlope;
                    } while (--x > 0);
                }
                x = endX - startX & 0x3;
                if (x > 0) {
                	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                    background = (invertedAlpha * (background & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (background & 0xff00) >> 8 & 0xff00);
                    do {
                        bufferOffset++;
                        if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                        	int pixel = pixels[bufferOffset];
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            } else {
                                pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                            }
                        }
                        depth += depthSlope;
                    } while (--x > 0);
                }
            }
        } else {
        	int x = endX - startX;
            if (this.blendingAlpha != 0) {
            	int alpha = this.blendingAlpha;
            	int invertedAlpha = 256 - this.blendingAlpha;
                do {
                    bufferOffset++;
                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                    	int background = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                        background = (invertedAlpha * (background & 0xff00ff) >> 8 & 0xff00ff) + (invertedAlpha * (background & 0xff00) >> 8 & 0xff00);
                        int pixel = pixels[bufferOffset];
                        if (transparent) {
                            pixels[bufferOffset] = (invertedAlpha | pixel >> 24) << 24 | background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                        } else {
                            pixels[bufferOffset] = background + ((pixel & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha * (pixel & 0xff00) >> 8 & 0xff00);
                        }
                    }
                    colour += colourSlope;
                    depth += depthSlope;
                } while (--x > 0);
            } else {
                do {
                    bufferOffset++;
                    if (!writeDepth || depth < this.depthBuffer[bufferOffset]) {
                        pixels[bufferOffset] = ColorUtil.hsvToRgbTable[(int) colour & 0xffff];
                    }
                    depth += depthSlope;
                    colour += colourSlope;
                } while (--x > 0);
            }
        }
    }
}
