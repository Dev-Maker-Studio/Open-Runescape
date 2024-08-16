package com.jagex.game.runetek5.graphics.model.particle;

public class ParticleTriangle {
	public int verticeZC;
	public int verticeYA;
	public int verticeZA;
    public int centerY;
    public int verticeXA;
    public int verticeZB;
    public int verticeXC;
    public int verticeYC;
    public int centerZ;
    public int verticeXB;
    public int centerX;
    public int verticeYB;

    boolean equals(ParticleTriangle other) {
        return this.centerX == other.centerX && this.centerY == other.centerY && this.centerZ == other.centerZ;
    }
}
