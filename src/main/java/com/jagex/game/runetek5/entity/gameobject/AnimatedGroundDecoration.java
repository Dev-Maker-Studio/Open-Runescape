package com.jagex.game.runetek5.entity.gameobject;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;
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
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class AnimatedGroundDecoration extends AbstractGroundDecoration implements GameObject {

    public final AnimatedGameObject gameObject;
    public boolean hasRenderingParticles = true;
    public Silhouette axisAlignedBoundingBox;
    public final boolean canInteract;

    public AnimatedGroundDecoration(Scene scene, AbstractToolkit renderer, ObjectTypeList loctypelist, ObjectType loctype, int logicHeight, int plane, int i_7, int i_8, int i_9, boolean overlayHidden, int rotation, int sequenceId) {
        super(scene, i_7, i_8, i_9, logicHeight, plane, loctype.groundDecorationHeight);
        this.gameObject = new AnimatedGameObject(renderer, loctypelist, loctype, ObjectShapes.GROUND_DECORATION.shape, rotation, logicHeight, plane, this, overlayHidden, sequenceId);
        this.canInteract = loctype.hasActions != 0 && !overlayHidden;
        setupBoundingBox(1);
    }

    @Override
    public void addShadow(AbstractToolkit graphicalrenderer1) {
        this.gameObject.getAnimatedShadowedModel(graphicalrenderer1);
    }
    
    @Override
    public boolean disregardTextureMovement() {
        return false;
    }

    @Override
    public boolean hasAlpha() {
        return this.hasRenderingParticles;
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return this.axisAlignedBoundingBox;
    }

    @Override
    public int getMinY() {
        return this.gameObject.getMinY();
    }

    @Override
    public int getIconHeight() {
        return this.gameObject.getNegativeMinY();
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        Model model = this.gameObject.getAnimatedModel(toolkit, HAS_TEXTURE_COORDS, false, true);
        if (model == null) {
            return null;
        } 
        Matrix43 projectionMatrix = getProjectionMatrix();
        CoordinateSpace absolute = getAbsolutePosition();
        RenderedEntity renderedEntity = RenderedEntityCache.generate(this.canInteract);
        int x = (int) absolute.coords.x >> 9;
        int y = (int) absolute.coords.z >> 9;
        this.gameObject.animateParticles(toolkit, model, projectionMatrix, true);
        model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        if (this.gameObject.particleSystem != null) {
            ParticleWrapper particles = this.gameObject.particleSystem.getAllParticles();
            toolkit.renderParticles(particles);
        }
        this.hasRenderingParticles = model.accountForLayers() || this.gameObject.particleSystem != null;
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate((int) absolute.coords.x, (int) absolute.coords.y, (int) absolute.coords.z, model);
        } else {
            Silhouette.set(this.axisAlignedBoundingBox, (int) absolute.coords.x, (int) absolute.coords.y, (int) absolute.coords.z, model);
        }
        return renderedEntity;
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
        Model model = this.gameObject.getAnimatedModel(toolkit, SHADOWED, true, true);
        if (model != null) {
            Vector3 abs = getAbsolutePosition().coords;
            int x = (int) abs.x >> 9;
            int z = (int) abs.z >> 9;
            this.gameObject.animateParticles(toolkit, model, getProjectionMatrix(), false);
        }
    }

    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int cameraX, int cameraY) {
        Model model = this.gameObject.getAnimatedModel(toolkit, 131072, false, false);
        return model != null && model.calculateProjection(cameraX, cameraY, getProjectionMatrix(), false, 0);
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
    public int getId() {
        return this.gameObject.locId;
    }

    @Override
    public int getRotation() {
        return this.gameObject.rotation;
    }

    public void addCustomisation(ObjectTypeCustomisation locCustomisation) {
        this.gameObject.addCustomisation(locCustomisation);
    }

    @Override
    public void upload() {
    }

    @Override
    public boolean hasRenderingShadow() {
        return this.gameObject.hasRenderingShadow();
    }

    @Override
    public void removeTileShadow() {
        throw new IllegalStateException();
    }

    @Override
    public int getShape() {
        return this.gameObject.type;
    }

    @Override
    public void removeShadow(AbstractToolkit toolkit) {
        this.gameObject.removeShadow();
    }
}
