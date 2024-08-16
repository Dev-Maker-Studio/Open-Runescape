package com.jagex.game.runetek5.entity.pathingentity;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenuBuilder;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.hitbar.HitbarType;
import com.jagex.game.runetek5.config.hitsplat.HitSplatType;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.sequence.*;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.PathingEntitySpotAnimation;
import com.jagex.game.runetek5.entity.EntityNode;
import com.jagex.game.runetek5.entity.EntityNodeList;
import com.jagex.game.runetek5.entity.InteractiveEntityContainer;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.hint.HintDetails;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

/**
 * jag::PathingEntity::AddPickableNode(jag::game::World &,jag::math::Matrix44 const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  000000000012FD30 000000FE 00000038 00000000 R . . . . T .
jag::PathingEntity::AddRouteWaypointFine(jag::game::CoordFine const&,jag::MoveSpeed const*)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           .text  00000000004125F0 00000302 00000038 00000000 R . . . . . .
jag::PathingEntity::Build(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000052C300 0000083E 000000E8 00000000 R . . . . T .
jag::PathingEntity::CalculateWorldPriority(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  00000000000ADAD0 00000006                   R . . . . . .
[X] jag::PathingEntity::CanRender(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  00000000001070B0 0000017E 00000038 00000000 R . . . . T .
jag::PathingEntity::ClearAppearance(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  000000000038A7C0 0000016B 00000028 00000000 R . . . . T .
jag::PathingEntity::Destruct(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  000000000040EB60 000000DB 00000018 00000000 R . . . . T .
jag::PathingEntity::GatherForPasses(jag::graphics::RT7Core &,uint,jag::math::Matrix44 const&,jag::graphics::SceneViewData const&,jag::graphics::EffectState const&,jag::graphics::DrawableQueue &)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000F2AD0 0000038A 00000078 00000008 R . . . . . .
jag::PathingEntity::GetBasID(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            .text  00000000000ADAC0 00000007                   R . . . . . .
jag::PathingEntity::GetDisplayNameWithExtras(bool) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            .text  000000000038E560 00000169 00000028 00000000 R . . . . . .
jag::PathingEntity::GetHeightForLevel(int,jag::math::Vector3 &)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  00000000000F0790 00000063 00000028 00000000 R . . . . . .
jag::PathingEntity::GetOverlayHeight(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000F7270 00000071                   R . . . . . .
jag::PathingEntity::GetPositionGrid(jag::game::CoordGrid &,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      .text  00000000000C9E90 00000066 00000018 00000000 R . . . . T .
jag::PathingEntity::GetTrackableID(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      .text  00000000000ADB20 00000007                   R . . . . . .
jag::PathingEntity::GetTranslation(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  00000000000AC3E0 00000008                   R . . . . T .
jag::PathingEntity::GetWalkSmoothing(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000ADA90 00000003                   R . . . . . .
jag::PathingEntity::GetWorldPriority(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000ADAE0 00000008                   R . . . . . .
jag::PathingEntity::IsInPlayersClan(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     .text  00000000000ADAF0 00000003                   R . . . . . .
jag::PathingEntity::IsInPlayersGroup(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000ADB00 00000003                   R . . . . . .
jag::PathingEntity::IsNonCentredEntity(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          .text  000000000010F3C0 00000329 00000028 00000000 R . . . . T .
[X] jag::PathingEntity::IsSoftHidden(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  00000000000ADB10 00000003                   R . . . . . .
jag::PathingEntity::LineIntersectionSupport(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  00000000000ADAA0 00000006                   R . . . . . .
jag::PathingEntity::MainLogic(jag::graphics::RT7Core &,uint,uint,jag::math::Matrix44 const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          .text  0000000000410F40 00000EE6 00000288 00000000 R . . . . T .
jag::PathingEntity::OnDeviceLost(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .text  00000000000AC3F0 0000000C                   R . . . . T .
jag::PathingEntity::OnDeviceReset(jag::render::DeviceResetState)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      .text  00000000000A54E0 0000001C                   R . . . . . .
jag::PathingEntity::PathingEntity(jag::Client &,int,jag::graphics::GraphNode *,jag::graphics::GraphEntity *,jag::shared_ptr<jag::game::World> const&,jag::game::EntityType,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               .text  00000000003EB640 0000082B 00000078 0000000C R . . . . . .
jag::PathingEntity::ProcessExtendedInfoPackets(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  0000000000337800 0000009E 00000018 00000000 R . . . . T .
jag::PathingEntity::QueueExtendedInfoPacket(jag::Packet &,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  0000000000310E00 00000459 00000068 00000000 R . . . . T .
jag::PathingEntity::RequestAnimationInner(std::array<int,4ul> const&,int,bool,uint,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  000000000040E3E0 00000481 00000058 00000000 R . . . . . .
jag::PathingEntity::RequestReadyAnimation(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000040F0B0 0000029E 00000018 00000000 R . . . . T .
jag::PathingEntity::SelectBaseAnimation(jag::MoveSpeed const*,float)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  000000000040F350 0000076B 00000038 00000000 R . . . . . .
jag::PathingEntity::SetExactMove(int,int,int,int,int,int,int,int,float,uint,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     .text  00000000000D5740 00000162 00000010 00000014 R . . . . T .
jag::PathingEntity::SetFacingGradual(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            .text  00000000001136D0 0000094E 00000058 00000000 R . . . . T .
jag::PathingEntity::SetTranslation(jag::math::Vector3 const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         .text  00000000000BD170 00000361 00000028 00000000 R . . . . . .
jag::PathingEntity::SetTranslationY(float)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            .text  00000000000AC3D0 00000009                   R . . . . T .
jag::PathingEntity::ShouldAnimate(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  00000000000ADAB0 00000006                   R . . . . . .
jag::PathingEntity::TeleportFine(jag::game::CoordFine const&,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  0000000000412900 000003A7 00000048 00000000 R . . . . . .
jag::PathingEntity::TestIntersection(jag::math::Vector3 const&,jag::math::Vector3 const&,int)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         .text  00000000000C80F0 000003C0 00000028 00000000 R . . . . . .
jag::PathingEntity::UpdateAudio(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 .text  0000000000137A80 000001E7 00000038 00000000 R . . . . T .
jag::PathingEntity::UpdateEntity(int,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            .text  0000000000495820 00001B50 00000198 00000000 R . . . . T .
jag::PathingEntity::UseUpWaypoint(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               .text  00000000000D5170 00000062                   R . . . . T .
jag::PathingEntity::~PathingEntity() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          .text  00000000003F4360 000003CC 00000048 00000000 R . . . . T .
jag::PathingEntity::~PathingEntity() [clone]    
 *
 */
public abstract class PathingEntity extends AbstractInteractiveEntity {

    public static final BasType DEFAULT_BASTYPE = new BasType();
    public int playerIndex;
    public int forceMovementT1XOff;
    public byte saturation;
    public int forceMovementT1YOff;
    public int forceMovementT2XOff; 
    public int forceMovementT1Delay;
    public int forceMovementT2YOff;
    public int forceMovementT2Delay;
    public int forceMoveDirection;
    public int drawCycle;
    public byte lightness;
    public byte hue;
    public int lastYawRotation;
    public int turningTicks;
    public int[] equipmentUpdateDetails;
    public int[] equipmentRotations;
    public int movementStepsLeft;
    public int previousMovementStepsLeft;
    public Interface inter;
    public int loadedInterfaceId;
    public boolean isHidden;
    public int anInt10319;
    public int clientCycles;
    /**
     * [J] The draw priority of this pathing entity
     */
    public int worldPriority;
    
    public final int[] activeHitsplatIds;
    public final int[] hitsplatText;
    public final int[] hitsplatCycles;
    public final int[] soakHitsplatIds;
    public final int[] soakHitsplatText;
    public final EntityNodeList entityHitbars;
    public int faceEntity;
    public int[] currentAnimations;
    public final MovingSequence movingSequence;
    public final Sequence currentSequence;
    public int tintStartCycle;
    public int tintEndCycle;
    public byte opacity;
    public int lastUpdate;
    public int turnDirection;
    public final EntityTurnDirection yaw;
    public final EntityTurnDirection roll;
    public final EntityTurnDirection pitch;
    public int anInt10366;
    public int anInt10368;
    public final int[] movementBaseX;
    public final int[] movementBaseY;
    public final byte[] movementTypes;
    
    /**
     * at index 0: main model
     * > index 0: spot anim models
     */
    public final Model[] models;
    public final PathingEntitySpotAnimation[] spotAnimations;
    public final PathingEntitySequence[] sequences;
    public ParticleSystem particleSystem;
    protected boolean particlesInitialized;
    public byte hitCachePtr;
    public int interfaceIdToLoad;
    public int sizeInSquares;
    protected int modelMinY;
    public int anInt10325;
    public boolean hasPreferedPriorities;
    public HintDetails hintDetails;
    protected int rotateX;
    protected int rotateZ;
    protected int translateY;
	public static int preferenceFlags;
	public static final LRUCache PATHING_ENTITY_MODEL_CACHE = new LRUCache(32);

    public PathingEntity(Scene scene) {
        this(scene, 10);
    }

    protected PathingEntity(Scene scene, int maxStepCount) {
        super(scene, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, (byte) 0);
        this.sizeInSquares = 1;
        this.modelMinY = -32768;
        this.isHidden = true;
        this.anInt10319 = 0;
        this.clientCycles = -1;
        this.worldPriority = 0;
        this.anInt10325 = 0;
        this.hitCachePtr = 0;
        this.activeHitsplatIds = new int[client.ENTITY_DEFAULTS.maximumHits];
        this.hitsplatText = new int[client.ENTITY_DEFAULTS.maximumHits];
        this.hitsplatCycles = new int[client.ENTITY_DEFAULTS.maximumHits];
        this.soakHitsplatIds = new int[client.ENTITY_DEFAULTS.maximumHits];
        this.soakHitsplatText = new int[client.ENTITY_DEFAULTS.maximumHits];
        this.entityHitbars = new EntityNodeList();
        this.faceEntity = -1;
        this.currentAnimations = null;
        this.movingSequence = new MovingSequence(this);
        this.currentSequence = new EntitySequence(this);
        this.tintStartCycle = -1;
        this.tintEndCycle = -1;
        this.opacity = 0;
        this.lastUpdate = 0;
        this.turningTicks = 0;
        this.turnDirection = 256;
        this.yaw = new EntityTurnDirection();
        this.roll = new EntityTurnDirection();
        this.pitch = new EntityTurnDirection();
        this.movementStepsLeft = 0;
        this.anInt10366 = 0;
        this.previousMovementStepsLeft = 0;
        this.anInt10368 = 0;
        this.particlesInitialized = false;
        this.hasPreferedPriorities = false;
        this.loadedInterfaceId = -1;
        this.interfaceIdToLoad = -1;
        this.movementBaseX = new int[maxStepCount];
        this.movementBaseY = new int[maxStepCount];
        this.movementTypes = new byte[maxStepCount];
        this.models = new Model[5];
        this.spotAnimations = new PathingEntitySpotAnimation[4];

        for (int plane = 0; plane < 4; plane++) {
            this.spotAnimations[plane] = new PathingEntitySpotAnimation(this);
        }

        this.sequences = new PathingEntitySequence[TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length];
    }

    public static PathingEntity getLargestEntity(int plane, int x, int z) {
        Tile tile = Resource.mapRegion.getScene().tiles[plane][x][z];
        if (tile == null) {
            return null;
        } 
        PathingEntity biggestEntity = null;
        int size = -1;
        for (InteractiveEntityContainer interactives = tile.interactives; interactives != null; interactives = interactives.child) {
            AbstractInteractiveEntity interactive = interactives.interactiveObject;
            if (interactive instanceof PathingEntity pathingEntity) {
                int delta = (pathingEntity.getSize() - 1) * 256 + 252;
                Vector3 absolutePosition = pathingEntity.getAbsolutePosition().coords;
                int minX = (int) absolutePosition.x - delta >> 9;
                int minZ = (int) absolutePosition.z - delta >> 9;
                int maxX = (int) absolutePosition.x + delta >> 9;
                int maxZ = (int) absolutePosition.z + delta >> 9;
                if (minX <= x && minZ <= z && maxX >= x && maxZ >= z) {
                    int size2d = (maxX + 1 - x) * (maxZ + 1 - z);
                    if (size2d > size) {
                        biggestEntity = pathingEntity;
                        size = size2d;
                    }
                }
            }
        }
        return biggestEntity;
    }

    public static void animateMovement(PathingEntity animable, int moveType, int turnDirectIndex, int rotation) {
        BasType basType = animable.getBastype();
        MovingSequence sequence = animable.movingSequence;
        int turnDirection = animable.lastYawRotation - animable.yaw.rotation & 0x3fff;
        if (moveType == MovementType.STATIONARY.id) {
            if (turnDirection == 0 && animable.turningTicks <= 25) {
                if (!sequence.hasType || !basType.isStandAnimation(sequence.getId())) {
                    sequence.animate(basType.getStandSequence());
                    sequence.hasType = sequence.hasType();
                }
            } else if (rotation < 0 && basType.standTurnCcwSequence != -1) {
                sequence.animate(basType.standTurnCcwSequence);
                sequence.hasType = false;
            } else if (rotation > 0 && basType.standTurnCwSequence != -1) {
                sequence.animate(basType.standTurnCwSequence);
                sequence.hasType = false;
            } else if (!sequence.hasType || !basType.isStandAnimation(sequence.getId())) {
                sequence.animate(basType.getStandSequence());
                sequence.hasType = animable.movingSequence.hasType();
            }
        } else if (animable.faceEntity == -1 || turnDirection < 10240 && turnDirection > 2048) {
            if (turnDirection == 0 && animable.turningTicks <= 25) {
                if (moveType == MovementType.RUNNING.id && basType.runningSequence != -1) {
                    sequence.animate(basType.runningSequence);
                } else if (moveType == MovementType.HALF_WALK.id && basType.teleportSequence != -1) {
                    sequence.animate(basType.teleportSequence);
                } else {
                    sequence.animate(basType.walkSequence);
                }
            } else {
                if (moveType == MovementType.RUNNING.id && basType.runningSequence != -1) {
                    if (rotation < 0 && basType.runTurnCcwSequence != -1) {
                        sequence.animate(basType.runTurnCcwSequence);
                    } else if (rotation > 0 && basType.runTurnCwSequence != -1) {
                        sequence.animate(basType.runTurnCwSequence);
                    } else {
                        sequence.animate(basType.runningSequence);
                    }
                } else if (moveType == MovementType.HALF_WALK.id && basType.teleportSequence != -1) {
                    if (rotation < 0 && basType.teleTurnCcwSequence != -1) {
                        sequence.animate(basType.teleTurnCcwSequence);
                    } else if (rotation > 0 && basType.teleTurnCwSequence != -1) {
                        sequence.animate(basType.teleTurnCwSequence);
                    } else {
                        sequence.animate(basType.teleportSequence);
                    }
                } else if (rotation < 0 && basType.walkTurnCcwSequence != -1) {
                    sequence.animate(basType.walkTurnCcwSequence);
                } else if (rotation > 0 && basType.walkTurnCwSequence != -1) {
                    sequence.animate(basType.walkTurnCwSequence);
                } else {
                    sequence.animate(basType.walkSequence);
                }
            }
            sequence.hasType = false;
        } else {
            int walkDirection = client.NPC_TURN_DIRECTIONS[turnDirectIndex] - animable.yaw.rotation & 0x3fff;
            if (moveType == MovementType.RUNNING.id && basType.runningSequence != -1) {
                if (walkDirection > 2048 && walkDirection <= 6144 && basType.runDir1 != -1) {
                    sequence.animate(basType.runDir1);
                } else if (walkDirection >= 10240 && walkDirection < 14336 && basType.runDir2 != -1) {
                    sequence.animate(basType.runDir2);
                } else if (walkDirection > 6144 && walkDirection < 10240 && basType.runDir3 != -1) {
                    sequence.animate(basType.runDir3);
                } else {
                    sequence.animate(basType.runningSequence);
                }
            } else if (moveType == MovementType.HALF_WALK.id && basType.teleportSequence != -1) {
                if (walkDirection > 2048 && walkDirection <= 6144 && basType.teleDir1 != -1) {
                    sequence.animate(basType.teleDir1);
                } else if (walkDirection >= 10240 && walkDirection < 14336 && basType.teleDir2 != -1) {
                    sequence.animate(basType.teleDir2);
                } else if (walkDirection > 6144 && walkDirection < 10240 && basType.teleDir3 != -1) {
                    sequence.animate(basType.teleDir3);
                } else {
                    sequence.animate(basType.teleportSequence);
                }
            } else if (walkDirection > 2048 && walkDirection <= 6144 && basType.walkDir1 != -1) {
                sequence.animate(basType.walkDir1);
            } else if (walkDirection >= 10240 && walkDirection < 14336 && basType.walkDir2 != -1) {
                sequence.animate(basType.walkDir2);
            } else if (walkDirection > 6144 && walkDirection < 10240 && basType.walkDir3 != -1) {
                sequence.animate(basType.walkDir3);
            } else {
                sequence.animate(basType.walkSequence);
            }
            sequence.hasType = false;
        }
    }

    public static void interfaceComponentOnEntity(PathingEntity entity) {
        if (entity instanceof NpcEntity npc) {
            if (npc.definitions != null) {
                MiniMenuBuilder.npcHovered(npc, npc.plane != PlayerUpdate.MY_PLAYER.plane);
            }
        } else if (entity instanceof PlayerEntity player) {
            MiniMenuBuilder.playerHovered(player, player.plane != PlayerUpdate.MY_PLAYER.plane);
        }
    }
    
	public static void forceMovementType1(PathingEntity entity) {
	    int cyclesLeft = entity.forceMovementT1Delay - client.cycles;
	    int i_3 = entity.forceMovementT1XOff * 512 + entity.getSize() * 256;
	    int i_4 = entity.forceMovementT1YOff * 512 + entity.getSize() * 256;
	    Vector3 abs = entity.getAbsolutePosition().coords;
	    entity.setBasePosition(((int) abs.x + (i_3 - (int) abs.x) / cyclesLeft), ((int) abs.y), ((int) abs.z + (i_4 - (int) abs.z) / cyclesLeft));
	    entity.anInt10366 = 0;
	    entity.turn(entity.forceMoveDirection);
	}

    public static void forceMovementType2(PathingEntity entity) {
        Sequence sequence = entity.currentSequence;
        if (entity.forceMovementT2Delay == client.cycles || !sequence.hasType() || sequence.method7585(1)) {
            int duration = entity.forceMovementT2Delay - entity.forceMovementT1Delay;
            int cyclesPassed = client.cycles - entity.forceMovementT1Delay;
            int x1 = entity.forceMovementT1XOff * 512 + entity.getSize() * 256;
            int y1 = entity.forceMovementT1YOff * 512 + entity.getSize() * 256;
            int x2 = entity.forceMovementT2XOff * 512 + entity.getSize() * 256;
            int y2 = entity.forceMovementT2YOff * 512 + entity.getSize() * 256;
            Vector3 abs = entity.getAbsolutePosition().coords;
            entity.setBasePosition(
            		((cyclesPassed * x2 + x1 * (duration - cyclesPassed)) / duration),
            		((int) abs.y), 
            		((cyclesPassed * y2 + y1 * (duration - cyclesPassed)) / duration));
        }
        entity.anInt10366 = 0;
        entity.turn(entity.forceMoveDirection, false);
    }

    public void turn(int rotation, boolean force) {
        BasType baseType = getBastype();
        if (force || baseType.anInt2820 != 0 || this.turnDirection != 0) {
            this.lastYawRotation = rotation & 0x3fff;
            this.yaw.setRotation(this.lastYawRotation);
        }
    }

    @Override
    public boolean hasAlpha() {
        return this.hasPreferedPriorities;
    }

    public abstract int getCS2Index();

    public void updateInterfaceDisplay(int i_1, int i_2, boolean bool3) {
        if (initInterface()) {
            InterfaceRenderer.updateInterfaceDimensions(this.inter.components, -1, i_1, i_2, bool3);
        }

    }

    public int setFacingGradual() { // J
        BasType renderAnimation = getBastype();
        int currentRotation = this.yaw.rotation;
        boolean bool4;
        if (renderAnimation.anInt2820 != 0) {
            bool4 = this.yaw.calculateRotation(this.lastYawRotation, renderAnimation.anInt2820, renderAnimation.anInt2804);
        } else {
            bool4 = this.yaw.calculateRotation(this.lastYawRotation, this.turnDirection, this.turnDirection);
        }

        int rotation = this.yaw.rotation - currentRotation;
        if (rotation != 0) {
            this.turningTicks++;
        } else {
            this.turningTicks = 0;
            this.yaw.setRotation(this.lastYawRotation);
        }

        if (bool4) {
            if (renderAnimation.anInt2825 != 0) {
                if (rotation > 0) {
                    this.roll.calculateRotation(renderAnimation.anInt2824, renderAnimation.anInt2825, renderAnimation.anInt2823);
                } else {
                    this.roll.calculateRotation(-renderAnimation.anInt2824, renderAnimation.anInt2825, renderAnimation.anInt2823);
                }
            }

            if (renderAnimation.anInt2816 != 0) {
                this.pitch.calculateRotation(renderAnimation.anInt2827, renderAnimation.anInt2816, renderAnimation.anInt2815);
            }
        } else {
            if (renderAnimation.anInt2825 != 0) {
                this.roll.calculateRotation(0, renderAnimation.anInt2825, renderAnimation.anInt2823);
            } else {
                this.roll.setRotation(0);
            }

            if (renderAnimation.anInt2816 != 0) {
                this.pitch.calculateRotation(0, renderAnimation.anInt2816, renderAnimation.anInt2815);
            } else {
                this.pitch.setRotation(0);
            }
        }

        return rotation;
    }

    public void method15797(int[] ints_1, int[] ints_2) {
        if (this.equipmentUpdateDetails == null && ints_1 != null) {
            this.equipmentUpdateDetails = new int[TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length];
        } else if (ints_1 == null) {
            this.equipmentUpdateDetails = null;
            return;
        }

        for (int index = 0; index < this.equipmentUpdateDetails.length; index++) {
            this.equipmentUpdateDetails[index] = -1;
        }

        for (int index = 0; index < ints_1.length; index++) {
            int flags = ints_2[index];

            for (int i_6 = 0; i_6 < this.equipmentUpdateDetails.length; flags >>= 1) {
                if ((flags & 0x1) != 0) {
                    this.equipmentUpdateDetails[i_6] = ints_1[index];
                }

                ++i_6;
            }
        }

    }

    public boolean rotateEquipment(int equipIndex, int requestedAngle) {
        if (this.equipmentRotations == null) {
            if (requestedAngle == -1) {
                return true;
            }

            this.equipmentRotations = new int[TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length];
            for (int index = 0; index < TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length; index++) {
                this.equipmentRotations[index] = -1;
            }
        }

        BasType bas = getBastype();
        int angleAdjustment = 256;
        if (bas.turnAngleAdjustment != null && bas.turnAngleAdjustment[equipIndex] > 0) {
            angleAdjustment = bas.turnAngleAdjustment[equipIndex];
        }

        if (requestedAngle == -1) {
            if (this.equipmentRotations[equipIndex] == -1) {
                return true;
            } 
            
            int turnAngle = this.yaw.getRotation();
            int equipAngle = this.equipmentRotations[equipIndex];
            int angleDelta = turnAngle - equipAngle;
            if (angleDelta >= -angleAdjustment && angleDelta <= angleAdjustment) {
                this.equipmentRotations[equipIndex] = -1;
                for (int index = 0; index < TypeLists.EQUIPMENT_DEFAULTS.customizableObjSlots.length; index++) {
                    if (this.equipmentRotations[index] != -1) {
                        return true;
                    }
                }
                this.equipmentRotations = null;
                return true;
            } 
            
            if ((angleDelta <= 0 || angleDelta > 8192) && angleDelta > -8192) {
                this.equipmentRotations[equipIndex] = equipAngle - angleAdjustment & 0x3fff;
            } else {
                this.equipmentRotations[equipIndex] = equipAngle + angleAdjustment & 0x3fff;
            }
            return false;
        } else {
            if (this.equipmentRotations[equipIndex] == -1) {
                this.equipmentRotations[equipIndex] = this.yaw.getRotation();
            }

            int equipAngle = this.equipmentRotations[equipIndex];
            int angleDelta = requestedAngle - equipAngle;
            if (angleDelta >= -angleAdjustment && angleDelta <= angleAdjustment) {
                this.equipmentRotations[equipIndex] = requestedAngle;
                return true;
            } 
            
            if ((angleDelta <= 0 || angleDelta > 8192) && angleDelta > -8192) {
                this.equipmentRotations[equipIndex] = equipAngle - angleAdjustment & 0x3fff;
            } else {
                this.equipmentRotations[equipIndex] = equipAngle + angleAdjustment & 0x3fff;
            }
            return false;
        }
    }

    protected void updateParticles(AbstractToolkit toolkit, Model[] models, Matrix43 particleMatrix, boolean loaded) {
        if (!loaded) {
            int emissiveTriangleCount = 0;
            int modelsWithEmissiveTriangles = 0;
            int effectiveVerticeCount = 0;
            int modelsWithEffectiveVertices = 0;
            int lastEmissiveTriangle = -1;
            int lastEffectiveVertice = -1;
            EmissiveTriangle[][] emissiveTrianglesPerModel = new EmissiveTriangle[models.length][];
            EffectiveVertex[][] effectiveVerticesPerModel = new EffectiveVertex[models.length][];

            for (int index = 0; index < models.length; index++) {
                if (models[index] != null) {
                    models[index].updateParticlePosition(particleMatrix);
                    emissiveTrianglesPerModel[index] = models[index].getEmissiveTriangles();
                    effectiveVerticesPerModel[index] = models[index].getEffectiveVertices();
                    if (emissiveTrianglesPerModel[index] != null) {
                        lastEmissiveTriangle = index;
                        modelsWithEmissiveTriangles++;
                        emissiveTriangleCount += emissiveTrianglesPerModel[index].length;
                    }

                    if (effectiveVerticesPerModel[index] != null) {
                        lastEffectiveVertice = index;
                        modelsWithEffectiveVertices++;
                        effectiveVerticeCount += effectiveVerticesPerModel[index].length;
                    }
                }
            }

            if ((this.particleSystem == null || this.particleSystem.isDisabled) && (modelsWithEmissiveTriangles > 0 || modelsWithEffectiveVertices > 0)) {
                this.particleSystem = ParticleSystem.get(client.cycles, true);
            }

            if (this.particleSystem != null) {
                EmissiveTriangle[] emissiveTriangles;
                if (modelsWithEmissiveTriangles == 1) {
                    emissiveTriangles = emissiveTrianglesPerModel[lastEmissiveTriangle];
                } else {
                    emissiveTriangles = new EmissiveTriangle[emissiveTriangleCount];
                    int position = 0;

                    for (int index = 0; index < models.length; index++) {
                        if (emissiveTrianglesPerModel[index] != null) {
                            System.arraycopy(emissiveTrianglesPerModel[index], 0, emissiveTriangles, position, emissiveTrianglesPerModel[index].length);
                            position += emissiveTrianglesPerModel[index].length;
                        }
                    }
                }

                EffectiveVertex[] effectiveVertices;
                if (modelsWithEffectiveVertices == 1) {
                    effectiveVertices = effectiveVerticesPerModel[lastEffectiveVertice];
                } else {
                    effectiveVertices = new EffectiveVertex[effectiveVerticeCount];
                    int position = 0;

                    for (int index = 0; index < models.length; index++) {
                        if (effectiveVerticesPerModel[index] != null) {
                            System.arraycopy(effectiveVerticesPerModel[index], 0, effectiveVertices, position, effectiveVerticesPerModel[index].length);
                            position += effectiveVerticesPerModel[index].length;
                        }
                    }
                }

                this.particleSystem.performCycle(toolkit, client.cycles, emissiveTriangles, effectiveVertices);
                this.particlesInitialized = true;
            }
        } else if (this.particleSystem != null) {
            this.particleSystem.setCurrentCycle(client.cycles);
        }

        if (this.particleSystem != null) {
            this.particleSystem.setLogicHeight(super.plane);
        }

    }

    public void playSpotAnimation(int reqSpotAnimationId, int bitpackedSpeedHeight, int rotation, int adjustmentType, boolean adjustReplaymode, int spotAnimationIndex) {
        PathingEntitySpotAnimation spotAnimation = this.spotAnimations[spotAnimationIndex];
        int curSpotAnimationId = spotAnimation.spotAnimationId;
        if (reqSpotAnimationId != -1 && curSpotAnimationId != -1) {
            if (curSpotAnimationId == reqSpotAnimationId) {
            	SpotAnimType gfx = TypeLists.SPOT_ANIM.list(reqSpotAnimationId);
                if (gfx.replay && gfx.sequenceId != -1) {
                    SeqType seqtype = TypeLists.SEQ.list(gfx.sequenceId);
                    int loopType = seqtype.loopType;
                    if (loopType == 0) {
                        return;
                    }

                    if (loopType == 2) {
                        spotAnimation.sequence.resetLoops();
                        return;
                    }
                }
            } else {
            	SpotAnimType reqSpotAnimation = TypeLists.SPOT_ANIM.list(reqSpotAnimationId);
                SpotAnimType curSpotAnimation = TypeLists.SPOT_ANIM.list(curSpotAnimationId);
                if (reqSpotAnimation.sequenceId != -1 && curSpotAnimation.sequenceId != -1) {
                    SeqType reqSpotSeqtype = TypeLists.SEQ.list(reqSpotAnimation.sequenceId);
                    SeqType curSpotSeqtype = TypeLists.SEQ.list(curSpotAnimation.sequenceId);
                    if (reqSpotSeqtype.priority < curSpotSeqtype.priority) {
                        return;
                    }
                }
            }
        }

        byte replayMode = 0;
        if (reqSpotAnimationId != -1 && !TypeLists.SPOT_ANIM.list(reqSpotAnimationId).replay) {
            replayMode = 2;
        }

        if (reqSpotAnimationId != -1 && adjustReplaymode) {
            replayMode = 1;
        }

        spotAnimation.spotAnimationId = reqSpotAnimationId;
        spotAnimation.adjustmentType = adjustmentType;
        spotAnimation.height = bitpackedSpeedHeight >> 16;
        spotAnimation.rotation = rotation;
        spotAnimation.sequence.animateSequence(reqSpotAnimationId != -1 ? TypeLists.SPOT_ANIM.list(reqSpotAnimationId).sequenceId : -1, bitpackedSpeedHeight & 0xffff, replayMode, false);
    }

    public void resetXyIndex() {
        this.movementStepsLeft = 0;
        this.previousMovementStepsLeft = 0;
    }

    public void applyHit(int hitsplatId, int hitText, int soakHitsplatId, int soakText, int currentCycles, int delay) {
        if (Loader.X10_HITS_MODE)
            hitText = (int) Math.ceil((double) hitText / 10.0);
        boolean clearCurrentHits = true;
        boolean hasPrerequisites = true;

        for (int index = 0; index < client.ENTITY_DEFAULTS.maximumHits; index++) {
            if (this.hitsplatCycles[index] > currentCycles) {
                clearCurrentHits = false;
            } else {
                hasPrerequisites = false;
            }
        }

        int hitIndex = -1;
        int hitDisplayType = -1;
        int cyclesVisible = 0;
        if (hitsplatId >= 0) {
            HitSplatType hitsplat = TypeLists.HITSPLAT.list(hitsplatId);
            hitDisplayType = hitsplat.displayType;
            cyclesVisible = hitsplat.cyclesVisible;
        }

        if (hasPrerequisites) {
            if (hitDisplayType == -1) {
                return;
            }

            hitIndex = 0;
            int displayedNumber = 0;
            if (hitDisplayType == 0) {
                displayedNumber = this.hitsplatCycles[0];
            } else if (hitDisplayType == 1) {
                displayedNumber = this.hitsplatText[0];
            }

            for (int index = 1; index < client.ENTITY_DEFAULTS.maximumHits; index++) {
                if (hitDisplayType == 0) {
                    if (this.hitsplatCycles[index] < displayedNumber) {
                        hitIndex = index;
                        displayedNumber = this.hitsplatCycles[index];
                    }
                } else if (hitDisplayType == 1 && this.hitsplatText[index] < displayedNumber) {
                    hitIndex = index;
                    displayedNumber = this.hitsplatText[index];
                }
            }

            if (hitDisplayType == 1 && displayedNumber >= hitText) {
                return;
            }
        } else {
            if (clearCurrentHits) {
                this.hitCachePtr = 0;
            }

            for (int hitCount = 0; hitCount < client.ENTITY_DEFAULTS.maximumHits; hitCount++) {
                byte index = this.hitCachePtr;
                this.hitCachePtr = (byte) ((this.hitCachePtr + 1) % client.ENTITY_DEFAULTS.maximumHits);
                if (this.hitsplatCycles[index] <= currentCycles) {
                    hitIndex = index;
                    break;
                }
            }
        }

        if (hitIndex >= 0) {
            this.activeHitsplatIds[hitIndex] = hitsplatId;
            this.hitsplatText[hitIndex] = hitText;
            this.soakHitsplatIds[hitIndex] = soakHitsplatId;
            this.soakHitsplatText[hitIndex] = soakText;
            this.hitsplatCycles[hitIndex] = currentCycles + cyclesVisible + delay;
        }

    }

    public void displayHitbar(int hitbarId, int currentCycle, int lifetimeCycles, int delay, int fromPercentage, int toPercentage) {
        HitbarType hitbarType = TypeLists.HITBAR.list(hitbarId);
        EntityHitbar next = null;
        EntityHitbar current = null;
        int priority = hitbarType.priority;
        int count = 0;

        for (EntityHitbar entityHitbar = (EntityHitbar) this.entityHitbars.getFirst(); entityHitbar != null; entityHitbar = (EntityHitbar) this.entityHitbars.getNext()) {
            count++;
            if (hitbarType == entityHitbar.hitbarType) {
                entityHitbar.updateHitbar(currentCycle + delay, fromPercentage, toPercentage, lifetimeCycles);
                return;
            }

            if (entityHitbar.hitbarType.anInt2446 <= hitbarType.anInt2446) {
                next = entityHitbar;
            }

            if (entityHitbar.hitbarType.priority > priority) {
                current = entityHitbar;
                priority = entityHitbar.hitbarType.priority;
            }
        }

        if (current != null || count < client.ENTITY_DEFAULTS.maxHitbars) {
        	EntityHitbar entityHitbar = new EntityHitbar(hitbarType);
            if (next == null) {
                this.entityHitbars.getPrevious(entityHitbar);
            } else {
                EntityNode.replace(entityHitbar, next);
            }

            entityHitbar.updateHitbar(currentCycle + delay, fromPercentage, toPercentage, lifetimeCycles);
            if (count >= client.ENTITY_DEFAULTS.maxHitbars) {
                current.unlinkNode();
            }
        }

    }

    @Override
    public int getIconHeight() {
        BasType basType = getBastype();
        int y;
        if (basType.anInt2798 != -1) {
            y = basType.anInt2798;
        } else if (this.modelMinY == -32768) {
            y = 200;
        } else {
            y = -this.modelMinY;
        }

        CoordinateSpace absolute = getAbsolutePosition();
        int tileX = (int) absolute.coords.x >> 9;
        int tileZ = (int) absolute.coords.z >> 9;
        if (super.scene != null && tileX >= 1 && tileZ >= 1 && tileX <= Resource.mapRegion.getSceneSizeX() - 1 && tileZ <= Resource.mapRegion.getSceneSizeY() - 1) {
            Tile tile = super.scene.tiles[super.plane][tileX][tileZ];
            if (tile != null && tile.groundDecoration != null) {
                return tile.groundDecoration.floorOffsetY + y;
            }
        }

        return y;
    }

    public int getSize() {
        return this.sizeInSquares;
    }

    @Override
    public void calculatePosition() {
        int offset = (this.sizeInSquares - 1 << 8) + 240;
        Vector3 abs = getAbsolutePosition().coords;
        super.startX = (short) ((int) abs.x - offset >> 9);
        super.startZ = (short) ((int) abs.z - offset >> 9);
        super.endX = (short) (offset + (int) abs.x >> 9);
        super.endZ = (short) (offset + (int) abs.z >> 9);
    }

    @Override
    public boolean disregardTextureMovement() {
        return false;
    }

    public abstract int getCurrentRenderAnimation();

    public abstract HintDetails getHintDetails();

    public void updateHintdetailsOntick() {
        if (this.hintDetails != null && this.hintDetails.text != null) {
            if (--this.hintDetails.ticksLeft == 0) {
                this.hintDetails.text = null;
            }
        }
    }

    public void method15813(int i_1) {
        if (this.interfaceIdToLoad != this.loadedInterfaceId) {
            method15816();
        }

        this.interfaceIdToLoad = i_1;
    }

    public boolean initInterface(int requestedInterface) {
        if (this.loadedInterfaceId == requestedInterface) {
            return true;
        } 
        this.inter = Interface.decodeComponents(requestedInterface, null, null, true);
        if (this.inter == null) {
            return false;
        }
        this.loadedInterfaceId = requestedInterface;
        Component.runComponentOnloadScripts(this.inter.components);
        return true; 
    }

    public void method15816() {
        if (this.loadedInterfaceId != -1) {
            Interface.clearComponents(this.loadedInterfaceId);
            this.inter = null;
            this.loadedInterfaceId = -1;
        }

    }

    public void method15818(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        if (!initInterface()) {
            if (i_1 != -1) {
                client.IS_COMPONENT_ON_TOP[i_1] = true;
            } else {
                for (int i_9 = 0; i_9 < 107; i_9++) {
                    client.IS_COMPONENT_ON_TOP[i_9] = true;
                }
            }
        } else {
            InterfaceComponentRenderer.drawComponents(this.inter.getComponents(), -1, i_2, i_3, i_4, i_5, i_6, i_7, i_1, false);
        }

    }

    protected void rotate(Model model) {
        int angleZ = this.roll.rotation;
        int angleX = this.pitch.rotation;
        if (angleZ != 0 || angleX != 0) {
            int translateY = model.getMinY() / 2;
            model.translate(0, -translateY, 0);
            model.zaxisRotateWithoutNormals(angleZ & 0x3fff);
            model.xaxisRotateWithoutNormals(angleX & 0x3fff);
            model.translate(0, translateY, 0);
        }
    }


    public void setSize(int size) {
        this.sizeInSquares = size;
    }

    protected void displace(AbstractToolkit toolkit, BasType basType, int initialFunctionMask, int displaceX, int displaceZ, int rotation) {
        for (int index = 0; index < this.spotAnimations.length; index++) {
            byte type = 0;
            if (index == 0) {
                type = 2;
            } else if (index == 1) {
                type = 5;
            } else if (index == 2) {
                type = 1;
            } else if (index == 3) {
                type = 7;
            }

            PathingEntitySpotAnimation spotAnim = this.spotAnimations[index];
            if (spotAnim.spotAnimationId != -1 && !spotAnim.sequence.hasSpeed()) {
                SpotAnimType spotAnimType = TypeLists.SPOT_ANIM.list(spotAnim.spotAnimationId);
                boolean fullDisplacement = spotAnimType.contourType == 3 && (displaceX != 0 || displaceZ != 0);
                int functionMask = initialFunctionMask;
                if (fullDisplacement) {
                    functionMask = initialFunctionMask | DISPLACE_X | DISPLACE_Y | DISPLACE_Z;
                } else {
                    if (spotAnim.rotation != 0) {
                        functionMask = initialFunctionMask | DISPLACE_X | DISPLACE_Z;
                    }

                    if (spotAnim.height != 0) {
                        functionMask |= DISPLACE_Y;
                    }

                    if (spotAnim.adjustmentType >= 0) {
                        functionMask |= DISPLACE_X | DISPLACE_Y | DISPLACE_Z;
                    }
                }

                Model model = this.models[index + 1] = spotAnimType.getAnimatedModel(toolkit, functionMask, spotAnim.sequence, type);
                if (model != null) {
                    if (spotAnim.adjustmentType >= 0 && basType.objVerticeTransformations != null && basType.objVerticeTransformations[spotAnim.adjustmentType] != null) {
                        int translateX = 0;
                        int translateY = 0;
                        int translateZ = 0;
                        if (basType.objVerticeTransformations[spotAnim.adjustmentType] != null) {
                            translateX += basType.objVerticeTransformations[spotAnim.adjustmentType][0];
                            translateY += basType.objVerticeTransformations[spotAnim.adjustmentType][1];
                            translateZ += basType.objVerticeTransformations[spotAnim.adjustmentType][2];
                        }

                        if (basType.projectionOffset != null && basType.projectionOffset[spotAnim.adjustmentType] != null) {
                            translateX += basType.projectionOffset[spotAnim.adjustmentType][0];
                            translateY += basType.projectionOffset[spotAnim.adjustmentType][1];
                            translateZ += basType.projectionOffset[spotAnim.adjustmentType][2];
                        }

                        if (translateZ != 0 || translateX != 0) {
                            int equipmentRotation = rotation;
                            if (this.equipmentRotations != null && this.equipmentRotations[spotAnim.adjustmentType] != -1) {
                                equipmentRotation = this.equipmentRotations[spotAnim.adjustmentType];
                            }

                            int angleY = equipmentRotation + spotAnim.rotation * 2048 - rotation & 0x3fff;
                            if (angleY != 0) {
                                model.yaxisRotateWithoutNormals(angleY);
                            }

                            int sin = MathUtils.SINE[angleY];
                            int cos = MathUtils.COSINE[angleY];
                            int tmp = cos * translateX + translateZ * sin >> 14;
                            translateZ = cos * translateZ - translateX * sin >> 14;
                            translateX = tmp;
                        }

                        model.translate(translateX, translateY, translateZ);
                    } else if (spotAnim.rotation != 0) {
                        model.yaxisRotateWithoutNormals(spotAnim.rotation * 2048);
                    }

                    if (spotAnim.height != 0) {
                        model.translate(0, -spotAnim.height << 2, 0);
                    }

                    if (fullDisplacement) {
                        if (this.rotateX != 0) {
                            model.xaxisRotateWithoutNormals(this.rotateX);
                        }

                        if (this.rotateZ != 0) {
                            model.zaxisRotateWithoutNormals(this.rotateZ);
                        }

                        if (this.translateY != 0) {
                            model.translate(0, this.translateY, 0);
                        }
                    }
                }
            } else {
                this.models[index + 1] = null;
            }
        }
    }

    public BasType getBastype() {
        int renderAnimationId = getCurrentRenderAnimation();
        return renderAnimationId == -1 ? DEFAULT_BASTYPE : TypeLists.BAS.list(renderAnimationId);
    }

    public void hideHitbar(int hitbarId) {
        HitbarType hitbarType = TypeLists.HITBAR.list(hitbarId);
        for (EntityHitbar entityHitbar = (EntityHitbar) this.entityHitbars.getFirst(); entityHitbar != null; entityHitbar = (EntityHitbar) this.entityHitbars.getNext()) {
            if (hitbarType == entityHitbar.hitbarType) {
                entityHitbar.unlinkNode();
                break;
            }
        }
    }

    public void turn(int rotation) {
        BasType renderAnimation = getBastype();
        if (renderAnimation.anInt2820 != 0 || this.turnDirection != 0) {
            this.yaw.clamp();
            int delta = rotation - this.yaw.rotation & 0x3fff;
            if (delta > 8192) {
                this.lastYawRotation = this.yaw.rotation - (16384 - delta);
            } else {
                this.lastYawRotation = this.yaw.rotation + delta;
            }
        }
    }

    public void setHintDetails(String text, int colorType, int drawType, int duration) {
        if (this.hintDetails == null) {
            this.hintDetails = new HintDetails();
        }

        this.hintDetails.text = text;
        this.hintDetails.colorType = colorType;
        this.hintDetails.drawType = drawType;
        this.hintDetails.ticksLeft = this.hintDetails.ticksVisible = duration;
    }

    public void method15880(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8) {
        if (initInterface()) {
            InterfaceRenderer.processInterfaceCycle(this.inter, this.inter.getComponents(), -1, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8);
        }
    }

    public abstract boolean privateMessagesEnabled();

    protected void hillChange(int i_1, int i_2, int i_3, int i_4, int i_5) {
        Vector3 vector37 = getAbsolutePosition().coords;
        int i_8 = super.startX + super.endX >> 1;
        int i_9 = super.endZ + super.startZ >> 1;
        int i_10 = MathUtils.SINE[i_1];
        int i_11 = MathUtils.COSINE[i_1];
        int i_12 = -i_2 / 2;
        int i_13 = -i_3 / 2;
        int i_14 = i_11 * i_12 + i_10 * i_13 >> 14;
        int i_15 = i_13 * i_11 - i_10 * i_12 >> 14;
        int i_16 = PathingEntity.calculateAvgHeightInclRoofs(i_14 + (int) vector37.x, i_15 + (int) vector37.z, i_8, i_9, super.plane);
        int i_17 = i_2 / 2;
        int i_18 = -i_3 / 2;
        int i_19 = i_17 * i_11 + i_10 * i_18 >> 14;
        int i_20 = i_11 * i_18 - i_17 * i_10 >> 14;
        int i_21 = PathingEntity.calculateAvgHeightInclRoofs(i_19 + (int) vector37.x, i_20 + (int) vector37.z, i_8, i_9, super.plane);
        int i_22 = -i_2 / 2;
        int i_23 = i_3 / 2;
        int i_24 = i_11 * i_22 + i_10 * i_23 >> 14;
        int i_25 = i_23 * i_11 - i_10 * i_22 >> 14;
        int i_26 = PathingEntity.calculateAvgHeightInclRoofs(i_24 + (int) vector37.x, i_25 + (int) vector37.z, i_8, i_9, super.plane);
        int i_27 = i_2 / 2;
        int i_28 = i_3 / 2;
        int i_29 = i_11 * i_27 + i_28 * i_10 >> 14;
        int i_30 = i_28 * i_11 - i_10 * i_27 >> 14;
        int i_31 = PathingEntity.calculateAvgHeightInclRoofs(i_29 + (int) vector37.x, i_30 + (int) vector37.z, i_8, i_9, super.plane);
        int i_32 = Math.min(i_16, i_21);
        int i_33 = Math.min(i_26, i_31);
        int i_34 = Math.min(i_21, i_31);
        int i_35 = Math.min(i_16, i_26);
        this.rotateX = (int) (Math.atan2(i_32 - i_33, i_3) * 2607.5945876176133D) & 0x3fff;
        this.rotateZ = (int) (Math.atan2(i_35 - i_34, i_2) * 2607.5945876176133D) & 0x3fff;
        int i_36;
        if (this.rotateX != 0 && i_4 != 0) {
            i_36 = 16384 - i_4;
            if (this.rotateX > 8192) {
                if (this.rotateX < i_36) {
                    this.rotateX = i_36;
                }
            } else if (this.rotateX > i_4) {
                this.rotateX = i_4;
            }
        }

        if (this.rotateZ != 0 && i_5 != 0) {
            i_36 = 16384 - i_5;
            if (this.rotateZ > 8192) {
                if (this.rotateZ < i_36) {
                    this.rotateZ = i_36;
                }
            } else if (this.rotateZ > i_5) {
                this.rotateZ = i_5;
            }
        }

        this.translateY = i_31 + i_16;
        if (i_21 + i_26 < this.translateY) {
            this.translateY = i_26 + i_21;
        }

        this.translateY = (this.translateY >> 1) - (int) vector37.y;
    }

    public abstract int getOverheadSprite();

    @Override
    public int getMinY() {
        return this.modelMinY == -32768 ? 0 : this.modelMinY;
    }

    public boolean initInterface() {
        return this.interfaceIdToLoad != -1 && initInterface(this.interfaceIdToLoad);
    }

	public static void sweepPathingEntityModelcache() {
	    synchronized (PATHING_ENTITY_MODEL_CACHE) {
	        PATHING_ENTITY_MODEL_CACHE.clean(5);
	    }
	}

	public static void resetPathingentityModels() {
	    synchronized (PATHING_ENTITY_MODEL_CACHE) {
	        PATHING_ENTITY_MODEL_CACHE.reset();
	    }
	}

	public static void resetCache(int flags) {
	    PathingEntity.preferenceFlags = flags;
	    synchronized (PATHING_ENTITY_MODEL_CACHE) {
	        PATHING_ENTITY_MODEL_CACHE.reset();
	    }
	}

	public static void clearQueue() {
	    synchronized (PathingEntity.PATHING_ENTITY_MODEL_CACHE) {
	        PathingEntity.PATHING_ENTITY_MODEL_CACHE.resize();
	    }
	}

	public static int calculateAvgHeightInclRoofs(int absX, int absZ, int nextAbsX, int nextAbsZ, int plane) {
	    if (Resource.mapRegion.getScene() == null) {
	        return 0;
	    } 
	    if (plane < 3) {
	        SettingsBits settingsBits = Resource.mapRegion.getSettingsBits();
	        int sceneX = absX >> 9;
	        int sceneZ = absZ >> 9;
	        if (nextAbsX >= 0 && nextAbsZ >= 0 && nextAbsX <= Resource.mapRegion.getSceneSizeX() - 1 && nextAbsZ <= Resource.mapRegion.getSceneSizeY() - 1) {
	            if (sceneX >= 1 && sceneZ >= 1 && sceneX <= Resource.mapRegion.getSceneSizeX() - 1 && sceneZ <= Resource.mapRegion.getSceneSizeY() - 1) {
	                boolean toggleRoofs = (settingsBits.tileFunctionMasks[1][absX >> 9][absZ >> 9] & 0x2) != 0;
	                boolean bool10;
	                boolean bool11;
	                if ((absX & 0x1ff) == 0) {
	                    bool10 = (settingsBits.tileFunctionMasks[1][sceneX - 1][absZ >> 9] & 0x2) != 0;
	                    bool11 = (settingsBits.tileFunctionMasks[1][sceneX][absZ >> 9] & 0x2) != 0;
	                    if (bool11 != bool10) {
	                        toggleRoofs = (settingsBits.tileFunctionMasks[1][nextAbsX][nextAbsZ] & 0x2) != 0;
	                    }
	                }
	                if ((absZ & 0x1ff) == 0) {
	                    bool10 = (settingsBits.tileFunctionMasks[1][absX >> 9][sceneZ - 1] & 0x2) != 0;
	                    bool11 = (settingsBits.tileFunctionMasks[1][absX >> 9][sceneZ] & 0x2) != 0;
	                    if (bool10 != bool11) {
	                        toggleRoofs = (settingsBits.tileFunctionMasks[1][nextAbsX][nextAbsZ] & 0x2) != 0;
	                    }
	                }
	                if (toggleRoofs) {
	                    ++plane;
	                }
	                return Resource.mapRegion.getScene().localHeightmap[plane].getAverageHeight(absX, absZ);
	            } 
	            return 0;
	        } 
	        return 0;
	    } 
	    return Resource.mapRegion.getScene().localHeightmap[plane].getAverageHeight(absX, absZ);
	}

	/**
	 * Revision 727 only draws cheap moving entity
	 * shadows consisting of 3 rings under the entity
	 * alpha decreasing with each ring
	 */
	public static Model getAnimatedShadowModel(AbstractToolkit toolkit, int rotationX, int rotationZ, int translateY, int size, Model shadowModel,
                                               int colorSrc, int colorDst, int alphaSrc, int alphaDst, Sequence animation) {
	    if (shadowModel == null) {
	        return null;
	    } 
        int functionMask = 2055;
        if (animation != null) {
            functionMask |= animation.getFunctionMask();
            functionMask &= -513;
        }
        long hash = ((long) colorDst << 48) + ((long) colorSrc << 32) + (size + ((long) alphaSrc << 16) + ((long) alphaDst << 24));
        Model stationaryModel;
        synchronized (PATHING_ENTITY_MODEL_CACHE) {
            stationaryModel = (Model) PATHING_ENTITY_MODEL_CACHE.get(hash);
        }
        if (stationaryModel == null || toolkit.mismatchingFunctionMask(stationaryModel.getFunctionMask(), functionMask) != 0) {
            if (stationaryModel != null) {
                functionMask = toolkit.mergeFunctionMasks(functionMask, stationaryModel.getFunctionMask());
            }
            byte totalFaces;
            if (size == 1) {
                totalFaces = 9;
            } else if (size == 2) {
                totalFaces = 12;
            } else if (size == 3) {
                totalFaces = 15;
            } else if (size == 4) {
                totalFaces = 18;
            } else {
                totalFaces = 21;
            }
            byte totalCircles = 3;
            int[] offsets = {64, 96, 128};
            Mesh mesh = new Mesh(totalCircles * totalFaces + 1, totalFaces * totalCircles * 2 - totalFaces, 0);
            int inialVertex = mesh.addVertex(0, 0, 0);
            int[][] vertices = new int[totalCircles][totalFaces];
            for (int circleIndex = 0; circleIndex < totalCircles; circleIndex++) {
                int offsetX = offsets[circleIndex];
                int offsetZ = offsets[circleIndex];
                for (int face = 0; face < totalFaces; face++) {
                    int angle = (face << 14) / totalFaces;
                    int vertexX = MathUtils.SINE[angle] * offsetX >> 14;
                    int vertexZ = MathUtils.COSINE[angle] * offsetZ >> 14;
                    vertices[circleIndex][face] = mesh.addVertex(vertexX, 0, vertexZ);
                }
            }
            
            for (int circleIndex = 0; circleIndex < totalCircles; circleIndex++) {
            	// [0] 42 [1] 128 [2] 213
                int dstAlphaModifier = (circleIndex * 256 + 128) / totalCircles;
                // [0] 214 [1] 128 [2] 43
                int srcAlphaModifier = 256 - dstAlphaModifier;
                byte faceAlpha = (byte) (srcAlphaModifier * alphaSrc + dstAlphaModifier * alphaDst >> 8);
                short faceColor = (short) ((
                		(srcAlphaModifier * (colorSrc & 0xfc00) + dstAlphaModifier * colorDst & 0xfc00) & 0xfc0000) +
                		(srcAlphaModifier * (colorSrc & 0x7f) + dstAlphaModifier * (colorDst & 0x7f) & 0x7f00) +
                		(srcAlphaModifier * (colorSrc & 0x380) + dstAlphaModifier * (colorDst & 0x380) & 0x38000) >> 8);
                for (int face = 0; face < totalFaces; face++) {
                    if (circleIndex == 0) {
                        mesh.addFace(
                                inialVertex,
                        		vertices[0][(face + 1) % totalFaces],
                        		vertices[0][face], 
                        		(byte) 1, (byte) -1,
                                faceColor, faceAlpha, (short) -1);
                    } else {
                        mesh.addFace(
                        		vertices[circleIndex - 1][face],
                        		vertices[circleIndex - 1][(face + 1) % totalFaces],
                        		vertices[circleIndex][(face + 1) % totalFaces],
                        		(byte) 1, (byte) -1,
                                faceColor, faceAlpha, (short) -1);
                        mesh.addFace(
                        		vertices[circleIndex - 1][face],
                        		vertices[circleIndex][(face + 1) % totalFaces],
                        		vertices[circleIndex][face],
                        		(byte) 1, (byte) -1,
                                faceColor, faceAlpha, (short) -1);
                    }
                }
            }
            stationaryModel = toolkit.createModel(mesh, functionMask, PathingEntity.preferenceFlags, 64, 768);
            synchronized (PATHING_ENTITY_MODEL_CACHE) {
                PATHING_ENTITY_MODEL_CACHE.put(stationaryModel, hash);
            }
        }
        int minX = shadowModel.getMinX();
        int maxX = shadowModel.getMaxX();
        int minZ = shadowModel.getMinZ();
        int maxZ = shadowModel.getMaxZ();
        if (animation != null) {
            stationaryModel = stationaryModel.copy((byte) 3, functionMask, true);
            stationaryModel.scale(maxX - minX >> 1, 128, maxZ - minZ >> 1);
            stationaryModel.translate(maxX + minX >> 1, 0, minZ + maxZ >> 1);
            animation.animateShadow(stationaryModel);
        } else {
            stationaryModel = stationaryModel.copy((byte) 3, functionMask, true);
            stationaryModel.scale(maxX - minX >> 1, 128, maxZ - minZ >> 1);
            stationaryModel.translate(minX + maxX >> 1, 0, minZ + maxZ >> 1);
        }
        if (rotationX != 0) {
            stationaryModel.xaxisRotateWithoutNormals(rotationX);
        }
        if (rotationZ != 0) {
            stationaryModel.zaxisRotateWithoutNormals(rotationZ);
        }
        if (translateY != 0) {
            stationaryModel.translate(0, translateY, 0);
        }
        return stationaryModel;
	}

	public static void animateMovement(PathingEntity pathingEntity) {
	    MovingSequence moveSequence = pathingEntity.movingSequence;
	    if (moveSequence.hasType() && moveSequence.progressCycle(1) && moveSequence.needsReset()) {
	        if (moveSequence.hasType) {
	            moveSequence.animate(pathingEntity.getBastype().getStandSequence());
	            moveSequence.hasType = moveSequence.hasType();
	        }
	        moveSequence.reset();
	    }
	    for (int index = 0; index < pathingEntity.spotAnimations.length; index++) {
	        if (pathingEntity.spotAnimations[index].spotAnimationId != -1) {
	            Sequence spotanimSequence = pathingEntity.spotAnimations[index].sequence;
	            if (spotanimSequence.hasSpeed()) {
	                SpotAnimType spotType = TypeLists.SPOT_ANIM.list(pathingEntity.spotAnimations[index].spotAnimationId);
	                SeqType seqType = spotanimSequence.getType();
	                if (spotType.replay) {
	                    if (seqType.interruptType == 3) {
	                        if (pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
	                            spotanimSequence.update(-1);
	                            pathingEntity.spotAnimations[index].spotAnimationId = -1;
	                            continue;
	                        }
	                    } else if (seqType.interruptType == 1 && pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
	                        continue;
	                    }
	                }
	            }
	            if (spotanimSequence.progressCycle(1) && spotanimSequence.needsReset()) {
	                spotanimSequence.update(-1);
	                pathingEntity.spotAnimations[index].spotAnimationId = -1;
	            }
	        }
	    }
	    Sequence currentSequence = pathingEntity.currentSequence;
	    if (currentSequence.hasType()) {
	        Update: {
	            SeqType currentSeqType = currentSequence.getType();
	            if (currentSeqType.interruptType == 3) {
	                if (pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
	                    pathingEntity.currentAnimations = null;
	                    currentSequence.update(-1);
	                    break Update;
	                }
	            } else if (currentSeqType.interruptType == 1) {
	                if (pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
	                    currentSequence.setSpeed(1);
	                    break Update;
	                }
	                currentSequence.setSpeed(0);
	            }
	            if (currentSequence.progressCycle(1) && currentSequence.needsReset()) {
	                pathingEntity.currentAnimations = null;
	                currentSequence.update(-1);
	            }
	        }
	    }
	    for (int index = 0; index < pathingEntity.sequences.length; index++) {
	        PathingEntitySequence entitySeq = pathingEntity.sequences[index];
	        if (entitySeq != null) {
	            if (entitySeq.anInt10065 > 0) {
	                entitySeq.anInt10065--;
	            } else if (entitySeq.progressCycle(1) && entitySeq.needsReset()) {
	                pathingEntity.sequences[index] = null;
	            }
	        }
	    }
	}

}
