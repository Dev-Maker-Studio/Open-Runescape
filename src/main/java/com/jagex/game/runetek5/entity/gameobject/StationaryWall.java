package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.loc.LocShapes;
import com.jagex.game.runetek5.config.loc.LocType;
import com.jagex.game.runetek5.config.loc.LocTypeList;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.DynamicTint;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.client.PreferenceManager;

public class StationaryWall extends AbstractWall implements GameObject {

    public static int anInt10526;
    public static final int[] anIntArray10522 = {1, 2, 4, 8};
    public static final int[] anIntArray10518 = {16, 32, 64, 128};
    public Shadow aCacheableNodeSub1710517;
    public Silhouette axisAlignedBoundingBox;
    public boolean shadowed;
    public final LocTypeList locTypeList;
    public final int locId;
    public final boolean aBool10525;
    public final byte shape;
    public final byte rotation;
    public final boolean interactive;
    public final boolean aBool10516;
    Model model;

    public StationaryWall(Scene scene, AbstractToolkit toolkit, LocTypeList locTypeList, LocType locType,
                          int plane, int collisionPlane, int x, int y, int z, boolean bool10, int shape, int rotation, boolean delayShading) {
        super(scene, x, y, z, plane, collisionPlane, getCullingType(shape, rotation));
        this.locTypeList = locTypeList;
        this.locId = locType.id;
        this.aBool10525 = bool10;
        this.shape = (byte) shape;
        this.rotation = (byte) rotation;
        this.interactive = locType.hasActions != 0 && !bool10;
        this.shadowed = delayShading;
        this.aBool10516 = toolkit.hasShadows() && locType.dynamicShadow && !this.aBool10525 && PreferenceManager.preferences.sceneryShadows.getValue() != 0;
        int i_14 = 2048;
        if (this.shadowed) {
            i_14 |= 0x10000;
        }

        if (locType.dynamicTint) {
            i_14 |= 0x80000;
        }

        TypeResult typeResult = method16101(toolkit, i_14, this.aBool10516);
        if (typeResult != null) {
        	this.model = (Model) typeResult.mainObject;
        	this.aCacheableNodeSub1710517 = (Shadow) typeResult.companionObject;
            if (this.shadowed || locType.dynamicTint) {
            	this.model = this.model.copy((byte) 0, i_14, false);
                if (locType.dynamicTint) {
                    DynamicTint tint = Resource.mapRegion.getDynamicTint();
                    this.model.tint(tint.hue, tint.saturation, tint.lightness, tint.opacity);
                }
            }
        }

        setupBoundingBox(1);
    }
    
    @Override
    public void addShadow(AbstractToolkit toolkit) {
        Shadow shadow;
        if (this.aCacheableNodeSub1710517 == null && this.aBool10516) {
            TypeResult typeResult = method16101(toolkit, 262144, true);
            shadow = (Shadow) (typeResult != null ? typeResult.companionObject : null);
        } else {
            shadow = this.aCacheableNodeSub1710517;
            this.aCacheableNodeSub1710517 = null;
        }

        Vector3 abs = getAbsolutePosition().coords;
        if (shadow != null) {
            super.scene.renderShadows(shadow, super.collisionPlane, (int) abs.x, (int) abs.z, null);
        }

    }
    
    public static int getCullingType(int type, int rotation) {
        return type != LocShapes.WALL_DIAGONAL_CORNER.shape && type != LocShapes.WALL_STRAIGHT_CORNER.shape ? StationaryWall.anIntArray10522[rotation & 0x3] : StationaryWall.anIntArray10518[rotation & 0x3];
    }

    @Override
    public void removeShadow(AbstractToolkit graphicalrenderer1) {
        Shadow class282Sub50Sub174;
        if (this.aCacheableNodeSub1710517 == null && this.aBool10516) {
            TypeResult class4525 = method16101(graphicalrenderer1, 262144, true);
            class282Sub50Sub174 = (Shadow) (class4525 != null ? class4525.companionObject : null);
        } else {
            class282Sub50Sub174 = this.aCacheableNodeSub1710517;
            this.aCacheableNodeSub1710517 = null;
        }

        Vector3 abs = getAbsolutePosition().coords;
        if (class282Sub50Sub174 != null) {
            super.scene.removeShadow(class282Sub50Sub174, super.collisionPlane, (int) abs.x, (int) abs.z, null);
        }

    }

    @Override
    public boolean disregardTextureMovement() {
        return this.model == null || !this.model.hasAnimatedTexture();
    }

    public TypeResult method16101(AbstractToolkit toolkit, int functionMask, boolean hasShadow) {
        LocType locType = this.locTypeList.list(this.locId);
        Ground currentLevel;
        Ground nextLevel;
        if (this.aBool10525) {
            currentLevel = super.scene.underwaterHeightmap[super.collisionPlane];
            nextLevel = super.scene.surfaceHeightmap[0];
        } else {
            currentLevel = super.scene.surfaceHeightmap[super.collisionPlane];
            if (super.collisionPlane < 3) {
                nextLevel = super.scene.surfaceHeightmap[super.collisionPlane + 1];
            } else {
                nextLevel = null;
            }
        }

        Vector3 abs = getAbsolutePosition().coords;
        return locType.getPair(toolkit, functionMask, this.shape, this.rotation, currentLevel, nextLevel, (int) abs.x, (int) abs.y, (int) abs.z, hasShadow, null);
    }

    public Model getModel(AbstractToolkit toolkit, int functionMask) {
        if (this.model != null && toolkit.mismatchingFunctionMask(this.model.getFunctionMask(), functionMask) == 0) {
            return this.model;
        } 
        TypeResult typeResult = method16101(toolkit, functionMask, false);
        return typeResult != null ? (Model) typeResult.mainObject : null;
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        Vector3 abs = getAbsolutePosition().coords;
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate((int) abs.x, (int) abs.y, (int) abs.z, getModel(toolkit, 0));
        }

        return this.axisAlignedBoundingBox;
    }

    @Override
    public void upload() {
        if (this.model != null) {
            this.model.uploadToGpu();
        }
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
    	// NOOP
    }


    @Override
    public boolean hasTileShadow() {
        return this.shadowed;
    }

    @Override
    public void removeTileShadow() {
        this.shadowed = false;
        if (this.model != null) {
            this.model.applyShading(this.model.getFunctionMask() & -65537);
        }

    }

    @Override
    public int getId() {
        return this.locId;
    }

    @Override
    public int getShape() {
        return this.shape;
    }

    @Override
    public int getRotation() {
        return this.rotation;
    }

    @Override
    public boolean hasRenderingShadow() {
        return this.aBool10516;
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        if (this.model == null) {
            return null;
        }
        Matrix43 projectionMatrix = getProjectionMatrix();
        RenderedEntity renderedEntity = RenderedEntityCache.generate(this.interactive);
        this.model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        return renderedEntity;
    }

    @Override
    public void castShadow(SceneGraphEntity sceneGraphEntity, int x, int y, int z, boolean renderTypeAdjustment) {
        if (sceneGraphEntity instanceof StationaryWall stationaryWall) {
            if (this.model != null && stationaryWall.model != null) {
                this.model.applyShading(stationaryWall.model, x, y, z, renderTypeAdjustment);
            }
        } else if (sceneGraphEntity instanceof StationaryInteractiveObject stationaryInteractiveObject) {
            if (this.model != null && stationaryInteractiveObject.interactiveModel != null) {
                this.model.applyShading(stationaryInteractiveObject.interactiveModel, x, y, z, renderTypeAdjustment);
            }
        }
    }

    @Override
    public boolean hasAlpha() {
        return this.model != null && this.model.accountForLayers();
    }

    @Override
    public int getMinY() {
        return this.model != null ? this.model.getMinY() : 0;
    }


    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int cameraX, int cameraY) {
        Model model = getModel(toolkit, PROJECTION);
        if (model != null) {
            Matrix43 projectionMatrix = getProjectionMatrix();
            return model.calculateProjection(cameraX, cameraY, projectionMatrix, false, 0);
        } 
        return false;
    }
}
