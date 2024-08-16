package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.config.loc.LocTypeCustomisation;
import com.jagex.game.runetek5.config.loc.LocType;
import com.jagex.game.runetek5.config.loc.LocTypeList;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class AnimatedWallDecoration extends AbstractWallDecoration implements GameObject {

    public final AnimatedGameObject wallDecoration;
    public boolean aBool10504 = true;
    public Silhouette axisAlignedBoundingBox;
    public final boolean interactive;

    public AnimatedWallDecoration(Scene scene, AbstractToolkit toolkit, LocTypeList loctypelist, LocType loctype, int h, int logicHeight, int x, int y, int z, boolean overlayHidden, int posX, int posY, int type, int rotation, int sequenceId) {
        super(scene, x, y, z, h, logicHeight, posX, posY);
        this.wallDecoration = new AnimatedGameObject(toolkit, loctypelist, loctype, type, rotation, super.plane, logicHeight, this, overlayHidden, sequenceId);
        this.interactive = loctype.hasActions != 0 && !overlayHidden;
        setupBoundingBox(1);
    }
    

    @Override
    public void addShadow(AbstractToolkit graphicalrenderer1) {
        this.wallDecoration.getAnimatedShadowedModel(graphicalrenderer1);
    }

    @Override
    public boolean hasAlpha() {
        return this.aBool10504;
    }


    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit graphicalrenderer1) {
        return this.axisAlignedBoundingBox;
    }


    @Override
    public int getIconHeight() {
        return this.wallDecoration.getNegativeMinY();
    }

    public void addCustomisation(LocTypeCustomisation class4761) {
        this.wallDecoration.addCustomisation(class4761);
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        Model model = this.wallDecoration.getAnimatedModel(toolkit, 2048, false, true);
        if (model == null) {
            return null;
        } 
        Matrix43 matrix44var4 = toolkit.getRenderingMatrix();
        matrix44var4.copyFrom(getProjectionMatrix());
        matrix44var4.translate(super.positionX, 0.0F, super.positionY);
        CoordinateSpace abs = getAbsolutePosition();
        RenderedEntity renderedEntity = RenderedEntityCache.generate(this.interactive);
        int i_7 = (int) abs.coords.x >> 9;
        int i_8 = (int) abs.coords.z >> 9;
        this.wallDecoration.animateParticles(toolkit, model, matrix44var4, true);
        model.drawMatrixThreaded(matrix44var4, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        if (this.wallDecoration.particleSystem != null) {
            ParticleWrapper class1519 = this.wallDecoration.particleSystem.getAllParticles();
            toolkit.renderParticles(class1519);
        }
        this.aBool10504 = model.accountForLayers() || this.wallDecoration.particleSystem != null;
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate((int) abs.coords.x, (int) abs.coords.y, (int) abs.coords.z, model);
        } else {
            Silhouette.set(this.axisAlignedBoundingBox, (int) abs.coords.x, (int) abs.coords.y, (int) abs.coords.z, model);
        }
        return renderedEntity;
    }

    @Override
    public void particleCycle(AbstractToolkit graphicalrenderer1) {
        Model meshrasterizer3 = this.wallDecoration.getAnimatedModel(graphicalrenderer1, 262144, false, true);
        if (meshrasterizer3 != null) {
            Matrix43 matrix44var4 = getProjectionMatrix();
            CoordinateSpace class3055 = getAbsolutePosition();
            int i_6 = (int) class3055.coords.x >> 9;
            int i_7 = (int) class3055.coords.z >> 9;
            this.wallDecoration.animateParticles(graphicalrenderer1, meshrasterizer3, matrix44var4, false);
        }
    }

    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int i_2, int i_3) {
        Model model = this.wallDecoration.getAnimatedModel(toolkit, 131072, false, false);
        return model != null && model.calculateProjection(i_2, i_3, getProjectionMatrix(), false, 0);
    }

    @Override
    public int getMinY() {
        return this.wallDecoration.getMinY();
    }

    @Override
    public void upload() {
    }

    @Override
    public boolean hasRenderingShadow() {
        return this.wallDecoration.hasRenderingShadow();
    }

    @Override
    public void removeShadow(AbstractToolkit graphicalrenderer1) {
        this.wallDecoration.removeShadow();
    }

    @Override
    public int getRotation() {
        return this.wallDecoration.rotation;
    }

    @Override
    public boolean disregardTextureMovement() {
        return false;
    }

    @Override
    public int getId() {
        return this.wallDecoration.locId;
    }

    @Override
    public int getShape() {
        return this.wallDecoration.type;
    }
}
