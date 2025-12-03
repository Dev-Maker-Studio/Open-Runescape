package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeResult
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.HAS_TEXTURE_COORDS
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.SHADED
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.TINT
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

class StationaryGroundDecoration(
    scene: Scene,
    toolkit: AbstractToolkit,
    @JvmField val locTypeLst: ObjectTypeList,
    loctype: ObjectType,
    i_5: Int,
    i_6: Int,
    i_7: Int,
    i_8: Int,
    i_9: Int,
    @JvmField val underwater: Boolean,
    rotation: Int,
    hasShadow: Boolean
) : AbstractGroundDecoration(scene, i_7, i_8, i_9, i_5, i_6, loctype.groundDecorationHeight), GameObject {

    @JvmField var primaryShadow: Shadow? = null
    @JvmField var axisAlignedBoundingBox: Silhouette? = null
    @JvmField var hasTileShadow: Boolean = false
    override val id: Int = loctype.id
    @JvmField val currentRotation: Byte = rotation.toByte()
    @JvmField val interactive: Boolean = loctype.hasActions != 0 && !underwater
    @JvmField val hasShadow: Boolean = toolkit.hasShadows() && loctype.dynamicShadow && !this.underwater && PreferenceManager.preferences!!.sceneryShadows.getValue() != 0
    @JvmField var model: Model? = null
    override val shape: Int = ObjectShapes.GROUND_DECORATION.shape

    init {
        var functionMask = HAS_TEXTURE_COORDS
        if (hasShadow) {
            functionMask = functionMask or SHADED
        }
        if (loctype.dynamicTint) {
            functionMask = functionMask or TINT
        }
        val typeResult = getLocresult(toolkit, functionMask, hasShadow)
        if (typeResult != null) {
            this.model = typeResult.mainObject as Model?
            this.primaryShadow = typeResult.companionObject as Shadow?
            if (hasShadow || loctype.dynamicTint) {
                this.model = this.model!!.copy(0.toByte(), functionMask, false)
                if (loctype.dynamicTint) {
                    val dynamicTint = Resource.mapRegion!!.getDynamicTint()
                    this.model!!.tint(dynamicTint.hue, dynamicTint.saturation, dynamicTint.lightness, dynamicTint.opacity)
                }
            }
        }
        this.hasTileShadow = hasShadow
        setupBoundingBox(1)
    }

    override fun addShadow(toolkit: AbstractToolkit) {
        val shadow: Shadow? = if (this.primaryShadow == null && this.hasShadow) {
            val typeResult = getLocresult(toolkit, 262144, true)
            if (typeResult != null) typeResult.companionObject as Shadow? else null
        } else {
            val s = this.primaryShadow
            this.primaryShadow = null
            s
        }
        val position = getAbsolutePosition().coords
        if (shadow != null) {
            super.scene!!.renderShadows(shadow, super.collisionPlane.toInt(), position.x.toInt(), position.z.toInt(), null)
        }
    }

    override fun getMinY(): Int {
        return if (this.model != null) this.model!!.getMinY() else 0
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        if (this.model == null) {
            return null
        }
        val projectionMatrix = getProjectionMatrix()
        val drawnModel = RenderedEntityCache.generate(this.interactive)
        this.model!!.drawMatrixThreaded(projectionMatrix, super.boundingBoxPolygons!![0], ModelFunctionMaskGL.EMPTY)
        return drawnModel
    }

    fun getLocresult(toolkit: AbstractToolkit, functionMask: Int, hasShadow: Boolean): TypeResult? {
        val loctype = this.locTypeLst.list(this.id)
        val collisionPlaneInt = super.collisionPlane.toInt()
        val heightsCurrentLvl: Ground
        val heightsNextLvl: Ground?
        if (this.underwater) {
            heightsCurrentLvl = super.scene!!.underwaterHeightmap[collisionPlaneInt]!!
            heightsNextLvl = super.scene!!.surfaceHeightmap[0]
        } else {
            heightsCurrentLvl = super.scene!!.surfaceHeightmap[collisionPlaneInt]!!
            heightsNextLvl = if (collisionPlaneInt < 3) {
                super.scene!!.surfaceHeightmap[collisionPlaneInt + 1]
            } else {
                null
            }
        }
        val absolute = getAbsolutePosition().coords
        return loctype!!.getPair(toolkit, functionMask, ObjectShapes.GROUND_DECORATION.shape, this.currentRotation.toInt(), heightsCurrentLvl, heightsNextLvl, absolute.x.toInt(), absolute.y.toInt(), absolute.z.toInt(), hasShadow, null)
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        val abs = getAbsolutePosition().coords
        if (this.axisAlignedBoundingBox == null) {
            this.axisAlignedBoundingBox = Silhouette.generate(abs.x.toInt(), abs.y.toInt(), abs.z.toInt(), adjustToFunctionmask(toolkit, 0))
        }
        return this.axisAlignedBoundingBox
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
    }

    override fun removeShadow(toolkit: AbstractToolkit) {
        val shadow: Shadow? = if (this.primaryShadow == null && this.hasShadow) {
            val locresult = getLocresult(toolkit, 262144, true)
            if (locresult != null) locresult.companionObject as Shadow? else null
        } else {
            val s = this.primaryShadow
            this.primaryShadow = null
            s
        }
        val absolute = getAbsolutePosition().coords
        if (shadow != null) {
            super.scene!!.removeShadow(shadow, super.collisionPlane.toInt(), absolute.x.toInt(), absolute.z.toInt(), null)
        }
    }

    override fun castShadow(renderedGraphNode: SceneGraphEntity, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
        if (renderedGraphNode is StationaryGroundDecoration) {
            if (this.model != null && renderedGraphNode.model != null) {
                this.model!!.applyShading(renderedGraphNode.model!!, i_3, i_4, i_5, bool6)
            }
        }
    }

    override fun removeTileShadow() {
        this.hasTileShadow = false
        if (this.model != null) {
            this.model!!.applyShading(this.model!!.getFunctionMask() and -65537)
        }
    }

    fun adjustToFunctionmask(toolkit: AbstractToolkit, functionMask: Int): Model? {
        if (this.model != null && toolkit.mismatchingFunctionMask(this.model!!.getFunctionMask(), functionMask) == 0) {
            return this.model
        }
        val locresult = getLocresult(toolkit, functionMask, false)
        return if (locresult != null) locresult.mainObject as Model? else null
    }

    override fun getRotation(): Int {
        return this.currentRotation.toInt()
    }

    override fun disregardTextureMovement(): Boolean {
        return this.model == null || !this.model!!.hasAnimatedTexture()
    }

    override fun hasTileShadow(): Boolean {
        return this.hasTileShadow
    }

    override fun hasRenderingShadow(): Boolean {
        return this.hasShadow
    }

    override fun upload() {
        if (this.model != null) {
            this.model!!.uploadToGpu()
        }
    }

    override fun hasAlpha(): Boolean {
        return this.model != null && this.model!!.accountForLayers()
    }

    override fun calculateProjection(toolkit: AbstractToolkit, i_2: Int, i_3: Int): Boolean {
        val model = adjustToFunctionmask(toolkit, 131072)
        if (model != null) {
            val projectionMatrix = getProjectionMatrix()
            return model.calculateProjection(i_2, i_3, projectionMatrix, false, 0)
        }
        return false
    }
}
