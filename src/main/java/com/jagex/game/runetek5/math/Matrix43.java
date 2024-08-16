package com.jagex.game.runetek5.math;

import com.jagex.game.runetek5.movement.CoordinateSpace;

/**
 * [J]
 * @author Steven
 *
 */
public class Matrix43 {

    public static final Matrix43 aClass2943518 = new Matrix43();
    float xB;
    float yA;
    float xC;
    float zA;
    float yC;
    float zB;
    float xD;
    float yD;
    float zD;
    float xA;
    float yB;
    float zC;

    public Matrix43(Matrix43 matrix44var1) {
        copyFrom(matrix44var1);
    }

    public Matrix43() {
        reset();
    }

    public void updateIdentitiyMatrix() {
        float originalX = this.xD;
        float originalY = this.yD;
        float f_3 = this.xB;
        this.xB = this.yA;
        this.yA = f_3;
        float f_4 = this.xC;
        this.xC = this.zA;
        this.zA = f_4;
        float f_5 = this.yC;
        this.yC = this.zB;
        this.zB = f_5;
        this.xD = -(originalX * this.xA + originalY * this.xB + this.zD * this.xC);
        this.yD = -(originalX * this.yA + originalY * this.yB + this.zD * this.yC);
        this.zD = -(originalX * this.zA + originalY * this.zB + this.zD * this.zC);
    }

    public void multiply(Matrix43 otherMatrix) {
        float oldXA = this.xA;
        float oldYA = this.yA;
        float oldXB = this.xB;
        float oldYB = this.yB;
        float oldXC = this.xC;
        float oldYC = this.yC;
        float oldXD = this.xD;
        float oldYD = this.yD;
        float oldZA = this.zA;
        float oldZB = this.zB;
        float oldZC = this.zC;
        float oldZD = this.zD;
        this.xA = oldXA * otherMatrix.xA + oldYA * otherMatrix.xB + oldZA * otherMatrix.xC;
        this.yA = oldXA * otherMatrix.yA + oldYA * otherMatrix.yB + oldZA * otherMatrix.yC;
        this.zA = oldXA * otherMatrix.zA + oldYA * otherMatrix.zB + oldZA * otherMatrix.zC;
        this.xB = oldXB * otherMatrix.xA + oldYB * otherMatrix.xB + oldZB * otherMatrix.xC;
        this.yB = oldXB * otherMatrix.yA + oldYB * otherMatrix.yB + oldZB * otherMatrix.yC;
        this.zB = oldXB * otherMatrix.zA + oldYB * otherMatrix.zB + oldZB * otherMatrix.zC;
        this.xC = oldXC * otherMatrix.xA + oldYC * otherMatrix.xB + oldZC * otherMatrix.xC;
        this.yC = oldXC * otherMatrix.yA + oldYC * otherMatrix.yB + oldZC * otherMatrix.yC;
        this.zC = oldXC * otherMatrix.zA + oldYC * otherMatrix.zB + oldZC * otherMatrix.zC;
        this.xD = oldXD * otherMatrix.xA + oldYD * otherMatrix.xB + oldZD * otherMatrix.xC + otherMatrix.xD;
        this.yD = oldXD * otherMatrix.yA + oldYD * otherMatrix.yB + oldZD * otherMatrix.yC + otherMatrix.yD;
        this.zD = oldXD * otherMatrix.zA + oldYD * otherMatrix.zB + oldZD * otherMatrix.zC + otherMatrix.zD;
    }

    public void copyFrom(Matrix43 matrix44var1) {
        this.xA = matrix44var1.xA;
        this.xB = matrix44var1.xB;
        this.xC = matrix44var1.xC;
        this.xD = matrix44var1.xD;
        this.yA = matrix44var1.yA;
        this.yB = matrix44var1.yB;
        this.yC = matrix44var1.yC;
        this.yD = matrix44var1.yD;
        this.zA = matrix44var1.zA;
        this.zB = matrix44var1.zB;
        this.zC = matrix44var1.zC;
        this.zD = matrix44var1.zD;
    }

    public void translateFromCoordspace(CoordinateSpace coordinateSpace) {
        setFromQuaternion(coordinateSpace.quaternion);
        translate(coordinateSpace.coords);
    }

    public void reset() {
        this.zD = 0.0F;
        this.yD = 0.0F;
        this.xD = 0.0F;
        this.yC = 0.0F;
        this.xC = 0.0F;
        this.zB = 0.0F;
        this.xB = 0.0F;
        this.zA = 0.0F;
        this.yA = 0.0F;
        this.zC = 1.0F;
        this.yB = 1.0F;
        this.xA = 1.0F;
    }

    public void method5213(int rotation, int i_2, int i_3, float translateX, float translateY, float translateZ) {
        if (rotation != 0) {
            float f_7 = MatrixRotations.COS[rotation & 0x3fff];
            float f_8 = MatrixRotations.SIN[rotation & 0x3fff];
            this.zC = 1.0F;
            this.yC = 0.0F;
            this.xC = 0.0F;
            this.zB = 0.0F;
            this.zA = 0.0F;
            this.xA = 2.0F * f_7 * i_2;
            this.yB = 2.0F * f_7 * i_3;
            this.yA = 2.0F * f_8 * i_2;
            this.xB = -2.0f * f_8 * i_3;
            this.xD = (i_2 * 2) * (0.5F * f_8 - 0.5F * f_7) + translateX;
            this.yD = (i_3 * 2) * (-0.5f * f_8 - 0.5F * f_7) + translateY;
        } else {
            this.yC = 0.0F;
            this.xC = 0.0F;
            this.zB = 0.0F;
            this.xB = 0.0F;
            this.zA = 0.0F;
            this.yA = 0.0F;
            this.xA = (i_2 * 2);
            this.yB = (i_3 * 2);
            this.zC = 1.0F;
            this.xD = translateX - i_2;
            this.yD = translateY - i_3;
        }
        this.zD = translateZ;

    }

    public void method5214() {
        this.xA = (float) 1.0;
        this.xB = (float) 0.0;
        this.xC = (float) 0.0;
        this.xD = 0.0F;
        this.yA = (float) 0.0;
        this.yB = (float) 0.0;
        this.yC = (float) 1.0;
        this.yD = 0.0F;
        this.zA = (float) 0.0;
        this.zB = (float) 1.0;
        this.zC = (float) 0.0;
        this.zD = 0.0F;
    }

    public void updateMatrix(Matrix43 matrix4x4var) {
        if (this == matrix4x4var) {
            updateIdentitiyMatrix();
        } else {
            this.xA = matrix4x4var.xA;
            this.xB = matrix4x4var.yA;
            this.xC = matrix4x4var.zA;
            this.yA = matrix4x4var.xB;
            this.yB = matrix4x4var.yB;
            this.yC = matrix4x4var.zB;
            this.zA = matrix4x4var.xC;
            this.zB = matrix4x4var.yC;
            this.zC = matrix4x4var.zC;
            this.xD = -(matrix4x4var.xD * this.xA + matrix4x4var.yD * this.xB + matrix4x4var.zD * this.xC);
            this.yD = -(matrix4x4var.xD * this.yA + matrix4x4var.yD * this.yB + matrix4x4var.zD * this.yC);
            this.zD = -(matrix4x4var.xD * this.zA + matrix4x4var.yD * this.zB + matrix4x4var.zD * this.zC);
        }

    }

    // rotate around axis
    public void rotateMatrix(float x, float y, float z, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        this.xA = cos + x * x * (1.0F - cos);
        this.yA = z * sin + y * x * (1.0F - cos);
        this.zA = -y * sin + z * x * (1.0F - cos);
        this.xB = -z * sin + x * y * (1.0F - cos);
        this.yB = cos + y * y * (1.0F - cos);
        this.zB = x * sin + z * y * (1.0F - cos);
        this.xC = y * sin + x * z * (1.0F - cos);
        this.yC = -x * sin + y * z * (1.0F - cos);
        this.zC = cos + z * z * (1.0F - cos);
        this.zD = 0.0F;
        this.yD = 0.0F;
        this.xD = 0.0F;
    }

    public void translate(float offsetX, float offsetY, float offsetZ) {
        this.xD += offsetX;
        this.yD += offsetY;
        this.zD += offsetZ;
    }
    
    /**
     * Rotates the matrix around the given axis by the given angle.
     *
     * @param axisX x-component of the axis vector
     * @param axisY y-component of the axis vector
     * @param axisZ z-component of the axis vector
     * @param theta angle to rotate by
     */
    public void rotate(float axisX, float axisY, float axisZ, float theta) {
        float cos = (float) Math.cos(theta);
        float sin = (float) Math.sin(theta);

        // Calculate the rotation matrix elements
        float m00 = cos + axisX * axisX * (1.0F - cos);
        float m01 = axisZ * sin + axisY * axisX * (1.0F - cos);
        float m02 = -axisY * sin + axisZ * axisX * (1.0F - cos);
        float m10 = -axisZ * sin + axisX * axisY * (1.0F - cos);
        float m11 = cos + axisY * axisY * (1.0F - cos);
        float m12 = axisX * sin + axisZ * axisY * (1.0F - cos);
        float m20 = axisY * sin + axisX * axisZ * (1.0F - cos);
        float m21 = -axisX * sin + axisY * axisZ * (1.0F - cos);
        float m22 = cos + axisZ * axisZ * (1.0F - cos);

        // Apply the rotation matrix to the matrix
        float newXA = this.xA * m00 + this.yA * m10 + this.zA * m20;
        float newYA = this.xA * m01 + this.yA * m11 + this.zA * m21;
        float newZA = this.xA * m02 + this.yA * m12 + this.zA * m22;
        this.xA = newXA;
        this.yA = newYA;
        this.zA = newZA;

        float newXB = this.xB * m00 + this.yB * m10 + this.zB * m20;
        float newYB = this.xB * m01 + this.yB * m11 + this.zB * m21;
        float newZB = this.xB * m02 + this.yB * m12 + this.zB * m22;
        this.xB = newXB;
        this.yB = newYB;
        this.zB = newZB;

        float newXC = this.xC * m00 + this.yC * m10 + this.zC * m20;
        float newYC = this.xC * m01 + this.yC * m11 + this.zC * m21;
        float newZC = this.xC * m02 + this.yC * m12 + this.zC * m22;
        this.xC = newXC;
        this.yC = newYC;
        this.zC = newZC;

        float newXD = this.xD * m00 + this.yD * m10 + this.zD * m20;
        float newYD = this.xD * m01 + this.yD * m11 + this.zD * m21;
        float newZD = this.xD * m02 + this.yD * m12 + this.zD * m22;
        this.xD = newXD;
        this.yD = newYD;
        this.zD = newZD;
    }

    public void setViewPosition(float viewX, float viewY, float viewZ) {
        this.yC = 0.0F;
        this.xC = 0.0F;
        this.zB = 0.0F;
        this.xB = 0.0F;
        this.zA = 0.0F;
        this.yA = 0.0F;
        this.zC = 1.0F;
        this.yB = 1.0F;
        this.xA = 1.0F;
        this.xD = viewX;
        this.yD = viewY;
        this.zD = viewZ;
    }

    public String toString() {
        return this.xA + "," + this.xB + "," + this.xC + "," + this.xD + " - " + this.yA + "," + this.yB + "," + this.yC + "," + this.yD + " - " + this.zA + "," + this.zB + "," + this.zC + "," + this.zD;
    }

    public void calculatePerspective(float f_1, float f_2, float f_3, float[] perspective) {
        perspective[0] = this.xA * f_1 + this.xB * f_2 + this.xC * f_3 + this.xD;
        perspective[1] = this.yA * f_1 + this.yB * f_2 + this.yC * f_3 + this.yD;
        perspective[2] = this.zA * f_1 + this.zB * f_2 + this.zC * f_3 + this.zD;
    }

    public void calculateEyePosition(float x, float y, float z, float[] eyePosition) {
        x -= this.xD;
        y -= this.yD;
        z -= this.zD;
        eyePosition[0] = ((int) (this.xA * x + this.yA * y + this.zA * z));
        eyePosition[1] = ((int) (this.xB * x + this.yB * y + this.zB * z));
        eyePosition[2] = ((int) (this.xC * x + this.yC * y + this.zC * z));
    }

    public void method5230(float[] floats_1) {
        float f_2 = floats_1[0];
        float f_3 = floats_1[1];
        float f_4 = floats_1[2];
        floats_1[0] = this.xA * f_2 + this.yA * f_3 + this.zA * f_4;
        floats_1[1] = this.xB * f_2 + this.yB * f_3 + this.zB * f_4;
        floats_1[2] = this.xC * f_2 + this.yC * f_3 + this.zC * f_4;
    }

    public float[] cacheInto(float[] floats_1) {
        floats_1[0] = this.xD;
        floats_1[1] = this.yD;
        floats_1[2] = this.zD;
        return floats_1;
    }

    public void setFromQuaternion(Quaternion q) {
    	computeMatrix(q.x, q.y, q.z, q.w);
    }

    public void computeMatrix(float i, float j, float k, float scalar) {
        float i2 = i * i;
        float ij = i * j;
        float ik = i * k;
        float is = i * scalar;
        float j2 = j * j;
        float jk = j * k;
        float js = j * scalar;
        float k2 = k * k;
        float ks = k * scalar;
        this.xA = 1.0F - 2.0F * (j2 + k2);
        this.xB = 2.0F * (ij - ks);
        this.xC = 2.0F * (ik + js);
        this.yA = 2.0F * (ij + ks);
        this.yB = 1.0F - 2.0F * (i2 + k2);
        this.yC = 2.0F * (jk - is);
        this.zA = 2.0F * (ik - js);
        this.zB = 2.0F * (jk + is);
        this.zC = 1.0F - 2.0F * (i2 + j2);
        this.zD = 0.0F;
        this.yD = 0.0F;
        this.xD = 0.0F;
    }
    public void method5243(float[] floats_1) {
        float f_2 = floats_1[0] - this.xD;
        float f_3 = floats_1[1] - this.yD;
        float f_4 = floats_1[2] - this.zD;
        floats_1[0] = ((int) (this.xA * f_2 + this.yA * f_3 + this.zA * f_4));
        floats_1[1] = ((int) (this.xB * f_2 + this.yB * f_3 + this.zB * f_4));
        floats_1[2] = ((int) (this.xC * f_2 + this.yC * f_3 + this.zC * f_4));
    }

    public void scale(float scaleX, float scaleY) {
        this.xA *= scaleX;
        this.xB *= scaleX;
        this.xC *= scaleX;
        this.xD *= scaleX;
        this.yA *= scaleY;
        this.yB *= scaleY;
        this.yC *= scaleY;
        this.yD *= scaleY;
        this.zA *= (float) 1.0;
        this.zB *= (float) 1.0;
        this.zC *= (float) 1.0;
        this.zD *= (float) 1.0;
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        this.xA = scaleX;
        this.xB = 0.0F;
        this.xC = 0.0F;
        this.xD = 0.0F;
        this.yA = 0.0F;
        this.yB = scaleY;
        this.yC = 0.0F;
        this.yD = 0.0F;
        this.zA = 0.0F;
        this.zB = 0.0F;
        this.zC = scaleZ;
        this.zD = 0.0F;
    }

    public void multiply(Matrix43 left, Matrix43 right) {
        this.xA = left.xA * right.xA + left.yA * right.xB + left.zA * right.xC;
        this.yA = left.xA * right.yA + left.yA * right.yB + left.zA * right.yC;
        this.zA = left.xA * right.zA + left.yA * right.zB + left.zA * right.zC;
        this.xB = left.xB * right.xA + left.yB * right.xB + left.zB * right.xC;
        this.yB = left.xB * right.yA + left.yB * right.yB + left.zB * right.yC;
        this.zB = left.xB * right.zA + left.yB * right.zB + left.zB * right.zC;
        this.xC = left.xC * right.xA + left.yC * right.xB + left.zC * right.xC;
        this.yC = left.xC * right.yA + left.yC * right.yB + left.zC * right.yC;
        this.zC = left.xC * right.zA + left.yC * right.zB + left.zC * right.zC;
        this.xD = left.xD * right.xA + left.yD * right.xB + left.zD * right.xC + right.xD;
        this.yD = left.xD * right.yA + left.yD * right.yB + left.zD * right.yC + right.yD;
        this.zD = left.xD * right.zA + left.yD * right.zB + left.zD * right.zC + right.zD;
    }

    public void translate(Vector3 vector3) {
        translate(vector3.x, vector3.y, vector3.z);
    }

    public void fromCoordinateSpace(CoordinateSpace class3051) {
        float f_2 = class3051.quaternion.w * class3051.quaternion.w;
        float f_3 = class3051.quaternion.w * class3051.quaternion.x;
        float f_4 = class3051.quaternion.w * class3051.quaternion.y;
        float f_5 = class3051.quaternion.w * class3051.quaternion.z;
        float f_6 = class3051.quaternion.x * class3051.quaternion.x;
        float f_7 = class3051.quaternion.x * class3051.quaternion.y;
        float f_8 = class3051.quaternion.x * class3051.quaternion.z;
        float f_9 = class3051.quaternion.y * class3051.quaternion.y;
        float f_10 = class3051.quaternion.y * class3051.quaternion.z;
        float f_11 = class3051.quaternion.z * class3051.quaternion.z;
        this.xA = f_6 + f_2 - f_11 - f_9;
        this.yA = f_7 + f_5 + f_7 + f_5;
        this.zA = f_8 - f_4 - f_4 + f_8;
        this.xB = f_7 - f_5 - f_5 + f_7;
        this.yB = f_9 + f_2 - f_6 - f_11;
        this.zB = f_10 + f_3 + f_10 + f_3;
        this.xC = f_8 + f_4 + f_8 + f_4;
        this.yC = f_10 - f_3 - f_3 + f_10;
        this.zC = f_11 + f_2 - f_9 - f_6;
        this.xD = class3051.coords.x;
        this.yD = class3051.coords.y;
        this.zD = class3051.coords.z;
    }

}
