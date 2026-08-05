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
                // File has not streamed in yet. Return the blank default WITHOUT
                // caching it: an undecoded BillboardType has hasUid = false, and
                // callers bake that answer into immutable geometry (ModelGL's
                // renderedFaceCount truncation, ModernModelBuilder's hidden-face
                // set). Caching it pins "this face is not a billboard anchor" for
                // the rest of the session, so the anchor face rasterizes as a flat
                // tinted quad — the floating green quads. Not caching lets the next
                // model build pick up the real type once the file lands.
                return billboardType
            }
            billboardType.decode(Packet(billboardData))
            BILLBOARD_CACHE.put(billboardType, id.toLong())
        }
        return billboardType
    }
}
