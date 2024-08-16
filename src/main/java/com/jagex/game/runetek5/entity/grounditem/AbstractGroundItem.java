package com.jagex.game.runetek5.entity.grounditem;

import com.jagex.game.runetek5.entity.InteractiveEntityContainer;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public abstract class AbstractGroundItem extends SceneGraphEntity {

    protected AbstractGroundItem(Scene scene, int x, int y, int z, int plane, int collisionPlane) {
        super(scene);
        this.plane = (byte) plane;
        this.collisionPlane = (byte) collisionPlane;
        this.setBasePosition(new Vector3(x, y, z));
    }

    @Override
    public boolean canRender() {
        Vector3 absolute = getAbsolutePosition().coords;
        return super.scene.adjacent_tile_on_screen
			[((int) absolute.x >> super.scene.tileUnits) - super.scene.cameraGridPositionX + super.scene.visibleTiles]
			[((int) absolute.z >> super.scene.tileUnits) - super.scene.cameraGridPositionY + super.scene.visibleTiles];
    }

    @Override
    public boolean hasTileShadow() {
        return false;
    }

    @Override
    public void castShadow(SceneGraphEntity class521Sub12, int i_3, int i_4, int i_5, boolean bool6) {
        throw new IllegalStateException();
    }

    @Override
    public void removeTileShadow() {
        throw new IllegalStateException();
    }


    @Override
    public int getActiveLights(PointLight[] lightNodes) {
        Vector3 absolute = getAbsolutePosition().coords;
        return getActiveLights((int) absolute.x >> super.scene.tileUnits, (int) absolute.z >> super.scene.tileUnits, lightNodes);
    }


    @Override
    public boolean isSoftHidden(AbstractToolkit toolkit) {
        Vector3 absolute = getAbsolutePosition().coords;
        InteractiveEntityContainer interactives = super.scene.getInteractiveObjects(super.plane, (int) absolute.x >> super.scene.tileUnits, (int) absolute.z >> super.scene.tileUnits);
        return interactives != null && interactives.interactiveObject.supportsItems ?
        		super.scene.culling.isTileHidden(super.plane, (int) absolute.x >> super.scene.tileUnits, (int) absolute.z >> super.scene.tileUnits, interactives.interactiveObject.getMinY() + getMinY()) :
        			super.scene.culling.isTileHidden(super.plane, (int) absolute.x >> super.scene.tileUnits, (int) absolute.z >> super.scene.tileUnits);
    }
}
