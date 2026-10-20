package com.jagex.game.runetek5.graphics.model.particle

import java.util.LinkedList

import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class ParticleSystem(cycle: Int, hasOntick: Boolean) {

    @JvmField var isDisabled: Boolean = false
    @JvmField var currentCycle: Long = 0
    @JvmField var lastCycleCompletion: Long = 0
    @JvmField var changePosition: Boolean = false
    @JvmField var containsOntickCycle: Boolean = false
    @JvmField var size: Int = 0
    @JvmField var particleProducers: MutableList<ParticleProducer> = LinkedList()
    @JvmField var particleProducerCount: Int = 0
    @JvmField var effectiveVerticeEffects: MutableList<EffectiveVerticeEffect> = LinkedList()
    @JvmField var effectsAwaitingRender: Int = 0
    @JvmField var isNewCycle: Boolean = false
    @JvmField var displayPlane: Int = 0
    @JvmField val wrapper: ParticleWrapper = ParticleWrapper()
    @JvmField val particles: Array<Particle?> = arrayOfNulls(8192)

    init {
        activate(cycle, hasOntick)
    }

    fun activate(startingCycle: Int, hasOntick: Boolean) {
        ParticleSystemManager.activeParticleSystems!!.add(this)
        this.currentCycle = startingCycle.toLong()
        this.lastCycleCompletion = startingCycle.toLong()
        this.changePosition = true
        this.containsOntickCycle = hasOntick
    }

    fun onEntityUpdate() {
        this.changePosition = true
    }

    fun destroy() {
        this.isDisabled = true
        for (particleNode in this.effectiveVerticeEffects) {
            if (particleNode.particleType.particleHandlingType == 1) {
                particleNode.unlink()
            }
        }
        for (particleIndex in this.particles.indices) {
            if (this.particles[particleIndex] != null) {
                this.particles[particleIndex]!!.kill()
                this.particles[particleIndex] = null
            }
        }
        this.size = 0
        this.particleProducers = LinkedList()
        this.particleProducerCount = 0
        this.effectiveVerticeEffects = LinkedList()
        this.effectsAwaitingRender = 0
    }

    fun setIsOldCycle() {
        this.isNewCycle = false
    }

    fun performUpdate(toolkit: AbstractToolkit, currentTime: Long): Boolean {
        if (this.currentCycle != this.lastCycleCompletion) {
            setIsNewCycle()
        } else {
            setIsOldCycle()
        }
        if (currentTime - this.currentCycle > 750L) {
            destroy()
            return false
        }
        val cycleDuration = (currentTime - this.lastCycleCompletion).toInt()
        if (this.changePosition) {
            val producerIt = this.particleProducers.iterator()
            while (producerIt.hasNext()) {
                val producer = producerIt.next()
                for (index in 0 until producer.definition.updatesPerCycle) {
                    producer.performUpdate(toolkit, currentTime, 1, !this.isNewCycle)
                }
            }
            this.changePosition = false
        }
        val producerIt = this.particleProducers.iterator()
        while (producerIt.hasNext()) {
            val producer = producerIt.next()
            producer.performUpdate(toolkit, currentTime, cycleDuration, !this.isNewCycle)
        }
        this.lastCycleCompletion = currentTime
        return true
    }

    fun performCycle(toolkit: AbstractToolkit, cycle: Long, emitters: Array<EmissiveTriangle>?, effectiveVertices: Array<EffectiveVertex>?) {
        if (!this.isDisabled) {
            build(toolkit, emitters)
            initializeEffects(effectiveVertices)
            this.currentCycle = cycle
        }
    }

    fun setCurrentCycle(currentCycle: Long) {
        this.currentCycle = currentCycle
    }

    fun initializeEffects(effectiveVertices: Array<EffectiveVertex>?) {
        for (index in 0..7) {
            EFFECT_SLOT_OCCUPIED[index] = false
        }
        val iterator = this.effectiveVerticeEffects.iterator()
        UnlinkMismatches@ while (iterator.hasNext()) {
            val effectiveVertexEffect = iterator.next()
            if (effectiveVertices != null) {
                for (index in effectiveVertices.indices) {
                    if (effectiveVertices[index] == effectiveVertexEffect.effectiveVertex || effectiveVertices[index].child == effectiveVertexEffect.effectiveVertex) {
                        EFFECT_SLOT_OCCUPIED[index] = true
                        effectiveVertexEffect.loadVertexPositions()
                        continue@UnlinkMismatches
                    }
                }
            }
            iterator.remove()
            this.effectsAwaitingRender--
            if (effectiveVertexEffect.linked()) {
                effectiveVertexEffect.unlink()
                ParticleSystemManager.activeVertexEffectCount--
            }
        }
        if (effectiveVertices != null) {
            var index = 0
            while (index < effectiveVertices.size && index != 8 && this.effectsAwaitingRender != 8) {
                if (!EFFECT_SLOT_OCCUPIED[index]) {
                    var effectiveVerticeEffect: EffectiveVerticeEffect? = null
                    if (effectiveVertices[index].getParticleType().particleHandlingType == 1 && ParticleSystemManager.activeVertexEffectCount < 32) {
                        effectiveVerticeEffect = EffectiveVerticeEffect(effectiveVertices[index])
                        ParticleSystemManager.activeEffectiveVertexEffects.put(effectiveVerticeEffect, effectiveVertices[index].effectId.toLong())
                        ParticleSystemManager.activeVertexEffectCount++
                    }
                    if (effectiveVerticeEffect == null) {
                        effectiveVerticeEffect = EffectiveVerticeEffect(effectiveVertices[index])
                    }
                    this.effectiveVerticeEffects.add(effectiveVerticeEffect)
                    this.effectsAwaitingRender++
                    EFFECT_SLOT_OCCUPIED[index] = true
                }
                index++
            }
        }
    }

    fun setLogicHeight(logicHeight: Int) {
        this.displayPlane = logicHeight
    }

    fun processCycleAllParticles(scene: Scene, toolkit: AbstractToolkit) {
        this.wrapper.particleBinaryTree.unlinkAll()
        for (producer in this.particleProducers) {
            producer.processCycle(scene, toolkit)
        }
    }

    fun getAllParticles(): ParticleWrapper {
        return this.wrapper
    }

    fun setIsNewCycle() {
        this.isNewCycle = true
    }

    fun rebuildRenderedParticles(): ParticleWrapper {
        this.wrapper.particleBinaryTree.unlinkAll()
        for (index in this.particles.indices) {
            if (this.particles[index] != null && this.particles[index]!!.producer != null) {
                this.wrapper.particleBinaryTree.insert(this.particles[index]!!)
            }
        }
        return this.wrapper
    }

    fun build(toolkit: AbstractToolkit, emitters: Array<EmissiveTriangle>?) {
        for (index in 0..31) {
            ACTIVE_PRODUCERS[index] = false
        }
        val producerIt = this.particleProducers.iterator()
        PositionUpdate@ while (producerIt.hasNext()) {
            val producer = producerIt.next()
            if (emitters != null) {
                for (index in emitters.indices) {
                    if (emitters[index] == producer.particleEmitter || emitters[index].child == producer.particleEmitter) {
                        ACTIVE_PRODUCERS[index] = true
                        producer.updatePosition()
                        producer.requiresPositionUpdate = false
                        continue@PositionUpdate
                    }
                }
            }
            if (producer.particleCount == 0) {
                producerIt.remove()
                this.particleProducerCount--
            } else {
                producer.requiresPositionUpdate = true
            }
        }
        if (emitters != null) {
            var index = 0
            while (index < emitters.size && index != 32 && this.particleProducerCount != 32) {
                if (!ACTIVE_PRODUCERS[index]) {
                    val producer = ParticleProducer(toolkit, emitters[index], this, this.currentCycle)
                    this.particleProducers.add(producer)
                    this.particleProducerCount++
                    ACTIVE_PRODUCERS[index] = true
                }
                index++
            }
        }
    }

    companion object {
        @JvmField val ACTIVE_PRODUCERS: BooleanArray = BooleanArray(32)
        @JvmField val EFFECT_SLOT_OCCUPIED: BooleanArray = BooleanArray(8)

        @JvmStatic
        fun get(cycle: Int, hasOntick: Boolean): ParticleSystem {
            if (ParticleSystemManager.awaitingRemovalCursorPosition != ParticleSystemManager.removalQueueSize) {
                val cachedParticleSystem = ParticleSystemManager.particleSystemsAwaitingRemoval!![ParticleSystemManager.removalQueueSize]!!
                ParticleSystemManager.removalQueueSize = ParticleSystemManager.removalQueueSize + 1 and ParticleSystemManager.DETAIL_LEVEL_LIMITS!![ParticleSystemManager.detailsSetting]
                cachedParticleSystem.activate(cycle, hasOntick)
                return cachedParticleSystem
            }
            return ParticleSystem(cycle, hasOntick)
        }
    }
}
