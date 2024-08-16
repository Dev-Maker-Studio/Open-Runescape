package com.jagex.game.runetek5.config.particle;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.utils.MathUtils;

//ParticleArchive1Def
public class ParticleType {

    public static final ParticleType[] particleTypes = new ParticleType[16];
    public static final IterableNodeMap particleTypeCache = new IterableNodeMap(16);
    public static int particleType;
    public static final LRUCache particleCache = new LRUCache(64);
    public int partileFileId;
    public int type;
    public int sizeMultiplier;
    public int offsetX;
    public int offsetY;
    public int offsetZ;
    public int particleHandlingType;
    public int verticeCalculationType;
    public int currentOffset;
    public int zan;
    public int size3d;
    public long uid;
    public int rotation;
    public boolean invert;

    public static void method5757() {
        particleCache.reset();
    }

    public void decode(Packet rsbytebuffer1) {
        while (true) {
            int i_3 = rsbytebuffer1.readUnsignedByte();
            if (i_3 == 0) {
                return;
            }
            decode(rsbytebuffer1, i_3);
        }
    }

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.rotation = stream.readUnsignedShort();
        } else if (opcode == 2) {
            stream.readUnsignedByte();
        } else if (opcode == 3) {
            this.offsetX = stream.readInt();
            this.offsetY = stream.readInt();
            this.offsetZ = stream.readInt();
        } else if (opcode == 4) {
            this.sizeMultiplier = stream.readInt();
        } else if (opcode == 6) {
            this.particleHandlingType = stream.readUnsignedByte();
        } else if (opcode == 8) {
            this.currentOffset = 1;
        } else if (opcode == 9) {
            this.verticeCalculationType = 1;
        } else if (opcode == 10) {
        	// NOOP
        }
    }

    public void setDefaults() {
        this.zan = MathUtils.COSINE[this.rotation << 3];
        long x = this.offsetX;
        long y = this.offsetY;
        long z = this.offsetZ;
        this.size3d = (int) Math.sqrt((x * x + y * y + z * z));
        if (this.sizeMultiplier == 0) {
            this.sizeMultiplier = 1;
        }
        if (this.type == 0) {
            this.uid = 2147483647L;
        } else if (this.type == 1) {
            this.uid = this.size3d * 8L / this.sizeMultiplier;
            this.uid *= this.uid;
        } else if (this.type == 2) {
            this.uid = this.size3d * 8L / this.sizeMultiplier;
        }
        if (this.invert) {
            this.size3d *= -1;
        }
    }
}
