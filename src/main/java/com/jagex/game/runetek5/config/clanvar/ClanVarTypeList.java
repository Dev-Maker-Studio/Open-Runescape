package com.jagex.game.runetek5.config.clanvar;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class ClanVarTypeList {

    public final int size;
    public final LRUCache clanvarCache = new LRUCache(64);
    public final Js5ResourceProvider configProvider;
	public static Object[] CLAN_VARS;

    public ClanVarTypeList(Js5ResourceProvider configProvider) {
        this.configProvider = configProvider;
        if (this.configProvider != null) {
        	this.size = this.configProvider.getArchiveFileCount(SharedConfigsType.CLAN_VAR.id);
        } else {
        	this.size = 0;
        }
    }

    public ClanVarType list(int clanvarId) {
        ClanVarType clanvarType;
        synchronized (this.clanvarCache) {
            clanvarType = (ClanVarType) this.clanvarCache.get(clanvarId);
        }
        if (clanvarType != null) {
            return clanvarType;
        } 
        byte[] data;
        synchronized (this.configProvider) {
            data = this.configProvider.getFile(SharedConfigsType.CLAN_VAR.id, clanvarId);
        }
        clanvarType = new ClanVarType();
        if (data != null) {
            clanvarType.decodeType(new Packet(data));
        }
        synchronized (this.clanvarCache) {
            this.clanvarCache.put(clanvarType, clanvarId);
            return clanvarType;
        }
    }

    public void reset() {
        synchronized (this.clanvarCache) {
            this.clanvarCache.reset();
        }
    }

    public void sweep() {
        synchronized (this.clanvarCache) {
            this.clanvarCache.clean(5);
        }
    }

    public void clearQueue() {
        synchronized (this.clanvarCache) {
            this.clanvarCache.resize();
        }
    }
}
