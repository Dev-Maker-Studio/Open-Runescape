package com.jagex.game.runetek5.graphics.ifcomponent;

import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;

/**
 * Official name = PickerHelper?
 * @author Steven
 *
 */
public class ColorPicker {

	public static void drawColorPicker(AbstractToolkit toolkit, int x, int y, Component component) {
	    byte lightness = 63;
	    byte saturation = 7;
	    for (int hue = 63; hue >= 0; --hue) {
	        int hsl = (hue & 0x3f) << 10 | (saturation & 0x7) << 7 | lightness & 0x7f;
	        ColorUtil.initializeTables(false, true);
	        int rgb = ColorUtil.hslToRgb[hsl];
	        ColorUtil.resetTables(false, true);
	        toolkit.drawFilledRectangle(x, ((63 - hue) * component.height >> 6) + y, component.width, (component.height >> 6) + 1, rgb, 0);
	    }
	}
}
