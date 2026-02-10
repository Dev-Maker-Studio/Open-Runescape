package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.movement.CoordinateSpace
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class AnimatedInteractiveObject(
    scene: Scene,
    toolkit: AbstractToolkit,
    loctypeList: ObjectTypeList,
    loctype: ObjectType,
    plane: Int,
    collisionPlane: Int,
    x: Int,
    y: Int,
    z: Int,
    overlayHidden: Boolean,
    startX: Int,
    endX: Int,
    startY: Int,
    endY: Int,
    type: Int,
    objRotation: Int,
    sequenceId: Int
) : AbstractInteractiveEntity(
    scene,
    plane,
    collisionPlane,
    x,
    y,
    z,
    startX,
    endX,
    startY,
    endY,
    loctype.supportsItems == 1,
    ObjectShapes.getRotatedWall(type, objRotation)
), GameObject {

    @JvmField val activeGameObject: AnimatedGameObject = AnimatedGameObject(
        toolkit, loctypeList, loctype, type, objRotation, plane.toInt(), collisionPlane, this, overlayHidden, sequenceId
    )
    var hasParticles: Boolean = true
    var axisAlignedBoundingBox: Silhouette? = null
    val canInteract: Boolean = loctype.hasActions != 0 && !overlayHidden

    override val id: Int get() = activeGameObject.locId
    override val shape: Int get() = activeGameObject.type
    override fun getRotation(): Int = activeGameObject.rotation

    init {
        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        activeGameObject.getAnimatedShadowedModel(toolkit)
    }

    override fun disregardTextureMovement(): Boolean {
        return false
    }

    override fun getMinY(): Int {
        return activeGameObject.getMinY()
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return axisAlignedBoundingBox
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        val model = activeGameObject.getAnimatedModel(toolkit, ModelFunctionMask.HAS_TEXTURE_COORDS, false, true) ?: return null
        val projectionMatrix = getProjectionMatrix()
        val renderedEntity = RenderedEntityCache.generate(canInteract)
        activeGameObject.animateParticles(toolkit, model, projectionMatrix, true)
        model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        if (activeGameObject.particleSystem != null) {
            val particles = activeGameObject.particleSystem!!.getAllParticles()
            toolkit.renderParticles(particles)
        }
        hasParticles = model.accountForLayers() || activeGameObject.particleSystem != null
        val absolute = getAbsolutePosition()
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
        val model = activeGameObject.getAnimatedModel(toolkit, ModelFunctionMask.SHADOWED, true, true)
        if (model != null) {
            activeGameObject.animateParticles(toolkit, model, getProjectionMatrix(), false)
        }
    }

    fun addCustomisation(customisation: ObjectTypeCustomisation) {
        activeGameObject.addCustomisation(customisation)
    }

    override fun calculateProjection(toolkit: AbstractToolkit, cameraX: Int, cameraY: Int): Boolean {
        val model = activeGameObject.getAnimatedModel(toolkit, 131072, false, false)
        return model != null && model.calculateProjection(cameraX, cameraY, getProjectionMatrix(), false, 0)
    }

    override fun hasTileShadow(): Boolean {
        return false
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    override fun upload() {
    }

    override fun hasRenderingShadow(): Boolean {
        return activeGameObject.hasRenderingShadow()
    }

    override fun removeShadow(toolkit: AbstractToolkit) {
        activeGameObject.removeShadow()
    }

    override fun hasAlpha(): Boolean {
        return hasParticles
    }

    override fun castShadow(entity: SceneGraphEntity, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        throw IllegalStateException()
    }

    override fun getIconHeight(): Int {
        return activeGameObject.getNegativeMinY()
    }
}
