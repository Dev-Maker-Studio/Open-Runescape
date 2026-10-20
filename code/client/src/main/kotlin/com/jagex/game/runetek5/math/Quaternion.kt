package com.jagex.game.runetek5.math

class Quaternion {

    @JvmField var x: Float = 0f
    @JvmField var y: Float = 0f
    @JvmField var z: Float = 0f

    @JvmField var w: Float = 1f

    constructor() {
        reset()
    }

    constructor(x: Float, y: Float, z: Float) {
        set(x, y, z, 0.0f)
    }

    constructor(other: Quaternion) {
        copy(other)
    }

    fun store() {
        synchronized(QuaternionPool.pool) {
            if (QuaternionPool.cursor < QuaternionPool.capacity - 1) {
                QuaternionPool.pool[QuaternionPool.cursor++] = this
            }
        }
    }

    fun set(x: Float, y: Float, z: Float, w: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    fun copy(source: Quaternion) {
        x = source.x
        y = source.y
        z = source.z
        w = source.w
    }

    fun rotate(x: Float, y: Float, z: Float, angleInRadians: Float) {
        val halfRotationSine = StrictMath.sin((angleInRadians * 0.5f).toDouble()).toFloat()
        val halfRotationCosine = StrictMath.cos((angleInRadians * 0.5f).toDouble()).toFloat()
        this.x = x * halfRotationSine
        this.y = y * halfRotationSine
        this.z = z * halfRotationSine
        w = halfRotationCosine
    }

    fun rotateEuler(yaw: Float, pitch: Float, roll: Float) {
        rotate(0.0f, 1.0f, 0.0f, yaw)
        val q = QuaternionPool.newQuaternion()
        q.rotate(1.0f, 0.0f, 0.0f, pitch)
        multiply(q)
        q.rotate(0.0f, 0.0f, 1.0f, roll)
        multiply(q)
        q.store()
    }

    fun reset() {
        z = 0.0f
        y = 0.0f
        x = 0.0f
        w = 1.0f
    }

    fun negate() {
        x = -x
        y = -y
        z = -z
    }

    fun multiply(other: Quaternion) {
        set(
            other.w * x + other.x * w + other.y * z - other.z * y,
            other.w * y - other.x * z + other.y * w + other.z * x,
            other.w * z + other.x * y - other.y * x + other.z * w,
            other.w * w - other.x * x - other.y * y - other.z * z
        )
    }

    override fun toString(): String {
        return "$x,$y,$z,$w"
    }
}
