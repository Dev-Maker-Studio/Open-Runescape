package com.jagex.game.runetek5.graphics.model.particle;

import java.util.Iterator;

import com.jagex.game.runetek5.config.particle.ParticleProducerType;
import com.jagex.game.runetek5.entity.EntityNodeList;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.utils.Utils;

public class ParticleProducer {
	
    public static final int[] MAX_PARTICLE_BY_SETTING = {0x7ff, 0x3fff, 0xffff};
    int particleCount;
    public int movementDirectionX;
    public int movementDirectionY;
    public int movementDirectionZ;
    public ParticleTriangle particleTriangle = new ParticleTriangle();
    public ParticleTriangle current = new ParticleTriangle();
    public int randomisedParticleRate;
    public int randomisedRotationOffsetX;
    public int baseRotationX;
    public int randomisedRotationOffsetY;
    public int baseRotationY;
    public boolean positionUpdated;
    public boolean unmoved;
    boolean requiresPositionUpdate;
    final EmissiveTriangle particleEmitter;
    final ParticleSystem parent;
    public final long startTime;
    ParticleProducerType definition;
    final EntityNodeList particles;

    ParticleProducer(AbstractToolkit renderer, EmissiveTriangle emitter, ParticleSystem particleSystem, long startTime) {
        this.particleEmitter = emitter;
        this.parent = particleSystem;
        this.startTime = startTime;
        this.definition = this.particleEmitter.getProducer();
        if (!renderer.hasTextures() && this.definition.nonTexturedProducerId != -1) {
        	this.definition = ParticleProducerType.list(this.definition.nonTexturedProducerId);
        }
        this.particles = new EntityNodeList();
        this.randomisedParticleRate = (int) (this.randomisedParticleRate + Math.random() * 64.0D);
        this.updatePosition();
        this.particleTriangle.verticeXA = this.current.verticeXA;
        this.particleTriangle.verticeYA = this.current.verticeYA;
        this.particleTriangle.verticeZA = this.current.verticeZA;
        this.particleTriangle.verticeXB = this.current.verticeXB;
        this.particleTriangle.verticeYB = this.current.verticeYB;
        this.particleTriangle.verticeZB = this.current.verticeZB;
        this.particleTriangle.verticeXC = this.current.verticeXC;
        this.particleTriangle.verticeYC = this.current.verticeYC;
        this.particleTriangle.verticeZC = this.current.verticeZC;
    }

    public static void cycle(AbstractToolkit renderer, long cycles) {
        ParticleSystemManager.lastRenderedParticleCount = ParticleSystemManager.renderedParticleCount;
        ParticleSystemManager.renderedParticleCount = 0;
        Utils.time();
        Iterator<ParticleSystem> particleIt = ParticleSystemManager.activeParticleSystems.iterator();
        while (particleIt.hasNext()) {
            ParticleSystem particle = particleIt.next();
            boolean isAlive = particle.performUpdate(renderer, cycles);
            if (!isAlive) {
                particleIt.remove();
                ParticleSystemManager.particleSystemsAwaitingRemoval[ParticleSystemManager.awaitingRemovalCursorPosition] = particle;
                ParticleSystemManager.awaitingRemovalCursorPosition = ParticleSystemManager.awaitingRemovalCursorPosition + 1 & ParticleSystemManager.anIntArray931[ParticleSystemManager.detailsSetting];
            }
        }
    }

    void performUpdate(AbstractToolkit toolkit, long currentTime, int delta, boolean resize) {
        boolean canResize = resize;
        if (this.requiresPositionUpdate) {
            canResize = false;
        } else if (ParticleSystemManager.detailsSetting < this.definition.minimumSetting) {
            canResize = false;
        } else if (ParticleSystemManager.lastRenderedParticleCount > MAX_PARTICLE_BY_SETTING[ParticleSystemManager.detailsSetting]) {
            canResize = false;
        } else if (this.unmoved) {
            canResize = false;
        } else if (this.definition.lifetime != -1) {
            int timeAlive = (int) (currentTime - this.startTime);
            if (!this.definition.periodic && timeAlive > this.definition.lifetime) {
                canResize = false;
            } else {
                timeAlive %= this.definition.lifetime;
            }
            if (!this.definition.activeFirst && timeAlive < this.definition.anInt537) {
                canResize = false;
            }
            if (this.definition.activeFirst && timeAlive >= this.definition.anInt537) {
                canResize = false;
            }
        }
        this.particleCount = 0;
        for (Particle particle = (Particle) this.particles.getFirst(); particle != null; particle = (Particle) this.particles.getNext()) {
            particle.visualUpdate(delta);
            this.particleCount++;
        }
        if (canResize) {
            int centerX = (this.current.verticeXC + this.current.verticeXB + this.current.verticeXA) / 3;
            int centerY = (this.current.verticeYA + this.current.verticeYB + this.current.verticeYC) / 3;
            int centerZ = (this.current.verticeZA + this.current.verticeZB + this.current.verticeZC) / 3;
            if (this.current.centerX != centerX || this.current.centerY != centerY || this.current.centerZ != centerZ || !this.positionUpdated) {
                this.current.centerX = centerX;
                this.current.centerY = centerY;
                this.current.centerZ = centerZ;
                int deltaXBa = this.current.verticeXB - this.current.verticeXA;
                int deltaYBa = this.current.verticeYB - this.current.verticeYA;
                int deltaZBa = this.current.verticeZB - this.current.verticeZA;
                int deltaXCa = this.current.verticeXC - this.current.verticeXA;
                int deltaYCa = this.current.verticeYC - this.current.verticeYA;
                int deltaZCa = this.current.verticeZC - this.current.verticeZA;
                this.movementDirectionX = deltaYBa * deltaZCa - deltaZBa * deltaYCa;
                this.movementDirectionY = deltaZBa * deltaXCa - deltaXBa * deltaZCa;
                this.movementDirectionZ = deltaXBa * deltaYCa - deltaXCa * deltaYBa;

                while (true) {
                    if (this.movementDirectionX <= 32767 && this.movementDirectionY <= 32767 && this.movementDirectionZ <= 32767 && this.movementDirectionX >= -32767 && this.movementDirectionY >= -32767 && this.movementDirectionZ >= -32767) {
                    	int divider = (int) Math.sqrt(this.movementDirectionZ * this.movementDirectionZ + this.movementDirectionY * this.movementDirectionY + this.movementDirectionX * this.movementDirectionX);
                        if (divider <= 0) {
                            divider = 1;
                        }
                        this.movementDirectionX = this.movementDirectionX * 32767 / divider;
                        this.movementDirectionY = this.movementDirectionY * 32767 / divider;
                        this.movementDirectionZ = this.movementDirectionZ * 32767 / divider;
                        if (this.definition.maximalAngleH > 0 || this.definition.maximalAngleV > 0) {
                        	int thetaH = (int) (Math.atan2(this.movementDirectionZ, this.movementDirectionX) * 2607.5945876176133D);
                        	int thetaV = (int) (Math.atan2(this.movementDirectionY, Math.sqrt(this.movementDirectionX * this.movementDirectionX + this.movementDirectionZ * this.movementDirectionZ)) * 2607.5945876176133D);
                            this.randomisedRotationOffsetX = this.definition.maximalAngleH - this.definition.minimalAngleH;
                            this.baseRotationX = this.definition.minimalAngleH + thetaH - (this.randomisedRotationOffsetX >> 1);
                            this.randomisedRotationOffsetY = this.definition.maximalAngleV - this.definition.minimumAngleV;
                            this.baseRotationY = this.definition.minimumAngleV + thetaV - (this.randomisedRotationOffsetY >> 1);
                        }

                        this.positionUpdated = true;
                        break;
                    }
                    this.movementDirectionX >>= 1;
                    this.movementDirectionY >>= 1;
                    this.movementDirectionZ >>= 1;
                }
            }

            this.randomisedParticleRate += (int) (delta * (this.definition.minimumParticleRate + Math.random() * (this.definition.maximumParticleRate - this.definition.minimumParticleRate)));
            if (this.randomisedParticleRate > 63) {
            	int cycleParticleCount = this.randomisedParticleRate >> 6;
                this.randomisedParticleRate &= 0x3f;
                int speedTowardsVisualUpdate = (delta << 8) / cycleParticleCount;
                int visualUpdateDelta = 0;

                for (int index = 0; index < cycleParticleCount; visualUpdateDelta += speedTowardsVisualUpdate) {
                    int dirX;
                    int dirY;
                    int dirZ;
                    if (this.definition.maximalAngleH <= 0 && this.definition.maximalAngleV <= 0) {
                        dirX = this.movementDirectionX;
                        dirY = this.movementDirectionY;
                        dirZ = this.movementDirectionZ;
                    } else {
                    	int rotationX = this.baseRotationX + (int) (this.randomisedRotationOffsetX * Math.random());
                        rotationX &= 0x3fff;
                        int xanX = MathUtils.SINE[rotationX];
                        int zanX = MathUtils.COSINE[rotationX];
                        int rotationY = this.baseRotationY + (int) (this.randomisedRotationOffsetY * Math.random());
                        rotationY &= 0x1fff;
                        int xanY = MathUtils.SINE[rotationY];
                        int zanY = MathUtils.COSINE[rotationY];
                        byte constant = 13;
                        dirX = zanX * xanY >> constant;
                        dirY = (zanY << 1) * -1;
                        dirZ = xanY * xanX >> constant;
                    }

                    float randomA = (float) Math.random();
                    float randomB = (float) Math.random();
                    if (randomA + randomB > 1.0F) {
                        randomA = 1.0F - randomA;
                        randomB = 1.0F - randomB;
                    }

                    float randomOffset = 1.0F - (randomA + randomB);
                    int x = (int) (randomB * this.current.verticeXB + this.current.verticeXA * randomA + this.current.verticeXC * randomOffset);
                    int y = (int) (randomB * this.current.verticeYB + this.current.verticeYA * randomA + this.current.verticeYC * randomOffset);
                    int z = (int) (randomOffset * this.current.verticeZC + this.current.verticeZA * randomA + this.current.verticeZB * randomB);
                    int speed = this.definition.minimumSpeed + (int) (Math.random() * (this.definition.maximumSpeed - this.definition.minimumSpeed));
                    int lifetime = this.definition.minimumLifetime + (int) (Math.random() * (this.definition.maximumLifetime - this.definition.minimumLifetime));
                    int size = this.definition.minimumSize + (int) (Math.random() * (this.definition.maximumSize - this.definition.minimumSize));
                    int color;
                    if (this.definition.uniformColorVariance) {
                        double random = Math.random();
                        color = (int) (random * this.definition.redVariance + this.definition.minimumStartR) << 16 | (int) (this.definition.minimumStartG + random * this.definition.greenVariance) << 8 | (int) (this.definition.minimumStartB + random * this.definition.blueVariance) | (int) (this.definition.minimumStartA + Math.random() * this.definition.alphaVariance) << 24;
                    } else {
                        color = (int) (this.definition.minimumStartR + Math.random() * this.definition.redVariance) << 16 | (int) (this.definition.minimumStartG + Math.random() * this.definition.greenVariance) << 8 | (int) (this.definition.minimumStartB + Math.random() * this.definition.blueVariance) | (int) (this.definition.minimumStartA + Math.random() * this.definition.alphaVariance) << 24;
                    }

                    int materialId = this.definition.materialId;
                    if (!toolkit.hasTextures() && !this.definition.isTextured) {
                        materialId = -1;
                    }

                    Particle particle;
                    if (ParticleSystemManager.killIndex != ParticleSystemManager.particleCacheCursorPos) {
                        particle = ParticleSystemManager.particleCache[ParticleSystemManager.particleCacheCursorPos];
                        ParticleSystemManager.particleCacheCursorPos = ParticleSystemManager.particleCacheCursorPos + 1 & 0x3ff;
                        particle.reset(this, x, y, z, dirX, dirY, dirZ, speed, lifetime, color, size, materialId, this.definition.adjustsLightIntensity);
                    } else {
                        particle = new Particle(this, x, y, z, dirX, dirY, dirZ, speed, lifetime, color, size, materialId, this.definition.adjustsLightIntensity);
                    }

                    if (visualUpdateDelta > 256) {
                        particle.visualUpdate(visualUpdateDelta >> 8);
                    }

                    this.particleCount++;
                    index++;
                }
            }
        }

        if (!this.current.equals(this.particleTriangle)) {
            ParticleTriangle triangle = this.particleTriangle;
            this.particleTriangle = this.current;
            this.current = triangle;
            this.current.verticeXA = this.particleEmitter.verticeXA;
            this.current.verticeYA = this.particleEmitter.verticeYA;
            this.current.verticeZA = this.particleEmitter.verticeZA;
            this.current.verticeXB = this.particleEmitter.verticeXB;
            this.current.verticeYB = this.particleEmitter.verticeYB;
            this.current.verticeZB = this.particleEmitter.verticeZB;
            this.current.verticeXC = this.particleEmitter.verticeXC;
            this.current.verticeYC = this.particleEmitter.verticeYC;
            this.current.verticeZC = this.particleEmitter.verticeZC;
            this.current.centerX = this.particleTriangle.centerX;
            this.current.centerY = this.particleTriangle.centerY;
            this.current.centerZ = this.particleTriangle.centerZ;
        }

        ParticleSystemManager.renderedParticleCount += this.particleCount;
    }

    void updatePosition() {
        this.current.verticeXA = this.particleEmitter.verticeXA;
        this.current.verticeYA = this.particleEmitter.verticeYA;
        this.current.verticeZA = this.particleEmitter.verticeZA;
        this.current.verticeXB = this.particleEmitter.verticeXB;
        this.current.verticeYB = this.particleEmitter.verticeYB;
        this.current.verticeZB = this.particleEmitter.verticeZB;
        this.current.verticeXC = this.particleEmitter.verticeXC;
        this.current.verticeYC = this.particleEmitter.verticeYC;
        this.current.verticeZC = this.particleEmitter.verticeZC;
        if (this.current.verticeXB == this.current.verticeXA && this.current.verticeXB == this.current.verticeXC && this.current.verticeYA == this.current.verticeYB && this.current.verticeYB == this.current.verticeYC && this.current.verticeZB == this.current.verticeZA && this.current.verticeZB == this.current.verticeZC) {
            this.unmoved = true;
        } else if (this.unmoved) {
            this.unmoved = false;
            this.particleTriangle.verticeXA = this.current.verticeXA;
            this.particleTriangle.verticeYA = this.current.verticeYA;
            this.particleTriangle.verticeZA = this.current.verticeZA;
            this.particleTriangle.verticeXB = this.current.verticeXB;
            this.particleTriangle.verticeYB = this.current.verticeYB;
            this.particleTriangle.verticeZB = this.current.verticeZB;
            this.particleTriangle.verticeXC = this.current.verticeXC;
            this.particleTriangle.verticeYC = this.current.verticeYC;
            this.particleTriangle.verticeZC = this.current.verticeZC;
        }

    }

    void processCycle(Scene scene, AbstractToolkit renderer) {
        for (Particle particle = (Particle) this.particles.getFirst(); particle != null; particle = (Particle) this.particles.getNext()) {
            particle.processCycle(scene, renderer);
        }
    }
}
