package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Abstract wall entity in the scene graph.
 */
abstract class AbstractWall(
    scene: Scene,
    x: Int,
    y: Int,
    z: Int,
    plane: Int,
    collisionPlane: Int,
    cullingType: Int
) : SceneGraphEntity(scene) {

    @JvmField val cullingType: Short

    init {
        this.plane = plane.toByte()
        this.collisionPlane = collisionPlane.toByte()
        this.cullingType = cullingType.toShort()
        setBasePosition(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    override fun isSoftHidden(toolkit: AbstractToolkit): Boolean {
        val absolute = getAbsolutePosition().coords
        return scene!!.culling.hideWall(
            this,
            collisionPlane.toInt(),
            (absolute.x.toInt() shr scene!!.tileUnits),
            (absolute.z.toInt() shr scene!!.tileUnits)
        )
    }

    override fun canRender(): Boolean {
        val absolute = getAbsolutePosition().coords
        val tileX = (absolute.x.toInt() shr scene!!.tileUnits) - scene!!.cameraGridPositionX + scene!!.visibleTiles
        val tileZ = (absolute.z.toInt() shr scene!!.tileUnits) - scene!!.cameraGridPositionY + scene!!.visibleTiles
        return scene!!.adjacent_tile_on_screen[tileX][tileZ]
    }

    override fun getActiveLights(cacheInto: Array<PointLight?>): Int {
        val absolute = getAbsolutePosition().coords
        val posX = absolute.x.toInt() shr scene!!.tileUnits
        val posY = absolute.z.toInt() shr scene!!.tileUnits
        var maskIndex = 0

        if (scene!!.cameraGridPositionX == posX) {
            maskIndex++
        } else if (scene!!.cameraGridPositionX < posX) {
            maskIndex += 2
        }

        if (scene!!.cameraGridPositionY == posY) {
            maskIndex += 3
        } else if (scene!!.cameraGridPositionY > posY) {
            maskIndex += 6
        }

        val mask = SETTING_MASKS[maskIndex]
        return if ((cullingType.toInt() and mask) != 0) {
            getActiveLights(posX, posY, cacheInto)
        } else if (cullingType.toInt() == 1 && posX > 0) {
            getActiveLights(posX - 1, posY, cacheInto)
        } else if (cullingType.toInt() == 4 && posX <= scene!!.sceneDimensionX) {
            getActiveLights(posX + 1, posY, cacheInto)
        } else if (cullingType.toInt() == 8 && posY > 0) {
            getActiveLights(posX, posY - 1, cacheInto)
        } else if (cullingType.toInt() == 2 && posY <= scene!!.sceneDimensionY) {
            getActiveLights(posX, posY + 1, cacheInto)
        } else if (cullingType.toInt() == 16 && posX > 0 && posY <= scene!!.sceneDimensionY) {
            getActiveLights(posX - 1, posY + 1, cacheInto)
        } else if (cullingType.toInt() == 32 && posX <= scene!!.sceneDimensionX && posY <= scene!!.sceneDimensionY) {
            getActiveLights(posX + 1, posY + 1, cacheInto)
        } else if (cullingType.toInt() == 128 && posX > 0 && posY > 0) {
            getActiveLights(posX - 1, posY - 1, cacheInto)
        } else if (cullingType.toInt() == 64 && posX <= scene!!.sceneDimensionX && posY > 0) {
            getActiveLights(posX + 1, posY - 1, cacheInto)
        } else {
            throw RuntimeException("")
        }
    }

    companion object {
        @JvmField val SETTING_MASKS = intArrayOf(19, 55, 38, 155, 255, 110, 137, 205, 76)
    }
}
