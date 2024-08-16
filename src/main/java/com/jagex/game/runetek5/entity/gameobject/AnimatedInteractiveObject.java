package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.config.loc.LocTypeCustomisation;
import com.jagex.game.runetek5.config.loc.LocShapes;
import com.jagex.game.runetek5.config.loc.LocType;
import com.jagex.game.runetek5.config.loc.LocTypeList;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
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

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class AnimatedInteractiveObject extends AbstractInteractiveEntity implements GameObject {

    public final AnimatedGameObject activeGameObject;
    public boolean hasParticles = true;
    public Silhouette axisAlignedBoundingBox;
    public final boolean canInteract;

    public AnimatedInteractiveObject(Scene scene, AbstractToolkit toolkit, LocTypeList loctypeList, LocType loctype, int i_5, int collisionPlane, int x, int y, int z, boolean overlayHidden, int startX, int endX, int startY, int endY, int type, int rotation, int sequenceId) {
        super(scene, i_5, collisionPlane, x, y, z, startX, endX, startY, endY, loctype.supportsItems == 1, LocShapes.getRotatedWall(type, rotation));
        this.activeGameObject = new AnimatedGameObject(toolkit, loctypeList, loctype, type, rotation, super.plane, collisionPlane, this, overlayHidden, sequenceId);
        this.canInteract = loctype.hasActions != 0 && !overlayHidden;
        setupBoundingBox(1);
    }

    @Override
    public void addShadow(AbstractToolkit graphicalrenderer1) {
        this.activeGameObject.getAnimatedShadowedModel(graphicalrenderer1);
    }
    
    @Override
    public boolean disregardTextureMovement() {
        return false;
    }


    @Override
    public int getMinY() {
        return this.activeGameObject.getMinY();
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return this.axisAlignedBoundingBox;
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        Model model = this.activeGameObject.getAnimatedModel(toolkit, HAS_TEXTURE_COORDS, false, true);
        if (model == null) {
            return null;
        } 
        Matrix43 projectionMatrix = getProjectionMatrix();
        RenderedEntity renderedEntity = RenderedEntityCache.generate(this.canInteract);
        this.activeGameObject.animateParticles(toolkit, model, projectionMatrix, true);
        model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        if (this.activeGameObject.particleSystem != null) {
            ParticleWrapper particles = this.activeGameObject.particleSystem.getAllParticles();
            toolkit.renderParticles(particles);
        }
        this.hasParticles = model.accountForLayers() || this.activeGameObject.particleSystem != null;
        CoordinateSpace absolute = getAbsolutePosition();
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate((int) absolute.coords.x, (int) absolute.coords.y, (int) absolute.coords.z, model);
        } else {
            Silhouette.set(this.axisAlignedBoundingBox, (int) absolute.coords.x, (int) absolute.coords.y, (int) absolute.coords.z, model);
        }
        return renderedEntity;
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
        Model model = this.activeGameObject.getAnimatedModel(toolkit, SHADOWED, true, true);
        if (model != null) {
            this.activeGameObject.animateParticles(toolkit, model, getProjectionMatrix(), false);
        }
    }

    public void addCustomisation(LocTypeCustomisation class4761) {
        this.activeGameObject.addCustomisation(class4761);
    }

    @Override
    public boolean calculateProjection(AbstractToolkit graphicalrenderer1, int i_2, int i_3) {
        Model meshrasterizer5 = this.activeGameObject.getAnimatedModel(graphicalrenderer1, 131072, false, false);
        return meshrasterizer5 != null && meshrasterizer5.calculateProjection(i_2, i_3, getProjectionMatrix(), false, 0);
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
    public int getShape() {
        return this.activeGameObject.type;
    }

    @Override
    public int getRotation() {
        return this.activeGameObject.rotation;
    }

    @Override
    public void upload() {
    }

    @Override
    public boolean hasRenderingShadow() {
        return this.activeGameObject.hasRenderingShadow();
    }

    @Override
    public void removeShadow(AbstractToolkit graphicalrenderer1) {
        this.activeGameObject.removeShadow();
    }

    @Override
    public int getId() {
        return this.activeGameObject.locId;
    }

    @Override
    public boolean hasAlpha() {
        return this.hasParticles;
    }


    @Override
    public void castShadow(SceneGraphEntity class521Sub12, int i_3, int i_4, int i_5, boolean bool6) {
        throw new IllegalStateException();
    }

    @Override
    public int getIconHeight() {
        return this.activeGameObject.getNegativeMinY();
    }
}
