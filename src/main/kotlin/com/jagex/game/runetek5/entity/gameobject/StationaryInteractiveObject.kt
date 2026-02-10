package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeResult
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
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

class StationaryInteractiveObject(
    scene: Scene,
    toolkit: AbstractToolkit,
    locTypeList: ObjectTypeList,
    objectdefinitions4: ObjectType,
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
    shape: Int,
    rotation: Int,
    hasTileShadow: Boolean
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
    objectdefinitions4.supportsItems == 1,
    ObjectShapes.getWallInteractByte(shape, rotation)
), GameObject {

    @JvmField
    var axisAlignedBoundingBox: Silhouette? = null

    @JvmField
    val locTypeList: ObjectTypeList = locTypeList

    override val id: Int = objectdefinitions4.id

    @JvmField
    val overlayHidden: Boolean = overlayHidden

    override val shape: Int = shape

    @JvmField
    val shapeAsByte: Byte = shape.toByte()

    @JvmField
    val rotationAsByte: Byte = rotation.toByte()

    @JvmField
    val interactive: Boolean = objectdefinitions4.hasActions != 0 && !overlayHidden

    @JvmField
    var hasTileShadow: Boolean = hasTileShadow

    @JvmField
    val hasDynamicShadow: Boolean = toolkit.hasShadows() && objectdefinitions4.dynamicShadow && !overlayHidden && PreferenceManager.preferences!!.sceneryShadows!!.getValue() != 0

    @JvmField
    var interactiveModel: Model? = null

    @JvmField
    var currentShadow: Shadow? = null

    init {
        this.collisionPlane = collisionPlane.toByte()
        var functionMask = 2048
        if (hasTileShadow) {
            functionMask = functionMask or 0x10000
        }
        if (objectdefinitions4.dynamicTint) {
            functionMask = functionMask or 0x80000
        }
        val typeResult = getTypeResult(toolkit, functionMask, hasDynamicShadow)
        if (typeResult != null) {
            interactiveModel = typeResult.mainObject as Model?
            currentShadow = typeResult.companionObject as Shadow?
            if (hasTileShadow || objectdefinitions4.dynamicTint) {
                interactiveModel = interactiveModel!!.copy(0.toByte(), functionMask, false)
                if (objectdefinitions4.dynamicTint) {
                    val class34120 = Resource.mapRegion!!.dynamicTint
                    interactiveModel!!.tint(class34120.hue, class34120.saturation, class34120.lightness, class34120.opacity)
                }
            }
        }
        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        val shadow: Shadow?
        if (currentShadow == null && hasDynamicShadow) {
            val typeResult = getTypeResult(toolkit, 262144, true)
            shadow = if (typeResult != null) typeResult.companionObject as Shadow? else null
        } else {
            shadow = currentShadow
            currentShadow = null
        }
        val abs = getAbsolutePosition().coords
        if (shadow != null) {
            scene!!.renderShadows(shadow, collisionPlane.toInt(), abs.x.toInt(), abs.z.toInt(), null)
        }
    }

    override fun hasAlpha(): Boolean {
        return interactiveModel != null && interactiveModel!!.accountForLayers()
    }

    fun getShadowDisplacement(): Int {
        return if (interactiveModel != null) interactiveModel!!.getSize2d() / 4 else 15
    }

    fun getModel(toolkit: AbstractToolkit, functionMask: Int): Model? {
        if (interactiveModel != null && toolkit.mismatchingFunctionMask(interactiveModel!!.getFunctionMask(), functionMask) == 0) {
            return interactiveModel
        }
        val typeResult = getTypeResult(toolkit, functionMask, false)
        return if (typeResult != null) typeResult.mainObject as Model? else null
    }

    fun getTypeResult(graphicalrenderer1: AbstractToolkit, functionMask: Int, includeShadow: Boolean): TypeResult? {
        val objectdefinitions5 = locTypeList.list(id)
        val heightmap: Ground
        val roofHeightmap: Ground?
        if (overlayHidden) {
            heightmap = scene!!.underwaterHeightmap!![collisionPlane.toInt()]!!
            roofHeightmap = scene!!.surfaceHeightmap[0]
        } else {
            heightmap = scene!!.surfaceHeightmap!![collisionPlane.toInt()]!!
            roofHeightmap = if (collisionPlane < 3) {
                scene!!.surfaceHeightmap[collisionPlane + 1]
            } else {
                null
            }
        }
        val vector38 = getAbsolutePosition().coords
        return objectdefinitions5.getPair(
            graphicalrenderer1,
            functionMask,
            if (shapeAsByte != ObjectShapes.GROUND_INTERACT.shape.toByte()) shapeAsByte.toInt() else ObjectShapes.SCENERY_INTERACT.shape,
            if (ObjectShapes.GROUND_INTERACT.shape.toByte() == shapeAsByte) rotationAsByte + 4 else rotationAsByte.toInt(),
            heightmap,
            roofHeightmap,
            vector38.x.toInt(),
            vector38.y.toInt(),
            vector38.z.toInt(),
            includeShadow,
            null
        )
    }

    override fun getAxisAlignedBoundingBox(graphicalrenderer1: AbstractToolkit): Silhouette? {
        val vector33 = getAbsolutePosition().coords
        if (axisAlignedBoundingBox == null) {
            axisAlignedBoundingBox = Silhouette.generate(
                vector33.x.toInt(),
                vector33.y.toInt(),
                vector33.z.toInt(),
                getModel(graphicalrenderer1, 0)
            )
        }
        return axisAlignedBoundingBox
    }

    override fun hasTileShadow(): Boolean {
        return hasTileShadow
    }

    override fun castShadow(entity: SceneGraphEntity, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        if (entity is StationaryWall) {
            if (interactiveModel != null && entity.model != null) {
                interactiveModel!!.applyShading(entity.model!!, x, y, z, renderTypeAdjustment)
            }
        } else if (entity is StationaryInteractiveObject) {
            if (interactiveModel != null && entity.interactiveModel != null) {
                interactiveModel!!.applyShading(entity.interactiveModel!!, x, y, z, renderTypeAdjustment)
            }
        }
    }

    override fun removeTileShadow() {
        hasTileShadow = false
        if (interactiveModel != null) {
            interactiveModel!!.applyShading(interactiveModel!!.getFunctionMask() and -65537)
        }
    }

    override fun draw(graphicalrenderer1: AbstractToolkit): RenderedEntity? {
        if (interactiveModel == null) {
            return null
        }
        val projectionMatrix = getProjectionMatrix()
        val renderedEntity = RenderedEntityCache.generate(interactive)
        interactiveModel!!.drawMatrixThreaded(projectionMatrix, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        return renderedEntity
    }

    override fun getRotation(): Int {
        return rotationAsByte.toInt()
    }

    override fun hasRenderingShadow(): Boolean {
        return hasDynamicShadow
    }

    override fun removeShadow(graphicalrenderer1: AbstractToolkit) {
        val class282Sub50Sub174: Shadow?
        if (currentShadow == null && hasDynamicShadow) {
            val class4525 = getTypeResult(graphicalrenderer1, 262144, true)
            class282Sub50Sub174 = if (class4525 != null) class4525.companionObject as Shadow? else null
        } else {
            class282Sub50Sub174 = currentShadow
            currentShadow = null
        }
        val vector36 = getAbsolutePosition().coords
        if (class282Sub50Sub174 != null) {
            scene!!.removeShadow(class282Sub50Sub174, collisionPlane.toInt(), vector36.x.toInt(), vector36.z.toInt(), null)
        }
    }

    override fun disregardTextureMovement(): Boolean {
        return interactiveModel == null || !interactiveModel!!.hasAnimatedTexture()
    }

    override fun calculateProjection(toolkit: AbstractToolkit, camX: Int, camY: Int): Boolean {
        val model = getModel(toolkit, 131072)
        if (model != null) {
            val projectionMatrix = getProjectionMatrix()
            return model.calculateProjection(camX, camY, projectionMatrix, false, 0)
        }
        return false
    }

    override fun upload() {
        if (interactiveModel != null) {
            interactiveModel!!.uploadToGpu()
        }
    }

    override fun getMinY(): Int {
        return if (interactiveModel != null) interactiveModel!!.getMinY() else 0
    }

    override fun particleCycle(graphicalrenderer1: AbstractToolkit) {
    }
}
