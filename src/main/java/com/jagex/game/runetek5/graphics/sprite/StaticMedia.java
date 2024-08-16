package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class StaticMedia {

	/**
	 * Sprite archive ids
	 */
	public static int headiconPkGroupid;
	public static int headiconsPrayerGroupid;
	public static int hintHeadiconsGroupid;
	public static int hintMapmarkersGroupid;
	public static int mapflagGroupid;
	public static int crossGroupid;
	public static int mapdotsGroupid;
	public static int nameIconsGroupid;
	public static int floorShadowsGroupid;
	public static int compassGroupid;
	public static int otherLevelGroupid;
	public static int hintMapedgeGroupid;
	
	/**
	 * Loaded sprites
	 */
	public static NativeSprite[] headiconPkSprites;
	public static NativeSprite[] headiconPrayerSprites;
	public static NativeSprite[] hintHeadiconSprites;
	public static NativeSprite[] hintMapmarkerSprites;
	public static NativeSprite[] hintMapedgeSprites;
	public static NativeSprite otherLevelSprite;
	public static NativeSprite[] nameIconSprites;
	public static NativeSprite[] mapflagSprites;
	public static NativeSprite compassSprite;
	public static NativeSprite[] mapdotSprites;
	public static NativeSprite[] crossSprites;
	
	
	public static NativeSprite[] chatIcons;
	public static int totalChatIcons;
	public static int[] chatScaleHeights;
	public static FontMetrics aClass4142200;
	public static NativeSprite aNativeSprite5576;

	public static void loadGroupIds(Js5ResourceProvider spriteResource) {
	    headiconPkGroupid = spriteResource.getArchiveId("headicons_pk");
	    headiconsPrayerGroupid = spriteResource.getArchiveId("headicons_prayer");
	    hintHeadiconsGroupid = spriteResource.getArchiveId("hint_headicons");
	    hintMapmarkersGroupid = spriteResource.getArchiveId("hint_mapmarkers");
	    mapflagGroupid = spriteResource.getArchiveId("mapflag");
	    crossGroupid = spriteResource.getArchiveId("cross");
	    mapdotsGroupid = spriteResource.getArchiveId("mapdots");
	    nameIconsGroupid = spriteResource.getArchiveId("name_icons");
	    floorShadowsGroupid = spriteResource.getArchiveId("floorshadows");
	    compassGroupid = spriteResource.getArchiveId("compass");
	    otherLevelGroupid = spriteResource.getArchiveId("otherlevel");
	    hintMapedgeGroupid = spriteResource.getArchiveId("hint_mapedge");
	}

	public static void loadSprites(AbstractToolkit toolkit, Js5ResourceProvider spriteResource) {
	    SpriteType[] spriteTypes = SpriteType.decodeGroup(spriteResource, headiconPkGroupid);
	    headiconPkSprites = new NativeSprite[spriteTypes.length];
	    for (int index = 0; index < spriteTypes.length; index++) {
	        headiconPkSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, headiconsPrayerGroupid);
	    headiconPrayerSprites = new NativeSprite[spriteTypes.length];
	    for (int index = 0; index < spriteTypes.length; index++) {
	        headiconPrayerSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, hintHeadiconsGroupid);
	    hintHeadiconSprites = new NativeSprite[spriteTypes.length];
	    for (int index = 0; index < spriteTypes.length; index++) {
	        hintHeadiconSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, hintMapmarkersGroupid);
	    hintMapmarkerSprites = new NativeSprite[spriteTypes.length];
	    for (int index = 0; index < spriteTypes.length; index++) {
	        hintMapmarkerSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, mapflagGroupid);
	    mapflagSprites = new NativeSprite[spriteTypes.length];
	    byte colorOffset = 25;
	    for (int index = 0; index < spriteTypes.length; index++) {
	        spriteTypes[index].offsetRgbColor(-colorOffset + (int) (Math.random() * colorOffset * 2.0D), -colorOffset + (int) (Math.random() * colorOffset * 2.0D), -colorOffset + (int) (Math.random() * colorOffset * 2.0D));
	        mapflagSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, crossGroupid);
	    crossSprites = new NativeSprite[spriteTypes.length];
	    for (int index = 0; index < spriteTypes.length; index++) {
	        crossSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, mapdotsGroupid);
	    mapdotSprites = new NativeSprite[spriteTypes.length];
	    colorOffset = 12;
	    for (int index = 0; index < spriteTypes.length; index++) {
	        spriteTypes[index].offsetRgbColor(-colorOffset + (int) (Math.random() * colorOffset * 2.0D), -colorOffset + (int) (Math.random() * colorOffset * 2.0D), -colorOffset + (int) (Math.random() * colorOffset * 2.0D));
	        mapdotSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, nameIconsGroupid);
	    nameIconSprites = new NativeSprite[spriteTypes.length];
	    colorOffset = 12;
	    for (int index = 0; index < spriteTypes.length; index++) {
	        spriteTypes[index].offsetRgbColor(-colorOffset + (int) (Math.random() * colorOffset * 2.0D), -colorOffset + (int) (Math.random() * colorOffset * 2.0D), -colorOffset + (int) (Math.random() * colorOffset * 2.0D));
	        nameIconSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	    
	    compassSprite = toolkit.generateSprite(SpriteType.listSingle(spriteResource, compassGroupid, 0), true);
	    otherLevelSprite = toolkit.generateSprite(SpriteType.listSingle(spriteResource, otherLevelGroupid, 0), true);
	    
	    spriteTypes = SpriteType.decodeGroup(spriteResource, hintMapedgeGroupid);
	    hintMapedgeSprites = new NativeSprite[spriteTypes.length];
	    for (int index = 0; index < spriteTypes.length; index++) {
	        hintMapedgeSprites[index] = toolkit.generateSprite(spriteTypes[index], true);
	    }
	}

	public static int fullyCached(Js5ResourceProvider spritesResource) {
	    int fullyCached = 0;
	    if (spritesResource.isFileCached(headiconPkGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(headiconsPrayerGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(hintHeadiconsGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(hintMapmarkersGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(mapflagGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(crossGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(mapdotsGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(nameIconsGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(floorShadowsGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(compassGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(otherLevelGroupid)) {
	        fullyCached++;
	    }
	    if (spritesResource.isFileCached(hintMapedgeGroupid)) {
	        fullyCached++;
	    }
	    return fullyCached;
	}

	public static void initializeChatIcons(NativeSprite[] nativeSprites) {
	    totalChatIcons = nativeSprites.length;
	    chatIcons = new NativeSprite[totalChatIcons + 10];
	    chatScaleHeights = new int[totalChatIcons + 10];
	    System.arraycopy(nativeSprites, 0, chatIcons, 0, totalChatIcons);
	    for (int index = 0; index < totalChatIcons; index++) {
	        chatScaleHeights[index] = chatIcons[index].getSpriteHeight();
	    }
	    for (int index = totalChatIcons; index < chatIcons.length; index++) {
	        chatScaleHeights[index] = 12;
	    }
	}

	public static void reset() {
	    Renderers.FONT_RENDERER = null;
	    FontManager.debugOverlayFont = null;
	    MiniMenu.minimenuHeader = null;
	    headiconPkSprites = null;
	    headiconPrayerSprites = null;
	    hintHeadiconSprites = null;
	    hintMapmarkerSprites = null;
	    mapflagSprites = null;
	    crossSprites = null;
	    mapdotSprites = null;
	    nameIconSprites = null;
	    compassSprite = null;
	    otherLevelSprite = null;
	    hintMapedgeSprites = null;
	}

	public static void resetChatMedia() {
	    StaticMedia.aNativeSprite647 = null;
	    StaticMethods.aNativeSprite612 = null;
	    aNativeSprite5576 = null;
	    StaticMedia.aNativeSprite5339 = null;
	    StaticMedia.aNativeSprite3452 = null;
	    StaticMethods.aNativeSprite4110 = null;
	    StaticMethods.aNativeSprite1571 = null;
	    StaticMethods.aNativeSprite9315 = null;
	    FontMetrics.aFontRenderer2348 = null;
	    StaticMedia.aClass4142200 = null;
	}

	public static NativeSprite aNativeSprite3452;
	public static NativeSprite aNativeSprite5339;

	public static void drawClickCross() {
	    int x = client.crossX;
	    int y = client.crossY;
	    NativeSprite sprite;
	    if (client.crossType == 1) {
	        sprite = crossSprites[client.crossSpritePtr / 100];
	        sprite.draw(x - 8, y - 8);
	    }
	    if (client.crossType == 2) {
	        sprite = crossSprites[client.crossSpritePtr / 100 + 4];
	        sprite.draw(x - 8, y - 8);
	    }
	}

	public static void resetChatMediaAndIcons() {
	    resetChatMedia();
	    chatIcons = null;
	}

	public static NativeSprite aNativeSprite647;
}
