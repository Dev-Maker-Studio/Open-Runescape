package com.jagex.game.runetek5.config.loc;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.tools.LocalizedText;

public class LocTypeList {

	/**
	 * The preferences set for the player, EG: textures off
	 */
    int preferenceMask;

    Mesh[] meshCombineCache = new Mesh[4];

    TypeResult locresult = new TypeResult(null, null);

    public LRUCache cache = new LRUCache(256);

    final LRUCache modelsByShapeCache = new LRUCache(500);

    final LRUCache typeresultCache = new LRUCache(30);

    final LRUCache uidToModelCache = new LRUCache(50);

    public final Game game;

    public final Language language;

    boolean playerIsMember;

    public final Js5ResourceProvider configResource;

    final Js5ResourceProvider meshResource;

    public final String[] defaultActions;

    public LocTypeList(Game game, Language language, Js5ResourceProvider configResource, Js5ResourceProvider meshResource) {
        this.game = game;
        this.language = language;
        this.playerIsMember = true;
        this.configResource = configResource;
        this.meshResource = meshResource;
        if (this.configResource != null) {
            int lastArchive = this.configResource.getArchiveCount() - 1;
            SharedConfigsType.OBJECTS.archiveFileCapacity();
            this.configResource.getArchiveFileCount(lastArchive);
        }
        if (this.game == Game.RUNESCAPE) {
        	this.defaultActions = new String[]{null, null, null, null, null, LocalizedText.EXAMINE.translate(this.language)};
        } else {
        	this.defaultActions = new String[6];
        }
    }

    public LocType list(int id) {
        LocType loctype;
        synchronized (this.cache) {
            loctype = (LocType) this.cache.get(id);
        }
        if (loctype != null) {
            return loctype;
        }
        byte[] data;
        synchronized (this.configResource) {
            data = this.configResource.getFile(SharedConfigsType.OBJECTS.archiveId(id), SharedConfigsType.OBJECTS.archiveFileId(id));
        }
        loctype = new LocType();
        loctype.id = id;
        loctype.locTypeList = this;
        loctype.actions = this.defaultActions.clone();
        if (data != null) {
            loctype.decodeType(new Packet(data));
        }
        loctype.postDecode();
        if (loctype.ignoreClipOnAltRoute) {
            loctype.blocksMovement = 0;
            loctype.blocksProjectiles = false;
        }
        if (!this.playerIsMember && loctype.members) {
            loctype.actions = null;
            loctype.quests = null;
        }
        synchronized (this.cache) {
            this.cache.put(loctype, id);
            return loctype;
        }
    }

    public void resetCaches(int prefFlags) {
        this.preferenceMask = prefFlags;
        synchronized (this.modelsByShapeCache) {
            this.modelsByShapeCache.reset();
        }
        synchronized (this.typeresultCache) {
            this.typeresultCache.reset();
        }
        synchronized (this.uidToModelCache) {
            this.uidToModelCache.reset();
        }
    }

    public void resetAll() {
        synchronized (this.cache) {
            this.cache.reset();
        }
        synchronized (this.modelsByShapeCache) {
            this.modelsByShapeCache.reset();
        }
        synchronized (this.typeresultCache) {
            this.typeresultCache.reset();
        }
        synchronized (this.uidToModelCache) {
            this.uidToModelCache.reset();
        }
        this.meshCombineCache = new Mesh[4];
        this.locresult = new TypeResult(null, null);
    }

    public void cacheClean() {
        synchronized (this.cache) {
            this.cache.clean(5);
        }
        synchronized (this.modelsByShapeCache) {
            this.modelsByShapeCache.clean(5);
        }
        synchronized (this.typeresultCache) {
            this.typeresultCache.clean(5);
        }
        synchronized (this.uidToModelCache) {
            this.uidToModelCache.clean(5);
        }
    }

    public void clearQueue() {
        synchronized (this.cache) {
            this.cache.resize();
        }
        synchronized (this.modelsByShapeCache) {
            this.modelsByShapeCache.resize();
        }
        synchronized (this.typeresultCache) {
            this.typeresultCache.resize();
        }
        synchronized (this.uidToModelCache) {
            this.uidToModelCache.resize();
        }
    }

    public void resetLruCache(int capacity) {
        this.cache = new LRUCache(capacity);
    }

    public void setMember(boolean member) {
        if (this.playerIsMember != member) {
            this.playerIsMember = member;
            resetAll();
        }
    }
}