package com.jagex.game.runetek5.trigger.rsinterface;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.movement.CoordGrid;

public class IFSubObjectPosition {

    public final CoordGrid position;

    public final int objectIndex;

    public final int gameobjectId;

    public IFSubObjectPosition(CoordGrid position, int objectIndex, int gameobjectId) {
        this.position = position;
        this.objectIndex = objectIndex;
        this.gameobjectId = gameobjectId;
    }

    public int getObjectType() {
        return client.OBJECT_TYPE_SLOTS[this.objectIndex];
    }

    public GameObject getGameobjectVerifyId() {
        GameObject gameObject = getGameobject();
        return gameObject == null ? null : (gameObject.getId() != this.gameobjectId ? null : gameObject);
    }

    public GameObject getGameobject() {
        int z = this.position.plane;
        CoordGrid base = Resource.mapRegion.getBaseCoordinates();
        int x = this.position.x - base.x;
        int y = this.position.y - base.y;
        if (x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
            switch (getObjectType()) {
                case 0:
                    return (GameObject) Resource.mapRegion.getScene().getWall(z, x, y);
                case 1:
                    return (GameObject) Resource.mapRegion.getScene().getPrimaryWallDecoration(z, x, y);
                case 2:
                    return (GameObject) Resource.mapRegion.getScene().getInteractiveObject(z, x, y, client.INTERACTIVE_OBJECT_COMPARABLE);
                case 3:
                    return (GameObject) Resource.mapRegion.getScene().getGroundDecoration(z, x, y);
                default:
                    return null;
            }
        } 
        return null;
    }
}
