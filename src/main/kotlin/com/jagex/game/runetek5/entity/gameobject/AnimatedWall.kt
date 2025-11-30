package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.movement.CoordinateSpace
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class AnimatedWall(
    scene: Scene,
    toolkit: AbstractToolkit,
    locTypeList: ObjectTypeList,
    locType: ObjectType,
    plane: Int,
    collisionPlane: Int,
    x: Int,
    y: Int,
    z: Int,
    hideOverlay: Boolean,
    type: Int,
    rotation: Int,
    sequenceId: Int
) : AbstractWall(scene, x, y, z, plane, collisionPlane, ObjectShapes.getWallCullType(type, rotation)), GameObject {

    @JvmField val gameObject: AnimatedGameObject = AnimatedGameObject(
        toolkit, locTypeList, locType, type, rotation, plane, collisionPlane, this, hideOverlay, sequenceId
    )
    var priorityRender: Boolean = true
    var silhouette: Silhouette? = null
    val interactable: Boolean = locType.hasActions != 0 && !hideOverlay

    override val id: Int get() = gameObject.locId
    override val shape: Int get() = gameObject.type
    override fun getRotation(): Int = gameObject.rotation

    init {
        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        gameObject.getAnimatedShadowedModel(toolkit)
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        val model = gameObject.getAnimatedModel(toolkit, 2048, false, true) ?: return null
        val projectionMatrix = getProjectionMatrix()
        val abs = getAbsolutePosition()
        val renderedEntity = RenderedEntityCache.generate(interactable)
        val x = abs.coords.x.toInt() shr 9
        val z = abs.coords.z.toInt() shr 9
        gameObject.animateParticles(toolkit, model, projectionMatrix, true)
        model.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        if (gameObject.particleSystem != null) {
            val particles = gameObject.particleSystem!!.getAllParticles()
            toolkit.renderParticles(particles)
        }

        priorityRender = model.accountForLayers() || gameObject.particleSystem != null
        if (silhouette == null) {
            silhouette = Silhouette.generate(
                abs.coords.x.toInt(),
                abs.coords.y.toInt(),
                abs.coords.z.toInt(),
                model
            )
        } else {
            Silhouette.set(
                silhouette!!,
                abs.coords.x.toInt(),
                abs.coords.y.toInt(),
                abs.coords.z.toInt(),
                model
            )
        }

        return renderedEntity
    }

    override fun disregardTextureMovement(): Boolean {
        return false
    }

    override fun hasAlpha(): Boolean {
        return priorityRender
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return silhouette
    }

    fun addCustomisation(customisation: ObjectTypeCustomisation) {
        gameObject.addCustomisation(customisation)
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        val model = gameObject.getAnimatedModel(toolkit, 262144, true, true)
        if (model != null) {
            val projectionMatrix = getProjectionMatrix()
            val abs = getAbsolutePosition()
            val x = abs.coords.x.toInt() shr 9
            val z = abs.coords.z.toInt() shr 9
            gameObject.animateParticles(toolkit, model, projectionMatrix, false)
        }
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
        return gameObject.hasRenderingShadow()
    }

    override fun removeShadow(toolkit: AbstractToolkit) {
        gameObject.removeShadow()
    }

    override fun getIconHeight(): Int {
        return gameObject.negativeMinY
    }

    override fun getMinY(): Int {
        return gameObject.minY
    }

    override fun calculateProjection(toolkit: AbstractToolkit, cameraX: Int, cameraY: Int): Boolean {
        val model = gameObject.getAnimatedModel(toolkit, 131072, false, false)
        return model != null && model.calculateProjection(cameraX, cameraY, getProjectionMatrix(), false, 0)
    }

    override fun castShadow(entity: SceneGraphEntity, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
        throw IllegalStateException()
    }
}
