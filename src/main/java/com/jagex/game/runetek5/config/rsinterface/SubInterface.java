package com.jagex.game.runetek5.config.rsinterface;

import com.jagex.game.runetek5.cache.node.Node;

public class SubInterface extends Node {

    public final int subInterfaceId;
    public final int overlay;

    public SubInterface(int subInterfaceId, int overlay) {
        this.subInterfaceId = subInterfaceId;
        this.overlay = overlay;
    }

    public boolean executeTrigger() {
        return true;
    }
}
