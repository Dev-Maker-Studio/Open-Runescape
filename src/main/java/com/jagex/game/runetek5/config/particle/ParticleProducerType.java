package com.jagex.game.runetek5.config.particle;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.Resource;

public class ParticleProducerType {

    public static final LRUCache particleProducers = new LRUCache(64);
    public int[] copiedParticleFileIds;
    public short minimalAngleH;

    public short maximalAngleH;

    public short minimumAngleV;

    public short maximalAngleV;

    public int minimumSpeed;

    public int maximumSpeed;

    public int speedUpdateType;

    public int speedFallOffStep;

    public int maximumSize;

    public int minimumSize;
    public int minimumLifetime;
    public int maximumLifetime;
    public int minimumParticleRate;
    public int maximumParticleRate;
    public int[] particleFileIds2;
    public int[] particleFileIds;
    public int lowestDisplayPlane = -2;
    public int highestDisplayPlane = -2;
    public int updatesPerCycle;
    public int materialId = -1;
    public int nonTexturedProducerId = -1;
    public int faceColorRgb;
    public boolean activeFirst = true;
    public int anInt537 = -1;
    public int lifetime = -1;
    public int minimumSetting;
    public boolean periodic = true;
    public int endSpeed = -1;
    public boolean uniformColorVariance = true;
    public int[] effectiveVertexUids;
    public boolean aBool572 = true;
    public int endSize = -1;
    public boolean isTextured;
    public boolean adjustsLightIntensity = true;
    public boolean killOverlapping;
    public boolean aBool541 = true;
    public boolean adjustsToScene;
    public int minimumStartR;
    public int redVariance;
    public int minimumStartG;
    public int greenVariance;
    public int minimumStartB;
    public int blueVariance;
    public int minimumStartA;
    public int alphaVariance;
    public int colorFadeStart;
    public int alphaFadeStart;
    public int redFadeStep;
    public int greenFadeStep;
    public int blueFadeStep;
    public int startSpeedChange;
    public int alphaFadeStep;
    public int speedStep;
    public int startSizeChange;
    public int sizeChangeStep;
    public int minimumStartColorRgb;
    public int maximumStartColorRgb;
    public int colorFading = 100;
    public int alphaFading = 100;
    public int speedChange = 100;
    public int sizeChange = 100;
    public int maximumStartR;
    public int maximumStartG;
    public int maximumStartB;
    public int maximumStartA;
    
    public static ParticleProducerType list(int id) {
        ParticleProducerType type = (ParticleProducerType) ParticleProducerType.particleProducers.get(id);
        if (type == null) {
            byte[] producerData = Resource.PARTICLE_PRODUCER.getFile(0, id);
            type = new ParticleProducerType();
            if (producerData != null) {
                type.decode(new Packet(producerData));
            }

            type.build();
            ParticleProducerType.particleProducers.put(type, id);
        }
        return type;
    }

    public static void method3166() {
        particleProducers.reset();
    }

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.minimalAngleH = (short) buffer.readUShort();
            this.maximalAngleH = (short) buffer.readUShort();
            this.minimumAngleV = (short) buffer.readUShort();
            this.maximalAngleV = (short) buffer.readUShort();
            byte scale = 3;
            this.minimalAngleH <<= scale;
            this.maximalAngleH <<= scale;
            this.minimumAngleV <<= scale;
            this.maximalAngleV <<= scale;
        } else if (opcode == 2) {
            buffer.readUByte();
        } else if (opcode == 3) {
            this.minimumSpeed = buffer.readInt();
            this.maximumSpeed = buffer.readInt();
        } else if (opcode == 4) {
            this.speedUpdateType = buffer.readUByte();
            this.speedFallOffStep = buffer.readByte();
        } else if (opcode == 5) {
            this.minimumSize = this.maximumSize = buffer.readUShort() << 12 << 2;
        } else if (opcode == 6) {
            this.minimumStartColorRgb = buffer.readInt();
            this.maximumStartColorRgb = buffer.readInt();
        } else if (opcode == 7) {
            this.minimumLifetime = buffer.readUShort();
            this.maximumLifetime = buffer.readUShort();
        } else if (opcode == 8) {
            this.minimumParticleRate = buffer.readUShort();
            this.maximumParticleRate = buffer.readUShort();
        } else if (opcode == 9) {
            int count = buffer.readUByte();
            this.particleFileIds2 = new int[count];
            for (int i_5 = 0; i_5 < count; i_5++) {
                this.particleFileIds2[i_5] = buffer.readUShort();
            }
        } else if (opcode == 10) {
            int count = buffer.readUByte();
            this.particleFileIds = new int[count];
            for (int i_5 = 0; i_5 < count; i_5++) {
                this.particleFileIds[i_5] = buffer.readUShort();
            }
        } else if (opcode == 12) {
            this.lowestDisplayPlane = buffer.readByte();
        } else if (opcode == 13) {
            this.highestDisplayPlane = buffer.readByte();
        } else if (opcode == 14) {
            this.updatesPerCycle = buffer.readUShort();
        } else if (opcode == 15) {
            this.materialId = buffer.readUShort();
        } else if (opcode == 16) {
            this.activeFirst = buffer.readUByte() == 1;
            this.anInt537 = buffer.readUShort();
            this.lifetime = buffer.readUShort();
            this.periodic = buffer.readUByte() == 1;
        } else if (opcode == 17) {
            this.nonTexturedProducerId = buffer.readUShort();
        } else if (opcode == 18) {
            this.faceColorRgb = buffer.readInt();
        } else if (opcode == 19) {
            this.minimumSetting = buffer.readUByte();
        } else if (opcode == 20) {
            this.colorFading = buffer.readUByte();
        } else if (opcode == 21) {
            this.alphaFading = buffer.readUByte();
        } else if (opcode == 22) {
            this.endSpeed = buffer.readInt();
        } else if (opcode == 23) {
            this.speedChange = buffer.readUByte();
        } else if (opcode == 24) {
            this.uniformColorVariance = false;
        } else if (opcode == 25) {
            int count = buffer.readUByte();
            this.effectiveVertexUids = new int[count];
            for (int i_5 = 0; i_5 < count; i_5++) {
                this.effectiveVertexUids[i_5] = buffer.readUShort();
            }
        } else if (opcode == 26) {
            this.aBool572 = false;
        } else if (opcode == 27) {
            this.endSize = buffer.readUShort() << 12 << 2;
        } else if (opcode == 28) {
            this.sizeChange = buffer.readUByte();
        } else if (opcode == 29) {
            buffer.readShort();
        } else if (opcode == 30) {
            this.isTextured = true;
        } else if (opcode == 31) {
            this.minimumSize = buffer.readUShort() << 12 << 2;
            this.maximumSize = buffer.readUShort() << 12 << 2;
        } else if (opcode == 32) {
            this.adjustsLightIntensity = false;
        } else if (opcode == 33) {
            this.killOverlapping = true;
        } else if (opcode == 34) {
            this.aBool541 = false;
        }
    }

    public void build() {
        if (this.lowestDisplayPlane > -2 || this.highestDisplayPlane > -2) {
            this.adjustsToScene = true;
        }
        this.minimumStartR = this.minimumStartColorRgb >> 16 & 0xff;
        this.maximumStartR = this.maximumStartColorRgb >> 16 & 0xff;
        this.redVariance = this.maximumStartR - this.minimumStartR;
        this.minimumStartG = this.minimumStartColorRgb >> 8 & 0xff;
        this.maximumStartG = this.maximumStartColorRgb >> 8 & 0xff;
        this.greenVariance = this.maximumStartG - this.minimumStartG;
        this.minimumStartB = this.minimumStartColorRgb & 0xff;
        this.maximumStartB = this.maximumStartColorRgb & 0xff;
        this.blueVariance = this.maximumStartB - this.minimumStartB;
        this.minimumStartA = this.minimumStartColorRgb >> 24 & 0xff;
        this.maximumStartA = this.maximumStartColorRgb >> 24 & 0xff;
        this.alphaVariance = this.maximumStartA - this.minimumStartA;
        if (this.faceColorRgb != 0) {
            this.colorFadeStart = this.colorFading * this.maximumLifetime / 100;
            this.alphaFadeStart = this.alphaFading * this.maximumLifetime / 100;
            if (this.colorFadeStart == 0) {
                this.colorFadeStart = 1;
            }
            this.redFadeStep = ((this.faceColorRgb >> 16 & 0xff) - (this.redVariance / 2 + this.minimumStartR) << 8) / this.colorFadeStart;
            this.greenFadeStep = ((this.faceColorRgb >> 8 & 0xff) - (this.greenVariance / 2 + this.minimumStartG) << 8) / this.colorFadeStart;
            this.blueFadeStep = ((this.faceColorRgb & 0xff) - (this.blueVariance / 2 + this.minimumStartB) << 8) / this.colorFadeStart;
            if (this.alphaFadeStart == 0) {
                this.alphaFadeStart = 1;
            }
            this.alphaFadeStep = ((this.faceColorRgb >> 24 & 0xff) - (this.alphaVariance / 2 + this.minimumStartA) << 8) / this.alphaFadeStart;
            this.redFadeStep += this.redFadeStep > 0 ? -4 : 4;
            this.greenFadeStep += this.greenFadeStep > 0 ? -4 : 4;
            this.blueFadeStep += this.blueFadeStep > 0 ? -4 : 4;
            this.alphaFadeStep += this.alphaFadeStep > 0 ? -4 : 4;
        }
        if (this.endSpeed != -1) {
            this.startSpeedChange = this.maximumLifetime * this.speedChange / 100;
            if (this.startSpeedChange == 0) {
                this.startSpeedChange = 1;
            }
            this.speedStep = (this.endSpeed - ((this.maximumSpeed - this.minimumSpeed) / 2 + this.minimumSpeed)) / this.startSpeedChange;
        }
        if (this.endSize != -1) {
            this.startSizeChange = this.sizeChange * this.maximumLifetime / 100;
            if (this.startSizeChange == 0) {
                this.startSizeChange = 1;
            }
            this.sizeChangeStep = (this.endSize - ((this.maximumSize - this.minimumSize) / 2 + this.minimumSize)) / this.startSizeChange;
        }
    }

    public void decode(Packet stream) {
        while (true) {
            int opcode = stream.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(stream, opcode);
        }
    }
}
