package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.QuaternionPool
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.movement.CoordinateSpace
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.tan

class Projectile(
    scene: Scene,
    spotAnimId: Int,
    fromPlane: Int,
    toPlane: Int,
    localX: Int,
    localY: Int,
    startHeight: Int,
    startTime: Int,
    endTime: Int,
    angle: Int,
    slope: Int,
    entitySource: Int,
    entityLockOn: Int,
    endHeight: Int,
    useTerrainHeight: Boolean,
    matrxIndex: Int
) : AbstractInteractiveEntity(
    scene,
    fromPlane,
    toPlane,
    localX,
    MapRegion.tileheightForAbsolutePosition(localX, localY, fromPlane) - startHeight,
    localY,
    localX shr 9,
    localX shr 9,
    localY shr 9,
    localY shr 9,
    false,
    0
) {

    @JvmField
    val cycleToSpawnAt: Int
    @JvmField
    val cyclesUntilDisappear: Int
    @JvmField
    val entityLockOn: Int
    @JvmField
    val endHeight: Int
    var stepX: Double = 0.0
    var stepZ: Double = 0.0
    var horizontalSpeed: Double = 0.0
    var verticalSpeed: Double = 0.0
    var verticalAcceleration: Double = 0.0
    var hasAlpha: Boolean = false
    var projectileMinY: Int = 0
    var particleSystem: ParticleSystem? = null
    var animating: Boolean = false
    val gfxId: Int
    val angle: Int
    val slope: Int
    val entitySource: Int
    val startHeight: Int
    val adjustToTerrain: Boolean
    val matrxIndex: Int
    val sequence: Sequence

    init {
        this.gfxId = spotAnimId
        this.cycleToSpawnAt = startTime
        this.cyclesUntilDisappear = endTime
        this.angle = angle
        this.slope = slope
        this.entitySource = entitySource
        this.entityLockOn = entityLockOn
        this.startHeight = startHeight
        this.endHeight = endHeight
        this.adjustToTerrain = useTerrainHeight
        this.animating = false
        this.matrxIndex = matrxIndex
        val sequenceId = TypeLists.SPOT_ANIM!!.list(this.gfxId).sequenceId
        this.sequence = EntitySequence(this)
        this.sequence.update(sequenceId)
        this.setupBoundingBox(1)
    }

    override fun disregardTextureMovement(): Boolean {
        return false
    }

    fun cycle() {
        if (!animating && entitySource != 0) {
            var sourceEntity: Any? = null
            if (Client.MAP_LOADING_STAGE == 4) {
                sourceEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![entitySource - 1].getEntity()
            } else {
                val entityIndex: Int
                if (entitySource < 0) {
                    entityIndex = -entitySource - 1
                    if (entityIndex == PlayerUpdate.myPlayerIndex) {
                        sourceEntity = PlayerUpdate.MY_PLAYER!!
                    } else {
                        sourceEntity = PlayerUpdate.players[entityIndex]
                    }
                } else {
                    entityIndex = entitySource - 1
                    val npcObjectNode = Client.NPC_MAP.get(entityIndex.toLong()) as ObjectNode?
                    if (npcObjectNode != null) {
                        sourceEntity = npcObjectNode.value
                    }
                }
            }

            if (sourceEntity != null) {
                val absolute = (sourceEntity as PathingEntity).getAbsolutePosition().coords
                setBasePosition(
                    absolute.x,
                    (MapRegion.tileheightForAbsolutePosition(absolute.x.toInt(), absolute.z.toInt(), plane.toInt()) - startHeight).toFloat(),
                    absolute.z
                )
                if (matrxIndex >= 0) {
                    val basType = (sourceEntity as PathingEntity).getBastype()
                    var projectionX = 0
                    var projectionZ = 0
                    if (basType.objVerticeTransformations != null && basType.objVerticeTransformations!![matrxIndex] != null) {
                        projectionX += basType.objVerticeTransformations!![matrxIndex]!![0]
                        projectionZ += basType.objVerticeTransformations!![matrxIndex]!![2]
                    }

                    if (basType.projectionOffset != null && basType.projectionOffset!![matrxIndex] != null) {
                        projectionX += basType.projectionOffset!![matrxIndex]!![0]
                        projectionZ += basType.projectionOffset!![matrxIndex]!![2]
                    }

                    if (projectionX != 0 || projectionZ != 0) {
                        val rotationOffset = (sourceEntity as PathingEntity).yaw.getRotation()
                        var baseRotation = rotationOffset
                        if ((sourceEntity as PathingEntity).equipmentRotations != null && (sourceEntity as PathingEntity).equipmentRotations!![matrxIndex] != -1) {
                            baseRotation = (sourceEntity as PathingEntity).equipmentRotations!![matrxIndex]
                        }

                        val rotation = baseRotation - rotationOffset and 0x3fff
                        val sin = MathUtils.SINE[rotation]
                        val cos = MathUtils.COSINE[rotation]
                        val offsetX = sin * projectionZ + projectionX * cos shr 14
                        val offsetZ = projectionZ * cos - sin * projectionX shr 14
                        val newAbsolutePosition = Vector3Pool.vector3From(getAbsolutePosition().coords)
                        newAbsolutePosition.x += offsetX
                        newAbsolutePosition.z += offsetZ
                        setBasePosition(newAbsolutePosition)
                        newAbsolutePosition.pushVectorStack()
                    }
                }
            }
        }
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return null
    }

    fun start(x: Int, z: Int, y: Int, currentCycle: Int) {
        val absolute = Vector3Pool.vector3From(getAbsolutePosition().coords)
        if (!animating) {
            val offsetX = x - absolute.x
            val offsetZ = z - absolute.z
            val size2d = sqrt((offsetZ * offsetZ + offsetX * offsetX).toDouble()).toFloat()
            if (size2d != 0.0f) {
                absolute.x += offsetX * slope / size2d
                absolute.z += offsetZ * slope / size2d
            }
            if (adjustToTerrain) {
                absolute.y = (MapRegion.tileheightForAbsolutePosition(absolute.x.toInt(), absolute.z.toInt(), plane.toInt()) - startHeight).toFloat()
            }
            setBasePosition(absolute)
        }
        val cyclesLeft = cyclesUntilDisappear + 1 - currentCycle.toDouble()
        stepX = (x - absolute.x) / cyclesLeft
        stepZ = (z - absolute.z) / cyclesLeft
        horizontalSpeed = sqrt(stepZ * stepZ + stepX * stepX)
        if (angle != -1) {
            if (!animating) {
                verticalSpeed = -horizontalSpeed * tan(0.02454369 * angle)
            }
            verticalAcceleration = ((y - absolute.y) - verticalSpeed * cyclesLeft) * 2.0 / (cyclesLeft * cyclesLeft)
        } else {
            verticalSpeed = (y - absolute.y) / cyclesLeft
        }
        absolute.pushVectorStack()
    }

    override fun getMinY(): Int {
        return projectileMinY
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        val gfxModel = generateGfxModel(toolkit, ModelFunctionMask.HAS_TEXTURE_COORDS)
            ?: return null
        val projectionMatrix = getProjectionMatrix()
        updateParticles(toolkit, gfxModel, projectionMatrix)
        val renderedObject = RenderedEntityCache.generate(false)
        gfxModel.drawMatrixThreaded(projectionMatrix, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        if (particleSystem != null) {
            val particles = particleSystem!!.getAllParticles()
            toolkit.renderParticles(particles)
        }
        hasAlpha = gfxModel.accountForLayers()
        gfxModel.getSize3d()
        projectileMinY = gfxModel.getMinY()
        return renderedObject
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        val gfxModel = generateGfxModel(toolkit, 0)
        if (gfxModel != null) {
            val projectionMatrix = getProjectionMatrix()
            projectileMinY = gfxModel.getMinY()
            gfxModel.getSize3d()
            updateParticles(toolkit, gfxModel, projectionMatrix)
        }
    }

    override fun calculatePosition() {
        val absolute = getAbsolutePosition().coords
        startX = (absolute.x / 512.0f).toInt().toShort()
        endX = startX
        startZ = (absolute.z / 512.0f).toInt().toShort()
        endZ = startZ
    }

    fun updateParticles(toolkit: AbstractToolkit, gfxModel: Model, projectionMatrix: Matrix43) {
        gfxModel.updateParticlePosition(projectionMatrix)
        val emitters = gfxModel.getEmissiveTriangles()
        val vertices = gfxModel.getEffectiveVertices()
        if ((particleSystem == null || particleSystem!!.isDisabled) && (emitters != null || vertices != null)) {
            particleSystem = ParticleSystem.get(Client.cycles, true)
        }
        if (particleSystem != null) {
            particleSystem!!.performCycle(toolkit, Client.cycles.toLong(), emitters, vertices)
            particleSystem!!.setLogicHeight(plane.toInt())
        }
    }

    fun nextParticleCycle() {
        if (particleSystem != null) {
            particleSystem!!.setIsNewCycle()
        }
    }

    override fun calculateProjection(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean {
        return false
    }

    override fun castShadow(entity: SceneGraphEntity, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        throw IllegalStateException()
    }

    fun generateGfxModel(toolkit: AbstractToolkit, functionMask: Int): Model? {
        val spotAnimType = TypeLists.SPOT_ANIM!!.list(gfxId)
        return spotAnimType.getAnimatedModel(toolkit, functionMask, sequence, 2.toByte())
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    override fun hasAlpha(): Boolean {
        return hasAlpha
    }

    fun animate(step: Int) {
        animating = true
        val absolutePosition = CoordinateSpace(getAbsolutePosition())
        absolutePosition.coords.x = (absolutePosition.coords.x + stepX * step).toFloat()
        absolutePosition.coords.z = (absolutePosition.coords.z + stepZ * step).toFloat()
        if (adjustToTerrain) {
            absolutePosition.coords.y = (MapRegion.tileheightForAbsolutePosition(absolutePosition.coords.x.toInt(), absolutePosition.coords.z.toInt(), plane.toInt()) - startHeight).toFloat()
        } else if (angle != -1) {
            absolutePosition.coords.y = (absolutePosition.coords.y + verticalSpeed * step + step.toDouble() * step * 0.5 * verticalAcceleration).toFloat()
            verticalSpeed += step * verticalAcceleration
        } else {
            absolutePosition.coords.y = (absolutePosition.coords.y + verticalSpeed * step).toFloat()
        }

        absolutePosition.quaternion.rotate(1.0f, 0.0f, 0.0f, atan2(verticalSpeed, horizontalSpeed).toFloat())
        val quaternion = QuaternionPool.newQuaternion()
        quaternion.rotate(0.0f, 1.0f, 0.0f, atan2(stepX, stepZ).toFloat() - 3.1415927f)
        absolutePosition.quaternion.multiply(quaternion)
        quaternion.store()
        setAbsolutePosition(absolutePosition)
        if (sequence.progressCycle(1) && sequence.needsReset()) {
            sequence.reset()
        }
    }

    override fun hasTileShadow(): Boolean {
        return false
    }
}
