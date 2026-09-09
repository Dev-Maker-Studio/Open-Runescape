package com.jagex.game.runetek5.js5.request

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.js5.request.remote.Js5NetQueueRequest
import com.jagex.game.runetek5.network.Connection

/**
 * Network queue for JS5 file requests.
 *
 * @author Steven - 11/10/2023
 */
abstract class Js5NetQueue {

    /**
     * The amount of requests that had issues (e.g., can't get the data from the request).
     */
    @Volatile
    @JvmField var failedRequests: Int = 0

    @Volatile
    @JvmField var connectionStatus: Int = 0
    @JvmField protected var cycleTime: Long = 0
    @JvmField protected var lastConnectionInteraction: Int = 0
    @JvmField protected var lastRemoteRequest: Js5NetQueueRequest? = null
    @JvmField protected val priorityRequests: Queue<Js5NetQueueRequest> = Queue()
    @JvmField protected val waitingPriorityRequests: Queue<Js5NetQueueRequest> = Queue()
    @JvmField protected val regularRequests: Queue<Js5NetQueueRequest> = Queue()
    @JvmField protected val waitingRegularRequests: Queue<Js5NetQueueRequest> = Queue()
    @JvmField protected val outgoing: Packet = Packet(6)
    @JvmField protected var encryption: Byte = 0
    @JvmField protected val incoming: Packet = Packet(10)

    // The request queues are intrusive linked lists shared between the game thread
    // (pulse draining) and the async map loader thread (cache-miss inserts), so every
    // structural access is guarded by this instance's monitor.
    @Synchronized
    fun request(index: Int, fileId: Int, padding: Byte, hasPriority: Boolean): Js5NetQueueRequest {
        val indexReference = (index.toLong() shl 32) + fileId
        val request = Js5NetQueueRequest()
        request.indexReference = indexReference
        request.padding = padding
        request.highPriority = hasPriority
        if (hasPriority) {
            if (priorityRequestCount() >= 50) {
                throw RuntimeException()
            }
            priorityRequests.insert(request)
        } else {
            if (regularRequestCount() >= 20) {
                throw RuntimeException()
            }
            regularRequests.insert(request)
        }
        return request
    }

    fun regularRequestUnavailable(): Boolean {
        return regularRequestCount() >= 20
    }

    fun priorityRequestUnavailable(): Boolean {
        return priorityRequestCount() >= 50
    }

    @Synchronized
    protected fun regularRequestCount(): Int {
        return regularRequests.size() + waitingRegularRequests.size()
    }

    abstract fun pulse(): Boolean

    abstract fun init(connection: Connection, loggedIn: Boolean)

    abstract fun sendLoginState(loginState: Boolean)

    abstract fun closeStream()

    abstract fun requestTermination()

    abstract fun dropConnection()

    abstract fun disconnect()

    @Synchronized
    fun priorityRequestCount(): Int {
        return priorityRequests.size() + waitingPriorityRequests.size()
    }
}
