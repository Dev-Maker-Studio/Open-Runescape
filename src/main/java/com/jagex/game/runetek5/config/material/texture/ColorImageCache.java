package com.jagex.game.runetek5.config.material.texture;

import com.jagex.game.runetek5.cache.node.LinkedList;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class ColorImageCache {

    public static short[] CS2_QUERY_RESULTS;
    public static final ColorImageCacheEntry aNodeSub163618 = new ColorImageCacheEntry(0, 0);
    public boolean dirty;
    public ColorImageCacheEntry[] aNodeSub16Array3615;
    public int[][][] anIntArrayArrayArray3614;
    public LinkedList aClass4733612 = new LinkedList();
    public int anInt3617;
    public int anInt3616 = -1;
    public final int anInt3611;
    public final int anInt3613;

    public ColorImageCache(int i_1, int i_2, int i_3) {
        this.anInt3611 = i_2;
        this.anInt3613 = i_1;
        this.anIntArrayArrayArray3614 = new int[this.anInt3613][3][i_3];
        this.aNodeSub16Array3615 = new ColorImageCacheEntry[this.anInt3611];
    }

    public void clear() {
        for (int i_2 = 0; i_2 < this.anInt3613; i_2++) {
            this.anIntArrayArrayArray3614[i_2][0] = null;
            this.anIntArrayArrayArray3614[i_2][1] = null;
            this.anIntArrayArrayArray3614[i_2][2] = null;
            this.anIntArrayArrayArray3614[i_2] = null;
        }
        this.aNodeSub16Array3615 = null;
        this.anIntArrayArrayArray3614 = null;
        this.aClass4733612.clear();
        this.aClass4733612 = null;
    }

    public int[][] getPalette(int i_1) {
        if (this.anInt3611 == this.anInt3613) {
            this.dirty = this.aNodeSub16Array3615[i_1] == null;
            this.aNodeSub16Array3615[i_1] = aNodeSub163618;
            return this.anIntArrayArrayArray3614[i_1];
        }
        if (this.anInt3613 == 1) {
            this.dirty = this.anInt3616 != i_1;
            this.anInt3616 = i_1;
            return this.anIntArrayArrayArray3614[0];
        }
        ColorImageCacheEntry class282Sub163 = this.aNodeSub16Array3615[i_1];
        if (class282Sub163 == null) {
            this.dirty = true;
            if (this.anInt3617 >= this.anInt3613) {
                ColorImageCacheEntry class282Sub164 = (ColorImageCacheEntry) this.aClass4733612.getNext();
                class282Sub163 = new ColorImageCacheEntry(i_1, class282Sub164.index);
                this.aNodeSub16Array3615[class282Sub164.row] = null;
                class282Sub164.unlink();
            } else {
                class282Sub163 = new ColorImageCacheEntry(i_1, this.anInt3617++);
            }
            this.aNodeSub16Array3615[i_1] = class282Sub163;
        } else {
            this.dirty = false;
        }
        this.aClass4733612.insertHead(class282Sub163);
        return this.anIntArrayArrayArray3614[class282Sub163.index];
    }

    public int[][][] method5464() {
        if (this.anInt3613 != this.anInt3611) {
            throw new RuntimeException();
        }
        for (int i_2 = 0; i_2 < this.anInt3613; i_2++) {
            this.aNodeSub16Array3615[i_2] = aNodeSub163618;
        }
        return this.anIntArrayArrayArray3614;
    }
}
