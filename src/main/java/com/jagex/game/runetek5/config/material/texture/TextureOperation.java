package com.jagex.game.runetek5.config.material.texture;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;

/**
 * 
 * @author Steven - 29/11/2023
 * @author OpenOSRS - Nearly all naming
 *
 */
public abstract class TextureOperation extends Node<TextureOperation> {

	public ColorImageCache colorImageCache;
    public MonochromeImageCache monochromeImageCache;
    public boolean monochrome;
    public int imageCacheCapacity;
    public final TextureOperation[] operations;

    protected TextureOperation(int count, boolean monochrome) {
        this.monochrome = monochrome;
        this.operations = new TextureOperation[count];
    }

    public void createImageCache(int width, int height) {
        int capacity = this.imageCacheCapacity == 255 ? height : this.imageCacheCapacity;
        if (this.monochrome) {
            this.monochromeImageCache = new MonochromeImageCache(capacity, height, width);
        } else {
            this.colorImageCache = new ColorImageCache(capacity, height, width);
        }
    }

    protected int[] getChildMonochromeOutput(int operationIndex, int y) {
        return !this.operations[operationIndex].monochrome ? this.operations[operationIndex].getColorOutput(y)[0] : this.operations[operationIndex].getMonochromeOutput(y);
    }

    public int[] getMonochromeOutput(int y) {
        throw new IllegalStateException();
    }

    public int[][] getColorOutput(int y) {
        throw new IllegalStateException();
    }

    public void postDecode() {
    }

    public void decodeOpcode(int opcode, Packet buffer) {
    }

    public int getTextureId() {
        return -1;
    }

    public int getMaterialId() {
        return -1;
    }

    public void clearImageCache() {
        if (this.monochrome) {
            this.monochromeImageCache.reset();
            this.monochromeImageCache = null;
        } else {
            this.colorImageCache.clear();
            this.colorImageCache = null;
        }
    }

    public int[][] getChildColorOutput(int operationIndex, int y) {
        if (this.operations[operationIndex].monochrome) {
            int[] monochromeRow = this.operations[operationIndex].getMonochromeOutput(y);
            int[][] colors = {monochromeRow, monochromeRow, monochromeRow};
            return colors;
        } 
        return this.operations[operationIndex].getColorOutput(y);
    }
}
