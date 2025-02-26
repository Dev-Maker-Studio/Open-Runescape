package com.jagex.game.runetek5.config.material;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.CacheableNodeQueue;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.config.material.texture.TextureType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class MaterialTypeList implements MaterialProvider {

    public final CacheableNodeQueue textureTypeCache = new CacheableNodeQueue(256);

    public final Js5ResourceProvider textureResource;

    public final Js5ResourceProvider spriteResource;

    public final int numMaterials;

    public final MaterialType[] textures;

    public MaterialTypeList(Js5ResourceProvider materialResource, Js5ResourceProvider textureResource, Js5ResourceProvider spriteResource) {
        this.textureResource = textureResource;
        this.spriteResource = spriteResource;
        Packet materialBuffer = new Packet(materialResource.getFile(0, 0));
        this.numMaterials = materialBuffer.readUShort();
        this.textures = new MaterialType[this.numMaterials];
        for (int index = 0; index < this.numMaterials; index++) {
            if (materialBuffer.readUByte() == 1) {
                this.textures[index] = new MaterialType();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].detailsOnly = materialBuffer.readUByte() == 0;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].isSmall = materialBuffer.readUByte() == 1;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].skipTriangles = materialBuffer.readUByte() == 1;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].brightness = materialBuffer.readByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].alpha = materialBuffer.readByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].effectId = materialBuffer.readByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].effectParam1 = materialBuffer.readByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].colorHsl = (short) materialBuffer.readUShort();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].textureSpeedU = materialBuffer.readByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].textureSpeedV = materialBuffer.readByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].aBool2087 = materialBuffer.readUByte() == 1;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].isBrickTile = materialBuffer.readUByte() == 1;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].mipmapping = materialBuffer.readByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].repeatS = materialBuffer.readUByte() == 1;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].repeatT = materialBuffer.readUByte() == 1;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].hdr = materialBuffer.readUByte() == 1;
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].combineMode = materialBuffer.readUByte();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].effectParam2 = materialBuffer.readInt();
            }
        }
        for (int index = 0; index < this.numMaterials; index++) {
            if (this.textures[index] != null) {
                this.textures[index].effectCombiner = materialBuffer.readUByte();
            }
        }
    }

    public TextureType getTextureType(int textureId) {
        CacheableNode cachedTextureType = this.textureTypeCache.get(textureId);
        if (cachedTextureType != null) {
            return (TextureType) cachedTextureType;
        } 
        byte[] data = this.textureResource.getFile(textureId);
        if (data == null) {
            return null;
        } 
        TextureType textureType = new TextureType(new Packet(data));
        this.textureTypeCache.put(textureType, textureId);
        return textureType;
    }

    @Override
    public int getSize() {
        return this.numMaterials;
    }

    @Override
    public MaterialType getMaterial(int textureId) {
        return this.textures[textureId];
    }

    @Override
    public void cycle() {
    }

    @Override
    public int[] getPixelsArgb(int textureId, int width, int height) {
        return getTextureType(textureId).getPixelsArgb(this.spriteResource, this, width, height, this.textures[textureId].isBrickTile);
    }

    @Override
    public float[] getPixelsHdr(int textureId, int width, int height) {
        return getTextureType(textureId).getPixelsHdr(this.spriteResource, this, width, height, this.textures[textureId].isBrickTile);
    }

    @Override
    public boolean isCached(int textureId) {
        TextureType textureType = getTextureType(textureId);
        return textureType != null && textureType.isCached(this.spriteResource, this);
    }

    @Override
    public int[] getPixels(int textureId, float brightness, int width, int height, boolean readReversed) {
        return getTextureType(textureId).getPixelsRgb(this.spriteResource, this, brightness, width, height, readReversed, this.textures[textureId].isBrickTile);
    }

	public static boolean isOpaqueEffect(int effectId) {
	    return effectId != 1 && effectId != 7;
	}

}
