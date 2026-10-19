package com.jagex.game.runetek5.js5.request.remote

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.js5.request.Js5WorkerRequestMessage

/**
 * A request to fetch a file from the update server
 * @author Brainpower - 21/05/2020
 *
 */
class Js5NetQueueRequest : Js5WorkerRequestMessage() {

    @JvmField var padding: Byte = 0

    /**
     * The request processing status
     */
    @JvmField var blockBytesRead: Int = 0

    /**
     * The stream holding the data of the request
     */
    @JvmField var stream: Packet? = null

    override fun getPayload(): ByteArray {
        if (!super.waiting && this.stream!!.currentPosition >= this.stream!!.buffer.size - this.padding) {
            return this.stream!!.buffer
        }
        throw RuntimeException()
    }

    override fun getCompletion(): Int {
        return if (this.stream == null) 0 else this.stream!!.currentPosition * 100 / (this.stream!!.buffer.size - this.padding)
    }
}
