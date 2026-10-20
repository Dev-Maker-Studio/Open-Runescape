package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

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

    @JvmField var positionX: Short = positionX.toShort()

    @JvmField var positionY: Short = positionY.toShort()

    init {
        this.plane = plane.toByte()
        this.collisionPlane = collisionPlane.toByte()
        setBasePosition(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
    }

    override fun removeTileShadow() {
        throw IllegalStateException()
    }

    override fun getActiveLights(lights: Array<PointLight?>): Int {
        val absolutePosition = getAbsolutePosition().coords
        @Suppress("UNCHECKED_CAST")
        return getActiveLights(
            absolutePosition.x.toInt() shr scene!!.tileUnits,
            absolutePosition.z.toInt() shr scene!!.tileUnits,
            lights as Array<PointLight?>
        )
    }

    override fun canRender(): Boolean {
        val abs = getAbsolutePosition().coords
        val tileX = (abs.x.toInt() shr scene!!.tileUnits) - scene!!.cameraGridPositionX + scene!!.visibleTiles
        val tileZ = (abs.z.toInt() shr scene!!.tileUnits) - scene!!.cameraGridPositionY + scene!!.visibleTiles
        return scene!!.tileInFrustum[tileX][tileZ]
    }

    override fun castShadow(node: SceneGraphEntity, width: Int, length: Int, height: Int, renderTypeAdjustment: Boolean) {
        throw IllegalStateException()
    }

    override fun isSoftHidden(toolkit: AbstractToolkit): Boolean {
        val abs = getAbsolutePosition().coords
        return scene!!.culling.isTileHidden(
            collisionPlane.toInt(),
            abs.x.toInt() shr scene!!.tileUnits,
            abs.z.toInt() shr scene!!.tileUnits,
            getMinY()
        )
    }

    override fun hasTileShadow(): Boolean = false
}
