package com.jagex.game.runetek5.config.material.texture;

import com.jagex.game.runetek5.cache.node.LinkedList;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class MonochromeImageCache {

    public static final MonochromeImageCacheEntry EMPTY = new MonochromeImageCacheEntry(0, 0);
    public boolean dirty;
    public MonochromeImageCacheEntry[] nodes;
    public int[][] paletteData;
    public LinkedList nodelist = new LinkedList();
    public int anInt3714;
    public int lastUsedNodeIndex = -1;
    public final int anInt3715;
    public final int paletteSize;

    public MonochromeImageCache(int i_1, int i_2, int dataLength) {
        this.anInt3715 = i_2;
        this.paletteSize = i_1;
        this.paletteData = new int[this.paletteSize][dataLength];
        this.nodes = new MonochromeImageCacheEntry[this.anInt3715];
    }

    public void reset() {
        for (int i = 0; i < this.paletteSize; i++) {
            this.paletteData[i] = null;
        }
        this.nodes = null;
        this.paletteData = null;
        this.nodelist.clear();
        this.nodelist = null;
    }

    public int[] getPalette(int i_1) {
        if (this.anInt3715 == this.paletteSize) {
            this.dirty = this.nodes[i_1] == null;
            this.nodes[i_1] = EMPTY;
            return this.paletteData[i_1];
        }
        if (this.paletteSize != 1) {
            MonochromeImageCacheEntry class282Sub253 = this.nodes[i_1];
            if (class282Sub253 == null) {
                this.dirty = true;
                if (this.anInt3714 >= this.paletteSize) {
                    MonochromeImageCacheEntry class282Sub254 = (MonochromeImageCacheEntry) this.nodelist.getNext();
                    class282Sub253 = new MonochromeImageCacheEntry(i_1, class282Sub254.anInt7689);
                    this.nodes[class282Sub254.nodeRef] = null;
                    class282Sub254.unlink();
                } else {
                    class282Sub253 = new MonochromeImageCacheEntry(i_1, this.anInt3714);
                    ++this.anInt3714;
                }
                this.nodes[i_1] = class282Sub253;
            } else {
                this.dirty = false;
            }
            this.nodelist.insertHead(class282Sub253);
            return this.paletteData[class282Sub253.anInt7689];
        }
        this.dirty = this.lastUsedNodeIndex != i_1;
        this.lastUsedNodeIndex = i_1;
        return this.paletteData[0];
    }

    public int[][] method5722() {
        if (this.paletteSize != this.anInt3715) {
            throw new RuntimeException();
        }
        for (int i_2 = 0; i_2 < this.paletteSize; i_2++) {
            this.nodes[i_2] = EMPTY;
        }
        return this.paletteData;
    }
}
