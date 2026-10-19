package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeResult
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.DynamicTint
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Stationary wall decoration game object.
 */
class StationaryWallDecoration(
    scene: Scene,
    toolkit: AbstractToolkit,
    val locTypeList: ObjectTypeList,
    locType: ObjectType,
    plane: Int,
    collisionPlane: Int,
    x: Int,
    y: Int,
    z: Int,
    belowWater: Boolean,
    positionX: Int,
    positionY: Int,
    type: Int,
    rotation: Int
) : AbstractWallDecoration(scene, x, y, z, plane, collisionPlane, positionX, positionY), GameObject {

    override val id: Int = locType.id
    @JvmField val underwater: Boolean = belowWater
    val rotationByte: Byte = rotation.toByte()
    override val shape: Int = type
    @JvmField val interactive: Boolean
    @JvmField val shadowed: Boolean
    @JvmField var model: Model? = null
    @JvmField var shadow: Shadow? = null
    @JvmField var axisAlignedBoundingBox: Silhouette? = null

    init {
        interactive = locType.hasActions != 0 && !belowWater
        shadowed = toolkit.hasShadows() && locType.dynamicShadow && !underwater &&
                   PreferenceManager.preferences!!.sceneryShadows!!.getValue() != 0

        var functionMask = 2048
        if (locType.dynamicTint) {
            functionMask = functionMask or ModelFunctionMask.TINT
        }

        val locresult = getLocResult(toolkit, functionMask, shadowed)
        if (locresult != null) {
            model = locresult.mainObject as Model?
            shadow = locresult.companionObject as Shadow?
            if (locType.dynamicTint) {
                model = model!!.copy(0.toByte(), functionMask, false)
                if (locType.dynamicTint) {
                    val dynamicTint: DynamicTint = Resource.mapRegion!!.dynamicTint
                    model!!.tint(dynamicTint.hue, dynamicTint.saturation, dynamicTint.lightness, dynamicTint.opacity)
                }
            }
        }
        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        val locresultShadow: Shadow? = if (shadow == null && shadowed) {
            val locresult = getLocResult(toolkit, 262144, true)
            locresult?.companionObject as Shadow?
        } else {
            val result = shadow
            shadow = null
            result
        }

        val absolutePosition: Vector3 = getAbsolutePosition().coords
        if (locresultShadow != null) {
            scene!!.renderShadows(locresultShadow, collisionPlane.toInt(), absolutePosition.x.toInt(), absolutePosition.z.toInt(), null)
        }
    }

    override fun disregardTextureMovement(): Boolean {
        return model == null || !model!!.hasAnimatedTexture()
    }

    override fun hasAlpha(): Boolean {
        return model != null && model!!.accountForLayers()
    }

    override fun getMinY(): Int {
        return if (model != null) model!!.getMinY() else 0
    }

    fun getAdjustedModel(toolkit: AbstractToolkit, functionMask: Int): Model? {
        if (model != null && toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), functionMask) == 0) {
            return model
        }
        val locresult = getLocResult(toolkit, functionMask, false)
        return locresult?.mainObject as Model?
    }

    fun getLocResult(toolkit: AbstractToolkit, functionMask: Int, hasShadow: Boolean): TypeResult? {
        val locType = locTypeList.list(id)
        val groundCurrentLvl: Ground?
        val groundNextLvl: Ground?

        if (underwater) {
            groundCurrentLvl = scene!!.underwaterHeightmap!![collisionPlane.toInt()]!!
            groundNextLvl = scene!!.surfaceHeightmap[0]
        } else {
            groundCurrentLvl = scene!!.surfaceHeightmap[collisionPlane.toInt()]
            groundNextLvl = if (collisionPlane < 3) {
                scene!!.surfaceHeightmap[collisionPlane + 1]
            } else {
                null
            }
        }

        val absolute: Vector3 = getAbsolutePosition().coords
        return locType.getPair(
            toolkit, functionMask, shape, rotationByte.toInt(),
            groundCurrentLvl, groundNextLvl,
            absolute.x.toInt(), absolute.y.toInt(), absolute.z.toInt(),
            hasShadow, null
        )
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        if (model == null) {
            return null
        } else {
            val matrix: Matrix43 = toolkit.getRenderingMatrix()
            matrix.copyFrom(getProjectionMatrix())
            matrix.translate(positionX.toFloat(), 0.0f, positionY.toFloat())
            val renderedEntity = RenderedEntityCache.generate(interactive)
            model!!.drawMatrixThreaded(matrix, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
            return renderedEntity
        }
    }

    override fun particleCycle(graphicalrenderer1: AbstractToolkit) {
    }

    override fun calculateProjection(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean {
        val adjustedModel = getAdjustedModel(toolkit, 131072)
        if (adjustedModel != null) {
            val matrix: Matrix43 = getProjectionMatrix()
            return adjustedModel.calculateProjection(mouseX, mouseY, matrix, false, 0)
        }
        return false
    }

    override fun getRotation(): Int {
        return rotationByte.toInt()
    }

    override fun upload() {
        if (model != null) {
            model!!.uploadToGpu()
        }
    }

    override fun removeShadow(graphicalrenderer1: AbstractToolkit) {
        val locresultShadow: Shadow? = if (shadow == null && shadowed) {
            val locresult = getLocResult(graphicalrenderer1, 262144, true)
            locresult?.companionObject as Shadow?
        } else {
            val result = shadow
            shadow = null
            result
        }

        val absolutePosition: Vector3 = getAbsolutePosition().coords
        if (locresultShadow != null) {
            scene!!.removeShadow(locresultShadow, collisionPlane.toInt(), absolutePosition.x.toInt(), absolutePosition.z.toInt(), null)
        }
    }

    override fun getAxisAlignedBoundingBox(graphicalrenderer1: AbstractToolkit): Silhouette? {
        val vector33: Vector3 = getAbsolutePosition().coords
        if (axisAlignedBoundingBox == null) {
            axisAlignedBoundingBox = Silhouette.generate(
                vector33.x.toInt(), vector33.y.toInt(), vector33.z.toInt(),
                getAdjustedModel(graphicalrenderer1, 0)
            )
        }

        return axisAlignedBoundingBox
    }

    override fun hasRenderingShadow(): Boolean {
        return shadowed
    }
}
