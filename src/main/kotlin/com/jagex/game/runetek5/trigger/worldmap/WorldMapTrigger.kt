package com.jagex.game.runetek5.trigger.worldmap

import com.jagex.game.runetek5.client.JSObjectManager
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.graphics.map.worldmap.ClickzoneMEC
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1
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
            WorldMapSub1.method1502()
            WorldMap.anInt3469 = WorldMap.mapZoom.toInt() shr 1
            WorldMap.aByteArrayArrayArray3464 = BufferUtils.generateBytePatterns(WorldMap.anInt3469)
        } else if (WorldMap.mapZoom > WorldMap.requestedMapZoom) {
            WorldMap.mapZoom = (WorldMap.mapZoom - WorldMap.mapZoom / 30.0).toFloat()
            if (WorldMap.mapZoom < WorldMap.requestedMapZoom) {
                WorldMap.mapZoom = WorldMap.requestedMapZoom
            }
            WorldMapSub1.method1502()
            WorldMap.anInt3469 = WorldMap.mapZoom.toInt() shr 1
            WorldMap.aByteArrayArrayArray3464 = BufferUtils.generateBytePatterns(WorldMap.anInt3469)
        }
        if (WorldMapSub1.anInt8014 != -1 && WorldMapSub1.anInt8016 != -1) {
            var i_3 = WorldMapSub1.anInt8014 - WorldMapSub1.anInt5123
            if (i_3 < 2 || i_3 > 2) {
                i_3 /= 8
            }
            var i_4 = WorldMapSub1.anInt8016 - WorldMapSub1.anInt5624
            if (i_4 < 2 || i_4 > 2) {
                i_4 /= 8
            }
            WorldMapSub1.anInt5123 += i_3
            WorldMapSub1.anInt5624 += i_4
            if (i_3 == 0 && i_4 == 0) {
                WorldMapSub1.anInt8014 = -1
                WorldMapSub1.anInt8016 = -1
            }
            WorldMapSub1.method1502()
        }
        if (JSObjectManager.anInt4185 > 0) {
            if (--ServerEnvironment.anInt9282 == 0) {
                JSObjectManager.anInt4185--
                ServerEnvironment.anInt9282 = 100
            }
        } else {
            WorldMapSub1.someMecId = -1
            WorldMapSub1.anInt8026 = -1
        }
        val clickzones = WorldMapInteraction.MEC_CLICKZONES
        if (WorldMapSub1.isInteractive && clickzones != null) {
            var mecClickzone = clickzones.getFirst() as ClickzoneMEC?
            while (mecClickzone != null) {
                val mec = WorldMap.MECTYPE_LIST!!.list(mecClickzone.category.mecId)
                val mecId = mecClickzone.category.mecId.toLong()
                if (mecClickzone.mouseWithin(x, y)) {
                    if (mec.minimenuActions[4] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[4]!!, mec.minimenuName!!, -1, 1012, -1, mecId, mec.anInt2718, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[3] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[3]!!, mec.minimenuName!!, -1, 1011, -1, mecId, mec.anInt2718, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[2] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[2]!!, mec.minimenuName!!, -1, 1010, -1, mecId, mec.anInt2718, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[1] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[1]!!, mec.minimenuName!!, -1, 1009, -1, mecId, mec.anInt2718, 0, true, false, mecId, false)
                    }
                    if (mec.minimenuActions[0] != null) {
                        MiniMenu.addEntry(mec.minimenuActions[0]!!, mec.minimenuName!!, -1, 1008, -1, mecId, mec.anInt2718, 0, true, false, mecId, false)
                    }
                    if (!mecClickzone.category.isMouseOver) {
                        mecClickzone.category.isMouseOver = true
                        CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_ENTER, mecClickzone.category.mecId, mec.anInt2718)
                    }
                    if (mecClickzone.category.isMouseOver) {
                        CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_OVER, mecClickzone.category.mecId, mec.anInt2718)
                    }
                } else if (mecClickzone.category.isMouseOver) {
                    mecClickzone.category.isMouseOver = false
                    CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_LEAVE, mecClickzone.category.mecId, mec.anInt2718)
                }
                mecClickzone = clickzones.next() as ClickzoneMEC?
            }
        }
    }
}
