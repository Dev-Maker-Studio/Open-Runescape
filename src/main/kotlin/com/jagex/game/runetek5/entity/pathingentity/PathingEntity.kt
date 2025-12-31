package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.menu.npcHovered
import com.jagex.game.runetek5.client.menu.playerHovered
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.hitbar.HitbarType
import com.jagex.game.runetek5.config.hitsplat.HitSplatType
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.sequence.*
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.PathingEntitySpotAnimation
import com.jagex.game.runetek5.entity.EntityNode
import com.jagex.game.runetek5.entity.EntityNodeList
import com.jagex.game.runetek5.entity.InteractiveEntityContainer
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordinateSpace
import com.jagex.game.runetek5.scene.SettingsBits
import com.jagex.game.runetek5.scene.tile.Tile
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.atan2
import kotlin.math.ceil

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
abstract class PathingEntity : AbstractInteractiveEntity {

    @JvmField
    var playerIndex: Int = 0
    @JvmField
    var forceMovementT1XOff: Int = 0
    @JvmField
    var saturation: Byte = 0
    @JvmField
    var forceMovementT1YOff: Int = 0
    @JvmField
    var forceMovementT2XOff: Int = 0
    @JvmField
    var forceMovementT1Delay: Int = 0
    @JvmField
    var forceMovementT2YOff: Int = 0
    @JvmField
    var forceMovementT2Delay: Int = 0
    @JvmField
    var forceMoveDirection: Int = 0
    @JvmField
    var drawCycle: Int = 0
    @JvmField
    var lightness: Byte = 0
    @JvmField
    var hue: Byte = 0
    @JvmField
    var lastYawRotation: Int = 0
    @JvmField
    var turningTicks: Int = 0
    @JvmField
    var equipmentUpdateDetails: IntArray? = null
    @JvmField
    var equipmentRotations: IntArray? = null
    @JvmField
    var movementStepsLeft: Int = 0
    @JvmField
    var previousMovementStepsLeft: Int = 0
    @JvmField
    var inter: Interface? = null
    @JvmField
    var loadedInterfaceId: Int = 0
    @JvmField
    var isHidden: Boolean = false
    @JvmField
    var anInt10319: Int = 0
    @JvmField
    var clientCycles: Int = 0
    /**
     * [J] The draw priority of this pathing entity
     */
    @JvmField
    var worldPriority: Int = 0

    @JvmField
    val activeHitsplatIds: IntArray
    @JvmField
    val hitsplatText: IntArray
    @JvmField
    val hitsplatCustomText: Array<String?>
    @JvmField
    val hitsplatCycles: IntArray
    @JvmField
    val soakHitsplatIds: IntArray
    @JvmField
    val soakHitsplatText: IntArray
    @JvmField
    val soakHitsplatCustomText: Array<String?>
    @JvmField
    val entityHitbars: EntityNodeList<EntityHitbar>
    @JvmField
    var faceEntity: Int = 0
    @JvmField
    var currentAnimations: IntArray? = null
    @JvmField
    val movingSequence: MovingSequence
    @JvmField
    val currentSequence: Sequence
    @JvmField
    var tintStartCycle: Int = 0
    @JvmField
    var tintEndCycle: Int = 0
    @JvmField
    var opacity: Byte = 0
    @JvmField
    var lastUpdate: Int = 0
    @JvmField
    var turnDirection: Int = 0
    @JvmField
    val yaw: EntityTurnDirection
    @JvmField
    val roll: EntityTurnDirection
    @JvmField
    val pitch: EntityTurnDirection
    @JvmField
    var anInt10366: Int = 0
    @JvmField
    var anInt10368: Int = 0
    @JvmField
    val movementBaseX: IntArray
    @JvmField
    val movementBaseY: IntArray
    @JvmField
    val movementTypes: ByteArray

    /**
     * at index 0: main model
     * > index 0: spot anim models
     */
    @JvmField
    val models: Array<Model?>
    @JvmField
    val spotAnimations: Array<PathingEntitySpotAnimation>
    @JvmField
    val sequences: Array<PathingEntitySequence?>
    @JvmField
    var particleSystem: ParticleSystem? = null
    @JvmField
    protected var particlesInitialized: Boolean = false
    @JvmField
    var hitCachePtr: Byte = 0
    @JvmField
    var interfaceIdToLoad: Int = 0
    @JvmField
    var sizeInSquares: Int = 0
    @JvmField
    protected var modelMinY: Int = 0
    @JvmField
    var anInt10325: Int = 0
    @JvmField
    var hasPreferedPriorities: Boolean = false
    @JvmField
    var hintDetails: HintDetails? = null
    @JvmField
    protected var rotateX: Int = 0
    @JvmField
    protected var rotateZ: Int = 0
    @JvmField
    protected var translateY: Int = 0

    constructor(scene: Scene?) : this(scene, 10)

    protected constructor(scene: Scene?, maxStepCount: Int) : super(scene, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0.toByte()) {
        this.sizeInSquares = 1
        this.modelMinY = -32768
        this.isHidden = true
        this.anInt10319 = 0
        this.clientCycles = -1
        this.worldPriority = 0
        this.anInt10325 = 0
        this.hitCachePtr = 0
        this.activeHitsplatIds = IntArray(client.ENTITY_DEFAULTS.maximumHits)
        this.hitsplatText = IntArray(client.ENTITY_DEFAULTS.maximumHits)
        this.hitsplatCustomText = arrayOfNulls(client.ENTITY_DEFAULTS.maximumHits)
        this.hitsplatCycles = IntArray(client.ENTITY_DEFAULTS.maximumHits)
        this.soakHitsplatIds = IntArray(client.ENTITY_DEFAULTS.maximumHits)
        this.soakHitsplatText = IntArray(client.ENTITY_DEFAULTS.maximumHits)
        this.soakHitsplatCustomText = arrayOfNulls(client.ENTITY_DEFAULTS.maximumHits)
        this.entityHitbars = EntityNodeList<EntityHitbar>()
        this.faceEntity = -1
        this.currentAnimations = null
        this.movingSequence = MovingSequence(this)
        this.currentSequence = EntitySequence(this)
        this.tintStartCycle = -1
        this.tintEndCycle = -1
        this.opacity = 0
        this.lastUpdate = 0
        this.turningTicks = 0
        this.turnDirection = 256
        this.yaw = EntityTurnDirection()
        this.roll = EntityTurnDirection()
        this.pitch = EntityTurnDirection()
        this.movementStepsLeft = 0
        this.anInt10366 = 0
        this.previousMovementStepsLeft = 0
        this.anInt10368 = 0
        this.particlesInitialized = false
        this.hasPreferedPriorities = false
        this.loadedInterfaceId = -1
        this.interfaceIdToLoad = -1
        this.movementBaseX = IntArray(maxStepCount)
        this.movementBaseY = IntArray(maxStepCount)
        this.movementTypes = ByteArray(maxStepCount)
        this.models = arrayOfNulls(5)
        this.spotAnimations = Array(4) { PathingEntitySpotAnimation(this) }
        this.sequences = arrayOfNulls(TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.size)
    }

    fun turn(rotation: Int, force: Boolean) {
        val baseType = getBastype()
        if (force || baseType.anInt2820 != 0 || this.turnDirection != 0) {
            this.lastYawRotation = rotation and 0x3fff
            this.yaw.setRotation(this.lastYawRotation)
        }
    }

    override fun hasAlpha(): Boolean {
        return this.hasPreferedPriorities
    }

    abstract fun getCS2Index(): Int

    fun updateInterfaceDisplay(i_1: Int, i_2: Int, bool3: Boolean) {
        if (initInterface()) {
            InterfaceRenderer.updateInterfaceDimensions(this.inter!!.components, -1, i_1, i_2, bool3)
        }
    }

    fun setFacingGradual(): Int { // J
        val renderAnimation = getBastype()
        val currentRotation = this.yaw.rotation
        val bool4: Boolean
        if (renderAnimation.anInt2820 != 0) {
            bool4 = this.yaw.calculateRotation(this.lastYawRotation, renderAnimation.anInt2820, renderAnimation.anInt2804)
        } else {
            bool4 = this.yaw.calculateRotation(this.lastYawRotation, this.turnDirection, this.turnDirection)
        }

        val rotation = this.yaw.rotation - currentRotation
        if (rotation != 0) {
            this.turningTicks++
        } else {
            this.turningTicks = 0
            this.yaw.setRotation(this.lastYawRotation)
        }

        if (bool4) {
            if (renderAnimation.anInt2825 != 0) {
                if (rotation > 0) {
                    this.roll.calculateRotation(renderAnimation.anInt2824, renderAnimation.anInt2825, renderAnimation.anInt2823)
                } else {
                    this.roll.calculateRotation(-renderAnimation.anInt2824, renderAnimation.anInt2825, renderAnimation.anInt2823)
                }
            }

            if (renderAnimation.anInt2816 != 0) {
                this.pitch.calculateRotation(renderAnimation.anInt2827, renderAnimation.anInt2816, renderAnimation.anInt2815)
            }
        } else {
            if (renderAnimation.anInt2825 != 0) {
                this.roll.calculateRotation(0, renderAnimation.anInt2825, renderAnimation.anInt2823)
            } else {
                this.roll.setRotation(0)
            }

            if (renderAnimation.anInt2816 != 0) {
                this.pitch.calculateRotation(0, renderAnimation.anInt2816, renderAnimation.anInt2815)
            } else {
                this.pitch.setRotation(0)
            }
        }

        return rotation
    }

    fun method15797(ints_1: IntArray?, ints_2: IntArray?) {
        if (this.equipmentUpdateDetails == null && ints_1 != null) {
            this.equipmentUpdateDetails = IntArray(TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.size)
        } else if (ints_1 == null) {
            this.equipmentUpdateDetails = null
            return
        }

        for (index in this.equipmentUpdateDetails!!.indices) {
            this.equipmentUpdateDetails!![index] = -1
        }

        for (index in ints_1!!.indices) {
            var flags = ints_2!![index]

            var i_6 = 0
            while (i_6 < this.equipmentUpdateDetails!!.size) {
                if ((flags and 0x1) != 0) {
                    this.equipmentUpdateDetails!![i_6] = ints_1[index]
                }
                ++i_6
                flags = flags shr 1
            }
        }
    }

    fun rotateEquipment(equipIndex: Int, requestedAngle: Int): Boolean {
        if (this.equipmentRotations == null) {
            if (requestedAngle == -1) {
                return true
            }

            this.equipmentRotations = IntArray(TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.size)
            for (index in TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.indices) {
                this.equipmentRotations!![index] = -1
            }
        }

        val bas = getBastype()
        var angleAdjustment = 256
        if (bas.turnAngleAdjustment != null && bas.turnAngleAdjustment!![equipIndex] > 0) {
            angleAdjustment = bas.turnAngleAdjustment!![equipIndex]
        }

        if (requestedAngle == -1) {
            if (this.equipmentRotations!![equipIndex] == -1) {
                return true
            }

            val turnAngle = this.yaw.getRotation()
            val equipAngle = this.equipmentRotations!![equipIndex]
            val angleDelta = turnAngle - equipAngle
            if (angleDelta >= -angleAdjustment && angleDelta <= angleAdjustment) {
                this.equipmentRotations!![equipIndex] = -1
                for (index in TypeLists.EQUIPMENT_DEFAULTS!!.customizableObjSlots!!.indices) {
                    if (this.equipmentRotations!![index] != -1) {
                        return true
                    }
                }
                this.equipmentRotations = null
                return true
            }

            if ((angleDelta <= 0 || angleDelta > 8192) && angleDelta > -8192) {
                this.equipmentRotations!![equipIndex] = equipAngle - angleAdjustment and 0x3fff
            } else {
                this.equipmentRotations!![equipIndex] = equipAngle + angleAdjustment and 0x3fff
            }
            return false
        } else {
            if (this.equipmentRotations!![equipIndex] == -1) {
                this.equipmentRotations!![equipIndex] = this.yaw.getRotation()
            }

            val equipAngle = this.equipmentRotations!![equipIndex]
            val angleDelta = requestedAngle - equipAngle
            if (angleDelta >= -angleAdjustment && angleDelta <= angleAdjustment) {
                this.equipmentRotations!![equipIndex] = requestedAngle
                return true
            }

            if ((angleDelta <= 0 || angleDelta > 8192) && angleDelta > -8192) {
                this.equipmentRotations!![equipIndex] = equipAngle - angleAdjustment and 0x3fff
            } else {
                this.equipmentRotations!![equipIndex] = equipAngle + angleAdjustment and 0x3fff
            }
            return false
        }
    }

    protected fun updateParticles(toolkit: AbstractToolkit, models: Array<Model?>, particleMatrix: Matrix43, loaded: Boolean) {
        if (!loaded) {
            var emissiveTriangleCount = 0
            var modelsWithEmissiveTriangles = 0
            var effectiveVerticeCount = 0
            var modelsWithEffectiveVertices = 0
            var lastEmissiveTriangle = -1
            var lastEffectiveVertice = -1
            val emissiveTrianglesPerModel = arrayOfNulls<Array<EmissiveTriangle>>(models.size)
            val effectiveVerticesPerModel = arrayOfNulls<Array<EffectiveVertex>>(models.size)

            for (index in models.indices) {
                if (models[index] != null) {
                    models[index]!!.updateParticlePosition(particleMatrix)
                    emissiveTrianglesPerModel[index] = models[index]!!.getEmissiveTriangles()
                    effectiveVerticesPerModel[index] = models[index]!!.getEffectiveVertices()
                    if (emissiveTrianglesPerModel[index] != null) {
                        lastEmissiveTriangle = index
                        modelsWithEmissiveTriangles++
                        emissiveTriangleCount += emissiveTrianglesPerModel[index]!!.size
                    }

                    if (effectiveVerticesPerModel[index] != null) {
                        lastEffectiveVertice = index
                        modelsWithEffectiveVertices++
                        effectiveVerticeCount += effectiveVerticesPerModel[index]!!.size
                    }
                }
            }

            if ((this.particleSystem == null || this.particleSystem!!.isDisabled) && (modelsWithEmissiveTriangles > 0 || modelsWithEffectiveVertices > 0)) {
                this.particleSystem = ParticleSystem.get(client.cycles, true)
            }

            if (this.particleSystem != null) {
                val emissiveTriangles: Array<EmissiveTriangle>
                if (modelsWithEmissiveTriangles == 1) {
                    emissiveTriangles = emissiveTrianglesPerModel[lastEmissiveTriangle]!!
                } else {
                    emissiveTriangles = Array(emissiveTriangleCount) {
                        // This will be filled by System.arraycopy
                        emissiveTrianglesPerModel[0]!![0]
                    }
                    var position = 0

                    for (index in models.indices) {
                        if (emissiveTrianglesPerModel[index] != null) {
                            System.arraycopy(emissiveTrianglesPerModel[index], 0, emissiveTriangles, position, emissiveTrianglesPerModel[index]!!.size)
                            position += emissiveTrianglesPerModel[index]!!.size
                        }
                    }
                }

                val effectiveVertices: Array<EffectiveVertex>
                if (modelsWithEffectiveVertices == 1) {
                    effectiveVertices = effectiveVerticesPerModel[lastEffectiveVertice]!!
                } else {
                    effectiveVertices = Array(effectiveVerticeCount) {
                        // This will be filled by System.arraycopy
                        effectiveVerticesPerModel[0]!![0]
                    }
                    var position = 0

                    for (index in models.indices) {
                        if (effectiveVerticesPerModel[index] != null) {
                            System.arraycopy(effectiveVerticesPerModel[index], 0, effectiveVertices, position, effectiveVerticesPerModel[index]!!.size)
                            position += effectiveVerticesPerModel[index]!!.size
                        }
                    }
                }

                this.particleSystem!!.performCycle(toolkit, client.cycles.toLong(), emissiveTriangles, effectiveVertices)
                this.particlesInitialized = true
            }
        } else if (this.particleSystem != null) {
            this.particleSystem!!.setCurrentCycle(client.cycles.toLong())
        }

        if (this.particleSystem != null) {
            this.particleSystem!!.setLogicHeight(super.plane.toInt())
        }
    }

    fun playSpotAnimation(reqSpotAnimationId: Int, bitpackedSpeedHeight: Int, rotation: Int, adjustmentType: Int, adjustReplaymode: Boolean, spotAnimationIndex: Int) {
        val spotAnimation = this.spotAnimations[spotAnimationIndex]
        val curSpotAnimationId = spotAnimation.spotAnimationId
        if (reqSpotAnimationId != -1 && curSpotAnimationId != -1) {
            if (curSpotAnimationId == reqSpotAnimationId) {
                val gfx = TypeLists.SPOT_ANIM!!.list(reqSpotAnimationId)
                if (gfx.replay && gfx.sequenceId != -1) {
                    val seqtype = TypeLists.SEQ!!.list(gfx.sequenceId)
                    val loopType = seqtype.loopType
                    if (loopType == 0) {
                        return
                    }

                    if (loopType == 2) {
                        spotAnimation.sequence.resetLoops()
                        return
                    }
                }
            } else {
                val reqSpotAnimation = TypeLists.SPOT_ANIM!!.list(reqSpotAnimationId)
                val curSpotAnimation = TypeLists.SPOT_ANIM!!.list(curSpotAnimationId)
                if (reqSpotAnimation.sequenceId != -1 && curSpotAnimation.sequenceId != -1) {
                    val reqSpotSeqtype = TypeLists.SEQ!!.list(reqSpotAnimation.sequenceId)
                    val curSpotSeqtype = TypeLists.SEQ!!.list(curSpotAnimation.sequenceId)
                    if (reqSpotSeqtype.priority < curSpotSeqtype.priority) {
                        return
                    }
                }
            }
        }

        var replayMode: Byte = 0
        if (reqSpotAnimationId != -1 && !TypeLists.SPOT_ANIM!!.list(reqSpotAnimationId).replay) {
            replayMode = 2
        }

        if (reqSpotAnimationId != -1 && adjustReplaymode) {
            replayMode = 1
        }

        spotAnimation.spotAnimationId = reqSpotAnimationId
        spotAnimation.adjustmentType = adjustmentType
        spotAnimation.height = bitpackedSpeedHeight shr 16
        spotAnimation.rotation = rotation
        spotAnimation.sequence.animateSequence(if (reqSpotAnimationId != -1) TypeLists.SPOT_ANIM!!.list(reqSpotAnimationId).sequenceId else -1, (bitpackedSpeedHeight and 0xffff).toInt(), replayMode.toInt(), false)
    }

    fun resetXyIndex() {
        this.movementStepsLeft = 0
        this.previousMovementStepsLeft = 0
    }

    fun applyHit(hitsplatId: Int, hitText: Int, hitCustomText: String?, soakHitsplatId: Int, soakText: Int, soakCustomText: String?, currentCycles: Int, delay: Int) {
        var hitText = hitText
        if (Loader.X10_HITS_MODE)
            hitText = ceil(hitText.toDouble() / 10.0).toInt()

        var clearCurrentHits = true
        var hasPrerequisites = true

        for (index in 0 until client.ENTITY_DEFAULTS.maximumHits) {
            if (this.hitsplatCycles[index] > currentCycles) {
                clearCurrentHits = false
            } else {
                hasPrerequisites = false
            }
        }

        var hitIndex = -1
        var hitDisplayType = -1
        var cyclesVisible = 0
        if (hitsplatId >= 0) {
            val hitsplat = TypeLists.HITSPLAT!!.list(hitsplatId)
            hitDisplayType = hitsplat.displayType
            cyclesVisible = hitsplat.cyclesVisible
        }

        if (hasPrerequisites) {
            if (hitDisplayType == -1) {
                return
            }

            hitIndex = 0
            var displayedNumber = 0
            if (hitDisplayType == 0) {
                displayedNumber = this.hitsplatCycles[0]
            } else if (hitDisplayType == 1) {
                displayedNumber = this.hitsplatText[0]
            }

            for (index in 1 until client.ENTITY_DEFAULTS.maximumHits) {
                if (hitDisplayType == 0) {
                    if (this.hitsplatCycles[index] < displayedNumber) {
                        hitIndex = index
                        displayedNumber = this.hitsplatCycles[index]
                    }
                } else if (hitDisplayType == 1 && this.hitsplatText[index] < displayedNumber) {
                    hitIndex = index
                    displayedNumber = this.hitsplatText[index]
                }
            }

            if (hitDisplayType == 1 && displayedNumber >= hitText) {
                return
            }
        } else {
            if (clearCurrentHits) {
                this.hitCachePtr = 0
            }

            for (hitCount in 0 until client.ENTITY_DEFAULTS.maximumHits) {
                val index = this.hitCachePtr
                this.hitCachePtr = ((this.hitCachePtr + 1) % client.ENTITY_DEFAULTS.maximumHits).toByte()
                if (this.hitsplatCycles[index.toInt()] <= currentCycles) {
                    hitIndex = index.toInt()
                    break
                }
            }
        }

        if (hitIndex >= 0) {
            this.activeHitsplatIds[hitIndex] = hitsplatId
            this.hitsplatText[hitIndex] = hitText
            this.hitsplatCustomText[hitIndex] = hitCustomText
            this.soakHitsplatIds[hitIndex] = soakHitsplatId
            this.soakHitsplatText[hitIndex] = soakText
            this.soakHitsplatCustomText[hitIndex] = soakCustomText
            this.hitsplatCycles[hitIndex] = currentCycles + cyclesVisible + delay
        }
    }

    fun displayHitbar(hitbarId: Int, currentCycle: Int, lifetimeCycles: Int, delay: Int, fromPercentage: Int, toPercentage: Int) {
        val hitbarType = TypeLists.HITBAR!!.list(hitbarId)
        var next: EntityHitbar? = null
        var current: EntityHitbar? = null
        var priority = hitbarType.priority
        var count = 0

        var entityHitbar = this.entityHitbars.getFirst() as EntityHitbar?
        while (entityHitbar != null) {
            count++
            if (hitbarType === entityHitbar.hitbarType) {
                entityHitbar.updateHitbar(currentCycle + delay, fromPercentage, toPercentage, lifetimeCycles)
                return
            }

            if (entityHitbar.hitbarType.anInt2446 <= hitbarType.anInt2446) {
                next = entityHitbar
            }

            if (entityHitbar.hitbarType.priority > priority) {
                current = entityHitbar
                priority = entityHitbar.hitbarType.priority
            }
            entityHitbar = this.entityHitbars.getNext() as EntityHitbar?
        }

        if (current != null || count < client.ENTITY_DEFAULTS.maxHitbars) {
            val newEntityHitbar = EntityHitbar(hitbarType)
            if (next == null) {
                this.entityHitbars.getPrevious(newEntityHitbar)
            } else {
                EntityNode.replace(newEntityHitbar, next)
            }

            newEntityHitbar.updateHitbar(currentCycle + delay, fromPercentage, toPercentage, lifetimeCycles)
            if (count >= client.ENTITY_DEFAULTS.maxHitbars) {
                current!!.unlinkNode()
            }
        }
    }

    override fun getIconHeight(): Int {
        val basType = getBastype()
        val y: Int
        if (basType.anInt2798 != -1) {
            y = basType.anInt2798
        } else if (this.modelMinY == -32768) {
            y = 200
        } else {
            y = -this.modelMinY
        }

        val absolute = getAbsolutePosition()
        val tileX = absolute.coords.x.toInt() shr 9
        val tileZ = absolute.coords.z.toInt() shr 9
        if (super.scene != null && tileX >= 1 && tileZ >= 1 && tileX <= Resource.mapRegion!!.getSceneSizeX() - 1 && tileZ <= Resource.mapRegion!!.getSceneSizeY() - 1) {
            val tile = super.scene!!.tiles[super.plane.toInt()][tileX][tileZ]
            if (tile != null && tile.groundDecoration != null) {
                return tile.groundDecoration!!.floorOffsetY + y
            }
        }

        return y
    }

    open fun getSize(): Int {
        return this.sizeInSquares
    }

    override fun calculatePosition() {
        val offset = (this.sizeInSquares - 1 shl 8) + 240
        val abs = getAbsolutePosition().coords
        super.startX = ((abs.x.toInt() - offset) shr 9).toShort()
        super.startZ = ((abs.z.toInt() - offset) shr 9).toShort()
        super.endX = ((offset + abs.x.toInt()) shr 9).toShort()
        super.endZ = ((offset + abs.z.toInt()) shr 9).toShort()
    }

    override fun disregardTextureMovement(): Boolean {
        return false
    }

    abstract fun getCurrentRenderAnimation(): Int

    abstract fun getHintDetails(): HintDetails?

    fun updateHintdetailsOntick() {
        if (this.hintDetails != null && this.hintDetails!!.text != null) {
            if (--this.hintDetails!!.ticksLeft == 0) {
                this.hintDetails!!.text = null
            }
        }
    }

    fun method15813(i_1: Int) {
        if (this.interfaceIdToLoad != this.loadedInterfaceId) {
            method15816()
        }

        this.interfaceIdToLoad = i_1
    }

    fun initInterface(requestedInterface: Int): Boolean {
        if (this.loadedInterfaceId == requestedInterface) {
            return true
        }
        this.inter = Interface.decodeComponents(requestedInterface, null, null, true)
        if (this.inter == null) {
            return false
        }
        this.loadedInterfaceId = requestedInterface
        Component.runComponentOnloadScripts(this.inter!!.components)
        return true
    }

    fun method15816() {
        if (this.loadedInterfaceId != -1) {
            Interface.clearComponents(this.loadedInterfaceId)
            this.inter = null
            this.loadedInterfaceId = -1
        }
    }

    fun method15818(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        if (!initInterface()) {
            if (i_1 != -1) {
                client.IS_COMPONENT_ON_TOP[i_1] = true
            } else {
                for (i_9 in 0..106) {
                    client.IS_COMPONENT_ON_TOP[i_9] = true
                }
            }
        } else {
            InterfaceComponentRenderer.drawComponents(this.inter!!.getComponents(), -1, i_2, i_3, i_4, i_5, i_6, i_7, i_1, false)
        }
    }

    protected fun rotate(model: Model) {
        val angleZ = this.roll.rotation
        val angleX = this.pitch.rotation
        if (angleZ != 0 || angleX != 0) {
            val translateY = model.getMinY() / 2
            model.translate(0, -translateY, 0)
            model.zaxisRotateWithoutNormals(angleZ and 0x3fff)
            model.xaxisRotateWithoutNormals(angleX and 0x3fff)
            model.translate(0, translateY, 0)
        }
    }

    fun setSize(size: Int) {
        this.sizeInSquares = size
    }

    protected fun displace(toolkit: AbstractToolkit, basType: BasType, initialFunctionMask: Int, displaceX: Int, displaceZ: Int, rotation: Int) {
        for (index in this.spotAnimations.indices) {
            val type: Byte = when (index) {
                0 -> 2
                1 -> 5
                2 -> 1
                3 -> 7
                else -> 0
            }

            val spotAnim = this.spotAnimations[index]
            if (spotAnim.spotAnimationId != -1 && !spotAnim.sequence.hasSpeed()) {
                val spotAnimType = TypeLists.SPOT_ANIM!!.list(spotAnim.spotAnimationId)
                val fullDisplacement = spotAnimType.contourType.toInt() == 3 && (displaceX != 0 || displaceZ != 0)
                var functionMask = initialFunctionMask
                if (fullDisplacement) {
                    functionMask = initialFunctionMask or ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Y or ModelFunctionMask.DISPLACE_Z
                } else {
                    if (spotAnim.rotation != 0) {
                        functionMask = initialFunctionMask or ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Z
                    }

                    if (spotAnim.height != 0) {
                        functionMask = functionMask or ModelFunctionMask.DISPLACE_Y
                    }

                    if (spotAnim.adjustmentType >= 0) {
                        functionMask = functionMask or ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Y or ModelFunctionMask.DISPLACE_Z
                    }
                }

                val model = spotAnimType.getAnimatedModel(toolkit, functionMask, spotAnim.sequence, type)
                this.models[index + 1] = model
                if (model != null) {
                    if (spotAnim.adjustmentType >= 0 && basType.objVerticeTransformations != null && basType.objVerticeTransformations!![spotAnim.adjustmentType] != null) {
                        var translateX = 0
                        var translateY = 0
                        var translateZ = 0
                        if (basType.objVerticeTransformations!![spotAnim.adjustmentType] != null) {
                            translateX += basType.objVerticeTransformations!![spotAnim.adjustmentType]!![0]
                            translateY += basType.objVerticeTransformations!![spotAnim.adjustmentType]!![1]
                            translateZ += basType.objVerticeTransformations!![spotAnim.adjustmentType]!![2]
                        }

                        if (basType.projectionOffset != null && basType.projectionOffset!![spotAnim.adjustmentType] != null) {
                            translateX += basType.projectionOffset!![spotAnim.adjustmentType]!![0]
                            translateY += basType.projectionOffset!![spotAnim.adjustmentType]!![1]
                            translateZ += basType.projectionOffset!![spotAnim.adjustmentType]!![2]
                        }

                        if (translateZ != 0 || translateX != 0) {
                            var equipmentRotation = rotation
                            if (this.equipmentRotations != null && this.equipmentRotations!![spotAnim.adjustmentType] != -1) {
                                equipmentRotation = this.equipmentRotations!![spotAnim.adjustmentType]
                            }

                            val angleY = equipmentRotation + spotAnim.rotation * 2048 - rotation and 0x3fff
                            if (angleY != 0) {
                                model.yaxisRotateWithoutNormals(angleY)
                            }

                            val sin = MathUtils.SINE[angleY]
                            val cos = MathUtils.COSINE[angleY]
                            val tmp = cos * translateX + translateZ * sin shr 14
                            translateZ = cos * translateZ - translateX * sin shr 14
                            translateX = tmp
                        }

                        model.translate(translateX, translateY, translateZ)
                    } else if (spotAnim.rotation != 0) {
                        model.yaxisRotateWithoutNormals(spotAnim.rotation * 2048)
                    }

                    if (spotAnim.height != 0) {
                        model.translate(0, -spotAnim.height shl 2, 0)
                    }

                    if (fullDisplacement) {
                        if (this.rotateX != 0) {
                            model.xaxisRotateWithoutNormals(this.rotateX)
                        }

                        if (this.rotateZ != 0) {
                            model.zaxisRotateWithoutNormals(this.rotateZ)
                        }

                        if (this.translateY != 0) {
                            model.translate(0, this.translateY, 0)
                        }
                    }
                }
            } else {
                this.models[index + 1] = null
            }
        }
    }

    fun getBastype(): BasType {
        val renderAnimationId = getCurrentRenderAnimation()
        return if (renderAnimationId == -1) DEFAULT_BASTYPE else TypeLists.BAS!!.list(renderAnimationId)
    }

    fun hideHitbar(hitbarId: Int) {
        val hitbarType = TypeLists.HITBAR!!.list(hitbarId)
        var entityHitbar = this.entityHitbars.getFirst() as EntityHitbar?
        while (entityHitbar != null) {
            if (hitbarType === entityHitbar.hitbarType) {
                entityHitbar.unlinkNode()
                break
            }
            entityHitbar = this.entityHitbars.getNext() as EntityHitbar?
        }
    }

    fun turn(rotation: Int) {
        val renderAnimation = getBastype()
        if (renderAnimation.anInt2820 != 0 || this.turnDirection != 0) {
            this.yaw.clamp()
            val delta = rotation - this.yaw.rotation and 0x3fff
            if (delta > 8192) {
                this.lastYawRotation = this.yaw.rotation - (16384 - delta)
            } else {
                this.lastYawRotation = this.yaw.rotation + delta
            }
        }
    }

    fun setHintDetails(text: String?, colorType: Int, drawType: Int, duration: Int) {
        if (this.hintDetails == null) {
            this.hintDetails = HintDetails()
        }

        this.hintDetails!!.text = text
        this.hintDetails!!.colorType = colorType
        this.hintDetails!!.drawType = drawType
        this.hintDetails!!.ticksLeft = duration
        this.hintDetails!!.ticksVisible = duration
    }

    fun method15880(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
        if (initInterface()) {
            InterfaceRenderer.processInterfaceCycle(this.inter!!, this.inter!!.getComponents(), -1, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8)
        }
    }

    abstract fun privateMessagesEnabled(): Boolean

    protected fun hillChange(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        val vector37 = getAbsolutePosition().coords
        val i_8 = super.startX + super.endX shr 1
        val i_9 = super.endZ + super.startZ shr 1
        val i_10 = MathUtils.SINE[i_1]
        val i_11 = MathUtils.COSINE[i_1]
        val i_12 = -i_2 / 2
        val i_13 = -i_3 / 2
        val i_14 = i_11 * i_12 + i_10 * i_13 shr 14
        val i_15 = i_13 * i_11 - i_10 * i_12 shr 14
        val i_16 = calculateAvgHeightInclRoofs(i_14 + vector37.x.toInt(), i_15 + vector37.z.toInt(), i_8.toInt(), i_9.toInt(), super.plane.toInt())
        val i_17 = i_2 / 2
        val i_18 = -i_3 / 2
        val i_19 = i_17 * i_11 + i_10 * i_18 shr 14
        val i_20 = i_11 * i_18 - i_17 * i_10 shr 14
        val i_21 = calculateAvgHeightInclRoofs(i_19 + vector37.x.toInt(), i_20 + vector37.z.toInt(), i_8.toInt(), i_9.toInt(), super.plane.toInt())
        val i_22 = -i_2 / 2
        val i_23 = i_3 / 2
        val i_24 = i_11 * i_22 + i_10 * i_23 shr 14
        val i_25 = i_23 * i_11 - i_10 * i_22 shr 14
        val i_26 = calculateAvgHeightInclRoofs(i_24 + vector37.x.toInt(), i_25 + vector37.z.toInt(), i_8.toInt(), i_9.toInt(), super.plane.toInt())
        val i_27 = i_2 / 2
        val i_28 = i_3 / 2
        val i_29 = i_11 * i_27 + i_28 * i_10 shr 14
        val i_30 = i_28 * i_11 - i_10 * i_27 shr 14
        val i_31 = calculateAvgHeightInclRoofs(i_29 + vector37.x.toInt(), i_30 + vector37.z.toInt(), i_8.toInt(), i_9.toInt(), super.plane.toInt())
        val i_32 = kotlin.math.min(i_16, i_21)
        val i_33 = kotlin.math.min(i_26, i_31)
        val i_34 = kotlin.math.min(i_21, i_31)
        val i_35 = kotlin.math.min(i_16, i_26)
        this.rotateX = (atan2((i_32 - i_33).toDouble(), i_3.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
        this.rotateZ = (atan2((i_35 - i_34).toDouble(), i_2.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
        var i_36: Int
        if (this.rotateX != 0 && i_4 != 0) {
            i_36 = 16384 - i_4
            if (this.rotateX > 8192) {
                if (this.rotateX < i_36) {
                    this.rotateX = i_36
                }
            } else if (this.rotateX > i_4) {
                this.rotateX = i_4
            }
        }

        if (this.rotateZ != 0 && i_5 != 0) {
            i_36 = 16384 - i_5
            if (this.rotateZ > 8192) {
                if (this.rotateZ < i_36) {
                    this.rotateZ = i_36
                }
            } else if (this.rotateZ > i_5) {
                this.rotateZ = i_5
            }
        }

        this.translateY = i_31 + i_16
        if (i_21 + i_26 < this.translateY) {
            this.translateY = i_26 + i_21
        }

        this.translateY = (this.translateY shr 1) - vector37.y.toInt()
    }

    abstract fun getOverheadSprite(): Int

    override fun getMinY(): Int {
        return if (this.modelMinY == -32768) 0 else this.modelMinY
    }

    fun initInterface(): Boolean {
        return this.interfaceIdToLoad != -1 && initInterface(this.interfaceIdToLoad)
    }

    companion object {
        @JvmField
        val DEFAULT_BASTYPE = BasType()

        @JvmField
        var preferenceFlags: Int = 0

        @JvmField
        val PATHING_ENTITY_MODEL_CACHE = LRUCache(32)

        @JvmStatic
        fun getLargestEntity(plane: Int, x: Int, z: Int): PathingEntity? {
            val tile = Resource.mapRegion!!.getScene().tiles[plane][x][z] ?: return null

            var biggestEntity: PathingEntity? = null
            var size = -1
            var interactives = tile.interactives
            while (interactives != null) {
                val interactive = interactives.interactiveObject
                if (interactive is PathingEntity) {
                    val pathingEntity = interactive
                    val delta = (pathingEntity.getSize() - 1) * 256 + 252
                    val absolutePosition = pathingEntity.getAbsolutePosition().coords
                    val minX = (absolutePosition.x.toInt() - delta) shr 9
                    val minZ = (absolutePosition.z.toInt() - delta) shr 9
                    val maxX = (absolutePosition.x.toInt() + delta) shr 9
                    val maxZ = (absolutePosition.z.toInt() + delta) shr 9
                    if (minX <= x && minZ <= z && maxX >= x && maxZ >= z) {
                        val size2d = (maxX + 1 - x) * (maxZ + 1 - z)
                        if (size2d > size) {
                            biggestEntity = pathingEntity
                            size = size2d
                        }
                    }
                }
                interactives = interactives.child
            }
            return biggestEntity
        }

        @JvmStatic
        fun animateMovement(animable: PathingEntity, moveType: Int, turnDirectIndex: Int, rotation: Int) {
            val basType = animable.getBastype()
            val sequence = animable.movingSequence
            val turnDirection = animable.lastYawRotation - animable.yaw.rotation and 0x3fff
            if (moveType == MovementType.STATIONARY.id.toInt()) {
                if (turnDirection == 0 && animable.turningTicks <= 25) {
                    if (!sequence.hasType || !basType.isStandAnimation(sequence.getId())) {
                        sequence.animate(basType.getStandSequence())
                        sequence.hasType = sequence.hasType()
                    }
                } else if (rotation < 0 && basType.standTurnCcwSequence != -1) {
                    sequence.animate(basType.standTurnCcwSequence)
                    sequence.hasType = false
                } else if (rotation > 0 && basType.standTurnCwSequence != -1) {
                    sequence.animate(basType.standTurnCwSequence)
                    sequence.hasType = false
                } else if (!sequence.hasType || !basType.isStandAnimation(sequence.getId())) {
                    sequence.animate(basType.getStandSequence())
                    sequence.hasType = animable.movingSequence.hasType()
                }
            } else if (animable.faceEntity == -1 || turnDirection < 10240 && turnDirection > 2048) {
                if (turnDirection == 0 && animable.turningTicks <= 25) {
                    if (moveType == MovementType.RUNNING.id.toInt() && basType.runningSequence != -1) {
                        sequence.animate(basType.runningSequence)
                    } else if (moveType == MovementType.HALF_WALK.id.toInt() && basType.teleportSequence != -1) {
                        sequence.animate(basType.teleportSequence)
                    } else {
                        sequence.animate(basType.walkSequence)
                    }
                } else {
                    if (moveType == MovementType.RUNNING.id.toInt() && basType.runningSequence != -1) {
                        if (rotation < 0 && basType.runTurnCcwSequence != -1) {
                            sequence.animate(basType.runTurnCcwSequence)
                        } else if (rotation > 0 && basType.runTurnCwSequence != -1) {
                            sequence.animate(basType.runTurnCwSequence)
                        } else {
                            sequence.animate(basType.runningSequence)
                        }
                    } else if (moveType == MovementType.HALF_WALK.id.toInt() && basType.teleportSequence != -1) {
                        if (rotation < 0 && basType.teleTurnCcwSequence != -1) {
                            sequence.animate(basType.teleTurnCcwSequence)
                        } else if (rotation > 0 && basType.teleTurnCwSequence != -1) {
                            sequence.animate(basType.teleTurnCwSequence)
                        } else {
                            sequence.animate(basType.teleportSequence)
                        }
                    } else if (rotation < 0 && basType.walkTurnCcwSequence != -1) {
                        sequence.animate(basType.walkTurnCcwSequence)
                    } else if (rotation > 0 && basType.walkTurnCwSequence != -1) {
                        sequence.animate(basType.walkTurnCwSequence)
                    } else {
                        sequence.animate(basType.walkSequence)
                    }
                }
                sequence.hasType = false
            } else {
                val walkDirection = client.NPC_TURN_DIRECTIONS[turnDirectIndex] - animable.yaw.rotation and 0x3fff
                if (moveType == MovementType.RUNNING.id.toInt() && basType.runningSequence != -1) {
                    if (walkDirection > 2048 && walkDirection <= 6144 && basType.runDir1 != -1) {
                        sequence.animate(basType.runDir1)
                    } else if (walkDirection >= 10240 && walkDirection < 14336 && basType.runDir2 != -1) {
                        sequence.animate(basType.runDir2)
                    } else if (walkDirection > 6144 && walkDirection < 10240 && basType.runDir3 != -1) {
                        sequence.animate(basType.runDir3)
                    } else {
                        sequence.animate(basType.runningSequence)
                    }
                } else if (moveType == MovementType.HALF_WALK.id.toInt() && basType.teleportSequence != -1) {
                    if (walkDirection > 2048 && walkDirection <= 6144 && basType.teleDir1 != -1) {
                        sequence.animate(basType.teleDir1)
                    } else if (walkDirection >= 10240 && walkDirection < 14336 && basType.teleDir2 != -1) {
                        sequence.animate(basType.teleDir2)
                    } else if (walkDirection > 6144 && walkDirection < 10240 && basType.teleDir3 != -1) {
                        sequence.animate(basType.teleDir3)
                    } else {
                        sequence.animate(basType.teleportSequence)
                    }
                } else if (walkDirection > 2048 && walkDirection <= 6144 && basType.walkDir1 != -1) {
                    sequence.animate(basType.walkDir1)
                } else if (walkDirection >= 10240 && walkDirection < 14336 && basType.walkDir2 != -1) {
                    sequence.animate(basType.walkDir2)
                } else if (walkDirection > 6144 && walkDirection < 10240 && basType.walkDir3 != -1) {
                    sequence.animate(basType.walkDir3)
                } else {
                    sequence.animate(basType.walkSequence)
                }
                sequence.hasType = false
            }
        }

        @JvmStatic
        fun interfaceComponentOnEntity(entity: PathingEntity) {
            when (entity) {
                is NpcEntity -> {
                    val npc = entity
                    if (npc.definitions != null) {
                        npcHovered(npc, npc.plane != PlayerUpdate.MY_PLAYER!!.plane)
                    }
                }
                is PlayerEntity -> {
                    val player = entity
                    playerHovered(player, player.plane != PlayerUpdate.MY_PLAYER!!.plane)
                }
            }
        }

        @JvmStatic
        fun forceMovementType1(entity: PathingEntity) {
            val cyclesLeft = entity.forceMovementT1Delay - client.cycles
            val i_3 = entity.forceMovementT1XOff * 512 + entity.getSize() * 256
            val i_4 = entity.forceMovementT1YOff * 512 + entity.getSize() * 256
            val abs = entity.getAbsolutePosition().coords
            entity.setBasePosition(
                (abs.x.toInt() + (i_3 - abs.x.toInt()) / cyclesLeft).toFloat(),
                (abs.y.toInt()).toFloat(),
                (abs.z.toInt() + (i_4 - abs.z.toInt()) / cyclesLeft).toFloat()
            )
            entity.anInt10366 = 0
            entity.turn(entity.forceMoveDirection)
        }

        @JvmStatic
        fun forceMovementType2(entity: PathingEntity) {
            val sequence = entity.currentSequence
            if (entity.forceMovementT2Delay == client.cycles || !sequence.hasType() || sequence.method7585(1)) {
                val duration = entity.forceMovementT2Delay - entity.forceMovementT1Delay
                val cyclesPassed = client.cycles - entity.forceMovementT1Delay
                val x1 = entity.forceMovementT1XOff * 512 + entity.getSize() * 256
                val y1 = entity.forceMovementT1YOff * 512 + entity.getSize() * 256
                val x2 = entity.forceMovementT2XOff * 512 + entity.getSize() * 256
                val y2 = entity.forceMovementT2YOff * 512 + entity.getSize() * 256
                val abs = entity.getAbsolutePosition().coords
                entity.setBasePosition(
                    ((cyclesPassed * x2 + x1 * (duration - cyclesPassed)) / duration).toFloat(),
                    (abs.y.toInt()).toFloat(),
                    ((cyclesPassed * y2 + y1 * (duration - cyclesPassed)) / duration).toFloat()
                )
            }
            entity.anInt10366 = 0
            entity.turn(entity.forceMoveDirection, false)
        }

        @JvmStatic
        fun sweepPathingEntityModelcache() {
            synchronized(PATHING_ENTITY_MODEL_CACHE) {
                PATHING_ENTITY_MODEL_CACHE.clean(5)
            }
        }

        @JvmStatic
        fun resetPathingentityModels() {
            synchronized(PATHING_ENTITY_MODEL_CACHE) {
                PATHING_ENTITY_MODEL_CACHE.reset()
            }
        }

        @JvmStatic
        fun resetCache(flags: Int) {
            preferenceFlags = flags
            synchronized(PATHING_ENTITY_MODEL_CACHE) {
                PATHING_ENTITY_MODEL_CACHE.reset()
            }
        }

        @JvmStatic
        fun clearQueue() {
            synchronized(PATHING_ENTITY_MODEL_CACHE) {
                PATHING_ENTITY_MODEL_CACHE.resize()
            }
        }

        @JvmStatic
        fun calculateAvgHeightInclRoofs(absX: Int, absZ: Int, nextAbsX: Int, nextAbsZ: Int, plane: Int): Int {
            if (Resource.mapRegion!!.getScene() == null) {
                return 0
            }
            var plane = plane
            if (plane < 3) {
                val settingsBits = Resource.mapRegion!!.getSettingsBits()
                val sceneX = absX shr 9
                val sceneZ = absZ shr 9
                if (nextAbsX >= 0 && nextAbsZ >= 0 && nextAbsX <= Resource.mapRegion!!.getSceneSizeX() - 1 && nextAbsZ <= Resource.mapRegion!!.getSceneSizeY() - 1) {
                    if (sceneX >= 1 && sceneZ >= 1 && sceneX <= Resource.mapRegion!!.getSceneSizeX() - 1 && sceneZ <= Resource.mapRegion!!.getSceneSizeY() - 1) {
                        var toggleRoofs = ((settingsBits.tileFunctionMasks[1][absX shr 9][absZ shr 9].toInt()) and 0x2) != 0
                        var bool10: Boolean
                        var bool11: Boolean
                        if ((absX and 0x1ff) == 0) {
                            bool10 = ((settingsBits.tileFunctionMasks[1][sceneX - 1][absZ shr 9].toInt()) and 0x2) != 0
                            bool11 = ((settingsBits.tileFunctionMasks[1][sceneX][absZ shr 9].toInt()) and 0x2) != 0
                            if (bool11 != bool10) {
                                toggleRoofs = ((settingsBits.tileFunctionMasks[1][nextAbsX][nextAbsZ].toInt()) and 0x2) != 0
                            }
                        }
                        if ((absZ and 0x1ff) == 0) {
                            bool10 = ((settingsBits.tileFunctionMasks[1][absX shr 9][sceneZ - 1].toInt()) and 0x2) != 0
                            bool11 = ((settingsBits.tileFunctionMasks[1][absX shr 9][sceneZ].toInt()) and 0x2) != 0
                            if (bool10 != bool11) {
                                toggleRoofs = ((settingsBits.tileFunctionMasks[1][nextAbsX][nextAbsZ].toInt()) and 0x2) != 0
                            }
                        }
                        if (toggleRoofs) {
                            ++plane
                        }
                        return Resource.mapRegion!!.getScene().localHeightmap!![plane]!!.getAverageHeight(absX, absZ)
                    }
                    return 0
                }
                return 0
            }
            return Resource.mapRegion!!.getScene().localHeightmap!![plane]!!.getAverageHeight(absX, absZ)
        }

        /**
         * Revision 727 only draws cheap moving entity
         * shadows consisting of 3 rings under the entity
         * alpha decreasing with each ring
         */
        @JvmStatic
        fun getAnimatedShadowModel(
            toolkit: AbstractToolkit, rotationX: Int, rotationZ: Int, translateY: Int, size: Int, shadowModel: Model?,
            colorSrc: Int, colorDst: Int, alphaSrc: Int, alphaDst: Int, animation: Sequence?
        ): Model? {
            if (shadowModel == null) {
                return null
            }
            var functionMask = 2055
            if (animation != null) {
                functionMask = functionMask or animation.getFunctionMask()
                functionMask = functionMask and -513
            }
            val hash = (colorDst.toLong() shl 48) + (colorSrc.toLong() shl 32) + (size + (alphaSrc.toLong() shl 16) + (alphaDst.toLong() shl 24))
            var stationaryModel: Model?
            synchronized(PATHING_ENTITY_MODEL_CACHE) {
                stationaryModel = PATHING_ENTITY_MODEL_CACHE.get(hash) as Model?
            }
            if (stationaryModel == null || toolkit.mismatchingFunctionMask(stationaryModel!!.getFunctionMask(), functionMask) != 0) {
                if (stationaryModel != null) {
                    functionMask = toolkit.mergeFunctionMasks(functionMask, stationaryModel!!.getFunctionMask())
                }
                val totalFaces: Byte = when (size) {
                    1 -> 9
                    2 -> 12
                    3 -> 15
                    4 -> 18
                    else -> 21
                }
                val totalCircles: Byte = 3
                val offsets = intArrayOf(64, 96, 128)
                val mesh = Mesh(totalCircles * totalFaces + 1, totalFaces * totalCircles * 2 - totalFaces, 0)
                val inialVertex = mesh.addVertex(0, 0, 0)
                val vertices = Array(totalCircles.toInt()) { IntArray(totalFaces.toInt()) }
                for (circleIndex in 0 until totalCircles) {
                    val offsetX = offsets[circleIndex]
                    val offsetZ = offsets[circleIndex]
                    for (face in 0 until totalFaces) {
                        val angle = (face shl 14) / totalFaces
                        val vertexX = MathUtils.SINE[angle] * offsetX shr 14
                        val vertexZ = MathUtils.COSINE[angle] * offsetZ shr 14
                        vertices[circleIndex][face] = mesh.addVertex(vertexX, 0, vertexZ)
                    }
                }

                for (circleIndex in 0 until totalCircles) {
                    // [0] 42 [1] 128 [2] 213
                    val dstAlphaModifier = (circleIndex * 256 + 128) / totalCircles
                    // [0] 214 [1] 128 [2] 43
                    val srcAlphaModifier = 256 - dstAlphaModifier
                    val faceAlpha = (srcAlphaModifier * alphaSrc + dstAlphaModifier * alphaDst shr 8).toByte()
                    val faceColor = (((
                            srcAlphaModifier * (colorSrc and 0xfc00) + dstAlphaModifier * colorDst and 0xfc00) and 0xfc0000) +
                            (srcAlphaModifier * (colorSrc and 0x7f) + dstAlphaModifier * (colorDst and 0x7f) and 0x7f00) +
                            (srcAlphaModifier * (colorSrc and 0x380) + dstAlphaModifier * (colorDst and 0x380) and 0x38000) shr 8).toShort()
                    for (face in 0 until totalFaces) {
                        if (circleIndex == 0) {
                            mesh.addFace(
                                inialVertex,
                                vertices[0][(face + 1) % totalFaces],
                                vertices[0][face],
                                1.toByte(), (-1).toByte(),
                                faceColor, faceAlpha, (-1).toShort()
                            )
                        } else {
                            mesh.addFace(
                                vertices[circleIndex - 1][face],
                                vertices[circleIndex - 1][(face + 1) % totalFaces],
                                vertices[circleIndex][(face + 1) % totalFaces],
                                1.toByte(), (-1).toByte(),
                                faceColor, faceAlpha, (-1).toShort()
                            )
                            mesh.addFace(
                                vertices[circleIndex - 1][face],
                                vertices[circleIndex][(face + 1) % totalFaces],
                                vertices[circleIndex][face],
                                1.toByte(), (-1).toByte(),
                                faceColor, faceAlpha, (-1).toShort()
                            )
                        }
                    }
                }
                stationaryModel = toolkit.createModel(mesh, functionMask, preferenceFlags, 64, 768)
                synchronized(PATHING_ENTITY_MODEL_CACHE) {
                    PATHING_ENTITY_MODEL_CACHE.put(stationaryModel, hash)
                }
            }
            val minX = shadowModel.getMinX()
            val maxX = shadowModel.getMaxX()
            val minZ = shadowModel.getMinZ()
            val maxZ = shadowModel.getMaxZ()
            if (animation != null) {
                stationaryModel = stationaryModel!!.copy(3.toByte(), functionMask, true)
                stationaryModel!!.scale(maxX - minX shr 1, 128, maxZ - minZ shr 1)
                stationaryModel.translate(maxX + minX shr 1, 0, minZ + maxZ shr 1)
                animation.animateShadow(stationaryModel)
            } else {
                stationaryModel = stationaryModel!!.copy(3.toByte(), functionMask, true)
                stationaryModel!!.scale(maxX - minX shr 1, 128, maxZ - minZ shr 1)
                stationaryModel.translate(minX + maxX shr 1, 0, minZ + maxZ shr 1)
            }
            if (rotationX != 0) {
                stationaryModel!!.xaxisRotateWithoutNormals(rotationX)
            }
            if (rotationZ != 0) {
                stationaryModel!!.zaxisRotateWithoutNormals(rotationZ)
            }
            if (translateY != 0) {
                stationaryModel!!.translate(0, translateY, 0)
            }
            return stationaryModel
        }

        @JvmStatic
        fun animateMovement(pathingEntity: PathingEntity) {
            val moveSequence = pathingEntity.movingSequence
            if (moveSequence.hasType() && moveSequence.progressCycle(1) && moveSequence.needsReset()) {
                if (moveSequence.hasType) {
                    moveSequence.animate(pathingEntity.getBastype().getStandSequence())
                    moveSequence.hasType = moveSequence.hasType()
                }
                moveSequence.reset()
            }
            for (index in pathingEntity.spotAnimations.indices) {
                if (pathingEntity.spotAnimations[index].spotAnimationId != -1) {
                    val spotanimSequence = pathingEntity.spotAnimations[index].sequence
                    if (spotanimSequence.hasSpeed()) {
                        val spotType = TypeLists.SPOT_ANIM!!.list(pathingEntity.spotAnimations[index].spotAnimationId)
                        val seqType = spotanimSequence.getType()
                        if (spotType.replay) {
                            if (seqType!!.interruptType == 3) {
                                if (pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
                                    spotanimSequence.update(-1)
                                    pathingEntity.spotAnimations[index].spotAnimationId = -1
                                    continue
                                }
                            } else if (seqType!!.interruptType == 1 && pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
                                continue
                            }
                        }
                    }
                    if (spotanimSequence.progressCycle(1) && spotanimSequence.needsReset()) {
                        spotanimSequence.update(-1)
                        pathingEntity.spotAnimations[index].spotAnimationId = -1
                    }
                }
            }
            val currentSequence = pathingEntity.currentSequence
            if (currentSequence.hasType()) {
                Update@ while (true) {
                    val currentSeqType = currentSequence.getType()
                    if (currentSeqType!!.interruptType == 3) {
                        if (pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
                            pathingEntity.currentAnimations = null
                            currentSequence.update(-1)
                            break
                        }
                    } else if (currentSeqType!!.interruptType == 1) {
                        if (pathingEntity.previousMovementStepsLeft > 0 && pathingEntity.forceMovementT1Delay <= client.cycles && pathingEntity.forceMovementT2Delay < client.cycles) {
                            currentSequence.setSpeed(1)
                            break
                        }
                        currentSequence.setSpeed(0)
                    }
                    if (currentSequence.progressCycle(1) && currentSequence.needsReset()) {
                        pathingEntity.currentAnimations = null
                        currentSequence.update(-1)
                    }
                    break
                }
            }
            for (index in pathingEntity.sequences.indices) {
                val entitySeq = pathingEntity.sequences[index]
                if (entitySeq != null) {
                    if (entitySeq.anInt10065 > 0) {
                        entitySeq.anInt10065--
                    } else if (entitySeq.progressCycle(1) && entitySeq.needsReset()) {
                        pathingEntity.sequences[index] = null
                    }
                }
            }
        }
    }
}
