package com.jagex.game.runetek5.math

object Vector3Pool {

    @JvmField var pool: Array<Vector3?> = arrayOfNulls(0)
    @JvmField var cursor: Int = 0
    @JvmField var capacity: Int = 0

    @JvmStatic
    fun initVectorStack() {
        capacity = 100
        pool = arrayOfNulls(capacity)
        cursor = 0
    }

    @JvmStatic
    fun vector3From(source: Vector3): Vector3 {
        synchronized(pool) {
            val vector3: Vector3
            if (cursor == 0) {
                vector3 = Vector3(source)
            } else {
                pool[--cursor]!!.copy(source)
                vector3 = pool[cursor]!!
            }
            return vector3
        }
    }

    @JvmStatic
    fun vector3From(x: Float, y: Float, z: Float): Vector3 {
        synchronized(pool) {
            val vector3: Vector3
            if (cursor == 0) {
                vector3 = Vector3(x, y, z)
            } else {
                pool[--cursor]!!.set(x, y, z)
                vector3 = pool[cursor]!!
            }
            return vector3
        }
    }

    @JvmStatic
    fun delta(a: Vector3, b: Vector3): Vector3 {
        val temp = vector3From(a)
        temp.subtract(b)
        return temp
    }
}
