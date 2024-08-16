package com.jagex.game.runetek5.entity.grounditem;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.obj.ObjType;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.RegionLoadingType;

public class GroundItemUpdating {

	public static void addGroundItem(int z, int x, int y, GroundItemNode addedGroundItem) {
	    long uid = (long) z << 28 | (long) y << 14 | x;
	    GroundItemWrapper groundItemWrapper = client.GROUND_ITEMS.get(uid);
	    if (groundItemWrapper == null) {
	        groundItemWrapper = new GroundItemWrapper();
	        client.GROUND_ITEMS.put(groundItemWrapper, uid);
	    } else {
	        ObjType addedObjType = TypeLists.OBJ.list(addedGroundItem.id);
	        int addedItemValue = addedObjType.value;
	        if (addedObjType.stackable == 1) {
	            addedItemValue *= addedGroundItem.amount + 1;
	        }
	        for (GroundItemNode existingGroundItem = (GroundItemNode) groundItemWrapper.groundItems.getFirst(); existingGroundItem != null; existingGroundItem = (GroundItemNode) groundItemWrapper.groundItems.next()) {
	            ObjType existingObjType = TypeLists.OBJ.list(existingGroundItem.id);
	            int existingItemValue = existingObjType.value;
	            if (existingObjType.stackable == 1) {
	                existingItemValue *= existingGroundItem.amount + 1;
	            }
	            if (addedItemValue > existingItemValue) {
	                Node.replace(addedGroundItem, existingGroundItem);
	                return;
	            }
	        }
	    }
	    groundItemWrapper.groundItems.addFirst(addedGroundItem);
	}
	
	/**
	 * Could use cleanup including naming
	 */
	public static void removeGroundItem(int plane, int x, int y) {
	    CoordGrid base = Resource.mapRegion.getBaseCoordinates();
	    int absX = base.x + x;
	    int absY = base.y + y;
	    if (Resource.mapRegion.getScene() != null && Resource.mapRegion.getLoadingType() != RegionLoadingType.CUTSCENE && x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
	        long hash = (long) plane << 28 | (long) absY << 14 | absX;
	        GroundItemWrapper node = client.GROUND_ITEMS.get(hash);
	        if (node == null) {
	            Resource.mapRegion.getScene().removeGroundItem(plane, x, y);
	        } else {
	            GroundItemNode groundItemNod = (GroundItemNode) node.groundItems.getFirst();
	            if (groundItemNod == null) {
	                Resource.mapRegion.getScene().removeGroundItem(plane, x, y);
	            } else {
	                GroundItem groundItem = (GroundItem) Resource.mapRegion.getScene().removeGroundItem(plane, x, y);
	                if (groundItem == null) {
	                    groundItem = new GroundItem(Resource.mapRegion.getScene(), x << 9, Resource.mapRegion.getScene().localHeightmap[plane].getHeight(x, y), y << 9, plane, plane);
	                } else {
	                    groundItem.itemId = -1;
	                    groundItem.lastId = -1;
	                    groundItem.scene = Resource.mapRegion.getScene();
	                }
	
	                groundItem.nodeId = groundItemNod.id;
	                groundItem.nodeAmount = groundItemNod.amount;
	
	                label44:
	                while (true) {
	                    GroundItemNode groundItemNode = (GroundItemNode) node.groundItems.next();
	                    if (groundItemNode == null) {
	                        break;
	                    }
	
	                    if (groundItemNode.id != groundItem.nodeId) {
	                        groundItem.lastId = groundItemNode.id;
	                        groundItem.lastAmount = groundItemNode.amount;
	
	                        while (true) {
	                            GroundItemNode class282Sub3413 = (GroundItemNode) node.groundItems.next();
	                            if (class282Sub3413 == null) {
	                                break label44;
	                            }
	
	                            if (class282Sub3413.id != groundItem.nodeId && groundItem.lastId != class282Sub3413.id) {
	                                groundItem.itemId = class282Sub3413.id;
	                                groundItem.amount = class282Sub3413.amount;
	                            }
	                        }
	                    }
	                }
	
	                int height = MapRegion.tileheightForAbsolutePosition((x << 9) + 256, (y << 9) + 256, plane);
	                groundItem.setBasePosition((x << 9), height, (y << 9));
	                groundItem.anInt10422 = 0;
	                groundItem.plane = (byte) plane;
	                groundItem.collisionPlane = (byte) plane;
	                if (Resource.mapRegion.getSettingsBits().areRoofsHidden(x, y)) {
	                    groundItem.collisionPlane += 1;
	                }
	
	                Resource.mapRegion.getScene().addGroundItem(plane, x, y, height, groundItem);
	            }
	        }
	    }	
	}
}