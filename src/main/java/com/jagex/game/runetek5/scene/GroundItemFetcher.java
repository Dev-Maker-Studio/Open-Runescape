package com.jagex.game.runetek5.scene;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.grounditem.GroundItem;
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode;
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper;
import com.jagex.game.runetek5.movement.CoordGrid;

public class GroundItemFetcher {

    public final CoordGrid itemPosition;

    public final int groundItemId;

    public GroundItemFetcher(CoordGrid groundItemPosition, int groundItemId) {
        this.itemPosition = groundItemPosition;
        this.groundItemId = groundItemId;
    }

    public GroundItem getGroundItem() {
        GroundItemWrapper wrapper = client.GROUND_ITEMS.get((long) this.itemPosition.plane << 28 | (long) this.itemPosition.y << 14 | this.itemPosition.x);
        if (wrapper != null) {
            CoordGrid coords = Resource.mapRegion.getBaseCoordinates();
            int sceneX = this.itemPosition.x - coords.x;
            int sceneY = this.itemPosition.y - coords.y;
            if (sceneX >= 0 && sceneY >= 0 && sceneX < Resource.mapRegion.getSceneSizeX() && sceneY < Resource.mapRegion.getSceneSizeY()) {
                for (GroundItemNode node = (GroundItemNode) wrapper.groundItems.getFirst(); node != null; node = (GroundItemNode) wrapper.groundItems.next()) {
                    if (node.id == this.groundItemId) {
                        return (GroundItem) Resource.mapRegion.getScene().getGroundItem(this.itemPosition.plane, sceneX, sceneY);
                    }
                }
            }
        }
        return null;
    }
}
