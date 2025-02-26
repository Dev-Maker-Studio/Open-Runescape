package com.jagex.game.runetek5.entity.pathingentity.player;

import java.util.Arrays;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeCustomisation;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.MovingSequence;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.graphics.hint.HintArrow;
import com.jagex.game.runetek5.graphics.hint.HintDetails;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.interaction.FriendList;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.scene.RenderedEntityCache;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.sound.player.area.AmbientSound;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.vardomain.VarProviders;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class PlayerEntity extends PathingEntity {

    public int basePositionX;
    public int basePositionY;
    public byte male;
    public int skullId = -1;
    public int prayerHeadIconId = -1;
    public int combatLevel;
    public int cbLevelWithSumm;
    public int cbLevelRelated = -1;
    public int elo;
    public int faceDirection = -1;
    public boolean aBool10573;
    public int teamId;
    public int walkingSoundEffectId = -1;
    public int teleportingSoundEffectId = -1;
    public int defaultSoundEffectId = -1;
    public int runningSoundEffectId = -1;
    public boolean instrumentSoundEffect;
    public int isNpc;
    public int ambientSoundVolume = 255;
    public boolean aBool10568;
    /**
     * Instead of a dot a 'P' is drawn
     * on the minimap
     * (presumably for player moderators
     * although I'm not 100% sure)
     */
    public boolean pMapdot;
    public boolean hidden;
    public boolean reduceAttackPriority;
    public final ConfigNodeMap playerConfigs = new ConfigNodeMap();
    public String username;
    public String displayName;
    public PlayerAppearance playerAppearance;
    public String prefixTitle;
    public String postfixTitle;
    public int renderEmote;
	public static boolean sceneUploaded;

    public PlayerEntity(Scene scene) {
        super(scene);
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit graphicalrenderer1) {
        return null;
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
        if (this.playerAppearance != null && (super.particlesInitialized || method16133(toolkit, 0))) {
            Matrix43 renderingMatrix = toolkit.getRenderingMatrix();
            renderingMatrix.translateFromCoordspace(getAbsolutePosition());
            renderingMatrix.translate(0.0F, -5.0f, 0.0F);
            updateParticles(toolkit, super.models, renderingMatrix, super.particlesInitialized);
            Arrays.fill(super.models, null);
        }
    }

    public boolean hasAppearance() {
        return this.playerAppearance != null;
    }


    public String getUsernameWithTitle() {
        String fullUsername = "";
        if (this.prefixTitle != null) {
            fullUsername = this.prefixTitle;
        }

        fullUsername += this.username;
        if (this.postfixTitle != null) {
            fullUsername += this.postfixTitle;
        }

        return fullUsername;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void move(int baseX, int baseY, byte movementType) {
        if (super.currentSequence.hasType() && super.currentSequence.getType().moveType == 1) {
            super.currentAnimations = null;
            super.currentSequence.update(-1);
        }

        for (int index = 0; index < super.spotAnimations.length; index++) {
            if (super.spotAnimations[index].spotAnimationId != -1) {
                SpotAnimType gfx = TypeLists.SPOT_ANIM.list(super.spotAnimations[index].spotAnimationId);
                if (gfx.replay && gfx.sequenceId != -1 && TypeLists.SEQ.list(gfx.sequenceId).moveType == 1) {
                    super.spotAnimations[index].sequence.update(-1);
                    super.spotAnimations[index].spotAnimationId = -1;
                }
            }
        }

        this.faceDirection = -1;
        if (baseX >= 0 && baseX < Resource.mapRegion.getSceneSizeX() && baseY >= 0 && baseY < Resource.mapRegion.getSceneSizeY()) {
            if (super.movementBaseX[0] >= 0 && super.movementBaseX[0] < Resource.mapRegion.getSceneSizeX() && super.movementBaseY[0] >= 0 && super.movementBaseY[0] < Resource.mapRegion.getSceneSizeY()) {
                if (movementType == MovementType.RUNNING.id) {
                    PlayerUpdate.findRoute(this, baseX, baseY, MovementType.RUNNING.id);
                }

                addMovementStep(baseX, baseY, movementType);
            } else {
                move(baseX, baseY);
            }
        } else {
            move(baseX, baseY);
        }
    }

    public void move(int baseX, int baseY) {
        super.movementStepsLeft = 0;
        super.previousMovementStepsLeft = 0;
        super.anInt10366 = 0;
        super.movementBaseX[0] = baseX;
        super.movementBaseY[0] = baseY;
        int size = getSize();
        Vector3 abs = Vector3Pool.vector3From(getAbsolutePosition().coords);
        abs.x = (size * 256 + super.movementBaseX[0] * 512);
        abs.z = (size * 256 + super.movementBaseY[0] * 512);
        setBasePosition(abs);
        abs.pushVectorStack();
        if (this == PlayerUpdate.MY_PLAYER) {
            Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
        }

        if (super.particleSystem != null) {
            super.particleSystem.onEntityUpdate();
        }
    }

    @Override
    public boolean privateMessagesEnabled() {
        return client.ENTITY_DEFAULTS.enablePlayerMessages;
    }

    @Override
    public int getSize() {
        return this.playerAppearance != null && this.playerAppearance.morphism != -1 ? TypeLists.NPC.list(this.playerAppearance.morphism).size : super.getSize();
    }

    @Override
    public int getCurrentRenderAnimation() {
        return this.renderEmote;
    }

    @Override
    public int getOverheadSprite() {
        return -1;
    }

    @Override
    public boolean calculateProjection(AbstractToolkit toolkit, int i_2, int i_3) {
        if (this.playerAppearance != null && method16133(toolkit, 131072)) {
            Matrix43 projectionMatrix = getProjectionMatrix();
            boolean bool6 = false;

            for (int index = 0; index < super.models.length; index++) {
                if (super.models[index] != null && super.models[index].calculateProjection(i_2, i_3, projectionMatrix, true, 0)) {
                    bool6 = true;
                    break;
                }
            }

            for (int index = 0; index < super.models.length; index++) {
                super.models[index] = null;
            }

            return bool6;
        } else {
            return false;
        }
    }

    @Override
    public void castShadow(SceneGraphEntity class521Sub12, int i_3, int i_4, int i_5, boolean bool6) {
        throw new IllegalStateException();
    }

    @Override
    public void removeTileShadow() {
        throw new IllegalStateException();
    }

    public void addMovementStep(int baseX, int baseY, byte movementType) {
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
        super.movementTypes[0] = movementType;
    }

    public boolean method16133(AbstractToolkit toolkit, int initialFunctionMask) {
        int functionMask = initialFunctionMask;
        BasType bastype = getBastype();
        Sequence currSeq = super.currentSequence.hasType() && !super.currentSequence.hasSpeed() ? super.currentSequence : null;
        MovingSequence moveAnimation = !super.movingSequence.hasType() || this.aBool10573 || super.movingSequence.hasType && currSeq != null ? null : super.movingSequence;
        int x = bastype.anInt2786;
        int z = bastype.anInt2829;
        if (x != 0 || z != 0 || bastype.anInt2824 != 0 || bastype.anInt2827 != 0) {
            initialFunctionMask |= DISPLACE_X | DISPLACE_Y | DISPLACE_Z;
        }

        int rotation = super.yaw.getRotation();
        boolean tintModel = super.opacity != 0 && client.cycles >= super.tintStartCycle && client.cycles < super.tintEndCycle;
        if (tintModel) {
            initialFunctionMask |= TINT;
        }

        Model model = super.models[0] = this.playerAppearance.getBodyModel(
        		toolkit, initialFunctionMask, TypeLists.BAS, TypeLists.IDK, TypeLists.NPC, TypeLists.OBJ,
        		VarProviders.PLAYER_VAR_PROVIDER, currSeq, moveAnimation, super.sequences,
                super.equipmentRotations, rotation, TypeLists.EQUIPMENT_DEFAULTS);
        int bodyModelCount = PlayerAppearance.countBodyModels();
        if (GameShell.maxMemory < 96 && bodyModelCount > 50) {
            PlayerEntity.uploadScene();
        }

        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE && bodyModelCount < 50) {
            int unusedSlots;
            for (unusedSlots = 50 - bodyModelCount; unusedSlots > client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES; client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES++) {
                client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA[client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES] = new byte[102400];
            }

            while (unusedSlots < client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES) {
                client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES--;
                client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA[client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES] = null;
            }
        } else if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
            client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA = new byte[50][];
            client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDES = 0;
        }

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
                model.xaxisRotateWithoutNormals(super.rotateX);
            }

            if (super.rotateZ != 0) {
                model.zaxisRotateWithoutNormals(super.rotateZ);
            }

            if (super.translateY != 0) {
                model.translate(0, super.translateY, 0);
            }
        }

        if (tintModel) {
            model.tint(super.hue, super.saturation, super.lightness, super.opacity & 0xff);
        }

        if (!this.aBool10573) {
            displace(toolkit, bastype, functionMask, x, z, rotation);
        }

        return true;
    }

    public void sendChat(String string_1, int i_2, int i_3) {
        setHintDetails(string_1, i_2, i_3, GameShell.gameCyclesPerSecond() * client.ENTITY_DEFAULTS.playerMessageDuration);
    }

    @Override
    public int getCS2Index() {
        return -super.playerIndex - 1;
    }

    public void drawHint(AbstractToolkit toolkit, Matrix43 hintMatrix, long x, long y, int fileId, long hintDimension) {
        long dimension = x * x + y * y;
        if (dimension >= 262144L && dimension <= hintDimension) {
            int rotationY = (int) (Math.atan2(x, y) * 2607.5945876176133D - super.yaw.getRotation()) & 0x3fff;
            Model model = PlayerEntity.createPlayerHint(toolkit, rotationY, super.rotateX, super.rotateZ, super.translateY, fileId);
            if (model != null) {
                toolkit.setDepthBuffering(false);
                model.drawMatrixThreaded(hintMatrix, null, ModelFunctionMaskGL.EMPTY);
                toolkit.setDepthBuffering(true);
            }
        }
    }
    
    public static Model createPlayerHint(AbstractToolkit toolkit, int rotationY, int rotationX, int rotationZ, int translateY, int fileId) {
        Model model = (Model) HintArrow.hintCache.get(fileId);
        short modelFlags = 2055;
        if (model == null) {
            Mesh mesh = Mesh.fromResource(Resource.MODEL, fileId);
            if (mesh == null) {
                return null;
            }
            if (mesh.version < 13) {
                mesh.upscale();
            }
            model = toolkit.createModel(mesh, modelFlags, HintArrow.hintPreferenceFlags, 64, 768);
            HintArrow.hintCache.put(model, fileId);
        }
        model = model.copy((byte) 6, modelFlags, true);
        if (rotationY != 0) {
            model.yaxisRotateWithoutNormals(rotationY);
        }
        if (rotationX != 0) {
            model.xaxisRotateWithoutNormals(rotationX);
        }
        if (rotationZ != 0) {
            model.zaxisRotateWithoutNormals(rotationZ);
        }
        if (translateY != 0) {
            model.translate(0, translateY, 0);
        }
        return model;
    }

    public void decodeAppearance(Packet buffer) {
        buffer.currentPosition = 0;
        int flags = buffer.readUByte();
        this.male = (byte) (flags & 0x1);
        boolean volumeUnadaptableBefore = this.instrumentSoundEffect;
        this.instrumentSoundEffect = (flags & 0x2) != 0;
        boolean showElo = (flags & 0x4) != 0;
        int size = super.getSize();
        setSize((flags >> 3 & 0x7) + 1);
        boolean titleBefore = (flags & 0x40) != 0;
        boolean titleAfter = (flags & 0x80) != 0;
        Vector3 absolutePosition = Vector3Pool.vector3From(getAbsolutePosition().coords);
        absolutePosition.x += (getSize() - size << 8);
        absolutePosition.z += (getSize() - size << 8);
        setBasePosition(absolutePosition);
        absolutePosition.pushVectorStack();
        if (titleBefore) {
            this.prefixTitle = buffer.readJagString();
        } else {
            this.prefixTitle = null;
        }

        if (titleAfter) {
            this.postfixTitle = buffer.readJagString();
        } else {
            this.postfixTitle = null;
        }

        this.skullId = buffer.readByte();
        this.prayerHeadIconId = buffer.readByte();
        this.hidden = buffer.readByte() == 1;
        if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LIVE && client.PLAYER_RIGHTS >= 2) {
            this.hidden = false;
        }

        int currentMorphId = -1;
        this.teamId = 0;
        int[] bodyAppearances = new int[TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length];
        ItemTypeCustomisation[] meshModifiers = new ItemTypeCustomisation[TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length];
        ItemType[] wornItems = new ItemType[TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length];

        for (int slot = 0; slot < TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length; slot++) {
            if (TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots[slot] != 1) {
            	int i_15 = buffer.readUByte();
                if (i_15 == 0) {
                    bodyAppearances[slot] = 0;
                } else {
                	int i_16 = buffer.readUByte();
                    int itemId = i_16 + (i_15 << 8);
                    if (slot == 0 && itemId == 65535) {
                        currentMorphId = buffer.readBigSmart();
                        this.teamId = buffer.readUByte();
                        break;
                    }

                    if (itemId >= 16384) {
                        itemId -= 16384;
                        bodyAppearances[slot] = itemId | 0x40000000;
                        wornItems[slot] = TypeLists.OBJ.list(itemId);
                        int team = wornItems[slot].teamId;
                        if (team != 0) {
                            this.teamId = team;
                        }
                    } else {
                        bodyAppearances[slot] = itemId - 256 | -2147483648;
                    }
                }
            }
        }

        if (currentMorphId == -1) {
        	int functionMask = buffer.readUShort();
        	int count = 0;
            for (int slot = 0; slot < TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length; slot++) {
                if (TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots[slot] == 0) {
                    if ((functionMask & 1 << count) != 0) {
                        meshModifiers[slot] = ItemTypeCustomisation.decodeItemEffects(wornItems[slot], buffer);
                    }
                    count++;
                }
            }
        }

        int[] staticRecolorDst = new int[10];
        for (int slot = 0; slot < 10; slot++) {
        	int color = buffer.readUByte();
            if (PlayerAppearance.recolorPaletteDst.length < 1 || color < 0 || color >= PlayerAppearance.recolorPaletteDst[slot][0].length) {
                color = 0;
            }
            staticRecolorDst[slot] = color;
        }

        this.renderEmote = buffer.readUShort();
        this.username = buffer.readString();
        this.displayName = this.username;
        if (this == PlayerUpdate.MY_PLAYER) {
            PlayerUpdate.MY_PLAYER_USERNAME = this.username;
        }

        this.combatLevel = buffer.readUByte();
        if (showElo) {
            this.elo = buffer.readUShort();
            if (this.elo == 65535) {
                this.elo = -1;
            }

            this.cbLevelWithSumm = this.combatLevel;
            this.cbLevelRelated = -1;
        } else {
            this.elo = 0;
            this.cbLevelWithSumm = buffer.readUByte();
            this.cbLevelRelated = buffer.readUByte();
            if (this.cbLevelRelated == 255) {
                this.cbLevelRelated = -1;
            }
        }

        int i_15 = this.isNpc;
        this.isNpc = buffer.readUByte();
        if (this.isNpc != 0) {
        	int walk = this.walkingSoundEffectId;
            int teleport = this.teleportingSoundEffectId;
            int def = this.defaultSoundEffectId;
            int run = this.runningSoundEffectId;
            int ambient = this.ambientSoundVolume;
            this.walkingSoundEffectId = buffer.readUShort();
            this.teleportingSoundEffectId = buffer.readUShort();
            this.defaultSoundEffectId = buffer.readUShort();
            this.runningSoundEffectId = buffer.readUShort();
            this.ambientSoundVolume = buffer.readUByte();
            if (volumeUnadaptableBefore != this.instrumentSoundEffect || i_15 != this.isNpc || walk != this.walkingSoundEffectId || teleport != this.teleportingSoundEffectId || def != this.defaultSoundEffectId || run != this.runningSoundEffectId || ambient != this.ambientSoundVolume) {
                PlayerEntity.createPlayerAmbientSound(this);
            }
        } else {
            AmbientSound.unlinkPlayerSound(this);
        }

        if (this.playerAppearance == null) {
            this.playerAppearance = new PlayerAppearance();
        }

        int morphedIdLastCycle = this.playerAppearance.morphism;
        int[] typeRecolorDst = this.playerAppearance.recolorDstIndices;
        this.playerAppearance.setAppearance(getCurrentRenderAnimation(), bodyAppearances, meshModifiers, staticRecolorDst, this.male == 1, currentMorphId);
        if (currentMorphId != morphedIdLastCycle) {
            absolutePosition = Vector3Pool.vector3From(getAbsolutePosition().coords);
            absolutePosition.x = ((super.movementBaseX[0] << 9) + (getSize() << 8));
            absolutePosition.z = ((super.movementBaseY[0] << 9) + (getSize() << 8));
            setBasePosition(absolutePosition);
            absolutePosition.pushVectorStack();
        }

        if (super.playerIndex == PlayerUpdate.myPlayerIndex && typeRecolorDst != null) {
            for (int i = 0; i < staticRecolorDst.length; i++) {
                if (staticRecolorDst[i] != typeRecolorDst[i]) {
                    TypeLists.OBJ.resetRecolorCache();
                    break;
                }
            }
        }

        if (super.particleSystem != null) {
            super.particleSystem.onEntityUpdate();
        }

        if (super.movingSequence.hasType() && super.movingSequence.hasType) {
            BasType basType = getBastype();
            if (!basType.isStandAnimation(super.movingSequence.getId())) {
                super.movingSequence.update(-1);
                super.movingSequence.hasType = false;
            }
        }
    }


    @Override
    public boolean hasTileShadow() {
        return false;
    }

    @Override
    public RenderedEntity draw(AbstractToolkit activeToolkit) {
        if (this.playerAppearance != null && method16133(activeToolkit, 2048)) {
            Matrix43 renderingMatrix = activeToolkit.getRenderingMatrix();
            Matrix43 projectionMatrix = getProjectionMatrix();
            CoordinateSpace absolute = getAbsolutePosition();
            int rotation = super.yaw.getRotation();
            Tile occupiedTile = super.scene.tiles[super.plane][(int) absolute.coords.x >> 9][(int) absolute.coords.z >> 9];
            if (occupiedTile != null && occupiedTile.groundDecoration != null) {
                int i_8 = super.anInt10325 - occupiedTile.groundDecoration.floorOffsetY;
                super.anInt10325 = (int) (super.anInt10325 - i_8 / 10.0F);
            } else {
                super.anInt10325 = (int) (super.anInt10325 - super.anInt10325 / 10.0F);
            }

            renderingMatrix.copyFrom(projectionMatrix);
            renderingMatrix.translate(0.0F, (-20 - super.anInt10325), 0.0F);
            RenderedEntity renderedEntity = null;
            super.hasPreferedPriorities = false;
            if (PreferenceManager.preferences.characterShadows.getValue() == 1) {
                BasType bastype = getBastype();
                if (bastype.rendersShadow && (this.playerAppearance.morphism == -1 || TypeLists.NPC.list(this.playerAppearance.morphism).shadowed)) {
                    Sequence shadowSequence = super.currentSequence.hasType() && super.currentSequence.hasSpeed() ? super.currentSequence : null;
                    MovingSequence shadowMovement = super.movingSequence.hasType() && (!super.movingSequence.hasType || shadowSequence == null) ? super.movingSequence : null;
                    Model shadowModel = getAnimatedShadowModel(activeToolkit, super.rotateX, super.rotateZ, super.translateY, 1, super.models[0], 0, 0, 160, 240, shadowMovement != null ? shadowMovement : shadowSequence);
                    if (shadowModel != null) {
                        if (super.boundingBoxPolygons == null || super.boundingBoxPolygons.length < super.models.length + 1) {
                            setupBoundingBox(super.models.length + 1);
                        }

                        renderedEntity = RenderedEntityCache.generate(true);
                        super.hasPreferedPriorities = true;
                        activeToolkit.setDepthBuffering(false);
                        shadowModel.drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons[super.models.length], ModelFunctionMaskGL.EMPTY);
                        activeToolkit.setDepthBuffering(true);
                    }
                }
            }

            if (this == PlayerUpdate.MY_PLAYER) {
                for (int index = PlayerUpdate.HINT_ARROWS.length - 1; index >= 0; --index) {
                    HintArrow hintArrow = PlayerUpdate.HINT_ARROWS[index];
                    if (hintArrow != null && hintArrow.modelId != -1) {
                        if (hintArrow.targetType == 1) {
                            ObjectNode objectNode = client.NPC_MAP.get(hintArrow.targetIndex);
                            if (objectNode != null) {
                                NpcEntity npcEntity = (NpcEntity) objectNode.value;
                                Vector3 abs = Vector3Pool.delta(npcEntity.getAbsolutePosition().coords, PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords);
                                int absX = (int) abs.x;
                                int absY = (int) abs.z;
                                drawHint(activeToolkit, renderingMatrix, absX, absY, hintArrow.modelId, 92160000L);
                            }
                        }

                        if (hintArrow.targetType == 2) {
                            Vector3 abs = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
                            long hintDrawX = hintArrow.x - (int) abs.x;
                            long hintDrawY = hintArrow.y - (int) abs.z;
                            long hintDimension = (long) hintArrow.distance << 9;
                            hintDimension *= hintDimension;
                            drawHint(activeToolkit, renderingMatrix, hintDrawX, hintDrawY, hintArrow.modelId, hintDimension);
                        }

                        if (hintArrow.targetType == 10 && hintArrow.targetIndex >= 0 && hintArrow.targetIndex < PlayerUpdate.players.length) {
                            PlayerEntity player = PlayerUpdate.players[hintArrow.targetIndex];
                            if (player != null) {
                                Vector3 delta = Vector3Pool.delta(player.getAbsolutePosition().coords, PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords);
                                int x = (int) delta.x;
                                int y = (int) delta.z;
                                drawHint(activeToolkit, renderingMatrix, x, y, hintArrow.modelId, 92160000L);
                            }
                        }
                    }
                }
            }

            renderingMatrix.copyFrom(projectionMatrix);
            renderingMatrix.translate(0.0F, (-5 - super.anInt10325), 0.0F);
            if (super.boundingBoxPolygons == null || super.boundingBoxPolygons.length < super.models.length) {
                setupBoundingBox(super.models.length);
            }

            if (renderedEntity == null) {
                renderedEntity = RenderedEntityCache.generate(true);
            }

            updateParticles(activeToolkit, super.models, renderingMatrix, false);

            for (int index = 0; index < super.models.length; index++) {
                if (super.models[index] != null) {
                    super.models[index].drawMatrixThreaded(renderingMatrix, super.boundingBoxPolygons[index], this == PlayerUpdate.MY_PLAYER ? ModelFunctionMaskGL.SMOOTH_EDGES : ModelFunctionMaskGL.EMPTY);
                }
            }

            if (super.particleSystem != null) {
                ParticleWrapper particles = super.particleSystem.getAllParticles();
                activeToolkit.renderParticles(particles);
            }

            for (int index = 0; index < super.models.length; index++) {
                if (super.models[index] != null) {
                    super.hasPreferedPriorities |= super.models[index].accountForLayers();
                }

                super.models[index] = null;
            }

            super.drawCycle = client.timesSceneRendered;
            return renderedEntity;
        } 
        return null;        
    }

    @Override
    public HintDetails getHintDetails() {
        if (super.hintDetails != null) {
            if (super.hintDetails.text == null) {
                return null;
            }
            if (ChatBox.PUBLIC_FILTER == 0 || ChatBox.PUBLIC_FILTER == 3 || ChatBox.PUBLIC_FILTER == 1 && FriendList.contains(this.displayName)) {
                return super.hintDetails;
            }
        }
        return null;
    }

	public static void createPlayerAmbientSound(PlayerEntity player) {
	    AmbientSound ambientSound = (AmbientSound) AmbientSound.PLAYER_AMBIENT_SOUNDS.get(player.playerIndex);
	    if (ambientSound == null) {
	        AmbientSound.createAmbientSoundEffect(player.plane, player.movementBaseX[0], player.movementBaseY[0], 0, null, null, player);
	    } else {
	        ambientSound.load();
	    }
	}

	public static void uploadScene() {
	    if (!sceneUploaded) {
	        MapRegion.upload(Resource.mapRegion.getScene().surfaceTiles);
	        if (Resource.mapRegion.getScene().waterTiles != null) {
	            MapRegion.upload(Resource.mapRegion.getScene().waterTiles);
	        }
	        sceneUploaded = true;
	    }
	}

}
