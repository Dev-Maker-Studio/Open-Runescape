package com.jagex.game.runetek5.graphics.model.particle;

import com.jagex.game.runetek5.config.particle.ParticleType;
import com.jagex.game.runetek5.config.particle.ParticleTypeList;
import com.jagex.game.runetek5.math.Matrix44;

public class EffectiveVertex {

    public int verticeX;

    public EffectiveVertex child;

    public Matrix44 matrix;

    public int verticeY;

    public int verticeZ;

    public final int effectId;

    public final int appliesOnVertex;

    public EffectiveVertex(int effectId, int appliesOnVertex) {
        this.effectId = effectId;
        this.appliesOnVertex = appliesOnVertex;
    }

    public EffectiveVertex switchVertex(int newApplyingVertex) {
        return new EffectiveVertex(this.effectId, newApplyingVertex);
    }

    public ParticleType getParticleType() {
        return ParticleTypeList.listParticle(this.effectId);
    }
}
