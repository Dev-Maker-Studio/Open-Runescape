package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public abstract class AbstractWallDecoration extends SceneGraphEntity {

	public short positionX;

	public short positionY;

    protected AbstractWallDecoration(Scene scene, int x, int y, int z, int plane, int collisionPlane, int positionX, int positionY) {
        super(scene);
        this.plane = (byte) plane;
        this.collisionPlane = (byte) collisionPlane;
        this.positionX = (short) positionX;
        this.positionY = (short) positionY;
        this.setBasePosition(new Vector3(x, y, z));
    }

    @Override
    public void removeTileShadow() {
        throw new IllegalStateException();
    }

    @Override
    public int getActiveLights(PointLight[] lights) {
        Vector3 absolutePosition = getAbsolutePosition().coords;
        return getActiveLights((int) absolutePosition.x >> super.scene.tileUnits, (int) absolutePosition.z >> super.scene.tileUnits, lights);
    }

    @Override
    public boolean canRender() {
        Vector3 abs = getAbsolutePosition().coords;
        return super.scene.adjacent_tile_on_screen
        		[((int) abs.x >> super.scene.tileUnits) - super.scene.cameraGridPositionX + super.scene.visibleTiles]
        		[((int) abs.z >> super.scene.tileUnits) - super.scene.cameraGridPositionY + super.scene.visibleTiles];
    }

    @Override
    public void castShadow(SceneGraphEntity node, int width, int length, int height, boolean bool6) {
        throw new IllegalStateException();
    }

    @Override
    public boolean isSoftHidden(AbstractToolkit toolkit) {
        Vector3 abs = getAbsolutePosition().coords;
        return super.scene.culling.isTileHidden(super.collisionPlane, (int) abs.x >> super.scene.tileUnits, (int) abs.z >> super.scene.tileUnits, getMinY());
    }


    @Override
    public boolean hasTileShadow() {
        return false;
    }
}
