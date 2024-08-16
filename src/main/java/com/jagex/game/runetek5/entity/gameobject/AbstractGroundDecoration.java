package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public abstract class AbstractGroundDecoration extends SceneGraphEntity {

    public final short floorOffsetY;

    protected AbstractGroundDecoration(Scene scene, int x, int y, int z, int plane, int collisionPlane, int floorOffsetY) {
        super(scene);
        this.plane = (byte) plane;
        this.collisionPlane = (byte) collisionPlane;
        this.floorOffsetY = (short) floorOffsetY;
        setBasePosition(new Vector3(x, y, z));
    }

    @Override
    public int getActiveLights(PointLight[] lights) {
        Vector3 absolutePosition = getAbsolutePosition().coords;
        return getActiveLights((int) absolutePosition.x >> super.scene.tileUnits, (int) absolutePosition.z >> super.scene.tileUnits, lights);
    }


    @Override
    public boolean isSoftHidden(AbstractToolkit toolkit) {
        Vector3 absolutePosition = getAbsolutePosition().coords;
        return super.scene.culling.isTileHidden(super.collisionPlane, (int) absolutePosition.x >> super.scene.tileUnits, (int) absolutePosition.z >> super.scene.tileUnits);
    }

    @Override
    public boolean canRender() {
        Vector3 absolutePosition = getAbsolutePosition().coords;
        return super.scene.adjacent_tile_on_screen
        		[((int) absolutePosition.x >> super.scene.tileUnits) - super.scene.cameraGridPositionX + super.scene.visibleTiles]
        		[((int) absolutePosition.z >> super.scene.tileUnits) - super.scene.cameraGridPositionY + super.scene.visibleTiles];
    }
}