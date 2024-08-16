package com.jagex.game.runetek5.entity.pathingentity.npc;

import java.util.Arrays;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.MovingSequence;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.graphics.hint.HintDetails;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.vardomain.VarProviders;
import com.jagex.game.runetek5.vardomain.VarnCache;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class NpcEntity extends PathingEntity {

    public NPCTypeCustomisation customisation;
    public int faceX = -1;
    public int faceY = -1;
    public int meshModifierCount = 1;
    public int headMeshModifierCount = 1;
    public int currRenderAnim = -1;
    public final VarnCache varns = new VarnCache();
    public NPCType definitions;
    public String modifiedName;
    public int modifiedCbLvl;
    public final int[] maxStats = new int[6];
    public final int[] currentStats = new int[6];
    public int tintHue;
    public int tintSaturation;
    public int tintLightness;
    public int tintOpacity;

    public NpcEntity(Scene scene) {
        super(scene, 25);
        createDynamicTint();
    }

    @Override
    public int getMaxSizeShift() {
        return this.definitions == null ? 0 : this.definitions.sizeShift;
    }

    /**
     * Generates random tints for the entity's model
     * presumably one of the purposes of this is to
     * deal with color bots
     */
    public void createDynamicTint() {
        this.tintHue = 32 + (int) (Math.random() * 4.0D);
        this.tintSaturation = 3 + (int) (Math.random() * 2.0D);
        this.tintLightness = 16 + (int) (Math.random() * 3.0D);
        if (PreferenceManager.preferences.textures.getValue() == 1) {
            this.tintOpacity = (int) (Math.random() * 6.0D);
        } else {
            this.tintOpacity = (int) (Math.random() * 12.0D);
        }

    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return null;
    }

    public boolean getAnimatedBodymodel(AbstractToolkit toolkit, int functionMask) {
        int initialFunctionMask = functionMask;
        BasType bastype = getBastype();
        Sequence seq = super.currentSequence.hasType() && !super.currentSequence.hasSpeed() ? super.currentSequence : null;
        MovingSequence moveAnimation = super.movingSequence.hasType() && (!super.movingSequence.hasType || seq == null) ? super.movingSequence : null;
        int x = bastype.anInt2786;
        int z = bastype.anInt2829;
        if (x != 0 || z != 0 || bastype.anInt2824 != 0 || bastype.anInt2827 != 0) {
            functionMask |= DISPLACE_X | DISPLACE_Y | DISPLACE_Z;
        }

        boolean hasTint = super.opacity != 0 && client.cycles >= super.tintStartCycle && client.cycles < super.tintEndCycle;
        if (hasTint) {
            functionMask |= TINT;
        }

        int rotation = super.yaw.getRotation();
        Model model = super.models[0] = this.definitions.getAnimatedBodymodel(toolkit, functionMask, TypeLists.BAS, VarProviders.PLAYER_VAR_PROVIDER, seq, moveAnimation, super.sequences, super.equipmentRotations, rotation, this.customisation, getCurrentRenderAnimation(), false);
        if (model == null) {
            return false;
        } 
        super.modelMinY = model.getMinY();
        model.getSize3d();
        rotate(model);
        if (x == 0 && z == 0) {
            hillChange(rotation, getSize() << 9, getSize() << 9, 0, 0);
        } else {
            hillChange(rotation, x, z, bastype.anInt2813, bastype.anInt2790);
            if (super.rotateX != 0) {
                super.models[0].xaxisRotateWithoutNormals(super.rotateX);
            }

            if (super.rotateZ != 0) {
                super.models[0].zaxisRotateWithoutNormals(super.rotateZ);
            }

            if (super.translateY != 0) {
                super.models[0].translate(0, super.translateY, 0);
            }
        }

        if (hasTint) {
            model.tint(super.hue, super.saturation, super.lightness, super.opacity & 0xff);
        }

        displace(toolkit, bastype, initialFunctionMask, x, z, rotation);
        return true;
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
        if (this.definitions != null && (super.particlesInitialized || getAnimatedBodymodel(toolkit, 0))) {
            Matrix43 renderingMatrix = toolkit.getRenderingMatrix();
            renderingMatrix.copyFrom(getProjectionMatrix());
            renderingMatrix.translate(0.0F, -5.0f, 0.0F);
            updateParticles(toolkit, super.models, renderingMatrix, super.particlesInitialized);
            Arrays.fill(super.models, null);
        }
    }

    @Override
    public int getCS2Index() {
        return super.playerIndex + 1;
    }

    public void move(int plane, int baseX, int baseY, boolean stationary, int size) {
        this.plane = super.collisionPlane = (byte) plane;
        if (Resource.mapRegion.getSettingsBits().areRoofsHidden(baseX, baseY)) {
            super.collisionPlane++;
        }

        if (super.currentSequence.hasType() && super.currentSequence.getType().moveType == 1) {
            super.currentAnimations = null;
            super.currentSequence.update(-1);
        }
        for (int index = 0; index < super.spotAnimations.length; index++) {
            if (super.spotAnimations[index].spotAnimationId != -1) {
                SpotAnimType gfxType = TypeLists.SPOT_ANIM.list(super.spotAnimations[index].spotAnimationId);
                if (gfxType.replay && gfxType.sequenceId != -1 && TypeLists.SEQ.list(gfxType.sequenceId).moveType == 1) {
                    super.spotAnimations[index].sequence.update(-1);
                    super.spotAnimations[index].spotAnimationId = -1;
                }
            }
        }

        if (!stationary) {
            int offsetX = baseX - super.movementBaseX[0];
            int offsetY = baseY - super.movementBaseY[0];
            if (offsetX >= -8 && offsetX <= 8 && offsetY >= -8 && offsetY <= 8) {
                if (super.movementStepsLeft < super.movementBaseX.length - 1) {
                    super.movementStepsLeft++;
                }

                for (int index = super.movementStepsLeft; index > 0; --index) {
                    super.movementBaseX[index] = super.movementBaseX[index - 1];
                    super.movementBaseY[index] = super.movementBaseY[index - 1];
                    super.movementTypes[index] = super.movementTypes[index - 1];
                }

                super.movementBaseX[0] = baseX;
                super.movementBaseY[0] = baseY;
                super.movementTypes[0] = MovementType.WALKING.id;
                return;
            }
        }

        super.movementStepsLeft = 0;
        super.previousMovementStepsLeft = 0;
        super.anInt10366 = 0;
        super.movementBaseX[0] = baseX;
        super.movementBaseY[0] = baseY;
        Vector3 abs = Vector3Pool.vector3From(getAbsolutePosition().coords);
        abs.x = ((size << 8) + (super.movementBaseX[0] << 9));
        abs.z = ((size << 8) + (super.movementBaseY[0] << 9));
        setBasePosition(abs);
        abs.pushVectorStack();
        if (super.particleSystem != null) {
            super.particleSystem.onEntityUpdate();
        }
    }

    public boolean hasDefinition() {
        return this.definitions != null;
    }

    @Override
    public int getCurrentRenderAnimation() {
        if (this.currRenderAnim != -1) {
            return this.currRenderAnim;
        } 
        if (this.definitions.alternativeIds != null) {
            NPCType npcType = this.definitions.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
            if (npcType != null && npcType.npcBasId != -1) {
                return npcType.npcBasId;
            }
        }
        return this.definitions.npcBasId;
    }

    public void move(NPCDirection direction, int movementType) {
        int baseX = super.movementBaseX[0];
        int baseY = super.movementBaseY[0];
        switch (direction.value) {
            case 0:
                ++baseY;
                break;
            case 1:
                --baseX;
                ++baseY;
                break;
            case 2:
                ++baseX;
                ++baseY;
                break;
            case 3:
                ++baseX;
                break;
            case 4:
                --baseX;
                --baseY;
                break;
            case 5:
                --baseX;
                break;
            case 6:
                --baseY;
                break;
            case 7:
                ++baseX;
                --baseY;
        }

        if (super.currentSequence.hasType() && super.currentSequence.getType().moveType == 1) {
            super.currentAnimations = null;
            super.currentSequence.update(-1);
        }

        for (int index = 0; index < super.spotAnimations.length; index++) {
            if (super.spotAnimations[index].spotAnimationId != -1) {
                SpotAnimType gfxtype = TypeLists.SPOT_ANIM.list(super.spotAnimations[index].spotAnimationId);
                if (gfxtype.replay && gfxtype.sequenceId != -1 && TypeLists.SEQ.list(gfxtype.sequenceId).moveType == 1) {
                    super.spotAnimations[index].sequence.update(-1);
                    super.spotAnimations[index].spotAnimationId = -1;
                }
            }
        }

        if (super.movementStepsLeft < super.movementBaseX.length - 1) {
            super.movementStepsLeft++;
        }

        for (int step = super.movementStepsLeft; step > 0; --step) {
            super.movementBaseX[step] = super.movementBaseX[step - 1];
            super.movementBaseY[step] = super.movementBaseY[step - 1];
            super.movementTypes[step] = super.movementTypes[step - 1];
        }

        super.movementBaseX[0] = baseX;
        super.movementBaseY[0] = baseY;
        super.movementTypes[0] = (byte) movementType;
    }

    @Override
    public int getIconHeight() {
        if (this.definitions.alternativeIds != null) {
            NPCType npctype = this.definitions.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
            if (npctype != null && npctype.iconHeight != -1) {
                return npctype.iconHeight;
            }
        }

        return this.definitions.iconHeight != -1 ? this.definitions.iconHeight : super.getIconHeight();
    }

    public boolean isVisibile() {
        return this.definitions.visible;
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

    public int getStat(int index) {
        return this.maxStats[index];
    }

    @Override
    public boolean calculateProjection(final AbstractToolkit toolkit, final int cameraX, final int cameraY) {
        if (this.definitions != null && this.getAnimatedBodymodel(toolkit, 131072)) {
            final Matrix43 projectionMatrix = this.getProjectionMatrix();
            boolean projectionCalculated = false;
            for (int index = 0; index < this.models.length; ++index) {
                if (this.models[index] != null) {
                    final boolean notRendered = this.definitions.sizeShift > 0 || (this.definitions.shadowSize != -1 ? this.definitions.shadowSize == 1 : this.definitions.size == 1);
                    final boolean isProjected = this.models[index].calculateProjection(cameraX, cameraY, projectionMatrix, notRendered, this.definitions.sizeShift);
                    if (isProjected) {
                        projectionCalculated = true;
                        break;
                    }
                }
            }
            for (int index = 0; index < this.models.length; ++index) {
                this.models[index] = null;
            }
            return projectionCalculated;
        }
        return false;
    }

    public void method16164(String string_1) {
        int i_5 = GameShell.gameCyclesPerSecond() * client.ENTITY_DEFAULTS.npcMessageDuration;
        setHintDetails(string_1, 0, 0, i_5);
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        if (this.definitions != null && getAnimatedBodymodel(toolkit, 526336)) {
            Matrix43 projectionMatrix = getProjectionMatrix();
            CoordinateSpace absPos = getAbsolutePosition();
            Matrix43 renderingMatrix = toolkit.getRenderingMatrix();
            int i_6 = super.yaw.getRotation();
            Tile class2937 = super.scene.tiles[super.plane][(int) absPos.coords.x >> 9][(int) absPos.coords.z >> 9];
            if (class2937 != null && class2937.groundDecoration != null) {
                int i_8 = super.anInt10325 - class2937.groundDecoration.floorOffsetY;
                super.anInt10325 = (int) (super.anInt10325 - i_8 / 10.0F);
            } else {
                super.anInt10325 = (int) (super.anInt10325 - super.anInt10325 / 10.0F);
            }

            renderingMatrix.copyFrom(projectionMatrix);
            renderingMatrix.translate(0.0F, (-20 - super.anInt10325), 0.0F);
            BasType bastype = getBastype();
            NPCType npctype = this.definitions.alternativeIds != null ? this.definitions.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER) : this.definitions;
            super.hasPreferedPriorities = false;
            RenderedEntity renderedEntity = null;
            if (PreferenceManager.preferences.characterShadows.getValue() == 1 && npctype.shadowed && bastype.rendersShadow) {
                Sequence seq = super.currentSequence.hasType() && super.currentSequence.hasSpeed() ? super.currentSequence : null;
                MovingSequence moveseq = !super.movingSequence.hasType() || super.movingSequence.hasType && seq != null ? null : super.movingSequence;
                Model model = PathingEntity.getAnimatedShadowModel(toolkit, super.rotateX, super.rotateZ, super.translateY, this.definitions.size, super.models[0],
                		this.definitions.shadowColorSrc & 0xffff, this.definitions.shadowColorDst & 0xffff, this.definitions.shadowAlphaSrc & 0xff, this.definitions.shadowAlphaDst & 0xff,
                		moveseq != null ? moveseq : seq);
                if (model != null) {
                    if (super.boundingBoxPolygons == null || super.boundingBoxPolygons.length < super.models.length + 1) {
                        setupBoundingBox(super.models.length + 1);
                    }

                    renderedEntity = RenderedEntityCache.generate(isVisibile());
                    super.hasPreferedPriorities = true;
                    toolkit.setDepthBuffering(false);
                    model.drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons[super.models.length], ModelFunctionMaskGL.EMPTY);
                    toolkit.setDepthBuffering(true);
                }
            }

            if (super.boundingBoxPolygons == null || super.boundingBoxPolygons.length < super.models.length) {
                setupBoundingBox(super.models.length);
            }

            if (renderedEntity == null) {
                renderedEntity = RenderedEntityCache.generate(isVisibile());
            }

            updateParticles(toolkit, super.models, renderingMatrix, false);

            for (int modelIndex = 0; modelIndex < super.models.length; modelIndex++) {
                if (super.models[modelIndex] != null) {
                    if (this.definitions.hasTint) {
                        super.models[modelIndex].tint(this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity);
                    }

                    super.models[modelIndex].drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons[modelIndex], ModelFunctionMaskGL.EMPTY);
                }
            }

            if (super.particleSystem != null) {
                toolkit.renderParticles(super.particleSystem.getAllParticles());
            }

            for (int modelIndex = 0; modelIndex < super.models.length; modelIndex++) {
                if (super.models[modelIndex] != null) {
                    super.hasPreferedPriorities |= super.models[modelIndex].accountForLayers();
                }
                super.models[modelIndex] = null;
            }

            super.drawCycle = client.timesSceneRendered;
            return renderedEntity;
        } 
        return null;
    }

    @Override
    public boolean privateMessagesEnabled() {
        return client.ENTITY_DEFAULTS.npcMessagesEnabled;
    }

    @Override
    public HintDetails getHintDetails() {
        return super.hintDetails != null && super.hintDetails.text == null ? null : super.hintDetails;
    }

    public void setDirection(NPCType npcType) {
        if (npcType != this.definitions && MiniMenu.isOpen && MiniMenu.containsEntryForNpc(super.playerIndex)) {
            MiniMenu.close();
        }

        this.definitions = npcType;
        if (this.definitions != null) {
            this.modifiedName = this.definitions.name;
            this.modifiedCbLvl = this.definitions.combatLevel;
        }

        if (super.particleSystem != null) {
            super.particleSystem.onEntityUpdate();
        }
    }


    public int getCurrentStat(int statIndex) {
        return this.currentStats[statIndex];
    }


    public void updateStats(int statIndex, int max, int current) {
        this.maxStats[statIndex] = max;
        this.currentStats[statIndex] = current;
    }

    @Override
    public int getOverheadSprite() {
        if (this.definitions.alternativeIds != null) {
            NPCType npcType = this.definitions.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
            if (npcType != null && npcType.overheadSprite != -1) {
                return npcType.overheadSprite;
            }
        }

        return this.definitions.overheadSprite;
    }
}
