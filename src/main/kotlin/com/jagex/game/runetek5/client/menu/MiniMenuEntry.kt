package com.jagex.game.runetek5.client.menu

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

class MiniMenuEntry(
    @JvmField val action: String,
    @JvmField val coloredName: String,
    @JvmField val cursor: Int,
    clickType: Int,
    @JvmField val itemId: Int,
    @JvmField val entityUid: Long,
    @JvmField val anInt9581: Int,
    @JvmField val anInt9582: Int,
    @JvmField val aBool9583: Boolean,
    @JvmField val aBool9577: Boolean,
    @JvmField val bitpacked: Long,
    @JvmField val aBool9586: Boolean
) : CacheableNode() {

    @JvmField var aString9585: String? = null
    @JvmField var clickType: Int = clickType
}
