package com.jagex.game.runetek5.config.quickchat

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class QuickchatCategoryTypeList(
    @JvmField val messageResource: Js5ResourceProvider,
    @JvmField val menuResource: Js5ResourceProvider
) {

    @JvmField val categoryCache: LRUCache = LRUCache(64)

    init {
        if (this.messageResource != null) {
            this.messageResource.getArchiveFileCount(0)
        }
        if (this.menuResource != null) {
            this.menuResource.getArchiveFileCount(0)
        }
    }

    fun list(categoryId: Int): QuickChatCategoryType {
        var categoryType = this.categoryCache.get(categoryId.toLong()) as QuickChatCategoryType?
        if (categoryType == null) {
            val data: ByteArray? = if (categoryId >= 32768) {
                this.menuResource.getFile(0, categoryId and 0x7fff)
            } else {
                this.messageResource.getFile(0, categoryId)
            }
            categoryType = QuickChatCategoryType()
            if (data != null) {
                categoryType.decode(Packet(data))
            }
            if (categoryId >= 32768) {
                categoryType.postDecode()
            }
            this.categoryCache.put(categoryType, categoryId.toLong())
        }
        return categoryType
    }
}
