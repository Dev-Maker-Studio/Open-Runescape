package com.jagex.game.runetek5.entity.hint

import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.hint.HintTrail
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.movement.CoordinateSpace
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class HintTrailGraphNode(
    scene: Scene,
    @JvmField val hintTrail: HintTrail,
    plane: Int,
    collisionPlane: Int,
    x: Int,
    y: Int,
    z: Int
) : AbstractInteractiveEntity(scene, plane, collisionPlane, x, y, z, x shr 9, x shr 9, z shr 9, z shr 9, false, 0) {

    @JvmField var model: Model? = null

    override fun calculateProjection(graphicalrenderer1: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean {
        return false
    }

    override fun hasTileShadow(): Boolean {
        return false
    }

    override fun disregardTextureMovement(): Boolean {
        return model == null || !model!!.hasAnimatedTexture()
    }

    override fun hasAlpha(): Boolean {
        return model != null && model!!.accountForLayers()
    }

    override fun castShadow(entity: SceneGraphEntity, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
    }

    override fun removeTileShadow() {
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        if (model == null) {
            model = hintTrail.getStationaryModel(toolkit)
        }

        if (model != null) {
            val renderingMatrix = toolkit.getRenderingMatrix()
            val projectionMatrix = getProjectionMatrix()
            val abs = getAbsolutePosition()
            renderingMatrix.copyFrom(projectionMatrix)
            val tile = scene!!.tiles[plane.toInt()][abs.coords.x.toInt() shr 9][abs.coords.z.toInt() shr 9]
            if (tile != null) {
                val decoration = tile.groundDecoration
                if (decoration != null) {
                    renderingMatrix.translate(0.0f, -decoration.floorOffsetY.toFloat(), 0.0f)
                }
            }

            model!!.drawMatrixThreaded(renderingMatrix, null, ModelFunctionMaskGL.EMPTY)
        }

        return null
    }

    override fun getMinY(): Int {
        return if (model != null) model!!.getMinY() else 0
    }

    override fun particleCycle(toolkit: AbstractToolkit) {
    }

    override fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette? {
        return null
    }
}
