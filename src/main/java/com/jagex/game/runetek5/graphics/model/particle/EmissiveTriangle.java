package com.jagex.game.runetek5.graphics.model.particle;

import com.jagex.game.runetek5.config.particle.ParticleProducerType;

public class EmissiveTriangle {

    public int verticeXB;
    public EmissiveTriangle child;
    public int verticeYC;
    public int verticeYA;
    public int verticeYB;
    public int verticeXA;
    public int verticeZB;
    public int verticeXC;
    public int verticeZA;
    public int verticeZC;
    public final int faceA;
    public final int faceB;
    public final int faceC;
    public final byte priority;
    public final int producerId;

    public EmissiveTriangle(int particleId, int faceX, int faceY, int faceZ, byte priority) {
        this.producerId = particleId;
        this.faceA = faceX;
        this.faceB = faceY;
        this.faceC = faceZ;
        this.priority = priority;
    }

    public ParticleProducerType getProducer() {
        return ParticleProducerType.list(this.producerId);
    }

    public EmissiveTriangle transform(int x, int y, int z) {
        return new EmissiveTriangle(this.producerId, x, y, z, this.priority);
    }
}
