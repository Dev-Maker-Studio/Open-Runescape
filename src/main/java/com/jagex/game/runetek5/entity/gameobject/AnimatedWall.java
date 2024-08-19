package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.config.object.ObjectTypeCustomisation;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class AnimatedWall extends AbstractWall implements GameObject {

    public final AnimatedGameObject gameObject;
    public boolean priorityRender = true;
    public Silhouette silhouette;
    public final boolean interactable;

    public AnimatedWall(Scene scene, AbstractToolkit toolkit, ObjectTypeList locTypeList, ObjectType locType, int plane, int collisionPlane, int x, int y, int z, boolean hideOverlay, int type, int rotation, int sequenceId) {
        super(scene, x, y, z, plane, collisionPlane, ObjectShapes.getWallCullType(type, rotation));
        this.gameObject = new AnimatedGameObject(toolkit, locTypeList, locType, type, rotation, plane, collisionPlane, this, hideOverlay, sequenceId);
        this.interactable = locType.hasActions != 0 && !hideOverlay;
        setupBoundingBox(1);
    }


    @Override
    public void addShadow(AbstractToolkit toolkit) {
        this.gameObject.getAnimatedShadowedModel(toolkit);
    }
    
    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        Model model = this.gameObject.getAnimatedModel(toolkit, 2048, false, true);
        if (model == null) {
            return null;
        }
        Matrix43 projectionMatrix = getProjectionMatrix();
        CoordinateSpace abs = getAbsolutePosition();
        RenderedEntity renderedEntity = RenderedEntityCache.generate(this.interactable);
        int x = (int) abs.coords.x >> 9;
        int z = (int) abs.coords.z >> 9;
        this.gameObject.animateParticles(toolkit, model, projectionMatrix, true);
        model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        if (this.gameObject.particleSystem != null) {
            ParticleWrapper particles = this.gameObject.particleSystem.getAllParticles();
            toolkit.renderParticles(particles);
        }

        this.priorityRender = model.accountForLayers() || this.gameObject.particleSystem != null;
        if (this.silhouette == null) {
            this.silhouette = Silhouette.generate((int) abs.coords.x, (int) abs.coords.y, (int) abs.coords.z, model);
        } else {
            Silhouette.set(this.silhouette, (int) abs.coords.x, (int) abs.coords.y, (int) abs.coords.z, model);
        }

        return renderedEntity;
    }

    @Override
    public boolean disregardTextureMovement() {
        return false;
    }

    @Override
    public boolean hasAlpha() {
        return this.priorityRender;
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return this.silhouette;
    }


    public void addCustomisation(ObjectTypeCustomisation customisation) {
        this.gameObject.addCustomisation(customisation);
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
        Model model = this.gameObject.getAnimatedModel(toolkit, 262144, true, true);
        if (model != null) {
            Matrix43 projectionMatrix = getProjectionMatrix();
            CoordinateSpace abs = getAbsolutePosition();
            int x = (int) abs.coords.x >> 9;
            int z = (int) abs.coords.z >> 9;
            this.gameObject.animateParticles(toolkit, model, projectionMatrix, false);
        }

    }

    @Override
    public boolean hasTileShadow() {
        return false;
    }

    @Override
    public void removeTileShadow() {
        throw new IllegalStateException();
    }

    @Override
    public int getId() {
        return this.gameObject.locId;
    }

    @Override
    public int getShape() {
        return this.gameObject.type;
    }

    @Override
    public int getRotation() {
        return this.gameObject.rotation;
    }

    @Override
    public void upload() {
    }

    @Override
    public boolean hasRenderingShadow() {
        return this.gameObject.hasRenderingShadow();
    }

    @Override
    public void removeShadow(AbstractToolkit graphicalrenderer1) {
        this.gameObject.removeShadow();
    }

    @Override
    public int getIconHeight() {
        return this.gameObject.getNegativeMinY();
    }

    @Override
    public int getMinY() {
        return this.gameObject.getMinY();
    }


    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int i_2, int i_3) {
        Model model = this.gameObject.getAnimatedModel(toolkit, 131072, false, false);
        return model != null && model.calculateProjection(i_2, i_3, getProjectionMatrix(), false, 0);
    }

    @Override
    public void castShadow(SceneGraphEntity class521Sub12, int i_3, int i_4, int i_5, boolean bool6) {
        throw new IllegalStateException();
    }
}
