package com.jagex.game.runetek5.config.quickchat

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class QuickchatMessageTypeList(
    @JvmField val messageResource: Js5ResourceProvider?,
    @JvmField val menuResource: Js5ResourceProvider?,
    @JvmField val quickchatFillerlist: QuickchatFillerList?
) {

    @JvmField var messageCount: Int = 0
    @JvmField var menuCount: Int = 0
    @JvmField val messageTypeCache = LRUCache(64)

    init {
        if (messageResource != null) {
            messageCount = messageResource.getArchiveFileCount(1)
        }
        if (menuResource != null) {
            menuCount = menuResource.getArchiveFileCount(1)
        }
    }

    fun getValueDisplay(value: QuickChatValueType, configs: IntArray, data: Long): String {
        if (quickchatFillerlist != null) {
            val displayName = quickchatFillerlist.getValueDisplay(value, configs, data)
            if (displayName != null) {
                return displayName
            }
        }
        return data.toString()
    }

    fun listMessage(id: Int): QuickchatMessageType {
        var messageType = messageTypeCache.get(id.toLong()) as QuickchatMessageType?
        if (messageType == null) {
            val data: ByteArray? = if (id >= 32768) {
                menuResource!!.getFile(1, id and 0x7fff)
            } else {
                messageResource!!.getFile(1, id)
            }

            messageType = QuickchatMessageType()
            messageType.quickchatMessageTypelist = this
            if (data != null) {
                messageType.decode(Packet(data))
            }

            if (id >= 32768) {
                messageType.postDecode()
            }

            messageTypeCache.put(messageType, id.toLong())
        }
        return messageType
    }

    companion object {
        @JvmStatic
        fun decodeQuickchatMessage(buffer: Packet): QuickChatMessage {
            val message = QuickChatMessage()
            message.id = buffer.readUShort()
            message.message = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(message.id)
            return message
        }
    }
}
