package com.jagex.game.runetek5.math;

public class Vector3 {

    public float x;
    public float y;
    public float z;

    public Vector3() {
    }

    public Vector3(Vector3 source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
    }

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void pushVectorStack() {
        synchronized (Vector3Pool.pool) {
            if (Vector3Pool.cursor < Vector3Pool.capacity - 1) {
                Vector3Pool.pool[Vector3Pool.cursor++] = this;
            }

        }
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void copy(Vector3 source) {
        set(source.x, source.y, source.z);
    }

    public void negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }

    public void subtract(float deltaX, float deltaY, float deltaZ) {
        this.x -= deltaX;
        this.y -= deltaY;
        this.z -= deltaZ;
    }

    public float magnitude() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public void transform(Quaternion source) {
        Quaternion pure = QuaternionPool.pureQuaternionFrom(this.x, this.y, this.z);
        Quaternion negatedSource = QuaternionPool.negateQuaternion(source);
        Quaternion result = QuaternionPool.multiply(negatedSource, pure);
        result.multiply(source);
        set(result.x, result.y, result.z);
        pure.store();
        negatedSource.store();
        result.store();
    }

    public void add(Vector3 vector31) {
        this.x += vector31.x;
        this.y += vector31.y;
        this.z += vector31.z;
    }

    public void transform(Matrix43 matrix44var1) {
        float f_2 = this.x;
        float f_3 = this.y;
        this.x = matrix44var1.xA * f_2 + matrix44var1.xB * f_3 + matrix44var1.xC * this.z;
        this.y = matrix44var1.yA * f_2 + matrix44var1.yB * f_3 + matrix44var1.yC * this.z;
        this.z = matrix44var1.zA * f_2 + matrix44var1.zB * f_3 + matrix44var1.zC * this.z;
    }

    public void transformWithTranslation(Matrix43 matrix) {
        float tmpX = this.x;
        float tmpY = this.y;
        this.x = matrix.xA * tmpX + matrix.xB * tmpY + matrix.xC * this.z + matrix.xD;
        this.y = matrix.yA * tmpX + matrix.yB * tmpY + matrix.yC * this.z + matrix.yD;
        this.z = matrix.zA * tmpX + matrix.zB * tmpY + matrix.zC * this.z + matrix.zD;
    }

    public void subtract(Vector3 vector) {
        this.x -= vector.x;
        this.y -= vector.y;
        this.z -= vector.z;
    }
    
    public String toString() {
        return this.x + ", " + this.y + ", " + this.z;
    }
}
