package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.cache.node.Node;

public class LocOnWorldMap extends Node<LocOnWorldMap> {

    public final int[] locIds;
    public final byte[] shapeAndRotation;

    public LocOnWorldMap(int[] locIds, byte[] shapeAndRotation) {
        this.locIds = locIds;
        this.shapeAndRotation = shapeAndRotation;
    }

}
