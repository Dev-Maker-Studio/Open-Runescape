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

    constructor(matrix441: Matrix44) {
        copyFrom(matrix441)
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

    fun fromMatrix(matrix4Var: Matrix43) {
        this.elements[0] = matrix4Var.xA
        this.elements[1] = matrix4Var.yA
        this.elements[2] = matrix4Var.zA
        this.elements[3] = 0.0f
        this.elements[4] = matrix4Var.xB
        this.elements[5] = matrix4Var.yB
        this.elements[6] = matrix4Var.zB
        this.elements[7] = 0.0f
        this.elements[8] = matrix4Var.xC
        this.elements[9] = matrix4Var.yC
        this.elements[10] = matrix4Var.zC
        this.elements[11] = 0.0f
        this.elements[12] = matrix4Var.xD
        this.elements[13] = matrix4Var.yD
        this.elements[14] = matrix4Var.zD
        this.elements[15] = 1.0f
    }

    fun multiplyBy(matrix441: Matrix44) {
        val f_2 = this.elements[0] * matrix441.elements[0] + this.elements[1] * matrix441.elements[4] + this.elements[2] * matrix441.elements[8] + this.elements[3] * matrix441.elements[12]
        val f_3 = this.elements[0] * matrix441.elements[1] + this.elements[1] * matrix441.elements[5] + this.elements[2] * matrix441.elements[9] + this.elements[3] * matrix441.elements[13]
        val f_4 = this.elements[0] * matrix441.elements[2] + this.elements[1] * matrix441.elements[6] + this.elements[2] * matrix441.elements[10] + this.elements[3] * matrix441.elements[14]
        val f_5 = this.elements[0] * matrix441.elements[3] + this.elements[1] * matrix441.elements[7] + this.elements[2] * matrix441.elements[11] + this.elements[3] * matrix441.elements[15]
        val f_6 = this.elements[4] * matrix441.elements[0] + this.elements[5] * matrix441.elements[4] + this.elements[6] * matrix441.elements[8] + this.elements[7] * matrix441.elements[12]
        val f_7 = this.elements[4] * matrix441.elements[1] + this.elements[5] * matrix441.elements[5] + this.elements[6] * matrix441.elements[9] + this.elements[7] * matrix441.elements[13]
        val f_8 = this.elements[4] * matrix441.elements[2] + this.elements[5] * matrix441.elements[6] + this.elements[6] * matrix441.elements[10] + this.elements[7] * matrix441.elements[14]
        val f_9 = this.elements[4] * matrix441.elements[3] + this.elements[5] * matrix441.elements[7] + this.elements[6] * matrix441.elements[11] + this.elements[7] * matrix441.elements[15]
        val f_10 = this.elements[8] * matrix441.elements[0] + this.elements[9] * matrix441.elements[4] + this.elements[10] * matrix441.elements[8] + this.elements[11] * matrix441.elements[12]
        val f_11 = this.elements[8] * matrix441.elements[1] + this.elements[9] * matrix441.elements[5] + this.elements[10] * matrix441.elements[9] + this.elements[11] * matrix441.elements[13]
        val f_12 = this.elements[8] * matrix441.elements[2] + this.elements[9] * matrix441.elements[6] + this.elements[10] * matrix441.elements[10] + this.elements[11] * matrix441.elements[14]
        val f_13 = this.elements[8] * matrix441.elements[3] + this.elements[9] * matrix441.elements[7] + this.elements[10] * matrix441.elements[11] + this.elements[11] * matrix441.elements[15]
        val f_14 = this.elements[12] * matrix441.elements[0] + this.elements[13] * matrix441.elements[4] + this.elements[14] * matrix441.elements[8] + this.elements[15] * matrix441.elements[12]
        val f_15 = this.elements[12] * matrix441.elements[1] + this.elements[13] * matrix441.elements[5] + this.elements[14] * matrix441.elements[9] + this.elements[15] * matrix441.elements[13]
        val f_16 = this.elements[12] * matrix441.elements[2] + this.elements[13] * matrix441.elements[6] + this.elements[14] * matrix441.elements[10] + this.elements[15] * matrix441.elements[14]
        val f_17 = this.elements[12] * matrix441.elements[3] + this.elements[13] * matrix441.elements[7] + this.elements[14] * matrix441.elements[11] + this.elements[15] * matrix441.elements[15]
        this.elements[0] = f_2
        this.elements[1] = f_3
        this.elements[2] = f_4
        this.elements[3] = f_5
        this.elements[4] = f_6
        this.elements[5] = f_7
        this.elements[6] = f_8
        this.elements[7] = f_9
        this.elements[8] = f_10
        this.elements[9] = f_11
        this.elements[10] = f_12
        this.elements[11] = f_13
        this.elements[12] = f_14
        this.elements[13] = f_15
        this.elements[14] = f_16
        this.elements[15] = f_17
    }

    fun invert() {
        val f_1 = 1.0f / determinant()
        val f_2 = (this.elements[5] * this.elements[10] * this.elements[15] - this.elements[5] * this.elements[11] * this.elements[14] - this.elements[6] * this.elements[9] * this.elements[15] + this.elements[6] * this.elements[11] * this.elements[13] + this.elements[7] * this.elements[9] * this.elements[14] - this.elements[7] * this.elements[10] * this.elements[13]) * f_1
        val f_3 = (-this.elements[1] * this.elements[10] * this.elements[15] + this.elements[1] * this.elements[11] * this.elements[14] + this.elements[2] * this.elements[9] * this.elements[15] - this.elements[2] * this.elements[11] * this.elements[13] - this.elements[3] * this.elements[9] * this.elements[14] + this.elements[3] * this.elements[10] * this.elements[13]) * f_1
        val f_4 = (this.elements[1] * this.elements[6] * this.elements[15] - this.elements[1] * this.elements[7] * this.elements[14] - this.elements[2] * this.elements[5] * this.elements[15] + this.elements[2] * this.elements[7] * this.elements[13] + this.elements[3] * this.elements[5] * this.elements[14] - this.elements[3] * this.elements[6] * this.elements[13]) * f_1
        val f_5 = (-this.elements[1] * this.elements[6] * this.elements[11] + this.elements[1] * this.elements[7] * this.elements[10] + this.elements[2] * this.elements[5] * this.elements[11] - this.elements[2] * this.elements[7] * this.elements[9] - this.elements[3] * this.elements[5] * this.elements[10] + this.elements[3] * this.elements[6] * this.elements[9]) * f_1
        val f_6 = (-this.elements[4] * this.elements[10] * this.elements[15] + this.elements[4] * this.elements[11] * this.elements[14] + this.elements[6] * this.elements[8] * this.elements[15] - this.elements[6] * this.elements[11] * this.elements[12] - this.elements[7] * this.elements[8] * this.elements[14] + this.elements[7] * this.elements[10] * this.elements[12]) * f_1
        val f_7 = (this.elements[0] * this.elements[10] * this.elements[15] - this.elements[0] * this.elements[11] * this.elements[14] - this.elements[2] * this.elements[8] * this.elements[15] + this.elements[2] * this.elements[11] * this.elements[12] + this.elements[3] * this.elements[8] * this.elements[14] - this.elements[3] * this.elements[10] * this.elements[12]) * f_1
        val f_8 = (-this.elements[0] * this.elements[6] * this.elements[15] + this.elements[0] * this.elements[7] * this.elements[14] + this.elements[2] * this.elements[4] * this.elements[15] - this.elements[2] * this.elements[7] * this.elements[12] - this.elements[3] * this.elements[4] * this.elements[14] + this.elements[3] * this.elements[6] * this.elements[12]) * f_1
        val f_9 = (this.elements[0] * this.elements[6] * this.elements[11] - this.elements[0] * this.elements[7] * this.elements[10] - this.elements[2] * this.elements[4] * this.elements[11] + this.elements[2] * this.elements[7] * this.elements[8] + this.elements[3] * this.elements[4] * this.elements[10] - this.elements[3] * this.elements[6] * this.elements[8]) * f_1
        val f_10 = (this.elements[4] * this.elements[9] * this.elements[15] - this.elements[4] * this.elements[11] * this.elements[13] - this.elements[5] * this.elements[8] * this.elements[15] + this.elements[5] * this.elements[11] * this.elements[12] + this.elements[7] * this.elements[8] * this.elements[13] - this.elements[7] * this.elements[9] * this.elements[12]) * f_1
        val f_11 = (-this.elements[0] * this.elements[9] * this.elements[15] + this.elements[0] * this.elements[11] * this.elements[13] + this.elements[1] * this.elements[8] * this.elements[15] - this.elements[1] * this.elements[11] * this.elements[12] - this.elements[3] * this.elements[8] * this.elements[13] + this.elements[3] * this.elements[9] * this.elements[12]) * f_1
        val f_12 = (this.elements[0] * this.elements[5] * this.elements[15] - this.elements[0] * this.elements[7] * this.elements[13] - this.elements[1] * this.elements[4] * this.elements[15] + this.elements[1] * this.elements[7] * this.elements[12] + this.elements[3] * this.elements[4] * this.elements[13] - this.elements[3] * this.elements[5] * this.elements[12]) * f_1
        val f_13 = (-this.elements[0] * this.elements[5] * this.elements[11] + this.elements[0] * this.elements[7] * this.elements[9] + this.elements[1] * this.elements[4] * this.elements[11] - this.elements[1] * this.elements[7] * this.elements[8] - this.elements[3] * this.elements[4] * this.elements[9] + this.elements[3] * this.elements[5] * this.elements[8]) * f_1
        val f_14 = (-this.elements[4] * this.elements[9] * this.elements[14] + this.elements[4] * this.elements[10] * this.elements[13] + this.elements[5] * this.elements[8] * this.elements[14] - this.elements[5] * this.elements[10] * this.elements[12] - this.elements[6] * this.elements[8] * this.elements[13] + this.elements[6] * this.elements[9] * this.elements[12]) * f_1
        val f_15 = (this.elements[0] * this.elements[9] * this.elements[14] - this.elements[0] * this.elements[10] * this.elements[13] - this.elements[1] * this.elements[8] * this.elements[14] + this.elements[1] * this.elements[10] * this.elements[12] + this.elements[2] * this.elements[8] * this.elements[13] - this.elements[2] * this.elements[9] * this.elements[12]) * f_1
        val f_16 = (-this.elements[0] * this.elements[5] * this.elements[14] + this.elements[0] * this.elements[6] * this.elements[13] + this.elements[1] * this.elements[4] * this.elements[14] - this.elements[1] * this.elements[6] * this.elements[12] - this.elements[2] * this.elements[4] * this.elements[13] + this.elements[2] * this.elements[5] * this.elements[12]) * f_1
        val f_17 = (this.elements[0] * this.elements[5] * this.elements[10] - this.elements[0] * this.elements[6] * this.elements[9] - this.elements[1] * this.elements[4] * this.elements[10] + this.elements[1] * this.elements[6] * this.elements[8] + this.elements[2] * this.elements[4] * this.elements[9] - this.elements[2] * this.elements[5] * this.elements[8]) * f_1
        this.elements[0] = f_2
        this.elements[1] = f_3
        this.elements[2] = f_4
        this.elements[3] = f_5
        this.elements[4] = f_6
        this.elements[5] = f_7
        this.elements[6] = f_8
        this.elements[7] = f_9
        this.elements[8] = f_10
        this.elements[9] = f_11
        this.elements[10] = f_12
        this.elements[11] = f_13
        this.elements[12] = f_14
        this.elements[13] = f_15
        this.elements[14] = f_16
        this.elements[15] = f_17
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

    fun scale(scaleFaceA: Float, scaleFaceB: Float, scaleFaceC: Float, matrix: FloatArray) {
        matrix[0] = this.elements[0] * scaleFaceA + this.elements[4] * scaleFaceB + this.elements[8] * scaleFaceC + this.elements[12]
        matrix[1] = this.elements[1] * scaleFaceA + this.elements[5] * scaleFaceB + this.elements[9] * scaleFaceC + this.elements[13]
        matrix[2] = this.elements[2] * scaleFaceA + this.elements[6] * scaleFaceB + this.elements[10] * scaleFaceC + this.elements[14]
        if (matrix.size > 3) {
            matrix[3] = this.elements[3] * scaleFaceA + this.elements[7] * scaleFaceB + this.elements[11] * scaleFaceC + this.elements[15]
        }
    }

    fun transformPoint(floats_1: FloatArray) {
        val f_2 = floats_1[0]
        val f_3 = floats_1[1]
        val f_4 = floats_1[2]
        floats_1[0] = this.elements[0] * f_2 + this.elements[4] * f_3 + this.elements[8] * f_4 + this.elements[12]
        floats_1[1] = this.elements[1] * f_2 + this.elements[5] * f_3 + this.elements[9] * f_4 + this.elements[13]
        floats_1[2] = this.elements[2] * f_2 + this.elements[6] * f_3 + this.elements[10] * f_4 + this.elements[14]
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

    fun setPerspective(centerX: Float, centerY: Float, f_3: Float, f_4: Float, viewRangeStart: Float, viewRangeEnd: Float, width: Float, height: Float) {
        this.elements[0] = f_3 * 2.0f / width
        this.elements[1] = 0.0f
        this.elements[2] = 0.0f
        this.elements[3] = 0.0f
        this.elements[4] = 0.0f
        this.elements[5] = f_4 * 2.0f / height
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

    fun setPerspectiveFromViewport(f_1: Float, f_2: Float, f_3: Float, f_4: Float, viewRangeStart: Float, viewRangeEnd: Float, f_7: Float, f_8: Float, f_9: Float) {
        setOrtho(-(f_1 * f_9) / f_3, f_9 * (f_7 - f_1) / f_3, -(f_2 * f_9) / f_4, f_9 * (f_8 - f_2) / f_4, viewRangeStart, viewRangeEnd)
    }

    fun getNearClipDistance(): Float {
        return -(this.elements[15] + this.elements[14]) / (this.elements[11] + this.elements[10])
    }

    fun extractRotationInto(floats_1: FloatArray): FloatArray {
        System.arraycopy(this.elements, 0, floats_1, 0, 16)
        floats_1[3] = 0.0f
        floats_1[7] = 0.0f
        floats_1[11] = 0.0f
        floats_1[12] = 0.0f
        floats_1[13] = 0.0f
        floats_1[14] = 0.0f
        floats_1[15] = 1.0f
        return floats_1
    }

    fun extractLeftPlane(floats_1: FloatArray): FloatArray {
        val f_2 = this.elements[3] + this.elements[0]
        val f_3 = this.elements[7] + this.elements[4]
        val f_4 = this.elements[11] + this.elements[8]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.elements[15] + this.elements[12]) / d_5).toFloat()
        return floats_1
    }

    fun extractRightPlane(floats_1: FloatArray): FloatArray {
        val f_2 = this.elements[3] - this.elements[0]
        val f_3 = this.elements[7] - this.elements[4]
        val f_4 = this.elements[11] - this.elements[8]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.elements[15] - this.elements[12]) / d_5).toFloat()
        return floats_1
    }

    fun extractTopPlane(floats_1: FloatArray): FloatArray {
        val f_2 = this.elements[3] - this.elements[1]
        val f_3 = this.elements[7] - this.elements[5]
        val f_4 = this.elements[11] - this.elements[9]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.elements[15] - this.elements[13]) / d_5).toFloat()
        return floats_1
    }

    fun extractNearPlane(floats_1: FloatArray): FloatArray {
        val f_2 = this.elements[3] + this.elements[2]
        val f_3 = this.elements[7] + this.elements[6]
        val f_4 = this.elements[11] + this.elements[10]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.elements[15] + this.elements[14]) / d_5).toFloat()
        return floats_1
    }

    fun extractFarPlane(floats_1: FloatArray): FloatArray {
        val f_2 = this.elements[3] - this.elements[2]
        val f_3 = this.elements[7] - this.elements[6]
        val f_4 = this.elements[11] - this.elements[10]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.elements[15] - this.elements[14]) / d_5).toFloat()
        return floats_1
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

    fun setRotatedTranslation(rotation: Int, i_2: Int, i_3: Int, f_4: Float, f_5: Float, f_6: Float) {
        var floats_9: FloatArray
        var floats_10: FloatArray
        var floats_11: FloatArray
        var floats_12: FloatArray
        var floats_13: FloatArray
        if (rotation != 0) {
            val f_7 = MatrixRotations.COS[rotation and 0x3fff]
            val f_8 = MatrixRotations.SIN[rotation and 0x3fff]
            this.elements[0] = f_7 * i_2
            this.elements[5] = f_7 * i_3
            this.elements[1] = f_8 * i_2
            this.elements[4] = -f_8 * i_3
            this.elements[10] = 1.0f
            floats_9 = this.elements
            floats_10 = this.elements
            floats_11 = this.elements
            this.elements[9] = 0.0f
            floats_11[8] = 0.0f
            floats_10[6] = 0.0f
            floats_9[2] = 0.0f
        } else {
            this.elements[0] = i_2.toFloat()
            this.elements[5] = i_3.toFloat()
            this.elements[10] = 1.0f
            floats_12 = this.elements
            floats_13 = this.elements
            floats_9 = this.elements
            floats_10 = this.elements
            floats_11 = this.elements
            this.elements[9] = 0.0f
            floats_11[8] = 0.0f
            floats_10[6] = 0.0f
            floats_9[4] = 0.0f
            floats_13[2] = 0.0f
            floats_12[1] = 0.0f
        }

        floats_12 = this.elements
        floats_13 = this.elements
        this.elements[11] = 0.0f
        floats_13[7] = 0.0f
        floats_12[3] = 0.0f
        this.elements[15] = 1.0f
        this.elements[12] = f_4
        this.elements[13] = f_5
        this.elements[14] = f_6
    }

    override fun hashCode(): Int {
        val b_1 = 1
        val i_2 = 31 * b_1 + elements.contentHashCode()
        return i_2
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
        for (x in 0 until 4) {
            for (y in 0 until 4) {
                if (y > 0) {
                    stringBuilder.append("\t")
                }
                stringBuilder.append(this.elements[y + x * 4])
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

    fun extractBottomPlane(floats_1: FloatArray): FloatArray {
        val f_2 = this.elements[3] + this.elements[1]
        val f_3 = this.elements[7] + this.elements[5]
        val f_4 = this.elements[11] + this.elements[9]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.elements[15] + this.elements[13]) / d_5).toFloat()
        return floats_1
    }

    fun multiply(multiplierA: Float, multiplierB: Float, multiplierC: Float, maxtrix: FloatArray) {
        maxtrix[0] = this.elements[0] * multiplierA + this.elements[4] * multiplierB + this.elements[8] * multiplierC
        maxtrix[1] = this.elements[1] * multiplierA + this.elements[5] * multiplierB + this.elements[9] * multiplierC
        maxtrix[2] = this.elements[2] * multiplierA + this.elements[6] * multiplierB + this.elements[10] * multiplierC
        if (maxtrix.size > 3) {
            maxtrix[3] = this.elements[3] * multiplierA + this.elements[7] * multiplierB + this.elements[11] * multiplierC
        }
    }
}
