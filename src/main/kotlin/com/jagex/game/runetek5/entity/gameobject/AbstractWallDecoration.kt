package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Base class for wall decorations (paintings, torches, etc.).
 * Wall decorations are attached to walls and rendered at specific positions.
 */
abstract class AbstractWallDecoration protected constructor(
    scene: Scene,
    x: Int,
    y: Int,
    z: Int,
    plane: Int,
    collisionPlane: Int,
    positionX: Int,
    positionY: Int
) : SceneGraphEntity(scene) {

    /** X position offset on the wall */
    @JvmField var positionX: Short = positionX.toShort()

    /** Y position offset on the wall */
    @JvmField var positionY: Short = positionY.toShort()

    init {
        this.plane = plane.toByte()
        this.collisionPlane = collisionPlane.toByte()
        setBasePosition(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    override fun getActiveLights(lights: Array<PointLight>): Int {
        val absolutePosition = getAbsolutePosition().coords
        return getActiveLights(
            absolutePosition.x.toInt() shr scene.tileUnits,
            absolutePosition.z.toInt() shr scene.tileUnits,
            lights
        )
    }

    override fun canRender(): Boolean {
        val abs = getAbsolutePosition().coords
        val tileX = (abs.x.toInt() shr scene.tileUnits) - scene.cameraGridPositionX + scene.visibleTiles
        val tileZ = (abs.z.toInt() shr scene.tileUnits) - scene.cameraGridPositionY + scene.visibleTiles
        return scene.adjacent_tile_on_screen[tileX][tileZ]
    }

    override fun castShadow(node: SceneGraphEntity, width: Int, length: Int, height: Int, bool6: Boolean) {
        throw IllegalStateException()
    }

    override fun isSoftHidden(toolkit: AbstractToolkit): Boolean {
        val abs = getAbsolutePosition().coords
        return scene.culling.isTileHidden(
            collisionPlane.toInt(),
            abs.x.toInt() shr scene.tileUnits,
            abs.z.toInt() shr scene.tileUnits,
            getMinY()
        )
    }

    override fun hasTileShadow(): Boolean = false
}
