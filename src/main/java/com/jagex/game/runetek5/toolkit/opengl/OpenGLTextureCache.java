package com.jagex.game.runetek5.toolkit.opengl;

import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

public class OpenGLTextureCache {

    public final LRUCache textureCache = new LRUCache(256);
    public final OpenGlToolkit toolkit;
    public final MaterialProvider materialProvider;

    public OpenGLTextureCache(OpenGlToolkit toolkit, MaterialProvider materialProvider) {
        this.toolkit = toolkit;
        this.materialProvider = materialProvider;
    }

    public OpenGl2DTexture getTexture(int textureId) {
        int textureSize = -1;
        Object bloomGl = this.textureCache.get(textureId);
        if (bloomGl != null) {
            return (OpenGl2DTexture) bloomGl;
        } 
        if (!this.materialProvider.isCached(textureId)) {
            return null;
        } 
        MaterialType material = this.materialProvider.getMaterial(textureId);
        textureSize = material.isSmall ? 64 : this.toolkit.currentTextureSize;

        OpenGl2DTexture texture;
        if (material.hdr && this.toolkit.bloomEnabled()) {
            float[] pixels = this.materialProvider.getPixelsHdr(textureId, textureSize, textureSize);
            texture = new OpenGl2DTexture(this.toolkit, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB, textureSize, textureSize, material.mipmapping != 0, pixels, GLFormat.HDR);
        } else {
            int[] pixels;
            if (material.effectCombiner != 2 && MaterialTypeList.isOpaqueEffect(material.effectId)) {
                pixels = this.materialProvider.getPixels(textureId, 0.7F, textureSize, textureSize, true);
            } else {
                pixels = this.materialProvider.getPixelsArgb(textureId, textureSize, textureSize);
            }

            texture = new OpenGl2DTexture(this.toolkit, GL_TEXTURE_2D, textureSize, textureSize, material.mipmapping != 0, pixels, 0, 0, false);
        }

        texture.setRepeat(material.repeatS, material.repeatT);
        this.textureCache.put(texture, textureId);
        return texture;
    }

    public OpenGl2DTexture get(int textureId) {
        return getTexture(textureId);
    }

    public void cycleBloomCache() {
        this.textureCache.clean(5);
    }

    public void resetBloomCache() {
        this.textureCache.reset();
    }
}
