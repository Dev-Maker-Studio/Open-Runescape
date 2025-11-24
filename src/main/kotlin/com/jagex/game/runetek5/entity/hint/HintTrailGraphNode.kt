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

    override fun calculateProjection(graphicalrenderer1: AbstractToolkit, i_2: Int, i_3: Int): Boolean {
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

    override fun castShadow(class521Sub12: SceneGraphEntity, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
    }

    override fun removeTileShadow() {
    }

    override fun draw(toolkit: AbstractToolkit): RenderedEntity? {
        if (model == null) {
            model = hintTrail.getStationaryModel(toolkit)
        }

        if (model != null) {
            val matrix44var3 = toolkit.getRenderingMatrix()
            val matrix44var4 = getProjectionMatrix()
            val abs = getAbsolutePosition()
            matrix44var3.copyFrom(matrix44var4)
            val tile = scene.tiles[plane.toInt()][abs.coords.x.toInt() shr 9][abs.coords.z.toInt() shr 9]
            if (tile != null) {
                val decoration = tile.groundDecoration
                if (decoration != null) {
                    matrix44var3.translate(0.0f, -decoration.floorOffsetY.toFloat(), 0.0f)
                }
            }

            model!!.drawMatrixThreaded(matrix44var3, null, ModelFunctionMaskGL.EMPTY)
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
