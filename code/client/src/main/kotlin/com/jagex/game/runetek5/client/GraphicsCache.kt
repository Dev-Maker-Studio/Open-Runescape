package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.graphics.hint.HintArrow
import com.jagex.game.runetek5.graphics.hint.HintTrail
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.scene.AsyncMapLoader

object GraphicsCache {

    @JvmStatic
    fun resetGraphicCaches() {
        var preferenceMask = 0
        if (PreferenceManager.preferences!!.lightDetail!!.getValue() == 1) {
            preferenceMask = preferenceMask or 0x1
            preferenceMask = preferenceMask or 0x10
            preferenceMask = preferenceMask or 0x20
            preferenceMask = preferenceMask or 0x2
            preferenceMask = preferenceMask or 0x4
        }
        if (PreferenceManager.preferences!!.textures!!.getValue() == 0) {
            preferenceMask = preferenceMask or ModelFunctionMask.DISABLE_TEXTURES
        }
        Component.resetCache(preferenceMask)
        Resource.mapRegion!!.getLoctypeList().resetCaches(preferenceMask)
        val mapRegionLoader = Resource.mapRegionLoaderTask!!
        synchronized(mapRegionLoader.stateLock) {
            if (mapRegionLoader.state == AsyncMapLoader.State.IDLE) {
                mapRegionLoader.getMapRegion().getLoctypeList().resetCaches(preferenceMask)
            }
        }
        TypeLists.OBJ!!.resetCache(preferenceMask)
        TypeLists.NPC!!.setPreferenceFlags(preferenceMask)
        TypeLists.SPOT_ANIM!!.setPreferenceMask(preferenceMask)
        HintArrow.resetCache(preferenceMask)
        HintTrail.resetCache(preferenceMask)
        PathingEntity.resetCache(preferenceMask)
        PlayerAppearance.setPreferences(preferenceMask)
        ItemContainer.resetCache(preferenceMask)
        Resource.mapRegion!!.reload()
    }

    @JvmStatic
    fun resetWorldmapCaches(preserveStaticElement: Boolean) {
        if (preserveStaticElement && WorldMap.staticElement != null) {
            WorldMapDisplay.pendingMapElementId = WorldMap.staticElement!!.elementId
        } else {
            WorldMapDisplay.pendingMapElementId = -1
        }
        WorldMap.staticElement = null
        WorldMapInteraction.MEC_CLICKZONES = null
        ComponentMinimap.mapAssetLoadingPercentage = 0
        WorldMap.worldmapComponent = null
        WorldMapDisplay.reset()
        WorldMap.worldMapCategories.remove_all()
        WorldMap.staticElements = null
        ComponentMinimap.worldMapOverviewSprite = null
        WorldMapDisplay.scrollTargetX = -1
        WorldMapDisplay.scrollTargetY = -1
        Resource.helvetica11Font = null
        Resource.helvetica12Font = null
        Resource.helvetica14Font = null
        Resource.helvetica17Font = null
        Resource.helvetica19Font = null
        Resource.helvetica22Font = null
        Resource.helvetica26Font = null
        Resource.helvetica30Font = null
        if (WorldMap.MECTYPE_LIST != null) {
            WorldMap.MECTYPE_LIST!!.resetCache()
            WorldMap.MECTYPE_LIST!!.createCache(128, 64)
        }
        if (WorldMap.MAP_SPRITE_TYPE_LIST != null) {
            WorldMap.MAP_SPRITE_TYPE_LIST!!.resetCaches(64, 64)
        }
        if (WorldMap.LOCTYPE_LIST != null) {
            WorldMap.LOCTYPE_LIST!!.resetLruCache(256)
        }
        TypeLists.VARPBIT!!.resetCache(64)
    }
}
