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
    val faceList = FloatArray(16)

    constructor(matrix441: Matrix44) {
        copyFrom(matrix441)
    }

    constructor() {
        identity()
    }

    fun copyInto(into: FloatArray): FloatArray {
        System.arraycopy(this.faceList, 0, into, 0, 16)
        return into
    }

    fun identity() {
        this.faceList[0] = 1.0f
        this.faceList[1] = 0.0f
        this.faceList[2] = 0.0f
        this.faceList[3] = 0.0f
        this.faceList[4] = 0.0f
        this.faceList[5] = 1.0f
        this.faceList[6] = 0.0f
        this.faceList[7] = 0.0f
        this.faceList[8] = 0.0f
        this.faceList[9] = 0.0f
        this.faceList[10] = 1.0f
        this.faceList[11] = 0.0f
        this.faceList[12] = 0.0f
        this.faceList[13] = 0.0f
        this.faceList[14] = 0.0f
        this.faceList[15] = 1.0f
    }

    fun rotate() {
        val xA = this.faceList[0]
        val xB = this.faceList[4]
        val xC = this.faceList[8]
        val xD = this.faceList[12]
        val yA = this.faceList[1]
        val yB = this.faceList[5]
        val yC = this.faceList[9]
        val yD = this.faceList[13]
        val zNearA = this.faceList[2]
        val zNearB = this.faceList[6]
        val zNearC = this.faceList[10]
        val zNearD = this.faceList[14]
        val zFarA = this.faceList[3]
        val zFarB = this.faceList[7]
        val zFarC = this.faceList[11]
        val zFarD = this.faceList[15]
        this.faceList[1] = xB
        this.faceList[2] = xC
        this.faceList[3] = xD
        this.faceList[4] = yA
        this.faceList[6] = yC
        this.faceList[7] = yD
        this.faceList[8] = zNearA
        this.faceList[9] = zNearB
        this.faceList[11] = zNearD
        this.faceList[12] = zFarA
        this.faceList[13] = zFarB
        this.faceList[14] = zFarC
    }

    fun fromMatrix(matrix4Var: Matrix43) {
        this.faceList[0] = matrix4Var.xA
        this.faceList[1] = matrix4Var.yA
        this.faceList[2] = matrix4Var.zA
        this.faceList[3] = 0.0f
        this.faceList[4] = matrix4Var.xB
        this.faceList[5] = matrix4Var.yB
        this.faceList[6] = matrix4Var.zB
        this.faceList[7] = 0.0f
        this.faceList[8] = matrix4Var.xC
        this.faceList[9] = matrix4Var.yC
        this.faceList[10] = matrix4Var.zC
        this.faceList[11] = 0.0f
        this.faceList[12] = matrix4Var.xD
        this.faceList[13] = matrix4Var.yD
        this.faceList[14] = matrix4Var.zD
        this.faceList[15] = 1.0f
    }

    fun multiplyBy(matrix441: Matrix44) {
        val f_2 = this.faceList[0] * matrix441.faceList[0] + this.faceList[1] * matrix441.faceList[4] + this.faceList[2] * matrix441.faceList[8] + this.faceList[3] * matrix441.faceList[12]
        val f_3 = this.faceList[0] * matrix441.faceList[1] + this.faceList[1] * matrix441.faceList[5] + this.faceList[2] * matrix441.faceList[9] + this.faceList[3] * matrix441.faceList[13]
        val f_4 = this.faceList[0] * matrix441.faceList[2] + this.faceList[1] * matrix441.faceList[6] + this.faceList[2] * matrix441.faceList[10] + this.faceList[3] * matrix441.faceList[14]
        val f_5 = this.faceList[0] * matrix441.faceList[3] + this.faceList[1] * matrix441.faceList[7] + this.faceList[2] * matrix441.faceList[11] + this.faceList[3] * matrix441.faceList[15]
        val f_6 = this.faceList[4] * matrix441.faceList[0] + this.faceList[5] * matrix441.faceList[4] + this.faceList[6] * matrix441.faceList[8] + this.faceList[7] * matrix441.faceList[12]
        val f_7 = this.faceList[4] * matrix441.faceList[1] + this.faceList[5] * matrix441.faceList[5] + this.faceList[6] * matrix441.faceList[9] + this.faceList[7] * matrix441.faceList[13]
        val f_8 = this.faceList[4] * matrix441.faceList[2] + this.faceList[5] * matrix441.faceList[6] + this.faceList[6] * matrix441.faceList[10] + this.faceList[7] * matrix441.faceList[14]
        val f_9 = this.faceList[4] * matrix441.faceList[3] + this.faceList[5] * matrix441.faceList[7] + this.faceList[6] * matrix441.faceList[11] + this.faceList[7] * matrix441.faceList[15]
        val f_10 = this.faceList[8] * matrix441.faceList[0] + this.faceList[9] * matrix441.faceList[4] + this.faceList[10] * matrix441.faceList[8] + this.faceList[11] * matrix441.faceList[12]
        val f_11 = this.faceList[8] * matrix441.faceList[1] + this.faceList[9] * matrix441.faceList[5] + this.faceList[10] * matrix441.faceList[9] + this.faceList[11] * matrix441.faceList[13]
        val f_12 = this.faceList[8] * matrix441.faceList[2] + this.faceList[9] * matrix441.faceList[6] + this.faceList[10] * matrix441.faceList[10] + this.faceList[11] * matrix441.faceList[14]
        val f_13 = this.faceList[8] * matrix441.faceList[3] + this.faceList[9] * matrix441.faceList[7] + this.faceList[10] * matrix441.faceList[11] + this.faceList[11] * matrix441.faceList[15]
        val f_14 = this.faceList[12] * matrix441.faceList[0] + this.faceList[13] * matrix441.faceList[4] + this.faceList[14] * matrix441.faceList[8] + this.faceList[15] * matrix441.faceList[12]
        val f_15 = this.faceList[12] * matrix441.faceList[1] + this.faceList[13] * matrix441.faceList[5] + this.faceList[14] * matrix441.faceList[9] + this.faceList[15] * matrix441.faceList[13]
        val f_16 = this.faceList[12] * matrix441.faceList[2] + this.faceList[13] * matrix441.faceList[6] + this.faceList[14] * matrix441.faceList[10] + this.faceList[15] * matrix441.faceList[14]
        val f_17 = this.faceList[12] * matrix441.faceList[3] + this.faceList[13] * matrix441.faceList[7] + this.faceList[14] * matrix441.faceList[11] + this.faceList[15] * matrix441.faceList[15]
        this.faceList[0] = f_2
        this.faceList[1] = f_3
        this.faceList[2] = f_4
        this.faceList[3] = f_5
        this.faceList[4] = f_6
        this.faceList[5] = f_7
        this.faceList[6] = f_8
        this.faceList[7] = f_9
        this.faceList[8] = f_10
        this.faceList[9] = f_11
        this.faceList[10] = f_12
        this.faceList[11] = f_13
        this.faceList[12] = f_14
        this.faceList[13] = f_15
        this.faceList[14] = f_16
        this.faceList[15] = f_17
    }

    fun invert() {
        val f_1 = 1.0f / method6612()
        val f_2 = (this.faceList[5] * this.faceList[10] * this.faceList[15] - this.faceList[5] * this.faceList[11] * this.faceList[14] - this.faceList[6] * this.faceList[9] * this.faceList[15] + this.faceList[6] * this.faceList[11] * this.faceList[13] + this.faceList[7] * this.faceList[9] * this.faceList[14] - this.faceList[7] * this.faceList[10] * this.faceList[13]) * f_1
        val f_3 = (-this.faceList[1] * this.faceList[10] * this.faceList[15] + this.faceList[1] * this.faceList[11] * this.faceList[14] + this.faceList[2] * this.faceList[9] * this.faceList[15] - this.faceList[2] * this.faceList[11] * this.faceList[13] - this.faceList[3] * this.faceList[9] * this.faceList[14] + this.faceList[3] * this.faceList[10] * this.faceList[13]) * f_1
        val f_4 = (this.faceList[1] * this.faceList[6] * this.faceList[15] - this.faceList[1] * this.faceList[7] * this.faceList[14] - this.faceList[2] * this.faceList[5] * this.faceList[15] + this.faceList[2] * this.faceList[7] * this.faceList[13] + this.faceList[3] * this.faceList[5] * this.faceList[14] - this.faceList[3] * this.faceList[6] * this.faceList[13]) * f_1
        val f_5 = (-this.faceList[1] * this.faceList[6] * this.faceList[11] + this.faceList[1] * this.faceList[7] * this.faceList[10] + this.faceList[2] * this.faceList[5] * this.faceList[11] - this.faceList[2] * this.faceList[7] * this.faceList[9] - this.faceList[3] * this.faceList[5] * this.faceList[10] + this.faceList[3] * this.faceList[6] * this.faceList[9]) * f_1
        val f_6 = (-this.faceList[4] * this.faceList[10] * this.faceList[15] + this.faceList[4] * this.faceList[11] * this.faceList[14] + this.faceList[6] * this.faceList[8] * this.faceList[15] - this.faceList[6] * this.faceList[11] * this.faceList[12] - this.faceList[7] * this.faceList[8] * this.faceList[14] + this.faceList[7] * this.faceList[10] * this.faceList[12]) * f_1
        val f_7 = (this.faceList[0] * this.faceList[10] * this.faceList[15] - this.faceList[0] * this.faceList[11] * this.faceList[14] - this.faceList[2] * this.faceList[8] * this.faceList[15] + this.faceList[2] * this.faceList[11] * this.faceList[12] + this.faceList[3] * this.faceList[8] * this.faceList[14] - this.faceList[3] * this.faceList[10] * this.faceList[12]) * f_1
        val f_8 = (-this.faceList[0] * this.faceList[6] * this.faceList[15] + this.faceList[0] * this.faceList[7] * this.faceList[14] + this.faceList[2] * this.faceList[4] * this.faceList[15] - this.faceList[2] * this.faceList[7] * this.faceList[12] - this.faceList[3] * this.faceList[4] * this.faceList[14] + this.faceList[3] * this.faceList[6] * this.faceList[12]) * f_1
        val f_9 = (this.faceList[0] * this.faceList[6] * this.faceList[11] - this.faceList[0] * this.faceList[7] * this.faceList[10] - this.faceList[2] * this.faceList[4] * this.faceList[11] + this.faceList[2] * this.faceList[7] * this.faceList[8] + this.faceList[3] * this.faceList[4] * this.faceList[10] - this.faceList[3] * this.faceList[6] * this.faceList[8]) * f_1
        val f_10 = (this.faceList[4] * this.faceList[9] * this.faceList[15] - this.faceList[4] * this.faceList[11] * this.faceList[13] - this.faceList[5] * this.faceList[8] * this.faceList[15] + this.faceList[5] * this.faceList[11] * this.faceList[12] + this.faceList[7] * this.faceList[8] * this.faceList[13] - this.faceList[7] * this.faceList[9] * this.faceList[12]) * f_1
        val f_11 = (-this.faceList[0] * this.faceList[9] * this.faceList[15] + this.faceList[0] * this.faceList[11] * this.faceList[13] + this.faceList[1] * this.faceList[8] * this.faceList[15] - this.faceList[1] * this.faceList[11] * this.faceList[12] - this.faceList[3] * this.faceList[8] * this.faceList[13] + this.faceList[3] * this.faceList[9] * this.faceList[12]) * f_1
        val f_12 = (this.faceList[0] * this.faceList[5] * this.faceList[15] - this.faceList[0] * this.faceList[7] * this.faceList[13] - this.faceList[1] * this.faceList[4] * this.faceList[15] + this.faceList[1] * this.faceList[7] * this.faceList[12] + this.faceList[3] * this.faceList[4] * this.faceList[13] - this.faceList[3] * this.faceList[5] * this.faceList[12]) * f_1
        val f_13 = (-this.faceList[0] * this.faceList[5] * this.faceList[11] + this.faceList[0] * this.faceList[7] * this.faceList[9] + this.faceList[1] * this.faceList[4] * this.faceList[11] - this.faceList[1] * this.faceList[7] * this.faceList[8] - this.faceList[3] * this.faceList[4] * this.faceList[9] + this.faceList[3] * this.faceList[5] * this.faceList[8]) * f_1
        val f_14 = (-this.faceList[4] * this.faceList[9] * this.faceList[14] + this.faceList[4] * this.faceList[10] * this.faceList[13] + this.faceList[5] * this.faceList[8] * this.faceList[14] - this.faceList[5] * this.faceList[10] * this.faceList[12] - this.faceList[6] * this.faceList[8] * this.faceList[13] + this.faceList[6] * this.faceList[9] * this.faceList[12]) * f_1
        val f_15 = (this.faceList[0] * this.faceList[9] * this.faceList[14] - this.faceList[0] * this.faceList[10] * this.faceList[13] - this.faceList[1] * this.faceList[8] * this.faceList[14] + this.faceList[1] * this.faceList[10] * this.faceList[12] + this.faceList[2] * this.faceList[8] * this.faceList[13] - this.faceList[2] * this.faceList[9] * this.faceList[12]) * f_1
        val f_16 = (-this.faceList[0] * this.faceList[5] * this.faceList[14] + this.faceList[0] * this.faceList[6] * this.faceList[13] + this.faceList[1] * this.faceList[4] * this.faceList[14] - this.faceList[1] * this.faceList[6] * this.faceList[12] - this.faceList[2] * this.faceList[4] * this.faceList[13] + this.faceList[2] * this.faceList[5] * this.faceList[12]) * f_1
        val f_17 = (this.faceList[0] * this.faceList[5] * this.faceList[10] - this.faceList[0] * this.faceList[6] * this.faceList[9] - this.faceList[1] * this.faceList[4] * this.faceList[10] + this.faceList[1] * this.faceList[6] * this.faceList[8] + this.faceList[2] * this.faceList[4] * this.faceList[9] - this.faceList[2] * this.faceList[5] * this.faceList[8]) * f_1
        this.faceList[0] = f_2
        this.faceList[1] = f_3
        this.faceList[2] = f_4
        this.faceList[3] = f_5
        this.faceList[4] = f_6
        this.faceList[5] = f_7
        this.faceList[6] = f_8
        this.faceList[7] = f_9
        this.faceList[8] = f_10
        this.faceList[9] = f_11
        this.faceList[10] = f_12
        this.faceList[11] = f_13
        this.faceList[12] = f_14
        this.faceList[13] = f_15
        this.faceList[14] = f_16
        this.faceList[15] = f_17
    }

    fun method6525(x: Float, y: Float, zNear: Float, zFar: Float) {
        this.faceList[0] = x
        this.faceList[1] = 0.0f
        this.faceList[2] = 0.0f
        this.faceList[3] = 0.0f
        this.faceList[4] = 0.0f
        this.faceList[5] = y
        this.faceList[6] = 0.0f
        this.faceList[7] = 0.0f
        this.faceList[8] = 0.0f
        this.faceList[9] = 0.0f
        this.faceList[10] = zNear
        this.faceList[11] = 0.0f
        this.faceList[12] = 0.0f
        this.faceList[13] = 0.0f
        this.faceList[14] = 0.0f
        this.faceList[15] = zFar
    }

    fun method6526(): Boolean {
        return this.faceList[0] == 1.0f && this.faceList[1] == 0.0f && this.faceList[2] == 0.0f && this.faceList[3] == 0.0f && this.faceList[4] == 0.0f && this.faceList[5] == 1.0f && this.faceList[6] == 0.0f && this.faceList[7] == 0.0f && this.faceList[8] == 0.0f && this.faceList[9] == 0.0f && this.faceList[10] == 1.0f && this.faceList[11] == 0.0f && this.faceList[12] == 0.0f && this.faceList[13] == 0.0f && this.faceList[14] == 0.0f && this.faceList[15] == 1.0f
    }

    fun scale(scaleFaceA: Float, scaleFaceB: Float, scaleFaceC: Float, matrix: FloatArray) {
        matrix[0] = this.faceList[0] * scaleFaceA + this.faceList[4] * scaleFaceB + this.faceList[8] * scaleFaceC + this.faceList[12]
        matrix[1] = this.faceList[1] * scaleFaceA + this.faceList[5] * scaleFaceB + this.faceList[9] * scaleFaceC + this.faceList[13]
        matrix[2] = this.faceList[2] * scaleFaceA + this.faceList[6] * scaleFaceB + this.faceList[10] * scaleFaceC + this.faceList[14]
        if (matrix.size > 3) {
            matrix[3] = this.faceList[3] * scaleFaceA + this.faceList[7] * scaleFaceB + this.faceList[11] * scaleFaceC + this.faceList[15]
        }
    }

    fun method6528(floats_1: FloatArray) {
        val f_2 = floats_1[0]
        val f_3 = floats_1[1]
        val f_4 = floats_1[2]
        floats_1[0] = this.faceList[0] * f_2 + this.faceList[4] * f_3 + this.faceList[8] * f_4 + this.faceList[12]
        floats_1[1] = this.faceList[1] * f_2 + this.faceList[5] * f_3 + this.faceList[9] * f_4 + this.faceList[13]
        floats_1[2] = this.faceList[2] * f_2 + this.faceList[6] * f_3 + this.faceList[10] * f_4 + this.faceList[14]
    }

    fun method6530(x: Float, width: Float, y: Float, height: Float, viewRangeStart: Float, viewRangeEnd: Float) {
        this.faceList[0] = 2.0f / (width - x)
        this.faceList[1] = 0.0f
        this.faceList[2] = 0.0f
        this.faceList[3] = 0.0f
        this.faceList[4] = 0.0f
        this.faceList[5] = 2.0f / (height - y)
        this.faceList[6] = 0.0f
        this.faceList[7] = 0.0f
        this.faceList[8] = 0.0f
        this.faceList[9] = 0.0f
        this.faceList[10] = 2.0f / (viewRangeEnd - viewRangeStart)
        this.faceList[11] = 0.0f
        this.faceList[12] = -(width + x) / (width - x)
        this.faceList[13] = -(height + y) / (height - y)
        this.faceList[14] = -(viewRangeEnd + viewRangeStart) / (viewRangeEnd - viewRangeStart)
        this.faceList[15] = 1.0f
    }

    fun method6531(centerX: Float, centerY: Float, f_3: Float, f_4: Float, viewRangeStart: Float, viewRangeEnd: Float, width: Float, height: Float) {
        this.faceList[0] = f_3 * 2.0f / width
        this.faceList[1] = 0.0f
        this.faceList[2] = 0.0f
        this.faceList[3] = 0.0f
        this.faceList[4] = 0.0f
        this.faceList[5] = f_4 * 2.0f / height
        this.faceList[6] = 0.0f
        this.faceList[7] = 0.0f
        this.faceList[8] = 2.0f * centerX / width - 1.0f
        this.faceList[9] = 2.0f * centerY / height - 1.0f
        this.faceList[10] = (viewRangeEnd + viewRangeStart) / (viewRangeEnd - viewRangeStart)
        this.faceList[11] = 1.0f
        this.faceList[12] = 0.0f
        this.faceList[13] = 0.0f
        this.faceList[14] = 2.0f * viewRangeEnd * viewRangeStart / (viewRangeStart - viewRangeEnd)
        this.faceList[15] = 0.0f
    }

    fun method6532(f_1: Float, f_2: Float, f_3: Float, f_4: Float, viewRangeStart: Float, viewRangeEnd: Float, f_7: Float, f_8: Float, f_9: Float) {
        method6530(-(f_1 * f_9) / f_3, f_9 * (f_7 - f_1) / f_3, -(f_2 * f_9) / f_4, f_9 * (f_8 - f_2) / f_4, viewRangeStart, viewRangeEnd)
    }

    fun method6533(): Float {
        return -(this.faceList[15] + this.faceList[14]) / (this.faceList[11] + this.faceList[10])
    }

    fun method6534(floats_1: FloatArray): FloatArray {
        System.arraycopy(this.faceList, 0, floats_1, 0, 16)
        floats_1[3] = 0.0f
        floats_1[7] = 0.0f
        floats_1[11] = 0.0f
        floats_1[12] = 0.0f
        floats_1[13] = 0.0f
        floats_1[14] = 0.0f
        floats_1[15] = 1.0f
        return floats_1
    }

    fun method6535(floats_1: FloatArray): FloatArray {
        val f_2 = this.faceList[3] + this.faceList[0]
        val f_3 = this.faceList[7] + this.faceList[4]
        val f_4 = this.faceList[11] + this.faceList[8]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.faceList[15] + this.faceList[12]) / d_5).toFloat()
        return floats_1
    }

    fun method6536(floats_1: FloatArray): FloatArray {
        val f_2 = this.faceList[3] - this.faceList[0]
        val f_3 = this.faceList[7] - this.faceList[4]
        val f_4 = this.faceList[11] - this.faceList[8]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.faceList[15] - this.faceList[12]) / d_5).toFloat()
        return floats_1
    }

    fun method6538(floats_1: FloatArray): FloatArray {
        val f_2 = this.faceList[3] - this.faceList[1]
        val f_3 = this.faceList[7] - this.faceList[5]
        val f_4 = this.faceList[11] - this.faceList[9]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.faceList[15] - this.faceList[13]) / d_5).toFloat()
        return floats_1
    }

    fun method6539(floats_1: FloatArray): FloatArray {
        val f_2 = this.faceList[3] + this.faceList[2]
        val f_3 = this.faceList[7] + this.faceList[6]
        val f_4 = this.faceList[11] + this.faceList[10]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.faceList[15] + this.faceList[14]) / d_5).toFloat()
        return floats_1
    }

    fun method6540(floats_1: FloatArray): FloatArray {
        val f_2 = this.faceList[3] - this.faceList[2]
        val f_3 = this.faceList[7] - this.faceList[6]
        val f_4 = this.faceList[11] - this.faceList[10]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.faceList[15] - this.faceList[14]) / d_5).toFloat()
        return floats_1
    }

    fun copy3x3NearMatrixInto(matrix: FloatArray): FloatArray {
        matrix[0] = this.faceList[0]        // x_a
        matrix[1] = this.faceList[1]        // y_a
        matrix[2] = this.faceList[2]        // z_near_a
        matrix[3] = this.faceList[4]        // x_b
        matrix[4] = this.faceList[5]        // y_b
        matrix[5] = this.faceList[6]        // z_near_b
        matrix[6] = this.faceList[8]        // x_c
        matrix[7] = this.faceList[9]        // y_c
        matrix[8] = this.faceList[10]        // z_near_c
        return matrix
    }

    fun copyFullyInto(matrix: FloatArray): FloatArray {
        matrix[0] = this.faceList[0]        // x_a
        matrix[1] = this.faceList[4]        // x_b
        matrix[2] = this.faceList[8]        // x_c
        matrix[3] = this.faceList[12]        // x_d
        matrix[4] = this.faceList[1]        // y_a
        matrix[5] = this.faceList[5]        // y_b
        matrix[6] = this.faceList[9]        // y_c
        matrix[7] = this.faceList[13]        // y_d
        matrix[8] = this.faceList[2]        // z_near_a
        matrix[9] = this.faceList[6]        // z_near_b
        matrix[10] = this.faceList[10]        // z_near_c
        matrix[11] = this.faceList[14]        // z_near_d
        matrix[12] = this.faceList[3]        // z_far_a
        matrix[13] = this.faceList[7]        // z_far_b
        matrix[14] = this.faceList[11]        // z_far_c
        matrix[15] = this.faceList[15]        // z_far_d
        return matrix
    }

    // Should be given better name
    fun partialCopyInto(copyInto: FloatArray): FloatArray {
        copyInto[0] = this.faceList[0]
        copyInto[1] = this.faceList[1]
        copyInto[2] = 0.0f
        copyInto[3] = 0.0f
        copyInto[4] = this.faceList[4]
        copyInto[5] = this.faceList[5]
        copyInto[6] = 0.0f
        copyInto[7] = 0.0f
        copyInto[8] = this.faceList[12]
        copyInto[9] = this.faceList[13]
        copyInto[10] = this.faceList[14]
        copyInto[11] = 0.0f
        copyInto[12] = 0.0f
        copyInto[13] = 0.0f
        copyInto[14] = 0.0f
        copyInto[15] = 1.0f
        return copyInto
    }

    fun method6549(rotation: Int, i_2: Int, i_3: Int, f_4: Float, f_5: Float, f_6: Float) {
        var floats_9: FloatArray
        var floats_10: FloatArray
        var floats_11: FloatArray
        var floats_12: FloatArray
        var floats_13: FloatArray
        if (rotation != 0) {
            val f_7 = MatrixRotations.COS[rotation and 0x3fff]
            val f_8 = MatrixRotations.SIN[rotation and 0x3fff]
            this.faceList[0] = f_7 * i_2
            this.faceList[5] = f_7 * i_3
            this.faceList[1] = f_8 * i_2
            this.faceList[4] = -f_8 * i_3
            this.faceList[10] = 1.0f
            floats_9 = this.faceList
            floats_10 = this.faceList
            floats_11 = this.faceList
            this.faceList[9] = 0.0f
            floats_11[8] = 0.0f
            floats_10[6] = 0.0f
            floats_9[2] = 0.0f
        } else {
            this.faceList[0] = i_2.toFloat()
            this.faceList[5] = i_3.toFloat()
            this.faceList[10] = 1.0f
            floats_12 = this.faceList
            floats_13 = this.faceList
            floats_9 = this.faceList
            floats_10 = this.faceList
            floats_11 = this.faceList
            this.faceList[9] = 0.0f
            floats_11[8] = 0.0f
            floats_10[6] = 0.0f
            floats_9[4] = 0.0f
            floats_13[2] = 0.0f
            floats_12[1] = 0.0f
        }

        floats_12 = this.faceList
        floats_13 = this.faceList
        this.faceList[11] = 0.0f
        floats_13[7] = 0.0f
        floats_12[3] = 0.0f
        this.faceList[15] = 1.0f
        this.faceList[12] = f_4
        this.faceList[13] = f_5
        this.faceList[14] = f_6
    }

    override fun hashCode(): Int {
        val b_1 = 1
        val i_2 = 31 * b_1 + faceList.contentHashCode()
        return i_2
    }

    fun getComponenetsXY(matrixXY: FloatArray): FloatArray {
        matrixXY[0] = this.faceList[0]
        matrixXY[1] = this.faceList[1]
        matrixXY[2] = this.faceList[4]
        matrixXY[3] = this.faceList[5]
        matrixXY[4] = this.faceList[8]
        matrixXY[5] = this.faceList[9]
        matrixXY[6] = this.faceList[12]
        matrixXY[7] = this.faceList[13]
        return matrixXY
    }

    fun copyFrom(src: Matrix44) {
        System.arraycopy(src.faceList, 0, this.faceList, 0, 16)
    }

    fun calculatePerspective(matrixA: Matrix44, matrixB: Matrix44) {
        val vertexXA = matrixA.faceList[0] * matrixB.faceList[0] + matrixA.faceList[1] * matrixB.faceList[4] + matrixA.faceList[2] * matrixB.faceList[8] + matrixA.faceList[3] * matrixB.faceList[12]
        val vertexYA = matrixA.faceList[0] * matrixB.faceList[1] + matrixA.faceList[1] * matrixB.faceList[5] + matrixA.faceList[2] * matrixB.faceList[9] + matrixA.faceList[3] * matrixB.faceList[13]
        val vertexZNearA = matrixA.faceList[0] * matrixB.faceList[2] + matrixA.faceList[1] * matrixB.faceList[6] + matrixA.faceList[2] * matrixB.faceList[10] + matrixA.faceList[3] * matrixB.faceList[14]
        val vertexZFarA = matrixA.faceList[0] * matrixB.faceList[3] + matrixA.faceList[1] * matrixB.faceList[7] + matrixA.faceList[2] * matrixB.faceList[11] + matrixA.faceList[3] * matrixB.faceList[15]
        val vertexXB = matrixA.faceList[4] * matrixB.faceList[0] + matrixA.faceList[5] * matrixB.faceList[4] + matrixA.faceList[6] * matrixB.faceList[8] + matrixA.faceList[7] * matrixB.faceList[12]
        val vertexYB = matrixA.faceList[4] * matrixB.faceList[1] + matrixA.faceList[5] * matrixB.faceList[5] + matrixA.faceList[6] * matrixB.faceList[9] + matrixA.faceList[7] * matrixB.faceList[13]
        val vertexZNearB = matrixA.faceList[4] * matrixB.faceList[2] + matrixA.faceList[5] * matrixB.faceList[6] + matrixA.faceList[6] * matrixB.faceList[10] + matrixA.faceList[7] * matrixB.faceList[14]
        val vertexZFarB = matrixA.faceList[4] * matrixB.faceList[3] + matrixA.faceList[5] * matrixB.faceList[7] + matrixA.faceList[6] * matrixB.faceList[11] + matrixA.faceList[7] * matrixB.faceList[15]
        val vertexXC = matrixA.faceList[8] * matrixB.faceList[0] + matrixA.faceList[9] * matrixB.faceList[4] + matrixA.faceList[10] * matrixB.faceList[8] + matrixA.faceList[11] * matrixB.faceList[12]
        val vertexYC = matrixA.faceList[8] * matrixB.faceList[1] + matrixA.faceList[9] * matrixB.faceList[5] + matrixA.faceList[10] * matrixB.faceList[9] + matrixA.faceList[11] * matrixB.faceList[13]
        val vertexZNearC = matrixA.faceList[8] * matrixB.faceList[2] + matrixA.faceList[9] * matrixB.faceList[6] + matrixA.faceList[10] * matrixB.faceList[10] + matrixA.faceList[11] * matrixB.faceList[14]
        val vertexZFarC = matrixA.faceList[8] * matrixB.faceList[3] + matrixA.faceList[9] * matrixB.faceList[7] + matrixA.faceList[10] * matrixB.faceList[11] + matrixA.faceList[11] * matrixB.faceList[15]
        val vertexXD = matrixA.faceList[12] * matrixB.faceList[0] + matrixA.faceList[13] * matrixB.faceList[4] + matrixA.faceList[14] * matrixB.faceList[8] + matrixA.faceList[15] * matrixB.faceList[12]
        val vertexYD = matrixA.faceList[12] * matrixB.faceList[1] + matrixA.faceList[13] * matrixB.faceList[5] + matrixA.faceList[14] * matrixB.faceList[9] + matrixA.faceList[15] * matrixB.faceList[13]
        val vertexZNearD = matrixA.faceList[12] * matrixB.faceList[2] + matrixA.faceList[13] * matrixB.faceList[6] + matrixA.faceList[14] * matrixB.faceList[10] + matrixA.faceList[15] * matrixB.faceList[14]
        val vertexZFarD = matrixA.faceList[12] * matrixB.faceList[3] + matrixA.faceList[13] * matrixB.faceList[7] + matrixA.faceList[14] * matrixB.faceList[11] + matrixA.faceList[15] * matrixB.faceList[15]
        this.faceList[0] = vertexXA
        this.faceList[1] = vertexYA
        this.faceList[2] = vertexZNearA
        this.faceList[3] = vertexZFarA
        this.faceList[4] = vertexXB
        this.faceList[5] = vertexYB
        this.faceList[6] = vertexZNearB
        this.faceList[7] = vertexZFarB
        this.faceList[8] = vertexXC
        this.faceList[9] = vertexYC
        this.faceList[10] = vertexZNearC
        this.faceList[11] = vertexZFarC
        this.faceList[12] = vertexXD
        this.faceList[13] = vertexYD
        this.faceList[14] = vertexZNearD
        this.faceList[15] = vertexZFarD
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Matrix44) {
            return false
        }
        for (index in 0 until 16) {
            if (this.faceList[index] != other.faceList[index]) {
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
                stringBuilder.append(this.faceList[y + x * 4])
            }
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    fun getDepthPerspectiveCD(): Float {
        return (this.faceList[14] - this.faceList[15]) / (this.faceList[11] - this.faceList[10])
    }

    fun copyXYInto(matrix: FloatArray): FloatArray {
        matrix[0] = this.faceList[0] // x_a
        matrix[1] = this.faceList[4] // x_b
        matrix[2] = this.faceList[8] // x_c
        matrix[3] = this.faceList[12]// x_d
        matrix[4] = this.faceList[1] // y_a
        matrix[5] = this.faceList[5] // y_b
        matrix[6] = this.faceList[9] // y_c
        matrix[7] = this.faceList[13]// y_d
        return matrix
    }

    fun method6612(): Float {
        return this.faceList[0] * this.faceList[5] * this.faceList[10] * this.faceList[15] - this.faceList[0] * this.faceList[5] * this.faceList[11] * this.faceList[14] - this.faceList[0] * this.faceList[6] * this.faceList[9] * this.faceList[15] + this.faceList[0] * this.faceList[6] * this.faceList[11] * this.faceList[13] + this.faceList[0] * this.faceList[7] * this.faceList[9] * this.faceList[14] - this.faceList[0] * this.faceList[7] * this.faceList[10] * this.faceList[13] - this.faceList[1] * this.faceList[4] * this.faceList[10] * this.faceList[15] + this.faceList[1] * this.faceList[4] * this.faceList[11] * this.faceList[14] + this.faceList[1] * this.faceList[6] * this.faceList[8] * this.faceList[15] - this.faceList[1] * this.faceList[6] * this.faceList[11] * this.faceList[12] - this.faceList[1] * this.faceList[7] * this.faceList[8] * this.faceList[14] + this.faceList[1] * this.faceList[7] * this.faceList[10] * this.faceList[12] + this.faceList[2] * this.faceList[4] * this.faceList[9] * this.faceList[15] - this.faceList[2] * this.faceList[4] * this.faceList[11] * this.faceList[13] - this.faceList[2] * this.faceList[5] * this.faceList[8] * this.faceList[15] + this.faceList[2] * this.faceList[5] * this.faceList[11] * this.faceList[12] + this.faceList[2] * this.faceList[7] * this.faceList[8] * this.faceList[13] - this.faceList[2] * this.faceList[7] * this.faceList[9] * this.faceList[12] - this.faceList[3] * this.faceList[4] * this.faceList[9] * this.faceList[14] + this.faceList[3] * this.faceList[4] * this.faceList[10] * this.faceList[13] + this.faceList[3] * this.faceList[5] * this.faceList[8] * this.faceList[14] - this.faceList[3] * this.faceList[5] * this.faceList[10] * this.faceList[12] - this.faceList[3] * this.faceList[6] * this.faceList[8] * this.faceList[13] + this.faceList[3] * this.faceList[6] * this.faceList[9] * this.faceList[12]
    }

    fun method6613(floats_1: FloatArray): FloatArray {
        val f_2 = this.faceList[3] + this.faceList[1]
        val f_3 = this.faceList[7] + this.faceList[5]
        val f_4 = this.faceList[11] + this.faceList[9]
        val d_5 = sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4)
        floats_1[0] = (f_2 / d_5).toFloat()
        floats_1[1] = (f_3 / d_5).toFloat()
        floats_1[2] = (f_4 / d_5).toFloat()
        floats_1[3] = ((this.faceList[15] + this.faceList[13]) / d_5).toFloat()
        return floats_1
    }

    fun multiply(multiplierA: Float, multiplierB: Float, multiplierC: Float, maxtrix: FloatArray) {
        maxtrix[0] = this.faceList[0] * multiplierA + this.faceList[4] * multiplierB + this.faceList[8] * multiplierC
        maxtrix[1] = this.faceList[1] * multiplierA + this.faceList[5] * multiplierB + this.faceList[9] * multiplierC
        maxtrix[2] = this.faceList[2] * multiplierA + this.faceList[6] * multiplierB + this.faceList[10] * multiplierC
        if (maxtrix.size > 3) {
            maxtrix[3] = this.faceList[3] * multiplierA + this.faceList[7] * multiplierB + this.faceList[11] * multiplierC
        }
    }
}
