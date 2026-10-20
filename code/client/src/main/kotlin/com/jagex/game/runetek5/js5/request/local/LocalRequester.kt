package com.jagex.game.runetek5.js5.request.local

import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.network.packet.PacketEncoder

class LocalRequester : Runnable {

    val queue: Queue<LocalRequest> = Queue()

    var executingThread: Thread? = Thread(this)

    @JvmField
    var requestCount: Int = 0

    @Volatile
    var shutdown: Boolean = false

    init {
        executingThread?.isDaemon = true
        executingThread?.start()
        executingThread?.priority = Thread.MIN_PRIORITY
    }

    override fun run() {
        while (!shutdown) {
            val requestNode: LocalRequest
            synchronized(queue) {
                requestNode = queue.getFirst() as? LocalRequest ?: run {
                    try {
                        (queue as Object).wait()
                    } catch (e: InterruptedException) {
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
                            requestNode.payload!!,
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

    fun createNormalRequest(fileId: Int, cacheFile: Decompressor): LocalRequest {
        val request = LocalRequest()
        request.type = 3
        request.indexReference = fileId.toLong()
        request.cacheFile = cacheFile
        request.highPriority = false
        addRequest(request)
        return request
    }

    fun addRequest(request: LocalRequest) {
        synchronized(queue) {
            queue.insert(request)
            requestCount++
            (queue as Object).notifyAll()
        }
    }

    fun shutdown() {
        shutdown = true
        synchronized(queue) {
            (queue as Object).notifyAll()
        }
        try {
            executingThread?.join()
        } catch (e: InterruptedException) {
        }
        executingThread = null
    }
}
