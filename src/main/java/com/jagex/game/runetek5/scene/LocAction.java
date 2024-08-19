package com.jagex.game.runetek5.scene;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.object.ObjectTypeCustomisation;
import com.jagex.game.runetek5.entity.gameobject.GameObject;

public class LocAction extends Node {

    public static NodeCollection pendingRemovals = new NodeCollection();
    public static NodeCollection pendingCustomizations = new NodeCollection();
    public int x;
    public int y;
    public int plane;
    public int srcId;
    public int gameObjectType;
    public int srcRotation;
    public ObjectTypeCustomisation dstCustomization;
    public int dstId;
    public int srcShape;
    public int dstShape;
    public int dstRotation;
    public boolean queued = true;
    public boolean inUpdateZone;
    
	public static void transformObjects() {
	    LocAction locAction;
	    for (locAction = (LocAction) pendingRemovals.getFirst(); locAction != null; locAction = (LocAction) pendingRemovals.next()) {
	        MapRegion.applyGameobjectAction(locAction, false);
	    }
	
	    for (locAction = (LocAction) pendingCustomizations.getFirst(); locAction != null; locAction = (LocAction) pendingCustomizations.next()) {
	        MapRegion.applyGameobjectAction(locAction, true);
	    }
	}
    
    public static void onRegionChanged() {
        for (LocAction locAction = (LocAction) pendingRemovals.getFirst(); locAction != null; locAction = (LocAction) pendingRemovals.next()) {
            if (!locAction.inUpdateZone) {
                locAction.queued = true;
                if (locAction.x >= 0 && locAction.y >= 0 && locAction.x < Resource.mapRegion.getSceneSizeX() && locAction.y < Resource.mapRegion.getSceneSizeY()) {
                    LocAction.fetchGameobject(locAction);
                }
            } else {
                locAction.unlink();
            }
        }

        for (LocAction locAction = (LocAction) pendingCustomizations.getFirst(); locAction != null; locAction = (LocAction) pendingCustomizations.next()) {
            if (!locAction.inUpdateZone) {
                locAction.queued = true;
            } else {
                locAction.unlink();
            }
        }
    }

    public static void destroyObject(int plane, int x, int y, int type, int id, int shape, int rotation) {
        LocAction locAction = null;
        for (LocAction cachedLoc = (LocAction) pendingRemovals.getFirst(); cachedLoc != null; cachedLoc = (LocAction) pendingRemovals.next()) {
            if (plane == cachedLoc.plane && cachedLoc.x == x && y == cachedLoc.y && type == cachedLoc.gameObjectType) {
                locAction = cachedLoc;
                break;
            }
        }
        if (locAction == null) {
            locAction = new LocAction();
            locAction.plane = plane;
            locAction.gameObjectType = type;
            locAction.x = x;
            locAction.y = y;
            if (x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                LocAction.fetchGameobject(locAction);
            }
            pendingRemovals.addFirst(locAction);
        }
        locAction.dstId = id;
        locAction.dstShape = shape;
        locAction.dstRotation = rotation;
        locAction.queued = true;
        locAction.inUpdateZone = false;
    }

    public static void customizeObject(int plane, int x, int y, int type, int locId, int locShape, ObjectTypeCustomisation locCustomization) {
        LocAction locAction = null;
        for (LocAction customization = (LocAction) pendingCustomizations.getFirst(); customization != null; customization = (LocAction) pendingCustomizations.next()) {
            if (plane == customization.plane && customization.x == x && y == customization.y && type == customization.gameObjectType) {
                locAction = customization;
                break;
            }
        }
        if (locAction == null) {
            locAction = new LocAction();
            locAction.plane = plane;
            locAction.gameObjectType = type;
            locAction.x = x;
            locAction.y = y;
            pendingCustomizations.addFirst(locAction);
        }
        locAction.dstId = locId;
        locAction.dstShape = locShape;
        locAction.dstCustomization = locCustomization;
        locAction.queued = true;
        locAction.inUpdateZone = false;
    }

	public static void fetchGameobject(LocAction action) {
	    Scene scene = Resource.mapRegion.getScene();
	    if (scene != null) {
	        GameObject gameObject = null;
	        if (action.gameObjectType == 0) {
	            gameObject = (GameObject) scene.getWall(action.plane, action.x, action.y);
	        }
	        if (action.gameObjectType == 1) {
	            gameObject = (GameObject) scene.getPrimaryWallDecoration(action.plane, action.x, action.y);
	        }
	        if (action.gameObjectType == 2) {
	            gameObject = (GameObject) scene.getInteractiveObject(action.plane, action.x, action.y, client.INTERACTIVE_OBJECT_COMPARABLE);
	        }
	        if (action.gameObjectType == 3) {
	            gameObject = (GameObject) scene.getGroundDecoration(action.plane, action.x, action.y);
	        }
	        if (gameObject != null) {
	            action.srcId = gameObject.getId();
	            action.srcShape = gameObject.getShape();
	            action.srcRotation = gameObject.getRotation();
	        } else {
	            action.srcId = -1;
	            action.srcShape = 0;
	            action.srcRotation = 0;
	        }
	    }
	}
}