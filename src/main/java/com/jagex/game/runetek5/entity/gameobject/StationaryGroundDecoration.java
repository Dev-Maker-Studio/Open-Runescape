package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
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

public class StationaryGroundDecoration extends AbstractGroundDecoration implements GameObject {
    public Shadow primaryShadow;
    public Silhouette axisAlignedBoundingBox;
    public boolean hasTileShadow;
    public final ObjectTypeList locTypeLst;
    public final int id;
    public final boolean underwater;
    public final byte currentRotation;
    public final boolean interactive;
    public final boolean hasShadow;
    public Model model;

    public StationaryGroundDecoration(Scene scene, AbstractToolkit toolkit, ObjectTypeList locTypeList, ObjectType loctype, int i_5, int i_6, int i_7, int i_8, int i_9, boolean underwater, int rotation, boolean hasShadow) {
        super(scene, i_7, i_8, i_9, i_5, i_6, loctype.groundDecorationHeight);
        this.locTypeLst = locTypeList;
        this.id = loctype.id;
        this.underwater = underwater;
        this.currentRotation = (byte) rotation;
        this.interactive = loctype.hasActions != 0 && !underwater;
        this.hasTileShadow = hasShadow;
        this.hasShadow = toolkit.hasShadows() && loctype.dynamicShadow && !this.underwater && PreferenceManager.preferences.sceneryShadows.getValue() != 0;
        int functionMask = HAS_TEXTURE_COORDS;
        if (this.hasTileShadow) {
            functionMask |= SHADED;
        }
        if (loctype.dynamicTint) {
            functionMask |= TINT;
        }
        TypeResult typeResult = getLocresult(toolkit, functionMask, hasShadow);
        if (typeResult != null) {
        	this.model = (Model) typeResult.mainObject;
        	this.primaryShadow = (Shadow) typeResult.companionObject;
            if (this.hasTileShadow || loctype.dynamicTint) {
            	this.model = this.model.copy((byte) 0, functionMask, false);
                if (loctype.dynamicTint) {
                    DynamicTint dynamicTint = Resource.mapRegion.getDynamicTint();
                    this.model.tint(dynamicTint.hue, dynamicTint.saturation, dynamicTint.lightness, dynamicTint.opacity);
                }
            }
        }
        setupBoundingBox(1);
    }
    
    @Override
    public void addShadow(AbstractToolkit toolkit) {
        Shadow shadow;
        if (this.primaryShadow == null && this.hasShadow) {
            TypeResult typeResult = getLocresult(toolkit, 262144, true);
            shadow = (Shadow) (typeResult != null ? typeResult.companionObject : null);
        } else {
            shadow = this.primaryShadow;
            this.primaryShadow = null;
        }
        Vector3 position = getAbsolutePosition().coords;
        if (shadow != null) {
            super.scene.renderShadows(shadow, super.collisionPlane, (int) position.x, (int) position.z, null);
        }
    }

    @Override
    public int getMinY() {
        return this.model != null ? this.model.getMinY() : 0;
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        if (this.model == null) {
            return null;
        } 
        Matrix43 projectionMatrix = getProjectionMatrix();
        RenderedEntity drawnModel = RenderedEntityCache.generate(this.interactive);
        this.model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        return drawnModel;
    }

    public TypeResult getLocresult(AbstractToolkit toolkit, int functionMask, boolean hasShadow) {
        ObjectType loctype = this.locTypeLst.list(this.id);
        Ground heightsCurrentLvl;
        Ground heightsNextLvl;
        if (this.underwater) {
            heightsCurrentLvl = super.scene.underwaterHeightmap[super.collisionPlane];
            heightsNextLvl = super.scene.surfaceHeightmap[0];
        } else {
            heightsCurrentLvl = super.scene.surfaceHeightmap[super.collisionPlane];
            if (super.collisionPlane < 3) {
                heightsNextLvl = super.scene.surfaceHeightmap[super.collisionPlane + 1];
            } else {
                heightsNextLvl = null;
            }
        }
        Vector3 absolute = getAbsolutePosition().coords;
        return loctype.getPair(toolkit, functionMask, ObjectShapes.GROUND_DECORATION.shape, this.currentRotation, heightsCurrentLvl, heightsNextLvl, (int) absolute.x, (int) absolute.y, (int) absolute.z, hasShadow, null);
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        Vector3 abs = getAbsolutePosition().coords;
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate((int) abs.x, (int) abs.y, (int) abs.z, adjustToFunctionmask(toolkit, 0));
        }
        return this.axisAlignedBoundingBox;
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
    }

    @Override
    public void removeShadow(AbstractToolkit toolkit) {
        Shadow shadow;
        if (this.primaryShadow == null && this.hasShadow) {
            TypeResult locresult = getLocresult(toolkit, 262144, true);
            shadow = (Shadow) (locresult != null ? locresult.companionObject : null);
        } else {
            shadow = this.primaryShadow;
            this.primaryShadow = null;
        }
        Vector3 absolute = getAbsolutePosition().coords;
        if (shadow != null) {
            super.scene.removeShadow(shadow, super.collisionPlane, (int) absolute.x, (int) absolute.z, null);
        }
    }

    @Override
    public void castShadow(SceneGraphEntity renderedGraphNode, int i_3, int i_4, int i_5, boolean bool6) {
        if (renderedGraphNode instanceof StationaryGroundDecoration groundDecoration) {
            if (this.model != null && groundDecoration.model != null) {
                this.model.applyShading(groundDecoration.model, i_3, i_4, i_5, bool6);
            }
        }
    }

    @Override
    public void removeTileShadow() {
        this.hasTileShadow = false;
        if (this.model != null) {
            this.model.applyShading(this.model.getFunctionMask() & -65537);
        }
    }

    @Override
    public int getId() {
        return this.id;
    }

    public Model adjustToFunctionmask(AbstractToolkit toolkit, int functionMask) {
        if (this.model != null && toolkit.mismatchingFunctionMask(this.model.getFunctionMask(), functionMask) == 0) {
            return this.model;
        } 
        TypeResult locresult = getLocresult(toolkit, functionMask, false);
        return locresult != null ? (Model) locresult.mainObject : null;
    }

    @Override
    public int getRotation() {
        return this.currentRotation;
    }

    @Override
    public boolean disregardTextureMovement() {
        return this.model == null || !this.model.hasAnimatedTexture();
    }

    @Override
    public boolean hasTileShadow() {
        return this.hasTileShadow;
    }

    @Override
    public boolean hasRenderingShadow() {
        return this.hasShadow;
    }

    @Override
    public void upload() {
        if (this.model != null) {
            this.model.uploadToGpu();
        }
    }

    @Override
    public boolean hasAlpha() {
        return this.model != null && this.model.accountForLayers();
    }

    @Override
    public int getShape() {
        return ObjectShapes.GROUND_DECORATION.shape;
    }


    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int i_2, int i_3) {
        Model model = adjustToFunctionmask(toolkit, 131072);
        if (model != null) {
            Matrix43 projectionMatrix = getProjectionMatrix();
            return model.calculateProjection(i_2, i_3, projectionMatrix, false, 0);
        } 
        return false;
    }
}
