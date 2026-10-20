package com.jagex.game.runetek5.graphics.model.billboard

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.js5.Js5ResourceProvider

object BillboardTypeList {
    @JvmField val BILLBOARD_CACHE = LRUCache(64)
    @JvmField var BILLBOARD_RESOURCE: Js5ResourceProvider? = null

    @JvmStatic
    fun initialize(billboardsResource: Js5ResourceProvider) {
        BILLBOARD_RESOURCE = billboardsResource
    }

    @JvmStatic
    fun list(id: Int): BillboardType {
        var billboardType = BILLBOARD_CACHE[id.toLong()] as BillboardType?
        if (billboardType == null) {
            val billboardData = BILLBOARD_RESOURCE!!.getFile(0, id)
            billboardType = BillboardType()
            if (billboardData == null) {
                return billboardType
            }
            billboardType.decode(Packet(billboardData))
            BILLBOARD_CACHE.put(billboardType, id.toLong())
        }
        return billboardType
    }
}
