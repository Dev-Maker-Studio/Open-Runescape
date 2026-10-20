package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.movement.CoordinateSpace
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class AnimatedWallDecoration(
    scene: Scene,
    toolkit: AbstractToolkit,
    locTypeList: ObjectTypeList,
    locType: ObjectType,
    plane: Int,
    collisionPlane: Int,
    x: Int,
    y: Int,
    z: Int,
    overlayHidden: Boolean,
    posX: Int,
    posY: Int,
    type: Int,
    rotation: Int,
    sequenceId: Int
) : AbstractWallDecoration(scene, x, y, z, plane, collisionPlane, posX, posY), GameObject {

    @JvmField val wallDecoration: AnimatedGameObject
    @JvmField var hasTransparency = true
    @JvmField var axisAlignedBoundingBox: Silhouette? = null
    @JvmField val interactive: Boolean

    init {
        wallDecoration = AnimatedGameObject(
            toolkit,
            locTypeList,
            locType,
            type,
            rotation,
            this.plane.toInt(),
            collisionPlane,
            this,
            overlayHidden,
            sequenceId
        )
        interactive = locType.hasActions != 0 && !overlayHidden
        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        wallDecoration.getAnimatedShadowedModel(toolkit)
    }

    override fun hasAlpha(): Boolean {
        return hasTransparency
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return axisAlignedBoundingBox
    }

    override fun getIconHeight(): Int {
        return wallDecoration.getNegativeMinY()
    }

    fun addCustomisation(customisation: ObjectTypeCustomisation?) {
        wallDecoration.addCustomisation(customisation)
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        val model = wallDecoration.getAnimatedModel(toolkit, 2048, false, true)
            ?: return null
        val renderingMatrix = toolkit.getRenderingMatrix()
        renderingMatrix.copyFrom(getProjectionMatrix())
        renderingMatrix.translate(positionX.toFloat(), 0.0f, positionY.toFloat())
        val abs = getAbsolutePosition()
        val renderedEntity = RenderedEntityCache.generate(interactive)
        val tileX = (abs.coords.x.toInt() shr 9)
        val tileZ = (abs.coords.z.toInt() shr 9)
        wallDecoration.animateParticles(toolkit, model, renderingMatrix, true)
        model.drawMatrixThreaded(renderingMatrix, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        if (wallDecoration.particleSystem != null) {
            val particles = wallDecoration.particleSystem!!.getAllParticles()
            toolkit.renderParticles(particles)
        }
        hasTransparency = model.accountForLayers() || wallDecoration.particleSystem != null
        val currentBoundingBox = axisAlignedBoundingBox
        if (currentBoundingBox == null) {
            axisAlignedBoundingBox = Silhouette.generate(abs.coords.x.toInt(), abs.coords.y.toInt(), abs.coords.z.toInt(), model)
        } else {
            Silhouette.set(currentBoundingBox, abs.coords.x.toInt(), abs.coords.y.toInt(), abs.coords.z.toInt(), model)
        }
        return renderedEntity
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        val model = wallDecoration.getAnimatedModel(toolkit, 262144, false, true)
        if (model != null) {
            val projectionMatrix = getProjectionMatrix()
            val abs = getAbsolutePosition()
            val tileX = (abs.coords.x.toInt() shr 9)
            val tileZ = (abs.coords.z.toInt() shr 9)
            wallDecoration.animateParticles(toolkit, model, projectionMatrix, false)
        }
    }

    override fun calculateProjection(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean {
        val model = wallDecoration.getAnimatedModel(toolkit, 131072, false, false)
        return model != null && model.calculateProjection(mouseX, mouseY, getProjectionMatrix(), false, 0)
    }

    override fun getMinY(): Int {
        return wallDecoration.getMinY()
    }

    override fun upload() {
    }

    override fun hasRenderingShadow(): Boolean {
        return wallDecoration.hasRenderingShadow()
    }

    override fun removeShadow(toolkit: AbstractToolkit) {
        wallDecoration.removeShadow()
    }

    override fun getRotation(): Int = wallDecoration.rotation

    override fun disregardTextureMovement(): Boolean {
        return false
    }

    override val id: Int
        get() = wallDecoration.locId

    override val shape: Int
        get() = wallDecoration.type
}
