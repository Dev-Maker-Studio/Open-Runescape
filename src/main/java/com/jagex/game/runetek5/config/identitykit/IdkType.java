package com.jagex.game.runetek5.config.identitykit;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.graphics.model.Mesh;

public class IdkType {

    public IdkTypeList identityTypeList;
    public int[] bodyModels;
    public short[] recolorSrc;
    public short[] recolorDst;
    public short[] retexSrc;
    public short[] retexDst;
    public final int[] headModels = {-1, -1, -1, -1, -1};

    public Mesh getHeadMesh() {
        Mesh[] meshes = new Mesh[5];
        int meshCount = 0;
        synchronized (this.identityTypeList.modelResource) {
            for (int modelIndex = 0; modelIndex < 5; modelIndex++) {
                if (this.headModels[modelIndex] != -1) {
                    meshes[meshCount++] = Mesh.fromResource(this.identityTypeList.modelResource, this.headModels[modelIndex]);
                }
            }
        }
        for (int meshIndex = 0; meshIndex < 5; meshIndex++) {
            if (meshes[meshIndex] != null && meshes[meshIndex].version < 13) {
                meshes[meshIndex].upscale();
            }
        }
        Mesh mesh = new Mesh(meshes, meshCount);
        if (this.recolorSrc != null) {
            for (int colorIndex = 0; colorIndex < this.recolorSrc.length; colorIndex++) {
                mesh.recolor(this.recolorSrc[colorIndex], this.recolorDst[colorIndex]);
            }
        }
        if (this.retexSrc != null) {
            for (int textureIndex = 0; textureIndex < this.retexSrc.length; textureIndex++) {
                mesh.retexture(this.retexSrc[textureIndex], this.retexDst[textureIndex]);
            }
        }
        return mesh;
    }

    /**
     * Decodes the variables of a single opcode
     */
    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            buffer.readUByte();
        } else if (opcode == 2) {
            int count = buffer.readUByte();
            this.bodyModels = new int[count];
            for (int modelIndex = 0; modelIndex < count; modelIndex++) {
                this.bodyModels[modelIndex] = buffer.readBigSmart();
            }
        } else if (opcode == 40) {
            int count = buffer.readUByte();
            this.recolorSrc = new short[count];
            this.recolorDst = new short[count];
            for (int colorIndex = 0; colorIndex < count; colorIndex++) {
                this.recolorSrc[colorIndex] = (short) buffer.readUShort();
                this.recolorDst[colorIndex] = (short) buffer.readUShort();
            }
        } else if (opcode == 41) {
            int count = buffer.readUByte();
            this.retexSrc = new short[count];
            this.retexDst = new short[count];
            for (int textureIndex = 0; textureIndex < count; textureIndex++) {
                this.retexSrc[textureIndex] = (short) buffer.readUShort();
                this.retexDst[textureIndex] = (short) buffer.readUShort();
            }
        } else if (opcode >= 60 && opcode < 70) {
            this.headModels[opcode - 60] = buffer.readBigSmart();
        }
    }

    public boolean isBodyCached() {
        if (this.bodyModels == null) {
            return true;
        } 
        boolean fullyCached = true;
        synchronized (this.identityTypeList.modelResource) {
            for (int modelIndex = 0; modelIndex < this.bodyModels.length; modelIndex++) {
                if (!this.identityTypeList.modelResource.isCached(this.bodyModels[modelIndex], 0)) {
                    fullyCached = false;
                }
            }
            return fullyCached;
        }
    }

    public Mesh getBodyMesh() {
        if (this.bodyModels == null) {
            return null;
        } 
        Mesh[] meshes = new Mesh[this.bodyModels.length];
        synchronized (this.identityTypeList.modelResource) {
            for (int meshIndex = 0; meshIndex < this.bodyModels.length; meshIndex++) {
                meshes[meshIndex] = Mesh.fromResource(this.identityTypeList.modelResource, this.bodyModels[meshIndex]);
            }
        }
        for (int modelIndex = 0; modelIndex < this.bodyModels.length; modelIndex++) {
            if (meshes[modelIndex].version < 13) {
                meshes[modelIndex].upscale();
            }
        }
        Mesh mesh;
        if (meshes.length == 1) {
            mesh = meshes[0];
        } else {
            mesh = new Mesh(meshes, meshes.length);
        }
        if (mesh == null) {
            return null;
        } 
        if (this.recolorSrc != null) {
            for (int colorIndex = 0; colorIndex < this.recolorSrc.length; colorIndex++) {
                mesh.recolor(this.recolorSrc[colorIndex], this.recolorDst[colorIndex]);
            }
        }
        if (this.retexSrc != null) {
            for (int textureIndex = 0; textureIndex < this.retexSrc.length; textureIndex++) {
                mesh.retexture(this.retexSrc[textureIndex], this.retexDst[textureIndex]);
            }
        }
        return mesh;
    }

    public boolean isHeadCached() {
        boolean fullyCached = true;
        synchronized (this.identityTypeList.modelResource) {
            for (int modelIndex = 0; modelIndex < 5; modelIndex++) {
                if (this.headModels[modelIndex] != -1 && !this.identityTypeList.modelResource.isCached(this.headModels[modelIndex], 0)) {
                    fullyCached = false;
                }
            }
            return fullyCached;
        }
    }

    /**
     * Loads all identity kit variables from the stream
     */
    void decode(Packet stream) {
        while (true) {
            int opcode = stream.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(stream, opcode);
        }
    }
}
