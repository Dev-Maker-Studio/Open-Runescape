package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.particle.ParticleType

class EffectiveVerticeEffect(
    @JvmField val effectiveVertex: EffectiveVertex
) : Node<EffectiveVerticeEffect>() {

    @JvmField var verticeX: Int = 0
    @JvmField var verticeY: Int = 0
    @JvmField var verticeZ: Int = 0
    @JvmField var defaultMatrixX: Float = 0f
    @JvmField var defaultMatrixZ: Float = 0f
    @JvmField val particleType: ParticleType

    init {
        this.particleType = this.effectiveVertex.getParticleType()
        this.loadVertexPositions()
    }

    fun loadVertexPositions() {
        this.verticeX = this.effectiveVertex.verticeX
        this.verticeY = this.effectiveVertex.verticeY
        this.verticeZ = this.effectiveVertex.verticeZ
        if (this.effectiveVertex.matrix != null) {
            this.effectiveVertex.matrix.multiply(this.particleType.offsetX.toFloat(), this.particleType.offsetY.toFloat(), this.particleType.offsetZ.toFloat(), defaultMatrix)
        }
        this.defaultMatrixX = defaultMatrix[0]
        this.defaultMatrixZ = defaultMatrix[2]
    }

    companion object {
        @JvmField val defaultMatrix: FloatArray = FloatArray(3)
    }
}
