package com.jagex.game.runetek5.config.varc

import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class VarcStringTypeList(
    @JvmField val configResource: Js5ResourceProvider
) {
    @JvmField
    val size: Int

    init {
        LRUCache(64)
        size = configResource.getArchiveFileCount(SharedConfigsType.VARC_STRING.id)
    }
}
