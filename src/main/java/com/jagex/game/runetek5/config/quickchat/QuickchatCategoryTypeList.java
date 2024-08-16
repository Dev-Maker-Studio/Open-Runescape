package com.jagex.game.runetek5.config.quickchat;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
public class QuickchatCategoryTypeList {

    public final LRUCache categoryCache = new LRUCache(64);
    public final Js5ResourceProvider messageResource;
    public final Js5ResourceProvider menuResource;

    public QuickchatCategoryTypeList(Js5ResourceProvider messageResource, Js5ResourceProvider menuResource) {
        this.messageResource = messageResource;
        this.menuResource = menuResource;
        if (this.messageResource != null) {
        	this.messageResource.getArchiveFileCount(0);
        }
        if (this.menuResource != null) {
        	this.menuResource.getArchiveFileCount(0);
        }
    }

    public QuickChatCategoryType list(int categoryId) {
        QuickChatCategoryType categoryType = (QuickChatCategoryType) this.categoryCache.get(categoryId);
        if (categoryType == null) {
            byte[] data;
            if (categoryId >= 32768) {
                data = this.menuResource.getFile(0, categoryId & 0x7fff);
            } else {
                data = this.messageResource.getFile(0, categoryId);
            }
            categoryType = new QuickChatCategoryType();
            if (data != null) {
                categoryType.decode(new Packet(data));
            }
            if (categoryId >= 32768) {
                categoryType.postDecode();
            }
            this.categoryCache.put(categoryType, categoryId);
        }
        return categoryType;
    }
}
