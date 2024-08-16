package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public abstract class AbstractWall extends SceneGraphEntity {

    public static final int[] SETTING_MASKS = {19, 55, 38, 155, 255, 110, 137, 205, 76};
    public final short cullingType;

    protected AbstractWall(Scene scene, int x, int y, int z, int plane, int collionPlane, int cullingType) {
        super(scene);
        this.plane = (byte) plane;
        this.collisionPlane = (byte) collionPlane;
        this.cullingType = (short) cullingType;
        setBasePosition(new Vector3(x, y, z));
    }

    @Override
    public boolean isSoftHidden(AbstractToolkit toolkit) {
        Vector3 absolute = getAbsolutePosition().coords;
        return super.scene.culling.hideWall(this, super.collisionPlane, (int) absolute.x >> super.scene.tileUnits, (int) absolute.z >> super.scene.tileUnits);
    }

    @Override
    public boolean canRender() {
        Vector3 absolute = getAbsolutePosition().coords;
        return super.scene.adjacent_tile_on_screen[((int) absolute.x >> super.scene.tileUnits) - super.scene.cameraGridPositionX + super.scene.visibleTiles][((int) absolute.z >> super.scene.tileUnits) - super.scene.cameraGridPositionY + super.scene.visibleTiles];
    }

    @Override
    public int getActiveLights(PointLight[] cacheInto) {
        Vector3 absolute = getAbsolutePosition().coords;
        int posX = (int) absolute.x >> super.scene.tileUnits;
        int posY = (int) absolute.z >> super.scene.tileUnits;
        int maskIndex = 0;
        if (super.scene.cameraGridPositionX == posX) {
            maskIndex++;
        } else if (super.scene.cameraGridPositionX < posX) {
            maskIndex += 2;
        }

        if (super.scene.cameraGridPositionY == posY) {
            maskIndex += 3;
        } else if (super.scene.cameraGridPositionY > posY) {
            maskIndex += 6;
        }

        int mask = SETTING_MASKS[maskIndex];
        if ((this.cullingType & mask) != 0) {
            return getActiveLights(posX, posY, cacheInto);
        } else if (this.cullingType == 1 && posX > 0) {
            return getActiveLights(posX - 1, posY, cacheInto);
        } else if (this.cullingType == 4 && posX <= super.scene.sceneDimensionX) {
            return getActiveLights(posX + 1, posY, cacheInto);
        } else if (this.cullingType == 8 && posY > 0) {
            return getActiveLights(posX, posY - 1, cacheInto);
        } else if (this.cullingType == 2 && posY <= super.scene.sceneDimensionY) {
            return getActiveLights(posX, posY + 1, cacheInto);
        } else if (this.cullingType == 16 && posX > 0 && posY <= super.scene.sceneDimensionY) {
            return getActiveLights(posX - 1, posY + 1, cacheInto);
        } else if (this.cullingType == 32 && posX <= super.scene.sceneDimensionX && posY <= super.scene.sceneDimensionY) {
            return getActiveLights(posX + 1, posY + 1, cacheInto);
        } else if (this.cullingType == 128 && posX > 0 && posY > 0) {
            return getActiveLights(posX - 1, posY - 1, cacheInto);
        } else if (this.cullingType == 64 && posX <= super.scene.sceneDimensionX && posY > 0) {
            return getActiveLights(posX + 1, posY - 1, cacheInto);
        } else {
            throw new RuntimeException("");
        }
    }
}
