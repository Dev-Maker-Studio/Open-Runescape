package com.jagex.game.runetek5.client.input.mouse;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CursorManager {
    public final Robot positionManager = new Robot();
	public static int defaultAttackCursor;
	public static int defaultCursorId;
	public static int activeCursorId;
	public static CursorManager instance;

    public CursorManager() throws Exception {
    	// NOOP
    }

    public void moveToPosition(int x, int y) {
        this.positionManager.mouseMove(x, y);
    }

    public void setCustomCursor(Component component, int[] cursorPixelsRgb, int width, int height, Point hotspotPoint) {
        if (cursorPixelsRgb != null) {
            BufferedImage bufferedImage = new BufferedImage(width, height, 2);
            bufferedImage.setRGB(0, 0, width, height, cursorPixelsRgb, 0, width);
            component.setCursor(component.getToolkit().createCustomCursor(bufferedImage, hotspotPoint, null));
        } else {
            component.setCursor(null);
        }
    }
}
