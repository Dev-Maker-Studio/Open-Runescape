package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
jag::graphics::GraphEntity::LineIntersectionSupport(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     .text  00000000000ADFD0 00000003                   R . . . . . .
jag::graphics::GraphEntity::LowerCurrentLOD(int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  00000000000ADFB0 00000002                   R . . . . . .
jag::graphics::GraphEntity::OccludesView(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .text  00000000000ADFE0 00000003                   R . . . . . .
jag::graphics::GraphEntity::TestIntersection(jag::math::Vector3 const&,jag::math::Vector3 const&,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         .text  00000000000ADFC0 00000003                   R . . . . . .
jag::graphics::GraphEntity::~GraphEntity() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000F7F50 0000000B                   R . . . . . .
jag::graphics::GraphEntity::~GraphEntity() [clone]
 */
@Suppress("EXPOSED_SUPER_CLASS")
abstract class SceneGraphEntity(
    @JvmField var scene: Scene?
) : SceneGraphNode() {

    @JvmField var plane: Byte = 0
    @JvmField var collisionPlane: Byte = 0
    @JvmField var boundingBoxPolygons: Array<BoundingBoxPolygon>? = null
    @JvmField var child: SceneGraphEntity? = null
    @JvmField var floorDepth: Int = 0

    abstract fun calculateProjection(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean

    abstract fun removeTileShadow()

    abstract fun hasTileShadow(): Boolean

    abstract fun disregardTextureMovement(): Boolean

    // FIXME: find correct name
    // has_alpha
    // account_for_layers
    // account_for_priorities
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
