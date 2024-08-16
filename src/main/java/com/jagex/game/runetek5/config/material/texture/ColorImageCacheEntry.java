package com.jagex.game.runetek5.config.material.texture;

import com.jagex.game.runetek5.cache.node.Node;

/**
 * 
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 *
 */
public class ColorImageCacheEntry extends Node {

	public final int row;
    public final int index;

    public ColorImageCacheEntry(int row, int index) {
        this.row = row;
        this.index = index;
    }

}
