package com.jagex.game.runetek5.math

import com.jagex.game.runetek5.movement.CoordinateSpace
import kotlin.math.cos
import kotlin.math.sin

/**
 * [J]
 * @author Steven
 *
 */
class Matrix43 {

    var xB: Float = 0.0f
    var yA: Float = 0.0f
    var xC: Float = 0.0f
    var zA: Float = 0.0f
    var yC: Float = 0.0f
    var zB: Float = 0.0f
    var xD: Float = 0.0f
    var yD: Float = 0.0f
    var zD: Float = 0.0f
    var xA: Float = 0.0f
    var yB: Float = 0.0f
    var zC: Float = 0.0f

    constructor(matrix44var1: Matrix43) {
        copyFrom(matrix44var1)
    }

    constructor() {
        reset()
    }

    fun updateIdentitiyMatrix() {
        val originalX = this.xD
        val originalY = this.yD
        val f_3 = this.xB
        this.xB = this.yA
        this.yA = f_3
        val f_4 = this.xC
        this.xC = this.zA
        this.zA = f_4
        val f_5 = this.yC
        this.yC = this.zB
        this.zB = f_5
        this.xD = -(originalX * this.xA + originalY * this.xB + this.zD * this.xC)
        this.yD = -(originalX * this.yA + originalY * this.yB + this.zD * this.yC)
        this.zD = -(originalX * this.zA + originalY * this.zB + this.zD * this.zC)
    }

    fun multiply(otherMatrix: Matrix43) {
        val oldXA = this.xA
        val oldYA = this.yA
        val oldXB = this.xB
        val oldYB = this.yB
        val oldXC = this.xC
        val oldYC = this.yC
        val oldXD = this.xD
        val oldYD = this.yD
        val oldZA = this.zA
        val oldZB = this.zB
        val oldZC = this.zC
        val oldZD = this.zD
        this.xA = oldXA * otherMatrix.xA + oldYA * otherMatrix.xB + oldZA * otherMatrix.xC
        this.yA = oldXA * otherMatrix.yA + oldYA * otherMatrix.yB + oldZA * otherMatrix.yC
        this.zA = oldXA * otherMatrix.zA + oldYA * otherMatrix.zB + oldZA * otherMatrix.zC
        this.xB = oldXB * otherMatrix.xA + oldYB * otherMatrix.xB + oldZB * otherMatrix.xC
        this.yB = oldXB * otherMatrix.yA + oldYB * otherMatrix.yB + oldZB * otherMatrix.yC
        this.zB = oldXB * otherMatrix.zA + oldYB * otherMatrix.zB + oldZB * otherMatrix.zC
        this.xC = oldXC * otherMatrix.xA + oldYC * otherMatrix.xB + oldZC * otherMatrix.xC
        this.yC = oldXC * otherMatrix.yA + oldYC * otherMatrix.yB + oldZC * otherMatrix.yC
        this.zC = oldXC * otherMatrix.zA + oldYC * otherMatrix.zB + oldZC * otherMatrix.zC
        this.xD = oldXD * otherMatrix.xA + oldYD * otherMatrix.xB + oldZD * otherMatrix.xC + otherMatrix.xD
        this.yD = oldXD * otherMatrix.yA + oldYD * otherMatrix.yB + oldZD * otherMatrix.yC + otherMatrix.yD
        this.zD = oldXD * otherMatrix.zA + oldYD * otherMatrix.zB + oldZD * otherMatrix.zC + otherMatrix.zD
    }

    fun copyFrom(matrix44var1: Matrix43) {
        this.xA = matrix44var1.xA
        this.xB = matrix44var1.xB
        this.xC = matrix44var1.xC
        this.xD = matrix44var1.xD
        this.yA = matrix44var1.yA
        this.yB = matrix44var1.yB
        this.yC = matrix44var1.yC
        this.yD = matrix44var1.yD
        this.zA = matrix44var1.zA
        this.zB = matrix44var1.zB
        this.zC = matrix44var1.zC
        this.zD = matrix44var1.zD
    }

    fun translateFromCoordspace(coordinateSpace: CoordinateSpace) {
        setFromQuaternion(coordinateSpace.quaternion)
        translate(coordinateSpace.coords)
    }

    fun reset() {
        this.zD = 0.0f
        this.yD = 0.0f
        this.xD = 0.0f
        this.yC = 0.0f
        this.xC = 0.0f
        this.zB = 0.0f
        this.xB = 0.0f
        this.zA = 0.0f
        this.yA = 0.0f
        this.zC = 1.0f
        this.yB = 1.0f
        this.xA = 1.0f
    }

    fun setTextureCoordTransform(rotation: Int, i_2: Int, i_3: Int, translateX: Float, translateY: Float, translateZ: Float) {
        if (rotation != 0) {
            val f_7 = MatrixRotations.COS[rotation and 0x3fff]
            val f_8 = MatrixRotations.SIN[rotation and 0x3fff]
            this.zC = 1.0f
            this.yC = 0.0f
            this.xC = 0.0f
            this.zB = 0.0f
            this.zA = 0.0f
            this.xA = 2.0f * f_7 * i_2
            this.yB = 2.0f * f_7 * i_3
            this.yA = 2.0f * f_8 * i_2
            this.xB = -2.0f * f_8 * i_3
            this.xD = (i_2 * 2) * (0.5f * f_8 - 0.5f * f_7) + translateX
            this.yD = (i_3 * 2) * (-0.5f * f_8 - 0.5f * f_7) + translateY
        } else {
            this.yC = 0.0f
            this.xC = 0.0f
            this.zB = 0.0f
            this.xB = 0.0f
            this.zA = 0.0f
            this.yA = 0.0f
            this.xA = (i_2 * 2).toFloat()
            this.yB = (i_3 * 2).toFloat()
            this.zC = 1.0f
            this.xD = translateX - i_2
            this.yD = translateY - i_3
        }
        this.zD = translateZ
    }

    fun setSwapYZ() {
        this.xA = 1.0f
        this.xB = 0.0f
        this.xC = 0.0f
        this.xD = 0.0f
        this.yA = 0.0f
        this.yB = 0.0f
        this.yC = 1.0f
        this.yD = 0.0f
        this.zA = 0.0f
        this.zB = 1.0f
        this.zC = 0.0f
        this.zD = 0.0f
    }

    fun updateMatrix(matrix4x4var: Matrix43) {
        if (this === matrix4x4var) {
            updateIdentitiyMatrix()
        } else {
            this.xA = matrix4x4var.xA
            this.xB = matrix4x4var.yA
            this.xC = matrix4x4var.zA
            this.yA = matrix4x4var.xB
            this.yB = matrix4x4var.yB
            this.yC = matrix4x4var.zB
            this.zA = matrix4x4var.xC
            this.zB = matrix4x4var.yC
            this.zC = matrix4x4var.zC
            this.xD = -(matrix4x4var.xD * this.xA + matrix4x4var.yD * this.xB + matrix4x4var.zD * this.xC)
            this.yD = -(matrix4x4var.xD * this.yA + matrix4x4var.yD * this.yB + matrix4x4var.zD * this.yC)
            this.zD = -(matrix4x4var.xD * this.zA + matrix4x4var.yD * this.zB + matrix4x4var.zD * this.zC)
        }
    }

    // rotate around axis
    fun rotateMatrix(x: Float, y: Float, z: Float, angle: Float) {
        val cos = cos(angle.toDouble()).toFloat()
        val sin = sin(angle.toDouble()).toFloat()
        this.xA = cos + x * x * (1.0f - cos)
        this.yA = z * sin + y * x * (1.0f - cos)
        this.zA = -y * sin + z * x * (1.0f - cos)
        this.xB = -z * sin + x * y * (1.0f - cos)
        this.yB = cos + y * y * (1.0f - cos)
        this.zB = x * sin + z * y * (1.0f - cos)
        this.xC = y * sin + x * z * (1.0f - cos)
        this.yC = -x * sin + y * z * (1.0f - cos)
        this.zC = cos + z * z * (1.0f - cos)
        this.zD = 0.0f
        this.yD = 0.0f
        this.xD = 0.0f
    }

    fun translate(offsetX: Float, offsetY: Float, offsetZ: Float) {
        this.xD += offsetX
        this.yD += offsetY
        this.zD += offsetZ
    }

    /**
     * Rotates the matrix around the given axis by the given angle.
     *
     * @param axisX x-component of the axis vector
     * @param axisY y-component of the axis vector
     * @param axisZ z-component of the axis vector
     * @param theta angle to rotate by
     */
    fun rotate(axisX: Float, axisY: Float, axisZ: Float, theta: Float) {
        val cos = cos(theta.toDouble()).toFloat()
        val sin = sin(theta.toDouble()).toFloat()

        // Calculate the rotation matrix elements
        val m00 = cos + axisX * axisX * (1.0f - cos)
        val m01 = axisZ * sin + axisY * axisX * (1.0f - cos)
        val m02 = -axisY * sin + axisZ * axisX * (1.0f - cos)
        val m10 = -axisZ * sin + axisX * axisY * (1.0f - cos)
        val m11 = cos + axisY * axisY * (1.0f - cos)
        val m12 = axisX * sin + axisZ * axisY * (1.0f - cos)
        val m20 = axisY * sin + axisX * axisZ * (1.0f - cos)
        val m21 = -axisX * sin + axisY * axisZ * (1.0f - cos)
        val m22 = cos + axisZ * axisZ * (1.0f - cos)

        // Apply the rotation matrix to the matrix
        val newXA = this.xA * m00 + this.yA * m10 + this.zA * m20
        val newYA = this.xA * m01 + this.yA * m11 + this.zA * m21
        val newZA = this.xA * m02 + this.yA * m12 + this.zA * m22
        this.xA = newXA
        this.yA = newYA
        this.zA = newZA

        val newXB = this.xB * m00 + this.yB * m10 + this.zB * m20
        val newYB = this.xB * m01 + this.yB * m11 + this.zB * m21
        val newZB = this.xB * m02 + this.yB * m12 + this.zB * m22
        this.xB = newXB
        this.yB = newYB
        this.zB = newZB

        val newXC = this.xC * m00 + this.yC * m10 + this.zC * m20
        val newYC = this.xC * m01 + this.yC * m11 + this.zC * m21
        val newZC = this.xC * m02 + this.yC * m12 + this.zC * m22
        this.xC = newXC
        this.yC = newYC
        this.zC = newZC

        val newXD = this.xD * m00 + this.yD * m10 + this.zD * m20
        val newYD = this.xD * m01 + this.yD * m11 + this.zD * m21
        val newZD = this.xD * m02 + this.yD * m12 + this.zD * m22
        this.xD = newXD
        this.yD = newYD
        this.zD = newZD
    }

    fun setViewPosition(viewX: Float, viewY: Float, viewZ: Float) {
        this.yC = 0.0f
        this.xC = 0.0f
        this.zB = 0.0f
        this.xB = 0.0f
        this.zA = 0.0f
        this.yA = 0.0f
        this.zC = 1.0f
        this.yB = 1.0f
        this.xA = 1.0f
        this.xD = viewX
        this.yD = viewY
        this.zD = viewZ
    }

    override fun toString(): String {
        return "$xA,$xB,$xC,$xD - $yA,$yB,$yC,$yD - $zA,$zB,$zC,$zD"
    }

    fun calculatePerspective(f_1: Float, f_2: Float, f_3: Float, perspective: FloatArray) {
        perspective[0] = this.xA * f_1 + this.xB * f_2 + this.xC * f_3 + this.xD
        perspective[1] = this.yA * f_1 + this.yB * f_2 + this.yC * f_3 + this.yD
        perspective[2] = this.zA * f_1 + this.zB * f_2 + this.zC * f_3 + this.zD
    }

    fun calculateEyePosition(x: Float, y: Float, z: Float, eyePosition: FloatArray) {
        var x = x
        var y = y
        var z = z
        x -= this.xD
        y -= this.yD
        z -= this.zD
        eyePosition[0] = (this.xA * x + this.yA * y + this.zA * z).toInt().toFloat()
        eyePosition[1] = (this.xB * x + this.yB * y + this.zB * z).toInt().toFloat()
        eyePosition[2] = (this.xC * x + this.yC * y + this.zC * z).toInt().toFloat()
    }

    fun transformDirection(floats_1: FloatArray) {
        val f_2 = floats_1[0]
        val f_3 = floats_1[1]
        val f_4 = floats_1[2]
        floats_1[0] = this.xA * f_2 + this.yA * f_3 + this.zA * f_4
        floats_1[1] = this.xB * f_2 + this.yB * f_3 + this.zB * f_4
        floats_1[2] = this.xC * f_2 + this.yC * f_3 + this.zC * f_4
    }

    fun cacheInto(floats_1: FloatArray): FloatArray {
        floats_1[0] = this.xD
        floats_1[1] = this.yD
        floats_1[2] = this.zD
        return floats_1
    }

    fun setFromQuaternion(q: Quaternion) {
        computeMatrix(q.x, q.y, q.z, q.w)
    }

    fun computeMatrix(i: Float, j: Float, k: Float, scalar: Float) {
        val i2 = i * i
        val ij = i * j
        val ik = i * k
        val is_ = i * scalar
        val j2 = j * j
        val jk = j * k
        val js = j * scalar
        val k2 = k * k
        val ks = k * scalar
        this.xA = 1.0f - 2.0f * (j2 + k2)
        this.xB = 2.0f * (ij - ks)
        this.xC = 2.0f * (ik + js)
        this.yA = 2.0f * (ij + ks)
        this.yB = 1.0f - 2.0f * (i2 + k2)
        this.yC = 2.0f * (jk - is_)
        this.zA = 2.0f * (ik - js)
        this.zB = 2.0f * (jk + is_)
        this.zC = 1.0f - 2.0f * (i2 + j2)
        this.zD = 0.0f
        this.yD = 0.0f
        this.xD = 0.0f
    }

    fun transformPointToInteger(floats_1: FloatArray) {
        val f_2 = floats_1[0] - this.xD
        val f_3 = floats_1[1] - this.yD
        val f_4 = floats_1[2] - this.zD
        floats_1[0] = (this.xA * f_2 + this.yA * f_3 + this.zA * f_4).toInt().toFloat()
        floats_1[1] = (this.xB * f_2 + this.yB * f_3 + this.zB * f_4).toInt().toFloat()
        floats_1[2] = (this.xC * f_2 + this.yC * f_3 + this.zC * f_4).toInt().toFloat()
    }

    fun scale(scaleX: Float, scaleY: Float) {
        this.xA *= scaleX
        this.xB *= scaleX
        this.xC *= scaleX
        this.xD *= scaleX
        this.yA *= scaleY
        this.yB *= scaleY
        this.yC *= scaleY
        this.yD *= scaleY
        this.zA *= 1.0f
        this.zB *= 1.0f
        this.zC *= 1.0f
        this.zD *= 1.0f
    }

    fun setScale(scaleX: Float, scaleY: Float, scaleZ: Float) {
        this.xA = scaleX
        this.xB = 0.0f
        this.xC = 0.0f
        this.xD = 0.0f
        this.yA = 0.0f
        this.yB = scaleY
        this.yC = 0.0f
        this.yD = 0.0f
        this.zA = 0.0f
        this.zB = 0.0f
        this.zC = scaleZ
        this.zD = 0.0f
    }

    fun multiply(left: Matrix43, right: Matrix43) {
        this.xA = left.xA * right.xA + left.yA * right.xB + left.zA * right.xC
        this.yA = left.xA * right.yA + left.yA * right.yB + left.zA * right.yC
        this.zA = left.xA * right.zA + left.yA * right.zB + left.zA * right.zC
        this.xB = left.xB * right.xA + left.yB * right.xB + left.zB * right.xC
        this.yB = left.xB * right.yA + left.yB * right.yB + left.zB * right.yC
        this.zB = left.xB * right.zA + left.yB * right.zB + left.zB * right.zC
        this.xC = left.xC * right.xA + left.yC * right.xB + left.zC * right.xC
        this.yC = left.xC * right.yA + left.yC * right.yB + left.zC * right.yC
        this.zC = left.xC * right.zA + left.yC * right.zB + left.zC * right.zC
        this.xD = left.xD * right.xA + left.yD * right.xB + left.zD * right.xC + right.xD
        this.yD = left.xD * right.yA + left.yD * right.yB + left.zD * right.yC + right.yD
        this.zD = left.xD * right.zA + left.yD * right.zB + left.zD * right.zC + right.zD
    }

    fun translate(vector3: Vector3) {
        translate(vector3.x, vector3.y, vector3.z)
    }

    fun fromCoordinateSpace(class3051: CoordinateSpace) {
        val f_2 = class3051.quaternion.w * class3051.quaternion.w
        val f_3 = class3051.quaternion.w * class3051.quaternion.x
        val f_4 = class3051.quaternion.w * class3051.quaternion.y
        val f_5 = class3051.quaternion.w * class3051.quaternion.z
        val f_6 = class3051.quaternion.x * class3051.quaternion.x
        val f_7 = class3051.quaternion.x * class3051.quaternion.y
        val f_8 = class3051.quaternion.x * class3051.quaternion.z
        val f_9 = class3051.quaternion.y * class3051.quaternion.y
        val f_10 = class3051.quaternion.y * class3051.quaternion.z
        val f_11 = class3051.quaternion.z * class3051.quaternion.z
        this.xA = f_6 + f_2 - f_11 - f_9
        this.yA = f_7 + f_5 + f_7 + f_5
        this.zA = f_8 - f_4 - f_4 + f_8
        this.xB = f_7 - f_5 - f_5 + f_7
        this.yB = f_9 + f_2 - f_6 - f_11
        this.zB = f_10 + f_3 + f_10 + f_3
        this.xC = f_8 + f_4 + f_8 + f_4
        this.yC = f_10 - f_3 - f_3 + f_10
        this.zC = f_11 + f_2 - f_9 - f_6
        this.xD = class3051.coords.x
        this.yD = class3051.coords.y
        this.zD = class3051.coords.z
    }

    companion object {
        @JvmField
        val TEMP = Matrix43()
    }
}
