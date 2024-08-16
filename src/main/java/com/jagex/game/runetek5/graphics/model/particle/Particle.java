package com.jagex.game.runetek5.graphics.model.particle;

import java.util.Iterator;

import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.config.particle.ParticleProducerType;
import com.jagex.game.runetek5.config.particle.ParticleType;
import com.jagex.game.runetek5.config.particle.ParticleTypeList;
import com.jagex.game.runetek5.entity.InteractiveEntityContainer;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class Particle extends PointEntity {

    public short producerParentSize;
    public ParticleProducer producer;
    public short lifetime;
    public short lifespan;
    public short directionX;
    public short directionY;
    public short directionZ;
    public int speed;
    public int intermediateColor;

    Particle(ParticleProducer producer, int x, int y, int z, int directionX, int directionY, int directionZ, int speed, int lifetime, int color, int size, int materialId, boolean lightIntensityAdjust) {
        this.producer = producer;
        this.x = x << 12;
        this.y = y << 12;
        this.z = z << 12;
        this.colorArgb = color;
        this.lifespan = this.lifetime = (short) lifetime;
        this.size = size;
        this.materialId = materialId;
        this.lightIntensityAdjustment = lightIntensityAdjust;
        this.directionX = (short) directionX;
        this.directionY = (short) directionY;
        this.directionZ = (short) directionZ;
        this.speed = speed;
        this.priority = this.producer.particleEmitter.priority;
        this.link();
    }

    public void link() {
        int index = this.producer.parent.size;
        if (this.producer.parent.particles[index] != null) {
            this.producer.parent.particles[index].kill();
        }

        this.producer.parent.particles[index] = this;
        this.producerParentSize = (short) this.producer.parent.size;
        this.producer.parent.size = index + 1 & 0x1fff;
        this.producer.particles.offer(this);
    }

    void reset(ParticleProducer producer, int x, int y, int z, int directionX, int directionY, int directionZ, int speed, int life, int color, int size, int materialId, boolean lightIntensityAdjustment) {
        this.producer = producer;
        this.x = x << 12;
        this.y = y << 12;
        this.z = z << 12;
        this.colorArgb = color;
        this.lifespan = this.lifetime = (short) life;
        this.size = size;
        this.materialId = materialId;
        this.lightIntensityAdjustment = lightIntensityAdjustment;
        this.directionX = (short) directionX;
        this.directionY = (short) directionY;
        this.directionZ = (short) directionZ;
        this.speed = speed;
        this.priority = this.producer.particleEmitter.priority;
        this.link();
    }

    void kill() {
        this.producer.parent.particles[this.producerParentSize] = null;
        ParticleSystemManager.particleCache[ParticleSystemManager.killIndex] = this;
        ParticleSystemManager.killIndex = ParticleSystemManager.killIndex + 1 & 0x3ff;
        unlinkNode();
        unlink();
    }

    void processCycle(Scene scene, AbstractToolkit toolkit) {
        int sceneX = super.x >> scene.tileUnits + 12;
        int sceneY = super.z >> scene.tileUnits + 12;
        int posY = super.y >> 12;
        if (posY <= 0 && posY >= -262144 && sceneX >= 0 && sceneX < scene.sceneDimensionX && sceneY >= 0 && sceneY < scene.sceneDimensionY) {
            ParticleSystem particleSystem = this.producer.parent;
            ParticleProducerType producerType = this.producer.definition;
            Ground[] grounds = scene.localHeightmap;
            int plane = particleSystem.displayPlane;
            Tile tile = scene.tiles[particleSystem.displayPlane][sceneX][sceneY];
            if (tile != null) {
                plane = tile.plane;
            }

            int heightCurrentLvl = grounds[plane].getHeight(sceneX, sceneY);
            int heightNextLvl;
            if (plane < scene.regionSizeZ - 1) {
                heightNextLvl = grounds[plane + 1].getHeight(sceneX, sceneY);
            } else {
                heightNextLvl = heightCurrentLvl - (8 << scene.tileUnits);
            }

            if (producerType.adjustsToScene) {
                if (producerType.lowestDisplayPlane == -1 && posY > heightCurrentLvl) {
                    kill();
                    return;
                }
                if (producerType.lowestDisplayPlane >= 0 && posY > grounds[producerType.lowestDisplayPlane].getHeight(sceneX, sceneY)) {
                    kill();
                    return;
                }
                if (producerType.highestDisplayPlane == -1 && posY < heightNextLvl) {
                    kill();
                    return;
                }
                if (producerType.highestDisplayPlane >= 0 && posY < grounds[producerType.highestDisplayPlane + 1].getHeight(sceneX, sceneY)) {
                    kill();
                    return;
                }
            }
            int lastRenderedPlane;
            for (lastRenderedPlane = scene.regionSizeZ - 1; lastRenderedPlane > 0 && posY > grounds[lastRenderedPlane].getHeight(sceneX, sceneY); --lastRenderedPlane) {
            }
            if (producerType.aBool541 && lastRenderedPlane == 0 && posY > grounds[0].getHeight(sceneX, sceneY)) {
                kill();
            } else if (lastRenderedPlane == scene.regionSizeZ - 1 && grounds[lastRenderedPlane].getHeight(sceneX, sceneY) - posY > 8 << scene.tileUnits) {
                kill();
            } else {
                tile = scene.tiles[lastRenderedPlane][sceneX][sceneY];
                if (tile == null) {
                    if (lastRenderedPlane == 0 || scene.tiles[0][sceneX][sceneY] == null) {
                        tile = scene.tiles[0][sceneX][sceneY] = new Tile(0);
                    }
                    boolean incrementPlane = scene.tiles[0][sceneX][sceneY].bridge != null;
                    if (lastRenderedPlane == 3 && incrementPlane) {
                        kill();
                        return;
                    }
                    for (int h = 1; h <= lastRenderedPlane; h++) {
                        if (scene.tiles[h][sceneX][sceneY] == null) {
                            tile = scene.tiles[h][sceneX][sceneY] = new Tile(h);
                            if (incrementPlane) {
                                tile.plane++;
                            }
                        }
                    }
                }
                if (producerType.killOverlapping) {
                    int posX = super.x >> 12;
                    int posZ = super.z >> 12;
                    if (tile.primaryWall != null) {
                    	Silhouette boundingBox = tile.primaryWall.getAxisAlignedBoundingBox(toolkit);
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill();
                            return;
                        }
                    }
                    if (tile.secondaryWall != null) {
                    	Silhouette boundingBox = tile.secondaryWall.getAxisAlignedBoundingBox(toolkit);
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill();
                            return;
                        }
                    }
                    if (tile.groundDecoration != null) {
                    	Silhouette boundingBox = tile.groundDecoration.getAxisAlignedBoundingBox(toolkit);
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill();
                            return;
                        }
                    }
                    for (InteractiveEntityContainer interactives = tile.interactives; interactives != null; interactives = interactives.child) {
                        Silhouette boundingBox = interactives.interactiveObject.getAxisAlignedBoundingBox(toolkit);
                        if (boundingBox != null && boundingBox.testIntersection(posX, posY, posZ)) {
                            kill();
                            return;
                        }
                    }
                }
                particleSystem.wrapper.particleBinaryTree.insert(this);
            }
        } else {
            kill();
        }
    }

    void visualUpdate(int delta) {
        this.lifetime -= delta;
        if (this.lifetime <= 0) {
            kill();
        } else {
            int tileX = super.x >> 12;
            int tileY = super.y >> 12;
            int tileZ = super.z >> 12;
            ParticleSystem system = this.producer.parent;
            ParticleProducerType definition = this.producer.definition;
            if (definition.faceColorRgb != 0) {
                if (this.lifespan - this.lifetime <= definition.colorFadeStart) {
                    int red = delta * definition.redFadeStep + (this.colorArgb >> 8 & 0xff00) + (this.intermediateColor >> 16 & 0xff);
                    int green = delta * definition.greenFadeStep + (this.colorArgb & 0xff00) + (this.intermediateColor >> 8 & 0xff);
                    int blue = delta * definition.blueFadeStep + (this.colorArgb << 8 & 0xff00) + (this.intermediateColor & 0xff);
                    if (red < 0) {
                        red = 0;
                    } else if (red > 0xffff) {
                        red = 0xffff;
                    }

                    if (green < 0) {
                        green = 0;
                    } else if (green > 0xffff) {
                        green = 0xffff;
                    }

                    if (blue < 0) {
                        blue = 0;
                    } else if (blue > 0xffff) {
                        blue = 0xffff;
                    }

                    this.colorArgb &= -16777216;
                    this.colorArgb |= (green & 0xff00) + ((blue & 0xff00) >> 8) + ((red & 0xff00) << 8);
                    this.intermediateColor &= -16777216;
                    this.intermediateColor |= (blue & 0xff) + ((red & 0xff) << 16) + ((green & 0xff) << 8);
                }

                if (this.lifespan - this.lifetime <= definition.alphaFadeStart) {
                    int alpha = delta * definition.alphaFadeStep + (this.intermediateColor >> 24 & 0xff) + (super.colorArgb >> 16 & 0xff00);
                    if (alpha < 0) {
                        alpha = 0;
                    } else if (alpha > 0xffff) {
                        alpha = 0xffff;
                    }

                    super.colorArgb &= 0xffffff;
                    super.colorArgb |= (alpha & 0xff00) << 16;
                    this.intermediateColor &= 0xffffff;
                    this.intermediateColor |= (alpha & 0xff) << 24;
                }
            }

            if (definition.endSpeed != -1 && this.lifespan - this.lifetime <= definition.startSpeedChange) {
                this.speed += definition.speedStep * delta;
            }

            if (definition.endSize != -1 && this.lifespan - this.lifetime <= definition.startSizeChange) {
            	this.size += definition.sizeChangeStep * delta;
            }

            double replacementDirectionX = this.directionX;
            double replacementDirectionY = this.directionY;
            double replacementDirectionZ = this.directionZ;
            boolean isOffset = false;
            if (definition.speedUpdateType == 1) {
            	int offX = tileX - this.producer.current.centerX;
            	int offY = tileY - this.producer.current.centerY;
            	int offZ = tileZ - this.producer.current.centerZ;
                int size3d = (int) Math.sqrt(offX * offX + offY * offY + offZ * offZ) >> 2;
                long speedFallOff = (long) delta * size3d * definition.speedFallOffStep;
                this.speed = (int) (this.speed - (this.speed * speedFallOff >> 18));
            } else if (definition.speedUpdateType == 2) {
            	int offX = tileX - this.producer.current.centerX;
            	int offY = tileY - this.producer.current.centerY;
            	int offZ = tileZ - this.producer.current.centerZ;
                int size3d = offX * offX + offY * offY + offZ * offZ;
                long speedFallOff = (long) delta * size3d * definition.speedFallOffStep;
                this.speed = (int) (this.speed - (this.speed * speedFallOff >> 28));
            }

            EffectiveVerticeEffect particleNode;
            ParticleType particleType;
            if (definition.particleFileIds2 != null) {
                Iterator<EffectiveVerticeEffect> nodeIt = system.effectiveVerticeEffects.iterator();

                label218:
                while (true) {
                    do {
                        if (!nodeIt.hasNext()) {
                            break label218;
                        }

                        particleNode = nodeIt.next();
                        particleType = particleNode.particleType;
                    } while (particleType.particleHandlingType == 1);

                    boolean found = false;

                    for (int i_47 = 0; i_47 < definition.particleFileIds2.length; i_47++) {
                        if (particleType.partileFileId == definition.particleFileIds2[i_47]) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        double offX = tileX - particleNode.verticeX;
                        double offY = tileY - particleNode.verticeY;
                        double offZ = tileZ - particleNode.verticeZ;
                        double size3d = offX * offX + offY * offY + offZ * offZ;
                        if (size3d <= particleType.uid) {
                            double dimension = Math.sqrt(size3d);
                            if (dimension == 0.0D) {
                                dimension = 1.0D;
                            }

                            double d_30 = (offX * particleNode.defaultMatrixX + offY * particleType.offsetY + offZ * particleNode.defaultMatrixZ) * 65535.0D / (particleType.size3d * dimension);
                            if (d_30 >= particleType.zan) {
                                double posOffset = 0.0D;
                                if (particleType.type == 1) {
                                    posOffset = dimension / 16.0D * particleType.sizeMultiplier;
                                } else if (particleType.type == 2) {
                                    posOffset = dimension / 16.0D * (dimension / 16.0D) * particleType.sizeMultiplier;
                                }

                                if (particleType.verticeCalculationType == 0) {
                                    if (particleType.currentOffset == 0) {
                                        replacementDirectionX += (particleNode.defaultMatrixX - posOffset) * delta;
                                        replacementDirectionY += (particleType.offsetY - posOffset) * delta;
                                        replacementDirectionZ += (particleNode.defaultMatrixZ - posOffset) * delta;
                                        isOffset = true;
                                    } else {
                                    	this.x = (int) (this.x + (particleNode.defaultMatrixX - posOffset) * delta);
                                    	this.y = (int) (this.y + (particleType.offsetY - posOffset) * delta);
                                    	this.z = (int) (this.z + (particleNode.defaultMatrixZ - posOffset) * delta);
                                    }
                                } else {
                                    double d_34 = offX / dimension * particleType.size3d;
                                    double d_36 = offY / dimension * particleType.size3d;
                                    double d_38 = offZ / dimension * particleType.size3d;
                                    if (particleType.currentOffset == 0) {
                                        replacementDirectionX += d_34 * delta;
                                        replacementDirectionY += d_36 * delta;
                                        replacementDirectionZ += d_38 * delta;
                                        isOffset = true;
                                    } else {
                                    	this.x = (int) (this.x + d_34 * delta);
                                    	this.y = (int) (this.y + d_36 * delta);
                                    	this.z = (int) (this.z + d_38 * delta);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (definition.effectiveVertexUids != null) {
                for (int index = 0; index < definition.effectiveVertexUids.length; index++) {
                    particleNode = ParticleSystemManager.activeEffectiveVertexEffects.get(definition.effectiveVertexUids[index]);

                    while (particleNode != null) {
                        particleType = particleNode.particleType;
                        double particleSizeX = tileX - particleNode.verticeX;
                        double particleSizeY = tileY - particleNode.verticeY;
                        double particleSizeZ = tileZ - particleNode.verticeZ;
                        double uid = particleSizeX * particleSizeX + particleSizeY * particleSizeY + particleSizeZ * particleSizeZ;
                        if (uid > particleType.uid) {
                            particleNode = ParticleSystemManager.activeEffectiveVertexEffects.nextInBucket();
                        } else {
                            double timesSizeHalved = Math.sqrt(uid);
                            if (timesSizeHalved == 0.0D) {
                                timesSizeHalved = 1.0D;
                            }

                            double d_29 = (particleSizeX * particleNode.defaultMatrixX + particleSizeY * particleType.offsetY + particleSizeZ * particleNode.defaultMatrixZ) * 65535.0D / (particleType.size3d * timesSizeHalved);
                            if (d_29 < particleType.zan) {
                                particleNode = ParticleSystemManager.activeEffectiveVertexEffects.nextInBucket();
                            } else {
                                double posOffset = 0.0D;
                                if (particleType.type == 1) {
                                    posOffset = timesSizeHalved / 16.0D * particleType.sizeMultiplier;
                                } else if (particleType.type == 2) {
                                    posOffset = timesSizeHalved / 16.0D * (timesSizeHalved / 16.0D) * particleType.sizeMultiplier;
                                }

                                if (particleType.verticeCalculationType == 0) {
                                    if (particleType.currentOffset == 0) {
                                        replacementDirectionX += (particleNode.defaultMatrixX - posOffset) * delta;
                                        replacementDirectionY += (particleType.offsetY - posOffset) * delta;
                                        replacementDirectionZ += (particleNode.defaultMatrixZ - posOffset) * delta;
                                        isOffset = true;
                                    } else {
                                    	this.x = (int) (this.x + (particleNode.defaultMatrixX - posOffset) * delta);
                                    	this.y = (int) (this.y + (particleType.offsetY - posOffset) * delta);
                                    	this.z = (int) (this.z + (particleNode.defaultMatrixZ - posOffset) * delta);
                                    }
                                } else {
                                    double stepXChange = particleSizeX / timesSizeHalved * particleType.size3d;
                                    double stepYChange = particleSizeY / timesSizeHalved * particleType.size3d;
                                    double stepZChange = particleSizeZ / timesSizeHalved * particleType.size3d;
                                    if (particleType.currentOffset == 0) {
                                        replacementDirectionX += stepXChange * delta;
                                        replacementDirectionY += stepYChange * delta;
                                        replacementDirectionZ += stepZChange * delta;
                                        isOffset = true;
                                    } else {
                                    	this.x = (int) (this.x + stepXChange * delta);
                                    	this.y = (int) (this.y + stepYChange * delta);
                                    	this.z = (int) (this.z + stepZChange * delta);
                                    }
                                }

                                particleNode = ParticleSystemManager.activeEffectiveVertexEffects.nextInBucket();
                            }
                        }
                    }
                }
            }

            if (definition.particleFileIds != null) {
                if (definition.copiedParticleFileIds == null) {
                    definition.copiedParticleFileIds = new int[definition.particleFileIds.length];

                    for (int index = 0; index < definition.particleFileIds.length; index++) {
                        ParticleTypeList.listParticle(definition.particleFileIds[index]);
                        definition.copiedParticleFileIds[index] = ((IntNode) ParticleType.particleTypeCache.get(definition.particleFileIds[index])).value;
                    }
                }

                for (int index = 0; index < definition.copiedParticleFileIds.length; index++) {
                    ParticleType particleCopedType = ParticleType.particleTypes[definition.copiedParticleFileIds[index]];
                    if (particleCopedType.currentOffset == 0) {
                        replacementDirectionX += delta * particleCopedType.offsetX;
                        replacementDirectionY += delta * particleCopedType.offsetY;
                        replacementDirectionZ += delta * particleCopedType.offsetZ;
                        isOffset = true;
                    } else {
                    	this.x += particleCopedType.offsetX * delta;
                    	this.y += particleCopedType.offsetY * delta;
                    	this.z += particleCopedType.offsetZ * delta;
                    }
                }
            }

            if (isOffset) {
                while (true) {
                    if (replacementDirectionX <= 32767.0D && replacementDirectionY <= 32767.0D && replacementDirectionZ <= 32767.0D && replacementDirectionX >= -32767.0 && replacementDirectionY >= -32767.0 && replacementDirectionZ >= -32767.0) {
                        this.directionX = (short) ((int) replacementDirectionX);
                        this.directionY = (short) ((int) replacementDirectionY);
                        this.directionZ = (short) ((int) replacementDirectionZ);
                        break;
                    }

                    replacementDirectionX /= 2.0D;
                    replacementDirectionY /= 2.0D;
                    replacementDirectionZ /= 2.0D;
                    this.speed <<= 1;
                }
            }

            this.x = (int) (this.x + ((long) this.directionX * (this.speed << 2) >> 23) * delta);
            this.y = (int) (this.y + ((long) this.directionY * (this.speed << 2) >> 23) * delta);
            this.z = (int) (this.z + ((long) this.directionZ * (this.speed << 2) >> 23) * delta);
        }

    }

}
