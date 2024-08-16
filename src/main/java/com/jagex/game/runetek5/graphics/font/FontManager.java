package com.jagex.game.runetek5.graphics.font;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

import java.awt.*;

public class FontManager {

    public static FontMetrics messageboxFont;
    public static AbstractFontRenderer debugOverlayFont;
    public static int p12FullIndex;
    public static int p11FullIndex;
    public static int b12FullIndex;
	public static Font helvetaBold13Font;
	public static FontMetrics worldMapAreaFont;

    public static int[] getFontSpecifications() {
        return new int[]{p11FullIndex, p12FullIndex, b12FullIndex};
    }

    public static void loadFontArchiveIds(Js5ResourceProvider resourceProvider) {
        p11FullIndex = resourceProvider.getArchiveId("p11_full");
        p12FullIndex = resourceProvider.getArchiveId("p12_full");
        b12FullIndex = resourceProvider.getArchiveId("b12_full");
    }
	
	public static void initialize() {
	    Renderers.FONT_RENDERER = (AbstractFontRenderer) StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, p11FullIndex, true, true);
	    FontManager.worldMapAreaFont = StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, p11FullIndex);
	    debugOverlayFont = (AbstractFontRenderer) StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, p12FullIndex, true, true);
	    messageboxFont = StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, p12FullIndex);
	    MiniMenu.minimenuHeader = (AbstractFontRenderer) StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, b12FullIndex, true, true);
	    FontMetrics.aClass4143438 = StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, b12FullIndex);
	}
}
