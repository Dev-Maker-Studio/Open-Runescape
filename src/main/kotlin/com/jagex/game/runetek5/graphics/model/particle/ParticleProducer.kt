package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.config.particle.ParticleProducerType
import com.jagex.game.runetek5.entity.EntityNodeList
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.Utils
import kotlin.math.atan2
import kotlin.math.sqrt

class ParticleProducer(
    renderer: AbstractToolkit,
    particleEmitter: EmissiveTriangle,
    parent: ParticleSystem,
    @JvmField val startTime: Long
) {

    @JvmField
    var particleCount: Int = 0

    @JvmField
    var movementDirectionX: Int = 0

    @JvmField
    var movementDirectionY: Int = 0

    @JvmField
    var movementDirectionZ: Int = 0

    @JvmField
    var particleTriangle: ParticleTriangle = ParticleTriangle()

    @JvmField
    var current: ParticleTriangle = ParticleTriangle()

    @JvmField
    var randomisedParticleRate: Int = 0

    @JvmField
    var randomisedRotationOffsetX: Int = 0

    @JvmField
    var baseRotationX: Int = 0

    @JvmField
    var randomisedRotationOffsetY: Int = 0

    @JvmField
    var baseRotationY: Int = 0

    @JvmField
    var positionUpdated: Boolean = false

    @JvmField
    var unmoved: Boolean = false

    @JvmField
    var requiresPositionUpdate: Boolean = false

    @JvmField
    val particleEmitter: EmissiveTriangle

    @JvmField
    val parent: ParticleSystem

    @JvmField
    var definition: ParticleProducerType

    @JvmField
    val particles: EntityNodeList<Particle>

    init {
        this.particleEmitter = particleEmitter
        this.parent = parent
        this.definition = this.particleEmitter.getProducer()
        if (!renderer.hasTextures() && this.definition.nonTexturedProducerId != -1) {
            this.definition = ParticleProducerType.list(this.definition.nonTexturedProducerId)
        }
        this.particles = EntityNodeList<Particle>()
        this.randomisedParticleRate = (this.randomisedParticleRate + Math.random() * 64.0).toInt()
        this.updatePosition()
        this.particleTriangle.verticeXA = this.current.verticeXA
        this.particleTriangle.verticeYA = this.current.verticeYA
        this.particleTriangle.verticeZA = this.current.verticeZA
        this.particleTriangle.verticeXB = this.current.verticeXB
        this.particleTriangle.verticeYB = this.current.verticeYB
        this.particleTriangle.verticeZB = this.current.verticeZB
        this.particleTriangle.verticeXC = this.current.verticeXC
        this.particleTriangle.verticeYC = this.current.verticeYC
        this.particleTriangle.verticeZC = this.current.verticeZC
    }

    fun performUpdate(toolkit: AbstractToolkit, currentTime: Long, delta: Int, resize: Boolean) {
        var canResize = resize
        if (this.requiresPositionUpdate) {
            canResize = false
        } else if (ParticleSystemManager.detailsSetting < this.definition.minimumSetting) {
            canResize = false
        } else if (ParticleSystemManager.lastRenderedParticleCount > MAX_PARTICLE_BY_SETTING[ParticleSystemManager.detailsSetting]) {
            canResize = false
        } else if (this.unmoved) {
            canResize = false
        } else if (this.definition.lifetime != -1) {
            var timeAlive = (currentTime - this.startTime).toInt()
            if (!this.definition.periodic && timeAlive > this.definition.lifetime) {
                canResize = false
            } else {
                timeAlive %= this.definition.lifetime
            }
            if (!this.definition.activeFirst && timeAlive < this.definition.anInt537) {
                canResize = false
            }
            if (this.definition.activeFirst && timeAlive >= this.definition.anInt537) {
                canResize = false
            }
        }
        this.particleCount = 0
        var particle = this.particles.getFirst() as Particle?
        while (particle != null) {
            particle.visualUpdate(delta)
            this.particleCount++
            particle = this.particles.getNext() as Particle?
        }
        if (canResize) {
            val centerX = (this.current.verticeXC + this.current.verticeXB + this.current.verticeXA) / 3
            val centerY = (this.current.verticeYA + this.current.verticeYB + this.current.verticeYC) / 3
            val centerZ = (this.current.verticeZA + this.current.verticeZB + this.current.verticeZC) / 3
            if (this.current.centerX != centerX || this.current.centerY != centerY || this.current.centerZ != centerZ || !this.positionUpdated) {
                this.current.centerX = centerX
                this.current.centerY = centerY
                this.current.centerZ = centerZ
                val deltaXBa = this.current.verticeXB - this.current.verticeXA
                val deltaYBa = this.current.verticeYB - this.current.verticeYA
                val deltaZBa = this.current.verticeZB - this.current.verticeZA
                val deltaXCa = this.current.verticeXC - this.current.verticeXA
                val deltaYCa = this.current.verticeYC - this.current.verticeYA
                val deltaZCa = this.current.verticeZC - this.current.verticeZA
                this.movementDirectionX = deltaYBa * deltaZCa - deltaZBa * deltaYCa
                this.movementDirectionY = deltaZBa * deltaXCa - deltaXBa * deltaZCa
                this.movementDirectionZ = deltaXBa * deltaYCa - deltaXCa * deltaYBa

                while (true) {
                    if (this.movementDirectionX <= 32767 && this.movementDirectionY <= 32767 && this.movementDirectionZ <= 32767 && this.movementDirectionX >= -32767 && this.movementDirectionY >= -32767 && this.movementDirectionZ >= -32767) {
                        var divider = sqrt((this.movementDirectionZ * this.movementDirectionZ + this.movementDirectionY * this.movementDirectionY + this.movementDirectionX * this.movementDirectionX).toDouble()).toInt()
                        if (divider <= 0) {
                            divider = 1
                        }
                        this.movementDirectionX = this.movementDirectionX * 32767 / divider
                        this.movementDirectionY = this.movementDirectionY * 32767 / divider
                        this.movementDirectionZ = this.movementDirectionZ * 32767 / divider
                        if (this.definition.maximalAngleH > 0 || this.definition.maximalAngleV > 0) {
                            val thetaH = (atan2(this.movementDirectionZ.toDouble(), this.movementDirectionX.toDouble()) * 2607.5945876176133).toInt()
                            val thetaV = (atan2(this.movementDirectionY.toDouble(), sqrt((this.movementDirectionX * this.movementDirectionX + this.movementDirectionZ * this.movementDirectionZ).toDouble())) * 2607.5945876176133).toInt()
                            this.randomisedRotationOffsetX = this.definition.maximalAngleH.toInt() - this.definition.minimalAngleH.toInt()
                            this.baseRotationX = this.definition.minimalAngleH + thetaH - (this.randomisedRotationOffsetX shr 1)
                            this.randomisedRotationOffsetY = this.definition.maximalAngleV.toInt() - this.definition.minimumAngleV.toInt()
                            this.baseRotationY = this.definition.minimumAngleV + thetaV - (this.randomisedRotationOffsetY shr 1)
                        }

                        this.positionUpdated = true
                        break
                    }
                    this.movementDirectionX = this.movementDirectionX shr 1
                    this.movementDirectionY = this.movementDirectionY shr 1
                    this.movementDirectionZ = this.movementDirectionZ shr 1
                }
            }

            this.randomisedParticleRate += (delta * (this.definition.minimumParticleRate + Math.random() * (this.definition.maximumParticleRate - this.definition.minimumParticleRate))).toInt()
            if (this.randomisedParticleRate > 63) {
                val cycleParticleCount = this.randomisedParticleRate shr 6
                this.randomisedParticleRate = this.randomisedParticleRate and 0x3f
                val speedTowardsVisualUpdate = (delta shl 8) / cycleParticleCount
                var visualUpdateDelta = 0

                var index = 0
                while (index < cycleParticleCount) {
                    val dirX: Int
                    val dirY: Int
                    val dirZ: Int
                    if (this.definition.maximalAngleH <= 0 && this.definition.maximalAngleV <= 0) {
                        dirX = this.movementDirectionX
                        dirY = this.movementDirectionY
                        dirZ = this.movementDirectionZ
                    } else {
                        var rotationX = this.baseRotationX + (this.randomisedRotationOffsetX * Math.random()).toInt()
                        rotationX = rotationX and 0x3fff
                        val xanX = MathUtils.SINE[rotationX]
                        val zanX = MathUtils.COSINE[rotationX]
                        var rotationY = this.baseRotationY + (this.randomisedRotationOffsetY * Math.random()).toInt()
                        rotationY = rotationY and 0x1fff
                        val xanY = MathUtils.SINE[rotationY]
                        val zanY = MathUtils.COSINE[rotationY]
                        val constant: Byte = 13
                        dirX = zanX * xanY shr constant.toInt()
                        dirY = (zanY shl 1) * -1
                        dirZ = xanY * xanX shr constant.toInt()
                    }

                    var randomA = Math.random().toFloat()
                    var randomB = Math.random().toFloat()
                    if (randomA + randomB > 1.0f) {
                        randomA = 1.0f - randomA
                        randomB = 1.0f - randomB
                    }

                    val randomOffset = 1.0f - (randomA + randomB)
                    val x = (randomB * this.current.verticeXB + this.current.verticeXA * randomA + this.current.verticeXC * randomOffset).toInt()
                    val y = (randomB * this.current.verticeYB + this.current.verticeYA * randomA + this.current.verticeYC * randomOffset).toInt()
                    val z = (randomOffset * this.current.verticeZC + this.current.verticeZA * randomA + this.current.verticeZB * randomB).toInt()
                    val speed = this.definition.minimumSpeed + (Math.random() * (this.definition.maximumSpeed - this.definition.minimumSpeed)).toInt()
                    val lifetime = this.definition.minimumLifetime + (Math.random() * (this.definition.maximumLifetime - this.definition.minimumLifetime)).toInt()
                    val size = this.definition.minimumSize + (Math.random() * (this.definition.maximumSize - this.definition.minimumSize)).toInt()
                    val color: Int
                    if (this.definition.uniformColorVariance) {
                        val random = Math.random()
                        color = (random * this.definition.redVariance + this.definition.minimumStartR).toInt() shl 16 or ((this.definition.minimumStartG + random * this.definition.greenVariance).toInt() shl 8) or (this.definition.minimumStartB + random * this.definition.blueVariance).toInt() or ((this.definition.minimumStartA + Math.random() * this.definition.alphaVariance).toInt() shl 24)
                    } else {
                        color = (this.definition.minimumStartR + Math.random() * this.definition.redVariance).toInt() shl 16 or ((this.definition.minimumStartG + Math.random() * this.definition.greenVariance).toInt() shl 8) or (this.definition.minimumStartB + Math.random() * this.definition.blueVariance).toInt() or ((this.definition.minimumStartA + Math.random() * this.definition.alphaVariance).toInt() shl 24)
                    }

                    var materialId = this.definition.materialId
                    if (!toolkit.hasTextures() && !this.definition.isTextured) {
                        materialId = -1
                    }

                    val particleObj: Particle
                    if (ParticleSystemManager.killIndex != ParticleSystemManager.particleCacheCursorPos) {
                        particleObj = ParticleSystemManager.particleCache!![ParticleSystemManager.particleCacheCursorPos]!!
                        ParticleSystemManager.particleCacheCursorPos = ParticleSystemManager.particleCacheCursorPos + 1 and 0x3ff
                        particleObj.reset(this, x, y, z, dirX, dirY, dirZ, speed, lifetime, color, size, materialId, this.definition.adjustsLightIntensity)
                    } else {
                        particleObj = Particle(this, x, y, z, dirX, dirY, dirZ, speed, lifetime, color, size, materialId, this.definition.adjustsLightIntensity)
                    }

                    if (visualUpdateDelta > 256) {
                        particleObj.visualUpdate(visualUpdateDelta shr 8)
                    }

                    this.particleCount++
                    visualUpdateDelta += speedTowardsVisualUpdate
                    index++
                }
            }
        }

        if (!this.current.equals(this.particleTriangle)) {
            val triangle = this.particleTriangle
            this.particleTriangle = this.current
            this.current = triangle
            this.current.verticeXA = this.particleEmitter.verticeXA
            this.current.verticeYA = this.particleEmitter.verticeYA
            this.current.verticeZA = this.particleEmitter.verticeZA
            this.current.verticeXB = this.particleEmitter.verticeXB
            this.current.verticeYB = this.particleEmitter.verticeYB
            this.current.verticeZB = this.particleEmitter.verticeZB
            this.current.verticeXC = this.particleEmitter.verticeXC
            this.current.verticeYC = this.particleEmitter.verticeYC
            this.current.verticeZC = this.particleEmitter.verticeZC
            this.current.centerX = this.particleTriangle.centerX
            this.current.centerY = this.particleTriangle.centerY
            this.current.centerZ = this.particleTriangle.centerZ
        }

        ParticleSystemManager.renderedParticleCount += this.particleCount
    }

    fun updatePosition() {
        this.current.verticeXA = this.particleEmitter.verticeXA
        this.current.verticeYA = this.particleEmitter.verticeYA
        this.current.verticeZA = this.particleEmitter.verticeZA
        this.current.verticeXB = this.particleEmitter.verticeXB
        this.current.verticeYB = this.particleEmitter.verticeYB
        this.current.verticeZB = this.particleEmitter.verticeZB
        this.current.verticeXC = this.particleEmitter.verticeXC
        this.current.verticeYC = this.particleEmitter.verticeYC
        this.current.verticeZC = this.particleEmitter.verticeZC
        if (this.current.verticeXB == this.current.verticeXA && this.current.verticeXB == this.current.verticeXC && this.current.verticeYA == this.current.verticeYB && this.current.verticeYB == this.current.verticeYC && this.current.verticeZB == this.current.verticeZA && this.current.verticeZB == this.current.verticeZC) {
            this.unmoved = true
        } else if (this.unmoved) {
            this.unmoved = false
            this.particleTriangle.verticeXA = this.current.verticeXA
            this.particleTriangle.verticeYA = this.current.verticeYA
            this.particleTriangle.verticeZA = this.current.verticeZA
            this.particleTriangle.verticeXB = this.current.verticeXB
            this.particleTriangle.verticeYB = this.current.verticeYB
            this.particleTriangle.verticeZB = this.current.verticeZB
            this.particleTriangle.verticeXC = this.current.verticeXC
            this.particleTriangle.verticeYC = this.current.verticeYC
            this.particleTriangle.verticeZC = this.current.verticeZC
        }
    }

    fun processCycle(scene: Scene, renderer: AbstractToolkit) {
        var particle = this.particles.getFirst() as Particle?
        while (particle != null) {
            particle.processCycle(scene, renderer)
            particle = this.particles.getNext() as Particle?
        }
    }

    companion object {
        @JvmField
        val MAX_PARTICLE_BY_SETTING = intArrayOf(0x7ff, 0x3fff, 0xffff)

        @JvmStatic
        fun cycle(renderer: AbstractToolkit, cycles: Long) {
            ParticleSystemManager.lastRenderedParticleCount = ParticleSystemManager.renderedParticleCount
            ParticleSystemManager.renderedParticleCount = 0
            Utils.time()
            val particleIt = ParticleSystemManager.activeParticleSystems!!.iterator()
            while (particleIt.hasNext()) {
                val particle = particleIt.next()
                val isAlive = particle.performUpdate(renderer, cycles)
                if (!isAlive) {
                    particleIt.remove()
                    ParticleSystemManager.particleSystemsAwaitingRemoval!![ParticleSystemManager.awaitingRemovalCursorPosition] = particle
                    ParticleSystemManager.awaitingRemovalCursorPosition = ParticleSystemManager.awaitingRemovalCursorPosition + 1 and ParticleSystemManager.anIntArray931[ParticleSystemManager.detailsSetting]
                }
            }
        }
    }
}
