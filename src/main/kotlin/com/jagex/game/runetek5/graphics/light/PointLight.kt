package com.jagex.game.runetek5.graphics.light

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.math.Vector3

class PointLight(
    x: Int,
    y: Int,
    z: Int,
    @JvmField val size2d: Int,
    @JvmField val color: Int
) : Node<PointLight>() {

    @JvmField val position: Vector3 = Vector3()
    @JvmField var transparency: Float = 0f

    init {
        this.position.set(x.toFloat(), y.toFloat(), z.toFloat())
        this.transparency = 1.0f
    }

    fun getX(): Int {
        return this.position.x.toInt()
    }

    fun getY(): Int {
        return this.position.y.toInt()
    }

    fun getSize2d(): Int {
        return this.size2d
    }

    fun getColor(): Int {
        return this.color
    }

    fun setTransparency(transparency: Float) {
        this.transparency = transparency
    }

    fun setPosition(x: Int, y: Int, z: Int) {
        this.position.set(x.toFloat(), y.toFloat(), z.toFloat())
    }

    fun getZ(): Int {
        return this.position.z.toInt()
    }

    fun getAlpha(): Float {
        return this.transparency
    }
}
