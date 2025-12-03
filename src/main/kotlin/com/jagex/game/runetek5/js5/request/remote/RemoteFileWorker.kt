package com.jagex.game.runetek5.js5.request.remote

import com.jagex.game.runetek5.js5.request.RequestStatus.Companion.PRIORITY
import com.jagex.game.runetek5.js5.request.RequestStatus.Companion.REGULAR
import com.jagex.game.runetek5.js5.request.RequestStatus.Companion.RECEIVED
import java.util.zip.CRC32
import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.js5.Js5MasterIndex
import com.jagex.game.runetek5.js5.Whirlpool
import com.jagex.game.runetek5.js5.request.Js5WorkerRequestMessage
import com.jagex.game.runetek5.js5.request.Js5NetQueue
import com.jagex.game.runetek5.js5.request.FileWorker
import com.jagex.game.runetek5.js5.request.local.LocalRequest
import com.jagex.game.runetek5.js5.request.local.LocalRequester
import com.jagex.game.runetek5.utils.Utils

class RemoteFileWorker(
    val indexId: Int,
    val cacheDataFile: Decompressor?,
    val masterIndexFile: Decompressor?,
    val standardRequester: Js5NetQueue,
    val localRequester: LocalRequester,
    val tableCrc: Int,
    val tableDigest: ByteArray,
    val tableRevision: Int
) : FileWorker() {

    var masterIndexRequest: Js5WorkerRequestMessage? = null

    private var masterIndex: Js5MasterIndex? = null

    var fileRequestStatuses: ByteArray? = null

    var filesSuccessfulyReceived: Int = 0

    var hasRegularRequests: Boolean = false

    var generalRequests: NodeCollection? = null

    val pendingRequests: IterableNodeMap<Js5WorkerRequestMessage> = IterableNodeMap(16)

    var requestedFileId: Int = 0

    val prefetchEntries: NodeCollection = NodeCollection()

    var cleanupCycle: Long = 0

    var hasPriorityRequests: Boolean = false

    val initialized: Boolean = true

    init {
        hasPriorityRequests = if (cacheDataFile == null) {
            false
        } else {
            generalRequests = NodeCollection()
            true
        }
        if (masterIndexFile != null) {
            masterIndexRequest = localRequester.createPriorityRequest(indexId, masterIndexFile)
        }
    }

    override fun getProgress(fileId: Int): Int {
        val fileRequest = pendingRequests[fileId.toLong()]
        return fileRequest?.getCompletion() ?: 0
    }

    override fun getMasterIndex(): Js5MasterIndex {
        if (masterIndex != null) {
            return masterIndex!!
        }
        if (masterIndexRequest == null) {
            if (standardRequester.priorityRequestUnavailable()) {
                throw RuntimeException("Priority request unavailable")
            }
            masterIndexRequest = standardRequester.request(255, indexId, 0.toByte(), true)
        }
        if (masterIndexRequest!!.waiting) {
            throw RuntimeException("Master index request waiting")
        }
        val referenceTablePayload = masterIndexRequest!!.getPayload()
        if (masterIndexRequest is LocalRequest) {
            /** ReferenceTable from disk **/
            if (referenceTablePayload == null) {
                throw RuntimeException()
            }
            masterIndex = Js5MasterIndex(referenceTablePayload, tableCrc, tableDigest)
            if (masterIndex!!.revision != tableRevision) {
                throw RuntimeException()
            }
        } else {
            /** ReferenceTable from update server **/
            if (referenceTablePayload == null) {
                throw RuntimeException()
            }
            masterIndex = Js5MasterIndex(referenceTablePayload, tableCrc, tableDigest)
            if (masterIndexFile != null) {
                localRequester.createStoreRequest(indexId, referenceTablePayload, masterIndexFile)
            }
        }
        masterIndexRequest = null
        if (cacheDataFile != null) {
            fileRequestStatuses = ByteArray(masterIndex!!.archiveCount)
            filesSuccessfulyReceived = 0
        }
        return masterIndex!!
    }

    override fun requestArchive(archiveId: Int): ByteArray? {
        val fileRequest = request(archiveId, 0)
        if (70291 == archiveId) {
            println("[RemoteFileWorker] $archiveId - $fileRequest")
        }
        if (fileRequest == null) {
            return null
        }
        val archivePayload = fileRequest.getPayload()
        fileRequest.unlink()
        return archivePayload
    }

    @Suppress("UNCHECKED_CAST")
    fun processPrefetch() {
        if (generalRequests != null && getMasterIndex() != null) {
            var prefetchEntry: Node<Any?>? = prefetchEntries.getFirst() as Node<Any?>?
            while (prefetchEntry != null) {
                val archiveId = prefetchEntry.pointer.toInt()
                if (archiveId >= 0 && archiveId < masterIndex!!.archiveCount && masterIndex!!.archiveFileCount[archiveId] != 0) {
                    if (fileRequestStatuses!![archiveId].toInt() == PRIORITY) {
                        request(archiveId, 1)
                    }
                    if (fileRequestStatuses!![archiveId].toInt() == REGULAR) {
                        request(archiveId, 2)
                    }
                    if (fileRequestStatuses!![archiveId].toInt() == RECEIVED) {
                        prefetchEntry.unlink()
                    }
                } else {
                    prefetchEntry.unlink()
                }
                prefetchEntry = prefetchEntries.next() as Node<Any?>?
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun prefetch(fileId: Int) {
        if (cacheDataFile != null) {
            var node: Node<Any?>? = prefetchEntries.getFirst() as Node<Any?>?
            while (node != null) {
                if (node.pointer == fileId.toLong()) {
                    return
                }
                node = prefetchEntries.next() as Node<Any?>?
            }
            val prefetchRequest = Node<Any?>()
            prefetchRequest.pointer = fileId.toLong()
            prefetchEntries.addFirst(prefetchRequest)
        }
    }

    fun getTotalArchives(): Int {
        return if (masterIndex == null) 0 else masterIndex!!.validArchiveCount
    }

    fun filesReceived(): Int {
        return filesSuccessfulyReceived
    }

    fun getReferenceTableCompletion(): Int {
        return if (getMasterIndex() == null) {
            if (masterIndexRequest == null) 0 else masterIndexRequest!!.getCompletion()
        } else {
            100
        }
    }

    fun reset() {
        if (cacheDataFile != null) {
            hasRegularRequests = true
            if (generalRequests == null) {
                generalRequests = NodeCollection()
            }
        }
    }

    /**
     *
     * @param actionType
     * [0 = local / standart requester priority req]
     * [1 = local requester normal request]
     * [2 = standart requester regular request]
     *
     */
    fun request(fileId: Int, actionType: Int): Js5WorkerRequestMessage? {
        var fileRequest: Js5WorkerRequestMessage? = pendingRequests[fileId.toLong()]
        if (fileRequest != null && actionType == 0 && !fileRequest.highPriority && fileRequest.waiting) {
            fileRequest.unlink()
            fileRequest = null
        }
        if (fileRequest == null) {
            when (actionType) {
                0 -> {
                    fileRequest = if (cacheDataFile != null && fileRequestStatuses!![fileId].toInt() != REGULAR) {
                        localRequester.createPriorityRequest(fileId, cacheDataFile)
                    } else {
                        if (standardRequester.priorityRequestUnavailable()) {
                            return null
                        }
                        standardRequester.request(indexId, fileId, 2.toByte(), true)
                    }
                }
                1 -> {
                    if (cacheDataFile == null) {
                        throw RuntimeException()
                    }
                    fileRequest = localRequester.createNormalRequest(fileId, cacheDataFile)
                }
                2 -> {
                    if (cacheDataFile == null) {
                        throw RuntimeException()
                    }
                    if (fileRequestStatuses!![fileId].toInt() != REGULAR) {
                        throw RuntimeException()
                    }
                    if (standardRequester.regularRequestUnavailable()) {
                        return null
                    }
                    fileRequest = standardRequester.request(indexId, fileId, 2.toByte(), false)
                }
                else -> throw RuntimeException()
            }
            pendingRequests.put(fileRequest, fileId.toLong())
        }
        if (fileRequest!!.waiting) {
            return null
        }
        val payload = fileRequest!!.getPayload()
        return if (fileRequest is LocalRequest) {
            try {
                if (payload != null && payload.size > 2) {
                    CRC32.reset()
                    CRC32.update(payload, 0, payload.size - 2)
                    val crc = CRC32.value.toInt()
                    if (crc != masterIndex!!.crcs[fileId]) {
                        throw RuntimeException()
                    }
                    val whirlpool = masterIndex!!.whirlpool
                    if (whirlpool != null && whirlpool[fileId] != null) {
                        val expectedWhirlpool = whirlpool[fileId]!!
                        val payloadWhirlpool = Whirlpool.calculateDigest(payload, 0, payload.size - 2)
                        for (idx in 0 until 64) {
                            if (payloadWhirlpool[idx] != expectedWhirlpool[idx]) {
                                throw RuntimeException()
                            }
                        }
                    }
                    val version = (payload[payload.size - 1].toInt() and 0xff) + ((payload[payload.size - 2].toInt() and 0xff) shl 8)
                    if (version != (masterIndex!!.versions[fileId] and 0xffff)) {
                        throw RuntimeException()
                    }
                    if (fileRequestStatuses!![fileId].toInt() != RECEIVED) {
                        filesSuccessfulyReceived++
                        fileRequestStatuses!![fileId] = RECEIVED.toByte()
                    }
                    if (!fileRequest!!.highPriority) {
                        fileRequest!!.unlink()
                    }
                    fileRequest
                } else {
                    throw RuntimeException()
                }
            } catch (exception: Exception) {
                fileRequestStatuses!![fileId] = REGULAR.toByte()
                fileRequest!!.unlink()
                if (fileRequest!!.highPriority && !standardRequester.priorityRequestUnavailable()) {
                    val request = standardRequester.request(indexId, fileId, 2.toByte(), true)
                    pendingRequests.put(request, fileId.toLong())
                }
                null
            }
        } else if (payload != null && payload.size > 2) {
            CRC32.reset()
            CRC32.update(payload, 0, payload.size - 2)
            val receivedCrc = CRC32.value.toInt()
            if (receivedCrc != masterIndex!!.crcs[fileId]) {
                throw RuntimeException()
            }
            val whirlpool = masterIndex!!.whirlpool
            if (whirlpool != null && whirlpool[fileId] != null) {
                val pool = whirlpool[fileId]!!
                val expectedWhirlpool = Whirlpool.calculateDigest(payload, 0, payload.size - 2)
                for (idx in 0 until 64) {
                    if (expectedWhirlpool[idx] != pool[idx]) {
                        throw RuntimeException()
                    }
                }
            }
            standardRequester.failedRequests = 0
            standardRequester.connectionStatus = 0
            payload[payload.size - 2] = (masterIndex!!.versions[fileId] ushr 8).toByte()
            payload[payload.size - 1] = masterIndex!!.versions[fileId].toByte()
            if (cacheDataFile != null) {
                localRequester.createStoreRequest(fileId, payload, cacheDataFile)
                if (fileRequestStatuses!![fileId].toInt() != RECEIVED) {
                    filesSuccessfulyReceived++
                    fileRequestStatuses!![fileId] = RECEIVED.toByte()
                }
            }
            if (!fileRequest!!.highPriority) {
                fileRequest!!.unlink()
            }
            fileRequest
        } else {
            throw RuntimeException()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun processGeneralRequests() {
        if (generalRequests != null) {
            if (getMasterIndex() == null) {
                return
            }
            if (hasPriorityRequests) {
                var completed = true
                var request: Node<Any?>? = generalRequests!!.getFirst() as Node<Any?>?
                while (request != null) {
                    val fileId = request.pointer.toInt()
                    if (fileRequestStatuses!![fileId].toInt() == PRIORITY) {
                        request(fileId, 1)
                    }
                    if (fileRequestStatuses!![fileId].toInt() != PRIORITY) {
                        request.unlink()
                    } else {
                        completed = false
                    }
                    request = generalRequests!!.next() as Node<Any?>?
                }
                while (requestedFileId < masterIndex!!.archiveFileCount.size) {
                    if (masterIndex!!.archiveFileCount[requestedFileId] != 0) {
                        if (localRequester.requestCount >= 250) {
                            completed = false
                            break
                        }
                        if (fileRequestStatuses!![requestedFileId].toInt() == PRIORITY) {
                            request(requestedFileId, 1)
                        }
                        if (fileRequestStatuses!![requestedFileId].toInt() == PRIORITY) {
                            val newRequest = Node<Any?>()
                            newRequest.pointer = requestedFileId.toLong()
                            generalRequests!!.addFirst(newRequest)
                            completed = false
                        }
                    }
                    requestedFileId++
                }
                if (completed) {
                    hasPriorityRequests = false
                    requestedFileId = 0
                }
            } else if (hasRegularRequests) {
                var completed = true
                var request: Node<Any?>? = generalRequests!!.getFirst() as Node<Any?>?
                while (request != null) {
                    val fileId = request.pointer.toInt()
                    if (fileRequestStatuses!![fileId].toInt() != RECEIVED) {
                        request(fileId, 2)
                    }
                    if (fileRequestStatuses!![fileId].toInt() == RECEIVED) {
                        request.unlink()
                    } else {
                        completed = false
                    }
                    request = generalRequests!!.next() as Node<Any?>?
                }
                while (requestedFileId < masterIndex!!.archiveFileCount.size) {
                    if (masterIndex!!.archiveFileCount[requestedFileId] != 0) {
                        if (standardRequester.regularRequestUnavailable()) {
                            completed = false
                            break
                        }
                        if (fileRequestStatuses!![requestedFileId].toInt() != RECEIVED) {
                            request(requestedFileId, 2)
                        }
                        if (fileRequestStatuses!![requestedFileId].toInt() != RECEIVED) {
                            val newRequest = Node<Any?>()
                            newRequest.pointer = requestedFileId.toLong()
                            generalRequests!!.addFirst(newRequest)
                            completed = false
                        }
                    }
                    requestedFileId++
                }
                if (completed) {
                    hasRegularRequests = false
                    requestedFileId = 0
                }
            } else {
                generalRequests = null
            }
        }

        /** Cleanup high priority requests **/
        if (initialized && Utils.time() >= cleanupCycle) {
            var request: Js5WorkerRequestMessage? = pendingRequests.first()
            while (request != null) {
                if (!request.waiting) {
                    if (request.isCompleted) {
                        if (!request.highPriority) {
                            throw RuntimeException()
                        }
                        request.unlink()
                    } else {
                        request.isCompleted = true
                    }
                }
                request = pendingRequests.next()
            }
            cleanupCycle = Utils.time() + 1000L
        }
    }

    fun currentRequestId(): Int {
        if (masterIndex == null) {
            return 0
        }
        if (!hasPriorityRequests) {
            return masterIndex!!.validArchiveCount
        }
        val currentRequest = generalRequests!!.getFirst()
        return currentRequest?.pointer?.toInt() ?: 0
    }

    companion object {
        @JvmStatic
        val CRC32 = CRC32()
    }
}
