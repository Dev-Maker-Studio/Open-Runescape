package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

@Suppress("EXPOSED_SUPER_CLASS")
abstract class SceneGraphEntity(
    @JvmField var scene: Scene?
) : SceneGraphNode() {

    @JvmField var plane: Byte = 0
    @JvmField var collisionPlane: Byte = 0
    @JvmField var boundingBoxPolygons: Array<BoundingBoxPolygon>? = null
    @JvmField var child: SceneGraphEntity? = null
    @JvmField var floorDepth: Int = 0

    @JvmField var modernRetained: Boolean = false

    abstract fun calculateProjection(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean

    abstract fun removeTileShadow()

    abstract fun hasTileShadow(): Boolean

    abstract fun disregardTextureMovement(): Boolean

    abstract fun hasAlpha(): Boolean

    abstract fun draw(toolkit: AbstractToolkit): RenderedEntity?

    abstract fun particleCycle(toolkit: AbstractToolkit)

    abstract fun getAxisAlignedBoundingBox(toolkit: AbstractToolkit): Silhouette?

    abstract fun getMinY(): Int

    open fun getMaxSizeShift(): Int {
        return 0
    }

    open fun getIconHeight(): Int {
        return -getMinY()
    }

    open fun getActiveLights(x: Int, y: Int, lights: Array<PointLight?>): Int {
        if (scene == null) return 0
        val tileLightBaseUid = scene!!.tileLightUids!![plane.toInt()][x][y]
        var uidShift = 0L
        var lightCount = 0
        while (uidShift <= 48L) {
            val slot = (tileLightBaseUid shr uidShift.toInt() and 65535L).toInt()
            if (slot <= 0) {
                break
            }
            lights[lightCount++] = scene!!.lightSlotOccupant!![slot - 1]!!.pointLight
            uidShift += 16L
        }
        for (lightIndex in lightCount until 4) {
            lights[lightIndex] = null
        }
        return lightCount
    }

    protected fun setupBoundingBox(amount: Int) {
        boundingBoxPolygons = Array(amount) { BoundingBoxPolygon() }
    }

    abstract fun castShadow(node: SceneGraphEntity, width: Int, length: Int, height: Int, renderTypeAdjustment: Boolean)

    abstract fun canRender(): Boolean

    abstract fun getActiveLights(lights: Array<PointLight?>): Int

    abstract fun isSoftHidden(toolkit: AbstractToolkit): Boolean
}
