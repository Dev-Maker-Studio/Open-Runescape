package com.jagex.game.runetek5.graphics.map.worldmap;

import java.util.ArrayList;
import java.util.Iterator;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.JSObjectManager;
import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.flo.FloType;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluType;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.config.mapsprite.MapSpriteType;
import com.jagex.game.runetek5.config.mapsprite.MapSpriteTypeList;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.config.mec.MECTypeList;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.staticelements.StaticElements;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.DebugDisplay;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.vardomain.VarDomain;

public class WorldMap {

    public static float requestedMapZoom;
    public static float mapZoom;
    public static int mapSquareAbsX;
    public static int mapSquareAbsY;
    public static WorldMapElement staticElement;
    public static byte[][][] aByteArrayArrayArray3464;
    public static int anInt3469;
    public static StaticElements staticElements;
    public static int mapSizeX;
    public static int mapSizeY;
    public static int hueRandomisation = (int) (Math.random() * 11.0D) - 5;
    public static int lightnessRandomisation = (int) (Math.random() * 17.0D) - 8;
    public static final NodeCollection worldMapCategories = new NodeCollection();
    public static Js5ResourceProvider MAP_AREA_RESOURCE;
    public static ObjectTypeList LOCTYPE_LIST;
    public  static MECTypeList MECTYPE_LIST;
    public static MapSpriteTypeList MAP_SPRITE_TYPE_LIST;
    public static VarDomain PLAYER_VAR_PROVIDER;
    public static int anInt3485;
    public static int anInt3486;
    public static int anInt3480;
    public static int anInt3465;
    public static int anInt3489;
    public static int anInt3490;
    public static int anInt3476;
    public static int anInt3492;
    public static final IterableNodeMap WORLDMAP_DEFS = new IterableNodeMap(16);
    public static final int[] LOC_ON_TILE = new int[1];
    public static final byte[] SHAPE_AND_ROTATION_ON_TILE = new byte[1];
    public static FloTypeList FLOTYPE_LIST;
    public static FluTypeList FLUTYPE_LIST;
    public static byte[] underlayIds;
    public static byte[] overlayIds;
    public static byte[] aByteArray3487;
    public static Object[] tileDrawInfo;
    public static byte[] shapesAndRotations;
    public static ArrayList[][][] worldMapTiles;
    public static int[] overlayFloorColors;
    public static byte[] underlayRgbColors;
    public static short[] aShortArray3479;
	public static Component worldmapComponent;

    WorldMap() {
        throw new Error();
    }

    public static void initialize(Js5ResourceProvider areaResource, FloTypeList floTypeList, FluTypeList fluTypeList, ObjectTypeList locTypeList, MECTypeList mecTypeList, MapSpriteTypeList mapspriteTypeList, VarDomain varProvider) {
        MAP_AREA_RESOURCE = areaResource;
        FLOTYPE_LIST = floTypeList;
        FLUTYPE_LIST = fluTypeList;
        LOCTYPE_LIST = locTypeList;
        MECTYPE_LIST = mecTypeList;
        MAP_SPRITE_TYPE_LIST = mapspriteTypeList;
        PLAYER_VAR_PROVIDER = varProvider;
        WORLDMAP_DEFS.clear();
        int detailsArchiveId = MAP_AREA_RESOURCE.getArchiveId("details");
        int[] detailsFileIds = MAP_AREA_RESOURCE.getValidFileIds(detailsArchiveId);
        if (detailsFileIds != null) {
            for (int index = 0; index < detailsFileIds.length; index++) {
                WorldMapElement element = WorldMapElement.list(MAP_AREA_RESOURCE, detailsArchiveId, detailsFileIds[index]);
                WORLDMAP_DEFS.put(element, element.anInt9536);
            }
        }

        ColorUtil.initializeTables(true, false);
    }

    public static void initialize() {
        underlayIds = new byte[mapSizeX * mapSizeY];
        overlayIds = new byte[mapSizeX * mapSizeY];
        aByteArray3487 = new byte[mapSizeX * mapSizeY];
        tileDrawInfo = new Object[mapSizeX * mapSizeY];
        shapesAndRotations = new byte[mapSizeX * mapSizeY];
        worldMapTiles = new ArrayList[3][mapSizeX >> 6][mapSizeY >> 6];
        overlayFloorColors = new int[FLOTYPE_LIST.totalFileCount + 1];
    }

    public static Queue getRenderedElements(int x, int y) {
        Queue renderedElements = new Queue();
        for (WorldMapElement worldmapDef = (WorldMapElement) WORLDMAP_DEFS.first(); worldmapDef != null; worldmapDef = (WorldMapElement) WORLDMAP_DEFS.next()) {
            if (worldmapDef.shouldRender && worldmapDef.allElementsInBounds(x, y)) {
                renderedElements.insert(worldmapDef);
            }
        }
        return renderedElements;
    }

    public static void fetch(int key) {
        staticElement = (WorldMapElement) WORLDMAP_DEFS.get(key);
    }

    public static WorldMapElement poll(int key) {
        return (WorldMapElement) WORLDMAP_DEFS.get(key);
    }

    public static void renderMap(AbstractToolkit toolkit, int i_1, int i_2) {
        int i_5 = anInt3480 - anInt3485;
        int i_6 = anInt3486 - anInt3465;
        if (anInt3480 < mapSizeX) {
            i_5++;
        }

        if (anInt3486 < mapSizeY) {
            i_6++;
        }

        for (int i_7 = 0; i_7 < i_5; i_7++) {
        	int startX = (i_7 * i_1 >> 16) + anInt3489;
        	int endX = ((i_7 + 1) * i_1 >> 16) + anInt3489;
        	int width = endX - startX;
            if (width > 0) {
            	int i_11 = i_7 + anInt3485;
                if (i_11 >= 0 && i_11 < mapSizeX) {
                    for (int i_12 = 0; i_12 < i_6; i_12++) {
                    	int startY = anInt3492 - (i_2 * (i_12 + 1) >> 16);
                        int endY = anInt3492 - (i_12 * i_2 >> 16);
                        int height = endY - startY;
                        if (height > 0) {
                        	int i_16 = i_12 + anInt3465;
                            int tileIndex = i_11 + i_16 * mapSizeX;
                            int underlayColor = 0;
                            int overlayId = 0;
                            Object drawnTile = null;
                            if (i_16 >= 0 && i_16 < mapSizeY) {
                                underlayColor = (underlayRgbColors[tileIndex] & 0xff) << 16 | aShortArray3479[tileIndex] & 0xffff;
                                if (underlayColor != 0) {
                                    underlayColor |= ~0xffffff;
                                }

                                overlayId = overlayIds[tileIndex] & 0xff;
                                drawnTile = tileDrawInfo[tileIndex];
                            }

                            if (underlayColor == 0 && overlayId == 0 && drawnTile == null) {
                                if (staticElement.color != -1) {
                                    underlayColor = ~0xffffff | staticElement.color;
                                } else if ((i_7 + anInt3485 & 0x4) != (i_12 + anInt3486 & 0x4)) {
                                    underlayColor = -11840664;
                                } else {
                                    underlayColor = overlayFloorColors[FLOTYPE_LIST.lastLoadedOverlayId + 1];
                                }

                                if (underlayColor == 0) {
                                    underlayColor = ~0xffffff;
                                }

                                toolkit.drawFilledRectangle(startX, startY, width, height, underlayColor, 0);
                            } else if (drawnTile != null) {
                                if (drawnTile instanceof LocOnWorldMap worldMapTile) {
                                    renderTile(toolkit, startX, startY, width, height, underlayColor, overlayId,
                                    		aByteArray3487[tileIndex], worldMapTile.locIds, worldMapTile.shapeAndRotation, true);
                                } else {
                                    Integer locId = (Integer) drawnTile;
                                    LOC_ON_TILE[0] = locId.intValue();
                                    SHAPE_AND_ROTATION_ON_TILE[0] = shapesAndRotations[tileIndex];
                                    renderTile(toolkit, startX, startY, width, height, underlayColor, overlayId,
                                    		aByteArray3487[tileIndex], LOC_ON_TILE, SHAPE_AND_ROTATION_ON_TILE, true);
                                }
                            } else {
                                renderTile(toolkit, startX, startY, width, height, underlayColor, overlayId,
                                		aByteArray3487[tileIndex], null, null, true);
                            }
                        }
                    }
                } else {
                    for (int i_12 = 0; i_12 < i_6; i_12++) {
                    	int i_13 = anInt3492 - (i_2 * (i_12 + 1) >> 16);
                        int i_14 = anInt3492 - (i_12 * i_2 >> 16);
                        int i_15 = i_14 - i_13;
                        int overlayFloorColor;
                        if (staticElement.color != -1) {
                            overlayFloorColor = ~0xffffff | staticElement.color;
                        } else if ((i_7 + anInt3485 & 0x4) != (i_12 + anInt3486 & 0x4)) {
                            overlayFloorColor = -11840664;
                        } else {
                            overlayFloorColor = overlayFloorColors[FLOTYPE_LIST.lastLoadedOverlayId + 1];
                        }

                        if (overlayFloorColor == 0) {
                            overlayFloorColor = ~0xffffff;
                        }

                        toolkit.drawFilledRectangle(startX, i_13, width, i_15, overlayFloorColor, 0);
                    }
                }
            }
        }

        for (int i_7 = -16; i_7 < i_5 + 16; i_7++) {
        	int i_8 = (i_7 * i_1 >> 16) + anInt3489;
        	int i_9 = ((i_7 + 1) * i_1 >> 16) + anInt3489;
        	int i_10 = i_9 - i_8;
            if (i_10 > 0) {
            	int i_11 = i_7 + anInt3485;
                if (i_11 >= 0 && i_11 < mapSizeX) {
                    for (int i_12 = -16; i_12 < i_6 + 16; i_12++) {
                    	int i_13 = anInt3492 - (i_2 * (i_12 + 1) >> 16);
                        int i_14 = anInt3492 - (i_12 * i_2 >> 16);
                        int i_15 = i_14 - i_13;
                        if (i_15 > 0) {
                        	int i_16 = i_12 + anInt3465;
                            if (i_16 >= 0 && i_16 < mapSizeY) {
                                Object object26 = tileDrawInfo[i_11 + i_16 * mapSizeX];
                                if (object26 != null) {
                                    if (object26 instanceof LocOnWorldMap class282Sub4927) {
                                        renderObjectSprites(toolkit, i_8, i_13, i_10, i_15, class282Sub4927.locIds, class282Sub4927.shapeAndRotation);
                                    } else {
                                        Integer integer31 = (Integer) object26;
                                        LOC_ON_TILE[0] = integer31.intValue();
                                        SHAPE_AND_ROTATION_ON_TILE[0] = shapesAndRotations[i_11 + i_16 * mapSizeX];
                                        renderObjectSprites(toolkit, i_8, i_13, i_10, i_15, LOC_ON_TILE, SHAPE_AND_ROTATION_ON_TILE);
                                    }
                                } else {
                                    renderObjectSprites(toolkit, i_8, i_13, i_10, i_15, null, null);
                                }
                            }
                        }
                    }
                }
            }
        }

        int startX = anInt3485 >> 6;
        int startY = anInt3465 >> 6;
        if (startX < 0) {
            startX = 0;
        }

        if (startY < 0) {
            startY = 0;
        }

        int endX = anInt3480 >> 6;
        int endY = anInt3486 >> 6;
        if (endX >= worldMapTiles[0].length) {
            endX = worldMapTiles[0].length - 1;
        }

        if (endY >= worldMapTiles[0][0].length) {
            endY = worldMapTiles[0][0].length - 1;
        }

        for (int plane = 0; plane < 3; plane++) {
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                	ArrayList tiles = worldMapTiles[plane][x][y];
                    if (tiles != null) {
                    	int i_15 = ((mapSquareAbsX >> 6) + x) * 64;
                        int i_16 = ((mapSquareAbsY >> 6) + y) * 64;
                        Iterator tilesIt = tiles.iterator();

                        while (tilesIt.hasNext()) {
                        	TileOnWorldMap tile = (TileOnWorldMap) tilesIt.next();
                            int i_19 = i_15 + tile.mapsquarePosX - mapSquareAbsX - anInt3485;
                            int i_28 = i_16 + tile.mapsquarePosY - mapSquareAbsY - anInt3465;
                            int i_21 = (i_19 * i_1 >> 16) + anInt3489;
                            int i_22 = ((i_19 + 1) * i_1 >> 16) + anInt3489;
                            int i_23 = anInt3492 - (i_2 * (i_28 + 1) >> 16);
                            int i_24 = anInt3492 - (i_28 * i_2 >> 16);
                            renderTile(toolkit, i_21, i_23, i_22 - i_21, i_24 - i_23, tile.underlayIdOrColor, tile.overlayId & 0xff,
                            		tile.tileShapeAndRotation, tile.locIds, tile.shapeAndRotations, false);
                        }
                    }
                }
            }

            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                	ArrayList tiles = worldMapTiles[plane][x][y];
                    if (tiles != null) {
                    	int absX = ((mapSquareAbsX >> 6) + x) * 64;
                        int absY = ((mapSquareAbsY >> 6) + y) * 64;
                        Iterator tilesIt = tiles.iterator();

                        while (tilesIt.hasNext()) {
                        	TileOnWorldMap class26932 = (TileOnWorldMap) tilesIt.next();
                            int i_19 = absX + class26932.mapsquarePosX - mapSquareAbsX - anInt3485;
                            int i_28 = absY + class26932.mapsquarePosY - mapSquareAbsY - anInt3465;
                            int i_21 = (i_19 * i_1 >> 16) + anInt3489;
                            int i_22 = ((i_19 + 1) * i_1 >> 16) + anInt3489;
                            int i_23 = anInt3492 - (i_2 * (i_28 + 1) >> 16);
                            int i_24 = anInt3492 - (i_28 * i_2 >> 16);
                            renderObjectSprites(toolkit, i_21, i_23, i_22 - i_21, i_24 - i_23, class26932.locIds, class26932.shapeAndRotations);
                        }
                    }
                }
            }
        }

    }

    public static void calculateRunetekPalette(MaterialProvider materialProvider, int hueRandomisation, int lightnessRandomisation) {
        for (int overlayId = 0; overlayId < FLOTYPE_LIST.totalFileCount; overlayId++) {
            overlayFloorColors[overlayId + 1] = getOverlayFloorColor(materialProvider, overlayId, hueRandomisation, lightnessRandomisation);
        }
    }

    public static NodeCollection method5135() {
        int i_1 = anInt3480 - anInt3485;
        int i_2 = anInt3486 - anInt3465;
        int i_3 = (anInt3476 - anInt3489 << 16) / i_1;
        int i_4 = (anInt3492 - anInt3490 << 16) / i_2;
        return method5179(i_3, i_4);
    }

    public static void method5136() {
        for (int x = 0; x < mapSizeX; x++) {
            for (int y = 0; y < mapSizeY; y++) {
                Object drawInfo = tileDrawInfo[x + y * mapSizeX];
                if (drawInfo != null) {
                    if (drawInfo instanceof LocOnWorldMap mapLoc) {
                        for (int index = 0; index < mapLoc.locIds.length; index++) {
                            ObjectType locType = LOCTYPE_LIST.list(mapLoc.locIds[index]);
                            int mecId = locType.mapCategoryid;
                            if (locType.alternativeIds != null) {
                                locType = locType.getMultiLoc(PLAYER_VAR_PROVIDER);
                                if (locType != null) {
                                    mecId = locType.mapCategoryid;
                                }
                            }

                            if (mecId != -1) {
                                WorldMapCategory category = new WorldMapCategory(mecId);
                                category.absX = x;
                                category.absY = y;
                                worldMapCategories.addFirst(category);
                            }
                        }
                    } else {
                        Integer locId = (Integer) drawInfo;
                        ObjectType locType = LOCTYPE_LIST.list(locId.intValue());
                        int mecId = locType.mapCategoryid;
                        if (locType.alternativeIds != null) {
                            locType = locType.getMultiLoc(PLAYER_VAR_PROVIDER);
                            if (locType != null) {
                                mecId = locType.mapCategoryid;
                            }
                        }

                        if (mecId != -1) {
                            WorldMapCategory category = new WorldMapCategory(mecId);
                            category.absX = x;
                            category.absY = y;
                            worldMapCategories.addFirst(category);
                        }
                    }
                }
            }
        }

        for (int plane = 0; plane < 3; plane++) {
            for (int x = 0; x < worldMapTiles[0].length; x++) {
                label90:
                for (int y = 0; y < worldMapTiles[0][0].length; y++) {
                    ArrayList arraylist15 = worldMapTiles[plane][x][y];
                    if (arraylist15 != null) {
                        Iterator it = arraylist15.iterator();

                        while (true) {
                            TileOnWorldMap tile;
                            do {
                                if (!it.hasNext()) {
                                    continue label90;
                                }

                                tile = (TileOnWorldMap) it.next();
                            } while (tile.locIds == null);

                            for (int index = 0; index < tile.locIds.length; index++) {
                                ObjectType locType = LOCTYPE_LIST.list(tile.locIds[index]);
                                int mecId = locType.mapCategoryid;
                                if (locType.alternativeIds != null) {
                                    locType = locType.getMultiLoc(PLAYER_VAR_PROVIDER);
                                    if (locType != null) {
                                        mecId = locType.mapCategoryid;
                                    }
                                }

                                if (mecId != -1) {
                                    WorldMapCategory category = new WorldMapCategory(mecId);
                                    category.absX = ((mapSquareAbsX >> 6) + x) * 64 + tile.mapsquarePosX - mapSquareAbsX;
                                    category.absY = ((mapSquareAbsY >> 6) + y) * 64 + tile.mapsquarePosY - mapSquareAbsY;
                                    worldMapCategories.addFirst(category);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public static void reset() {
        underlayIds = null;
        underlayRgbColors = null;
        aShortArray3479 = null;
        overlayIds = null;
        aByteArray3487 = null;
        tileDrawInfo = null;
        shapesAndRotations = null;
        worldMapTiles = null;
        overlayFloorColors = null;
    }

    public static void method5139(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        anInt3485 = i_0 - mapSquareAbsX;
        anInt3486 = i_1 - mapSquareAbsY;
        anInt3480 = i_2 - mapSquareAbsX;
        anInt3465 = i_3 - mapSquareAbsY;
        anInt3489 = i_4;
        anInt3490 = i_5;
        anInt3476 = i_6;
        anInt3492 = i_7;
    }

    public static void renderTile(AbstractToolkit toolkit, int x, int y, int width, int height, int underlayColor, int overlayId, int tileShapeAndRotation, int[] locids, byte[] locShapesAndRotations, boolean clearPixels) {
        if (clearPixels || underlayColor != 0 || overlayId > 0) {
            if (overlayId == 0) {
                toolkit.drawFilledRectangle(x, y, width, height, underlayColor, 0);
            } else {
                int shape = tileShapeAndRotation & 0x3f;
                if (shape != 0 && width > 1 && height > 1) {
                    int clearType = clearPixels ? 0 : 1;
                    renderWorldmapTile(toolkit, aByteArrayArrayArray3464, anInt3469, x, y, underlayColor,
                    		overlayFloorColors[overlayId], width, height, shape, tileShapeAndRotation >> 6 & 0x3, clearType);
                } else {
                    int floorColor = overlayFloorColors[overlayId];
                    if (clearPixels || floorColor != 0) {
                        toolkit.drawFilledRectangle(x, y, width, height, floorColor, 0);
                    }
                }
            }
        }

        if (locids != null) {
            int endX;
            int endY;
            if (width == 1) {
                endX = x;
            } else {
                endX = width + x - 1;
            }

            if (height == 1) {
                endY = y;
            } else {
                endY = y + height - 1;
            }

            for (int index = 0; index < locids.length; index++) {
                int shape = locShapesAndRotations[index] & 0x3f;
                if (shape == ObjectShapes.WALL_STRAIGHT.shape || shape == ObjectShapes.WALL_WHOLE_CORNER.shape
                		|| shape == ObjectShapes.WALL_STRAIGHT_CORNER.shape || shape == ObjectShapes.WALL_INTERACT.shape) {
                    ObjectType loctype = LOCTYPE_LIST.list(locids[index]);
                    if (loctype.mapIconId == -1) {
                        int color = -3355444;
                        if (loctype.hasActions == 1) {
                            color = -3407872;
                        }

                        int rotation = locShapesAndRotations[index] >> 6 & 0x3;
                        if (shape == ObjectShapes.WALL_STRAIGHT.shape) {
                            if (rotation == 0) {
                                toolkit.drawTransparentVerticalLine(x, y, height, color, 0);
                            } else if (rotation == 1) {
                                toolkit.drawTransparentHorizontalLine(x, y, width, color, 0);
                            } else if (rotation == 2) {
                                toolkit.drawTransparentVerticalLine(endX, y, height, color, 0);
                            } else {
                                toolkit.drawTransparentHorizontalLine(x, endY, width, color, 0);
                            }
                        } else if (shape == ObjectShapes.WALL_WHOLE_CORNER.shape) {
                            if (rotation == 0) {
                                toolkit.drawTransparentVerticalLine(x, y, height, -1, 0);
                                toolkit.drawTransparentHorizontalLine(x, y, width, color, 0);
                            } else if (rotation == 1) {
                                toolkit.drawTransparentVerticalLine(endX, y, height, -1, 0);
                                toolkit.drawTransparentHorizontalLine(x, y, width, color, 0);
                            } else if (rotation == 2) {
                                toolkit.drawTransparentVerticalLine(endX, y, height, -1, 0);
                                toolkit.drawTransparentHorizontalLine(x, endY, width, color, 0);
                            } else {
                                toolkit.drawTransparentVerticalLine(x, y, height, -1, 0);
                                toolkit.drawTransparentHorizontalLine(x, endY, width, color, 0);
                            }
                        } else if (shape == ObjectShapes.WALL_STRAIGHT_CORNER.shape) {
                            if (rotation == 0) {
                                toolkit.drawTransparentHorizontalLine(x, y, 1, color, 0);
                            } else if (rotation == 1) {
                                toolkit.drawTransparentHorizontalLine(endX, y, 1, color, 0);
                            } else if (rotation == 2) {
                                toolkit.drawTransparentHorizontalLine(endX, endY, 1, color, 0);
                            } else {
                                toolkit.drawTransparentHorizontalLine(x, endY, 1, color, 0);
                            }
                        } else if (shape == ObjectShapes.WALL_INTERACT.shape) {
                            if (rotation != 0 && rotation != 2) {
                                for (int h = 0; h < height; h++) {
                                    toolkit.drawTransparentHorizontalLine(h + x, h + y, 1, color, 0);
                                }
                            } else {
                                for (int h = 0; h < height; h++) {
                                    toolkit.drawTransparentHorizontalLine(h + x, endY - h, 1, color, 0);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    public static void loadWorldMapCategories() {
        int[] placement = new int[3];
        for (int index = 0; index < staticElements.elementCount; index++) {
            boolean addToMap = staticElement.method14778(staticElements.bitpackedPlacement[index] >> 28 & 0x3, staticElements.bitpackedPlacement[index] >> 14 & 0x3fff, staticElements.bitpackedPlacement[index] & 0x3fff, placement);
            if (addToMap) {
                WorldMapCategory worldMapCategory = new WorldMapCategory(staticElements.mapCategoryIds[index]);
                worldMapCategory.absX = placement[1] - mapSquareAbsX;
                worldMapCategory.absY = placement[2] - mapSquareAbsY;
                worldMapCategories.addFirst(worldMapCategory);
            }
        }
    }

    public static void method5146(WorldMapCategory class282Sub361, int i_2, int i_3) {
        class282Sub361.anInt7990 = (i_2 * (class282Sub361.absX - anInt3485) >> 16) + anInt3489;
        class282Sub361.anInt7992 = anInt3492 - (i_3 * (class282Sub361.absY - anInt3465) >> 16);
    }

    public static void method5147(AbstractToolkit toolkit, WorldMapCategory worldMapCategory, MECType mectype) {
        if (mectype.offsets != null) {
            int[] positioning = new int[mectype.offsets.length];
            for (int i_4 = 0; i_4 < positioning.length / 2; i_4++) {
            	int i_5 = mectype.offsets[i_4 * 2] + worldMapCategory.absX;
            	int i_6 = worldMapCategory.absY + mectype.offsets[i_4 * 2 + 1];
                positioning[i_4 * 2] = anInt3489 + (anInt3476 - anInt3489) * (i_5 - anInt3485) / (anInt3480 - anInt3485);
                positioning[i_4 * 2 + 1] = anInt3492 - (anInt3492 - anInt3490) * (i_6 - anInt3465) / (anInt3486 - anInt3465);
            }

            WorldMapRasterManager.method2471(toolkit, positioning, mectype.lineColor);
            if (mectype.anInt2748 > 0) {
                for (int index = 0; index < positioning.length / 2 - 1; index++) {
                	int x = positioning[index * 2];
                	int y = positioning[index * 2 + 1];
                	int width = positioning[(index + 1) * 2];
                	int length = positioning[(index + 1) * 2 + 1];
                    if (width < x) {
                    	int i_9 = x;
                        int i_10 = y;
                        x = width;
                        y = length;
                        width = i_9;
                        length = i_10;
                    } else if (width == x && length < y) {
                    	int i_9 = y;
                        y = length;
                        length = i_9;
                    }

                    toolkit.drawLine(x, y, width, length, mectype.colors[mectype.colorPointers[index] & 0xff], mectype.anInt2748, mectype.anInt2749, mectype.anInt2756);
                }

                int i_4 = positioning[positioning.length - 2];
                int i_5 = positioning[positioning.length - 1];
                int i_6 = positioning[0];
                int i_7 = positioning[1];
                if (i_6 < i_4) {
                	int i_8 = i_4;
                	int i_9 = i_5;
                    i_4 = i_6;
                    i_5 = i_7;
                    i_6 = i_8;
                    i_7 = i_9;
                } else if (i_6 == i_4 && i_7 < i_5) {
                	int i_8 = i_5;
                    i_5 = i_7;
                    i_7 = i_8;
                }

                toolkit.drawLine(i_4, i_5, i_6, i_7, mectype.colors[mectype.colorPointers[mectype.colorPointers.length - 1] & 0xff], mectype.anInt2748, mectype.anInt2749, mectype.anInt2756);
            } else {
                for (int index = 0; index < positioning.length / 2 - 1; index++) {
                    toolkit.drawOpaqueLine(positioning[index * 2], positioning[index * 2 + 1], positioning[(index + 1) * 2], positioning[(index + 1) * 2 + 1], mectype.colors[mectype.colorPointers[index] & 0xff]);
                }
                toolkit.drawOpaqueLine(positioning[positioning.length - 2], positioning[positioning.length - 1], positioning[0], positioning[1], mectype.colors[mectype.colorPointers[mectype.colorPointers.length - 1] & 0xff]);
            }
        }
    }

    public static void prepareMapdata(Packet buffer, int i_2, int i_3, int x, int y, int[] ints_6, int[] ints_7) {
        int flags = buffer.readUByte();
        if ((flags & 0x1) == 0) {
            boolean bool9 = (flags & 0x2) == 0;
            int i_10 = flags >> 2 & 0x3f;
            if (i_10 != 62) {
                if (i_10 == 63) {
                    i_10 = buffer.readUByte();
                } else if (bool9) {
                    i_10 = ints_6[i_10];
                } else {
                    i_10 = ints_7[i_10];
                }

                if (bool9) {
                    underlayIds[y * mapSizeX + x] = (byte) i_10;
                    overlayIds[y * mapSizeX + x] = 0;
                } else {
                    overlayIds[y * mapSizeX + x] = (byte) i_10;
                    aByteArray3487[y * mapSizeX + x] = 0;
                    underlayIds[y * mapSizeX + x] = buffer.readByte();
                }
            }
        } else {
            int i_21 = (flags >> 1 & 0x3) + 1;
            boolean bool22 = (flags & 0x8) != 0;
            boolean hasLocs = (flags & 0x10) != 0;

            for (int tileType = 0; tileType < i_21; tileType++) {
                int underlay = buffer.readUByte();
                int overlay = 0;
                int tileRotationAndShape = 0;
                if (bool22) {
                    overlay = buffer.readUByte();
                    tileRotationAndShape = buffer.readUByte();
                }

                int locCount = 0;
                if (hasLocs) {
                    locCount = buffer.readUByte();
                }

                int[] objectIds;
                byte[] locRotationAndShape;
                if (tileType == 0) {
                    underlayIds[y * mapSizeX + x] = (byte) underlay;
                    overlayIds[y * mapSizeX + x] = (byte) overlay;
                    aByteArray3487[y * mapSizeX + x] = (byte) tileRotationAndShape;
                    if (locCount == 1) {
                        tileDrawInfo[y * mapSizeX + x] = Integer.valueOf(buffer.readBigSmart());
                        shapesAndRotations[y * mapSizeX + x] = buffer.readByte();
                    } else if (locCount > 1) {
                        objectIds = new int[locCount];
                        locRotationAndShape = new byte[locCount];

                        for (int index = 0; index < locCount; index++) {
                            objectIds[index] = buffer.readBigSmart();
                            locRotationAndShape[index] = buffer.readByte();
                        }

                        tileDrawInfo[y * mapSizeX + x] = new LocOnWorldMap(objectIds, locRotationAndShape);
                    }
                } else {
                    objectIds = null;
                    locRotationAndShape = null;
                    if (locCount > 0) {
                        objectIds = new int[locCount];
                        locRotationAndShape = new byte[locCount];

                        for (int index = 0; index < locCount; index++) {
                            objectIds[index] = buffer.readBigSmart();
                            locRotationAndShape[index] = buffer.readByte();
                        }
                    }

                    if (worldMapTiles[tileType - 1][i_2 - (mapSquareAbsX >> 6)][i_3 - (mapSquareAbsY >> 6)] == null) {
                        worldMapTiles[tileType - 1][i_2 - (mapSquareAbsX >> 6)][i_3 - (mapSquareAbsY >> 6)] = new ArrayList();
                    }

                    TileOnWorldMap tile = new TileOnWorldMap(x & 0x3f, y & 0x3f, underlay, overlay, tileRotationAndShape, objectIds, locRotationAndShape);
                    worldMapTiles[tileType - 1][i_2 - (mapSquareAbsX >> 6)][i_3 - (mapSquareAbsY >> 6)].add(tile);
                }
            }
        }

    }

    public static int getOverlayFloorColor(MaterialProvider materialProvider, int overlayId, int hueRandomisation, int lightnessRandomisation) {
        FloType overlay = FLOTYPE_LIST.list(overlayId);
        if (overlay == null) {
            return 0;
        } 
        int textureId = overlay.texture;
        if (textureId >= 0 && materialProvider.getMaterial(textureId).detailsOnly) {
            textureId = -1;
        }

        int finalFloorColor;
        int colorHsl;
        int lightness;
        int modifiedHsl;
        if (overlay.minimapColorHsl >= 0) {
            colorHsl = overlay.minimapColorHsl;
            lightness = lightnessRandomisation + (colorHsl & 0x7f);
            if (lightness < 0) {
                lightness = 0;
            } else if (lightness > 127) {
                lightness = 127;
            }

            modifiedHsl = lightness + (colorHsl + hueRandomisation & 0xfc00) + (colorHsl & 0x380);
            finalFloorColor = ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(ColorUtil.method4162(modifiedHsl, 96)) & 0xffff];
        } else if (textureId >= 0) {
            finalFloorColor = ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(ColorUtil.method4162(materialProvider.getMaterial(textureId).colorHsl, 96)) & 0xffff];
        } else if (overlay.tileColorHsl == -1) {
            finalFloorColor = 0;
        } else {
            colorHsl = overlay.tileColorHsl;
            lightness = lightnessRandomisation + (colorHsl & 0x7f);
            if (lightness < 0) {
                lightness = 0;
            } else if (lightness > 127) {
                lightness = 127;
            }

            modifiedHsl = lightness + (colorHsl + hueRandomisation & 0xfc00) + (colorHsl & 0x380);
            finalFloorColor = ~0xffffff | ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(ColorUtil.method4162(modifiedHsl, 96)) & 0xffff];
        }

        return finalFloorColor;
    }

    public static void renderObjectSprites(AbstractToolkit toolkit, int i_1, int i_2, int i_3, int i_4, int[] ints_5, byte[] spriteRotation) {
        if (ints_5 != null) {
            for (int index = 0; index < ints_5.length; index++) {
                ObjectType loctype = LOCTYPE_LIST.list(ints_5[index]);
                int i_9 = loctype.mapIconId;
                if (i_9 != -1) {
                    MapSpriteType mapspriteType = MAP_SPRITE_TYPE_LIST.list(i_9);
                    NativeSprite nativeSprite = mapspriteType.createRotatedNativeSprite(toolkit, loctype.mapIconRotates ? spriteRotation[index] >> 6 & 0x3 : 0, loctype.mapIconFlipped && loctype.inverted);
                    if (nativeSprite != null) {
                        int i_12 = i_3 * nativeSprite.getSpriteWidth() >> 2;
                        int i_13 = i_4 * nativeSprite.getSpriteHeight() >> 2;
                        if (mapspriteType.requiresUpscaling) {
                            int x = loctype.sizeX;
                            int y = loctype.sizeY;
                            /** Rotate the sprite **/
                            if ((spriteRotation[index] >> 6 & 0x1) == 1) {
                                int tmpX = x;
                                x = y;
                                y = tmpX;
                            }

                            i_12 = x * i_3;
                            i_13 = y * i_4;
                        }

                        if (i_12 != 0 && i_13 != 0) {
                            if (mapspriteType.backgroundColor != 0) {
                                nativeSprite.drawCircle(i_1, i_4 + (i_2 - i_13), i_12, i_13, 0, ~0xffffff | mapspriteType.backgroundColor, 1);
                            } else {
                                nativeSprite.drawCircle(i_1, i_4 + (i_2 - i_13), i_12, i_13);
                            }
                        }
                    }
                }
            }
        }

    }

    public static WorldMapElement getMapDef(int baseX, int baseY) {
        for (WorldMapElement mapDef = (WorldMapElement) WORLDMAP_DEFS.first(); mapDef != null; mapDef = (WorldMapElement) WORLDMAP_DEFS.next()) {
            if (mapDef.shouldRender && mapDef.allElementsInBounds(baseX, baseY)) {
                return mapDef;
            }
        }
        return null;
    }

    public static NodeCollection method5179(int i_1, int i_2) {
        for (WorldMapCategory class282Sub365 = (WorldMapCategory) worldMapCategories.getFirst(); class282Sub365 != null; class282Sub365 = (WorldMapCategory) worldMapCategories.next()) {
            method5146(class282Sub365, i_1, i_2);
        }

        return worldMapCategories;
    }

    public static void renderMap(AbstractToolkit graphicalrenderer0) {
        int i_1 = anInt3480 - anInt3485;
        int i_2 = anInt3486 - anInt3465;
        int i_3 = (anInt3476 - anInt3489 << 16) / i_1;
        int i_4 = (anInt3492 - anInt3490 << 16) / i_2;
        renderMap(graphicalrenderer0, i_3, i_4);
    }

    public static void method5183(int i_1, int i_2) {
        Packet buffer = new Packet(MAP_AREA_RESOURCE.getNamedFile(staticElement.filenamePrefix, "area"));
        int i_4 = buffer.readUByte();
        int[] ints_5 = new int[i_4];

        for (int i_6 = 0; i_6 < i_4; i_6++) {
            ints_5[i_6] = buffer.readUByte();
        }

        int i_6 = buffer.readUByte();
        int[] ints_7 = new int[i_6];

        for (int i_8 = 0; i_8 < i_6; i_8++) {
            ints_7[i_8] = buffer.readUByte();
        }

        while (true) {
            while (buffer.currentPosition < buffer.buffer.length) {
                if (buffer.readUByte() == 0) {
                	// By tiles in map square
                    int i_8 = buffer.readUByte();
                    int i_9 = buffer.readUByte();

                    for (int tileX = 0; tileX < 64; tileX++) {
                        for (int tileY = 0; tileY < 64; tileY++) {
                            int i_12 = i_8 * 64 + tileX - mapSquareAbsX;
                            int i_13 = i_9 * 64 + tileY - mapSquareAbsY;
                            prepareMapdata(buffer, i_8, i_9, i_12, i_13, ints_5, ints_7);
                        }
                    }
                } else {
                	// By chunks in map square
                    int i_8 = buffer.readUByte();
                    int i_9 = buffer.readUByte();
                    int i_18 = buffer.readUByte();
                    int i_11 = buffer.readUByte();

                    for (int chunkPosX = 0; chunkPosX < 8; chunkPosX++) {
                        for (int chunkPosY = 0; chunkPosY < 8; chunkPosY++) {
                            int i_20 = i_8 * 64 + chunkPosX + i_18 * 8 - mapSquareAbsX;
                            int i_15 = i_9 * 64 + chunkPosY + i_11 * 8 - mapSquareAbsY;
                            prepareMapdata(buffer, i_8, i_9, i_20, i_15, ints_5, ints_7);
                        }
                    }
                }
            }

            underlayRgbColors = new byte[mapSizeX * mapSizeY];
            aShortArray3479 = new short[mapSizeX * mapSizeY];

            for (int plane = 0; plane < 3; plane++) {
                byte[] underlays = new byte[mapSizeX * mapSizeY];

                Iterator iterator14;
                ArrayList arraylist19;
                TileOnWorldMap class26921;
                for (int x = 0; x < worldMapTiles[plane].length; x++) {
                    for (int y = 0; y < worldMapTiles[plane][0].length; y++) {
                        arraylist19 = worldMapTiles[plane][x][y];
                        if (arraylist19 != null) {
                            for (iterator14 = arraylist19.iterator(); iterator14.hasNext(); underlays[x * 64 + class26921.mapsquarePosX + (y * 64 + class26921.mapsquarePosY) * mapSizeX] = (byte) class26921.underlayIdOrColor) {
                                class26921 = (TileOnWorldMap) iterator14.next();
                            }
                        }
                    }
                }

                decodeUnderlayColors(underlays, underlayRgbColors, aShortArray3479, i_1, i_2);

                for (int i_11 = 0; i_11 < worldMapTiles[plane].length; i_11++) {
                    for (int i_12 = 0; i_12 < worldMapTiles[plane][0].length; i_12++) {
                        arraylist19 = worldMapTiles[plane][i_11][i_12];
                        if (arraylist19 != null) {
                            iterator14 = arraylist19.iterator();

                            while (iterator14.hasNext()) {
                                class26921 = (TileOnWorldMap) iterator14.next();
                                int i_16 = i_11 * 64 + class26921.mapsquarePosX + (i_12 * 64 + class26921.mapsquarePosY) * mapSizeX;
                                class26921.underlayIdOrColor = (underlayRgbColors[i_16] & 0xff) << 16 | aShortArray3479[i_16] & 0xffff;
                                if (class26921.underlayIdOrColor != 0) {
                                    class26921.underlayIdOrColor |= ~0xffffff;
                                }
                            }
                        }
                    }
                }
            }

            decodeUnderlayColors(underlayIds, underlayRgbColors, aShortArray3479, i_1, i_2);
            underlayIds = null;
            method5136();
            return;
        }
    }

    public static void decodeUnderlayColors(byte[] underlayIds, byte[] bytes_1, short[] shorts_2, int i_3, int i_4) {
        int[] underlayHue = new int[mapSizeY];
        int[] underlaySaturation = new int[mapSizeY];
        int[] underlayLightness = new int[mapSizeY];
        int[] underlayDivisor = new int[mapSizeY];
        int[] underlayDirection = new int[mapSizeY];

        for (int x = -5; x < mapSizeX; x++) {
            int positiveX = x + 5;
            int negativeX = x - 5;

            for (int y = 0; y < mapSizeY; y++) {
                FluType underlay;
                if (positiveX < mapSizeX) {
                    int underlayId = underlayIds[positiveX + y * mapSizeX] & 0xff;
                    if (underlayId > 0) {
                        underlay = FLUTYPE_LIST.list(underlayId - 1);
                        underlayHue[y] += underlay.hue;
                        underlaySaturation[y] += underlay.saturation;
                        underlayLightness[y] += underlay.lightness;
                        underlayDivisor[y] += underlay.divisor;
                        underlayDirection[y]++;
                    }
                }

                if (negativeX >= 0) {
                    int underlayId = underlayIds[negativeX + y * mapSizeX] & 0xff;
                    if (underlayId > 0) {
                        underlay = FLUTYPE_LIST.list(underlayId - 1);
                        underlayHue[y] -= underlay.hue;
                        underlaySaturation[y] -= underlay.saturation;
                        underlayLightness[y] -= underlay.lightness;
                        underlayDivisor[y] -= underlay.divisor;
                        underlayDirection[y]--;
                    }
                }
            }

            if (x >= 0) {
                int hue = 0;
                int saturation = 0;
                int lightness = 0;
                int divisor = 0;
                int direction = 0;

                for (int y = -5; y < mapSizeY; y++) {
                    int positiveY = y + 5;
                    if (positiveY < mapSizeY) {
                        hue += underlayHue[positiveY];
                        saturation += underlaySaturation[positiveY];
                        lightness += underlayLightness[positiveY];
                        divisor += underlayDivisor[positiveY];
                        direction += underlayDirection[positiveY];
                    }

                    int negativeY = y - 5;
                    if (negativeY >= 0) {
                        hue -= underlayHue[negativeY];
                        saturation -= underlaySaturation[negativeY];
                        lightness -= underlayLightness[negativeY];
                        divisor -= underlayDivisor[negativeY];
                        direction -= underlayDirection[negativeY];
                    }

                    if (y >= 0 && direction > 0) {
                        if ((underlayIds[x + y * mapSizeX] & 0xff) == 0) {
                            int i_21 = x + y * mapSizeX;
                            bytes_1[i_21] = 0;
                            shorts_2[i_21] = 0;
                        } else {
                            int hsl24 = divisor == 0 ? 0 : ColorUtil.hsl16to24(hue * 256 / divisor, saturation / direction, lightness / direction);
                            int clampedLightness = (hsl24 & 0x7f) + i_4;
                            if (clampedLightness < 0) {
                                clampedLightness = 0;
                            } else if (clampedLightness > 127) {
                                clampedLightness = 127;
                            }

                            int i_23 = clampedLightness + (i_3 + hsl24 & 0xfc00) + (hsl24 & 0x380);
                            int i_24 = x + y * mapSizeX;
                            int rgb = ColorUtil.hsvToRgbTable[ColorUtil.adjustHsv(ColorUtil.method14677(i_23, 96)) & 0xffff];
                            bytes_1[i_24] = (byte) (rgb >> 16 & 0xff);
                            shorts_2[i_24] = (short) (rgb & 0xffff);
                        }
                    }
                }
            }
        }

    }

	public static void renderWorldmapTile(AbstractToolkit toolkit, byte[][][] bytes_1, int i_2, int x, int y, int underlayColor, int overlayColor, int width, int height, int tileShape, int tileRotation, int clearType) {
	    if (tileShape != 0 && i_2 != 0) {
	        if (tileShape == 9) {
	            tileShape = 1;
	            tileRotation = tileRotation + 1 & 0x3;
	        }
	        if (tileShape == 10) {
	            tileShape = 1;
	            tileRotation = tileRotation + 3 & 0x3;
	        }
	        if (tileShape == 11) {
	            tileShape = 8;
	            tileRotation = tileRotation + 3 & 0x3;
	        }
	        toolkit.renderWorldmapTile(x, y, width, height, underlayColor, overlayColor, bytes_1[tileShape - 1][tileRotation], i_2, clearType);
	    }
	}

	public static WorldMapCategory method11595() {
	    for (WorldMapCategory category = (WorldMapCategory) WorldMapSub1.aClass4608030.next(); category != null; category = (WorldMapCategory) WorldMapSub1.aClass4608030.next()) {
	        MECType mecType = MECTYPE_LIST.list(category.mecId);
	        if (mecType != null && mecType.visible && mecType.shouldDraw(PLAYER_VAR_PROVIDER)) {
	            return category;
	        }
	    }
	    return null;
	}

	public static void drawWorldmap(AbstractToolkit toolkit, MaterialProvider materials, int left, int top, int width, int height) {
	    if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
	        WorldMapSub1.loadAssets(toolkit, materials);
	    }
	    toolkit.setClipping(left, top, left + width, top + height);
	    if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
	        byte b_7 = 20;
	        int centerX = width / 2 + left;
	        int centerY = top + height / 2 - 18 - b_7;
	        toolkit.drawFilledRectangle(left, top, width, height, ~0xffffff, 0);
	        toolkit.drawRectangleOutline(centerX - 152, centerY, 304, 34, client.DEBUG_BG_COLOR[client.DEBUG_DISPLAY_COLOR_INDEX].getRGB(), 0);
	        toolkit.drawFilledRectangle(centerX - 150, centerY + 2, ComponentMinimap.mapAssetLoadingPercentage * 3, 30, client.DEBUG_LOAD_COLOR[client.DEBUG_DISPLAY_COLOR_INDEX].getRGB(), 0);
	        MiniMenu.minimenuHeader.renderCentered(LocalizedText.LOADING.translate(client.CURRENT_LANGUAGE), centerX, b_7 + centerY, client.aColorArray7200[client.DEBUG_DISPLAY_COLOR_INDEX].getRGB(), -1);
	    } else {
	        int i_17 = WorldMapSub1.anInt5123 - (int) (width / mapZoom);
	        int i_8 = (int) (height / mapZoom) + WorldMapSub1.anInt5624;
	        int i_9 = (int) (width / mapZoom) + WorldMapSub1.anInt5123;
	        int i_10 = WorldMapSub1.anInt5624 - (int) (height / mapZoom);
	        DebugDisplay.anInt3991 = WorldMapSub1.anInt5123 - (int) (width / mapZoom);
	        WorldMapSub1.anInt5765 = WorldMapSub1.anInt5624 - (int) (height / mapZoom);
	        StaticMethods.anInt5760 = (int) ((width * 2) / mapZoom);
	        ComponentMinimap.anInt4097 = (int) ((height * 2) / mapZoom);
	        method5139(i_17 + mapSquareAbsX, i_8 + mapSquareAbsY, i_9 + mapSquareAbsX, i_10 + mapSquareAbsY, left, top, left + width, top + height + 1);
	        renderMap(toolkit);
	        NodeCollection nodecollection11 = method5135();
	        WorldMapSub1.renderInformation(toolkit, nodecollection11);
	        if (JSObjectManager.anInt4185 > 0) {
	            if (--ServerEnvironment.anInt9282 == 0) {
	                JSObjectManager.anInt4185--;
	                ServerEnvironment.anInt9282 = 20;
	            }
	        }
	        if (client.DRAW_DEBUG) {
	            int textX = left + width - 5;
	            int textY = top + height - 8;
	            FontManager.debugOverlayFont.renderLeftAligned("Fps:" + GameShell.FPS, textX, textY, 16776960, -1);
	            textY -= 15;
	            Runtime runtime = Runtime.getRuntime();
	            int freeMemoryKbs = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
	            int textColor = 16776960;
	            if (freeMemoryKbs > 65536) {
	                textColor = 16711680;
	            }
	            FontManager.debugOverlayFont.renderLeftAligned("Mem:" + freeMemoryKbs + "k", textX, textY, textColor, -1);
	            textY -= 15;
	        }
	    }
	}

}
