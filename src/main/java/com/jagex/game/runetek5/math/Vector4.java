package com.jagex.game.runetek5.math;

/**
 * 
 * @author Brainpower - 04/03/2023
 *
 */
public class Vector4 {

    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4(float x, float y, float z, float w) {
        set(x, y, z, w);
    }

    public Vector4() {
        reset();
    }

    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void copy(Vector4 vector4) {
        set(vector4.x, vector4.y, vector4.z, vector4.w);
    }

    public void unpack(int bitpacked) {
        set(
    		(bitpacked >> 16 & 0xff) * 0.003921569F, 
    		(bitpacked >> 8 & 0xff) * 0.003921569F, 
    		(bitpacked & 0xff) * 0.003921569F, 
    		(bitpacked >> 24 & 0xff) * 0.003921569F
        );
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = -this.w;
    }

    public void reset() {
        this.w = 0.0F;
        this.z = 0.0F;
        this.y = 0.0F;
        this.x = 0.0F;
    }

    public String toString() {
        return this.x + "," + this.y + "," + this.z + "," + this.w;
    }

    public void concat(Matrix44 matrix4x4) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        this.x = matrix4x4.faceList[0] * x + matrix4x4.faceList[4] * y + matrix4x4.faceList[8] * z + matrix4x4.faceList[12] * w;
        this.y = matrix4x4.faceList[1] * x + matrix4x4.faceList[5] * y + matrix4x4.faceList[9] * z + matrix4x4.faceList[13] * w;
        this.z = matrix4x4.faceList[2] * x + matrix4x4.faceList[6] * y + matrix4x4.faceList[10] * z + matrix4x4.faceList[14] * w;
        this.w = matrix4x4.faceList[3] * x + matrix4x4.faceList[7] * y + matrix4x4.faceList[11] * z + matrix4x4.faceList[15] * w;
    }

    public void scale(float multiplier) {
        this.x *= multiplier;
        this.y *= multiplier;
        this.z *= multiplier;
        this.w *= multiplier;
    }

}
