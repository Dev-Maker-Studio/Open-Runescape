package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordinateSpace
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class AnimatedGroundDecoration(
    scene: Scene,
    renderer: AbstractToolkit,
    loctypelist: ObjectTypeList,
    loctype: ObjectType,
    logicHeight: Int,
    plane: Int,
    x: Int,
    y: Int,
    z: Int,
    overlayHidden: Boolean,
    objRotation: Int,
    sequenceId: Int
) : AbstractGroundDecoration(scene, x, y, z, logicHeight, plane, loctype.groundDecorationHeight), GameObject {

    @JvmField val gameObject: AnimatedGameObject = AnimatedGameObject(
        renderer, loctypelist, loctype, ObjectShapes.GROUND_DECORATION.shape, objRotation, logicHeight, plane, this, overlayHidden, sequenceId
    )
    var hasRenderingParticles: Boolean = true
    var axisAlignedBoundingBox: Silhouette? = null
    val canInteract: Boolean = loctype.hasActions != 0 && !overlayHidden

    override val id: Int get() = gameObject.locId
    override val shape: Int get() = gameObject.type
    override fun getRotation(): Int = gameObject.rotation

    init {
        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        gameObject.getAnimatedShadowedModel(toolkit)
    }

    override fun disregardTextureMovement(): Boolean {
        return false
    }

    override fun hasAlpha(): Boolean {
        return hasRenderingParticles
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return axisAlignedBoundingBox
    }

    override fun getMinY(): Int {
        return gameObject.getMinY()
    }

    override fun getIconHeight(): Int {
        return gameObject.getNegativeMinY()
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        val model = gameObject.getAnimatedModel(toolkit, ModelFunctionMask.HAS_TEXTURE_COORDS, false, true) ?: return null
        val projectionMatrix = getProjectionMatrix()
        val absolute = getAbsolutePosition()
        val renderedEntity = RenderedEntityCache.generate(canInteract)
        val x = absolute.coords.x.toInt() shr 9
        val y = absolute.coords.z.toInt() shr 9
        gameObject.animateParticles(toolkit, model, projectionMatrix, true)
        model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        if (gameObject.particleSystem != null) {
            val particles = gameObject.particleSystem!!.getAllParticles()
            toolkit.renderParticles(particles)
        }
        hasRenderingParticles = model.accountForLayers() || gameObject.particleSystem != null
        if (axisAlignedBoundingBox == null) {
            axisAlignedBoundingBox = Silhouette.generate(
                absolute.coords.x.toInt(),
                absolute.coords.y.toInt(),
                absolute.coords.z.toInt(),
                model
            )
        } else {
            Silhouette.set(
                axisAlignedBoundingBox!!,
                absolute.coords.x.toInt(),
                absolute.coords.y.toInt(),
                absolute.coords.z.toInt(),
                model
            )
        }
        return renderedEntity
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        val model = gameObject.getAnimatedModel(toolkit, ModelFunctionMask.SHADOWED, true, true)
        if (model != null) {
            val abs = getAbsolutePosition().coords
            val x = abs.x.toInt() shr 9
            val z = abs.z.toInt() shr 9
            gameObject.animateParticles(toolkit, model, getProjectionMatrix(), false)
        }
    }

    override fun calculateProjection(toolkit: AbstractToolkit, cameraX: Int, cameraY: Int): Boolean {
        val model = gameObject.getAnimatedModel(toolkit, 131072, false, false)
        return model != null && model.calculateProjection(cameraX, cameraY, getProjectionMatrix(), false, 0)
    }

    override fun hasTileShadow(): Boolean {
        return false
    }

    override fun castShadow(entity: SceneGraphEntity, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        throw IllegalStateException()
    }

    fun addCustomisation(locCustomisation: ObjectTypeCustomisation) {
        gameObject.addCustomisation(locCustomisation)
    }

    override fun upload() {
    }

    override fun hasRenderingShadow(): Boolean {
        return gameObject.hasRenderingShadow()
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    override fun removeShadow(toolkit: AbstractToolkit) {
        gameObject.removeShadow()
    }
}
