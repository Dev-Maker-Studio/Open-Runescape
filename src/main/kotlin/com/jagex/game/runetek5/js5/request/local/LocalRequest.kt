package com.jagex.game.runetek5.js5.request.local

import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.js5.request.Js5WorkerRequestMessage

/**
 * Represents a request to fetch the reference table from the update server
 * @author Brainpower - 21/05/2020
 */
class LocalRequest : Js5WorkerRequestMessage() {

    @JvmField
    var cacheFile: Decompressor? = null

    @JvmField
    var type: Int = 0

    @JvmField
    var payload: ByteArray? = null

    override fun getCompletion(): Int {
        return if (waiting) 0 else 100
    }

    override fun getPayload(): ByteArray? {
        if (waiting)
            throw RuntimeException()
        return payload
    }
}
