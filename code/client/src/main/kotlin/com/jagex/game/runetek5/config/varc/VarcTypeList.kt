package com.jagex.game.runetek5.config.varc

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.JFS32
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import java.io.IOException

class VarcTypeList(
    @JvmField val varcTypeResource: Js5ResourceProvider
) {

    @JvmField val size: Int = varcTypeResource.getArchiveFileCount(SharedConfigsType.VARC.id)
    @JvmField val varcTypeCache: LRUCache = LRUCache(64)

    fun list(id: Int): VarcType {
        var varcType: VarcType?
        synchronized(varcTypeCache) {
            varcType = varcTypeCache.get(id.toLong()) as VarcType?
        }

        if (varcType != null) {
            return varcType!!
        }
        val varcData: ByteArray?
        synchronized(varcTypeResource) {
            varcData = varcTypeResource.getFile(SharedConfigsType.VARC.id, id)
        }

        varcType = VarcType()
        if (varcData != null) {
            varcType!!.decode(Packet(varcData))
        }
        synchronized(varcTypeCache) {
            varcTypeCache.put(varcType, id.toLong())
            return varcType!!
        }
    }

    companion object {
        @JvmField var VARC_INT: IntArray? = null
        @JvmField var VARC_PERSISTENT: BooleanArray? = null
        @JvmField var VARC_STRING: Array<String?>? = null

        @JvmStatic
        fun readVarcIntPreferences() {
            var jagexFilesystem: JFS32? = null
            try {
                jagexFilesystem = PreferenceManager.loadPreferenceFile("2", Client.CURRENT_GAME!!.name, false)
                val data = ByteArray(jagexFilesystem.length().toInt())
                var seekOffset: Int
                var position = 0
                while (position < data.size) {
                    seekOffset = jagexFilesystem.getSeekOffset(data, position, data.size - position)
                    if (seekOffset == -1) {
                        throw IOException()
                    }
                    position += seekOffset
                }
                decodeVarcInt(Packet(data))
            } catch (exception: Exception) {
                // NOOP
            }
            try {
                if (jagexFilesystem != null) {
                    jagexFilesystem.close()
                }
            } catch (exception: Exception) {
                // NOOP
            }
        }

        @JvmStatic
        fun decodeVarcInt(buffer: Packet) {
            if (buffer.buffer.size - buffer.currentPosition >= 1) {
                val version = buffer.readUByte()
                if (version in 0..1 && buffer.buffer.size - buffer.currentPosition >= 2) {
                    val size = buffer.readUShort()
                    if (buffer.buffer.size - buffer.currentPosition >= size * 6) {
                        for (index in 0 until size) {
                            val varcId = buffer.readUShort()
                            val varcState = buffer.readInt()
                            if (varcId < VARC_INT!!.size && VARC_PERSISTENT!![varcId] && (TypeLists.VARC!!.list(varcId).typeChar != '1' || varcState >= -1 && varcState <= 1)) {
                                VARC_INT!![varcId] = varcState
                            }
                        }
                    }
                }
            }
        }
    }
}
