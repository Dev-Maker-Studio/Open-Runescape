package com.jagex.game.runetek5.entity.spotanimation;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.sequence.EntitySequence;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;


public class SpotAnimation extends AbstractInteractiveEntity {

    public boolean accountForLayers = true;
    public int objectMinY;
    public final int rotation;
    public final int gfxId;
    public Sequence sequence;
    public ParticleSystem particleSystem;

    public SpotAnimation(Scene scene, int gfxId, int speed, int fromPlane, int toPlane, int localX, int height, int localY, int startX, int endX, int startY, int endY, int rotation, boolean adjustReplayMode) {
        super(scene, fromPlane, toPlane, localX, height, localY, startX, endX, startY, endY, false, (byte) 0);
        this.gfxId = gfxId;
        this.rotation = rotation;
        SpotAnimType spotanimtype = TypeLists.SPOT_ANIM.list(this.gfxId);
        int sequenceId = spotanimtype.sequenceId;
        if (sequenceId != -1) {
            this.sequence = new EntitySequence(this);
            int replayMode = spotanimtype.replay ? 0 : 2;
            if (adjustReplayMode) {
                replayMode = 1;
            }
            this.sequence.animateSequence(sequenceId, speed, replayMode, false);
        }
        setupBoundingBox(1);
    }

    @Override
    public boolean disregardTextureMovement() {
        return false;
    }

    @Override
    public int getMinY() {
        return this.objectMinY;
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return null;
    }

    public void progressCycle() {
        if (this.sequence != null && !this.sequence.needsReset()) {
            this.sequence.progressCycle(1);
        }
    }

    public boolean notAccelerated() {
        return this.sequence != null && !this.sequence.hasSpeed();
    }

    public boolean needsReset() {
        return this.sequence == null || this.sequence.needsReset();
    }

    public Model generateGfxModel(AbstractToolkit toolkit, int functionMask, int gfxId) {
        SpotAnimType gfxType = TypeLists.SPOT_ANIM.list(gfxId);
        Ground currentLvl = super.scene.surfaceHeightmap[super.plane];
        Ground nextLvl = super.collisionPlane < 3 ? super.scene.surfaceHeightmap[super.collisionPlane + 1] : null;
        Vector3 absolute = getAbsolutePosition().coords;
        return this.sequence != null && !this.sequence.needsReset() ?
        		gfxType.getAnimatedModel(toolkit, functionMask, true, currentLvl, nextLvl, (int) absolute.x, (int) absolute.y, (int) absolute.z, this.sequence, (byte) 2)
        		: gfxType.getAnimatedModel(toolkit, functionMask, true, currentLvl, nextLvl, (int) absolute.x, (int) absolute.y, (int) absolute.z, null, (byte) 2);
    }

    public void updateParticles(AbstractToolkit toolkit, Model gfxModel, Matrix43 particlePosition) {
        gfxModel.updateParticlePosition(particlePosition);
        EmissiveTriangle[] emitter = gfxModel.getEmissiveTriangles();
        EffectiveVertex[] vertices = gfxModel.getEffectiveVertices();
        if ((this.particleSystem == null || this.particleSystem.isDisabled) && (emitter != null || vertices != null)) {
            this.particleSystem = ParticleSystem.get(client.cycles, true);
        }

        if (this.particleSystem != null) {
            this.particleSystem.performCycle(toolkit, client.cycles, emitter, vertices);
            this.particleSystem.setLogicHeight(super.plane);
        }

    }

    public void resetCycle() {
        if (this.particleSystem != null) {
            this.particleSystem.setIsNewCycle();
        }
    }

    @Override
    public boolean calculateProjection(AbstractToolkit graphicalrenderer1, int i_2, int i_3) {
        return false;
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
    public void removeTileShadow() {
        throw new IllegalStateException();
    }


    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        Model model = generateGfxModel(toolkit, HAS_TEXTURE_COORDS | (this.rotation != 0 ? DISPLACE_X | DISPLACE_Z : 0), this.gfxId);
        if (model == null) {
            return null;
        } 
        if (this.rotation != 0) {
            model.yaxisRotateWithoutNormals(this.rotation * 2048);
        }

        Matrix43 projectionMatrix = getProjectionMatrix();
        updateParticles(toolkit, model, projectionMatrix);
        RenderedEntity renderedEntity = RenderedEntityCache.generate(false);
        model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        if (this.particleSystem != null) {
            ParticleWrapper particles = this.particleSystem.getAllParticles();
            toolkit.renderParticles(particles);
        }

        this.accountForLayers = model.accountForLayers();
        this.objectMinY = model.getMinY();
        model.getSize3d();
        return renderedEntity;
    }


    @Override
    public void particleCycle(AbstractToolkit toolkit) {
        Model gfxModel = generateGfxModel(toolkit, 0, this.gfxId);
        if (gfxModel != null) {
            updateParticles(toolkit, gfxModel, getProjectionMatrix());
        }
    }


    @Override
    public boolean hasAlpha() {
        return this.accountForLayers;
    }
}
