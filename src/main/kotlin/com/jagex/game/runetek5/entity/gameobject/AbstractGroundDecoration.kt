package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

abstract class AbstractGroundDecoration(
    scene: Scene,
    x: Int,
    y: Int,
    z: Int,
    plane: Int,
    collisionPlane: Int,
    floorOffsetY: Int
) : SceneGraphEntity(scene) {

    @JvmField val floorOffsetY: Short

    init {
        this.plane = plane.toByte()
        this.collisionPlane = collisionPlane.toByte()
        this.floorOffsetY = floorOffsetY.toShort()
        setBasePosition(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    override fun getActiveLights(lights: Array<PointLight?>): Int {
        val absolutePosition = getAbsolutePosition().coords
        @Suppress("UNCHECKED_CAST")
        return getActiveLights((absolutePosition.x.toInt() shr super.scene!!.tileUnits), (absolutePosition.z.toInt() shr super.scene!!.tileUnits), lights as Array<PointLight?>)
    }

    override fun isSoftHidden(toolkit: AbstractToolkit): Boolean {
        val absolutePosition = getAbsolutePosition().coords
        return super.scene!!.culling.isTileHidden(super.collisionPlane.toInt(), (absolutePosition.x.toInt() shr super.scene!!.tileUnits), (absolutePosition.z.toInt() shr super.scene!!.tileUnits))
    }

    override fun canRender(): Boolean {
        val absolutePosition = getAbsolutePosition().coords
        return super.scene!!.adjacent_tile_on_screen[(absolutePosition.x.toInt() shr super.scene!!.tileUnits) - super.scene!!.cameraGridPositionX + super.scene!!.visibleTiles][(absolutePosition.z.toInt() shr super.scene!!.tileUnits) - super.scene!!.cameraGridPositionY + super.scene!!.visibleTiles]
    }
}
