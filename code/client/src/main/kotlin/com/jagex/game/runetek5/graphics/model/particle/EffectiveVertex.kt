package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.config.particle.ParticleType
import com.jagex.game.runetek5.config.particle.ParticleTypeList
import com.jagex.game.runetek5.math.Matrix44

class EffectiveVertex(
    @JvmField val effectId: Int,
    @JvmField val appliesOnVertex: Int
) {

    @JvmField var verticeX: Int = 0
    @JvmField var child: EffectiveVertex? = null
    @JvmField var matrix: Matrix44? = null
    @JvmField var verticeY: Int = 0
    @JvmField var verticeZ: Int = 0

    fun switchVertex(newApplyingVertex: Int): EffectiveVertex {
        return EffectiveVertex(this.effectId, newApplyingVertex)
    }

    fun getParticleType(): ParticleType {
        return ParticleTypeList.listParticle(this.effectId)
    }
}
