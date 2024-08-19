package com.jagex.game.runetek5.entity.gameobject;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.entity.RenderedEntity;
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

public class StationaryWallDecoration extends AbstractWallDecoration implements GameObject {

    public final ObjectTypeList locTypeList;
    public final int locTypeId;
    public final boolean underwater;
    public final byte rotation;
    public final byte objectType;
    public final boolean interactive;
    public final boolean shadowed;
    public Model model;
    public Shadow shadow;
    public Silhouette axisAlignedBoundingBox;

    public StationaryWallDecoration(Scene scene, AbstractToolkit toolkit, ObjectTypeList loctypelist, ObjectType locType, int i_5, int i_6, int i_7, int i_8, int i_9, boolean belowWater, int i_11, int i_12, int type, int rotation) {
        super(scene, i_7, i_8, i_9, i_5, i_6, i_11, i_12);
        this.locTypeList = loctypelist;
        this.locTypeId = locType.id;
        this.underwater = belowWater;
        this.rotation = (byte) rotation;
        this.objectType = (byte) type;
        this.interactive = locType.hasActions != 0 && !belowWater;
        this.shadowed = toolkit.hasShadows() && locType.dynamicShadow && !this.underwater && PreferenceManager.preferences.sceneryShadows.getValue() != 0;
        int functionMask = 2048;
        if (locType.dynamicTint) {
            functionMask |= TINT;
        }
        TypeResult locresult = getLocResult(toolkit, functionMask, this.shadowed);
        if (locresult != null) {
        	this.model = (Model) locresult.mainObject;
        	this.shadow = (Shadow) locresult.companionObject;
            if (locType.dynamicTint) {
            	this.model = this.model.copy((byte) 0, functionMask, false);
                if (locType.dynamicTint) {
                    DynamicTint dynamicTint = Resource.mapRegion.getDynamicTint();
                    this.model.tint(dynamicTint.hue, dynamicTint.saturation, dynamicTint.lightness, dynamicTint.opacity);
                }
            }
        }
        this.setupBoundingBox(1);
    }
    

    @Override
    public void addShadow(AbstractToolkit toolkit) {
        Shadow locresultShadow;
        if (this.shadow == null && this.shadowed) {
            TypeResult locresult = getLocResult(toolkit, 262144, true);
            locresultShadow = (Shadow) (locresult != null ? locresult.companionObject : null);
        } else {
            locresultShadow = this.shadow;
            this.shadow = null;
        }
        Vector3 absolutePosition = getAbsolutePosition().coords;
        if (locresultShadow != null) {
            super.scene.renderShadows(locresultShadow, super.collisionPlane, (int) absolutePosition.x, (int) absolutePosition.z, null);
        }
    }


    @Override
    public boolean disregardTextureMovement() {
        return this.model == null || !this.model.hasAnimatedTexture();
    }

    @Override
    public boolean hasAlpha() {
        return this.model != null && this.model.accountForLayers();
    }

    @Override
    public int getMinY() {
        return this.model != null ? this.model.getMinY() : 0;
    }

    public Model getAdjustedModel(AbstractToolkit toolkit, int functionMask) {
        if (this.model != null && toolkit.mismatchingFunctionMask(this.model.getFunctionMask(), functionMask) == 0) {
            return this.model;
        } 
        TypeResult locresult = getLocResult(toolkit, functionMask, false);
        return locresult != null ? (Model) locresult.mainObject : null;
    }

    public TypeResult getLocResult(AbstractToolkit toolkit, int functionMask, boolean hasShadow) {
        ObjectType locType = this.locTypeList.list(this.locTypeId);
        Ground groundCurrentLvl;
        Ground groundNextLvl;
        if (this.underwater) {
            groundCurrentLvl = super.scene.underwaterHeightmap[super.collisionPlane];
            groundNextLvl = super.scene.surfaceHeightmap[0];
        } else {
            groundCurrentLvl = super.scene.surfaceHeightmap[super.collisionPlane];
            if (super.collisionPlane < 3) {
                groundNextLvl = super.scene.surfaceHeightmap[super.collisionPlane + 1];
            } else {
                groundNextLvl = null;
            }
        }
        Vector3 absolute = getAbsolutePosition().coords;
        return locType.getPair(toolkit, functionMask, this.objectType, this.rotation, groundCurrentLvl, groundNextLvl, (int) absolute.x, (int) absolute.y, (int) absolute.z, hasShadow, null);
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        if (this.model == null) {
            return null;
        } else {
            Matrix43 matrix = toolkit.getRenderingMatrix();
            matrix.copyFrom(getProjectionMatrix());
            matrix.translate(super.positionX, 0.0F, super.positionY);
            RenderedEntity renderedEntity = RenderedEntityCache.generate(this.interactive);
            this.model.drawMatrixThreaded(matrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
            return renderedEntity;
        }
    }

    @Override
    public void particleCycle(AbstractToolkit graphicalrenderer1) {
    }

    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int i_2, int i_3) {
        Model adjustedModel = getAdjustedModel(toolkit, 131072);
        if (adjustedModel != null) {
            Matrix43 matrix = getProjectionMatrix();
            return adjustedModel.calculateProjection(i_2, i_3, matrix, false, 0);
        }
        return false;
    }


    @Override
    public int getRotation() {
        return this.rotation;
    }

    @Override
    public void upload() {
        if (this.model != null) {
            this.model.uploadToGpu();
        }
    }


    @Override
    public void removeShadow(AbstractToolkit graphicalrenderer1) {
        Shadow locresultShadow;
        if (this.shadow == null && this.shadowed) {
            TypeResult locresult = getLocResult(graphicalrenderer1, 262144, true);
            locresultShadow = (Shadow) (locresult != null ? locresult.companionObject : null);
        } else {
            locresultShadow = this.shadow;
            this.shadow = null;
        }

        Vector3 absolutePosition = getAbsolutePosition().coords;
        if (locresultShadow != null) {
            super.scene.removeShadow(locresultShadow, super.collisionPlane, (int) absolutePosition.x, (int) absolutePosition.z, null);
        }
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit graphicalrenderer1) {
        Vector3 vector33 = getAbsolutePosition().coords;
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate((int) vector33.x, (int) vector33.y, (int) vector33.z, getAdjustedModel(graphicalrenderer1, 0));
        }

        return this.axisAlignedBoundingBox;
    }


    @Override
    public int getId() {
        return this.locTypeId;
    }


    @Override
    public boolean hasRenderingShadow() {
        return this.shadowed;
    }

    @Override
    public int getShape() {
        return this.objectType;
    }
}
