package com.jagex.game.runetek5.entity.grounditem;

import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.entity.InteractiveEntityContainer;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.client.PreferenceManager;

public class GroundItem extends AbstractGroundItem {

    public int amount;
    public int lastAmount;
    public int nodeAmount;
    public int nodeId;
    public int anInt10422;
    public int lastId = -1;
    public int itemId = -1;
    public int tintHue;
    public int tintSaturation;
    public int tintLightness;
    public int tintOpacity;
    public boolean layerPriorities;
    public int size3d;

    public GroundItem(Scene scene, int x, int y, int z, int plane, int collisionPlane) {
        super(scene, x, y, z, plane, collisionPlane);
        setupBoundingBox(3);
        randomizeAppearance();
    }

    @Override
    public int getIconHeight() {
        return this.anInt10422 - getMinY();
    }

    public void randomizeAppearance() {
        this.tintHue = 32 + (int) (Math.random() * 4.0D);
        this.tintSaturation = 3 + (int) (Math.random() * 2.0D);
        this.tintLightness = 16 + (int) (Math.random() * 3.0D);
        if (PreferenceManager.preferences.textures.getValue() == 1) {
            this.tintOpacity = (int) (Math.random() * 10.0D);
        } else {
            this.tintOpacity = (int) (Math.random() * 20.0D);
        }

    }

    @Override
    public boolean hasAlpha() {
        return this.layerPriorities;
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return null;
    }

    @Override
    public int getMinY() {
        return -10;
    }


    @Override
    public void particleCycle(AbstractToolkit graphicalrenderer1) {
    }

    @Override
    public boolean disregardTextureMovement() {
        return false;
    }

    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int i_2, int i_3) {
        Matrix43 matrix44var5 = toolkit.getRenderingMatrix();
        matrix44var5.translateFromCoordspace(getAbsolutePosition());
        matrix44var5.translate(0.0F, -10.0f, 0.0F);
        Model model = TypeLists.OBJ.list(this.nodeId).getAnimatedModel(toolkit, PROJECTION, this.nodeAmount, null, null, 0, 0, 0, 0);
        if (model != null && model.calculateProjection(i_2, i_3, matrix44var5, true, 0)) {
            return true;
        } 
        if (this.lastId != -1) {
            model = TypeLists.OBJ.list(this.lastId).getAnimatedModel(toolkit, PROJECTION, this.lastAmount, null, null, 0, 0, 0, 0);
            if (model != null && model.calculateProjection(i_2, i_3, matrix44var5, true, 0)) {
                return true;
            }
        }

        if (this.itemId != -1) {
            model = TypeLists.OBJ.list(this.itemId).getAnimatedModel(toolkit, PROJECTION, this.amount, null, null, 0, 0, 0, 0);
            return model != null && model.calculateProjection(i_2, i_3, matrix44var5, true, 0);
        }

        return false;
    }


    @Override
    public int getMaxSizeShift() {
        ItemType objType = TypeLists.OBJ.list(this.nodeId);
        int sizeShift = objType.pickSizeShift;
        if (this.lastId != -1) {
        	ItemType nextObjType = TypeLists.OBJ.list(this.lastId);
            if (nextObjType.pickSizeShift > sizeShift) {
                sizeShift = nextObjType.pickSizeShift;
            }
        }

        if (this.itemId != -1) {
        	ItemType nextObjType = TypeLists.OBJ.list(this.itemId);
            if (nextObjType.pickSizeShift > sizeShift) {
                sizeShift = nextObjType.pickSizeShift;
            }
        }

        return sizeShift;
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        Vector3 vector33 = Vector3Pool.vector3From(getAbsolutePosition().coords);
        InteractiveEntityContainer interactive = super.scene.getInteractiveObjects(super.plane, (int) vector33.x >> 9, (int) vector33.z >> 9);
        AbstractGroundDecoration groundDecoration = super.scene.getGroundDecoration(super.plane, (int) vector33.x >> 9, (int) vector33.z >> 9);
        int offsetY = 0;
        if (interactive != null && interactive.interactiveObject.supportsItems) {
            offsetY = interactive.interactiveObject.getMinY();
        }

        if (groundDecoration != null && groundDecoration.floorOffsetY > -offsetY) {
            offsetY = -groundDecoration.floorOffsetY;
        }

        if (offsetY != this.anInt10422) {
            vector33.y += (offsetY - this.anInt10422);
            setBasePosition(vector33);
            this.anInt10422 = offsetY;
        }

        Matrix43 renderingMatrix = toolkit.getRenderingMatrix();
        renderingMatrix.reset();
        if (this.anInt10422 == 0) {
            boolean bool8 = false;
            boolean bool9 = false;
            boolean bool10 = false;
            Ground class39011 = super.scene.localHeightmap[super.collisionPlane];
            int i_12 = this.size3d << 1;
            int i_14 = -i_12 / 2;
            int i_15 = -i_12 / 2;
            int i_16 = class39011.getAverageHeight(i_14 + (int) vector33.x, i_15 + (int) vector33.z);
            int i_17 = i_12 / 2;
            int i_18 = -i_12 / 2;
            int i_19 = class39011.getAverageHeight(i_17 + (int) vector33.x, i_18 + (int) vector33.z);
            int i_20 = -i_12 / 2;
            int i_21 = i_12 / 2;
            int i_22 = class39011.getAverageHeight(i_20 + (int) vector33.x, i_21 + (int) vector33.z);
            int i_23 = i_12 / 2;
            int i_24 = i_12 / 2;
            int i_25 = class39011.getAverageHeight(i_23 + (int) vector33.x, i_24 + (int) vector33.z);
            int i_26 = Math.min(i_16, i_19);
            int i_27 = Math.min(i_22, i_25);
            int i_28 = Math.min(i_19, i_25);
            int i_29 = Math.min(i_16, i_22);
            int i_30;
            if (i_12 != 0) {
                i_30 = (int) (Math.atan2(i_26 - i_27, i_12) * 2607.5945876176133D) & 0x3fff;
                if (i_30 != 0) {
                    renderingMatrix.rotate(1.0F, 0.0F, 0.0F, MathUtils.getTheta(i_30));
                }
            }

            if (i_12 != 0) {
                i_30 = (int) (Math.atan2(i_29 - i_28, i_12) * 2607.5945876176133D) & 0x3fff;
                if (i_30 != 0) {
                    renderingMatrix.rotate(0.0F, 0.0F, 1.0F, MathUtils.getTheta(-i_30));
                }
            }

            i_30 = i_16 + i_25;
            if (i_19 + i_22 < i_30) {
                i_30 = i_19 + i_22;
            }

            i_30 = (i_30 >> 1) - (int) vector33.y;
            if (i_30 != 0) {
                renderingMatrix.translate(0.0F, i_30, 0.0F);
            }
        }

        vector33.pushVectorStack();
        Vector3 absolute = getAbsolutePosition().coords;
        renderingMatrix.translate(absolute.x, absolute.y - 10.0F, absolute.z);
        RenderedEntity renderedEntity = RenderedEntityCache.generate(true);
        this.layerPriorities = false;
        this.size3d = 0;
        Model model;
        if (this.itemId != -1) {
            model = TypeLists.OBJ.list(this.itemId).getAnimatedModel(toolkit, 526336, this.amount, null, null, this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity);
            if (model != null) {
                model.drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons[2], ModelFunctionMaskGL.EMPTY);
                this.layerPriorities |= model.accountForLayers();
                this.size3d = model.getSize3d();
            }
        }

        if (this.lastId != -1) {
            model = TypeLists.OBJ.list(this.lastId).getAnimatedModel(toolkit, 526336, this.lastAmount, null, null, this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity);
            if (model != null) {
                model.drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons[1], ModelFunctionMaskGL.EMPTY);
                this.layerPriorities |= model.accountForLayers();
                if (model.getSize3d() > this.size3d) {
                    this.size3d = model.getSize3d();
                }
            }
        }

        model = TypeLists.OBJ.list(this.nodeId).getAnimatedModel(toolkit, 526336, this.nodeAmount, null, null, this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity);
        if (model != null) {
            model.drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
            this.layerPriorities |= model.accountForLayers();
            if (model.getSize3d() > this.size3d) {
                this.size3d = model.getSize3d();
            }
        }

        return renderedEntity;
    }

}
