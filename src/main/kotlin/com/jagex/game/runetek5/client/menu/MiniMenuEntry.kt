package com.jagex.game.runetek5.client.menu

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

class MiniMenuEntry(
    @JvmField val action: String,
    @JvmField val coloredName: String,
    @JvmField val cursor: Int,
    clickType: Int,
    @JvmField val itemId: Int,
    @JvmField val entityUid: Long,
    @JvmField val param0: Int,
    @JvmField val param1: Int,
    @JvmField val isSelectable: Boolean,
    @JvmField val showOtherLevelIcon: Boolean,
    @JvmField val bitpacked: Long,
    @JvmField val isUncategorized: Boolean
) : CacheableNode() {

    @JvmField var targetName: String? = null
    @JvmField var clickType: Int = clickType
}
