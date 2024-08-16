package com.jagex.game.runetek5.config.clanvarsettings;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class ClanVarSettingsTypeList {

    public final LRUCache cache = new LRUCache(64);

    public final Js5ResourceProvider configResource;

    public ClanVarSettingsTypeList(Js5ResourceProvider configResource) {
        this.configResource = configResource;
        if (this.configResource != null) {
        	this.configResource.getArchiveFileCount(SharedConfigsType.CLAN_VAR_SETTINGS.id);
        }
    }
    
    public ClanVarSettingsType list(int id) {
        ClanVarSettingsType clanvarSettingsType;
        synchronized (this.cache) {
            clanvarSettingsType = (ClanVarSettingsType) this.cache.get(id);
        }
        if (clanvarSettingsType != null) {
            return clanvarSettingsType;
        } 
        byte[] data;
        synchronized (this.configResource) {
            data = this.configResource.getFile(SharedConfigsType.CLAN_VAR_SETTINGS.id, id);
        }
        clanvarSettingsType = new ClanVarSettingsType();
        if (data != null) {
            clanvarSettingsType.decode(new Packet(data));
        }
        synchronized (this.cache) {
            this.cache.put(clanvarSettingsType, id);
            return clanvarSettingsType;
        }
    }

    public void sweepCache() {
        synchronized (this.cache) {
            this.cache.clean(5);
        }
    }

    public void resetCache() {
        synchronized (this.cache) {
            this.cache.reset();
        }
    }

    public void clearCacheQueue() {
        synchronized (this.cache) {
            this.cache.resize();
        }
    }
}
