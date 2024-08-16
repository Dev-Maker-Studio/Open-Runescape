package com.jagex.game.runetek5.graphics.model.particle;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

/**
 * [J]
 * @author Steven
 *
 */
public class ParticleSystem {
	
    public static final boolean[] ACTIVE_PRODUCERS = new boolean[32];
    public static final boolean[] EFFECT_SLOT_OCCUPIED = new boolean[8];
    public boolean isDisabled;
    public long currentCycle;
    public long lastCycleCompletion;
    public boolean changePosition;
    public boolean containsOntickCycle;
    int size;
    public List<ParticleProducer> particleProducers = new LinkedList<>();
    public int particleProducerCount;
    List<EffectiveVerticeEffect> effectiveVerticeEffects = new LinkedList<EffectiveVerticeEffect>();
    public int effectsAwaitingRender;
    public boolean isNewCycle;
    int displayPlane;
    final ParticleWrapper wrapper = new ParticleWrapper();
    final Particle[] particles = new Particle[8192];

    public ParticleSystem(int cycle, boolean hasOntick) {
        activate(cycle, hasOntick);
    }

    public static ParticleSystem get(int cycle, boolean hasOntick) {
        if (ParticleSystemManager.awaitingRemovalCursorPosition != ParticleSystemManager.anInt2899) {
            ParticleSystem cachedParticleSystem = ParticleSystemManager.particleSystemsAwaitingRemoval[ParticleSystemManager.anInt2899];
            ParticleSystemManager.anInt2899 = ParticleSystemManager.anInt2899 + 1 & ParticleSystemManager.anIntArray931[ParticleSystemManager.detailsSetting];
            cachedParticleSystem.activate(cycle, hasOntick);
            return cachedParticleSystem;
        } 
        return new ParticleSystem(cycle, hasOntick);
    }

    public void activate(int startingCycle, boolean hasOntick) {
        ParticleSystemManager.activeParticleSystems.add(this);
        this.currentCycle = startingCycle;
        this.lastCycleCompletion = startingCycle;
        this.changePosition = true;
        this.containsOntickCycle = hasOntick;
    }

    /**
     * Called on rotation / movement
     */
    public void onEntityUpdate() {
    	this.changePosition = true;
    }

    public void destroy() {
        this.isDisabled = true;
        for (EffectiveVerticeEffect particleNode : this.effectiveVerticeEffects) {
            if (particleNode.particleType.particleHandlingType == 1) {
                particleNode.unlink();
            }
        }
        for (int particleIndex = 0; particleIndex < this.particles.length; particleIndex++) {
            if (this.particles[particleIndex] != null) {
            	this.particles[particleIndex].kill();
            	this.particles[particleIndex] = null;
            }
        }
        this.size = 0;
        this.particleProducers = new LinkedList<>();
        this.particleProducerCount = 0;
        this.effectiveVerticeEffects = new LinkedList<>();
        this.effectsAwaitingRender = 0;
    }

    public void setIsOldCycle() {
    	this.isNewCycle = false;
    }

    public boolean performUpdate(AbstractToolkit toolkit, long currentTime) {
        if (this.currentCycle != this.lastCycleCompletion) {
            setIsNewCycle();
        } else {
            setIsOldCycle();
        }
        if (currentTime - this.currentCycle > 750L) {
            destroy();
            return false;
        } 
        int cycleDuration = (int) (currentTime - this.lastCycleCompletion);
        if (this.changePosition) {
        	Iterator<ParticleProducer> producerIt = this.particleProducers.iterator();
            while (producerIt.hasNext()) {
            	ParticleProducer producer = producerIt.next();
                for (int index = 0; index < producer.definition.updatesPerCycle; index++) {
                    producer.performUpdate(toolkit, currentTime, 1, !this.isNewCycle);
                }
            }
            this.changePosition = false;
        } 
        Iterator<ParticleProducer> producerIt = this.particleProducers.iterator();
        while (producerIt.hasNext()) {
        	ParticleProducer producer = producerIt.next();
            producer.performUpdate(toolkit, currentTime, cycleDuration, !this.isNewCycle);
        }
        this.lastCycleCompletion = currentTime;
        return true;
    }

    public void performCycle(AbstractToolkit toolkit, long cycle, EmissiveTriangle[] emitters, EffectiveVertex[] effectiveVertices) {
        if (!this.isDisabled) {
            build(toolkit, emitters);
            initializeEffects(effectiveVertices);
            this.currentCycle = cycle;
        }
    }

    public void setCurrentCycle(long currentCycle) {
    	this.currentCycle = currentCycle;
    }
    
    public void initializeEffects(final EffectiveVertex[] effectiveVertices) {
        for (int index = 0; index < 8; ++index) {
            ParticleSystem.EFFECT_SLOT_OCCUPIED[index] = false;
        }    
        UnlinkMismatches: for (final EffectiveVerticeEffect effectiveVertexEffect : this.effectiveVerticeEffects) {
            if (effectiveVertices != null) {
                for (int index = 0; index < effectiveVertices.length; index++) {
                    if (effectiveVertices[index] == effectiveVertexEffect.effectiveVertex || effectiveVertices[index].child == effectiveVertexEffect.effectiveVertex) {
                        ParticleSystem.EFFECT_SLOT_OCCUPIED[index] = true;
                        effectiveVertexEffect.loadVertexPositions();
                        continue UnlinkMismatches;
                    }
                }
            }
            effectiveVertexEffect.unlink();
            this.effectsAwaitingRender--;
            if (effectiveVertexEffect.linked()) {
                effectiveVertexEffect.unlink();
                ParticleSystemManager.activeVertexEffectCount--;
            }
        }
        if (effectiveVertices != null) {
            for (int index = 0; index < effectiveVertices.length && index != 8 && this.effectsAwaitingRender != 8; index++) {
                if (!ParticleSystem.EFFECT_SLOT_OCCUPIED[index]) {
                    EffectiveVerticeEffect effectiveVerticeEffect = null;
                    if (effectiveVertices[index].getParticleType().particleHandlingType == 1 && ParticleSystemManager.activeVertexEffectCount < 32) {
                        effectiveVerticeEffect = new EffectiveVerticeEffect(effectiveVertices[index]);
                        ParticleSystemManager.activeEffectiveVertexEffects.put(effectiveVerticeEffect, effectiveVertices[index].effectId);
                        ParticleSystemManager.activeVertexEffectCount++;
                    }
                    if (effectiveVerticeEffect == null) {
                        effectiveVerticeEffect = new EffectiveVerticeEffect(effectiveVertices[index]);
                    }
                    this.effectiveVerticeEffects.add(effectiveVerticeEffect);
                    this.effectsAwaitingRender++;
                    ParticleSystem.EFFECT_SLOT_OCCUPIED[index] = true;
                }
            }
        }
    }

    public void setLogicHeight(int logicHeight) {
        this.displayPlane = logicHeight;
    }

    public void processCycleAllParticles(Scene scene, AbstractToolkit toolkit) {
    	this.wrapper.particleBinaryTree.unlinkAll();
        for (ParticleProducer producer : this.particleProducers) {
            producer.processCycle(scene, toolkit);
        }
    }

    public ParticleWrapper getAllParticles() {
        return this.wrapper;
    }

    public void setIsNewCycle() {
    	this.isNewCycle = true;
    }

    public ParticleWrapper rebuildRenderedParticles() {
    	this.wrapper.particleBinaryTree.unlinkAll();
        for (int index = 0; index < this.particles.length; index++) {
            if (this.particles[index] != null && this.particles[index].producer != null) {
            	this.wrapper.particleBinaryTree.insert(this.particles[index]);
            }
        }
        return this.wrapper;
    }   
    
    public void build(final AbstractToolkit toolkit, final EmissiveTriangle[] emitters) {
        for (int index = 0; index < 32; index++) {
            ParticleSystem.ACTIVE_PRODUCERS[index] = false;
        }
        final Iterator<ParticleProducer> producerIt = this.particleProducers.iterator();
        PositionUpdate: while (producerIt.hasNext()) {
            final ParticleProducer producer = producerIt.next();
            if (emitters != null) {
                for (int index = 0; index < emitters.length; index++) {
                    if (emitters[index] == producer.particleEmitter || emitters[index].child == producer.particleEmitter) {
                        ParticleSystem.ACTIVE_PRODUCERS[index] = true;
                        producer.updatePosition();
                        producer.requiresPositionUpdate = false;
                        continue PositionUpdate;
                    }
                }
            }
            if (producer.particleCount == 0) {
                producerIt.remove();
                this.particleProducerCount--;
            }
            else {
                producer.requiresPositionUpdate = true;
            }
        }
        if (emitters != null) {
            for (int index = 0; index < emitters.length && index != 32 && this.particleProducerCount != 32; index++) {
                if (!ParticleSystem.ACTIVE_PRODUCERS[index]) {
                    final ParticleProducer producer = new ParticleProducer(toolkit, emitters[index], this, this.currentCycle);
                    this.particleProducers.add(producer);
                    this.particleProducerCount++;
                    ParticleSystem.ACTIVE_PRODUCERS[index] = true;
                }
            }
        }
    }
}
