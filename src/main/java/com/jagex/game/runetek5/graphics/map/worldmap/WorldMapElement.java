package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.graphics.font.HelveticaFont;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.scene.MapSize;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class WorldMapElement extends CacheableNode {

    public int anInt9542 = -1;
    public int anInt9539 = 12800;
    public int anInt9540;
    public int anInt9541 = 12800;
    public int anInt9535;
    public final int anInt9536;
    public final String filenamePrefix;
    public final String aString9534;
    public final int bitpackedPlacement;
    public int color = -1;
    public boolean shouldRender = true;
    public final NodeCollection worldMapPlacements;

    public WorldMapElement(int i_1, String string_2, String string_3, int i_4, int i_5, boolean bool6, int i_7, int i_8) {
        this.anInt9536 = i_1;
        this.filenamePrefix = string_2;
        this.aString9534 = string_3;
        this.bitpackedPlacement = i_4;
        this.color = i_5;
        this.shouldRender = bool6;
        this.anInt9542 = i_7;
        if (this.anInt9542 == 255) {
            this.anInt9542 = 0;
        }

        MapSize.get(i_8);
        this.worldMapPlacements = new NodeCollection();
    }

    public static WorldMapElement list(Js5ResourceProvider resourceProvider, int arhiveId, int fileId) {
        Packet buffer = new Packet(resourceProvider.getFile(arhiveId, fileId));
        WorldMapElement def = new WorldMapElement(fileId, buffer.readString(), buffer.readString(), buffer.readInt(), buffer.readInt(), buffer.readUByte() == 1, buffer.readUByte(), buffer.readUByte());
        int count = buffer.readUByte();
        for (int index = 0; index < count; index++) {
            def.worldMapPlacements.addFirst(new WorldMapPlacement(buffer.readUByte(), buffer.readUShort(), buffer.readUShort(), buffer.readUShort(), buffer.readUShort(), buffer.readUShort(), buffer.readUShort(), buffer.readUShort(), buffer.readUShort()));
        }
        def.method14779();
        return def;
    }

    public static void renderWorldmapAreaText(AbstractToolkit toolkit, WorldMapCategory category, MECType mectype, int lineCount, int x, int y, int i_6, HelveticaFont font) {
        int rectX = x - i_6 / 2 - 5;
        int rectY = y + 2;
        if (mectype.backgroundColor != 0) {
            toolkit.clearPixels(rectX, rectY, i_6 + 10, y + lineCount * font.getLineHeight() - rectY + 1, mectype.backgroundColor);
        }
        if (mectype.outlineColor != 0) {
            toolkit.drawOpaqueRectangle(rectX, rectY, i_6 + 10, y + lineCount * font.getLineHeight() - rectY + 1, mectype.outlineColor);
        }
        int textColor = mectype.defaultTextColor;
        if (category.isMouseOver && mectype.mouseOverTextColor != -1) {
            textColor = mectype.mouseOverTextColor;
        }
        for (int line = 0; line < lineCount; line++) {
            String areaNameLine = WorldMapSub1.RENDERED_WORLDMAP_AREA_NAMELINES[line];
            if (line < lineCount - 1) {
            	// If not the last line of the area name strip the <br> tag
                areaNameLine = areaNameLine.substring(0, areaNameLine.length() - 4);
            }
            font.drawClippedText(toolkit, areaNameLine, x, y, textColor);
            y += font.getLineHeight();
        }
    }

    public boolean method14775(int i_1, int i_2, int[] position) {
        for (WorldMapPlacement class282Sub285 = (WorldMapPlacement) this.worldMapPlacements.getFirst(); class282Sub285 != null; class282Sub285 = (WorldMapPlacement) this.worldMapPlacements.next()) {
            if (class282Sub285.method12409(i_1, i_2)) {
                class282Sub285.method12410(i_1, i_2, position);
                return true;
            }
        }

        return false;
    }

    public boolean method14777(int x, int y, int[] cacheInto) {
        for (WorldMapPlacement class282Sub285 = (WorldMapPlacement) this.worldMapPlacements.getFirst(); class282Sub285 != null; class282Sub285 = (WorldMapPlacement) this.worldMapPlacements.next()) {
            if (class282Sub285.withinBounds(x, y)) {
                class282Sub285.cache(x, y, cacheInto);
                return true;
            }
        }

        return false;
    }

    public boolean method14778(int plane, int x, int y, int[] cacheInto) {
        for (WorldMapPlacement worldmapPlacement = (WorldMapPlacement) this.worldMapPlacements.getFirst(); worldmapPlacement != null; worldmapPlacement = (WorldMapPlacement) this.worldMapPlacements.next()) {
            if (worldmapPlacement.withinPlaneBounds(plane, x, y)) {
                worldmapPlacement.cache(x, y, cacheInto);
                return true;
            }
        }

        return false;
    }

    public void method14779() {
        this.anInt9539 = 12800;
        this.anInt9540 = 0;
        this.anInt9541 = 12800;
        this.anInt9535 = 0;

        for (WorldMapPlacement class282Sub282 = (WorldMapPlacement) this.worldMapPlacements.getFirst(); class282Sub282 != null; class282Sub282 = (WorldMapPlacement) this.worldMapPlacements.next()) {
            if (class282Sub282.anInt7701 < this.anInt9539) {
                this.anInt9539 = class282Sub282.anInt7701;
            }

            if (class282Sub282.anInt7704 > this.anInt9540) {
                this.anInt9540 = class282Sub282.anInt7704;
            }

            if (class282Sub282.anInt7703 < this.anInt9541) {
                this.anInt9541 = class282Sub282.anInt7703;
            }

            if (class282Sub282.anInt7705 > this.anInt9535) {
                this.anInt9535 = class282Sub282.anInt7705;
            }
        }

    }

    public boolean allElementsInBounds(int x, int y) {
        for (WorldMapPlacement placement = (WorldMapPlacement) this.worldMapPlacements.getFirst(); placement != null; placement = (WorldMapPlacement) this.worldMapPlacements.next()) {
            if (placement.withinBounds(x, y)) {
                return true;
            }
        }

        return false;
    }

}
