package com.jagex.game.runetek5.math

/**
 * Quaternion object pool to minimize memory allocations.
 *
 * Represents a cache used to minimize the amount of memory
 * allocated when using quaternions.
 *
 * Documentation: http://www.math.umd.edu/~immortal/MATH431/book/ch_quaternions.pdf
 *
 * @author Steven - 16/08/2023
 */
object QuaternionPool {

    @JvmField var pool: Array<Quaternion?> = emptyArray()
    @JvmField var cursor: Int = 0
    @JvmField var capacity: Int = 0

    @JvmStatic
    fun initialize() {
        capacity = 10
        pool = arrayOfNulls(capacity)
        cursor = 0
    }

    @JvmStatic
    fun newQuaternion(): Quaternion {
        synchronized(pool) {
            val quaternion: Quaternion
            if (cursor == 0) {
                quaternion = Quaternion()
            } else {
                pool[--cursor]!!.reset()
                quaternion = pool[cursor]!!
            }
            return quaternion
        }
    }

    @JvmStatic
    fun quaternionFrom(source: Quaternion): Quaternion {
        synchronized(pool) {
            val destination: Quaternion
            if (cursor == 0) {
                destination = Quaternion(source)
            } else {
                pool[--cursor]!!.copy(source)
                destination = pool[cursor]!!
            }
            return destination
        }
    }

    /**
     * Define a quaternion that represents a pure vector (also known as a pure quaternion).
     *
     * @param x The x component
     * @param y The y component
     * @param z The z component
     * @return A pure quaternion with the specified vector components
     */
    @JvmStatic
    fun pureQuaternionFrom(x: Float, y: Float, z: Float): Quaternion {
        synchronized(pool) {
            val quaternion: Quaternion
            if (cursor == 0) {
                quaternion = Quaternion(x, y, z)
            } else {
                pool[--cursor]!!.set(x, y, z, 0.0f)
                quaternion = pool[cursor]!!
            }
            return quaternion
        }
    }

    @JvmStatic
    fun negateQuaternion(source: Quaternion): Quaternion {
        val quaternion = quaternionFrom(source)
        quaternion.negate()
        return quaternion
    }

    @JvmStatic
    fun multiply(source: Quaternion, with: Quaternion): Quaternion {
        val destination = quaternionFrom(source)
        destination.multiply(with)
        return destination
    }
}
