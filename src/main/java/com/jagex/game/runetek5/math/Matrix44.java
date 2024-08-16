package com.jagex.game.runetek5.math;

import java.util.Arrays;

/**
 * [J]
 * @author Brainpower
 *
 **/
public class Matrix44 {

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
    public final float[] faceList = new float[16];

    public Matrix44(Matrix44 matrix441) {
        copyFrom(matrix441);
    }

    public Matrix44() {
        identity();
    }

    public float[] copyInto(float[] into) {
        System.arraycopy(this.faceList, 0, into, 0, 16);
        return into;
    }

    public void identity() {
        this.faceList[0] = 1.0F;
        this.faceList[1] = 0.0F;
        this.faceList[2] = 0.0F;
        this.faceList[3] = 0.0F;
        this.faceList[4] = 0.0F;
        this.faceList[5] = 1.0F;
        this.faceList[6] = 0.0F;
        this.faceList[7] = 0.0F;
        this.faceList[8] = 0.0F;
        this.faceList[9] = 0.0F;
        this.faceList[10] = 1.0F;
        this.faceList[11] = 0.0F;
        this.faceList[12] = 0.0F;
        this.faceList[13] = 0.0F;
        this.faceList[14] = 0.0F;
        this.faceList[15] = 1.0F;
    }

    public void rotate() {
        float xA = this.faceList[0];
        float xB = this.faceList[4];
        float xC = this.faceList[8];
        float xD = this.faceList[12];
        float yA = this.faceList[1];
        float yB = this.faceList[5];
        float yC = this.faceList[9];
        float yD = this.faceList[13];
        float zNearA = this.faceList[2];
        float zNearB = this.faceList[6];
        float zNearC = this.faceList[10];
        float zNearD = this.faceList[14];
        float zFarA = this.faceList[3];
        float zFarB = this.faceList[7];
        float zFarC = this.faceList[11];
        float zFarD = this.faceList[15];
        this.faceList[1] = xB;
        this.faceList[2] = xC;
        this.faceList[3] = xD;
        this.faceList[4] = yA;
        this.faceList[6] = yC;
        this.faceList[7] = yD;
        this.faceList[8] = zNearA;
        this.faceList[9] = zNearB;
        this.faceList[11] = zNearD;
        this.faceList[12] = zFarA;
        this.faceList[13] = zFarB;
        this.faceList[14] = zFarC;
    }

    public void fromMatrix(Matrix43 matrix4Var) {
        this.faceList[0] = matrix4Var.xA;
        this.faceList[1] = matrix4Var.yA;
        this.faceList[2] = matrix4Var.zA;
        this.faceList[3] = 0.0F;
        this.faceList[4] = matrix4Var.xB;
        this.faceList[5] = matrix4Var.yB;
        this.faceList[6] = matrix4Var.zB;
        this.faceList[7] = 0.0F;
        this.faceList[8] = matrix4Var.xC;
        this.faceList[9] = matrix4Var.yC;
        this.faceList[10] = matrix4Var.zC;
        this.faceList[11] = 0.0F;
        this.faceList[12] = matrix4Var.xD;
        this.faceList[13] = matrix4Var.yD;
        this.faceList[14] = matrix4Var.zD;
        this.faceList[15] = 1.0F;
    }

    public void multiplyBy(Matrix44 matrix441) {
        float f_2 = this.faceList[0] * matrix441.faceList[0] + this.faceList[1] * matrix441.faceList[4] + this.faceList[2] * matrix441.faceList[8] + this.faceList[3] * matrix441.faceList[12];
        float f_3 = this.faceList[0] * matrix441.faceList[1] + this.faceList[1] * matrix441.faceList[5] + this.faceList[2] * matrix441.faceList[9] + this.faceList[3] * matrix441.faceList[13];
        float f_4 = this.faceList[0] * matrix441.faceList[2] + this.faceList[1] * matrix441.faceList[6] + this.faceList[2] * matrix441.faceList[10] + this.faceList[3] * matrix441.faceList[14];
        float f_5 = this.faceList[0] * matrix441.faceList[3] + this.faceList[1] * matrix441.faceList[7] + this.faceList[2] * matrix441.faceList[11] + this.faceList[3] * matrix441.faceList[15];
        float f_6 = this.faceList[4] * matrix441.faceList[0] + this.faceList[5] * matrix441.faceList[4] + this.faceList[6] * matrix441.faceList[8] + this.faceList[7] * matrix441.faceList[12];
        float f_7 = this.faceList[4] * matrix441.faceList[1] + this.faceList[5] * matrix441.faceList[5] + this.faceList[6] * matrix441.faceList[9] + this.faceList[7] * matrix441.faceList[13];
        float f_8 = this.faceList[4] * matrix441.faceList[2] + this.faceList[5] * matrix441.faceList[6] + this.faceList[6] * matrix441.faceList[10] + this.faceList[7] * matrix441.faceList[14];
        float f_9 = this.faceList[4] * matrix441.faceList[3] + this.faceList[5] * matrix441.faceList[7] + this.faceList[6] * matrix441.faceList[11] + this.faceList[7] * matrix441.faceList[15];
        float f_10 = this.faceList[8] * matrix441.faceList[0] + this.faceList[9] * matrix441.faceList[4] + this.faceList[10] * matrix441.faceList[8] + this.faceList[11] * matrix441.faceList[12];
        float f_11 = this.faceList[8] * matrix441.faceList[1] + this.faceList[9] * matrix441.faceList[5] + this.faceList[10] * matrix441.faceList[9] + this.faceList[11] * matrix441.faceList[13];
        float f_12 = this.faceList[8] * matrix441.faceList[2] + this.faceList[9] * matrix441.faceList[6] + this.faceList[10] * matrix441.faceList[10] + this.faceList[11] * matrix441.faceList[14];
        float f_13 = this.faceList[8] * matrix441.faceList[3] + this.faceList[9] * matrix441.faceList[7] + this.faceList[10] * matrix441.faceList[11] + this.faceList[11] * matrix441.faceList[15];
        float f_14 = this.faceList[12] * matrix441.faceList[0] + this.faceList[13] * matrix441.faceList[4] + this.faceList[14] * matrix441.faceList[8] + this.faceList[15] * matrix441.faceList[12];
        float f_15 = this.faceList[12] * matrix441.faceList[1] + this.faceList[13] * matrix441.faceList[5] + this.faceList[14] * matrix441.faceList[9] + this.faceList[15] * matrix441.faceList[13];
        float f_16 = this.faceList[12] * matrix441.faceList[2] + this.faceList[13] * matrix441.faceList[6] + this.faceList[14] * matrix441.faceList[10] + this.faceList[15] * matrix441.faceList[14];
        float f_17 = this.faceList[12] * matrix441.faceList[3] + this.faceList[13] * matrix441.faceList[7] + this.faceList[14] * matrix441.faceList[11] + this.faceList[15] * matrix441.faceList[15];
        this.faceList[0] = f_2;
        this.faceList[1] = f_3;
        this.faceList[2] = f_4;
        this.faceList[3] = f_5;
        this.faceList[4] = f_6;
        this.faceList[5] = f_7;
        this.faceList[6] = f_8;
        this.faceList[7] = f_9;
        this.faceList[8] = f_10;
        this.faceList[9] = f_11;
        this.faceList[10] = f_12;
        this.faceList[11] = f_13;
        this.faceList[12] = f_14;
        this.faceList[13] = f_15;
        this.faceList[14] = f_16;
        this.faceList[15] = f_17;
    }

    public void invert() {
        float f_1 = 1.0F / method6612();
        float f_2 = (this.faceList[5] * this.faceList[10] * this.faceList[15] - this.faceList[5] * this.faceList[11] * this.faceList[14] - this.faceList[6] * this.faceList[9] * this.faceList[15] + this.faceList[6] * this.faceList[11] * this.faceList[13] + this.faceList[7] * this.faceList[9] * this.faceList[14] - this.faceList[7] * this.faceList[10] * this.faceList[13]) * f_1;
        float f_3 = (-this.faceList[1] * this.faceList[10] * this.faceList[15] + this.faceList[1] * this.faceList[11] * this.faceList[14] + this.faceList[2] * this.faceList[9] * this.faceList[15] - this.faceList[2] * this.faceList[11] * this.faceList[13] - this.faceList[3] * this.faceList[9] * this.faceList[14] + this.faceList[3] * this.faceList[10] * this.faceList[13]) * f_1;
        float f_4 = (this.faceList[1] * this.faceList[6] * this.faceList[15] - this.faceList[1] * this.faceList[7] * this.faceList[14] - this.faceList[2] * this.faceList[5] * this.faceList[15] + this.faceList[2] * this.faceList[7] * this.faceList[13] + this.faceList[3] * this.faceList[5] * this.faceList[14] - this.faceList[3] * this.faceList[6] * this.faceList[13]) * f_1;
        float f_5 = (-this.faceList[1] * this.faceList[6] * this.faceList[11] + this.faceList[1] * this.faceList[7] * this.faceList[10] + this.faceList[2] * this.faceList[5] * this.faceList[11] - this.faceList[2] * this.faceList[7] * this.faceList[9] - this.faceList[3] * this.faceList[5] * this.faceList[10] + this.faceList[3] * this.faceList[6] * this.faceList[9]) * f_1;
        float f_6 = (-this.faceList[4] * this.faceList[10] * this.faceList[15] + this.faceList[4] * this.faceList[11] * this.faceList[14] + this.faceList[6] * this.faceList[8] * this.faceList[15] - this.faceList[6] * this.faceList[11] * this.faceList[12] - this.faceList[7] * this.faceList[8] * this.faceList[14] + this.faceList[7] * this.faceList[10] * this.faceList[12]) * f_1;
        float f_7 = (this.faceList[0] * this.faceList[10] * this.faceList[15] - this.faceList[0] * this.faceList[11] * this.faceList[14] - this.faceList[2] * this.faceList[8] * this.faceList[15] + this.faceList[2] * this.faceList[11] * this.faceList[12] + this.faceList[3] * this.faceList[8] * this.faceList[14] - this.faceList[3] * this.faceList[10] * this.faceList[12]) * f_1;
        float f_8 = (-this.faceList[0] * this.faceList[6] * this.faceList[15] + this.faceList[0] * this.faceList[7] * this.faceList[14] + this.faceList[2] * this.faceList[4] * this.faceList[15] - this.faceList[2] * this.faceList[7] * this.faceList[12] - this.faceList[3] * this.faceList[4] * this.faceList[14] + this.faceList[3] * this.faceList[6] * this.faceList[12]) * f_1;
        float f_9 = (this.faceList[0] * this.faceList[6] * this.faceList[11] - this.faceList[0] * this.faceList[7] * this.faceList[10] - this.faceList[2] * this.faceList[4] * this.faceList[11] + this.faceList[2] * this.faceList[7] * this.faceList[8] + this.faceList[3] * this.faceList[4] * this.faceList[10] - this.faceList[3] * this.faceList[6] * this.faceList[8]) * f_1;
        float f_10 = (this.faceList[4] * this.faceList[9] * this.faceList[15] - this.faceList[4] * this.faceList[11] * this.faceList[13] - this.faceList[5] * this.faceList[8] * this.faceList[15] + this.faceList[5] * this.faceList[11] * this.faceList[12] + this.faceList[7] * this.faceList[8] * this.faceList[13] - this.faceList[7] * this.faceList[9] * this.faceList[12]) * f_1;
        float f_11 = (-this.faceList[0] * this.faceList[9] * this.faceList[15] + this.faceList[0] * this.faceList[11] * this.faceList[13] + this.faceList[1] * this.faceList[8] * this.faceList[15] - this.faceList[1] * this.faceList[11] * this.faceList[12] - this.faceList[3] * this.faceList[8] * this.faceList[13] + this.faceList[3] * this.faceList[9] * this.faceList[12]) * f_1;
        float f_12 = (this.faceList[0] * this.faceList[5] * this.faceList[15] - this.faceList[0] * this.faceList[7] * this.faceList[13] - this.faceList[1] * this.faceList[4] * this.faceList[15] + this.faceList[1] * this.faceList[7] * this.faceList[12] + this.faceList[3] * this.faceList[4] * this.faceList[13] - this.faceList[3] * this.faceList[5] * this.faceList[12]) * f_1;
        float f_13 = (-this.faceList[0] * this.faceList[5] * this.faceList[11] + this.faceList[0] * this.faceList[7] * this.faceList[9] + this.faceList[1] * this.faceList[4] * this.faceList[11] - this.faceList[1] * this.faceList[7] * this.faceList[8] - this.faceList[3] * this.faceList[4] * this.faceList[9] + this.faceList[3] * this.faceList[5] * this.faceList[8]) * f_1;
        float f_14 = (-this.faceList[4] * this.faceList[9] * this.faceList[14] + this.faceList[4] * this.faceList[10] * this.faceList[13] + this.faceList[5] * this.faceList[8] * this.faceList[14] - this.faceList[5] * this.faceList[10] * this.faceList[12] - this.faceList[6] * this.faceList[8] * this.faceList[13] + this.faceList[6] * this.faceList[9] * this.faceList[12]) * f_1;
        float f_15 = (this.faceList[0] * this.faceList[9] * this.faceList[14] - this.faceList[0] * this.faceList[10] * this.faceList[13] - this.faceList[1] * this.faceList[8] * this.faceList[14] + this.faceList[1] * this.faceList[10] * this.faceList[12] + this.faceList[2] * this.faceList[8] * this.faceList[13] - this.faceList[2] * this.faceList[9] * this.faceList[12]) * f_1;
        float f_16 = (-this.faceList[0] * this.faceList[5] * this.faceList[14] + this.faceList[0] * this.faceList[6] * this.faceList[13] + this.faceList[1] * this.faceList[4] * this.faceList[14] - this.faceList[1] * this.faceList[6] * this.faceList[12] - this.faceList[2] * this.faceList[4] * this.faceList[13] + this.faceList[2] * this.faceList[5] * this.faceList[12]) * f_1;
        float f_17 = (this.faceList[0] * this.faceList[5] * this.faceList[10] - this.faceList[0] * this.faceList[6] * this.faceList[9] - this.faceList[1] * this.faceList[4] * this.faceList[10] + this.faceList[1] * this.faceList[6] * this.faceList[8] + this.faceList[2] * this.faceList[4] * this.faceList[9] - this.faceList[2] * this.faceList[5] * this.faceList[8]) * f_1;
        this.faceList[0] = f_2;
        this.faceList[1] = f_3;
        this.faceList[2] = f_4;
        this.faceList[3] = f_5;
        this.faceList[4] = f_6;
        this.faceList[5] = f_7;
        this.faceList[6] = f_8;
        this.faceList[7] = f_9;
        this.faceList[8] = f_10;
        this.faceList[9] = f_11;
        this.faceList[10] = f_12;
        this.faceList[11] = f_13;
        this.faceList[12] = f_14;
        this.faceList[13] = f_15;
        this.faceList[14] = f_16;
        this.faceList[15] = f_17;
    }

    public void method6525(float x, float y, float zNear, float zFar) {
        this.faceList[0] = x;
        this.faceList[1] = 0.0F;
        this.faceList[2] = 0.0F;
        this.faceList[3] = 0.0F;
        this.faceList[4] = 0.0F;
        this.faceList[5] = y;
        this.faceList[6] = 0.0F;
        this.faceList[7] = 0.0F;
        this.faceList[8] = 0.0F;
        this.faceList[9] = 0.0F;
        this.faceList[10] = zNear;
        this.faceList[11] = 0.0F;
        this.faceList[12] = 0.0F;
        this.faceList[13] = 0.0F;
        this.faceList[14] = 0.0F;
        this.faceList[15] = zFar;
    }

    public boolean method6526() {
        return this.faceList[0] == 1.0F && this.faceList[1] == 0.0F && this.faceList[2] == 0.0F && this.faceList[3] == 0.0F && this.faceList[4] == 0.0F && this.faceList[5] == 1.0F && this.faceList[6] == 0.0F && this.faceList[7] == 0.0F && this.faceList[8] == 0.0F && this.faceList[9] == 0.0F && this.faceList[10] == 1.0F && this.faceList[11] == 0.0F && this.faceList[12] == 0.0F && this.faceList[13] == 0.0F && this.faceList[14] == 0.0F && this.faceList[15] == 1.0F;
    }

    public void scale(float scaleFaceA, float scaleFaceB, float scaleFaceC, float[] matrix) {
        matrix[0] = this.faceList[0] * scaleFaceA + this.faceList[4] * scaleFaceB + this.faceList[8] * scaleFaceC + this.faceList[12];
        matrix[1] = this.faceList[1] * scaleFaceA + this.faceList[5] * scaleFaceB + this.faceList[9] * scaleFaceC + this.faceList[13];
        matrix[2] = this.faceList[2] * scaleFaceA + this.faceList[6] * scaleFaceB + this.faceList[10] * scaleFaceC + this.faceList[14];
        if (matrix.length > 3) {
            matrix[3] = this.faceList[3] * scaleFaceA + this.faceList[7] * scaleFaceB + this.faceList[11] * scaleFaceC + this.faceList[15];
        }

    }

    public void method6528(float[] floats_1) {
        float f_2 = floats_1[0];
        float f_3 = floats_1[1];
        float f_4 = floats_1[2];
        floats_1[0] = this.faceList[0] * f_2 + this.faceList[4] * f_3 + this.faceList[8] * f_4 + this.faceList[12];
        floats_1[1] = this.faceList[1] * f_2 + this.faceList[5] * f_3 + this.faceList[9] * f_4 + this.faceList[13];
        floats_1[2] = this.faceList[2] * f_2 + this.faceList[6] * f_3 + this.faceList[10] * f_4 + this.faceList[14];
    }

    public void method6530(float x, float width, float y, float height, float viewRangeStart, float viewRangeEnd) {
        this.faceList[0] = 2.0F / (width - x);
        this.faceList[1] = 0.0F;
        this.faceList[2] = 0.0F;
        this.faceList[3] = 0.0F;
        this.faceList[4] = 0.0F;
        this.faceList[5] = 2.0F / (height - y);
        this.faceList[6] = 0.0F;
        this.faceList[7] = 0.0F;
        this.faceList[8] = 0.0F;
        this.faceList[9] = 0.0F;
        this.faceList[10] = 2.0F / (viewRangeEnd - viewRangeStart);
        this.faceList[11] = 0.0F;
        this.faceList[12] = -(width + x) / (width - x);
        this.faceList[13] = -(height + y) / (height - y);
        this.faceList[14] = -(viewRangeEnd + viewRangeStart) / (viewRangeEnd - viewRangeStart);
        this.faceList[15] = 1.0F;
    }

    public void method6531(float centerX, float centerY, float f_3, float f_4, float viewRangeStart, float viewRangeEnd, float width, float height) {
        this.faceList[0] = f_3 * 2.0F / width;
        this.faceList[1] = 0.0F;
        this.faceList[2] = 0.0F;
        this.faceList[3] = 0.0F;
        this.faceList[4] = 0.0F;
        this.faceList[5] = f_4 * 2.0F / height;
        this.faceList[6] = 0.0F;
        this.faceList[7] = 0.0F;
        this.faceList[8] = 2.0F * centerX / width - 1.0F;
        this.faceList[9] = 2.0F * centerY / height - 1.0F;
        this.faceList[10] = (viewRangeEnd + viewRangeStart) / (viewRangeEnd - viewRangeStart);
        this.faceList[11] = 1.0F;
        this.faceList[12] = 0.0F;
        this.faceList[13] = 0.0F;
        this.faceList[14] = 2.0F * viewRangeEnd * viewRangeStart / (viewRangeStart - viewRangeEnd);
        this.faceList[15] = 0.0F;
    }

    public void method6532(float f_1, float f_2, float f_3, float f_4, float viewRangeStart, float viewRangeEnd, float f_7, float f_8, float f_9) {
        method6530(-(f_1 * f_9) / f_3, f_9 * (f_7 - f_1) / f_3, -(f_2 * f_9) / f_4, f_9 * (f_8 - f_2) / f_4, viewRangeStart, viewRangeEnd);
    }

    public float method6533() {
        return -(this.faceList[15] + this.faceList[14]) / (this.faceList[11] + this.faceList[10]);
    }

    public float[] method6534(float[] floats_1) {
        System.arraycopy(this.faceList, 0, floats_1, 0, 16);
        floats_1[3] = 0.0F;
        floats_1[7] = 0.0F;
        floats_1[11] = 0.0F;
        floats_1[12] = 0.0F;
        floats_1[13] = 0.0F;
        floats_1[14] = 0.0F;
        floats_1[15] = 1.0F;
        return floats_1;
    }

    public float[] method6535(float[] floats_1) {
        float f_2 = this.faceList[3] + this.faceList[0];
        float f_3 = this.faceList[7] + this.faceList[4];
        float f_4 = this.faceList[11] + this.faceList[8];
        double d_5 = Math.sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4);
        floats_1[0] = (float) (f_2 / d_5);
        floats_1[1] = (float) (f_3 / d_5);
        floats_1[2] = (float) (f_4 / d_5);
        floats_1[3] = (float) ((this.faceList[15] + this.faceList[12]) / d_5);
        return floats_1;
    }

    public float[] method6536(float[] floats_1) {
        float f_2 = this.faceList[3] - this.faceList[0];
        float f_3 = this.faceList[7] - this.faceList[4];
        float f_4 = this.faceList[11] - this.faceList[8];
        double d_5 = Math.sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4);
        floats_1[0] = (float) (f_2 / d_5);
        floats_1[1] = (float) (f_3 / d_5);
        floats_1[2] = (float) (f_4 / d_5);
        floats_1[3] = (float) ((this.faceList[15] - this.faceList[12]) / d_5);
        return floats_1;
    }

    public float[] method6538(float[] floats_1) {
        float f_2 = this.faceList[3] - this.faceList[1];
        float f_3 = this.faceList[7] - this.faceList[5];
        float f_4 = this.faceList[11] - this.faceList[9];
        double d_5 = Math.sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4);
        floats_1[0] = (float) (f_2 / d_5);
        floats_1[1] = (float) (f_3 / d_5);
        floats_1[2] = (float) (f_4 / d_5);
        floats_1[3] = (float) ((this.faceList[15] - this.faceList[13]) / d_5);
        return floats_1;
    }

    public float[] method6539(float[] floats_1) {
        float f_2 = this.faceList[3] + this.faceList[2];
        float f_3 = this.faceList[7] + this.faceList[6];
        float f_4 = this.faceList[11] + this.faceList[10];
        double d_5 = Math.sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4);
        floats_1[0] = (float) (f_2 / d_5);
        floats_1[1] = (float) (f_3 / d_5);
        floats_1[2] = (float) (f_4 / d_5);
        floats_1[3] = (float) ((this.faceList[15] + this.faceList[14]) / d_5);
        return floats_1;
    }

    public float[] method6540(float[] floats_1) {
        float f_2 = this.faceList[3] - this.faceList[2];
        float f_3 = this.faceList[7] - this.faceList[6];
        float f_4 = this.faceList[11] - this.faceList[10];
        double d_5 = Math.sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4);
        floats_1[0] = (float) (f_2 / d_5);
        floats_1[1] = (float) (f_3 / d_5);
        floats_1[2] = (float) (f_4 / d_5);
        floats_1[3] = (float) ((this.faceList[15] - this.faceList[14]) / d_5);
        return floats_1;
    }

    public float[] copy3x3NearMatrixInto(float[] matrix) {
        matrix[0] = this.faceList[0];		// x_a
        matrix[1] = this.faceList[1];		// y_a
        matrix[2] = this.faceList[2];		// z_near_a
        matrix[3] = this.faceList[4];		// x_b
        matrix[4] = this.faceList[5];		// y_b
        matrix[5] = this.faceList[6];		// z_near_b
        matrix[6] = this.faceList[8];		// x_c
        matrix[7] = this.faceList[9];		// y_c
        matrix[8] = this.faceList[10];		// z_near_c
        return matrix;
    }

    public float[] copyFullyInto(float[] matrix) {
        matrix[0] = this.faceList[0];		// x_a
        matrix[1] = this.faceList[4];		// x_b
        matrix[2] = this.faceList[8];		// x_c
        matrix[3] = this.faceList[12];		// x_d
        matrix[4] = this.faceList[1];		// y_a
        matrix[5] = this.faceList[5];		// y_b
        matrix[6] = this.faceList[9];		// y_c
        matrix[7] = this.faceList[13];		// y_d
        matrix[8] = this.faceList[2];		// z_near_a
        matrix[9] = this.faceList[6];		// z_near_b
        matrix[10] = this.faceList[10];		// z_near_c
        matrix[11] = this.faceList[14];		// z_near_d
        matrix[12] = this.faceList[3];		// z_far_a
        matrix[13] = this.faceList[7];		// z_far_b
        matrix[14] = this.faceList[11];		// z_far_c
        matrix[15] = this.faceList[15];		// z_far_d
        return matrix;
    }

    // Should be given better name
    public float[] partialCopyInto(float[] copyInto) {
        copyInto[0] = this.faceList[0];
        copyInto[1] = this.faceList[1];
        copyInto[2] = 0.0F;
        copyInto[3] = 0.0F;
        copyInto[4] = this.faceList[4];
        copyInto[5] = this.faceList[5];
        copyInto[6] = 0.0F;
        copyInto[7] = 0.0F;
        copyInto[8] = this.faceList[12];
        copyInto[9] = this.faceList[13];
        copyInto[10] = this.faceList[14];
        copyInto[11] = 0.0F;
        copyInto[12] = 0.0F;
        copyInto[13] = 0.0F;
        copyInto[14] = 0.0F;
        copyInto[15] = 1.0F;
        return copyInto;
    }

    public void method6549(int rotation, int i_2, int i_3, float f_4, float f_5, float f_6) {
        float[] floats_9;
        float[] floats_10;
        float[] floats_11;
        float[] floats_12;
        float[] floats_13;
        if (rotation != 0) {
            float f_7 = MatrixRotations.COS[rotation & 0x3fff];
            float f_8 = MatrixRotations.SIN[rotation & 0x3fff];
            this.faceList[0] = f_7 * i_2;
            this.faceList[5] = f_7 * i_3;
            this.faceList[1] = f_8 * i_2;
            this.faceList[4] = -f_8 * i_3;
            this.faceList[10] = 1.0F;
            floats_9 = this.faceList;
            floats_10 = this.faceList;
            floats_11 = this.faceList;
            this.faceList[9] = 0.0F;
            floats_11[8] = 0.0F;
            floats_10[6] = 0.0F;
            floats_9[2] = 0.0F;
        } else {
            this.faceList[0] = i_2;
            this.faceList[5] = i_3;
            this.faceList[10] = 1.0F;
            floats_12 = this.faceList;
            floats_13 = this.faceList;
            floats_9 = this.faceList;
            floats_10 = this.faceList;
            floats_11 = this.faceList;
            this.faceList[9] = 0.0F;
            floats_11[8] = 0.0F;
            floats_10[6] = 0.0F;
            floats_9[4] = 0.0F;
            floats_13[2] = 0.0F;
            floats_12[1] = 0.0F;
        }

        floats_12 = this.faceList;
        floats_13 = this.faceList;
        this.faceList[11] = 0.0F;
        floats_13[7] = 0.0F;
        floats_12[3] = 0.0F;
        this.faceList[15] = 1.0F;
        this.faceList[12] = f_4;
        this.faceList[13] = f_5;
        this.faceList[14] = f_6;
    }

    public int hashCode() {
        byte b_1 = 1;
        int i_2 = 31 * b_1 + Arrays.hashCode(this.faceList);
        return i_2;
    }

    public float[] getComponenetsXY(float[] matrixXY) {
        matrixXY[0] = this.faceList[0];
        matrixXY[1] = this.faceList[1];
        matrixXY[2] = this.faceList[4];
        matrixXY[3] = this.faceList[5];
        matrixXY[4] = this.faceList[8];
        matrixXY[5] = this.faceList[9];
        matrixXY[6] = this.faceList[12];
        matrixXY[7] = this.faceList[13];
        return matrixXY;
    }

    public void copyFrom(Matrix44 src) {
        System.arraycopy(src.faceList, 0, this.faceList, 0, 16);
    }

    public void calculatePerspective(Matrix44 matrixA, Matrix44 matrixB) {
        float vertexXA = matrixA.faceList[0] * matrixB.faceList[0] + matrixA.faceList[1] * matrixB.faceList[4] + matrixA.faceList[2] * matrixB.faceList[8] + matrixA.faceList[3] * matrixB.faceList[12];
        float vertexYA = matrixA.faceList[0] * matrixB.faceList[1] + matrixA.faceList[1] * matrixB.faceList[5] + matrixA.faceList[2] * matrixB.faceList[9] + matrixA.faceList[3] * matrixB.faceList[13];
        float vertexZNearA = matrixA.faceList[0] * matrixB.faceList[2] + matrixA.faceList[1] * matrixB.faceList[6] + matrixA.faceList[2] * matrixB.faceList[10] + matrixA.faceList[3] * matrixB.faceList[14];
        float vertexZFarA = matrixA.faceList[0] * matrixB.faceList[3] + matrixA.faceList[1] * matrixB.faceList[7] + matrixA.faceList[2] * matrixB.faceList[11] + matrixA.faceList[3] * matrixB.faceList[15];
        float vertexXB = matrixA.faceList[4] * matrixB.faceList[0] + matrixA.faceList[5] * matrixB.faceList[4] + matrixA.faceList[6] * matrixB.faceList[8] + matrixA.faceList[7] * matrixB.faceList[12];
        float vertexYB = matrixA.faceList[4] * matrixB.faceList[1] + matrixA.faceList[5] * matrixB.faceList[5] + matrixA.faceList[6] * matrixB.faceList[9] + matrixA.faceList[7] * matrixB.faceList[13];
        float vertexZNearB = matrixA.faceList[4] * matrixB.faceList[2] + matrixA.faceList[5] * matrixB.faceList[6] + matrixA.faceList[6] * matrixB.faceList[10] + matrixA.faceList[7] * matrixB.faceList[14];
        float vertexZFarB = matrixA.faceList[4] * matrixB.faceList[3] + matrixA.faceList[5] * matrixB.faceList[7] + matrixA.faceList[6] * matrixB.faceList[11] + matrixA.faceList[7] * matrixB.faceList[15];
        float vertexXC = matrixA.faceList[8] * matrixB.faceList[0] + matrixA.faceList[9] * matrixB.faceList[4] + matrixA.faceList[10] * matrixB.faceList[8] + matrixA.faceList[11] * matrixB.faceList[12];
        float vertexYC = matrixA.faceList[8] * matrixB.faceList[1] + matrixA.faceList[9] * matrixB.faceList[5] + matrixA.faceList[10] * matrixB.faceList[9] + matrixA.faceList[11] * matrixB.faceList[13];
        float vertexZNearC = matrixA.faceList[8] * matrixB.faceList[2] + matrixA.faceList[9] * matrixB.faceList[6] + matrixA.faceList[10] * matrixB.faceList[10] + matrixA.faceList[11] * matrixB.faceList[14];
        float vertexZFarC = matrixA.faceList[8] * matrixB.faceList[3] + matrixA.faceList[9] * matrixB.faceList[7] + matrixA.faceList[10] * matrixB.faceList[11] + matrixA.faceList[11] * matrixB.faceList[15];
        float vertexXD = matrixA.faceList[12] * matrixB.faceList[0] + matrixA.faceList[13] * matrixB.faceList[4] + matrixA.faceList[14] * matrixB.faceList[8] + matrixA.faceList[15] * matrixB.faceList[12];
        float vertexYD = matrixA.faceList[12] * matrixB.faceList[1] + matrixA.faceList[13] * matrixB.faceList[5] + matrixA.faceList[14] * matrixB.faceList[9] + matrixA.faceList[15] * matrixB.faceList[13];
        float vertexZNearD = matrixA.faceList[12] * matrixB.faceList[2] + matrixA.faceList[13] * matrixB.faceList[6] + matrixA.faceList[14] * matrixB.faceList[10] + matrixA.faceList[15] * matrixB.faceList[14];
        float vertexZFarD = matrixA.faceList[12] * matrixB.faceList[3] + matrixA.faceList[13] * matrixB.faceList[7] + matrixA.faceList[14] * matrixB.faceList[11] + matrixA.faceList[15] * matrixB.faceList[15];
        this.faceList[0] = vertexXA;
        this.faceList[1] = vertexYA;
        this.faceList[2] = vertexZNearA;
        this.faceList[3] = vertexZFarA;
        this.faceList[4] = vertexXB;
        this.faceList[5] = vertexYB;
        this.faceList[6] = vertexZNearB;
        this.faceList[7] = vertexZFarB;
        this.faceList[8] = vertexXC;
        this.faceList[9] = vertexYC;
        this.faceList[10] = vertexZNearC;
        this.faceList[11] = vertexZFarC;
        this.faceList[12] = vertexXD;
        this.faceList[13] = vertexYD;
        this.faceList[14] = vertexZNearD;
        this.faceList[15] = vertexZFarD;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Matrix44 matrix)) {
            return false;
        }
        for (int index = 0; index < 16; index++) {
            if (this.faceList[index] != matrix.faceList[index]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                if (y > 0) {
                    stringBuilder.append("\t");
                }
                stringBuilder.append(this.faceList[y + x * 4]);
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public float getDepthPerspectiveCD() {
        return (this.faceList[14] - this.faceList[15]) / (this.faceList[11] - this.faceList[10]);
    }

    public float[] copyXYInto(float[] matrix) {
        matrix[0] = this.faceList[0]; // x_a
        matrix[1] = this.faceList[4]; // x_b
        matrix[2] = this.faceList[8]; // x_c
        matrix[3] = this.faceList[12];// x_d
        matrix[4] = this.faceList[1]; // y_a
        matrix[5] = this.faceList[5]; // y_b
        matrix[6] = this.faceList[9]; // y_c
        matrix[7] = this.faceList[13];// y_d
        return matrix;
    }

    public float method6612() {
        return this.faceList[0] * this.faceList[5] * this.faceList[10] * this.faceList[15] - this.faceList[0] * this.faceList[5] * this.faceList[11] * this.faceList[14] - this.faceList[0] * this.faceList[6] * this.faceList[9] * this.faceList[15] + this.faceList[0] * this.faceList[6] * this.faceList[11] * this.faceList[13] + this.faceList[0] * this.faceList[7] * this.faceList[9] * this.faceList[14] - this.faceList[0] * this.faceList[7] * this.faceList[10] * this.faceList[13] - this.faceList[1] * this.faceList[4] * this.faceList[10] * this.faceList[15] + this.faceList[1] * this.faceList[4] * this.faceList[11] * this.faceList[14] + this.faceList[1] * this.faceList[6] * this.faceList[8] * this.faceList[15] - this.faceList[1] * this.faceList[6] * this.faceList[11] * this.faceList[12] - this.faceList[1] * this.faceList[7] * this.faceList[8] * this.faceList[14] + this.faceList[1] * this.faceList[7] * this.faceList[10] * this.faceList[12] + this.faceList[2] * this.faceList[4] * this.faceList[9] * this.faceList[15] - this.faceList[2] * this.faceList[4] * this.faceList[11] * this.faceList[13] - this.faceList[2] * this.faceList[5] * this.faceList[8] * this.faceList[15] + this.faceList[2] * this.faceList[5] * this.faceList[11] * this.faceList[12] + this.faceList[2] * this.faceList[7] * this.faceList[8] * this.faceList[13] - this.faceList[2] * this.faceList[7] * this.faceList[9] * this.faceList[12] - this.faceList[3] * this.faceList[4] * this.faceList[9] * this.faceList[14] + this.faceList[3] * this.faceList[4] * this.faceList[10] * this.faceList[13] + this.faceList[3] * this.faceList[5] * this.faceList[8] * this.faceList[14] - this.faceList[3] * this.faceList[5] * this.faceList[10] * this.faceList[12] - this.faceList[3] * this.faceList[6] * this.faceList[8] * this.faceList[13] + this.faceList[3] * this.faceList[6] * this.faceList[9] * this.faceList[12];
    }

    public float[] method6613(float[] floats_1) {
        float f_2 = this.faceList[3] + this.faceList[1];
        float f_3 = this.faceList[7] + this.faceList[5];
        float f_4 = this.faceList[11] + this.faceList[9];
        double d_5 = Math.sqrt(f_2 * f_2 + f_3 * f_3 + f_4 * f_4);
        floats_1[0] = (float) (f_2 / d_5);
        floats_1[1] = (float) (f_3 / d_5);
        floats_1[2] = (float) (f_4 / d_5);
        floats_1[3] = (float) ((this.faceList[15] + this.faceList[13]) / d_5);
        return floats_1;
    }

    public void multiply(float multiplierA, float multiplierB, float multiplierC, float[] maxtrix) {
        maxtrix[0] = this.faceList[0] * multiplierA + this.faceList[4] * multiplierB + this.faceList[8] * multiplierC;
        maxtrix[1] = this.faceList[1] * multiplierA + this.faceList[5] * multiplierB + this.faceList[9] * multiplierC;
        maxtrix[2] = this.faceList[2] * multiplierA + this.faceList[6] * multiplierB + this.faceList[10] * multiplierC;
        if (maxtrix.length > 3) {
            maxtrix[3] = this.faceList[3] * multiplierA + this.faceList[7] * multiplierB + this.faceList[11] * multiplierC;
        }

    }

}
