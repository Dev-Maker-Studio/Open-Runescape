package com.jagex.game.runetek5.graphics.loginscreen;

import java.awt.*;
import java.io.InputStream;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.LoadingStage;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.utils.Utils;

/**
 * Draws the vertically centered loading bar in the first seconds of the client starting
 * as to give an indication the client is loading while the login screen assets are
 * yet to display
 * @author Steven - 01/10/2023
 *
 */
public class LegacyAssetLoadingScreen implements LoginScreenComponent {

    public boolean resourcesUnavailable;
    public boolean drawInCenterX;
    public boolean drawInCenterY;
    public int candyBarOffsetX;
    public int candyBarOffsetY;
    public int candyBarWidth;
    public int candyBarHeight;
    public int anInt3569;
    public int anInt3590;
    public int anInt3591;
    public int anInt3592;
    public Font font;
    public java.awt.FontMetrics fontMetrics;
    public Color color;
    public Image candyBarMiddle;
    public Image candyBarBack;
    public Image candyBarOutlineLeft;
    public Image candyBarOutlineRight;
    public Image candyBarOutlineTop;
    public Image candyBarOutlineBottom;
    public Image loadbarBodyLeft;
    public Image loadbarBodyRight;
    public Image loadbarBodyFill;
    public int candyBarElementCount;
    public Image candyBarImage;
	public static Image canvasImage;

    public int getDrawX(int width) {
        return this.drawInCenterX ? (GameShell.screenCanvasWidth - width) / 2 : 0;
    }

    public int getDrawY(int height) {
        return this.drawInCenterY ? (GameShell.screenCanvasHeight * -969250379 - height) / 2 : 0;
    }

    @Override
    public void cleanup() {
        LegacyAssetLoadingScreen.cleanupPlainLoadingbarResources();
    }

    public Image getImageFromResource(String resourceName) {
        try {
            byte[] buffer;
            try (InputStream inputstream = GameShell.suppliedApplet.getClass().getClassLoader().getResourceAsStream(resourceName)) {
                buffer = new byte[4000];
                int data;
                if (inputstream == null) return null;
                assert inputstream != null;
                for (int index = 0; (data = inputstream.read()) != -1; buffer[index++] = (byte) data) {
                }
            }
            return Toolkit.getDefaultToolkit().createImage(buffer);
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public void initialize() {
    }

    @Override
    public int calculateLoadingPercent() {
        return 100;
    }

    @Override
    public boolean finished(long long_1) {
        return true;
    }

    @Override
    public int timeBetweenUpdatesMs() {
        return 0;
    }
  
    @Override
    public void draw() {
        if (!this.resourcesUnavailable && this.font == null) {
            try {
                String[] resourceNames = Utils.split(client.loginScreenImageResourceNames, ',');
                if (resourceNames.length != 23) {
                    this.resourcesUnavailable = true;
                }
                this.drawInCenterX = Boolean.parseBoolean(resourceNames[0]);
                this.drawInCenterY = Boolean.parseBoolean(resourceNames[1]);
                this.candyBarOffsetX = Utils.parseInt(resourceNames[2]);
                this.candyBarOffsetY = Utils.parseInt(resourceNames[3]);
                this.candyBarWidth = Utils.parseInt(resourceNames[4]);
                this.candyBarHeight = Utils.parseInt(resourceNames[5]);
                this.anInt3569 = Utils.parseInt(resourceNames[6]);
                this.anInt3590 = Utils.parseInt(resourceNames[7]);
                this.anInt3591 = Utils.parseInt(resourceNames[8]);
                this.anInt3592 = Utils.parseInt(resourceNames[9]);
                String fontName = resourceNames[10];
                int size = Utils.parseInt(resourceNames[11]);
                int color = Utils.parse_int(resourceNames[12].substring(2));
                this.font = new Font(fontName, Font.PLAIN, size);
                this.fontMetrics = GameShell.gameCanvas.getFontMetrics(this.font);
                this.color = new Color(color);
                MediaTracker mediatracker = new MediaTracker(GameShell.gameCanvas);
                this.candyBarMiddle = getImageFromResource(resourceNames[13]);
                mediatracker.addImage(this.candyBarMiddle, 0);
                this.candyBarBack = getImageFromResource(resourceNames[14]);
                mediatracker.addImage(this.candyBarBack, 1);
                this.candyBarOutlineLeft = getImageFromResource(resourceNames[15]);
                mediatracker.addImage(this.candyBarOutlineLeft, 2);
                this.candyBarOutlineRight = getImageFromResource(resourceNames[16]);
                mediatracker.addImage(this.candyBarOutlineRight, 3);
                this.candyBarOutlineTop = getImageFromResource(resourceNames[17]);
                mediatracker.addImage(this.candyBarOutlineTop, 4);
                this.candyBarOutlineBottom = getImageFromResource(resourceNames[18]);
                mediatracker.addImage(this.candyBarOutlineBottom, 5);
                this.loadbarBodyLeft = getImageFromResource(resourceNames[19]);
                mediatracker.addImage(this.loadbarBodyLeft, 6);
                this.loadbarBodyRight = getImageFromResource(resourceNames[20]);
                mediatracker.addImage(this.loadbarBodyRight, 7);
                this.loadbarBodyFill = getImageFromResource(resourceNames[21]);
                mediatracker.addImage(this.loadbarBodyFill, 8);
                mediatracker.waitForAll();
                this.candyBarElementCount = Utils.parseInt(resourceNames[22]);
            } catch (Exception exception) {
                this.resourcesUnavailable = true;
            }
        }
        if (this.resourcesUnavailable) {
            drawPlainLoadingBar();
        } else {
            Graphics graphics = GameShell.gameCanvas.getGraphics();
            if (graphics != null) {
                try {
                    int loadPercentage = LoadingStage.plainLoginScreenRenderer.getLoadingPercentage();
                    String loadText = LoadingStage.plainLoginScreenRenderer.getLoadingText();
                    if (LegacyAssetLoadingScreen.canvasImage == null) {
                        LegacyAssetLoadingScreen.canvasImage = GameShell.gameCanvas.createImage(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379);
                    }
                    Graphics canvasImgGraphics = LegacyAssetLoadingScreen.canvasImage.getGraphics();
                    canvasImgGraphics.setColor(Color.BLACK);
                    canvasImgGraphics.fillRect(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379);
                    int loadbarLeftWidth = this.loadbarBodyLeft.getWidth(null);
                    int loadbarRightWidth = this.loadbarBodyRight.getWidth(null);
                    int loadbarFillWidth = this.loadbarBodyFill.getWidth(null);
                    int loadbarLeftHeight = this.loadbarBodyLeft.getHeight(null);
                    int loadbarRightHeight = this.loadbarBodyRight.getHeight(null);
                    int loadbarFillHeight = this.loadbarBodyFill.getHeight(null);
                    canvasImgGraphics.drawImage(this.loadbarBodyLeft, getDrawX(loadbarLeftWidth) + this.anInt3569 - this.anInt3591 / 2, getDrawY(loadbarLeftHeight) + this.anInt3590, null);
                    int i_13 = this.anInt3569 - this.anInt3591 / 2 + loadbarLeftWidth;
                    int i_14 = this.anInt3591 / 2 + this.anInt3569;
                    for (int i_15 = i_13; i_15 <= i_14; i_15 += loadbarFillWidth) {
                        canvasImgGraphics.drawImage(this.loadbarBodyFill, getDrawX(loadbarLeftWidth) + this.anInt3569 + i_15, getDrawY(loadbarFillHeight) + this.anInt3590, null);
                    }
                    canvasImgGraphics.drawImage(this.loadbarBodyRight, getDrawX(loadbarRightWidth) + this.anInt3569 + this.anInt3591 / 2, getDrawY(loadbarRightHeight) + this.anInt3590, null);
                    int candyLeftWidth = this.candyBarOutlineLeft.getWidth(null);
                    int candyLeftHeight = this.candyBarOutlineLeft.getHeight(null);
                    int candyRightWidth = this.candyBarOutlineRight.getWidth(null);
                    int candyRightHeight = this.candyBarOutlineRight.getHeight(null);
                    int candyBottomHeight = this.candyBarOutlineBottom.getHeight(null);
                    int candyTopWidth = this.candyBarOutlineTop.getWidth(null);
                    int candyTopHeight = this.candyBarOutlineTop.getHeight(null);
                    int candyBottomWidth = this.candyBarOutlineBottom.getWidth(null);
                    int candyMiddleWidth = this.candyBarMiddle.getWidth(null);
                    int candyBackWidth = this.candyBarBack.getWidth(null);
                    int candyBarX = getDrawX(this.candyBarWidth) + this.candyBarOffsetX;
                    int candyBarY = getDrawY(this.candyBarHeight) + this.candyBarOffsetY;
                    canvasImgGraphics.drawImage(this.candyBarOutlineLeft, candyBarX, candyBarY + (this.candyBarHeight - candyLeftHeight) / 2, null);
                    canvasImgGraphics.drawImage(this.candyBarOutlineRight, candyBarX + this.candyBarWidth - candyRightWidth, candyBarY + (this.candyBarHeight - candyRightHeight) / 2, null);
                    if (this.candyBarImage == null) {
                        this.candyBarImage = GameShell.gameCanvas.createImage(this.candyBarWidth - candyLeftWidth - candyRightWidth, this.candyBarHeight);
                    }
                    Graphics candyGraphics = this.candyBarImage.getGraphics();
                    for (int x = 0; x < this.candyBarWidth - candyLeftWidth - candyRightWidth; x += candyTopWidth) {
                        candyGraphics.drawImage(this.candyBarOutlineTop, x, 0, null);
                    }
                    for (int x = 0; x < this.candyBarWidth - candyLeftWidth - candyRightWidth; x += candyBottomWidth) {
                        candyGraphics.drawImage(this.candyBarOutlineBottom, x, this.candyBarHeight - candyBottomHeight, null);
                    }
                    int completionWidth = loadPercentage * (this.candyBarWidth - candyLeftWidth - candyRightWidth) / 100;
                    
                    if (completionWidth > 0) {
                        Image image29 = GameShell.gameCanvas.createImage(completionWidth, this.candyBarHeight - candyTopHeight - candyBottomHeight);
                        int i_30 = image29.getWidth(null);
                        Graphics graphics31 = image29.getGraphics();
                        int i_32 = this.candyBarElementCount * LegacyAssetLoadingScreen.method8740() / 10 % candyMiddleWidth;
                        for (int x = i_32 - candyMiddleWidth; x < i_30; x += candyMiddleWidth) {
                            graphics31.drawImage(this.candyBarMiddle, x, 0, null);
                        }
                        candyGraphics.drawImage(image29, 0, candyTopHeight, null);
                    }
                    int i_38 = completionWidth;
                    completionWidth = this.candyBarWidth - candyLeftWidth - candyRightWidth - completionWidth;
                    if (completionWidth > 0) {
                        Image completionImage = GameShell.gameCanvas.createImage(completionWidth, this.candyBarHeight - candyTopHeight - candyBottomHeight);
                        int completionImageWidth = completionImage.getWidth(null);
                        Graphics completionGraphics = completionImage.getGraphics();
                        for (int x = 0; x < completionImageWidth; x += candyBackWidth) {
                            completionGraphics.drawImage(this.candyBarBack, x, 0, null);
                        }
                        candyGraphics.drawImage(completionImage, i_38, candyTopHeight, null);
                    }
                    canvasImgGraphics.drawImage(this.candyBarImage, candyBarX + candyLeftWidth, candyBarY, null);
                    canvasImgGraphics.setFont(this.font);
                    canvasImgGraphics.setColor(this.color);
                    canvasImgGraphics.drawString(loadText, candyBarX + (this.candyBarWidth - this.fontMetrics.stringWidth(loadText)) / 2, this.candyBarHeight / 2 + candyBarY + this.anInt3592 + 4);
                    graphics.drawImage(LegacyAssetLoadingScreen.canvasImage, 0, 0, null);
                } catch (Exception exception43) {
                    this.resourcesUnavailable = true;
                }
            } else {
                GameShell.gameCanvas.repaint();
            }
        }
    }
    
    /**
     * Draws the loading bar when the login screen image resources are unavailable
     */
    public void drawPlainLoadingBar() {
        LegacyAssetLoadingScreen.drawPlainLoadingBar(LoadingStage.plainLoginScreenRenderer.getLoadingPercentage(), LoadingStage.plainLoginScreenRenderer.getLoadingText(), client.DEBUG_LOAD_COLOR[client.DEBUG_DISPLAY_COLOR_INDEX], client.DEBUG_BG_COLOR[client.DEBUG_DISPLAY_COLOR_INDEX], client.aColorArray7200[client.DEBUG_DISPLAY_COLOR_INDEX]);
    }

    public static int method8740() {
	    return LoadingStage.plainLoginScreenRenderer.getCycle();
	}

	public static void cleanupPlainLoadingbarResources() {
	    canvasImage = null;
	    FontManager.helvetaBold13Font = null;
	}

	/**
     * Draws the loading bar when the login screen image resources are unavailable
     */
	public static void drawPlainLoadingBar(int loadingPercentage, String rectText, Color innerRectColor, Color rectBackgroundColor, Color textColor) {
	    try {
	        Graphics canvasGraphics = GameShell.gameCanvas.getGraphics();
	        if (FontManager.helvetaBold13Font == null) {
	            FontManager.helvetaBold13Font = new Font("Helvetica", Font.BOLD, 13);
	        }
	
	        if (innerRectColor == null) {
	            innerRectColor = new Color(140, 17, 17);
	        }
	
	        if (rectBackgroundColor == null) {
	            rectBackgroundColor = new Color(140, 17, 17);
	        }
	
	        if (textColor == null) {
	            textColor = new Color(255, 255, 255);
	        }
	
	        try {
	            if (canvasImage == null) {
	                canvasImage = GameShell.gameCanvas.createImage(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379);
	            }
	
	            Graphics imageGraphics = canvasImage.getGraphics();
	            imageGraphics.setColor(Color.black);
	            imageGraphics.fillRect(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379);
	            int x = GameShell.screenCanvasWidth / 2 - 152;
	            int y = GameShell.screenCanvasHeight * -969250379 / 2 - 18;
	            imageGraphics.setColor(rectBackgroundColor);
	            imageGraphics.drawRect(x, y, 303, 33);
	            imageGraphics.setColor(innerRectColor);
	            imageGraphics.fillRect(x + 2, y + 2, loadingPercentage * 3, 30);
	            imageGraphics.setColor(Color.black);
	            imageGraphics.drawRect(x + 1, y + 1, 301, 31);
	            imageGraphics.fillRect(x + loadingPercentage * 3 + 2, y + 2, 300 - loadingPercentage * 3, 30);
	            imageGraphics.setFont(FontManager.helvetaBold13Font);
	            imageGraphics.setColor(textColor);
	            imageGraphics.drawString(rectText, x + (304 - rectText.length() * 6) / 2, y + 22);
	            if (GameShell.translatedLoadingText != null) {
	                imageGraphics.setFont(FontManager.helvetaBold13Font);
	                imageGraphics.setColor(textColor);
	                imageGraphics.drawString(GameShell.translatedLoadingText, GameShell.screenCanvasWidth / 2 - GameShell.translatedLoadingText.length() * 6 / 2, GameShell.screenCanvasHeight * -969250379 / 2 - 26);
	            }
	
	            canvasGraphics.drawImage(canvasImage, 0, 0, null);
	        } catch (Exception exception10) {
	            canvasGraphics.setColor(Color.black);
	            canvasGraphics.fillRect(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379);
	            int x = GameShell.screenCanvasWidth / 2 - 152;
	            int y = GameShell.screenCanvasHeight * -969250379 / 2 - 18;
	            canvasGraphics.setColor(rectBackgroundColor);
	            canvasGraphics.drawRect(x, y, 303, 33);
	            canvasGraphics.setColor(innerRectColor);
	            canvasGraphics.fillRect(x + 2, y + 2, loadingPercentage * 3, 30);
	            canvasGraphics.setColor(Color.black);
	            canvasGraphics.drawRect(x + 1, y + 1, 301, 31);
	            canvasGraphics.fillRect(x + loadingPercentage * 3 + 2, y + 2, 300 - loadingPercentage * 3, 30);
	            canvasGraphics.setFont(FontManager.helvetaBold13Font);
	            canvasGraphics.setColor(textColor);
	            if (GameShell.translatedLoadingText != null) {
	                canvasGraphics.setFont(FontManager.helvetaBold13Font);
	                canvasGraphics.setColor(textColor);
	                canvasGraphics.drawString(GameShell.translatedLoadingText, GameShell.screenCanvasWidth / 2 - GameShell.translatedLoadingText.length() * 6 / 2, GameShell.screenCanvasHeight * -969250379 / 2 - 26);
	            }
	
	            canvasGraphics.drawString(rectText, x + (304 - rectText.length() * 6) / 2, y + 22);
	        }
	    } catch (Exception exception) {
	        GameShell.gameCanvas.repaint();
	    }	
	}
}