package com.jagex.game.runetek5.math;

/**
 * [J]
 * @author Steven - 16/08/2023
 * 
 * A quaternion is basically an axis in 3D space with a angle of rotation around the axis. 
 * Four values make up a quaternion, namely x, y, z and w. 
 * Three of the values are used to represent the axis in vector format, and the forth value would be the angle of rotation around the axis.
 *
 */
public class Quaternion {

    public float x;
    public float y;
    public float z;
    
    /**
     * The angle of rotation around the axis
     */
    public float w;

    public Quaternion() {
        reset();
    }

    public Quaternion(float x, float y, float z) {
        set(x, y, z, 0.0f);
    }

    public Quaternion(Quaternion other) {
        copy(other);
    }
    
    public void store() {
        synchronized (QuaternionPool.pool) {
            if (QuaternionPool.cursor < QuaternionPool.capacity - 1) {
                QuaternionPool.pool[QuaternionPool.cursor++] = this;
            }
        }
    }
    
    public void set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void copy(Quaternion source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        this.w = source.w;
    }
    
    public void rotate(float x, float y, float z, float angleInRadians) {
        float halfRotationSine = (float) StrictMath.sin(angleInRadians * 0.5F);
        float halfRotationCosine = (float) StrictMath.cos(angleInRadians * 0.5F);
        this.x = x * halfRotationSine;
        this.y = y * halfRotationSine;
        this.z = z * halfRotationSine;
        this.w = halfRotationCosine;
    }

    public void rotateEuler(float yaw, float pitch, float roll) {
        rotate(0.0F, 1.0F, 0.0F, yaw);
        Quaternion q = QuaternionPool.newQuaternion();
        q.rotate(1.0F, 0.0F, 0.0F, pitch);
        multiply(q);
        q.rotate(0.0F, 0.0F, 1.0F, roll);
        multiply(q);
        q.store();
    }
    
    public void reset() {
        this.z = 0.0F;
        this.y = 0.0F;
        this.x = 0.0F;
        this.w = 1.0F;
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }

    public void multiply(Quaternion other) {
        set(
			other.w * this.x + other.x * this.w + other.y * this.z - other.z * this.y,
			other.w * this.y - other.x * this.z + other.y * this.w + other.z * this.x,
			other.w * this.z + other.x * this.y - other.y * this.x + other.z * this.w,
			other.w * this.w - other.x * this.x - other.y * this.y - other.z * this.z
		);
    }

    public String toString() {
        return this.x + "," + this.y + "," + this.z + "," + this.w;
    }
}
