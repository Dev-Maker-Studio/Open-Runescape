package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Quaternion;
import com.jagex.game.runetek5.math.QuaternionPool;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class Projectile extends AbstractInteractiveEntity {

    public final int cycleToSpawnAt;
    public final int cyclesUntilDisappear;
    public final int entityLockOn;
    public final int endHeight;
    public double stepX;
    public double stepZ;
    public double aDouble10396;
    public double aDouble10397;
    public double aDouble10390;
    public boolean hasAlpha;
    public int projectileMinY;
    public ParticleSystem particleSystem;
    public boolean animating;
    public final int gfxId;
    public final int angle;
    public final int slope;
    public final int entitySource;
    public final int startHeight;
    public final boolean adjustToTerrain;
    public final int matrxIndex;
    public final Sequence sequence;

    public Projectile(Scene scene, int spotAnimId, int fromPlane, int toPlane, int localX, int localY, int startHeight, int startTime, int endTime, int angle, int slope, int entitySource, int entityLockOn, int endHeight, boolean useTerrainHeight, int i_16) {
        super(scene, fromPlane, toPlane, localX, MapRegion.tileheightForAbsolutePosition(localX, localY, fromPlane) - startHeight, localY, localX >> 9, localX >> 9, localY >> 9, localY >> 9, false, (byte) 0);
        this.gfxId = spotAnimId;
        this.cycleToSpawnAt = startTime;
        this.cyclesUntilDisappear = endTime;
        this.angle = angle;
        this.slope = slope;
        this.entitySource = entitySource;
        this.entityLockOn = entityLockOn;
        this.startHeight = startHeight;
        this.endHeight = endHeight;
        this.adjustToTerrain = useTerrainHeight;
        this.animating = false;
        this.matrxIndex = i_16;
        int sequenceId = TypeLists.SPOT_ANIM.list(this.gfxId).sequenceId;
        this.sequence = new EntitySequence(this);
        this.sequence.update(sequenceId);
        this.setupBoundingBox(1);
    }

    @Override
    public boolean disregardTextureMovement() {
        return false;
    }

    public void cycle() {
        if (!this.animating && this.entitySource != 0) {
            Object lockedOnEntity = null;
            if (client.MAP_LOADING_STAGE == 4) {
                lockedOnEntity = CutsceneTypeList.CUTSCENE_ENTITIES[this.entitySource - 1].getEntity();
            } else {
                int entityLockon;
                if (this.entitySource < 0) {
                    entityLockon = -this.entitySource - 1;
                    if (entityLockon == PlayerUpdate.myPlayerIndex) {
                        lockedOnEntity = PlayerUpdate.MY_PLAYER;
                    } else {
                        lockedOnEntity = PlayerUpdate.players[entityLockon];
                    }
                } else {
                    entityLockon = this.entitySource - 1;
                    ObjectNode npcObjectNode = client.NPC_MAP.get(entityLockon);
                    if (npcObjectNode != null) {
                        lockedOnEntity = npcObjectNode.value;
                    }
                }
            }

            if (lockedOnEntity != null) {
                Vector3 absolute = ((PathingEntity) lockedOnEntity).getAbsolutePosition().coords;
                setBasePosition(absolute.x, (MapRegion.tileheightForAbsolutePosition((int) absolute.x, (int) absolute.z, super.plane) - this.startHeight), absolute.z);
                if (this.matrxIndex >= 0) {
                    BasType bastype = ((PathingEntity) lockedOnEntity).getBastype();
                    int projectionX = 0;
                    int projectionZ = 0;
                    if (bastype.objVerticeTransformations != null && bastype.objVerticeTransformations[this.matrxIndex] != null) {
                        projectionX += bastype.objVerticeTransformations[this.matrxIndex][0];
                        projectionZ += bastype.objVerticeTransformations[this.matrxIndex][2];
                    }

                    if (bastype.projectionOffset != null && bastype.projectionOffset[this.matrxIndex] != null) {
                        projectionX += bastype.projectionOffset[this.matrxIndex][0];
                        projectionZ += bastype.projectionOffset[this.matrxIndex][2];
                    }

                    if (projectionX != 0 || projectionZ != 0) {
                        int rotationOffset = ((PathingEntity) lockedOnEntity).yaw.getRotation();
                        int baseRotation = rotationOffset;
                        if (((PathingEntity) lockedOnEntity).equipmentRotations != null && ((PathingEntity) lockedOnEntity).equipmentRotations[this.matrxIndex] != -1) {
                            baseRotation = ((PathingEntity) lockedOnEntity).equipmentRotations[this.matrxIndex];
                        }

                        int rotation = baseRotation - rotationOffset & 0x3fff;
                        int xan = MathUtils.SINE[rotation];
                        int zan = MathUtils.COSINE[rotation];
                        int offsetX = xan * projectionZ + projectionX * zan >> 14;
                        int offsetZ = projectionZ * zan - xan * projectionX >> 14;
                        Vector3 newAbsolutePosition = Vector3Pool.vector3From(getAbsolutePosition().coords);
                        newAbsolutePosition.x += offsetX;
                        newAbsolutePosition.z += offsetZ;
                        setBasePosition(newAbsolutePosition);
                        newAbsolutePosition.pushVectorStack();
                    }
                }
            }
        }

    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return null;
    }

    public void start(int x, int z, int y, int currentCycle) {
        Vector3 absolute = Vector3Pool.vector3From(getAbsolutePosition().coords);
        if (!this.animating) {
            float offsetX = x - absolute.x;
            float offsetZ = z - absolute.z;
            float size2d = (float) Math.sqrt(offsetZ * offsetZ + offsetX * offsetX);
            if (size2d != 0.0F) {
                absolute.x += offsetX * this.slope / size2d;
                absolute.z += offsetZ * this.slope / size2d;
            }
            if (this.adjustToTerrain) {
                absolute.y = (MapRegion.tileheightForAbsolutePosition((int) absolute.x, (int) absolute.z, super.plane) - this.startHeight);
            }
            setBasePosition(absolute);
        }
        double cyclesLeft = this.cyclesUntilDisappear + 1 - currentCycle;
        this.stepX = (x - absolute.x) / cyclesLeft;
        this.stepZ = (z - absolute.z) / cyclesLeft;
        this.aDouble10396 = Math.sqrt(this.stepZ * this.stepZ + this.stepX * this.stepX);
        if (this.angle != -1) {
            if (!this.animating) {
                this.aDouble10397 = -this.aDouble10396 * Math.tan(0.02454369D * this.angle);
            }
            this.aDouble10390 = ((y - absolute.y) - this.aDouble10397 * cyclesLeft) * 2.0D / (cyclesLeft * cyclesLeft);
        } else {
            this.aDouble10397 = (y - absolute.y) / cyclesLeft;
        }
        absolute.pushVectorStack();
    }

    @Override
    public int getMinY() {
        return this.projectileMinY;
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        Model gfxModel = generateGfxModel(toolkit, HAS_TEXTURE_COORDS);
        if (gfxModel == null) {
            return null;
        } 
        Matrix43 projectionMatrix = getProjectionMatrix();
        updateParticles(toolkit, gfxModel, projectionMatrix);
        RenderedEntity renderedObject = RenderedEntityCache.generate(false);
        gfxModel.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons[0], ModelFunctionMaskGL.EMPTY);
        if (this.particleSystem != null) {
            ParticleWrapper particles = this.particleSystem.getAllParticles();
            toolkit.renderParticles(particles);
        }
        this.hasAlpha = gfxModel.accountForLayers();
        gfxModel.getSize3d();
        this.projectileMinY = gfxModel.getMinY();
        return renderedObject;
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
        Model gfxModel = generateGfxModel(toolkit, 0);
        if (gfxModel != null) {
            Matrix43 projectionMatrix = getProjectionMatrix();
            this.projectileMinY = gfxModel.getMinY();
            gfxModel.getSize3d();
            updateParticles(toolkit, gfxModel, projectionMatrix);
        }
    }

    @Override
    public void calculatePosition() {
        Vector3 absolute = getAbsolutePosition().coords;
        super.startX = super.endX = (short) ((int) (absolute.x / 512.0F));
        super.startZ = super.endZ = (short) ((int) (absolute.z / 512.0F));
    }

    public void updateParticles(AbstractToolkit toolkit, Model gfxModel, Matrix43 projectionMatrix) {
        gfxModel.updateParticlePosition(projectionMatrix);
        EmissiveTriangle[] emitters = gfxModel.getEmissiveTriangles();
        EffectiveVertex[] vertices = gfxModel.getEffectiveVertices();
        if ((this.particleSystem == null || this.particleSystem.isDisabled) && (emitters != null || vertices != null)) {
            this.particleSystem = ParticleSystem.get(client.cycles, true);
        }
        if (this.particleSystem != null) {
            this.particleSystem.performCycle(toolkit, client.cycles, emitters, vertices);
            this.particleSystem.setLogicHeight(super.plane);
        }
    }

    public void nextParticleCycle() {
        if (this.particleSystem != null) {
            this.particleSystem.setIsNewCycle();
        }
    }

    @Override
    public boolean calculateProjection(AbstractToolkit graphicalrenderer1, int i_2, int i_3) {
        return false;
    }

    @Override
    public void castShadow(SceneGraphEntity class521Sub12, int i_3, int i_4, int i_5, boolean bool6) {
        throw new IllegalStateException();
    }

    public Model generateGfxModel(AbstractToolkit toolkit, int functionMask) {
        SpotAnimType gfxtype = TypeLists.SPOT_ANIM.list(this.gfxId);
        return gfxtype.getAnimatedModel(toolkit, functionMask, this.sequence, (byte) 2);
    }

    @Override
    public void removeTileShadow() {
        throw new IllegalStateException();
    }

    @Override
    public boolean hasAlpha() {
        return this.hasAlpha;
    }

    public void animate(int step) {
        this.animating = true;
        CoordinateSpace abs = new CoordinateSpace(getAbsolutePosition());
        abs.coords.x = (float) (abs.coords.x + this.stepX * step);
        abs.coords.z = (float) (abs.coords.z + this.stepZ * step);
        if (this.adjustToTerrain) {
            abs.coords.y = (MapRegion.tileheightForAbsolutePosition((int) abs.coords.x, (int) abs.coords.z, super.plane) - this.startHeight);
        } else if (this.angle != -1) {
            abs.coords.y = (float) (abs.coords.y + this.aDouble10397 * step + (double) step * step * 0.5D * this.aDouble10390);
            this.aDouble10397 += step * this.aDouble10390;
        } else {
            abs.coords.y = (float) (abs.coords.y + this.aDouble10397 * step);
        }

        abs.quaternion.rotate(1.0F, 0.0F, 0.0F, (float) Math.atan2(this.aDouble10397, this.aDouble10396));
        Quaternion quaternion = QuaternionPool.newQuaternion();
        quaternion.rotate(0.0F, 1.0F, 0.0F, (float) Math.atan2(this.stepX, this.stepZ) - 3.1415927F);
        abs.quaternion.multiply(quaternion);
        quaternion.store();
        setAbsolutePosition(abs);
        if (this.sequence.progressCycle(1) && this.sequence.needsReset()) {
            this.sequence.reset();
        }
    }


    @Override
    public boolean hasTileShadow() {
        return false;
    }

}
