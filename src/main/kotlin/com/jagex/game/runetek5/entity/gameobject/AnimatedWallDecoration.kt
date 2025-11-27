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
    loctypelist: ObjectTypeList,
    loctype: ObjectType,
    h: Int,
    logicHeight: Int,
    x: Int,
    y: Int,
    z: Int,
    overlayHidden: Boolean,
    posX: Int,
    posY: Int,
    type: Int,
    rotation: Int,
    sequenceId: Int
) : AbstractWallDecoration(scene, x, y, z, h, logicHeight, posX, posY), GameObject {

    @JvmField val wallDecoration: AnimatedGameObject
    @JvmField var aBool10504 = true
    @JvmField var axisAlignedBoundingBox: Silhouette? = null
    @JvmField val interactive: Boolean

    init {
        wallDecoration = AnimatedGameObject(
            toolkit,
            loctypelist,
            loctype,
            type,
            rotation,
            plane.toInt(),
            logicHeight,
            this,
            overlayHidden,
            sequenceId
        )
        interactive = loctype.hasActions != 0 && !overlayHidden
        setupBoundingBox(1)
    }


    override fun addShadow(graphicalrenderer1: AbstractToolkit) {
        wallDecoration.getAnimatedShadowedModel(graphicalrenderer1)
    }

    override fun hasAlpha(): Boolean {
        return aBool10504
    }


    override fun getAxisAlignedBoundingBox(graphicalrenderer1: AbstractToolkit): Silhouette? {
        return axisAlignedBoundingBox
    }


    override fun getIconHeight(): Int {
        return wallDecoration.getNegativeMinY()
    }

    fun addCustomisation(class4761: ObjectTypeCustomisation) {
        wallDecoration.addCustomisation(class4761)
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        val model = wallDecoration.getAnimatedModel(toolkit, 2048, false, true)
            ?: return null
        val matrix44var4 = toolkit.getRenderingMatrix()
        matrix44var4.copyFrom(getProjectionMatrix())
        matrix44var4.translate(positionX.toFloat(), 0.0f, positionY.toFloat())
        val abs = getAbsolutePosition()
        val renderedEntity = RenderedEntityCache.generate(interactive)
        val i_7 = (abs.coords.x.toInt() shr 9)
        val i_8 = (abs.coords.z.toInt() shr 9)
        wallDecoration.animateParticles(toolkit, model, matrix44var4, true)
        model.drawMatrixThreaded(matrix44var4, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        if (wallDecoration.particleSystem != null) {
            val class1519 = wallDecoration.particleSystem!!.getAllParticles()
            toolkit.renderParticles(class1519)
        }
        aBool10504 = model.accountForLayers() || wallDecoration.particleSystem != null
        val currentBoundingBox = axisAlignedBoundingBox
        if (currentBoundingBox == null) {
            axisAlignedBoundingBox = Silhouette.generate(abs.coords.x.toInt(), abs.coords.y.toInt(), abs.coords.z.toInt(), model)
        } else {
            Silhouette.set(currentBoundingBox, abs.coords.x.toInt(), abs.coords.y.toInt(), abs.coords.z.toInt(), model)
        }
        return renderedEntity
    }

    override fun particleCycle(graphicalrenderer1: AbstractToolkit) {
        val meshrasterizer3 = wallDecoration.getAnimatedModel(graphicalrenderer1, 262144, false, true)
        if (meshrasterizer3 != null) {
            val matrix44var4 = getProjectionMatrix()
            val class3055 = getAbsolutePosition()
            val i_6 = (class3055.coords.x.toInt() shr 9)
            val i_7 = (class3055.coords.z.toInt() shr 9)
            wallDecoration.animateParticles(graphicalrenderer1, meshrasterizer3, matrix44var4, false)
        }
    }

    override fun calculateProjection(toolkit: AbstractToolkit, i_2: Int, i_3: Int): Boolean {
        val model = wallDecoration.getAnimatedModel(toolkit, 131072, false, false)
        return model != null && model.calculateProjection(i_2, i_3, getProjectionMatrix(), false, 0)
    }

    override fun getMinY(): Int {
        return wallDecoration.getMinY()
    }

    override fun upload() {
    }

    override fun hasRenderingShadow(): Boolean {
        return wallDecoration.hasRenderingShadow()
    }

    override fun removeShadow(graphicalrenderer1: AbstractToolkit) {
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
