package com.jagex.game.runetek5.config.particle;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class ParticleTypeList {
	
    public static Js5ResourceProvider PARTICLE_INDEX;

    public static void initialize(Js5ResourceProvider particleProvider) {
    	ParticleTypeList.PARTICLE_INDEX = particleProvider;
    }

    public static ParticleType listParticle(int particleFileId) {
        ParticleType particletype = (ParticleType) ParticleType.particleCache.get(particleFileId);
        if (particletype == null) {
            byte[] data = ParticleTypeList.PARTICLE_INDEX.getFile(1, particleFileId);
            particletype = new ParticleType();
            particletype.partileFileId = particleFileId;
            if (data != null) {
                particletype.decode(new Packet(data));
            }

            particletype.setDefaults();
            if (particletype.particleHandlingType == 2 && ParticleType.particleTypeCache.get(particleFileId) == null) {
                ParticleType.particleTypeCache.put(new IntNode(ParticleType.particleType), particleFileId);
                ParticleType.particleTypes[++ParticleType.particleType - 1] = particletype;
            }

            ParticleType.particleCache.put(particletype, particleFileId);
        }
        return particletype;
    }
}
