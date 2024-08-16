package com.jagex.game.runetek5.client.menu;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;

public class MiniMenuEntry extends CacheableNode {

	public String aString9585;
    public final String coloredName;
    public final String action;
    public final int cursor;
    public int clickType;
    public final int itemId;
    public final long entityUid;
    public final int anInt9581;
    public final int anInt9582;
    public final boolean aBool9583;
    public final boolean aBool9577;
    public final long bitpacked;
    public final boolean aBool9586;

    public MiniMenuEntry(String action, String coloredName, int cursor, int clickType, int id, long entityUid, int i_8, int i_9, boolean bool10, boolean bool11, long bitpacked, boolean bool14) {
        this.coloredName = coloredName;
        this.action = action;
        this.cursor = cursor;
        this.clickType = clickType;
        this.itemId = id;
        this.entityUid = entityUid;
        this.anInt9581 = i_8;
        this.anInt9582 = i_9;
        this.aBool9583 = bool10;
        this.aBool9577 = bool11;
        this.bitpacked = bitpacked;
        this.aBool9586 = bool14;
    }
}
