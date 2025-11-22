package com.jagex.game.runetek5.math

/**
 * 4D vector for homogeneous coordinates.
 *
 * @author Brainpower - 04/03/2023
 */
class Vector4 {

    @JvmField var x: Float = 0f
    @JvmField var y: Float = 0f
    @JvmField var z: Float = 0f
    @JvmField var w: Float = 0f

    constructor(x: Float, y: Float, z: Float, w: Float) {
        set(x, y, z, w)
    }

    constructor() {
        reset()
    }

    /**
     * Sets the vector components.
     *
     * @param x The x component
     * @param y The y component
     * @param z The z component
     * @param w The w component
     */
    fun set(x: Float, y: Float, z: Float, w: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    /**
     * Copies values from another vector.
     *
     * @param vector4 The source vector
     */
    fun copy(vector4: Vector4) {
        set(vector4.x, vector4.y, vector4.z, vector4.w)
    }

    /**
     * Unpacks a bitpacked RGBA value into normalized float components.
     *
     * @param bitpacked The bitpacked color value (ARGB format)
     */
    fun unpack(bitpacked: Int) {
        set(
            ((bitpacked shr 16) and 0xff) * 0.003921569f,
            ((bitpacked shr 8) and 0xff) * 0.003921569f,
            (bitpacked and 0xff) * 0.003921569f,
            ((bitpacked shr 24) and 0xff) * 0.003921569f
        )
    }

    /**
     * Negates all components of the vector.
     */
    fun negate() {
        x = -x
        y = -y
        z = -z
        w = -w
    }

    /**
     * Resets all components to zero.
     */
    fun reset() {
        w = 0.0f
        z = 0.0f
        y = 0.0f
        x = 0.0f
    }

    override fun toString(): String {
        return "$x,$y,$z,$w"
    }

    /**
     * Transforms this vector by a 4x4 matrix.
     *
     * @param matrix The transformation matrix
     */
    fun concat(matrix: Matrix44) {
        val origX = x
        val origY = y
        val origZ = z
        val origW = w
        x = matrix.faceList[0] * origX + matrix.faceList[4] * origY + matrix.faceList[8] * origZ + matrix.faceList[12] * origW
        y = matrix.faceList[1] * origX + matrix.faceList[5] * origY + matrix.faceList[9] * origZ + matrix.faceList[13] * origW
        z = matrix.faceList[2] * origX + matrix.faceList[6] * origY + matrix.faceList[10] * origZ + matrix.faceList[14] * origW
        w = matrix.faceList[3] * origX + matrix.faceList[7] * origY + matrix.faceList[11] * origZ + matrix.faceList[15] * origW
    }

    /**
     * Scales all components by a multiplier.
     *
     * @param multiplier The scale factor
     */
    fun scale(multiplier: Float) {
        x *= multiplier
        y *= multiplier
        z *= multiplier
        w *= multiplier
    }
}
