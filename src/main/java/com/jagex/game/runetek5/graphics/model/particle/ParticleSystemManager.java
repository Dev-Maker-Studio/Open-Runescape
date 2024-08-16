package com.jagex.game.runetek5.graphics.model.particle;

import java.util.LinkedList;
import java.util.List;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.particle.ParticleTypeList;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class ParticleSystemManager {

	public static List<ParticleSystem> activeParticleSystems;
    public static Particle[] particleCache;
    public static int particleCacheCursorPos;
    public static int killIndex;
    public static int anInt2899;
    public static int awaitingRemovalCursorPosition;
    public static int lastRenderedParticleCount;
    public static IterableNodeMap<EffectiveVerticeEffect> activeEffectiveVertexEffects = new IterableNodeMap<EffectiveVerticeEffect>(8);
    public static int activeVertexEffectCount;
    public static int detailsSetting = 2;
    public static int renderedParticleCount;
    public static ParticleSystem[] particleSystemsAwaitingRemoval;
	public static final int[] anIntArray931 = {3, 7, 15};

    public ParticleSystemManager() {
        throw new Error();
    }
    
    public static void initializeIndex(Js5ResourceProvider particleProvider) {
        ParticleSystemManager.particleCacheCursorPos = 0;
        ParticleSystemManager.killIndex = 0;
        ParticleSystemManager.activeParticleSystems = new LinkedList<ParticleSystem>();
        ParticleSystemManager.particleCache = new Particle[1024];
        ParticleSystemManager.particleSystemsAwaitingRemoval = new ParticleSystem[ParticleSystemManager.anIntArray931[ParticleSystemManager.detailsSetting] + 1];
        ParticleSystemManager.anInt2899 = 0;
        ParticleSystemManager.awaitingRemovalCursorPosition = 0;
        ParticleSystemManager.initialize(particleProvider);
        ParticleTypeList.initialize(particleProvider);
    }
    
    public static void initialize(Js5ResourceProvider particleProvider) {
        Resource.PARTICLE_PRODUCER = particleProvider;
    }
    
    public static void destroy() {
        ParticleSystemManager.activeEffectiveVertexEffects = new IterableNodeMap<EffectiveVerticeEffect>(8);
        ParticleSystemManager.activeVertexEffectCount = 0;
        for (ParticleSystem system : ParticleSystemManager.activeParticleSystems) {
            system.destroy();
        }
    }

    public static void processParticleCycle(Scene scene, AbstractToolkit toolkit) {
        for (ParticleSystem activeSystem : activeParticleSystems) {
            if (activeSystem.containsOntickCycle) {
                activeSystem.processCycleAllParticles(scene, toolkit);
            }
        }
    }

    public static void setDetail(int value) {
        if (value < 0 || value > 2) {
            value = 0;
        }
        detailsSetting = value;
        particleSystemsAwaitingRemoval = new ParticleSystem[ParticleSystemManager.anIntArray931[detailsSetting] + 1];
        anInt2899 = 0;
        awaitingRemovalCursorPosition = 0;
    }

    public static int method5206() {
        return detailsSetting;
    }
}
