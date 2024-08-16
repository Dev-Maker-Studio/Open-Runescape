package com.jagex.game.runetek5.graphics.font;

import java.util.Random;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.Utils;

/**
 * 
 * @author Brainpower - 21/03/2023
 *
 */
public abstract class AbstractFontRenderer {

    public static int strikeThroughEffect = -1;
    public static int underlineEffect = -1;
    public static int colorArgb;
    public static int argbColorEffect;
    public static int anInt65;
    public static int shadingEffect;
    public static int anInt67;
    public static int anInt68;
    public static final String[] LINE_BUFFER = new String[100];
    public final AbstractToolkit toolkit;
    public final FontMetrics fontMetrics;

    protected AbstractFontRenderer(AbstractToolkit graphicalrenderer1, FontMetrics fontmetrics2) {
        this.toolkit = graphicalrenderer1;
        this.fontMetrics = fontmetrics2;
    }

    public void renderPlain(String text, int x, int y, int color, int shadow) {
        if (text != null) {
            calibrate(color, shadow);
            renderLine(text, x, y, null, null, null, 0, 0);
        }
    }

    public void renderLeftAligned(String text, int x, int y, int color, int shadow) {
        if (text != null) {
            calibrate(color, shadow);
            renderLine(text, x - this.fontMetrics.getWidthNoSprites(text), y, null, null, null, 0, 0);
        }
    }

    public void renderCentered(String text, int x, int y, int color, int shadow) {
        if (text != null) {
            calibrate(color, shadow);
            renderLine(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, 0, 0);
        }
    }

    public void calibrate(int textColor, int shadowEffect) {
        int shadeEff = shadowEffect;
        strikeThroughEffect = -1;
        underlineEffect = -1;
        colorArgb = textColor;
        argbColorEffect = textColor;
        anInt67 = 0;
        anInt68 = 0;
        if (shadeEff == -1) {
            shadeEff = 0;
        }
        anInt65 = shadeEff;
        shadingEffect = shadeEff;
    }

	public int renderTextAntiMacro(String chatText, int x, int y, int maxWidth, int maxHeight, int textEffectParam1,
                                   int textEffectParam2, int horizontalAlign, int verticalAlign, Random random, int seed, int[] dimensions,
                                   NativeSprite[] chatIcons, int[] chatScaleHeights) {
		if (chatText == null) {
			return 0;
		}
		random.setSeed(seed);
		int textColor = 192 + (random.nextInt() & 0x1f);
		calibrate(textColor << 24 | textEffectParam1 & 0xffffff,
				textEffectParam2 == -1 ? 0 : textColor << 24 | textEffectParam2 & 0xffffff);
		int textLength = chatText.length();
		int[] glyphWidths = new int[textLength];
		int textWidth = 0;
		for (int index = 0; index < textLength; index++) {
			glyphWidths[index] = textWidth;
			if ((random.nextInt() & 0x3) == 0) {
				textWidth++;
			}
		}
		int drawX = x;
		int drawY = y + this.fontMetrics.topPadding;
		int plainWidth = -1;
		if (verticalAlign == 1) {
			drawY += (maxHeight - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding) / 2;
		} else if (verticalAlign == 2) {
			drawY = y + maxHeight - this.fontMetrics.bottomPadding;
		}
		if (horizontalAlign == 1) {
			plainWidth = this.fontMetrics.getWidthNoSprites(chatText) + textWidth;
			drawX = (maxWidth - plainWidth) / 2 + x;
		} else if (horizontalAlign == 2) {
			plainWidth = this.fontMetrics.getWidthNoSprites(chatText) + textWidth;
			drawX = x + (maxWidth - plainWidth);
		}
		method372(chatText, drawX, drawY, chatIcons, chatScaleHeights, glyphWidths, null);
		if (dimensions != null) {
			if (plainWidth == -1) {
				plainWidth = this.fontMetrics.getWidthNoSprites(chatText) + textWidth;
			}
			dimensions[0] = drawX;
			dimensions[1] = drawY - this.fontMetrics.topPadding;
			dimensions[2] = plainWidth;
			dimensions[3] = this.fontMetrics.bottomPadding + this.fontMetrics.topPadding;
		}
		return textWidth;
	}

    public void updateTextEffects(String effectString) {
        try {
            if (effectString.startsWith("col=")) {
                argbColorEffect = argbColorEffect & -16777216 | Utils.parse_int(effectString.substring(4)) & 0xffffff;
            } else if ("/col".equals(effectString)) {
                argbColorEffect = argbColorEffect & -16777216 | colorArgb & 0xffffff;
            }
            if ("argb=".startsWith(effectString)) {
                argbColorEffect = Utils.parse_int(effectString.substring(5));
            } else if ("/argb".equals(effectString)) {
                argbColorEffect = colorArgb;
            } else if ("str=".startsWith(effectString)) {
                strikeThroughEffect = argbColorEffect & -16777216 | Utils.parse_int(effectString.substring(4));
            } else if ("str".equals(effectString)) {
                strikeThroughEffect = argbColorEffect & -16777216 | 0x800000;
            } else if ("/str".equals(effectString)) {
                strikeThroughEffect = -1;
            } else if ("u=".startsWith(effectString)) {
                underlineEffect = argbColorEffect & -16777216 | Utils.parse_int(effectString.substring(2));
            } else if ("u".equals(effectString)) {
                underlineEffect = argbColorEffect & -16777216;
            } else if ("/u".equals(effectString)) {
                underlineEffect = -1;
            } else if ("shad=-1".equalsIgnoreCase(effectString)) {
                shadingEffect = 0;
            } else if ("shad=".startsWith(effectString)) {
                shadingEffect = argbColorEffect & -16777216 | Utils.parse_int(effectString.substring(5));
            } else if ("shad".equals(effectString)) {
                shadingEffect = argbColorEffect & -16777216;
            } else if ("/shad".equals(effectString)) {
                shadingEffect = anInt65;
            } else if ("br".equals(effectString)) {
                calibrate(colorArgb, anInt65);
            }
        } catch (Exception exception) {
        	// NOOP
        }
    }

    public void method370(String string_1, int i_2) {
        int i_4 = 0;
        boolean bool5 = false;
        for (int i_6 = 0; i_6 < string_1.length(); i_6++) {
            char var_7 = string_1.charAt(i_6);
            if (var_7 == 60) {
                bool5 = true;
            } else if (var_7 == 62) {
                bool5 = false;
            } else if (!bool5 && var_7 == 32) {
                ++i_4;
            }
        }
        if (i_4 > 0) {
            anInt67 = (i_2 - this.fontMetrics.getWidthNoSprites(string_1) << 8) / i_4;
        }
    }

    public void renderLine(String text, int x, int y, NativeSprite[] sprites, int[] spriteHeights, ComponentSurface dimensions, int shadingEffect, int colorEffect) {
        y -= this.fontMetrics.verticalSpacing;
        int tagStart = -1;
        int prevChar = -1;
        int textLength = text.length();
        for (int i = 0; i < textLength; i++) {
            char ch = (char) (Utils.byteForChar(text.charAt(i)) & 0xff);
            if (ch == 60) {
                tagStart = i;
            } else {
                if (ch == 62 && tagStart != -1) {
                    String string_15 = text.substring(tagStart + 1, i);
                    tagStart = -1;
                    if ("lt".equals(string_15)) {
                        ch = 60;
                    } else if ("gt".equals(string_15)) {
                        ch = 62;
                    } else if ("nbsp".equals(string_15)) {
                        ch = 160;
                    } else if ("shy".equals(string_15)) {
                        ch = 173;
                    } else if ("times".equals(string_15)) {
                        ch = 215;
                    } else if ("euro".equals(string_15)) {
                        ch = 8364;
                    } else if ("copy".equals(string_15)) {
                        ch = 169;
                    } else {
                        if (!"reg".equals(string_15)) {
                            if (string_15.startsWith("img=")) {
                                try {
                                    int spriteIndex = Utils.parseInt(string_15.substring(4));
                                    NativeSprite sprite = sprites[spriteIndex];
                                    int spriteHeight = spriteHeights != null ? spriteHeights[spriteIndex] : sprite.getSpriteHeight();
                                    if ((argbColorEffect & -16777216) == -16777216) {
                                        sprite.draw(x, y + this.fontMetrics.verticalSpacing - spriteHeight, 1, 0, 1);
                                    } else {
                                        sprite.draw(x, y + this.fontMetrics.verticalSpacing - spriteHeight, 0, argbColorEffect & -16777216 | 0xffffff, 1);
                                    }
                                    x += sprites[spriteIndex].getSpriteWidth();
                                    prevChar = -1;
                                } catch (Exception ignored) {
                                }
                            } else {
                                updateTextEffects(string_15);
                            }
                            continue;
                        }
                        ch = 174;
                    }
                }
                if (tagStart == -1) {
                    if (prevChar != -1) {
                        x += this.fontMetrics.getKerning(prevChar, ch);
                    }
                    if (ch != 32) {
                        if (dimensions == null) {
                            if ((shadingEffect & -16777216) != 0) {
                                drawChar(ch, x + 1, y + 1, shadingEffect, true);
                            }
                            drawChar(ch, x, y, argbColorEffect, false);
                        } else {
                            if ((shadingEffect & -16777216) != 0) {
                                drawChar(ch, x + 1, y + 1, shadingEffect, true, dimensions, shadingEffect, colorEffect);
                            }
                            drawChar(ch, x, y, argbColorEffect, false, dimensions, shadingEffect, colorEffect);
                        }
                    } else if (anInt67 > 0) {
                        anInt68 += anInt67;
                        x += anInt68 >> 8;
                        anInt68 &= 0xff;
                    }
                    int glyphWidth = this.fontMetrics.getGlyphWidth(ch);
                    if (strikeThroughEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(x, y + (int) (0.7D * this.fontMetrics.verticalSpacing), glyphWidth, strikeThroughEffect);
                    }
                    if (underlineEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(x, y + this.fontMetrics.verticalSpacing + 1, glyphWidth, underlineEffect);
                    }
                    x += glyphWidth;
                    prevChar = ch;
                }
            }
        }
    }

    public void method372(String chatText, int drawX, int drawY, NativeSprite[] chatIcons, int[] chatScaleHeights, int[] widths, int[] heights) {
        drawY -= this.fontMetrics.verticalSpacing;
        int beginIndex = -1;
        int i_10 = -1;
        int imageIndex = 0;
        int textLength = chatText.length();
        for (int charIndex = 0; charIndex < textLength; charIndex++) {
            char reference = (char) (Utils.byteForChar(chatText.charAt(charIndex)) & 0xff);
            if (reference == 60) {
                beginIndex = charIndex;
            } else {
                if (reference == 62 && beginIndex != -1) {
                    String effect = chatText.substring(beginIndex + 1, charIndex);
                    beginIndex = -1;
                    if ("lt".equals(effect)) {
                        reference = 60;
                    } else if ("gt".equals(effect)) {
                        reference = 62;
                    } else if ("nbsp".equals(effect)) {
                        reference = 160;
                    } else if ("shy".equals(effect)) {
                        reference = 173;
                    } else if ("times".equals(effect)) {
                        reference = 215;
                    } else if ("euro".equals(effect)) {
                        reference = 8364;
                    } else if ("copy".equals(effect)) {
                        reference = 169;
                    } else {
                        if (!"reg".equals(effect)) {
                            if (effect.startsWith("img=")) {
                                try {
                                	int w;
                                	int h;
                                    if (widths != null) {
                                        w = widths[imageIndex];
                                    } else {
                                        w = 0;
                                    }
                                    if (heights != null) {
                                        h = heights[imageIndex];
                                    } else {
                                        h = 0;
                                    }
                                    imageIndex++;
                                    int iconIndex = Utils.parseInt(effect.substring(4));
                                    NativeSprite chatIcon = chatIcons[iconIndex];
                                    int scaleHeight = chatScaleHeights != null ? chatScaleHeights[iconIndex] : chatIcon.getSpriteHeight();
                                    chatIcon.draw(w + drawX, drawY + this.fontMetrics.verticalSpacing - scaleHeight + h, 1, 0, 1);
                                    drawX += chatIcons[iconIndex].getSpriteWidth();
                                    i_10 = -1;
                                } catch (Exception exception) {
                                	// NOOP
                                }
                            } else {
                                updateTextEffects(effect);
                            }
                            continue;
                        }
                        reference = 174;
                    }
                }
                if (beginIndex == -1) {
                    if (i_10 != -1) {
                        drawX += this.fontMetrics.getKerning(i_10, reference);
                    }
                    int w;
                    int h;
                    if (widths != null) {
                        w = widths[imageIndex];
                    } else {
                        w = 0;
                    }
                    if (heights != null) {
                        h = heights[imageIndex];
                    } else {
                        h = 0;
                    }
                    ++imageIndex;
                    if (reference != 32) {
                        if ((shadingEffect & -16777216) != 0) {
                            drawChar(reference, w + drawX + 1, drawY + h + 1, shadingEffect, true);
                        }
                        drawChar(reference, w + drawX, drawY + h, argbColorEffect, false);
                    } else if (anInt67 > 0) {
                        anInt68 += anInt67;
                        drawX += anInt68 >> 8;
                        anInt68 &= 0xff;
                    }
                    int glyphWidth = this.fontMetrics.getGlyphWidth(reference);
                    if (strikeThroughEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(drawX, drawY + (int) (this.fontMetrics.verticalSpacing * 0.7D), glyphWidth, strikeThroughEffect);
                    }
                    if (underlineEffect != -1) {
                        this.toolkit.drawOpaqueHorizontalLine(drawX, drawY + this.fontMetrics.verticalSpacing, glyphWidth, underlineEffect);
                    }
                    drawX += glyphWidth;
                    i_10 = reference;
                }
            }
        }
    }

    public abstract void drawChar(int var1, int var2, int var3, int var4, boolean var5);

    public int renderText(String text, int x, int y, int width, int height, int textColor, int shadowColor, int alignment, int verticalAlignment, int lineSpacing, int maxLines, NativeSprite[] sprites, int[] spriteIndices, ComponentSurface dimensions, int leftPadding, int topPadding) {
        int currentLineSpacing = lineSpacing;
        int maxLinesToShow = maxLines;
        int verticalAlignmentType = verticalAlignment;
        if (text == null) {
            return 0;
        } 
        calibrate(textColor, shadowColor);
        if (currentLineSpacing == 0) {
            currentLineSpacing = this.fontMetrics.verticalSpacing;
        }
        int[] lineWidths;
        if (height < currentLineSpacing + this.fontMetrics.bottomPadding + this.fontMetrics.topPadding && height < currentLineSpacing + currentLineSpacing) {
            lineWidths = null;
        } else {
            lineWidths = new int[]{width};
        }
        int numLines = this.fontMetrics.splitLinesToBuffer(text, lineWidths, LINE_BUFFER, sprites);
        if (maxLinesToShow == -1) {
            maxLinesToShow = height / currentLineSpacing;
            if (maxLinesToShow <= 0) {
                maxLinesToShow = 1;
            }
        }
        if (maxLinesToShow > 0 && numLines >= maxLinesToShow) {
            LINE_BUFFER[maxLinesToShow - 1] = this.fontMetrics.fitText(LINE_BUFFER[maxLinesToShow - 1], width, sprites);
            numLines = maxLinesToShow;
        }
        if (verticalAlignmentType == 3 && numLines == 1) {
            verticalAlignmentType = 1;
        }
        int currentY;
        if (verticalAlignmentType == 0) {
            currentY = y + this.fontMetrics.topPadding;
        } else if (verticalAlignmentType == 1) {
            currentY = (height - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1)) / 2 + y + this.fontMetrics.topPadding;
        } else if (verticalAlignmentType == 2) {
            currentY = y + height - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1);
        } else {
            int lineSpacingToAdd = (height - this.fontMetrics.topPadding - this.fontMetrics.bottomPadding - currentLineSpacing * (numLines - 1)) / (numLines + 1);
            if (lineSpacingToAdd < 0) {
                lineSpacingToAdd = 0;
            }
            currentY = y + this.fontMetrics.topPadding + lineSpacingToAdd;
            currentLineSpacing += lineSpacingToAdd;
        }
        for (int i = 0; i < numLines; i++) {
            if (alignment == 0) {
                renderLine(LINE_BUFFER[i], x, currentY, sprites, spriteIndices, dimensions, leftPadding, topPadding);
            } else if (alignment == 1) {
                renderLine(LINE_BUFFER[i], x + (width - this.fontMetrics.getWidthNoSprites(LINE_BUFFER[i])) / 2, currentY, sprites, spriteIndices, dimensions, leftPadding, topPadding);
            } else if (alignment == 2) {
                renderLine(LINE_BUFFER[i], x + width - this.fontMetrics.getWidthNoSprites(LINE_BUFFER[i]), currentY, sprites, spriteIndices, dimensions, leftPadding, topPadding);
            } else if (i == numLines - 1) {
                renderLine(LINE_BUFFER[i], x, currentY, sprites, spriteIndices, dimensions, leftPadding, topPadding);
            } else {
                method370(LINE_BUFFER[i], width);
                renderLine(LINE_BUFFER[i], x, currentY, sprites, spriteIndices, dimensions, leftPadding, topPadding);
                anInt67 = 0;
            }
            currentY += currentLineSpacing;
        }
        return numLines;
    }

    public abstract void drawChar(int var1, int var2, int var3, int var4, boolean var5, ComponentSurface dimension, int shadingEffect, int colorEffect);

    public int drawText(String text, int x, int y, int width, int height, int textColor, int shadowColor, int allignment, int verticalAllignment, int lineSpacing, NativeSprite[] sprites, int[] spriteIndices, ComponentSurface dimensions, int leftPadding, int topPadding) {
        return renderText(text, x, y, width, height, textColor, shadowColor, allignment, verticalAllignment, lineSpacing, 0, sprites, spriteIndices, dimensions, leftPadding, topPadding);
    }

    public void renderCenteredShake(String text, int x, int y, int color, int i_6, int i_7) {
        if (text != null) {
            calibrate(color, -16777216);
            double step = 7.0D - i_7 / 8.0D;
            if (step < 0.0D) {
                step = 0.0D;
            }
            int length = text.length();
            int[] shakeOffsetsY = new int[length];
            for (int i = 0; i < length; i++) {
                shakeOffsetsY[i] = (int) (Math.sin(i_6 + i / 1.5D) * step);
            }
            method372(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, shakeOffsetsY);
        }
    }

    public int method386(String text, int x, int y, int i_4, Random random, int randomSeed, NativeSprite[] sprites, int[] scaleHeights) {
        if (text == null) {
            return 0;
        } 
        random.setSeed(randomSeed);
        int color = 192 + (random.nextInt() & 0x1f);
        calibrate(color << 24 | i_4 & 0xffffff, color << 24);
        int length = text.length();
        int[] heights = new int[length];
        int height = 0;
        for (int i = 0; i < length; i++) {
            heights[i] = height;
            if ((random.nextInt() & 0x3) == 0) {
                height++;
            }
        }
        method372(text, x, y, sprites, scaleHeights, heights, null);
        return height;
    }

    // Only vertical
    public void renderCenteredWave(String text, int x, int y, int color, int waveSeed) {
        if (text != null) {
            calibrate(color, -16777216);
            int length = text.length();
            int[] waveOffsetsY = new int[length];
            for (int i = 0; i < length; i++) {
                waveOffsetsY[i] = (int) (Math.sin(waveSeed / 5.0D + i / 2.0D) * 5.0D);
            }
            method372(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, null, waveOffsetsY);
        }
    }
    
    // Horizontal & vertical
    public void renderCenteredWave2(String text, int x, int y, int color, int waveSeed) {
        if (text != null) {
            calibrate(color, -16777216);
            int length = text.length();
            int[] waveOffsetsX = new int[length];
            int[] waveOffsetsY = new int[length];
            for (int i = 0; i < length; i++) {
                waveOffsetsX[i] = (int) (Math.sin(waveSeed / 5.0D + i / 5.0D) * 5.0D);
                waveOffsetsY[i] = (int) (Math.sin(waveSeed / 5.0D + i / 3.0D) * 5.0D);
            }
            method372(text, x - this.fontMetrics.getWidthNoSprites(text) / 2, y, null, null, waveOffsetsX, waveOffsetsY);
        }
    }
}
