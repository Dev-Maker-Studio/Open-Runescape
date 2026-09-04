package com.jagex.game.runetek5.config.cursor

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import java.awt.Point

class CursorTypeList(
    val cursorResource: Js5ResourceProvider,
    val spriteResource: Js5ResourceProvider
) {

    @JvmField val cursorTypeCache = LRUCache(64)
    @JvmField val spriteCache = LRUCache(2)

    init {
        cursorResource.getArchiveFileCount(SharedConfigsType.CURSORS.id)
    }

    fun list(cursorId: Int): CursorType {
        var cursortype: CursorType?
        synchronized(cursorTypeCache) {
            cursortype = cursorTypeCache.get(cursorId.toLong()) as CursorType?
        }
        if (cursortype != null) {
            return cursortype!!
        }
        val cursorData: ByteArray?
        synchronized(cursorResource) {
            cursorData = cursorResource.getFile(SharedConfigsType.CURSORS.id, cursorId)
        }
        cursortype = CursorType()
        cursortype!!.cursorTypeList = this
        if (cursorData != null) {
            cursortype!!.decode(Packet(cursorData))
        }
        synchronized(cursorTypeCache) {
            cursorTypeCache.put(cursortype, cursorId.toLong())
            return cursortype!!
        }
    }

    fun processCycle() {
        synchronized(cursorTypeCache) {
            cursorTypeCache.clean(5)
        }
        synchronized(spriteCache) {
            spriteCache.clean(5)
        }
    }

    fun clearQueues() {
        synchronized(cursorTypeCache) {
            cursorTypeCache.resize()
        }
        synchronized(spriteCache) {
            spriteCache.resize()
        }
    }

    fun resetCaches() {
        synchronized(cursorTypeCache) {
            cursorTypeCache.reset()
        }
        synchronized(spriteCache) {
            spriteCache.reset()
        }
    }

    companion object {
        @JvmStatic
        fun setCustomCursor(pickedCursor: Int) {
            var cursor = pickedCursor
            if (PreferenceManager.preferences!!.customCursor!!.getValue() == 0) {
                cursor = -1
            }
            if (cursor != Client.CURRENT_CURSOR) {
                if (cursor != -1) {
                    val cursortype = TypeLists.CURSOR!!.list(cursor)
                    val spritetype = cursortype.getSprite()
                    if (spritetype != null) {
                        CursorManager.instance!!.setCustomCursor(
                            GameShell.gameCanvas!!,
                            spritetype.getPixels(),
                            spritetype.widthSum(),
                            spritetype.heightSum(),
                            Point(cursortype.hotspotPointX, cursortype.hotspotPointY)
                        )
                        Client.CURRENT_CURSOR = cursor
                    } else {
                        cursor = -1
                    }
                }
                if (cursor == -1 && Client.CURRENT_CURSOR != -1) {
                    CursorManager.instance!!.setCustomCursor(GameShell.gameCanvas!!, null, -1, -1, Point())
                    Client.CURRENT_CURSOR = -1
                }
            }
        }
    }
}
