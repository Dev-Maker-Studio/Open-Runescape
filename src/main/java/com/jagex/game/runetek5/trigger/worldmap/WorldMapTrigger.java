package com.jagex.game.runetek5.trigger.worldmap;

import com.jagex.game.runetek5.client.JSObjectManager;
import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.graphics.map.worldmap.ClickzoneMEC;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1;
import com.jagex.game.runetek5.trigger.ClientTriggerType;
import com.jagex.game.runetek5.utils.BufferUtils;

/**
 * 
 * @author Steven - 06/10/2023
 *
 */
public class WorldMapTrigger {

	public static void onWorldmapMinimenuClick(int clickType, int uid, int attr1) {
	    if (clickType == 1008) {
	        CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION1, uid, attr1);
	    } else if (clickType == 1009) {
	        CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION2, uid, attr1);
	    } else if (clickType == 1010) {
	        CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION3, uid, attr1);
	    } else if (clickType == 1011) {
	        CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION4, uid, attr1);
	    } else if (clickType == 1012) {
	        CS2Executor.executeTrigger(ClientTriggerType.MINIMENU_CLICK_MEC_OPTION5, uid, attr1);
	    }
	}

	public static void ifWorldmapHover(int x, int y) {
	    if (WorldMapSub1.mapZoom < WorldMapSub1.requestedMapZoom) {
	        WorldMapSub1.mapZoom = (float) (WorldMapSub1.mapZoom + WorldMapSub1.mapZoom / 30.0D);
	        if (WorldMapSub1.mapZoom > WorldMapSub1.requestedMapZoom) {
	            WorldMapSub1.mapZoom = WorldMapSub1.requestedMapZoom;
	        }
	        WorldMapSub1.method1502();
	        WorldMapSub1.anInt3469 = (int) WorldMapSub1.mapZoom >> 1;
	        WorldMapSub1.aByteArrayArrayArray3464 = BufferUtils.generateBytePatterns(WorldMapSub1.anInt3469);
	    } else if (WorldMapSub1.mapZoom > WorldMapSub1.requestedMapZoom) {
	        WorldMapSub1.mapZoom = (float) (WorldMapSub1.mapZoom - WorldMapSub1.mapZoom / 30.0D);
	        if (WorldMapSub1.mapZoom < WorldMapSub1.requestedMapZoom) {
	            WorldMapSub1.mapZoom = WorldMapSub1.requestedMapZoom;
	        }
	        WorldMapSub1.method1502();
	        WorldMapSub1.anInt3469 = (int) WorldMapSub1.mapZoom >> 1;
	        WorldMapSub1.aByteArrayArrayArray3464 = BufferUtils.generateBytePatterns(WorldMapSub1.anInt3469);
	    }
	    if (WorldMapSub1.anInt8014 != -1 && WorldMapSub1.anInt8016 != -1) {
	        int i_3 = WorldMapSub1.anInt8014 - WorldMapSub1.anInt5123;
	        if (i_3 < 2 || i_3 > 2) {
	            i_3 /= 8;
	        }
	        int i_4 = WorldMapSub1.anInt8016 - WorldMapSub1.anInt5624;
	        if (i_4 < 2 || i_4 > 2) {
	            i_4 /= 8;
	        }
	        WorldMapSub1.anInt5123 += i_3;
	        WorldMapSub1.anInt5624 += i_4;
	        if (i_3 == 0 && i_4 == 0) {
	            WorldMapSub1.anInt8014 = -1;
	            WorldMapSub1.anInt8016 = -1;
	        }
	        WorldMapSub1.method1502();
	    }
	    if (JSObjectManager.anInt4185 > 0) {
	        if (--ServerEnvironment.anInt9282 == 0) {
	            JSObjectManager.anInt4185--;
	            ServerEnvironment.anInt9282 = 100;
	        }
	    } else {
	        WorldMapSub1.someMecId = -1;
	        WorldMapSub1.anInt8026 = -1;
	    }
	    if (WorldMapSub1.isInteractive && WorldMapInteraction.MEC_CLICKZONES != null) {
	        for (ClickzoneMEC mecClickzone = (ClickzoneMEC) WorldMapInteraction.MEC_CLICKZONES.getFirst(); mecClickzone != null; mecClickzone = (ClickzoneMEC) WorldMapInteraction.MEC_CLICKZONES.next()) {
	            MECType mec = WorldMapSub1.MECTYPE_LIST.list(mecClickzone.category.mecId);
	            if (mecClickzone.mouseWithin(x, y)) {
	                if (mec.minimenuActions[4] != null) {
	                    MiniMenu.addEntry(mec.minimenuActions[4], mec.minimenuName, -1, 1012, -1, mecClickzone.category.mecId, mec.anInt2718, 0, true, false, mecClickzone.category.mecId, false);
	                }
	                if (mec.minimenuActions[3] != null) {
	                    MiniMenu.addEntry(mec.minimenuActions[3], mec.minimenuName, -1, 1011, -1, mecClickzone.category.mecId, mec.anInt2718, 0, true, false, mecClickzone.category.mecId, false);
	                }
	                if (mec.minimenuActions[2] != null) {
	                    MiniMenu.addEntry(mec.minimenuActions[2], mec.minimenuName, -1, 1010, -1, mecClickzone.category.mecId, mec.anInt2718, 0, true, false, mecClickzone.category.mecId, false);
	                }
	                if (mec.minimenuActions[1] != null) {
	                    MiniMenu.addEntry(mec.minimenuActions[1], mec.minimenuName, -1, 1009, -1, mecClickzone.category.mecId, mec.anInt2718, 0, true, false, mecClickzone.category.mecId, false);
	                }
	                if (mec.minimenuActions[0] != null) {
	                    MiniMenu.addEntry(mec.minimenuActions[0], mec.minimenuName, -1, 1008, -1, mecClickzone.category.mecId, mec.anInt2718, 0, true, false, mecClickzone.category.mecId, false);
	                }
	                if (!mecClickzone.category.isMouseOver) {
	                    mecClickzone.category.isMouseOver = true;
	                    CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_ENTER, mecClickzone.category.mecId, mec.anInt2718);
	                }
	                if (mecClickzone.category.isMouseOver) {
	                    CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_OVER, mecClickzone.category.mecId, mec.anInt2718);
	                }
	            } else if (mecClickzone.category.isMouseOver) {
	                mecClickzone.category.isMouseOver = false;
	                CS2Executor.executeTrigger(ClientTriggerType.MEC_MOUSE_LEAVE, mecClickzone.category.mecId, mec.anInt2718);
	            }
	        }
	    }
	}

}
