package com.jagex.game.runetek5.config.rsinterface;

import com.jagex.game.runetek5.cache.node.Node;

public class IFTargetParams extends Node {

    public final int settingsHash;
    public final int interfaceId;

    public IFTargetParams(int i_1, int i_2) {
        this.settingsHash = i_1;
        this.interfaceId = i_2;
    }

    public boolean dragEnabled() {
        return (this.settingsHash >> 21 & 0x1) != 0;
    }

    public boolean clickOptionEnabled(int option) {
        return (this.settingsHash >> option + 1 & 0x1) != 0;
    }

    public int getUseOptionFlags() {
        return Component.getUseOptionFlags(this.settingsHash);
    }

    public int depthFlags() {
        return this.settingsHash >> 18 & 0x7;
    }

    public boolean bit22Enabled() {
        return (this.settingsHash >> 22 & 0x1) != 0;
    }

    public boolean continueOptionEnabled() {
        return (this.settingsHash & 0x1) != 0;
    }

    public boolean bit23Enabled() {
        return (this.settingsHash >> 23 & 0x1) != 0;
    }

}
