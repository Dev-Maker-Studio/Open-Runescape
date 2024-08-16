package com.jagex.game.runetek5.graphics.model.normal;

public class VertexNormal {

	public int x;
    public int y;
    public int z;
    public int magnitude;

    public VertexNormal() {
    	// NOOP
    }

    public VertexNormal(VertexNormal other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.magnitude = other.magnitude;
    }

}
