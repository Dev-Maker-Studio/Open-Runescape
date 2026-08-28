package com.jagex.game.runetek5.trigger.worldmap

import com.jagex.game.runetek5.client.JSObjectManager
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.graphics.map.worldmap.ClickzoneMEC
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay
import com.jagex.game.runetek5.trigger.ClientTriggerType
import com.jagex.game.runetek5.utils.BufferUtils

/**
 *
 * @author Steven - 06/10/2023
 *
 */
object WorldMapTrigger {

    @JvmStatic
    fun onWorldmapMinimenuClick(clickType: Int, uid: Int, attr1: Int) {
        when (clickType) {
            1008 -> CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION1, uid, attr1)
            1009 -> CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION2, uid, attr1)
            1010 -> CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION3, uid, attr1)
            1011 -> CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION4, uid, attr1)
            1012 -> CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION5, uid, attr1)
        }
    }

    @JvmStatic
    fun ifWorldmapHover(x: Int, y: Int) {
        if (WorldMap.mapZoom < WorldMap.requestedMapZoom) {
            WorldMap.mapZoom = (WorldMap.mapZoom + WorldMap.mapZoom / 30.0).toFloat()
            if (WorldMap.mapZoom > WorldMap.requestedMapZoom) {
                WorldMap.mapZoom = WorldMap.requestedMapZoom
            }
            WorldMapDisplay.clampMapCenter()
            WorldMap.halfZoomLevel = WorldMap.mapZoom.toInt() shr 1
            WorldMap.tileShapePatterns = BufferUtils.generateBytePatterns(WorldMap.halfZoomLevel)
        } else if (WorldMap.mapZoom > WorldMap.requestedMapZoom) {
            WorldMap.mapZoom = (WorldMap.mapZoom - WorldMap.mapZoom / 30.0).toFloat()
            if (WorldMap.mapZoom < WorldMap.requestedMapZoom) {
                WorldMap.mapZoom = WorldMap.requestedMapZoom
            }
            WorldMapDisplay.clampMapCenter()
            WorldMap.halfZoomLevel = WorldMap.mapZoom.toInt() shr 1
            WorldMap.tileShapePatterns = BufferUtils.generateBytePatterns(WorldMap.halfZoomLevel)
        }
        if (WorldMapDisplay.scrollTargetX != -1 && WorldMapDisplay.scrollTargetY != -1) {
            var dx = WorldMapDisplay.scrollTargetX - WorldMapDisplay.mapCenterX
            if (dx < 2 || dx > 2) {
                dx /= 8
            }
            var dy = WorldMapDisplay.scrollTargetY - WorldMapDisplay.mapCenterY
            if (dy < 2 || dy > 2) {
                dy /= 8
            }
            WorldMapDisplay.mapCenterX += dx
            WorldMapDisplay.mapCenterY += dy
            if (dx == 0 && dy == 0) {
                WorldMapDisplay.scrollTargetX = -1
                WorldMapDisplay.scrollTargetY = -1
            }
            WorldMapDisplay.clampMapCenter()
        }
        if (JSObjectManager.highlightFlashCycles > 0) {
            if (--ServerEnvironment.flashCycleCounter == 0) {
                JSObjectManager.highlightFlashCycles--
                ServerEnvironment.flashCycleCounter = 100
            }
        } else {
            WorldMapDisplay.someMecId = -1
            WorldMapDisplay.highlightedCategoryId = -1
        }
        val clickzones = WorldMapInteraction.MEC_CLICKZONES
        if (WorldMapDisplay.isInteractive && clickzones != null) {
            var mecClickzone = clickzones.getFirst() as ClickzoneMEC?
            while (mecClickzone != null) {
                val mec = WorldMap.MECTYPE_LIST!!.list(mecClickzone.category.mecId)
                val mecId = mecClickzone.category.mecId.toLong()
                if (mecClickzone.mouseWithin(x, y)) {
                    if (mec.minimenuActions[4] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[4]!!, mec.minimenuName!!, -1, 1012, -1, mecId, mec.categoryId, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[3] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[3]!!, mec.minimenuName!!, -1, 1011, -1, mecId, mec.categoryId, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[2] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[2]!!, mec.minimenuName!!, -1, 1010, -1, mecId, mec.categoryId, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[1] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[1]!!, mec.minimenuName!!, -1, 1009, -1, mecId, mec.categoryId, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[0] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[0]!!, mec.minimenuName!!, -1, 1008, -1, mecId, mec.categoryId, 0, true, false, mecId, false)
                    }
                    if (!mecClickzone.category.isMouseOver) {
                        mecClickzone.category.isMouseOver = true
                        CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_ENTER, mecClickzone.category.mecId, mec.categoryId)
                    }
                    if (mecClickzone.category.isMouseOver) {
                        CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_OVER, mecClickzone.category.mecId, mec.categoryId)
                    }
                } else if (mecClickzone.category.isMouseOver) {
                    mecClickzone.category.isMouseOver = false
                    CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_LEAVE, mecClickzone.category.mecId, mec.categoryId)
                }
                mecClickzone = clickzones.next() as ClickzoneMEC?
            }
        }
    }
}
