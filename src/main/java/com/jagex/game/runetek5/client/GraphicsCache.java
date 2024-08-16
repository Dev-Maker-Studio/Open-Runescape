package com.jagex.game.runetek5.client;

import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.graphics.hint.HintArrow;
import com.jagex.game.runetek5.graphics.hint.HintTrail;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class GraphicsCache {

	public static void resetGraphicCaches() {
	    int preferenceMask = 0;
	    if (PreferenceManager.preferences.lightDetail.getValue() == 1) {
	        preferenceMask |= 0x1;
	        preferenceMask |= 0x10;
	        preferenceMask |= 0x20;
	        preferenceMask |= 0x2;
	        preferenceMask |= 0x4;
	    }
	    if (PreferenceManager.preferences.textures.getValue() == 0) {
	        preferenceMask |= DISABLE_TEXTURES;
	    }
	    Component.resetCache(preferenceMask);
	    Resource.mapRegion.getLoctypeList().resetCaches(preferenceMask);
	    Resource.mapRegionLoaderTask.getMapRegion().getLoctypeList().resetCaches(preferenceMask);
	    TypeLists.OBJ.resetCache(preferenceMask);
	    TypeLists.NPC.setPreferenceFlags(preferenceMask);
	    TypeLists.SPOT_ANIM.setPreferenceMask(preferenceMask);
	    HintArrow.resetCache(preferenceMask);
	    HintTrail.resetCache(preferenceMask);
	    PathingEntity.resetCache(preferenceMask);
	    PlayerAppearance.setPreferences(preferenceMask);
	    ItemContainer.resetCache(preferenceMask);
	    Resource.mapRegion.reload();
	}

	public static void resetWorldmapCaches(boolean bool0) {
	    if (bool0 && WorldMapSub1.staticElement != null) {
	        WorldMapSub1.anInt488 = WorldMapSub1.staticElement.anInt9536;
	    } else {
	        WorldMapSub1.anInt488 = -1;
	    }
	    WorldMapSub1.staticElement = null;
	    WorldMapInteraction.MEC_CLICKZONES = null;
	    ComponentMinimap.mapAssetLoadingPercentage = 0;
	    WorldMap.worldmapComponent = null;
	    WorldMapSub1.reset();
	    WorldMapSub1.worldMapCategories.remove_all();
	    WorldMapSub1.staticElements = null;
	    ComponentMinimap.aNativeSprite5883 = null;
	    WorldMapSub1.anInt8014 = -1;
	    WorldMapSub1.anInt8016 = -1;
	    Resource.helvetica11Font = null;
	    Resource.helvetica12Font = null;
	    Resource.helvetica14Font = null;
	    Resource.helvetica17Font = null;
	    Resource.helvetica19Font = null;
	    Resource.helvetica22Font = null;
	    Resource.helvetica26Font = null;
	    Resource.helvetica30Font = null;
	    if (WorldMapSub1.MECTYPE_LIST != null) {
	        WorldMapSub1.MECTYPE_LIST.resetCache();
	        WorldMapSub1.MECTYPE_LIST.createCache(128, 64);
	    }
	    if (WorldMapSub1.MAP_SPRITE_TYPE_LIST != null) {
	        WorldMapSub1.MAP_SPRITE_TYPE_LIST.resetCaches(64, 64);
	    }
	    if (WorldMapSub1.LOCTYPE_LIST != null) {
	        WorldMapSub1.LOCTYPE_LIST.resetLruCache(256);
	    }
	    TypeLists.VARPBIT.resetCache(64);
	}
}
