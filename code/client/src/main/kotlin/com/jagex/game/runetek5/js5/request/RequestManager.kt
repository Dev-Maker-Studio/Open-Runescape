package com.jagex.game.runetek5.js5.request

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.js5.Whirlpool
import com.jagex.game.runetek5.js5.request.local.LocalRequester
import com.jagex.game.runetek5.js5.request.remote.Js5NetQueueRequest
import com.jagex.game.runetek5.js5.request.remote.RemoteFileWorker
import java.math.BigInteger
import java.util.*

/**
 * < Fully refactored >
 * @author Brainpower - 22/05/2020
 *
 * Manages remote and local file requests
 *
 */
class RequestManager(
    /**
     * The requester fetching remote files, from the update server
     */
    val remoteRequester: Js5NetQueue,

    /**
     * The requester fetching local files, directly from the cache
     */
    val localRequester: LocalRequester
) {
    /**
     * The workers retrieving the payload from requested files (one worker per index)
     */
    var remoteFileWorkers: Array<RemoteFileWorker?>? = null

    /**
     * The master index (ReferenceTable index 255) request
     */
    var masterIndexRequest: Js5NetQueueRequest? = null

    /**
     * The master index (ReferenceTable index 255) buffer
     */
    var masterIndexBuffer: Packet? = null

    /**
     * The RSA public key pair for decrypting data sent from the update server
     */
    val updateServerExponent: BigInteger? = Loader.RSA_PUBLIC_EXPONENT
    val updateServerModulus: BigInteger? = Loader.RSA_PUBLIC_MODULUS

    init {
        if (!remoteRequester.priorityRequestUnavailable()) {
            masterIndexRequest = remoteRequester.request(255, 255, 0.toByte(), true)
        }
    }

    /**
     * Proceeds the request cycle
     */
    fun pulse() {
        val workers = remoteFileWorkers ?: return

        for (index in workers.indices) {
            workers[index]?.processPrefetch()
        }
        for (index in workers.indices) {
            workers[index]?.processGeneralRequests()
        }
    }

    /**
     * Load the reference from the server and prepare the remote file workers
     */
    fun initialize(): Boolean {
        if (masterIndexBuffer != null) {
            return true
        }

        /** Request the master index to identify which files we do have, do not have or which are outdated **/
        if (masterIndexRequest == null) {
            if (remoteRequester.priorityRequestUnavailable()) {
                return false
            }
            masterIndexRequest = remoteRequester.request(255, 255, 0.toByte(), true)
        }

        if (masterIndexRequest!!.waiting) {
            return false
        }

        /** Read and strip the header from the master index payload **/
        val referenceTableBuffer = Packet(masterIndexRequest!!.getPayload())
        referenceTableBuffer.currentPosition = 5
        val numIndices = referenceTableBuffer.readUByte()
        referenceTableBuffer.currentPosition += numIndices * 72
        val rsaEncrypted = ByteArray(referenceTableBuffer.buffer.size - referenceTableBuffer.currentPosition)
        referenceTableBuffer.readBytes(rsaEncrypted, 0, rsaEncrypted.size)

        /** Decrypt the stripped payload with the RSA key pair **/
        val rsaDecrypted: ByteArray = if (updateServerExponent != null && updateServerModulus != null) {
            val encryptedReferenceTable = BigInteger(rsaEncrypted)
            val decryptedReferenceTable = encryptedReferenceTable.modPow(updateServerExponent, updateServerModulus)
            decryptedReferenceTable.toByteArray()
        } else {
            rsaEncrypted
        }

        if (rsaDecrypted.size != 65 && rsaDecrypted.size != 64) {
            println("Invalid length: ${rsaDecrypted.size}")
            println(Arrays.toString(rsaDecrypted))
            throw RuntimeException()
        }

        /** Decrypt the stripped payload with whirlpool **/
        val digest = Whirlpool.calculateDigest(
            referenceTableBuffer.buffer,
            5,
            referenceTableBuffer.currentPosition - rsaEncrypted.size - 5
        )

        for (index in 0..63) {
            val offset = if (rsaDecrypted.size == 65) 1 else 0
            if (digest[index] != rsaDecrypted[index + offset]) {
                throw RuntimeException()
            }
        }

        remoteFileWorkers = arrayOfNulls(numIndices)
        masterIndexBuffer = referenceTableBuffer
        return true
    }

    /**
     * Gets a worker used to create the ResourceProvider
     */
    fun getWorker(index: Int, dataFile: Decompressor?, masterIndex: Decompressor?): RemoteFileWorker {
        return getRemoteFileWorker(index, dataFile, masterIndex)
    }

    /**
     * Loads a remote file worker, used for requesting files from the update server
     */
    fun getRemoteFileWorker(indexId: Int, dataFile: Decompressor?, masterIndex: Decompressor?): RemoteFileWorker {
        val buffer = masterIndexBuffer ?: throw RuntimeException()
        val workers = remoteFileWorkers ?: throw RuntimeException()

        if (indexId < 0 || indexId >= workers.size) {
            throw RuntimeException()
        }

        workers[indexId]?.let { return it }

        buffer.currentPosition = indexId * 72 + 6
        val tableCrc = buffer.readInt()
        val tableRevision = buffer.readInt()
        val tableDigest = ByteArray(64)
        buffer.readBytes(tableDigest, 0, 64)

        val fileWorker = RemoteFileWorker(
            indexId,
            dataFile,
            masterIndex,
            remoteRequester,
            localRequester,
            tableCrc,
            tableDigest,
            tableRevision
        )

        workers[indexId] = fileWorker
        return fileWorker
    }
}
