package com.jagex.game.runetek5.entity.grounditem

import com.jagex.game.runetek5.entity.InteractiveEntityContainer
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Abstract base class for ground items rendered in the scene.
 */
abstract class AbstractGroundItem(
    scene: Scene,
    x: Int,
    y: Int,
    z: Int,
    plane: Int,
    collisionPlane: Int
) : SceneGraphEntity(scene) {

    init {
        this.plane = plane.toByte()
        this.collisionPlane = collisionPlane.toByte()
        this.setBasePosition(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    override fun canRender(): Boolean {
        val absolute = getAbsolutePosition().coords
        val tileX = (absolute.x.toInt() shr super.scene.tileUnits) - super.scene.cameraGridPositionX + super.scene.visibleTiles
        val tileZ = (absolute.z.toInt() shr super.scene.tileUnits) - super.scene.cameraGridPositionY + super.scene.visibleTiles
        return super.scene.adjacent_tile_on_screen[tileX][tileZ]
    }

    override fun hasTileShadow(): Boolean {
        return false
    }

    override fun castShadow(class521Sub12: SceneGraphEntity, i_3: Int, i_4: Int, i_5: Int, bool6: Boolean) {
        throw IllegalStateException()
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    override fun getActiveLights(lightNodes: Array<PointLight?>): Int {
        val absolute = getAbsolutePosition().coords
        return getActiveLights(
            absolute.x.toInt() shr super.scene.tileUnits,
            absolute.z.toInt() shr super.scene.tileUnits,
            lightNodes
        )
    }

    override fun isSoftHidden(toolkit: AbstractToolkit): Boolean {
        val absolute = getAbsolutePosition().coords
        val tileX = absolute.x.toInt() shr super.scene.tileUnits
        val tileZ = absolute.z.toInt() shr super.scene.tileUnits
        val interactives = super.scene.getInteractiveObjects(super.plane.toInt(), tileX, tileZ)
        return if (interactives != null && interactives.interactiveObject!!.supportsItems) {
            super.scene.culling.isTileHidden(
                super.plane.toInt(),
                tileX,
                tileZ,
                interactives.interactiveObject!!.getMinY() + getMinY()
            )
        } else {
            super.scene.culling.isTileHidden(super.plane.toInt(), tileX, tileZ)
        }
    }
}
