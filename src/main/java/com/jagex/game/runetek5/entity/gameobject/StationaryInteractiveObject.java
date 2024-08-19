package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
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

public class StationaryInteractiveObject extends AbstractInteractiveEntity implements GameObject {

    public Silhouette axisAlignedBoundingBox;

    public final ObjectTypeList aClass47410542;

    public final int id;

    public final boolean aBool10536;

    public final byte aByte10539;

    public final byte aByte10540;

    public final boolean aBool10541;

    public boolean aBool10543;

    public final boolean aBool10544;

    Model interactiveModel;

    public Shadow currentShadow;

    public StationaryInteractiveObject(Scene scene, AbstractToolkit toolkit, ObjectTypeList locTypeList, ObjectType objectdefinitions4, int i_5, int i_6, int i_7, int i_8, int i_9, boolean bool10, int i_11, int i_12, int i_13, int i_14, int i_15, int i_16, boolean bool17) {
        super(scene, i_5, i_6, i_7, i_8, i_9, i_11, i_12, i_13, i_14, objectdefinitions4.supportsItems == 1, ObjectShapes.method7853(i_15, i_16));
        this.aClass47410542 = locTypeList;
        this.id = objectdefinitions4.id;
        super.collisionPlane = (byte) i_6;
        this.aBool10536 = bool10;
        this.aByte10539 = (byte) i_15;
        this.aByte10540 = (byte) i_16;
        this.aBool10541 = objectdefinitions4.hasActions != 0 && !bool10;
        this.aBool10543 = bool17;
        this.aBool10544 = toolkit.hasShadows() && objectdefinitions4.dynamicShadow && !this.aBool10536 && PreferenceManager.preferences.sceneryShadows.getValue() != 0;
        int i_18 = 2048;
        if (this.aBool10543) {
            i_18 |= 0x10000;
        }
        if (objectdefinitions4.dynamicTint) {
            i_18 |= 0x80000;
        }
        TypeResult class45219 = method16117(toolkit, i_18, this.aBool10544);
        if (class45219 != null) {
            this.interactiveModel = (Model) class45219.mainObject;
            this.currentShadow = (Shadow) class45219.companionObject;
            if (this.aBool10543 || objectdefinitions4.dynamicTint) {
                this.interactiveModel = this.interactiveModel.copy((byte) 0, i_18, false);
                if (objectdefinitions4.dynamicTint) {
                    DynamicTint class34120 = Resource.mapRegion.getDynamicTint();
                    this.interactiveModel.tint(class34120.hue, class34120.saturation, class34120.lightness, class34120.opacity);
                }
            }
        }
        setupBoundingBox(1);
    }
    
    @Override
    public void addShadow(AbstractToolkit toolkit) {
        Shadow shadow;
        if (this.currentShadow == null && this.aBool10544) {
            TypeResult typeResult = method16117(toolkit, 262144, true);
            shadow = (Shadow) (typeResult != null ? typeResult.companionObject : null);
        } else {
            shadow = this.currentShadow;
            this.currentShadow = null;
        }
        Vector3 abs = getAbsolutePosition().coords;
        if (shadow != null) {
            super.scene.renderShadows(shadow, super.collisionPlane, (int) abs.x, (int) abs.z, null);
        }
    }

    @Override
    public boolean hasAlpha() {
        return this.interactiveModel != null && this.interactiveModel.accountForLayers();
    }

    public int getShadowDisplacement() {
        return this.interactiveModel != null ? this.interactiveModel.getSize2d() / 4 : 15;
    }

    public Model method16116(AbstractToolkit toolkit, int functionMask) {
        if (this.interactiveModel != null && toolkit.mismatchingFunctionMask(this.interactiveModel.getFunctionMask(), functionMask) == 0) {
            return this.interactiveModel;
        } 
        TypeResult typeResult = method16117(toolkit, functionMask, false);
        return typeResult != null ? (Model) typeResult.mainObject : null;
    }

    public TypeResult method16117(AbstractToolkit graphicalrenderer1, int i_2, boolean bool3) {
        ObjectType objectdefinitions5 = this.aClass47410542.list(this.id);
        Ground class3906;
        Ground class3907;
        if (this.aBool10536) {
            class3906 = super.scene.underwaterHeightmap[super.collisionPlane];
            class3907 = super.scene.surfaceHeightmap[0];
        } else {
            class3906 = super.scene.surfaceHeightmap[super.collisionPlane];
            if (super.collisionPlane < 3) {
                class3907 = super.scene.surfaceHeightmap[super.collisionPlane + 1];
            } else {
                class3907 = null;
            }
        }
        Vector3 vector38 = getAbsolutePosition().coords;
        return objectdefinitions5.getPair(graphicalrenderer1, i_2, this.aByte10539 != ObjectShapes.GROUND_INTERACT.shape ? this.aByte10539 : ObjectShapes.SCENERY_INTERACT.shape, ObjectShapes.GROUND_INTERACT.shape == this.aByte10539 ? this.aByte10540 + 4 : this.aByte10540, class3906, class3907, (int) vector38.x, (int) vector38.y, (int) vector38.z, bool3, null);
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit graphicalrenderer1) {
        Vector3 vector33 = getAbsolutePosition().coords;
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate((int) vector33.x, (int) vector33.y, (int) vector33.z, method16116(graphicalrenderer1, 0));
        }
        return this.axisAlignedBoundingBox;
    }

    @Override
    public int getShape() {
        return this.aByte10539;
    }

    @Override
    public boolean hasTileShadow() {
        return this.aBool10543;
    }

    @Override
    public void castShadow(SceneGraphEntity class521Sub12, int i_3, int i_4, int i_5, boolean bool6) {
        if (class521Sub12 instanceof StationaryWall class521Sub1Sub5Sub18) {
            if (this.interactiveModel != null && class521Sub1Sub5Sub18.model != null) {
                this.interactiveModel.applyShading(class521Sub1Sub5Sub18.model, i_3, i_4, i_5, bool6);
            }
        } else if (class521Sub12 instanceof StationaryInteractiveObject class521Sub1Sub1Sub69) {
            if (this.interactiveModel != null && class521Sub1Sub1Sub69.interactiveModel != null) {
                this.interactiveModel.applyShading(class521Sub1Sub1Sub69.interactiveModel, i_3, i_4, i_5, bool6);
            }
        }
    }

    @Override
    public void removeTileShadow() {
        this.aBool10543 = false;
        if (this.interactiveModel != null) {
            this.interactiveModel.applyShading(this.interactiveModel.getFunctionMask() & -65537);
        }
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public RenderedEntity draw(AbstractToolkit graphicalrenderer1) {
        if (this.interactiveModel == null) {
            return null;
        }
        Matrix43 matrix44var3 = getProjectionMatrix();
        RenderedEntity class2854 = RenderedEntityCache.generate(this.aBool10541);
        this.interactiveModel.drawMatrixThreaded(matrix44var3, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        return class2854;
    }

    @Override
    public int getRotation() {
        return this.aByte10540;
    }

    @Override
    public boolean hasRenderingShadow() {
        return this.aBool10544;
    }

    @Override
    public void removeShadow(AbstractToolkit graphicalrenderer1) {
        Shadow class282Sub50Sub174;
        if (this.currentShadow == null && this.aBool10544) {
            TypeResult class4525 = method16117(graphicalrenderer1, 262144, true);
            class282Sub50Sub174 = (Shadow) (class4525 != null ? class4525.companionObject : null);
        } else {
            class282Sub50Sub174 = this.currentShadow;
            this.currentShadow = null;
        }
        Vector3 vector36 = getAbsolutePosition().coords;
        if (class282Sub50Sub174 != null) {
            super.scene.removeShadow(class282Sub50Sub174, super.collisionPlane, (int) vector36.x, (int) vector36.z, null);
        }
    }

    @Override
    public boolean disregardTextureMovement() {
        return this.interactiveModel == null || !this.interactiveModel.hasAnimatedTexture();
    }

    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int camX, int camY) {
        Model model = method16116(toolkit, 131072);
        if (model != null) {
            Matrix43 projectionMatrix = getProjectionMatrix();
            return model.calculateProjection(camX, camY, projectionMatrix, false, 0);
        }
        return false;
    }

    @Override
    public void upload() {
        if (this.interactiveModel != null) {
            this.interactiveModel.uploadToGpu();
        }
    }

    @Override
    public int getMinY() {
        return this.interactiveModel != null ? this.interactiveModel.getMinY() : 0;
    }

    @Override
    public void particleCycle(AbstractToolkit graphicalrenderer1) {
    }
}
