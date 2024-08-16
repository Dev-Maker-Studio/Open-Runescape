package com.jagex.game.runetek5.entity.pathingentity;

public class EntityTurnDirection {

    public int rotation;

    public int anInt154;

    public boolean calculateRotation(int lastYawRotation, int turndir1, int turndir2) {
        int i_5 = this.anInt154;
        if (this.rotation == lastYawRotation && this.anInt154 == 0) {
            return false;
        } 
        boolean bool6;
        if (this.anInt154 != 0) {
            if (this.anInt154 > 0 && lastYawRotation > this.rotation) {
                int i_7 = this.anInt154 * this.anInt154 / (turndir1 * 2);
                int i_8 = i_7 + this.rotation;
                bool6 = i_8 < lastYawRotation && i_8 >= this.rotation;
            } else if (this.anInt154 < 0 && lastYawRotation < this.rotation) {
                int i_7 = this.anInt154 * this.anInt154 / (turndir1 * 2);
                int i_8 = this.rotation - i_7;
                bool6 = i_8 > lastYawRotation && i_8 <= this.rotation;
            } else {
                bool6 = false;
            }
        } else {
            if (lastYawRotation > this.rotation && lastYawRotation <= turndir1 + this.rotation || lastYawRotation < this.rotation && lastYawRotation >= this.rotation - turndir1) {
                this.rotation = lastYawRotation;
                return false;
            }
            bool6 = true;
        }
        if (bool6) {
            if (lastYawRotation > this.rotation) {
                this.anInt154 += turndir1;
                if (turndir2 != 0 && this.anInt154 > turndir2) {
                    this.anInt154 = turndir2;
                }
            } else {
                this.anInt154 -= turndir1;
                if (turndir2 != 0 && this.anInt154 < -turndir2) {
                    this.anInt154 = -turndir2;
                }
            }
            if (i_5 != this.anInt154) {
                int i_7 = this.anInt154 * this.anInt154 / (turndir1 * 2);
                if (lastYawRotation > this.rotation) {
                    if (i_7 + this.rotation > lastYawRotation) {
                        this.anInt154 = i_5;
                    }
                } else if (this.rotation - i_7 < lastYawRotation) {
                    this.anInt154 = i_5;
                }
            }
        } else if (this.anInt154 > 0) {
            this.anInt154 -= turndir1;
            if (this.anInt154 < 0) {
                this.anInt154 = 0;
            }
        } else {
            this.anInt154 += turndir1;
            if (this.anInt154 > 0) {
                this.anInt154 = 0;
            }
        }
        this.rotation += i_5 + this.anInt154 >> 1;
        return bool6;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        this.anInt154 = 0;
    }

    public int getRotation() {
        return this.rotation & 0x3fff;
    }

    public void clamp() {
    	this.rotation &= 0x3fff;
    }
}
