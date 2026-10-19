package com.jagex.game.runetek5.math

class Vector3 {

    @JvmField var x: Float = 0f
    @JvmField var y: Float = 0f
    @JvmField var z: Float = 0f

    constructor()

    constructor(source: Vector3) {
        x = source.x
        y = source.y
        z = source.z
    }

    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun pushVectorStack() {
        synchronized(Vector3Pool.pool) {
            if (Vector3Pool.cursor < Vector3Pool.capacity - 1) {
                Vector3Pool.pool[Vector3Pool.cursor++] = this
            }
        }
    }

    fun set(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun copy(source: Vector3) {
        set(source.x, source.y, source.z)
    }

    fun negate() {
        x = -x
        y = -y
        z = -z
    }

    fun subtract(deltaX: Float, deltaY: Float, deltaZ: Float) {
        x -= deltaX
        y -= deltaY
        z -= deltaZ
    }

    fun magnitude(): Float {
        return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    fun transform(source: Quaternion) {
        val pure = QuaternionPool.pureQuaternionFrom(x, y, z)
        val negatedSource = QuaternionPool.negateQuaternion(source)
        val result = QuaternionPool.multiply(negatedSource, pure)
        result.multiply(source)
        set(result.x, result.y, result.z)
        pure.store()
        negatedSource.store()
        result.store()
    }

    fun add(vector: Vector3) {
        x += vector.x
        y += vector.y
        z += vector.z
    }

    fun transform(matrix: Matrix43) {
        val tmpX = x
        val tmpY = y
        x = matrix.xA * tmpX + matrix.xB * tmpY + matrix.xC * z
        y = matrix.yA * tmpX + matrix.yB * tmpY + matrix.yC * z
        z = matrix.zA * tmpX + matrix.zB * tmpY + matrix.zC * z
    }

    fun transformWithTranslation(matrix: Matrix43) {
        val tmpX = x
        val tmpY = y
        x = matrix.xA * tmpX + matrix.xB * tmpY + matrix.xC * z + matrix.xD
        y = matrix.yA * tmpX + matrix.yB * tmpY + matrix.yC * z + matrix.yD
        z = matrix.zA * tmpX + matrix.zB * tmpY + matrix.zC * z + matrix.zD
    }

    fun subtract(vector: Vector3) {
        x -= vector.x
        y -= vector.y
        z -= vector.z
    }

    override fun toString(): String {
        return "$x, $y, $z"
    }
}
