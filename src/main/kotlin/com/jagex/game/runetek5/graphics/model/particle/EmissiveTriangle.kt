package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.config.particle.ParticleProducerType

class EmissiveTriangle(
    @JvmField val producerId: Int,
    @JvmField val faceA: Int,
    @JvmField val faceB: Int,
    @JvmField val faceC: Int,
    @JvmField val priority: Byte
) {

    @JvmField var verticeXB: Int = 0
    @JvmField var child: EmissiveTriangle? = null
    @JvmField var verticeYC: Int = 0
    @JvmField var verticeYA: Int = 0
    @JvmField var verticeYB: Int = 0
    @JvmField var verticeXA: Int = 0
    @JvmField var verticeZB: Int = 0
    @JvmField var verticeXC: Int = 0
    @JvmField var verticeZA: Int = 0
    @JvmField var verticeZC: Int = 0

    fun getProducer(): ParticleProducerType {
        return ParticleProducerType.list(this.producerId)
    }

    fun transform(x: Int, y: Int, z: Int): EmissiveTriangle {
        return EmissiveTriangle(this.producerId, x, y, z, this.priority)
    }
}
