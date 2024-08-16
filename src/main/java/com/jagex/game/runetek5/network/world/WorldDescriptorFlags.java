package com.jagex.game.runetek5.network.world;

public abstract class WorldDescriptorFlags {

    public int flags;
    public int playerCount;
    public int worldIndex;

    public boolean isMembers() {
        return (this.flags & 0x1) != 0;
    }

    public boolean isQuickChat() {
        return (this.flags & 0x2) != 0;
    }

    public boolean isPvp() {
        return (this.flags & 0x4) != 0;
    }

    public boolean isLootshare() {
        return (this.flags & 0x8) != 0;
    }

    public boolean hasPort() {
        return (flags & 0x40000000) != 0;
    }
}
