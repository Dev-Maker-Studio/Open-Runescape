package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.client.JSObjectManager;
import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.DebugDisplay;

public class WorldMapOverview {

	/**
	 * Draws a small map containing the entire world,
	 * used to allow user to reposition position
	 * on worldmap
	 */
	public static void drawWorldmapOverview(AbstractToolkit toolkit, int x, int y, int width, int height) {
	    toolkit.setClipping(x, y, width + x, y + height);
	    toolkit.clearPixels(x, y, width, height, -16777216);
	    if (ComponentMinimap.mapAssetLoadingPercentage >= 100) {
	        float f_6 = (float) WorldMapSub1.mapSizeY / WorldMapSub1.mapSizeX;
	        int Width = width;
	        int Height = height;
	        if (f_6 < 1.0F) {
	            Height = (int) (f_6 * width);
	        } else {
	            Width = (int) (height / f_6);
	        }
	        x += (width - Width) / 2;
	        y += (height - Height) / 2;
	        if (ComponentMinimap.aNativeSprite5883 == null || ComponentMinimap.aNativeSprite5883.getWidth() != width || ComponentMinimap.aNativeSprite5883.getHeight() != height) {
	            WorldMapSub1.method5139(WorldMapSub1.mapSquareAbsX, WorldMapSub1.mapSquareAbsY + WorldMapSub1.mapSizeY, WorldMapSub1.mapSizeX + WorldMapSub1.mapSquareAbsX, WorldMapSub1.mapSquareAbsY, x, y, Width + x, Height + y);
	            WorldMapSub1.renderMap(toolkit);
	            ComponentMinimap.aNativeSprite5883 = toolkit.createNativeSprite(x, y, Width, Height, false);
	        }
	        ComponentMinimap.aNativeSprite5883.draw(x, y);
	        int backgroundWidth = Width * StaticMethods.anInt5760 / WorldMapSub1.mapSizeX;
	        int backgroundHeight = Height * ComponentMinimap.anInt4097 / WorldMapSub1.mapSizeY;
	        int backgroundX = Width * DebugDisplay.anInt3991 / WorldMapSub1.mapSizeX + x;
	        int backgroundY = Height + y - Height * WorldMapSub1.anInt5765 / WorldMapSub1.mapSizeY - backgroundHeight;
	        int backgroundColor = -1996554240;
	        if (Game.STELLAR_DAWN == client.CURRENT_GAME) {
	            backgroundColor = -1996488705;
	        }
	        toolkit.drawFilledRectangle(backgroundX, backgroundY, backgroundWidth, backgroundHeight, backgroundColor, 1);
	        toolkit.drawRectangleOutline(backgroundX, backgroundY, backgroundWidth, backgroundHeight, backgroundColor, 0);
	        if (JSObjectManager.anInt4185 > 0) {
	            int randomizedColor;
	            if (ServerEnvironment.anInt9282 > 50) {
	                randomizedColor = 500 - ServerEnvironment.anInt9282 * 5;
	            } else {
	                randomizedColor = ServerEnvironment.anInt9282 * 5;
	            }
	            for (WorldMapCategory mapCategory = (WorldMapCategory) WorldMapSub1.worldMapCategories.getFirst(); mapCategory != null; mapCategory = (WorldMapCategory) WorldMapSub1.worldMapCategories.next()) {
	                MECType mectype = WorldMapSub1.MECTYPE_LIST.list(mapCategory.mecId);
	                if (WorldMapSub1.shouldDraw(mectype)) {
	                    if (mapCategory.mecId == WorldMapSub1.someMecId) {
	                        int mecStartX = Width * mapCategory.absX / WorldMapSub1.mapSizeX + x;
	                        int mecStartY = Height * (WorldMapSub1.mapSizeY - mapCategory.absY) / WorldMapSub1.mapSizeY + y;
	                        toolkit.clearPixels(mecStartX - 2, mecStartY - 2, 4, 4, randomizedColor << 24 | 0xffff00);
	                    } else if (WorldMapSub1.anInt8026 != -1 && mectype.anInt2718 == WorldMapSub1.anInt8026) {
	                    	int mecStartX = Width * mapCategory.absX / WorldMapSub1.mapSizeX + x;
	                    	int mecStartY = Height * (WorldMapSub1.mapSizeY - mapCategory.absY) / WorldMapSub1.mapSizeY + y;
	                        toolkit.clearPixels(mecStartX - 2, mecStartY - 2, 4, 4, randomizedColor << 24 | 0xffff00);
	                    }
	                }
	            }
	        }
	    }
	}

}
