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
    i_5: Int,
    i_6: Int,
    i_7: Int,
    i_8: Int,
    i_9: Int,
    bool10: Boolean,
    i_11: Int,
    i_12: Int,
    i_13: Int,
    i_14: Int,
    i_15: Int,
    i_16: Int,
    bool17: Boolean
) : AbstractInteractiveEntity(
    scene,
    i_5,
    i_6,
    i_7,
    i_8,
    i_9,
    i_11,
    i_12,
    i_13,
    i_14,
    objectdefinitions4.supportsItems == 1,
    ObjectShapes.method7853(i_15, i_16)
), GameObject {

    @JvmField
    var axisAlignedBoundingBox: Silhouette? = null

    @JvmField
    val aClass47410542: ObjectTypeList = locTypeList

    override val id: Int = objectdefinitions4.id

    @JvmField
    val aBool10536: Boolean = bool10

    override val shape: Int = i_15

    @JvmField
    val aByte10539: Byte = i_15.toByte()

    @JvmField
    val aByte10540: Byte = i_16.toByte()

    @JvmField
    val aBool10541: Boolean = objectdefinitions4.hasActions != 0 && !bool10

    @JvmField
    var aBool10543: Boolean = bool17

    @JvmField
    val aBool10544: Boolean = toolkit.hasShadows() && objectdefinitions4.dynamicShadow && !aBool10536 && PreferenceManager.preferences!!.sceneryShadows!!.getValue() != 0

    @JvmField
    var interactiveModel: Model? = null

    @JvmField
    var currentShadow: Shadow? = null

    init {
        collisionPlane = i_6.toByte()
        var i_18 = 2048
        if (aBool10543) {
            i_18 = i_18 or 0x10000
        }
        if (objectdefinitions4.dynamicTint) {
            i_18 = i_18 or 0x80000
        }
        val class45219 = method16117(toolkit, i_18, aBool10544)
        if (class45219 != null) {
            interactiveModel = class45219.mainObject as Model?
            currentShadow = class45219.companionObject as Shadow?
            if (aBool10543 || objectdefinitions4.dynamicTint) {
                interactiveModel = interactiveModel!!.copy(0.toByte(), i_18, false)
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
        if (currentShadow == null && aBool10544) {
            val typeResult = method16117(toolkit, 262144, true)
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

    fun method16116(toolkit: AbstractToolkit, functionMask: Int): Model? {
        if (interactiveModel != null && toolkit.mismatchingFunctionMask(interactiveModel!!.getFunctionMask(), functionMask) == 0) {
            return interactiveModel
        }
        val typeResult = method16117(toolkit, functionMask, false)
        return if (typeResult != null) typeResult.mainObject as Model? else null
    }

    fun method16117(graphicalrenderer1: AbstractToolkit, i_2: Int, bool3: Boolean): TypeResult? {
        val objectdefinitions5 = aClass47410542.list(id)
        val class3906: Ground
        val class3907: Ground?
        if (aBool10536) {
            class3906 = scene!!.underwaterHeightmap!![collisionPlane.toInt()]!!
            class3907 = scene!!.surfaceHeightmap[0]
        } else {
            class3906 = scene!!.surfaceHeightmap!![collisionPlane.toInt()]!!
            class3907 = if (collisionPlane < 3) {
                scene!!.surfaceHeightmap[collisionPlane + 1]
            } else {
                null
            }
        }
        val vector38 = getAbsolutePosition().coords
        return objectdefinitions5.getPair(
            graphicalrenderer1,
            i_2,
            if (aByte10539 != ObjectShapes.GROUND_INTERACT.shape.toByte()) aByte10539.toInt() else ObjectShapes.SCENERY_INTERACT.shape,
            if (ObjectShapes.GROUND_INTERACT.shape.toByte() == aByte10539) aByte10540 + 4 else aByte10540.toInt(),
            class3906,
            class3907,
            vector38.x.toInt(),
            vector38.y.toInt(),
            vector38.z.toInt(),
            bool3,
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
                method16116(graphicalrenderer1, 0)
            )
        }
        return axisAlignedBoundingBox
    }

    override fun hasTileShadow(): Boolean {
        return aBool10543
    }

    override fun castShadow(class521Sub12: SceneGraphEntity, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
        if (class521Sub12 is StationaryWall) {
            if (interactiveModel != null && class521Sub12.model != null) {
                interactiveModel!!.applyShading(class521Sub12.model!!, i_3, i_4, i_5, bool6)
            }
        } else if (class521Sub12 is StationaryInteractiveObject) {
            if (interactiveModel != null && class521Sub12.interactiveModel != null) {
                interactiveModel!!.applyShading(class521Sub12.interactiveModel!!, i_3, i_4, i_5, bool6)
            }
        }
    }

    override fun removeTileShadow() {
        aBool10543 = false
        if (interactiveModel != null) {
            interactiveModel!!.applyShading(interactiveModel!!.getFunctionMask() and -65537)
        }
    }

    override fun draw(graphicalrenderer1: AbstractToolkit): RenderedEntity? {
        if (interactiveModel == null) {
            return null
        }
        val matrix44var3 = getProjectionMatrix()
        val class2854 = RenderedEntityCache.generate(aBool10541)
        interactiveModel!!.drawMatrixThreaded(matrix44var3, boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        return class2854
    }

    override fun getRotation(): Int {
        return aByte10540.toInt()
    }

    override fun hasRenderingShadow(): Boolean {
        return aBool10544
    }

    override fun removeShadow(graphicalrenderer1: AbstractToolkit) {
        val class282Sub50Sub174: Shadow?
        if (currentShadow == null && aBool10544) {
            val class4525 = method16117(graphicalrenderer1, 262144, true)
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
        val model = method16116(toolkit, 131072)
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
