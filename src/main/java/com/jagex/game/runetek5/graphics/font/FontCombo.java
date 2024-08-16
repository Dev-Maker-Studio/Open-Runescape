package com.jagex.game.runetek5.graphics.font;

import java.util.HashMap;
import java.util.Map;

import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * 
 * @author Brainpower - 13/09/2021
 *
 */
public class FontCombo {

    public Map<Integer, TypeResult> fontMap;
    public final LRUCache cache = new LRUCache(20);
    public final Js5ResourceProvider loadingFontResource;
    public final Js5ResourceProvider fontmetricsResource;
    public final int[] fontFileIds;

    public FontCombo(Js5ResourceProvider index1, Js5ResourceProvider index2, int[] fontSpecs) {
        this.loadingFontResource = index1;
        this.fontmetricsResource = index2;
        this.fontFileIds = fontSpecs;
    }

    public void initializeFontMap(FontProvider fontprovider) {
        this.fontMap = new HashMap<>(this.fontFileIds.length);
        for (int index = 0; index < this.fontFileIds.length; index++) {
            int fileId = this.fontFileIds[index];
            FontMetrics fontMetrics = FontMetrics.list(this.fontmetricsResource, fileId);
            byte[] fontData = this.loadingFontResource.getFile(fileId);
            Object fontRenderer = fontprovider.createFontRenderer(fontData, fontMetrics, true);
            this.fontMap.put(Integer.valueOf(index), new TypeResult(fontRenderer, fontMetrics));
        }
    }

    public int getNumCached() {
        return getNumCached(false);
    }

    public int getNumCached(boolean direct) {
        if (this.fontFileIds == null) {
            return 0;
        } 
        if (!direct && this.fontMap != null) {
            return this.fontFileIds.length * 2;
        } 
        int numCached = 0;
        for (int index = 0; index < this.fontFileIds.length; index++) {
            int fileId = this.fontFileIds[index];
            if (this.loadingFontResource.isFileCached(fileId)) {
                numCached++;
            }
            if (this.fontmetricsResource.isFileCached(fileId)) {
                numCached++;
            }
        }
        return numCached;
    }

    public TypeResult listFontCombo(FontProvider fontProvider, int fontId, boolean includeFontmetrics, boolean monospaced) {
        if (fontId == -1) {
            return null;
        } 
        if (this.fontFileIds != null) {
            for (int index = 0; index < this.fontFileIds.length; index++) {
                if (fontId == this.fontFileIds[index]) {
                    return this.fontMap.get(Integer.valueOf(index));
                }
            }
        }
        TypeResult typeResult = (TypeResult) this.cache.get((long) fontId << 1 | (monospaced ? 1 : 0));
        if (typeResult != null) {
            if (includeFontmetrics && typeResult.companionObject == null) {
                FontMetrics companionMetrics = FontMetrics.list(this.fontmetricsResource, fontId);
                if (companionMetrics == null) {
                    return null;
                }
                typeResult.companionObject = companionMetrics;
            }
            return typeResult;
        }
        byte[] loadingFontData = this.loadingFontResource.getFile(fontId);
        if (loadingFontData == null) {
            return null;
        } 
        FontMetrics fontMetrics = FontMetrics.list(this.fontmetricsResource, fontId);
        if (fontMetrics == null) {
            return null;
        } 
        if (includeFontmetrics) {
            typeResult = new TypeResult(fontProvider.createFontRenderer(loadingFontData, fontMetrics, monospaced), fontMetrics);
        } else {
            typeResult = new TypeResult(fontProvider.createFontRenderer(loadingFontData, fontMetrics, monospaced), null);
        }
        this.cache.put(typeResult, (long) fontId << 1 | (monospaced ? 1 : 0));
        return typeResult;
    }

    public void resetCache() {
        this.cache.reset();
    }

    public void sweepCache() {
        this.cache.clean(50);
    }

    public void clearCacheQueue() {
        this.cache.resize();
    }

    public FontMetrics list(FontProvider fontProvider, int fontId) {
        TypeResult typeResult = listFontCombo(fontProvider, fontId, true, true);
        return typeResult == null ? null : (FontMetrics) typeResult.companionObject;
    }

    public int getFontCount() {
        return this.fontFileIds == null ? 0 : this.fontFileIds.length * 2;
    }

    public Object list(FontProvider fontProvider, int fontId, boolean includeFontmetrics, boolean monospaced) {
        TypeResult typeResult = listFontCombo(fontProvider, fontId, includeFontmetrics, monospaced);
        return typeResult == null ? null : typeResult.mainObject;
    }

    public void resetFontMap() {
        this.fontMap = null;
    }
}
