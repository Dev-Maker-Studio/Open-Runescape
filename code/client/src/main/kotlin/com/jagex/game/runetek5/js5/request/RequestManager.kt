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

class RequestManager(
    val remoteRequester: Js5NetQueue,

    val localRequester: LocalRequester
) {
    var remoteFileWorkers: Array<RemoteFileWorker?>? = null

    var masterIndexRequest: Js5NetQueueRequest? = null

    var masterIndexBuffer: Packet? = null

    val updateServerExponent: BigInteger? = Loader.RSA_PUBLIC_EXPONENT
    val updateServerModulus: BigInteger? = Loader.RSA_PUBLIC_MODULUS

    init {
        if (!remoteRequester.priorityRequestUnavailable()) {
            masterIndexRequest = remoteRequester.request(255, 255, 0.toByte(), true)
        }
    }

    fun pulse() {
        val workers = remoteFileWorkers ?: return

        for (index in workers.indices) {
            workers[index]?.processPrefetch()
        }
        for (index in workers.indices) {
            workers[index]?.processGeneralRequests()
        }
    }

    fun initialize(): Boolean {
        if (masterIndexBuffer != null) {
            return true
        }

        if (masterIndexRequest == null) {
            if (remoteRequester.priorityRequestUnavailable()) {
                return false
            }
            masterIndexRequest = remoteRequester.request(255, 255, 0.toByte(), true)
        }

        if (masterIndexRequest!!.waiting) {
            return false
        }

        val referenceTableBuffer = Packet(masterIndexRequest!!.getPayload())
        referenceTableBuffer.currentPosition = 5
        val numIndices = referenceTableBuffer.readUByte()
        referenceTableBuffer.currentPosition += numIndices * 72
        val rsaEncrypted = ByteArray(referenceTableBuffer.buffer.size - referenceTableBuffer.currentPosition)
        referenceTableBuffer.readBytes(rsaEncrypted, 0, rsaEncrypted.size)

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

    fun getWorker(index: Int, dataFile: Decompressor?, masterIndex: Decompressor?): RemoteFileWorker {
        return getRemoteFileWorker(index, dataFile, masterIndex)
    }

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
