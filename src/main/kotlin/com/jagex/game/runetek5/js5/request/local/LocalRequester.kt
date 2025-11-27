package com.jagex.game.runetek5.js5.request.local

import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.network.packet.PacketEncoder

/**
 * Handles local file fetching and storage with multi-threaded request processing.
 *
 * Fully refactored
 * @author Brainpower - 21/05/2020
 */
class LocalRequester : Runnable {

    /**
     * The queue holding all multi-threaded cache requests
     */
    val queue: Queue<LocalRequest> = Queue()

    /**
     * The request executing thread
     */
    var executingThread: Thread? = Thread(this)

    /**
     * The amount of requests in our [queue]
     */
    @JvmField
    var requestCount: Int = 0

    /**
     * Has shutdown been requested
     */
    @Volatile
    var shutdown: Boolean = false

    init {
        executingThread?.isDaemon = true
        executingThread?.start()
        executingThread?.priority = Thread.MIN_PRIORITY
    }

    /**
     * The main threaded cycle which stores / fetches files
     */
    override fun run() {
        while (!shutdown) {
            val requestNode: LocalRequest
            synchronized(queue) {
                requestNode = queue.getFirst() as? LocalRequest ?: run {
                    try {
                        (queue as Object).wait()
                    } catch (e: InterruptedException) {
                        // NOOP
                    }
                    continue
                }
                requestCount--
            }

            try {
                when (requestNode.type) {
                    2 -> {
                        requestNode.cacheFile?.put(
                            requestNode.indexReference.toInt(),
                            requestNode.payload,
                            requestNode.payload!!.size
                        )
                    }
                    3 -> {
                        requestNode.payload = requestNode.cacheFile?.decompress(requestNode.indexReference.toInt())
                    }
                }
            } catch (exception: Exception) {
                PacketEncoder.sendErrorToServer(null, exception)
            }

            requestNode.waiting = false
        }
    }

    /**
     * (Blocking call)
     * Requests a directly needed file to be loaded into memory
     */
    fun createPriorityRequest(indexReference: Int, cacheFile: Decompressor): LocalRequest {
        val node = LocalRequest()
        node.type = 1

        synchronized(queue) {
            var anotherNode = queue.getNext() as? LocalRequest
            while (anotherNode != null) {
                if (indexReference.toLong() == anotherNode.indexReference &&
                    cacheFile === anotherNode.cacheFile &&
                    anotherNode.type == 2
                ) {
                    node.payload = anotherNode.payload
                    node.waiting = false
                    return node
                }
                anotherNode = queue.getDirectNext() as? LocalRequest
            }
        }

        node.payload = cacheFile.decompress(indexReference)
        node.waiting = false
        node.highPriority = true
        return node
    }

    /**
     * (Non blocking call)
     * Requests the payload from an already received file to be stored in our cache
     */
    fun createStoreRequest(key: Int, payload: ByteArray, cacheFile: Decompressor): LocalRequest {
        val request = LocalRequest()
        request.type = 2
        request.indexReference = key.toLong()
        request.payload = payload
        request.cacheFile = cacheFile
        request.highPriority = false
        addRequest(request)
        return request
    }

    /**
     * (Non blocking call)
     * Requests an indirectly needed file to be loaded into memory
     */
    fun createNormalRequest(fileId: Int, cacheFile: Decompressor): LocalRequest {
        val request = LocalRequest()
        request.type = 3
        request.indexReference = fileId.toLong()
        request.cacheFile = cacheFile
        request.highPriority = false
        addRequest(request)
        return request
    }

    /**
     * Requests a file to be fetched from our cache on a separate thread
     */
    fun addRequest(request: LocalRequest) {
        synchronized(queue) {
            queue.insert(request)
            requestCount++
            (queue as Object).notifyAll()
        }
    }

    /**
     * Cleans up the queue and kills the executing thread
     */
    fun shutdown() {
        shutdown = true
        synchronized(queue) {
            (queue as Object).notifyAll()
        }
        try {
            executingThread?.join()
        } catch (e: InterruptedException) {
            // NOOP
        }
        executingThread = null
    }
}
