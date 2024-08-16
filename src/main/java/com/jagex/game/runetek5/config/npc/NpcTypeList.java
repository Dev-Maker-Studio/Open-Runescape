package com.jagex.game.runetek5.config.npc;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.tools.LocalizedText;

public class NpcTypeList {

    public int preferenceFlags;

    public final LRUCache typeCache = new LRUCache(64);

    public final LRUCache bodymodelCache = new LRUCache(50);

    public final LRUCache headmodelCache = new LRUCache(5);

    public final Game game;

    public final Language lang;

    public boolean isMember;

    public final Js5ResourceProvider configResource;

    public final Js5ResourceProvider modelResource;

    public final String[] defaultActions;

    public NpcTypeList(Game game, Language language, Js5ResourceProvider configResource, Js5ResourceProvider modelResource) {
        this.game = game;
        this.lang = language;
        this.isMember = true;
        this.configResource = configResource;
        this.modelResource = modelResource;
        if (this.configResource != null) {
            int lastArchive = this.configResource.getArchiveCount() - 1;
            SharedConfigsType.NPCS.archiveFileCapacity();
            this.configResource.getArchiveFileCount(lastArchive);
        }
        if (this.game == Game.RUNESCAPE) {
        	this.defaultActions = new String[]{null, null, null, null, null, LocalizedText.EXAMINE.translate(this.lang)};
        } else {
        	this.defaultActions = new String[6];
        }
    }

    public NPCType list(int id) {
        NPCType npcType;
        synchronized (this.typeCache) {
            npcType = (NPCType) this.typeCache.get(id);
        }
        if (npcType != null) {
            return npcType;
        } 
        byte[] data;
        synchronized (this.configResource) {
            data = this.configResource.getFile(SharedConfigsType.NPCS.archiveId(id), SharedConfigsType.NPCS.archiveFileId(id));
        }
        npcType = new NPCType();
        npcType.type = id;
        npcType.npcTypeList = this;
        npcType.actions = this.defaultActions.clone();
        if (data != null) {
            npcType.decodeType(new Packet(data));
        }
        npcType.setGameType();
        synchronized (this.typeCache) {
            this.typeCache.put(npcType, id);
            return npcType;
        }
    }

    public void setPreferenceFlags(int prefFlags) {
        this.preferenceFlags = prefFlags;
        synchronized (this.bodymodelCache) {
            this.bodymodelCache.reset();
        }
        synchronized (this.headmodelCache) {
            this.headmodelCache.reset();
        }
    }

    public void resetBodyCaches() {
        synchronized (this.bodymodelCache) {
            this.bodymodelCache.reset();
        }
        synchronized (this.headmodelCache) {
            this.headmodelCache.reset();
        }
    }

    public void sweepCaches() {
        synchronized (this.typeCache) {
            this.typeCache.clean(5);
        }
        synchronized (this.bodymodelCache) {
            this.bodymodelCache.clean(5);
        }
        synchronized (this.headmodelCache) {
            this.headmodelCache.clean(5);
        }
    }

    public void clearCacheQueues() {
        synchronized (this.typeCache) {
            this.typeCache.resize();
        }
        synchronized (this.bodymodelCache) {
            this.bodymodelCache.resize();
        }
        synchronized (this.headmodelCache) {
            this.headmodelCache.resize();
        }
    }

    public void resetCaches() {
        synchronized (this.typeCache) {
            this.typeCache.reset();
        }
        synchronized (this.bodymodelCache) {
            this.bodymodelCache.reset();
        }
        synchronized (this.headmodelCache) {
            this.headmodelCache.reset();
        }
    }

    public void setMember(boolean isMember) {
        if (this.isMember != isMember) {
            this.isMember = isMember;
            resetCaches();
        }
    }
}
