package com.jagex.game.runetek5.graphics.font;

import java.awt.*;
import java.awt.image.PixelGrabber;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class HelveticaFont {

    public static final int VALID_CHARACTER_COUNT = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| \u00c4\u00cb\u00cf\u00d6\u00dc\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff\u00df\u00c1\u00c0\u00c9\u00c8\u00cd\u00cc\u00d3\u00d2\u00da\u00d9\u00e1\u00e0\u00e9\u00e8\u00ed\u00ec\u00f3\u00f2\u00fa\u00f9\u00c2\u00ca\u00ce\u00d4\u00db\u00e2\u00ea\u00ee\u00f4\u00fb\u00c6\u00e6\u00e3\u00c3\u00f5\u00d5\u00e7\u00c7".length();
    public static final int[] VALID_CHARACTERS = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int ch = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| \u00c4\u00cb\u00cf\u00d6\u00dc\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff\u00df\u00c1\u00c0\u00c9\u00c8\u00cd\u00cc\u00d3\u00d2\u00da\u00d9\u00e1\u00e0\u00e9\u00e8\u00ed\u00ec\u00f3\u00f2\u00fa\u00f9\u00c2\u00ca\u00ce\u00d4\u00db\u00e2\u00ea\u00ee\u00f4\u00fb\u00c6\u00e6\u00e3\u00c3\u00f5\u00d5\u00e7\u00c7".indexOf(i);
            if (ch == -1)
                ch = 74;
            VALID_CHARACTERS[i] = ch;
        }

    }

    public int sizeY;
    public int startY;
    public boolean loadPlain;
    public final int[] clipping = new int[4];
    public NativeSprite[] nativeSprites;
    public int[] characterWidths;

    public HelveticaFont(AbstractToolkit toolkit, int fontSize, Component component) {
        this.loadPlain = false;
        this.nativeSprites = new NativeSprite[256];
        this.characterWidths = new int[256];
        Font font = new Font("Helvetica", Font.BOLD, fontSize);
        java.awt.FontMetrics fontmetrics = component.getFontMetrics(font);
        for (int index = 0; index < VALID_CHARACTER_COUNT; index++)
            initialize(toolkit, font, fontmetrics, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| \u00c4\u00cb\u00cf\u00d6\u00dc\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff\u00df\u00c1\u00c0\u00c9\u00c8\u00cd\u00cc\u00d3\u00d2\u00da\u00d9\u00e1\u00e0\u00e9\u00e8\u00ed\u00ec\u00f3\u00f2\u00fa\u00f9\u00c2\u00ca\u00ce\u00d4\u00db\u00e2\u00ea\u00ee\u00f4\u00fb\u00c6\u00e6\u00e3\u00c3\u00f5\u00d5\u00e7\u00c7".charAt(index), index, false);
        if (this.loadPlain) {
            this.loadPlain = false;
            font = new Font("Helvetica", Font.PLAIN, fontSize);
            fontmetrics = component.getFontMetrics(font);
            for (int index = 0; index < VALID_CHARACTER_COUNT; index++)
                initialize(toolkit, font, fontmetrics, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| \u00c4\u00cb\u00cf\u00d6\u00dc\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff\u00df\u00c1\u00c0\u00c9\u00c8\u00cd\u00cc\u00d3\u00d2\u00da\u00d9\u00e1\u00e0\u00e9\u00e8\u00ed\u00ec\u00f3\u00f2\u00fa\u00f9\u00c2\u00ca\u00ce\u00d4\u00db\u00e2\u00ea\u00ee\u00f4\u00fb\u00c6\u00e6\u00e3\u00c3\u00f5\u00d5\u00e7\u00c7".charAt(index), index, false);
            if (!this.loadPlain) {
                this.loadPlain = false;
                for (int index = 0; index < VALID_CHARACTER_COUNT; index++)
                    initialize(toolkit, font, fontmetrics, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\u00a3$%^&*()-_=+[{]};:'@#~,<.>/?\\| \u00c4\u00cb\u00cf\u00d6\u00dc\u00e4\u00eb\u00ef\u00f6\u00fc\u00ff\u00df\u00c1\u00c0\u00c9\u00c8\u00cd\u00cc\u00d3\u00d2\u00da\u00d9\u00e1\u00e0\u00e9\u00e8\u00ed\u00ec\u00f3\u00f2\u00fa\u00f9\u00c2\u00ca\u00ce\u00d4\u00db\u00e2\u00ea\u00ee\u00f4\u00fb\u00c6\u00e6\u00e3\u00c3\u00f5\u00d5\u00e7\u00c7".charAt(index), index, true);
            }
        }
    }

    public int getLineHeight() {
        return this.startY - 1;
    }

    public void drawText(String text, int drawX, int drawY, int color) {
        boolean hasColor = color != 0;

        color |= ~0xffffff;

        for (int index = 0; index < text.length(); index++) {
            int c = VALID_CHARACTERS[text.charAt(index)];
            if (hasColor) {
                this.nativeSprites[c].draw(drawX + 1, drawY + 1, 0, ~0xffffff, 1);
            }

            this.nativeSprites[c].draw(drawX, drawY, 0, color, 1);
            drawX += this.characterWidths[c];
        }
    }

    public int getTextWidth(String text) {
        int totalWidth = 0;

        for (int index = 0; index < text.length(); index++) {
            int c = VALID_CHARACTERS[text.charAt(index)];
            totalWidth += this.characterWidths[c];
        }

        return totalWidth;
    }

    public void drawClippedText(AbstractToolkit toolkit, String text, int x, int y, int color) {
        int textCenterX = getTextWidth(text) / 2;
        toolkit.getClipping(this.clipping);
        if (x - textCenterX <= this.clipping[2] && x + textCenterX >= this.clipping[0] && y - this.sizeY <= this.clipping[3] && y + this.startY >= this.clipping[1]) {
            drawText(text, x - textCenterX, y, color);
        }
    }

    public void initialize(AbstractToolkit toolkit, Font font, java.awt.FontMetrics fontmetrics, char c, int cacheIndex, boolean large) {
        int width = fontmetrics.charWidth(c);
        int characterWidth = width;
        if (large) {
            try {
                if (c == 47) {
                    large = false;
                }

                if (c == 102 || c == 116 || c == 119 || c == 118 || c == 107 || c == 120 || c == 121 || c == 65 || c == 86 || c == 87) {
                    ++width;
                }
            } catch (Exception ignored) {
            	// NOOP
            }
        }

        int maxAscent = fontmetrics.getMaxAscent();
        int height = fontmetrics.getMaxAscent() + fontmetrics.getMaxDescent();
        int strtY = fontmetrics.getHeight();
        Image image = GameShell.gameCanvas.createImage(width, height);
        Graphics graphics = image.getGraphics();
        graphics.setColor(Color.black);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.white);
        graphics.setFont(font);
        graphics.drawString(c + "", 0, maxAscent);
        if (large) {
            graphics.drawString(c + "", 1, maxAscent);
        }

        int[] pixels = new int[width * height];
        PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

        try {
            pixelGrabber.grabPixels();
        } catch (Exception ignored) {
        	// NOOP
        }

        image.flush();
        Object obj16 = null;
        int Y = 0;

        label66:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[x + y * width];
                if ((pixel & 0xffffff) != 0) {
                    Y = y;
                    break label66;
                }
            }
        }

        for (int index = 0; index < pixels.length; index++) {
            if ((pixels[index] & 0xffffff) == 0) {
                pixels[index] = 0;
            }
        }

        this.sizeY = maxAscent - Y;
        this.startY = strtY;
        this.characterWidths[cacheIndex] = characterWidth;
        this.nativeSprites[cacheIndex] = toolkit.createNativeSprite(pixels, width, width, height);
    }

    public int method3186() {
        return this.sizeY;
    }

}
