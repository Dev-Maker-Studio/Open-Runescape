package com.jagex.game.runetek5.movement;

import com.jagex.game.runetek5.math.Quaternion;
import com.jagex.game.runetek5.math.Vector3;

public class CoordinateSpace {

    public final Quaternion quaternion = new Quaternion();
    public final Vector3 coords = new Vector3();

    public CoordinateSpace() {
    }

    public CoordinateSpace(CoordinateSpace coordinateSpace) {
        copy(coordinateSpace);
    }

    public void copy(CoordinateSpace coordinateSpace) {
        this.quaternion.copy(coordinateSpace.quaternion);
        this.coords.copy(coordinateSpace.coords);
    }

    public void negate() {
        this.quaternion.negate();
        this.coords.negate();
        this.coords.transform(this.quaternion);
    }

    public void offset(CoordinateSpace pos2) {
        this.quaternion.multiply(pos2.quaternion);
        this.coords.transform(pos2.quaternion);
        this.coords.add(pos2.coords);
    }

    public String toString() {
        return "[" + this.quaternion + "|" + this.coords + "]";
    }

}
