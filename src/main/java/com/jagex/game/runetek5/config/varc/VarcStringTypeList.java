package com.jagex.game.runetek5.config.varc;

import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class VarcStringTypeList {

    public final int size;
    public final Js5ResourceProvider configResource;

    public VarcStringTypeList(Js5ResourceProvider configResource) {
        new LRUCache(64);
        this.configResource = configResource;
        this.size = configResource.getArchiveFileCount(SharedConfigsType.VARC_STRING.id);
    }
}
