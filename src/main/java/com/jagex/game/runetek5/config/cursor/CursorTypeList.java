package com.jagex.game.runetek5.config.cursor;

import java.awt.*;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class CursorTypeList {

    public final LRUCache cursorTypeCache = new LRUCache(64);
    final LRUCache spriteCache = new LRUCache(2);
    public final Js5ResourceProvider cursorResource;
    final Js5ResourceProvider spriteResource;

    public CursorTypeList(Js5ResourceProvider cursorResource, Js5ResourceProvider spriteResource) {
        this.cursorResource = cursorResource;
        this.spriteResource = spriteResource;
        this.cursorResource.getArchiveFileCount(SharedConfigsType.CURSORS.id);
    }

    public static void setCustomCursor(int pickedCursor) {
        if (PreferenceManager.preferences.customCursor.getValue() == 0) {
            pickedCursor = -1;
        }
        if (pickedCursor != client.CURRENT_CURSOR) {
            if (pickedCursor != -1) {
                CursorType cursortype = TypeLists.CURSOR.list(pickedCursor);
                SpriteType spritetype = cursortype.getSprite();
                if (spritetype != null) {
                    CursorManager.instance.setCustomCursor(GameShell.gameCanvas,
	            		spritetype.getPixels(),
	            		spritetype.widthSum(),
	            		spritetype.heightSum(),
	            		new Point(cursortype.hotspotPointX, cursortype.hotspotPointY)
                    );
                    client.CURRENT_CURSOR = pickedCursor;
                } else {
                    pickedCursor = -1;
                }
            }
            if (pickedCursor == -1 && client.CURRENT_CURSOR != -1) {
                CursorManager.instance.setCustomCursor(GameShell.gameCanvas, null, -1, -1, new Point());
                client.CURRENT_CURSOR = -1;
            }
        }
    }

    public CursorType list(int cursorId) {
        CursorType cursortype;
        synchronized (this.cursorTypeCache) {
            cursortype = (CursorType) this.cursorTypeCache.get(cursorId);
        }
        if (cursortype != null) {
            return cursortype;
        }
        byte[] cursorData;
        synchronized (this.cursorResource) {
            cursorData = this.cursorResource.getFile(SharedConfigsType.CURSORS.id, cursorId);
        }
        cursortype = new CursorType();
        cursortype.cursorTypeList = this;
        if (cursorData != null) {
            cursortype.decode(new Packet(cursorData));
        }
        synchronized (this.cursorTypeCache) {
            this.cursorTypeCache.put(cursortype, cursorId);
            return cursortype;
        }
    }

    public void processCycle() {
        synchronized (this.cursorTypeCache) {
            this.cursorTypeCache.clean(5);
        }
        synchronized (this.spriteCache) {
            this.spriteCache.clean(5);
        }
    }

    public void clearQueues() {
        synchronized (this.cursorTypeCache) {
            this.cursorTypeCache.resize();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.resize();
        }
    }
    
    public void resetCaches() {
        synchronized (this.cursorTypeCache) {
            this.cursorTypeCache.reset();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.reset();
        }
    }
}
