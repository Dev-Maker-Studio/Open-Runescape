package com.jagex.game.runetek5.math

import kotlin.math.sqrt

/**
 * [J]
 * @author Brainpower
 *
 **/
class Matrix44 {

    /**
     * Structure:
     *
     * A
     * [0]	vertex_x_a
     * [1]	vertex_y_a
     * [2]	vertex_z_near_a
     * [3]	vertex_z_far_a
     *
     * B
     * [4]	vertex_x_b
     * [5]	vertex_y_b
     * [6]	vertex_z_near_b
     * [7]	vertex_z_far_b
     *
     * C
     * [8]	vertex_x_c
     * [9]	vertex_y_c
     * [10]	vertex_z_near_c
     * [11]	vertex_z_far_c
     *
     * D
     * [12]	vertex_x_d
     * [13]	vertex_y_d
     * [14]	vertex_z_near_d
     * [15]	vertex_z_far_d
     *
     */
    val elements = FloatArray(16)

    constructor(source: Matrix44) {
        copyFrom(source)
    }

    constructor() {
        identity()
    }

    fun copyInto(into: FloatArray): FloatArray {
        System.arraycopy(this.elements, 0, into, 0, 16)
        return into
    }

    fun identity() {
        this.elements[0] = 1.0f
        this.elements[1] = 0.0f
        this.elements[2] = 0.0f
        this.elements[3] = 0.0f
        this.elements[4] = 0.0f
        this.elements[5] = 1.0f
        this.elements[6] = 0.0f
        this.elements[7] = 0.0f
        this.elements[8] = 0.0f
        this.elements[9] = 0.0f
        this.elements[10] = 1.0f
        this.elements[11] = 0.0f
        this.elements[12] = 0.0f
        this.elements[13] = 0.0f
        this.elements[14] = 0.0f
        this.elements[15] = 1.0f
    }

    fun transpose() {
        val xA = this.elements[0]
        val xB = this.elements[4]
        val xC = this.elements[8]
        val xD = this.elements[12]
        val yA = this.elements[1]
        val yB = this.elements[5]
        val yC = this.elements[9]
        val yD = this.elements[13]
        val zNearA = this.elements[2]
        val zNearB = this.elements[6]
        val zNearC = this.elements[10]
        val zNearD = this.elements[14]
        val zFarA = this.elements[3]
        val zFarB = this.elements[7]
        val zFarC = this.elements[11]
        val zFarD = this.elements[15]
        this.elements[1] = xB
        this.elements[2] = xC
        this.elements[3] = xD
        this.elements[4] = yA
        this.elements[6] = yC
        this.elements[7] = yD
        this.elements[8] = zNearA
        this.elements[9] = zNearB
        this.elements[11] = zNearD
        this.elements[12] = zFarA
        this.elements[13] = zFarB
        this.elements[14] = zFarC
    }

    fun fromMatrix(source: Matrix43) {
        this.elements[0] = source.xA
        this.elements[1] = source.yA
        this.elements[2] = source.zA
        this.elements[3] = 0.0f
        this.elements[4] = source.xB
        this.elements[5] = source.yB
        this.elements[6] = source.zB
        this.elements[7] = 0.0f
        this.elements[8] = source.xC
        this.elements[9] = source.yC
        this.elements[10] = source.zC
        this.elements[11] = 0.0f
        this.elements[12] = source.xD
        this.elements[13] = source.yD
        this.elements[14] = source.zD
        this.elements[15] = 1.0f
    }

    fun multiplyBy(other: Matrix44) {
        val r00 = this.elements[0] * other.elements[0] + this.elements[1] * other.elements[4] + this.elements[2] * other.elements[8] + this.elements[3] * other.elements[12]
        val r01 = this.elements[0] * other.elements[1] + this.elements[1] * other.elements[5] + this.elements[2] * other.elements[9] + this.elements[3] * other.elements[13]
        val r02 = this.elements[0] * other.elements[2] + this.elements[1] * other.elements[6] + this.elements[2] * other.elements[10] + this.elements[3] * other.elements[14]
        val r03 = this.elements[0] * other.elements[3] + this.elements[1] * other.elements[7] + this.elements[2] * other.elements[11] + this.elements[3] * other.elements[15]
        val r10 = this.elements[4] * other.elements[0] + this.elements[5] * other.elements[4] + this.elements[6] * other.elements[8] + this.elements[7] * other.elements[12]
        val r11 = this.elements[4] * other.elements[1] + this.elements[5] * other.elements[5] + this.elements[6] * other.elements[9] + this.elements[7] * other.elements[13]
        val r12 = this.elements[4] * other.elements[2] + this.elements[5] * other.elements[6] + this.elements[6] * other.elements[10] + this.elements[7] * other.elements[14]
        val r13 = this.elements[4] * other.elements[3] + this.elements[5] * other.elements[7] + this.elements[6] * other.elements[11] + this.elements[7] * other.elements[15]
        val r20 = this.elements[8] * other.elements[0] + this.elements[9] * other.elements[4] + this.elements[10] * other.elements[8] + this.elements[11] * other.elements[12]
        val r21 = this.elements[8] * other.elements[1] + this.elements[9] * other.elements[5] + this.elements[10] * other.elements[9] + this.elements[11] * other.elements[13]
        val r22 = this.elements[8] * other.elements[2] + this.elements[9] * other.elements[6] + this.elements[10] * other.elements[10] + this.elements[11] * other.elements[14]
        val r23 = this.elements[8] * other.elements[3] + this.elements[9] * other.elements[7] + this.elements[10] * other.elements[11] + this.elements[11] * other.elements[15]
        val r30 = this.elements[12] * other.elements[0] + this.elements[13] * other.elements[4] + this.elements[14] * other.elements[8] + this.elements[15] * other.elements[12]
        val r31 = this.elements[12] * other.elements[1] + this.elements[13] * other.elements[5] + this.elements[14] * other.elements[9] + this.elements[15] * other.elements[13]
        val r32 = this.elements[12] * other.elements[2] + this.elements[13] * other.elements[6] + this.elements[14] * other.elements[10] + this.elements[15] * other.elements[14]
        val r33 = this.elements[12] * other.elements[3] + this.elements[13] * other.elements[7] + this.elements[14] * other.elements[11] + this.elements[15] * other.elements[15]
        this.elements[0] = r00
        this.elements[1] = r01
        this.elements[2] = r02
        this.elements[3] = r03
        this.elements[4] = r10
        this.elements[5] = r11
        this.elements[6] = r12
        this.elements[7] = r13
        this.elements[8] = r20
        this.elements[9] = r21
        this.elements[10] = r22
        this.elements[11] = r23
        this.elements[12] = r30
        this.elements[13] = r31
        this.elements[14] = r32
        this.elements[15] = r33
    }

    fun invert() {
        val inverseDet = 1.0f / determinant()
        val c00 = (this.elements[5] * this.elements[10] * this.elements[15] - this.elements[5] * this.elements[11] * this.elements[14] - this.elements[6] * this.elements[9] * this.elements[15] + this.elements[6] * this.elements[11] * this.elements[13] + this.elements[7] * this.elements[9] * this.elements[14] - this.elements[7] * this.elements[10] * this.elements[13]) * inverseDet
        val c01 = (-this.elements[1] * this.elements[10] * this.elements[15] + this.elements[1] * this.elements[11] * this.elements[14] + this.elements[2] * this.elements[9] * this.elements[15] - this.elements[2] * this.elements[11] * this.elements[13] - this.elements[3] * this.elements[9] * this.elements[14] + this.elements[3] * this.elements[10] * this.elements[13]) * inverseDet
        val c02 = (this.elements[1] * this.elements[6] * this.elements[15] - this.elements[1] * this.elements[7] * this.elements[14] - this.elements[2] * this.elements[5] * this.elements[15] + this.elements[2] * this.elements[7] * this.elements[13] + this.elements[3] * this.elements[5] * this.elements[14] - this.elements[3] * this.elements[6] * this.elements[13]) * inverseDet
        val c03 = (-this.elements[1] * this.elements[6] * this.elements[11] + this.elements[1] * this.elements[7] * this.elements[10] + this.elements[2] * this.elements[5] * this.elements[11] - this.elements[2] * this.elements[7] * this.elements[9] - this.elements[3] * this.elements[5] * this.elements[10] + this.elements[3] * this.elements[6] * this.elements[9]) * inverseDet
        val c10 = (-this.elements[4] * this.elements[10] * this.elements[15] + this.elements[4] * this.elements[11] * this.elements[14] + this.elements[6] * this.elements[8] * this.elements[15] - this.elements[6] * this.elements[11] * this.elements[12] - this.elements[7] * this.elements[8] * this.elements[14] + this.elements[7] * this.elements[10] * this.elements[12]) * inverseDet
        val c11 = (this.elements[0] * this.elements[10] * this.elements[15] - this.elements[0] * this.elements[11] * this.elements[14] - this.elements[2] * this.elements[8] * this.elements[15] + this.elements[2] * this.elements[11] * this.elements[12] + this.elements[3] * this.elements[8] * this.elements[14] - this.elements[3] * this.elements[10] * this.elements[12]) * inverseDet
        val c12 = (-this.elements[0] * this.elements[6] * this.elements[15] + this.elements[0] * this.elements[7] * this.elements[14] + this.elements[2] * this.elements[4] * this.elements[15] - this.elements[2] * this.elements[7] * this.elements[12] - this.elements[3] * this.elements[4] * this.elements[14] + this.elements[3] * this.elements[6] * this.elements[12]) * inverseDet
        val c13 = (this.elements[0] * this.elements[6] * this.elements[11] - this.elements[0] * this.elements[7] * this.elements[10] - this.elements[2] * this.elements[4] * this.elements[11] + this.elements[2] * this.elements[7] * this.elements[8] + this.elements[3] * this.elements[4] * this.elements[10] - this.elements[3] * this.elements[6] * this.elements[8]) * inverseDet
        val c20 = (this.elements[4] * this.elements[9] * this.elements[15] - this.elements[4] * this.elements[11] * this.elements[13] - this.elements[5] * this.elements[8] * this.elements[15] + this.elements[5] * this.elements[11] * this.elements[12] + this.elements[7] * this.elements[8] * this.elements[13] - this.elements[7] * this.elements[9] * this.elements[12]) * inverseDet
        val c21 = (-this.elements[0] * this.elements[9] * this.elements[15] + this.elements[0] * this.elements[11] * this.elements[13] + this.elements[1] * this.elements[8] * this.elements[15] - this.elements[1] * this.elements[11] * this.elements[12] - this.elements[3] * this.elements[8] * this.elements[13] + this.elements[3] * this.elements[9] * this.elements[12]) * inverseDet
        val c22 = (this.elements[0] * this.elements[5] * this.elements[15] - this.elements[0] * this.elements[7] * this.elements[13] - this.elements[1] * this.elements[4] * this.elements[15] + this.elements[1] * this.elements[7] * this.elements[12] + this.elements[3] * this.elements[4] * this.elements[13] - this.elements[3] * this.elements[5] * this.elements[12]) * inverseDet
        val c23 = (-this.elements[0] * this.elements[5] * this.elements[11] + this.elements[0] * this.elements[7] * this.elements[9] + this.elements[1] * this.elements[4] * this.elements[11] - this.elements[1] * this.elements[7] * this.elements[8] - this.elements[3] * this.elements[4] * this.elements[9] + this.elements[3] * this.elements[5] * this.elements[8]) * inverseDet
        val c30 = (-this.elements[4] * this.elements[9] * this.elements[14] + this.elements[4] * this.elements[10] * this.elements[13] + this.elements[5] * this.elements[8] * this.elements[14] - this.elements[5] * this.elements[10] * this.elements[12] - this.elements[6] * this.elements[8] * this.elements[13] + this.elements[6] * this.elements[9] * this.elements[12]) * inverseDet
        val c31 = (this.elements[0] * this.elements[9] * this.elements[14] - this.elements[0] * this.elements[10] * this.elements[13] - this.elements[1] * this.elements[8] * this.elements[14] + this.elements[1] * this.elements[10] * this.elements[12] + this.elements[2] * this.elements[8] * this.elements[13] - this.elements[2] * this.elements[9] * this.elements[12]) * inverseDet
        val c32 = (-this.elements[0] * this.elements[5] * this.elements[14] + this.elements[0] * this.elements[6] * this.elements[13] + this.elements[1] * this.elements[4] * this.elements[14] - this.elements[1] * this.elements[6] * this.elements[12] - this.elements[2] * this.elements[4] * this.elements[13] + this.elements[2] * this.elements[5] * this.elements[12]) * inverseDet
        val c33 = (this.elements[0] * this.elements[5] * this.elements[10] - this.elements[0] * this.elements[6] * this.elements[9] - this.elements[1] * this.elements[4] * this.elements[10] + this.elements[1] * this.elements[6] * this.elements[8] + this.elements[2] * this.elements[4] * this.elements[9] - this.elements[2] * this.elements[5] * this.elements[8]) * inverseDet
        this.elements[0] = c00
        this.elements[1] = c01
        this.elements[2] = c02
        this.elements[3] = c03
        this.elements[4] = c10
        this.elements[5] = c11
        this.elements[6] = c12
        this.elements[7] = c13
        this.elements[8] = c20
        this.elements[9] = c21
        this.elements[10] = c22
        this.elements[11] = c23
        this.elements[12] = c30
        this.elements[13] = c31
        this.elements[14] = c32
        this.elements[15] = c33
    }

    fun setDiagonal(x: Float, y: Float, zNear: Float, zFar: Float) {
        this.elements[0] = x
        this.elements[1] = 0.0f
        this.elements[2] = 0.0f
        this.elements[3] = 0.0f
        this.elements[4] = 0.0f
        this.elements[5] = y
        this.elements[6] = 0.0f
        this.elements[7] = 0.0f
        this.elements[8] = 0.0f
        this.elements[9] = 0.0f
        this.elements[10] = zNear
        this.elements[11] = 0.0f
        this.elements[12] = 0.0f
        this.elements[13] = 0.0f
        this.elements[14] = 0.0f
        this.elements[15] = zFar
    }

    fun isIdentity(): Boolean {
        return this.elements[0] == 1.0f && this.elements[1] == 0.0f && this.elements[2] == 0.0f && this.elements[3] == 0.0f && this.elements[4] == 0.0f && this.elements[5] == 1.0f && this.elements[6] == 0.0f && this.elements[7] == 0.0f && this.elements[8] == 0.0f && this.elements[9] == 0.0f && this.elements[10] == 1.0f && this.elements[11] == 0.0f && this.elements[12] == 0.0f && this.elements[13] == 0.0f && this.elements[14] == 0.0f && this.elements[15] == 1.0f
    }

    fun scale(x: Float, y: Float, z: Float, output: FloatArray) {
        output[0] = this.elements[0] * x + this.elements[4] * y + this.elements[8] * z + this.elements[12]
        output[1] = this.elements[1] * x + this.elements[5] * y + this.elements[9] * z + this.elements[13]
        output[2] = this.elements[2] * x + this.elements[6] * y + this.elements[10] * z + this.elements[14]
        if (output.size > 3) {
            output[3] = this.elements[3] * x + this.elements[7] * y + this.elements[11] * z + this.elements[15]
        }
    }

    fun transformPoint(point: FloatArray) {
        val x = point[0]
        val y = point[1]
        val z = point[2]
        point[0] = this.elements[0] * x + this.elements[4] * y + this.elements[8] * z + this.elements[12]
        point[1] = this.elements[1] * x + this.elements[5] * y + this.elements[9] * z + this.elements[13]
        point[2] = this.elements[2] * x + this.elements[6] * y + this.elements[10] * z + this.elements[14]
    }

    fun setOrtho(x: Float, width: Float, y: Float, height: Float, viewRangeStart: Float, viewRangeEnd: Float) {
        this.elements[0] = 2.0f / (width - x)
        this.elements[1] = 0.0f
        this.elements[2] = 0.0f
        this.elements[3] = 0.0f
        this.elements[4] = 0.0f
        this.elements[5] = 2.0f / (height - y)
        this.elements[6] = 0.0f
        this.elements[7] = 0.0f
        this.elements[8] = 0.0f
        this.elements[9] = 0.0f
        this.elements[10] = 2.0f / (viewRangeEnd - viewRangeStart)
        this.elements[11] = 0.0f
        this.elements[12] = -(width + x) / (width - x)
        this.elements[13] = -(height + y) / (height - y)
        this.elements[14] = -(viewRangeEnd + viewRangeStart) / (viewRangeEnd - viewRangeStart)
        this.elements[15] = 1.0f
    }

    fun setPerspective(centerX: Float, centerY: Float, focalX: Float, focalY: Float, viewRangeStart: Float, viewRangeEnd: Float, width: Float, height: Float) {
        this.elements[0] = focalX * 2.0f / width
        this.elements[1] = 0.0f
        this.elements[2] = 0.0f
        this.elements[3] = 0.0f
        this.elements[4] = 0.0f
        this.elements[5] = focalY * 2.0f / height
        this.elements[6] = 0.0f
        this.elements[7] = 0.0f
        this.elements[8] = 2.0f * centerX / width - 1.0f
        this.elements[9] = 2.0f * centerY / height - 1.0f
        this.elements[10] = (viewRangeEnd + viewRangeStart) / (viewRangeEnd - viewRangeStart)
        this.elements[11] = 1.0f
        this.elements[12] = 0.0f
        this.elements[13] = 0.0f
        this.elements[14] = 2.0f * viewRangeEnd * viewRangeStart / (viewRangeStart - viewRangeEnd)
        this.elements[15] = 0.0f
    }

    fun setPerspectiveFromViewport(originX: Float, originY: Float, focalX: Float, focalY: Float, viewRangeStart: Float, viewRangeEnd: Float, viewportWidth: Float, viewportHeight: Float, scale: Float) {
        setOrtho(-(originX * scale) / focalX, scale * (viewportWidth - originX) / focalX, -(originY * scale) / focalY, scale * (viewportHeight - originY) / focalY, viewRangeStart, viewRangeEnd)
    }

    fun getNearClipDistance(): Float {
        return -(this.elements[15] + this.elements[14]) / (this.elements[11] + this.elements[10])
    }

    fun extractRotationInto(output: FloatArray): FloatArray {
        System.arraycopy(this.elements, 0, output, 0, 16)
        output[3] = 0.0f
        output[7] = 0.0f
        output[11] = 0.0f
        output[12] = 0.0f
        output[13] = 0.0f
        output[14] = 0.0f
        output[15] = 1.0f
        return output
    }

    fun extractLeftPlane(plane: FloatArray): FloatArray {
        val nx = this.elements[3] + this.elements[0]
        val ny = this.elements[7] + this.elements[4]
        val nz = this.elements[11] + this.elements[8]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        plane[0] = (nx / length).toFloat()
        plane[1] = (ny / length).toFloat()
        plane[2] = (nz / length).toFloat()
        plane[3] = ((this.elements[15] + this.elements[12]) / length).toFloat()
        return plane
    }

    fun extractRightPlane(plane: FloatArray): FloatArray {
        val nx = this.elements[3] - this.elements[0]
        val ny = this.elements[7] - this.elements[4]
        val nz = this.elements[11] - this.elements[8]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        plane[0] = (nx / length).toFloat()
        plane[1] = (ny / length).toFloat()
        plane[2] = (nz / length).toFloat()
        plane[3] = ((this.elements[15] - this.elements[12]) / length).toFloat()
        return plane
    }

    fun extractTopPlane(plane: FloatArray): FloatArray {
        val nx = this.elements[3] - this.elements[1]
        val ny = this.elements[7] - this.elements[5]
        val nz = this.elements[11] - this.elements[9]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        plane[0] = (nx / length).toFloat()
        plane[1] = (ny / length).toFloat()
        plane[2] = (nz / length).toFloat()
        plane[3] = ((this.elements[15] - this.elements[13]) / length).toFloat()
        return plane
    }

    fun extractNearPlane(plane: FloatArray): FloatArray {
        val nx = this.elements[3] + this.elements[2]
        val ny = this.elements[7] + this.elements[6]
        val nz = this.elements[11] + this.elements[10]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        plane[0] = (nx / length).toFloat()
        plane[1] = (ny / length).toFloat()
        plane[2] = (nz / length).toFloat()
        plane[3] = ((this.elements[15] + this.elements[14]) / length).toFloat()
        return plane
    }

    fun extractFarPlane(plane: FloatArray): FloatArray {
        val nx = this.elements[3] - this.elements[2]
        val ny = this.elements[7] - this.elements[6]
        val nz = this.elements[11] - this.elements[10]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        plane[0] = (nx / length).toFloat()
        plane[1] = (ny / length).toFloat()
        plane[2] = (nz / length).toFloat()
        plane[3] = ((this.elements[15] - this.elements[14]) / length).toFloat()
        return plane
    }

    fun copy3x3NearMatrixInto(matrix: FloatArray): FloatArray {
        matrix[0] = this.elements[0]        // x_a
        matrix[1] = this.elements[1]        // y_a
        matrix[2] = this.elements[2]        // z_near_a
        matrix[3] = this.elements[4]        // x_b
        matrix[4] = this.elements[5]        // y_b
        matrix[5] = this.elements[6]        // z_near_b
        matrix[6] = this.elements[8]        // x_c
        matrix[7] = this.elements[9]        // y_c
        matrix[8] = this.elements[10]        // z_near_c
        return matrix
    }

    fun copyFullyInto(matrix: FloatArray): FloatArray {
        matrix[0] = this.elements[0]        // x_a
        matrix[1] = this.elements[4]        // x_b
        matrix[2] = this.elements[8]        // x_c
        matrix[3] = this.elements[12]        // x_d
        matrix[4] = this.elements[1]        // y_a
        matrix[5] = this.elements[5]        // y_b
        matrix[6] = this.elements[9]        // y_c
        matrix[7] = this.elements[13]        // y_d
        matrix[8] = this.elements[2]        // z_near_a
        matrix[9] = this.elements[6]        // z_near_b
        matrix[10] = this.elements[10]        // z_near_c
        matrix[11] = this.elements[14]        // z_near_d
        matrix[12] = this.elements[3]        // z_far_a
        matrix[13] = this.elements[7]        // z_far_b
        matrix[14] = this.elements[11]        // z_far_c
        matrix[15] = this.elements[15]        // z_far_d
        return matrix
    }

    // Should be given better name
    fun partialCopyInto(copyInto: FloatArray): FloatArray {
        copyInto[0] = this.elements[0]
        copyInto[1] = this.elements[1]
        copyInto[2] = 0.0f
        copyInto[3] = 0.0f
        copyInto[4] = this.elements[4]
        copyInto[5] = this.elements[5]
        copyInto[6] = 0.0f
        copyInto[7] = 0.0f
        copyInto[8] = this.elements[12]
        copyInto[9] = this.elements[13]
        copyInto[10] = this.elements[14]
        copyInto[11] = 0.0f
        copyInto[12] = 0.0f
        copyInto[13] = 0.0f
        copyInto[14] = 0.0f
        copyInto[15] = 1.0f
        return copyInto
    }

    fun setRotatedTranslation(rotation: Int, scaleX: Int, scaleY: Int, translateX: Float, translateY: Float, translateZ: Float) {
        var elementsRef: FloatArray
        var elementsRef2: FloatArray
        var elementsRef3: FloatArray
        var elementsRef4: FloatArray
        var elementsRef5: FloatArray
        if (rotation != 0) {
            val cosAngle = MatrixRotations.COS[rotation and 0x3fff]
            val sinAngle = MatrixRotations.SIN[rotation and 0x3fff]
            this.elements[0] = cosAngle * scaleX
            this.elements[5] = cosAngle * scaleY
            this.elements[1] = sinAngle * scaleX
            this.elements[4] = -sinAngle * scaleY
            this.elements[10] = 1.0f
            elementsRef = this.elements
            elementsRef2 = this.elements
            elementsRef3 = this.elements
            this.elements[9] = 0.0f
            elementsRef3[8] = 0.0f
            elementsRef2[6] = 0.0f
            elementsRef[2] = 0.0f
        } else {
            this.elements[0] = scaleX.toFloat()
            this.elements[5] = scaleY.toFloat()
            this.elements[10] = 1.0f
            elementsRef4 = this.elements
            elementsRef5 = this.elements
            elementsRef = this.elements
            elementsRef2 = this.elements
            elementsRef3 = this.elements
            this.elements[9] = 0.0f
            elementsRef3[8] = 0.0f
            elementsRef2[6] = 0.0f
            elementsRef[4] = 0.0f
            elementsRef5[2] = 0.0f
            elementsRef4[1] = 0.0f
        }

        elementsRef4 = this.elements
        elementsRef5 = this.elements
        this.elements[11] = 0.0f
        elementsRef5[7] = 0.0f
        elementsRef4[3] = 0.0f
        this.elements[15] = 1.0f
        this.elements[12] = translateX
        this.elements[13] = translateY
        this.elements[14] = translateZ
    }

    override fun hashCode(): Int {
        val prime = 1
        val result = 31 * prime + elements.contentHashCode()
        return result
    }

    fun getComponentsXY(matrixXY: FloatArray): FloatArray {
        matrixXY[0] = this.elements[0]
        matrixXY[1] = this.elements[1]
        matrixXY[2] = this.elements[4]
        matrixXY[3] = this.elements[5]
        matrixXY[4] = this.elements[8]
        matrixXY[5] = this.elements[9]
        matrixXY[6] = this.elements[12]
        matrixXY[7] = this.elements[13]
        return matrixXY
    }

    fun copyFrom(src: Matrix44) {
        System.arraycopy(src.elements, 0, this.elements, 0, 16)
    }

    fun setProduct(matrixA: Matrix44, matrixB: Matrix44) {
        val vertexXA = matrixA.elements[0] * matrixB.elements[0] + matrixA.elements[1] * matrixB.elements[4] + matrixA.elements[2] * matrixB.elements[8] + matrixA.elements[3] * matrixB.elements[12]
        val vertexYA = matrixA.elements[0] * matrixB.elements[1] + matrixA.elements[1] * matrixB.elements[5] + matrixA.elements[2] * matrixB.elements[9] + matrixA.elements[3] * matrixB.elements[13]
        val vertexZNearA = matrixA.elements[0] * matrixB.elements[2] + matrixA.elements[1] * matrixB.elements[6] + matrixA.elements[2] * matrixB.elements[10] + matrixA.elements[3] * matrixB.elements[14]
        val vertexZFarA = matrixA.elements[0] * matrixB.elements[3] + matrixA.elements[1] * matrixB.elements[7] + matrixA.elements[2] * matrixB.elements[11] + matrixA.elements[3] * matrixB.elements[15]
        val vertexXB = matrixA.elements[4] * matrixB.elements[0] + matrixA.elements[5] * matrixB.elements[4] + matrixA.elements[6] * matrixB.elements[8] + matrixA.elements[7] * matrixB.elements[12]
        val vertexYB = matrixA.elements[4] * matrixB.elements[1] + matrixA.elements[5] * matrixB.elements[5] + matrixA.elements[6] * matrixB.elements[9] + matrixA.elements[7] * matrixB.elements[13]
        val vertexZNearB = matrixA.elements[4] * matrixB.elements[2] + matrixA.elements[5] * matrixB.elements[6] + matrixA.elements[6] * matrixB.elements[10] + matrixA.elements[7] * matrixB.elements[14]
        val vertexZFarB = matrixA.elements[4] * matrixB.elements[3] + matrixA.elements[5] * matrixB.elements[7] + matrixA.elements[6] * matrixB.elements[11] + matrixA.elements[7] * matrixB.elements[15]
        val vertexXC = matrixA.elements[8] * matrixB.elements[0] + matrixA.elements[9] * matrixB.elements[4] + matrixA.elements[10] * matrixB.elements[8] + matrixA.elements[11] * matrixB.elements[12]
        val vertexYC = matrixA.elements[8] * matrixB.elements[1] + matrixA.elements[9] * matrixB.elements[5] + matrixA.elements[10] * matrixB.elements[9] + matrixA.elements[11] * matrixB.elements[13]
        val vertexZNearC = matrixA.elements[8] * matrixB.elements[2] + matrixA.elements[9] * matrixB.elements[6] + matrixA.elements[10] * matrixB.elements[10] + matrixA.elements[11] * matrixB.elements[14]
        val vertexZFarC = matrixA.elements[8] * matrixB.elements[3] + matrixA.elements[9] * matrixB.elements[7] + matrixA.elements[10] * matrixB.elements[11] + matrixA.elements[11] * matrixB.elements[15]
        val vertexXD = matrixA.elements[12] * matrixB.elements[0] + matrixA.elements[13] * matrixB.elements[4] + matrixA.elements[14] * matrixB.elements[8] + matrixA.elements[15] * matrixB.elements[12]
        val vertexYD = matrixA.elements[12] * matrixB.elements[1] + matrixA.elements[13] * matrixB.elements[5] + matrixA.elements[14] * matrixB.elements[9] + matrixA.elements[15] * matrixB.elements[13]
        val vertexZNearD = matrixA.elements[12] * matrixB.elements[2] + matrixA.elements[13] * matrixB.elements[6] + matrixA.elements[14] * matrixB.elements[10] + matrixA.elements[15] * matrixB.elements[14]
        val vertexZFarD = matrixA.elements[12] * matrixB.elements[3] + matrixA.elements[13] * matrixB.elements[7] + matrixA.elements[14] * matrixB.elements[11] + matrixA.elements[15] * matrixB.elements[15]
        this.elements[0] = vertexXA
        this.elements[1] = vertexYA
        this.elements[2] = vertexZNearA
        this.elements[3] = vertexZFarA
        this.elements[4] = vertexXB
        this.elements[5] = vertexYB
        this.elements[6] = vertexZNearB
        this.elements[7] = vertexZFarB
        this.elements[8] = vertexXC
        this.elements[9] = vertexYC
        this.elements[10] = vertexZNearC
        this.elements[11] = vertexZFarC
        this.elements[12] = vertexXD
        this.elements[13] = vertexYD
        this.elements[14] = vertexZNearD
        this.elements[15] = vertexZFarD
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Matrix44) {
            return false
        }
        for (index in 0 until 16) {
            if (this.elements[index] != other.elements[index]) {
                return false
            }
        }
        return true
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                if (col > 0) {
                    stringBuilder.append("\t")
                }
                stringBuilder.append(this.elements[col + row * 4])
            }
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    fun getDepthPerspectiveCD(): Float {
        return (this.elements[14] - this.elements[15]) / (this.elements[11] - this.elements[10])
    }

    fun copyXYInto(matrix: FloatArray): FloatArray {
        matrix[0] = this.elements[0] // x_a
        matrix[1] = this.elements[4] // x_b
        matrix[2] = this.elements[8] // x_c
        matrix[3] = this.elements[12]// x_d
        matrix[4] = this.elements[1] // y_a
        matrix[5] = this.elements[5] // y_b
        matrix[6] = this.elements[9] // y_c
        matrix[7] = this.elements[13]// y_d
        return matrix
    }

    fun determinant(): Float {
        return this.elements[0] * this.elements[5] * this.elements[10] * this.elements[15] - this.elements[0] * this.elements[5] * this.elements[11] * this.elements[14] - this.elements[0] * this.elements[6] * this.elements[9] * this.elements[15] + this.elements[0] * this.elements[6] * this.elements[11] * this.elements[13] + this.elements[0] * this.elements[7] * this.elements[9] * this.elements[14] - this.elements[0] * this.elements[7] * this.elements[10] * this.elements[13] - this.elements[1] * this.elements[4] * this.elements[10] * this.elements[15] + this.elements[1] * this.elements[4] * this.elements[11] * this.elements[14] + this.elements[1] * this.elements[6] * this.elements[8] * this.elements[15] - this.elements[1] * this.elements[6] * this.elements[11] * this.elements[12] - this.elements[1] * this.elements[7] * this.elements[8] * this.elements[14] + this.elements[1] * this.elements[7] * this.elements[10] * this.elements[12] + this.elements[2] * this.elements[4] * this.elements[9] * this.elements[15] - this.elements[2] * this.elements[4] * this.elements[11] * this.elements[13] - this.elements[2] * this.elements[5] * this.elements[8] * this.elements[15] + this.elements[2] * this.elements[5] * this.elements[11] * this.elements[12] + this.elements[2] * this.elements[7] * this.elements[8] * this.elements[13] - this.elements[2] * this.elements[7] * this.elements[9] * this.elements[12] - this.elements[3] * this.elements[4] * this.elements[9] * this.elements[14] + this.elements[3] * this.elements[4] * this.elements[10] * this.elements[13] + this.elements[3] * this.elements[5] * this.elements[8] * this.elements[14] - this.elements[3] * this.elements[5] * this.elements[10] * this.elements[12] - this.elements[3] * this.elements[6] * this.elements[8] * this.elements[13] + this.elements[3] * this.elements[6] * this.elements[9] * this.elements[12]
    }

    fun extractBottomPlane(plane: FloatArray): FloatArray {
        val nx = this.elements[3] + this.elements[1]
        val ny = this.elements[7] + this.elements[5]
        val nz = this.elements[11] + this.elements[9]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        plane[0] = (nx / length).toFloat()
        plane[1] = (ny / length).toFloat()
        plane[2] = (nz / length).toFloat()
        plane[3] = ((this.elements[15] + this.elements[13]) / length).toFloat()
        return plane
    }

    fun multiply(x: Float, y: Float, z: Float, output: FloatArray) {
        output[0] = this.elements[0] * x + this.elements[4] * y + this.elements[8] * z
        output[1] = this.elements[1] * x + this.elements[5] * y + this.elements[9] * z
        output[2] = this.elements[2] * x + this.elements[6] * y + this.elements[10] * z
        if (output.size > 3) {
            output[3] = this.elements[3] * x + this.elements[7] * y + this.elements[11] * z
        }
    }
}
