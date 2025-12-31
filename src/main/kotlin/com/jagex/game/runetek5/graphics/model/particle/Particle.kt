package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.config.particle.ParticleProducerType
import com.jagex.game.runetek5.config.particle.ParticleType
import com.jagex.game.runetek5.config.particle.ParticleTypeList
import com.jagex.game.runetek5.entity.InteractiveEntityContainer
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.tile.Tile
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class Particle(
    var producer: ParticleProducer,
    x: Int,
    y: Int,
    z: Int,
    directionX: Int,
    directionY: Int,
    directionZ: Int,
    speed: Int,
    lifetime: Int,
    color: Int,
    size: Int,
    materialId: Int,
    lightIntensityAdjust: Boolean
) : PointEntity() {

    var producerParentSize: Short = 0
    var lifetime: Short = 0
    var lifespan: Short = 0
    var directionX: Short = 0
    var directionY: Short = 0
    var directionZ: Short = 0
    var speed: Int = 0
    var intermediateColor: Int = 0

    init {
        this.producer = producer
        this.x = x shl 12
        this.y = y shl 12
        this.z = z shl 12
        this.colorArgb = color
        this.lifespan = lifetime.toShort()
        this.lifetime = this.lifespan
        this.size = size
        this.materialId = materialId
        this.lightIntensityAdjustment = lightIntensityAdjust
        this.directionX = directionX.toShort()
        this.directionY = directionY.toShort()
        this.directionZ = directionZ.toShort()
        this.speed = speed
        this.priority = this.producer.particleEmitter.priority
        this.link()
    }

    fun link() {
        val index = this.producer.parent.size
        if (this.producer.parent.particles[index] != null) {
            this.producer.parent.particles[index]!!.kill()
        }

        this.producer.parent.particles[index] = this
        this.producerParentSize = this.producer.parent.size.toShort()
        this.producer.parent.size = index + 1 and 0x1fff
        this.producer.particles.offer(this)
    }

    fun reset(
        producer: ParticleProducer,
        x: Int,
        y: Int,
        z: Int,
        directionX: Int,
        directionY: Int,
        directionZ: Int,
        speed: Int,
        life: Int,
        color: Int,
        size: Int,
        materialId: Int,
        lightIntensityAdjustment: Boolean
    ) {
        this.producer = producer
        this.x = x shl 12
        this.y = y shl 12
        this.z = z shl 12
        this.colorArgb = color
        this.lifespan = life.toShort()
        this.lifetime = this.lifespan
        this.size = size
        this.materialId = materialId
        this.lightIntensityAdjustment = lightIntensityAdjustment
        this.directionX = directionX.toShort()
        this.directionY = directionY.toShort()
        this.directionZ = directionZ.toShort()
        this.speed = speed
        this.priority = this.producer.particleEmitter.priority
        this.link()
    }

    fun kill() {
        this.producer.parent.particles!![this.producerParentSize.toInt()] = null
        ParticleSystemManager.particleCache!![ParticleSystemManager.killIndex] = this
        ParticleSystemManager.killIndex = ParticleSystemManager.killIndex + 1 and 0x3ff
        unlinkNode()
        unlinkTree()
    }

    fun processCycle(scene: Scene, toolkit: AbstractToolkit) {
        val sceneX = super.x shr scene.tileUnits + 12
        val sceneY = super.z shr scene.tileUnits + 12
        val posY = super.y shr 12
        if (posY <= 0 && posY >= -262144 && sceneX >= 0 && sceneX < scene.sceneDimensionX && sceneY >= 0 && sceneY < scene.sceneDimensionY) {
            val particleSystem = this.producer.parent
            val producerType = this.producer.definition
            val grounds = scene.localHeightmap
            var plane = particleSystem.displayPlane.toInt()
            var tile = scene.tiles[particleSystem.displayPlane.toInt()][sceneX][sceneY]
            if (tile != null) {
                plane = tile.plane.toInt()
            }

            val heightCurrentLvl = grounds[plane]!!.getHeight(sceneX, sceneY)
            val heightNextLvl: Int
            if (plane < scene.regionSizeZ - 1) {
                heightNextLvl = grounds[plane + 1]!!.getHeight(sceneX, sceneY)
            } else {
                heightNextLvl = heightCurrentLvl - (8 shl scene.tileUnits)
            }

            if (producerType.adjustsToScene) {
                if (producerType.lowestDisplayPlane == -1 && posY > heightCurrentLvl) {
                    kill()
                    return
                }
                if (producerType.lowestDisplayPlane >= 0 && posY > grounds[producerType.lowestDisplayPlane]!!.getHeight(sceneX, sceneY)) {
                    kill()
                    return
                }
                if (producerType.highestDisplayPlane == -1 && posY < heightNextLvl) {
                    kill()
                    return
                }
                if (producerType.highestDisplayPlane >= 0 && posY < grounds[producerType.highestDisplayPlane + 1]!!.getHeight(sceneX, sceneY)) {
                    kill()
                    return
                }
            }
            var lastRenderedPlane = scene.regionSizeZ - 1
            while (lastRenderedPlane > 0 && posY > grounds[lastRenderedPlane]!!.getHeight(sceneX, sceneY)) {
                lastRenderedPlane--
            }
            if (producerType.aBool541 && lastRenderedPlane == 0 && posY > grounds[0]!!.getHeight(sceneX, sceneY)) {
                kill()
            } else if (lastRenderedPlane == scene.regionSizeZ - 1 && grounds[lastRenderedPlane]!!.getHeight(sceneX, sceneY) - posY > 8 shl scene.tileUnits) {
                kill()
            } else {
                tile = scene.tiles[lastRenderedPlane][sceneX][sceneY]
                if (tile == null) {
                    if (lastRenderedPlane == 0 || scene.tiles[0][sceneX][sceneY] == null) {
                        tile = Tile(0)
                        scene.tiles[0][sceneX][sceneY] = tile
                    }
                    val incrementPlane = scene.tiles[0][sceneX][sceneY]!!.bridge != null
                    if (lastRenderedPlane == 3 && incrementPlane) {
                        kill()
                        return
                    }
                    for (h in 1..lastRenderedPlane) {
                        if (scene.tiles[h][sceneX][sceneY] == null) {
                            tile = Tile(h)
                            scene.tiles[h][sceneX][sceneY] = tile
                            if (incrementPlane) {
                                tile.plane++
                            }
                        }
                    }
                }
                if (producerType.killOverlapping) {
                    val posX = super.x shr 12
                    val posZ = super.z shr 12
                    if (tile!!.primaryWall != null) {
                        val boundingBox = tile.primaryWall!!.getAxisAlignedBoundingBox(toolkit)
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill()
                            return
                        }
                    }
                    if (tile.secondaryWall != null) {
                        val boundingBox = tile.secondaryWall!!.getAxisAlignedBoundingBox(toolkit)
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill()
                            return
                        }
                    }
                    if (tile.groundDecoration != null) {
                        val boundingBox = tile.groundDecoration!!.getAxisAlignedBoundingBox(toolkit)
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill()
                            return
                        }
                    }
                    var interactives: InteractiveEntityContainer? = tile.interactives
                    while (interactives != null) {
                        val boundingBox = interactives.interactiveObject!!.getAxisAlignedBoundingBox(toolkit)
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill()
                            return
                        }
                        interactives = interactives.child
                    }
                }
                particleSystem.wrapper.particleBinaryTree.insert(this)
            }
        } else {
            kill()
        }
    }

    fun visualUpdate(delta: Int) {
        this.lifetime = (this.lifetime - delta).toShort()
        if (this.lifetime <= 0) {
            kill()
        } else {
            val tileX = super.x shr 12
            val tileY = super.y shr 12
            val tileZ = super.z shr 12
            val system = this.producer.parent
            val definition = this.producer.definition
            if (definition.faceColorRgb != 0) {
                if (this.lifespan - this.lifetime <= definition.colorFadeStart) {
                    var red = delta * definition.redFadeStep + (this.colorArgb shr 8 and 0xff00) + (this.intermediateColor shr 16 and 0xff)
                    var green = delta * definition.greenFadeStep + (this.colorArgb and 0xff00) + (this.intermediateColor shr 8 and 0xff)
                    var blue = delta * definition.blueFadeStep + (this.colorArgb shl 8 and 0xff00) + (this.intermediateColor and 0xff)
                    if (red < 0) {
                        red = 0
                    } else if (red > 0xffff) {
                        red = 0xffff
                    }

                    if (green < 0) {
                        green = 0
                    } else if (green > 0xffff) {
                        green = 0xffff
                    }

                    if (blue < 0) {
                        blue = 0
                    } else if (blue > 0xffff) {
                        blue = 0xffff
                    }

                    this.colorArgb = this.colorArgb and 0xffffff.inv()
                    this.colorArgb = this.colorArgb or ((green and 0xff00) + ((blue and 0xff00) shr 8) + ((red and 0xff00) shl 8))
                    this.intermediateColor = this.intermediateColor and 0xffffff.inv()
                    this.intermediateColor = this.intermediateColor or ((blue and 0xff) + ((red and 0xff) shl 16) + ((green and 0xff) shl 8))
                }

                if (this.lifespan - this.lifetime <= definition.alphaFadeStart) {
                    var alpha = delta * definition.alphaFadeStep + (this.intermediateColor shr 24 and 0xff) + (super.colorArgb shr 16 and 0xff00)
                    if (alpha < 0) {
                        alpha = 0
                    } else if (alpha > 0xffff) {
                        alpha = 0xffff
                    }

                    super.colorArgb = super.colorArgb and 0xffffff
                    super.colorArgb = super.colorArgb or ((alpha and 0xff00) shl 16)
                    this.intermediateColor = this.intermediateColor and 0xffffff
                    this.intermediateColor = this.intermediateColor or ((alpha and 0xff) shl 24)
                }
            }

            if (definition.endSpeed != -1 && this.lifespan - this.lifetime <= definition.startSpeedChange) {
                this.speed += definition.speedStep * delta
            }

            if (definition.endSize != -1 && this.lifespan - this.lifetime <= definition.startSizeChange) {
                this.size += definition.sizeChangeStep * delta
            }

            var replacementDirectionX = this.directionX.toDouble()
            var replacementDirectionY = this.directionY.toDouble()
            var replacementDirectionZ = this.directionZ.toDouble()
            var isOffset = false
            if (definition.speedUpdateType == 1) {
                val offX = tileX - this.producer.current.centerX
                val offY = tileY - this.producer.current.centerY
                val offZ = tileZ - this.producer.current.centerZ
                val size3d = (Math.sqrt((offX * offX + offY * offY + offZ * offZ).toDouble())).toInt() shr 2
                val speedFallOff = delta.toLong() * size3d * definition.speedFallOffStep
                this.speed = (this.speed - (this.speed * speedFallOff shr 18)).toInt()
            } else if (definition.speedUpdateType == 2) {
                val offX = tileX - this.producer.current.centerX
                val offY = tileY - this.producer.current.centerY
                val offZ = tileZ - this.producer.current.centerZ
                val size3d = offX * offX + offY * offY + offZ * offZ
                val speedFallOff = delta.toLong() * size3d * definition.speedFallOffStep
                this.speed = (this.speed - (this.speed * speedFallOff shr 28)).toInt()
            }

            var particleNode: EffectiveVerticeEffect? = null
            var particleType: ParticleType? = null
            if (definition.particleFileIds2 != null) {
                val nodeIt = system.effectiveVerticeEffects.iterator()

                var continueLoop = true
                while (continueLoop) {
                    do {
                        if (!nodeIt.hasNext()) {
                            continueLoop = false
                            break
                        }

                        particleNode = nodeIt.next()
                        particleType = particleNode!!.particleType
                    } while (particleType!!.particleHandlingType == 1)

                    if (continueLoop) {
                        var found = false

                        for (i in definition.particleFileIds2!!.indices) {
                            if (particleType!!.partileFileId == definition.particleFileIds2!![i]) {
                                found = true
                                break
                            }
                        }

                        if (found) {
                            val offX = tileX - particleNode!!.verticeX
                            val offY = tileY - particleNode.verticeY
                            val offZ = tileZ - particleNode.verticeZ
                            val size3d = offX * offX + offY * offY + offZ * offZ
                            if (size3d <= particleType!!.uid) {
                                var dimension = Math.sqrt(size3d.toDouble())
                                if (dimension == 0.0) {
                                    dimension = 1.0
                                }

                                val d_30 = (offX * particleNode.defaultMatrixX + offY * particleType!!.offsetY + offZ * particleNode.defaultMatrixZ) * 65535.0 / (particleType.size3d * dimension)
                                if (d_30 >= particleType!!.zan) {
                                    var posOffset = 0.0
                                    if (particleType!!.type == 1) {
                                        posOffset = dimension / 16.0 * particleType!!.sizeMultiplier
                                    } else if (particleType!!.type == 2) {
                                        posOffset = dimension / 16.0 * (dimension / 16.0) * particleType!!.sizeMultiplier
                                    }

                                    if (particleType!!.verticeCalculationType == 0) {
                                        if (particleType!!.currentOffset == 0) {
                                            replacementDirectionX += (particleNode.defaultMatrixX - posOffset) * delta
                                            replacementDirectionY += (particleType!!.offsetY - posOffset) * delta
                                            replacementDirectionZ += (particleNode.defaultMatrixZ - posOffset) * delta
                                            isOffset = true
                                        } else {
                                            this.x = (this.x + (particleNode.defaultMatrixX - posOffset) * delta).toInt()
                                            this.y = (this.y + (particleType!!.offsetY - posOffset) * delta).toInt()
                                            this.z = (this.z + (particleNode.defaultMatrixZ - posOffset) * delta).toInt()
                                        }
                                    } else {
                                        val d_34 = offX / dimension * particleType!!.size3d
                                        val d_36 = offY / dimension * particleType!!.size3d
                                        val d_38 = offZ / dimension * particleType!!.size3d
                                        if (particleType!!.currentOffset == 0) {
                                            replacementDirectionX += d_34 * delta
                                            replacementDirectionY += d_36 * delta
                                            replacementDirectionZ += d_38 * delta
                                            isOffset = true
                                        } else {
                                            this.x = (this.x + d_34 * delta).toInt()
                                            this.y = (this.y + d_36 * delta).toInt()
                                            this.z = (this.z + d_38 * delta).toInt()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (definition.effectiveVertexUids != null) {
                for (index in definition.effectiveVertexUids!!.indices) {
                    particleNode = ParticleSystemManager.activeEffectiveVertexEffects.get(definition.effectiveVertexUids!![index].toLong())

                    while (particleNode != null) {
                        particleType = particleNode.particleType
                        val particleSizeX = tileX - particleNode.verticeX
                        val particleSizeY = tileY - particleNode.verticeY
                        val particleSizeZ = tileZ - particleNode.verticeZ
                        val uid = particleSizeX * particleSizeX + particleSizeY * particleSizeY + particleSizeZ * particleSizeZ
                        if (uid > particleType.uid) {
                            particleNode = ParticleSystemManager.activeEffectiveVertexEffects.nextInBucket()
                        } else {
                            var timesSizeHalved = Math.sqrt(uid.toDouble())
                            if (timesSizeHalved == 0.0) {
                                timesSizeHalved = 1.0
                            }

                            val d_29 = (particleSizeX * particleNode.defaultMatrixX + particleSizeY * particleType.offsetY + particleSizeZ * particleNode.defaultMatrixZ) * 65535.0 / (particleType.size3d * timesSizeHalved)
                            if (d_29 < particleType.zan) {
                                particleNode = ParticleSystemManager.activeEffectiveVertexEffects.nextInBucket()
                            } else {
                                var posOffset = 0.0
                                if (particleType.type == 1) {
                                    posOffset = timesSizeHalved / 16.0 * particleType.sizeMultiplier
                                } else if (particleType.type == 2) {
                                    posOffset = timesSizeHalved / 16.0 * (timesSizeHalved / 16.0) * particleType.sizeMultiplier
                                }

                                if (particleType.verticeCalculationType == 0) {
                                    if (particleType.currentOffset == 0) {
                                        replacementDirectionX += (particleNode.defaultMatrixX - posOffset) * delta
                                        replacementDirectionY += (particleType.offsetY - posOffset) * delta
                                        replacementDirectionZ += (particleNode.defaultMatrixZ - posOffset) * delta
                                        isOffset = true
                                    } else {
                                        this.x = (this.x + (particleNode.defaultMatrixX - posOffset) * delta).toInt()
                                        this.y = (this.y + (particleType.offsetY - posOffset) * delta).toInt()
                                        this.z = (this.z + (particleNode.defaultMatrixZ - posOffset) * delta).toInt()
                                    }
                                } else {
                                    val stepXChange = particleSizeX / timesSizeHalved * particleType.size3d
                                    val stepYChange = particleSizeY / timesSizeHalved * particleType.size3d
                                    val stepZChange = particleSizeZ / timesSizeHalved * particleType.size3d
                                    if (particleType.currentOffset == 0) {
                                        replacementDirectionX += stepXChange * delta
                                        replacementDirectionY += stepYChange * delta
                                        replacementDirectionZ += stepZChange * delta
                                        isOffset = true
                                    } else {
                                        this.x = (this.x + stepXChange * delta).toInt()
                                        this.y = (this.y + stepYChange * delta).toInt()
                                        this.z = (this.z + stepZChange * delta).toInt()
                                    }
                                }

                                particleNode = ParticleSystemManager.activeEffectiveVertexEffects.nextInBucket()
                            }
                        }
                    }
                }
            }

            if (definition.particleFileIds != null) {
                if (definition.copiedParticleFileIds == null) {
                    definition.copiedParticleFileIds = IntArray(definition.particleFileIds!!.size)

                    for (index in definition.particleFileIds!!.indices) {
                        ParticleTypeList.listParticle(definition.particleFileIds!![index])
                        definition.copiedParticleFileIds!![index] = ((ParticleType.particleTypeCache.get(definition.particleFileIds!![index].toLong()) as IntNode).value)
                    }
                }

                for (index in definition.copiedParticleFileIds!!.indices) {
                    val particleCopedType = ParticleType.particleTypes[definition.copiedParticleFileIds!![index]]
                    if (particleCopedType!!.currentOffset == 0) {
                        replacementDirectionX += delta * particleCopedType.offsetX
                        replacementDirectionY += delta * particleCopedType.offsetY
                        replacementDirectionZ += delta * particleCopedType.offsetZ
                        isOffset = true
                    } else {
                        this.x += particleCopedType.offsetX * delta
                        this.y += particleCopedType.offsetY * delta
                        this.z += particleCopedType.offsetZ * delta
                    }
                }
            }

            if (isOffset) {
                while (true) {
                    if (replacementDirectionX <= 32767.0 && replacementDirectionY <= 32767.0 && replacementDirectionZ <= 32767.0 && replacementDirectionX >= -32767.0 && replacementDirectionY >= -32767.0 && replacementDirectionZ >= -32767.0) {
                        this.directionX = replacementDirectionX.toInt().toShort()
                        this.directionY = replacementDirectionY.toInt().toShort()
                        this.directionZ = replacementDirectionZ.toInt().toShort()
                        break
                    }

                    replacementDirectionX /= 2.0
                    replacementDirectionY /= 2.0
                    replacementDirectionZ /= 2.0
                    this.speed = this.speed shl 1
                }
            }

            this.x = (this.x + ((this.directionX.toLong() * (this.speed shl 2) shr 23) * delta)).toInt()
            this.y = (this.y + ((this.directionY.toLong() * (this.speed shl 2) shr 23) * delta)).toInt()
            this.z = (this.z + ((this.directionZ.toLong() * (this.speed shl 2) shr 23) * delta)).toInt()
        }
    }
}
