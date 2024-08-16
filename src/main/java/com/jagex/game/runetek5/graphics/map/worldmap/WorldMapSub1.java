package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.NodeIterator;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.GraphicsCache;
import com.jagex.game.runetek5.client.JSObjectManager;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.config.staticelements.StaticElementTypeList;
import com.jagex.game.runetek5.config.staticelements.StaticElements;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.HelveticaFont;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.BufferUtils;
import com.jagex.game.runetek5.utils.Utils;

public class WorldMapSub1 extends WorldMap {

    public static boolean aBool8027;
    public static final IterableNodeMap aClass4658025 = new IterableNodeMap(8);
    public static final IterableNodeMap aClass4658029 = new IterableNodeMap(8);
    public static int anInt8014 = -1;
    public static int anInt8016 = -1;
    public static boolean isInteractive;
    public static int anInt8019 = -1;
    public static int anInt8020 = -1;
    public static boolean aBool8028;
    
    /**
     * (White) text above worldmap areas
     * 
     * Toll<br>
     * gate
     * 
     * Mage<br>
     * Training<br>
     * Arena
     */
    public static final String[] RENDERED_WORLDMAP_AREA_NAMELINES = new String[5];
    public static int someMecId = -1;
    public static int anInt8026 = -1;
    public static final NodeIterator aClass4608030 = new NodeIterator(new NodeCollection());
	public static int anInt5123;
	public static int anInt5624;
	public static int anInt5765;
	public static int anInt488;

    public WorldMapSub1() throws Throwable {
        throw new Error();
    }

	public static void reset(int i_0, int i_1, int i_2, boolean bool3) {
	    if (PreferenceManager.preferences.activeToolkit.getValue() != 0) {
	        AbstractToolkit.activeRenderMode = PreferenceManager.preferences.activeToolkit.getValue();
	        client.loadAssets(0, true);
	    } else {
	        GraphicsCache.resetWorldmapCaches(false);
	    }
	    anInt8019 = i_1;
	    anInt8020 = i_2;
	    aBool8028 = bool3;
	    WorldMapSub1.fetch(i_0);
	}

	public static void markDestination(int x, int y, int width, int height) {
	    float fitXInY = (float) WorldMapSub1.mapSizeY / WorldMapSub1.mapSizeX;
	    int offsetX = width;
	    int offsetY = height;
	    if (fitXInY < 1.0F) {
	        offsetY = (int) (width * fitXInY);
	    } else {
	        offsetX = (int) (height / fitXInY);
	    }
	    x -= (width - offsetX) / 2;
	    y -= (height - offsetY) / 2;
	    WorldMapSub1.anInt5123 = x * WorldMapSub1.mapSizeX / offsetX;
	    WorldMapSub1.anInt5624 = WorldMapSub1.mapSizeY - WorldMapSub1.mapSizeY * y / offsetY;
	    anInt8014 = -1;
	    anInt8016 = -1;
	    WorldMapSub1.method1502();
	}

	public static void loadAssets(AbstractToolkit toolkit, MaterialProvider materialProvider) {
	    if (WorldMapSub1.staticElement != null) {
	        Utils.time();
	        Utils.time();
	        if (ComponentMinimap.mapAssetLoadingPercentage < 10) {
	            if (!WorldMapSub1.MAP_AREA_RESOURCE.isCached(WorldMapSub1.staticElement.filenamePrefix)) {
	                ComponentMinimap.mapAssetLoadingPercentage = Resource.WORLD_MAP.getArchiveLoadingPercent(WorldMapSub1.staticElement.filenamePrefix) / 10;
	                return;
	            }
	
	            StaticMethods.method6907();
	            ComponentMinimap.mapAssetLoadingPercentage = 10;
	        }
	
	        if (ComponentMinimap.mapAssetLoadingPercentage == 10) {
	            WorldMapSub1.mapSquareAbsX = WorldMapSub1.staticElement.anInt9539 >> 6 << 6;
	            WorldMapSub1.mapSquareAbsY = WorldMapSub1.staticElement.anInt9541 >> 6 << 6;
	            WorldMapSub1.mapSizeX = (WorldMapSub1.staticElement.anInt9540 >> 6 << 6) - WorldMapSub1.mapSquareAbsX + 64;
	            WorldMapSub1.mapSizeY = (WorldMapSub1.staticElement.anInt9535 >> 6 << 6) - WorldMapSub1.mapSquareAbsY + 64;
	            int[] placement = new int[3];
	            int i_4 = -1;
	            int i_5 = -1;
	            Vector3 absolute = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
	            CoordGrid base = Resource.mapRegion.getBaseCoordinates();
	            if (WorldMapSub1.staticElement.method14778(PlayerUpdate.MY_PLAYER.plane, ((int) absolute.x >> 9) + base.x, ((int) absolute.z >> 9) + base.y, placement)) {
	                i_4 = placement[1] - WorldMapSub1.mapSquareAbsX;
	                i_5 = placement[2] - WorldMapSub1.mapSquareAbsY;
	            }
	
	            if (!aBool8028 && i_4 >= 0 && i_4 < WorldMapSub1.mapSizeX && i_5 >= 0 && i_5 < WorldMapSub1.mapSizeY) {
	                i_4 += (int) (Math.random() * 10.0D) - 5;
	                i_5 += (int) (Math.random() * 10.0D) - 5;
	                WorldMapSub1.anInt5123 = i_4;
	                WorldMapSub1.anInt5624 = i_5;
	            } else if (anInt8019 != -1 && anInt8020 != -1) {
	                WorldMapSub1.staticElement.method14777(anInt8019, anInt8020, placement);
	                WorldMapSub1.anInt5123 = placement[1] - WorldMapSub1.mapSquareAbsX;
	                WorldMapSub1.anInt5624 = placement[2] - WorldMapSub1.mapSquareAbsY;
	
	                anInt8020 = -1;
	                anInt8019 = -1;
	                aBool8028 = false;
	            } else {
	                WorldMapSub1.staticElement.method14777(WorldMapSub1.staticElement.bitpackedPlacement >> 14 & 0x3fff, WorldMapSub1.staticElement.bitpackedPlacement & 0x3fff, placement);
	                WorldMapSub1.anInt5123 = placement[1] - WorldMapSub1.mapSquareAbsX;
	                WorldMapSub1.anInt5624 = placement[2] - WorldMapSub1.mapSquareAbsY;
	            }
	
	            if (WorldMapSub1.staticElement.anInt9542 == 37) {
	                WorldMapSub1.mapZoom = 3.0F;
	                WorldMapSub1.requestedMapZoom = 3.0F;
	            } else if (WorldMapSub1.staticElement.anInt9542 == 50) {
	                WorldMapSub1.mapZoom = 4.0F;
	                WorldMapSub1.requestedMapZoom = 4.0F;
	            } else if (WorldMapSub1.staticElement.anInt9542 == 75) {
	                WorldMapSub1.mapZoom = 6.0F;
	                WorldMapSub1.requestedMapZoom = 6.0F;
	            } else if (WorldMapSub1.staticElement.anInt9542 == 100) {
	                WorldMapSub1.mapZoom = 8.0F;
	                WorldMapSub1.requestedMapZoom = 8.0F;
	            } else if (WorldMapSub1.staticElement.anInt9542 == 200) {
	                WorldMapSub1.mapZoom = 16.0F;
	                WorldMapSub1.requestedMapZoom = 16.0F;
	            } else {
	                WorldMapSub1.mapZoom = 8.0F;
	                WorldMapSub1.requestedMapZoom = 8.0F;
	            }
	
	            WorldMapSub1.anInt3469 = (int) WorldMapSub1.mapZoom >> 1;
	            WorldMapSub1.aByteArrayArrayArray3464 = BufferUtils.generateBytePatterns(WorldMapSub1.anInt3469);
	            WorldMapSub1.method1502();
	            WorldMapSub1.initialize();
	            WorldMapInteraction.MEC_CLICKZONES = new NodeCollection();
	            WorldMapSub1.hueRandomisation += (int) (Math.random() * 5.0D) - 2;
	            if (WorldMapSub1.hueRandomisation < -8) {
	                WorldMapSub1.hueRandomisation = -8;
	            }
	
	            if (WorldMapSub1.hueRandomisation > 8) {
	                WorldMapSub1.hueRandomisation = 8;
	            }
	
	            WorldMapSub1.lightnessRandomisation += (int) (Math.random() * 5.0D) - 2;
	            if (WorldMapSub1.lightnessRandomisation < -16) {
	                WorldMapSub1.lightnessRandomisation = -16;
	            }
	
	            if (WorldMapSub1.lightnessRandomisation > 16) {
	                WorldMapSub1.lightnessRandomisation = 16;
	            }
	
	            WorldMapSub1.calculateRunetekPalette(materialProvider, WorldMapSub1.hueRandomisation >> 2 << 10, WorldMapSub1.lightnessRandomisation >> 1);
	            WorldMapSub1.MECTYPE_LIST.createCache(1024, 256);
	            WorldMapSub1.MAP_SPRITE_TYPE_LIST.resetCaches(256, 256);
	            WorldMapSub1.LOCTYPE_LIST.resetLruCache(4096);
	            TypeLists.VARPBIT.resetCache(256);
	            ComponentMinimap.mapAssetLoadingPercentage = 20;
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 20) {
	            PingRequester.keepalive(true);
	            WorldMapSub1.method5183(WorldMapSub1.hueRandomisation, WorldMapSub1.lightnessRandomisation);
	            ComponentMinimap.mapAssetLoadingPercentage = 60;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 60) {
	            if (WorldMapSub1.MAP_AREA_RESOURCE.validFile(WorldMapSub1.staticElement.filenamePrefix + "_staticelements")) {
	                if (!WorldMapSub1.MAP_AREA_RESOURCE.isCached(WorldMapSub1.staticElement.filenamePrefix + "_staticelements")) {
	                    return;
	                }
	
	                WorldMapSub1.staticElements = StaticElementTypeList.list(WorldMapSub1.MAP_AREA_RESOURCE, WorldMapSub1.staticElement.filenamePrefix + "_staticelements", client.memberWorld);
	            } else {
	                WorldMapSub1.staticElements = new StaticElements(0);
	            }
	
	            WorldMapSub1.loadWorldMapCategories();
	            ComponentMinimap.mapAssetLoadingPercentage = 70;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 70) {
	            Resource.helvetica11Font = new HelveticaFont(toolkit, 11, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 73;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 73) {
	            Resource.helvetica12Font = new HelveticaFont(toolkit, 12, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 76;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 76) {
	            Resource.helvetica14Font = new HelveticaFont(toolkit, 14, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 79;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 79) {
	            Resource.helvetica17Font = new HelveticaFont(toolkit, 17, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 82;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 82) {
	            Resource.helvetica19Font = new HelveticaFont(toolkit, 19, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 85;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 85) {
	            Resource.helvetica22Font = new HelveticaFont(toolkit, 22, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 88;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else if (ComponentMinimap.mapAssetLoadingPercentage == 88) {
	            Resource.helvetica26Font = new HelveticaFont(toolkit, 26, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 91;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	        } else {
	            Resource.helvetica30Font = new HelveticaFont(toolkit, 30, GameShell.gameCanvas);
	            ComponentMinimap.mapAssetLoadingPercentage = 100;
	            PingRequester.keepalive(true);
	            GameShell.resetDrawCache();
	            System.gc();
	        }
	    }	
	}

	public static void method1502() {
	    if (anInt5123 < 0) {
	        anInt5123 = 0;
	        anInt8014 = -1;
	        anInt8016 = -1;
	    }
	    if (anInt5123 > WorldMapSub1.mapSizeX) {
	        anInt5123 = WorldMapSub1.mapSizeX;
	        anInt8014 = -1;
	        anInt8016 = -1;
	    }
	    if (anInt5624 < 0) {
	        anInt5624 = 0;
	        anInt8014 = -1;
	        anInt8016 = -1;
	    }
	    if (anInt5624 > WorldMapSub1.mapSizeY) {
	        anInt5624 = WorldMapSub1.mapSizeY;
	        anInt8014 = -1;
	        anInt8016 = -1;
	    }
	}

	public static void method1503(int i_0) {
	    anInt5123 = i_0;
	    anInt8014 = -1;
	    anInt8016 = -1;
	    method1502();
	}

	public static WorldMapElement getStaticElement() {
	    return WorldMapSub1.staticElement;
	}

	public static void method5599(int i_0) {
	    anInt5624 = i_0;
	    anInt8016 = -1;
	    method1502();
	}

	public static void method2094(int i_0) {
	    someMecId = -1;
	    anInt8026 = i_0;
	    JSObjectManager.anInt4185 = 3;
	    ServerEnvironment.anInt9282 = 100;
	}

	public static boolean renderIconsAndNames(AbstractToolkit toolkit, WorldMapCategory mapCategory, MECType mectype) {
	    int i_6 = Integer.MAX_VALUE;
	    int i_7 = Integer.MIN_VALUE;
	    int i_8 = Integer.MAX_VALUE;
	    int i_9 = Integer.MIN_VALUE;
	    if (mectype.offsets != null) {
	        i_6 = (WorldMapSub1.anInt3476 - WorldMapSub1.anInt3489) * (mectype.minOffsetX + mapCategory.absX - WorldMapSub1.anInt3485) / (WorldMapSub1.anInt3480 - WorldMapSub1.anInt3485) + WorldMapSub1.anInt3489;
	        i_7 = WorldMapSub1.anInt3489 + (WorldMapSub1.anInt3476 - WorldMapSub1.anInt3489) * (mectype.maxOffsetX + mapCategory.absX - WorldMapSub1.anInt3485) / (WorldMapSub1.anInt3480 - WorldMapSub1.anInt3485);
	        i_9 = WorldMapSub1.anInt3492 - (WorldMapSub1.anInt3492 - WorldMapSub1.anInt3490) * (mectype.minOffsetY + mapCategory.absY - WorldMapSub1.anInt3465) / (WorldMapSub1.anInt3486 - WorldMapSub1.anInt3465);
	        i_8 = WorldMapSub1.anInt3492 - (WorldMapSub1.anInt3492 - WorldMapSub1.anInt3490) * (mectype.maxOffsetY + mapCategory.absY - WorldMapSub1.anInt3465) / (WorldMapSub1.anInt3486 - WorldMapSub1.anInt3465);
	    }
	    NativeSprite iconSprite = null;
	    int spriteLeft = 0;
	    int spriteRight = 0;
	    int spriteBottom = 0;
	    int spriteTop = 0;
	    if (mectype.defaultIconArchive != -1) {
	        if (mapCategory.isMouseOver && mectype.mouseOverIconArchive != -1) {
	            iconSprite = mectype.getIcon(toolkit, true);
	        } else {
	            iconSprite = mectype.getIcon(toolkit, false);
	        }
	        if (iconSprite != null) {
	            spriteLeft = mapCategory.anInt7990 - (iconSprite.getSpriteWidth() + 1 >> 1);
	            if (spriteLeft < i_6) {
	                i_6 = spriteLeft;
	            }
	            spriteRight = mapCategory.anInt7990 + (iconSprite.getSpriteWidth() + 1 >> 1);
	            if (spriteRight > i_7) {
	                i_7 = spriteRight;
	            }
	            spriteBottom = mapCategory.anInt7992 - (iconSprite.getSpriteHeight() + 1 >> 1);
	            if (spriteBottom < i_8) {
	                i_8 = spriteBottom;
	            }
	            spriteTop = mapCategory.anInt7992 + (iconSprite.getSpriteHeight() + 1 >> 1);
	            if (spriteTop > i_9) {
	                i_9 = spriteTop;
	            }
	        }
	    }
	    HelveticaFont font = null;
	    int i_16 = 0;
	    int i_17 = 0;
	    int i_18 = 0;
	    int i_19 = 0;
	    int textLeft = 0;
	    int textRight = 0;
	    int textBottom = 0;
	    int textTop = 0;
	    if (mectype.areaName != null) {
	        font = WorldMapSub1.getZoomedHelveticafont(mectype.baseTextzoom);
	        if (font != null) {
	            i_16 = FontManager.worldMapAreaFont.splitLinesToBuffer(mectype.areaName, null, RENDERED_WORLDMAP_AREA_NAMELINES, null);
	            i_17 = (WorldMapSub1.anInt3476 - WorldMapSub1.anInt3489) * mectype.anInt2750 / (WorldMapSub1.anInt3480 - WorldMapSub1.anInt3485) + mapCategory.anInt7990;
	            i_18 = mapCategory.anInt7992 - (WorldMapSub1.anInt3492 - WorldMapSub1.anInt3490) * mectype.anInt2752 / (WorldMapSub1.anInt3486 - WorldMapSub1.anInt3465);
	            if (iconSprite == null) {
	                i_18 -= font.method3186() * i_16 / 2;
	            } else {
	                i_18 -= (iconSprite.getSpriteHeight() >> 1) + font.getLineHeight() * i_16;
	            }
	            for (int index = 0; index < i_16; index++) {
	                String areaName = RENDERED_WORLDMAP_AREA_NAMELINES[index];
	                if (index < i_16 - 1) {
	                    areaName = areaName.substring(0, areaName.length() - 4);
	                }
	                int areaNameWidth = font.getTextWidth(areaName);
	                if (areaNameWidth > i_19) {
	                    i_19 = areaNameWidth;
	                }
	            }
	            textLeft = (i_17 - i_19 / 2);
	            if (textLeft < i_6) {
	                i_6 = textLeft;
	            }
	            textRight = i_19 / 2 + i_17;
	            if (textRight > i_7) {
	                i_7 = textRight;
	            }
	            textBottom = i_18;
	            if (textBottom < i_8) {
	                i_8 = textBottom;
	            }
	            textTop = i_18 + i_16 * font.getLineHeight();
	            if (textTop > i_9) {
	                i_9 = textTop;
	            }
	        }
	    }
	    if (i_7 >= WorldMapSub1.anInt3489 && i_6 <= WorldMapSub1.anInt3476 && i_9 >= WorldMapSub1.anInt3490 && i_8 <= WorldMapSub1.anInt3492) {
	        WorldMapSub1.method5147(toolkit, mapCategory, mectype);
	        if (iconSprite != null) {
	            if (JSObjectManager.anInt4185 > 0 && (someMecId != -1 && someMecId == mapCategory.mecId || anInt8026 != -1 && mectype.anInt2718 == anInt8026)) {
	                int color;
	            	if (ServerEnvironment.anInt9282 > 50) {
	                    color = 200 - ServerEnvironment.anInt9282 * 2;
	                } else {
	                    color = ServerEnvironment.anInt9282 * 2;
	                }
	                int alpha = color << 24 | 0xffff00;
	                toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 7, alpha);
	                toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 5, alpha);
	                toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 3, alpha);
	                toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 1, alpha);
	                toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2, alpha);
	            }
	            iconSprite.draw(mapCategory.anInt7990 - (iconSprite.getSpriteWidth() >> 1), mapCategory.anInt7992 - (iconSprite.getSpriteHeight() >> 1));
	        }
	        if (mectype.areaName != null && font != null) {
	            WorldMapElement.renderWorldmapAreaText(toolkit, mapCategory, mectype, i_16, i_17, i_18, i_19, font);
	        }
	        if (mectype.defaultIconArchive != -1 || mectype.areaName != null) {
	            ClickzoneMEC mecClickzone = new ClickzoneMEC(mapCategory);
	            mecClickzone.spriteLeft = spriteLeft;
	            mecClickzone.spriteRight = spriteRight;
	            mecClickzone.spriteBottom = spriteBottom;
	            mecClickzone.spriteTop = spriteTop;
	            mecClickzone.textLeft = textLeft;
	            mecClickzone.textRight = textRight;
	            mecClickzone.textBottom = textBottom;
	            mecClickzone.textTop = textTop;
	            WorldMapInteraction.MEC_CLICKZONES.addFirst(mecClickzone);
	        }
	        return false;
	    } else {
	        return true;
	    }
	}

	public static HelveticaFont getZoomedHelveticafont(int predefinedTextZoom) {
	    if (predefinedTextZoom == 0) {
	        if (WorldMapSub1.mapZoom == 3.0D) {
	            return Resource.helvetica11Font;
	        }
	        if (WorldMapSub1.mapZoom == 4.0D) {
	            return Resource.helvetica12Font;
	        }
	        if (WorldMapSub1.mapZoom == 6.0D) {
	            return Resource.helvetica14Font;
	        }
	        if (WorldMapSub1.mapZoom >= 8.0D) {
	            return Resource.helvetica17Font;
	        }
	    } else if (predefinedTextZoom == 1) {
	        if (WorldMapSub1.mapZoom == 3.0D) {
	            return Resource.helvetica14Font;
	        }
	        if (WorldMapSub1.mapZoom == 4.0D) {
	            return Resource.helvetica17Font;
	        }
	        if (WorldMapSub1.mapZoom == 6.0D) {
	            return Resource.helvetica19Font;
	        }
	        if (WorldMapSub1.mapZoom >= 8.0D) {
	            return Resource.helvetica22Font;
	        }
	    } else if (predefinedTextZoom == 2) {
	        if (WorldMapSub1.mapZoom == 3.0D) {
	            return Resource.helvetica19Font;
	        }
	        if (WorldMapSub1.mapZoom == 4.0D) {
	            return Resource.helvetica22Font;
	        }
	        if (WorldMapSub1.mapZoom == 6.0D) {
	            return Resource.helvetica26Font;
	        }
	        if (WorldMapSub1.mapZoom >= 8.0D) {
	            return Resource.helvetica30Font;
	        }
	    }
	    return null;
	}

	public static void renderInformation(AbstractToolkit toolkit, NodeCollection collection) {
	    WorldMapInteraction.MEC_CLICKZONES.remove_all();
	    if (!aBool8027) {
	        for (WorldMapCategory category = (WorldMapCategory) collection.getFirst(); category != null; category = (WorldMapCategory) collection.next()) {
	            MECType mecType = WorldMapSub1.MECTYPE_LIST.list(category.mecId);
	            if (WorldMapSub1.shouldDraw(mecType)) {
	                boolean bool7 = renderIconsAndNames(toolkit, category, mecType);
	                if (bool7) {
	                    WorldMapSub1.method13297(toolkit, category, mecType);
	                }
	            }
	        }
	    }
	}

	public static boolean shouldDraw(MECType mecType) {
	    return mecType != null && (mecType.displayedOnWorldmap && (mecType.shouldDraw(WorldMapSub1.PLAYER_VAR_PROVIDER) && (aClass4658025.get(mecType.mectypeId) == null && aClass4658029.get(mecType.anInt2718) == null)));
	}

	public static void method13297(AbstractToolkit toolkit, WorldMapCategory category, MECType mectype) {
	    NativeSprite sprite = mectype.getSprite(toolkit);
	    if (sprite != null) {
	        int spriteDimension = sprite.getWidth();
	        if (sprite.getHeight() > spriteDimension) {
	            spriteDimension = sprite.getHeight();
	        }
	
	        byte b_6 = 10;
	        int spriteCenterX = category.anInt7990;
	        int spriteCenterY = category.anInt7992;
	        int i_9 = 0;
	        int i_10 = 0;
	        int i_11 = 0;
	        int i_14;
	        if (mectype.areaName != null) {
	            i_9 = FontManager.worldMapAreaFont.splitLinesToBuffer(mectype.areaName, null, RENDERED_WORLDMAP_AREA_NAMELINES, null);
	
	            for (int iii = 0; iii < i_9; iii++) {
	                String string_22 = RENDERED_WORLDMAP_AREA_NAMELINES[iii];
	                if (iii < i_9 - 1) {
	                    string_22 = string_22.substring(0, string_22.length() - 4);
	                }
	
	                i_14 = Resource.helvetica14Font.getTextWidth(string_22);
	                if (i_14 > i_10) {
	                    i_10 = i_14;
	                }
	            }
	
	            i_11 = Resource.helvetica14Font.getLineHeight() * i_9 + Resource.helvetica14Font.method3186() / 2;
	        }
	
	        int x = spriteDimension / 2 + category.anInt7990;
	        int y = category.anInt7992;
	        if (spriteCenterX < spriteDimension + WorldMapSub1.anInt3489) {
	            spriteCenterX = WorldMapSub1.anInt3489;
	            x = spriteDimension / 2 + i_10 / 2 + b_6 + WorldMapSub1.anInt3489 + 5;
	        } else if (spriteCenterX > WorldMapSub1.anInt3476 - spriteDimension) {
	            spriteCenterX = WorldMapSub1.anInt3476 - spriteDimension;
	            x = WorldMapSub1.anInt3476 - spriteDimension / 2 - b_6 - i_10 / 2 - 5;
	        }
	
	        if (spriteCenterY < spriteDimension + WorldMapSub1.anInt3490) {
	            spriteCenterY = WorldMapSub1.anInt3490;
	            y = spriteDimension / 2 + b_6 + WorldMapSub1.anInt3490;
	        } else if (spriteCenterY > WorldMapSub1.anInt3492 - spriteDimension) {
	            spriteCenterY = WorldMapSub1.anInt3492 - spriteDimension;
	            y = WorldMapSub1.anInt3492 - spriteDimension / 2 - b_6 - i_11;
	        }
	
	        i_14 = (int) (Math.atan2(spriteCenterX - category.anInt7990, spriteCenterY - category.anInt7992) / Math.PI * 32767.0D) & 0xffff;
	        sprite.method2758(spriteCenterX + spriteDimension / 2.0F, spriteDimension / 2.0F + spriteCenterY, 4096, i_14);
	        int textLeft = -2;
	        int textBottom = -2;
	        int textRight = -2;
	        int textTop = -2;
	        if (mectype.areaName != null) {
	            textLeft = x - i_10 / 2 - 5;
	            textBottom = y;
	            textRight = textLeft + i_10 + 10;
	            textTop = y + Resource.helvetica14Font.getLineHeight() * i_9 + 3;
	            if (mectype.backgroundColor != 0) {
	                toolkit.clearPixels(textLeft, y, textRight - textLeft, textTop - y, mectype.backgroundColor);
	            }
	
	            if (mectype.outlineColor != 0) {
	                toolkit.drawOpaqueRectangle(textLeft, y, textRight - textLeft, textTop - y, mectype.outlineColor);
	            }
	
	            for (int i_19 = 0; i_19 < i_9; i_19++) {
	                String text = RENDERED_WORLDMAP_AREA_NAMELINES[i_19];
	                if (i_19 < i_9 - 1) {
	                    text = text.substring(0, text.length() - 4);
	                }
	
	                Resource.helvetica14Font.drawClippedText(toolkit, text, x, y, mectype.defaultTextColor);
	                y += Resource.helvetica14Font.getLineHeight();
	            }
	        }
	
	        if (mectype.defaultIconArchive != -1 || mectype.areaName != null) {
	            ClickzoneMEC clickzone = new ClickzoneMEC(category);
	            spriteDimension >>= 1;
	            clickzone.spriteLeft = spriteCenterX - spriteDimension;
	            clickzone.spriteRight = spriteCenterX + spriteDimension;
	            clickzone.spriteBottom = spriteCenterY - spriteDimension;
	            clickzone.spriteTop = spriteCenterY + spriteDimension;
	            clickzone.textLeft = textLeft;
	            clickzone.textRight = textRight;
	            clickzone.textBottom = textBottom;
	            clickzone.textTop = textTop;
	            WorldMapInteraction.MEC_CLICKZONES.addFirst(clickzone);
	        }
	    }
	
	}

	public static WorldMapCategory method12538() {
	    aClass4608030.method7684(WorldMapSub1.worldMapCategories);
	    WorldMapCategory category = (WorldMapCategory) aClass4608030.method7683();
	    if (category == null) {
	        return null;
	    } 
	    MECType mectype = WorldMapSub1.MECTYPE_LIST.list(category.mecId);
	    return mectype != null && mectype.visible && mectype.shouldDraw(WorldMapSub1.PLAYER_VAR_PROVIDER) ? category : WorldMap.method11595();
	}

	public static void reset() {
	    if (WorldMapSub1.anInt488 != -1) {
	        reset(WorldMapSub1.anInt488, -1, -1, false);
	        WorldMapSub1.anInt488 = -1;
	    }
	}
}
