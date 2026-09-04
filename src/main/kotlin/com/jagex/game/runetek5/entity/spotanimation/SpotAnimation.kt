package com.jagex.game.runetek5.entity.spotanimation

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.sequence.EntitySequence
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Represents an animated spot graphics effect in the game world.
 */
class SpotAnimation(
    scene: Scene,
    @JvmField val gfxId: Int,
    speed: Int,
    fromPlane: Int,
    toPlane: Int,
    localX: Int,
    height: Int,
    localY: Int,
    startX: Int,
    endX: Int,
    startY: Int,
    endY: Int,
    @JvmField val gfxRotation: Int,
    adjustReplayMode: Boolean
) : AbstractInteractiveEntity(
    scene,
    fromPlane,
    toPlane,
    localX,
    height,
    localY,
    startX,
    endX,
    startY,
    endY,
    false,
    0.toByte()
) {

    @JvmField var accountForLayers: Boolean = true
    @JvmField var objectMinY: Int = 0
    @JvmField var sequence: Sequence? = null
    @JvmField var particleSystem: ParticleSystem? = null

    init {
        val spotAnimType = TypeLists.SPOT_ANIM!!.list(gfxId)
        val sequenceId = spotAnimType.sequenceId
        if (sequenceId != -1) {
            sequence = EntitySequence(this)
            var replayMode = if (spotAnimType.replay) 0 else 2
            if (adjustReplayMode) {
                replayMode = 1
            }
            sequence!!.animateSequence(sequenceId, speed, replayMode, false)
        }
        setupBoundingBox(1)
    }

    override fun disregardTextureMovement(): Boolean = false

    override fun getMinY(): Int = objectMinY

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? = null

    fun progressCycle() {
        if (sequence != null && !sequence!!.needsReset()) {
            sequence!!.progressCycle(1)
        }
    }

    fun notAccelerated(): Boolean {
        return sequence != null && !sequence!!.hasSpeed()
    }

    fun needsReset(): Boolean {
        return sequence == null || sequence!!.needsReset()
    }

    fun generateGfxModel(toolkit: AbstractToolkit, functionMask: Int, gfxId: Int): Model? {
        val gfxType = TypeLists.SPOT_ANIM!!.list(gfxId)
        val currentLvl = scene!!.surfaceHeightmap[plane.toInt()]
        val nextLvl = if (collisionPlane < 3) scene!!.surfaceHeightmap[collisionPlane.toInt() + 1] else null
        val absolute = getAbsolutePosition().coords

        return if (sequence != null && !sequence!!.needsReset()) {
            gfxType.getAnimatedModel(
                toolkit,
                functionMask,
                true,
                currentLvl,
                nextLvl,
                absolute.x.toInt(),
                absolute.y.toInt(),
                absolute.z.toInt(),
                sequence,
                2
            )
        } else {
            gfxType.getAnimatedModel(
                toolkit,
                functionMask,
                true,
                currentLvl,
                nextLvl,
                absolute.x.toInt(),
                absolute.y.toInt(),
                absolute.z.toInt(),
                null,
                2
            )
        }
    }

    fun updateParticles(toolkit: AbstractToolkit, gfxModel: Model, particlePosition: Matrix43) {
        gfxModel.updateParticlePosition(particlePosition)
        val emitter = gfxModel.getEmissiveTriangles()
        val vertices = gfxModel.getEffectiveVertices()

        if ((particleSystem == null || particleSystem!!.isDisabled) && (emitter != null || vertices != null)) {
            particleSystem = ParticleSystem.get(Client.cycles, true)
        }

        particleSystem?.let {
            it.performCycle(toolkit, Client.cycles.toLong(), emitter, vertices)
            it.setLogicHeight(plane.toInt())
        }
    }

    fun resetCycle() {
        particleSystem?.setIsNewCycle()
    }

    override fun calculateProjection(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean = false

    override fun hasTileShadow(): Boolean = false

    override fun castShadow(entity: SceneGraphEntity, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        throw IllegalStateException()
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        val model = generateGfxModel(
            toolkit,
            ModelFunctionMask.HAS_TEXTURE_COORDS or if (gfxRotation != 0) ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Z else 0,
            gfxId
        ) ?: return null

        if (gfxRotation != 0) {
            model.yaxisRotateWithoutNormals(gfxRotation * 2048)
        }

        val projectionMatrix = getProjectionMatrix()
        updateParticles(toolkit, model, projectionMatrix)
        val renderedEntity = RenderedEntityCache.generate(false)
        model.drawMatrixThreaded(projectionMatrix, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)

        particleSystem?.let {
            val particles = it.getAllParticles()
            toolkit.renderParticles(particles)
        }

        accountForLayers = model.accountForLayers()
        objectMinY = model.getMinY()
        model.getSize3d()
        return renderedEntity
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        val gfxModel = generateGfxModel(toolkit, 0, gfxId)
        if (gfxModel != null) {
            updateParticles(toolkit, gfxModel, getProjectionMatrix())
        }
    }

    override fun hasAlpha(): Boolean = accountForLayers
}
