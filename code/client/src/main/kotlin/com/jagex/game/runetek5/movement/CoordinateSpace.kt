package com.jagex.game.runetek5.movement

import com.jagex.game.runetek5.math.Quaternion
import com.jagex.game.runetek5.math.Vector3

class CoordinateSpace() {
    @JvmField val quaternion: Quaternion = Quaternion()
    @JvmField val coords: Vector3 = Vector3()

    constructor(coordinateSpace: CoordinateSpace) : this() {
        copy(coordinateSpace)
    }

    fun copy(coordinateSpace: CoordinateSpace) {
        this.quaternion.copy(coordinateSpace.quaternion)
        this.coords.copy(coordinateSpace.coords)
    }

    fun negate() {
        this.quaternion.negate()
        this.coords.negate()
        this.coords.transform(this.quaternion)
    }

    fun offset(pos2: CoordinateSpace) {
        this.quaternion.multiply(pos2.quaternion)
        this.coords.transform(pos2.quaternion)
        this.coords.add(pos2.coords)
    }

    override fun toString(): String {
        return "[${this.quaternion}|${this.coords}]"
    }
}
