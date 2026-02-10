package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.particle.ParticleTypeList
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import java.util.LinkedList

/**
 * Manages active particle systems and particle pooling.
 */
object ParticleSystemManager {

    @JvmField var activeParticleSystems: MutableList<ParticleSystem>? = null
    @JvmField var particleCache: Array<Particle?>? = null
    @JvmField var particleCacheCursorPos: Int = 0
    @JvmField var killIndex: Int = 0
    @JvmField var removalQueueSize: Int = 0
    @JvmField var awaitingRemovalCursorPosition: Int = 0
    @JvmField var lastRenderedParticleCount: Int = 0
    @JvmField var activeEffectiveVertexEffects = IterableNodeMap<EffectiveVerticeEffect>(8)
    @JvmField var activeVertexEffectCount: Int = 0
    @JvmField var detailsSetting: Int = 2
    @JvmField var renderedParticleCount: Int = 0
    @JvmField var particleSystemsAwaitingRemoval: Array<ParticleSystem?>? = null
    @JvmField val DETAIL_LEVEL_LIMITS = intArrayOf(3, 7, 15)

    @JvmStatic
    fun initializeIndex(particleProvider: Js5ResourceProvider) {
        particleCacheCursorPos = 0
        killIndex = 0
        activeParticleSystems = LinkedList()
        particleCache = arrayOfNulls(1024)
        particleSystemsAwaitingRemoval = arrayOfNulls(DETAIL_LEVEL_LIMITS[detailsSetting] + 1)
        removalQueueSize = 0
        awaitingRemovalCursorPosition = 0
        initialize(particleProvider)
        ParticleTypeList.initialize(particleProvider)
    }

    @JvmStatic
    fun initialize(particleProvider: Js5ResourceProvider) {
        Resource.PARTICLE_PRODUCER = particleProvider
    }

    @JvmStatic
    fun destroy() {
        activeEffectiveVertexEffects = IterableNodeMap(8)
        activeVertexEffectCount = 0
        for (system in activeParticleSystems!!) {
            system.destroy()
        }
    }

    @JvmStatic
    fun processParticleCycle(scene: Scene, toolkit: AbstractToolkit) {
        for (activeSystem in activeParticleSystems!!) {
            if (activeSystem.containsOntickCycle) {
                activeSystem.processCycleAllParticles(scene, toolkit)
            }
        }
    }

    @JvmStatic
    fun setDetail(value: Int) {
        var adjustedValue = value
        if (adjustedValue < 0 || adjustedValue > 2) {
            adjustedValue = 0
        }
        detailsSetting = adjustedValue
        particleSystemsAwaitingRemoval = arrayOfNulls(DETAIL_LEVEL_LIMITS[detailsSetting] + 1)
        removalQueueSize = 0
        awaitingRemovalCursorPosition = 0
    }

    @JvmStatic
    fun getDetailLevel(): Int {
        return detailsSetting
    }
}
