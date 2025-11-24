package com.jagex.game.runetek5.config.particle

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.js5.Js5ResourceProvider

object ParticleTypeList {

    @JvmField var PARTICLE_INDEX: Js5ResourceProvider? = null

    @JvmStatic
    fun initialize(particleProvider: Js5ResourceProvider) {
        PARTICLE_INDEX = particleProvider
    }

    @JvmStatic
    fun listParticle(particleFileId: Int): ParticleType {
        var particletype = ParticleType.particleCache.get(particleFileId.toLong()) as ParticleType?
        if (particletype == null) {
            val data = PARTICLE_INDEX!!.getFile(1, particleFileId)
            particletype = ParticleType()
            particletype.partileFileId = particleFileId
            if (data != null) {
                particletype.decode(Packet(data))
            }

            particletype.setDefaults()
            if (particletype.particleHandlingType == 2 && ParticleType.particleTypeCache.get(particleFileId.toLong()) == null) {
                @Suppress("UNCHECKED_CAST")
                (ParticleType.particleTypeCache as com.jagex.game.runetek5.cache.node.IterableNodeMap<IntNode>).put(IntNode(ParticleType.particleType), particleFileId.toLong())
                ParticleType.particleTypes[++ParticleType.particleType - 1] = particletype
            }

            ParticleType.particleCache.put(particletype, particleFileId.toLong())
        }
        return particletype
    }
}
