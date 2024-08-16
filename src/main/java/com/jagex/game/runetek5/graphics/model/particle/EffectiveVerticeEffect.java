package com.jagex.game.runetek5.graphics.model.particle;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.particle.ParticleType;

public class EffectiveVerticeEffect extends Node<EffectiveVerticeEffect> {

    public static final float[] defaultMatrix = new float[3];
    int verticeX;
    int verticeY;
    int verticeZ;
    float defaultMatrixX;
    float defaultMatrixZ;
    final EffectiveVertex effectiveVertex;
    final ParticleType particleType;

    EffectiveVerticeEffect(EffectiveVertex effectiveVertex) {
        this.effectiveVertex = effectiveVertex;
        this.particleType = this.effectiveVertex.getParticleType();
        this.loadVertexPositions();
    }

    void loadVertexPositions() {
        this.verticeX = this.effectiveVertex.verticeX;
        this.verticeY = this.effectiveVertex.verticeY;
        this.verticeZ = this.effectiveVertex.verticeZ;
        if (this.effectiveVertex.matrix != null) {
            this.effectiveVertex.matrix.multiply(this.particleType.offsetX, this.particleType.offsetY, this.particleType.offsetZ, defaultMatrix);
        }
        this.defaultMatrixX = defaultMatrix[0];
        this.defaultMatrixZ = defaultMatrix[2];
    }
}
