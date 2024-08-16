package com.jagex.game.runetek5.config.material;

import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class NativeMaterialCache {

    public final LRUCache cache = new LRUCache(256);
    public final AbstractNativeToolkit toolkit;
    public final MaterialProvider materialProvider;

    public NativeMaterialCache(AbstractNativeToolkit toolkit, MaterialProvider materials) {
        this.toolkit = toolkit;
        this.materialProvider = materials;
    }

    public NativeTextureSampler animateTexture(int materialId) {
        int baseId = -1;
        long hash = baseId << 16 | materialId;
        Object cachedTexture = this.cache.get(hash);
        if (cachedTexture != null) {
            return (NativeTextureSampler) cachedTexture;
        } 
        if (!this.materialProvider.isCached(materialId)) {
            return null;
        } 
        MaterialType material = this.materialProvider.getMaterial(materialId);
        int size = material.isSmall ? 64 : this.toolkit.materialSize;
        NativeTextureSampler texture;
        if (material.hdr && this.toolkit.bloomEnabled()) {
            float[] pixelsHdr = this.materialProvider.getPixelsHdr(materialId, size, size);
            texture = this.toolkit.createHdrTexture(GLFormat.HDR, size, size, pixelsHdr);
        } else {
            int[] pixels;
            if (material.effectCombiner != 2 && MaterialTypeList.isOpaqueEffect(material.effectId)) {
                pixels = this.materialProvider.getPixels(materialId, 0.7F, size, size, true);
            } else {
                pixels = this.materialProvider.getPixelsArgb(materialId, size, size);
            }
            texture = this.toolkit.createTexture(size, size, true, pixels);
        }
        texture.setRepeat(material.repeatS, material.repeatT);
        this.cache.put(texture, hash);
        return texture;
    }

    public void processCycle() {
        this.cache.clean(5);
    }

    public void reset() {
        this.cache.reset();
    }

    public NativeTextureSampler getTextureSampler(int id) {
        return animateTexture(id);
    }

}
