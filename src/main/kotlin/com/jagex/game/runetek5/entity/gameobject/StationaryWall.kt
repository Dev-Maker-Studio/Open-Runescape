package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeResult
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.PROJECTION
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
import com.jagex.game.runetek5.client.PreferenceManager

class StationaryWall(
    scene: Scene,
    toolkit: AbstractToolkit,
    locTypeList: ObjectTypeList,
    locType: ObjectType,
    plane: Int,
    collisionPlane: Int,
    x: Int,
    y: Int,
    z: Int,
    bool10: Boolean,
    shape: Int,
    rotation: Int,
    delayShading: Boolean
) : AbstractWall(scene, x, y, z, plane, collisionPlane, getCullingType(shape, rotation)), GameObject {

    @JvmField
    var cachedShadow: Shadow? = null

    @JvmField
    var axisAlignedBoundingBox: Silhouette? = null

    @JvmField
    var shadowed: Boolean = delayShading

    @JvmField
    val locTypeList: ObjectTypeList = locTypeList

    override val id: Int = locType.id

    @JvmField
    val locId: Int = locType.id

    @JvmField
    val overlayHidden: Boolean = bool10

    override val shape: Int = shape

    @JvmField
    val shapeAsByte: Byte = shape.toByte()

    @JvmField
    val rotation: Byte = rotation.toByte()

    @JvmField
    val interactive: Boolean = locType.hasActions != 0 && !bool10

    @JvmField
    val hasDynamicShadow: Boolean = toolkit.hasShadows() && locType.dynamicShadow && !overlayHidden && PreferenceManager.preferences!!.sceneryShadows!!.getValue() != 0

    @JvmField
    var model: Model? = null

    init {
        var i_14 = 2048
        if (shadowed) {
            i_14 = i_14 or 0x10000
        }

        if (locType.dynamicTint) {
            i_14 = i_14 or 0x80000
        }

        val typeResult = getTypeResult(toolkit, i_14, hasDynamicShadow)
        if (typeResult != null) {
            model = typeResult.mainObject as Model?
            cachedShadow = typeResult.companionObject as Shadow?
            if (shadowed || locType.dynamicTint) {
                model = model!!.copy(0.toByte(), i_14, false)
                if (locType.dynamicTint) {
                    val tint = Resource.mapRegion!!.dynamicTint
                    model!!.tint(tint.hue, tint.saturation, tint.lightness, tint.opacity)
                }
            }
        }

        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        val shadow: Shadow?
        if (cachedShadow == null && hasDynamicShadow) {
            val typeResult = getTypeResult(toolkit, 262144, true)
            shadow = if (typeResult != null) typeResult.companionObject as Shadow? else null
        } else {
            shadow = cachedShadow
            cachedShadow = null
        }

        val abs = getAbsolutePosition().coords
        if (shadow != null) {
            scene!!.renderShadows(shadow, collisionPlane.toInt(), abs.x.toInt(), abs.z.toInt(), null)
        }
    }

    override fun removeShadow(graphicalrenderer1: AbstractToolkit) {
        val class282Sub50Sub174: Shadow?
        if (cachedShadow == null && hasDynamicShadow) {
            val class4525 = getTypeResult(graphicalrenderer1, 262144, true)
            class282Sub50Sub174 = if (class4525 != null) class4525.companionObject as Shadow? else null
        } else {
            class282Sub50Sub174 = cachedShadow
            cachedShadow = null
        }

        val abs = getAbsolutePosition().coords
        if (class282Sub50Sub174 != null) {
            scene!!.removeShadow(class282Sub50Sub174, collisionPlane.toInt(), abs.x.toInt(), abs.z.toInt(), null)
        }
    }

    override fun disregardTextureMovement(): Boolean {
        return model == null || !model!!.hasAnimatedTexture()
    }

    fun getTypeResult(toolkit: AbstractToolkit, functionMask: Int, hasShadow: Boolean): TypeResult? {
        val locType = locTypeList.list(locId)
        val currentLevel: Ground
        val nextLevel: Ground?
        if (overlayHidden) {
            currentLevel = scene!!.underwaterHeightmap!![collisionPlane.toInt()]!!
            nextLevel = scene!!.surfaceHeightmap[0]
        } else {
            currentLevel = scene!!.surfaceHeightmap!![collisionPlane.toInt()]!!
            nextLevel = if (collisionPlane < 3) {
                scene!!.surfaceHeightmap[collisionPlane + 1]
            } else {
                null
            }
        }

        val abs = getAbsolutePosition().coords
        return locType.getPair(
            toolkit,
            functionMask,
            shapeAsByte.toInt(),
            rotation.toInt(),
            currentLevel,
            nextLevel,
            abs.x.toInt(),
            abs.y.toInt(),
            abs.z.toInt(),
            hasShadow,
            null
        )
    }

    fun getModel(toolkit: AbstractToolkit, functionMask: Int): Model? {
        if (model != null && toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), functionMask) == 0) {
            return model
        }
        val typeResult = getTypeResult(toolkit, functionMask, false)
        return if (typeResult != null) typeResult.mainObject as Model? else null
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        val abs = getAbsolutePosition().coords
        if (axisAlignedBoundingBox == null) {
            axisAlignedBoundingBox = Silhouette.generate(
                abs.x.toInt(),
                abs.y.toInt(),
                abs.z.toInt(),
                getModel(toolkit, 0)
            )
        }

        return axisAlignedBoundingBox
    }

    override fun upload() {
        if (model != null) {
            model!!.uploadToGpu()
        }
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
        // NOOP
    }

    override fun hasTileShadow(): Boolean {
        return shadowed
    }

    override fun removeTileShadow() {
        shadowed = false
        if (model != null) {
            model!!.applyShading(model!!.getFunctionMask() and -65537)
        }
    }

    override fun getRotation(): Int {
        return rotation.toInt()
    }

    override fun hasRenderingShadow(): Boolean {
        return hasDynamicShadow
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        if (model == null) {
            return null
        }
        val projectionMatrix = getProjectionMatrix()
        val renderedEntity = RenderedEntityCache.generate(interactive)
        model!!.drawMatrixThreaded(projectionMatrix, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        return renderedEntity
    }

    override fun castShadow(sceneGraphEntity: SceneGraphEntity, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        if (sceneGraphEntity is StationaryWall) {
            if (model != null && sceneGraphEntity.model != null) {
                model!!.applyShading(sceneGraphEntity.model!!, x, y, z, renderTypeAdjustment)
            }
        } else if (sceneGraphEntity is StationaryInteractiveObject) {
            if (model != null && sceneGraphEntity.interactiveModel != null) {
                model!!.applyShading(sceneGraphEntity.interactiveModel!!, x, y, z, renderTypeAdjustment)
            }
        }
    }

    override fun hasAlpha(): Boolean {
        return model != null && model!!.accountForLayers()
    }

    override fun getMinY(): Int {
        return if (model != null) model!!.getMinY() else 0
    }

    override fun calculateProjection(toolkit: AbstractToolkit, cameraX: Int, cameraY: Int): Boolean {
        val model = getModel(toolkit, PROJECTION)
        if (model != null) {
            val projectionMatrix = getProjectionMatrix()
            return model.calculateProjection(cameraX, cameraY, projectionMatrix, false, 0)
        }
        return false
    }

    companion object {
        @JvmField
        var menuHeight: Int = 0

        @JvmField
        val WALL_STRAIGHT_CULLING_BITS = intArrayOf(1, 2, 4, 8)

        @JvmField
        val WALL_CORNER_CULLING_BITS = intArrayOf(16, 32, 64, 128)

        @JvmStatic
        fun getCullingType(type: Int, rotation: Int): Int {
            return if (type != ObjectShapes.WALL_DIAGONAL_CORNER.shape && type != ObjectShapes.WALL_STRAIGHT_CORNER.shape) {
                WALL_STRAIGHT_CULLING_BITS[rotation and 0x3]
            } else {
                WALL_CORNER_CULLING_BITS[rotation and 0x3]
            }
        }
    }
}
