package com.jagex.game.runetek5.config.enumtype;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class EnumTypeList {

    public final LRUCache cache = new LRUCache(128);

    public final Js5ResourceProvider enumTypeResource;

    public EnumTypeList(Js5ResourceProvider enumTypeResource) {
        this.enumTypeResource = enumTypeResource;
        if (this.enumTypeResource != null) {
            int archiveCount = this.enumTypeResource.getArchiveCount() - 1;
            SharedConfigsType.ENUMS.archiveFileCapacity();
            this.enumTypeResource.getArchiveFileCount(archiveCount);
        }
    }

    public EnumType list(int id) {
        EnumType enumType;
        synchronized (this.cache) {
            enumType = (EnumType) this.cache.get(id);
        }
        if (enumType != null) {
            return enumType;
        }
        byte[] data = this.enumTypeResource.getFile(SharedConfigsType.ENUMS.archiveId(id), SharedConfigsType.ENUMS.archiveFileId(id));
        enumType = new EnumType();
        if (data != null) {
            enumType.decode(new Packet(data));
        }
        synchronized (this.cache) {
            this.cache.put(enumType, id);
            return enumType;
        }
    }
}
