package com.jagex.game.runetek5.js5.request.remote

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.js5.request.Js5NetQueue
import com.jagex.game.runetek5.network.Connection
import com.jagex.game.runetek5.utils.Utils
import java.io.IOException
import kotlin.math.min

class RemoteFileRequester : Js5NetQueue() {

    @JvmField
    var updateServerConnection: Connection? = null

    private fun dropWaitingRequests() {
        var request: Js5NetQueueRequest?
        while (super.waitingPriorityRequests.getNext().also { request = it as Js5NetQueueRequest? } != null) {
            request!!.unlinkSub() // Remove from queue
            request!!.unlink()     // Remove from RemoteFileWorker.pendingRequests
        }
        while (super.waitingRegularRequests.getNext().also { request = it as Js5NetQueueRequest? } != null) {
            request!!.unlinkSub() // Remove from queue
            request!!.unlink()     // Remove from RemoteFileWorker.pendingRequests
        }
        lastRemoteRequest = null
    }

    @Synchronized
    override fun init(connection: Connection, loggedIn: Boolean) {
        if (this.updateServerConnection != null) {
            try {
                this.updateServerConnection!!.close()
            } catch (ignored: Exception) {
                // NOOP
            }
            this.updateServerConnection = null
        }

        this.updateServerConnection = connection
        initializeSession()
        sendLoginState(loggedIn)
        incoming.currentPosition = 0
        lastRemoteRequest = null

        while (true) {
            var request = super.waitingPriorityRequests.getFirst() as Js5NetQueueRequest?
            if (request == null) {
                while (true) {
                    request = super.waitingRegularRequests.getFirst() as Js5NetQueueRequest?
                    if (request == null) {
                        if (encryption.toInt() != 0) {
                            try {
                                outgoing.currentPosition = 0
                                outgoing.writeByte(4)
                                outgoing.writeByte(encryption.toInt())
                                outgoing.writeInt(0)
                                this.updateServerConnection!!.write(outgoing.buffer, outgoing.buffer.size)
                            } catch (ioexception: IOException) {
                                try {
                                    this.updateServerConnection!!.close()
                                } catch (exception: Exception) {
                                    // NOOP
                                }
                                this.updateServerConnection = null
                                failedRequests++
                                connectionStatus = -2
                            }
                        }
                        super.lastConnectionInteraction = 0
                        super.cycleTime = Utils.time()
                        return
                    }
                    super.regularRequests.insert(request)
                }
            }
            super.priorityRequests.insert(request)
        }
    }

    override fun sendLoginState(loggedIn: Boolean) {
        if (updateServerConnection != null) {
            try {
                outgoing.currentPosition = 0
                outgoing.writeByte(if (loggedIn) 2 else 3)
                outgoing.write40BitLong(0L)
                updateServerConnection!!.write(outgoing.buffer, outgoing.buffer.size)
            } catch (ioexception: IOException) {
                try {
                    updateServerConnection!!.close()
                } catch (exception: Exception) {
                    // NOOP
                }
                updateServerConnection = null
                failedRequests++
                connectionStatus = -2
            }
        }
    }

    override fun requestTermination() {
        if (updateServerConnection != null) {
            try {
                outgoing.currentPosition = 0
                outgoing.writeByte(7)
                outgoing.write40BitLong(0L)
                updateServerConnection!!.write(outgoing.buffer, outgoing.buffer.size)
            } catch (ioexception: IOException) {
                try {
                    updateServerConnection!!.close()
                } catch (exception: Exception) {
                    // NOOP
                }
                updateServerConnection = null
                failedRequests++
                connectionStatus = -2
            }
        }
    }

    override fun closeStream() {
        if (updateServerConnection != null) {
            updateServerConnection!!.close()
        }
    }

    override fun dropConnection() {
        if (updateServerConnection != null) {
            updateServerConnection!!.drop()
        }
    }

    @Synchronized
    override fun disconnect() {
        try {
            updateServerConnection?.close()
        } catch (ignored: Exception) {
            // NOOP
        }
        updateServerConnection = null
        dropWaitingRequests()
        super.priorityRequests.reset()
        super.regularRequests.reset()
        failedRequests = 0
        connectionStatus = 0
        incoming.currentPosition = 0
    }

    /**
     * Sends a request to the update server to start
     * a new file update session with this client
     */
    @Synchronized
    override fun rejectPayload() {
        try {
            updateServerConnection?.close()
        } catch (ignored: Exception) {
            // NOOP
        }
        updateServerConnection = null
        failedRequests++
        connectionStatus = -1
    }

    fun initializeSession() {
        if (updateServerConnection != null) {
            try {
                outgoing.currentPosition = 0
                outgoing.writeByte(6)
                outgoing.writeMedium(3)
                outgoing.writeShort(0)
                updateServerConnection!!.write(outgoing.buffer, outgoing.buffer.size)
            } catch (ioexception: IOException) {
                try {
                    updateServerConnection!!.close()
                } catch (ignored: Exception) {
                    // NOOP
                }
                updateServerConnection = null
                failedRequests++
                connectionStatus = -2
            }
        }
    }

    /**
     * Performs a cycle communicating with the update server
     * @return All requests processed
     *
     * Holds the queue monitor for the whole cycle: the drain loops interleave queue
     * unlinks with socket writes, and the async map loader inserts requests (via
     * [Js5NetQueue.request]) concurrently. Reads are bounded by the socket's available
     * capacity, so the monitor is never held across a blocking read.
     */
    @Synchronized
    override fun pulse(): Boolean {
        if (updateServerConnection != null) {
            val currentTime = Utils.time()
            var timeDelta = (currentTime - super.cycleTime).toInt()
            super.cycleTime = currentTime
            if (timeDelta > 200) {
                timeDelta = 200
            }

            super.lastConnectionInteraction += timeDelta
            if (super.lastConnectionInteraction > 30000) {
                try {
                    updateServerConnection!!.close()
                } catch (ignored: Exception) {
                    // NOOP
                }
                updateServerConnection = null
            }
        }

        if (updateServerConnection == null) {
            return priorityRequestCount() == 0 && regularRequestCount() == 0
        }
        try {
            var priorityRequest = super.priorityRequests.getNext() as Js5NetQueueRequest?
            while (priorityRequest != null) {
                outgoing.currentPosition = 0
                outgoing.writeByte(1)
                outgoing.write40BitLong(priorityRequest.indexReference)
                updateServerConnection!!.write(outgoing.buffer, outgoing.buffer.size)
                super.waitingPriorityRequests.insert(priorityRequest)
                priorityRequest = super.priorityRequests.getDirectNext() as Js5NetQueueRequest?
            }

            var regularRequest = super.regularRequests.getNext() as Js5NetQueueRequest?
            while (regularRequest != null) {
                outgoing.currentPosition = 0
                outgoing.writeByte(0)
                outgoing.write40BitLong(regularRequest.indexReference)
                updateServerConnection!!.write(outgoing.buffer, outgoing.buffer.size)
                super.waitingRegularRequests.insert(regularRequest)
                regularRequest = super.regularRequests.getDirectNext() as Js5NetQueueRequest?
            }

            for (loop in 0 until 100) {
                val capacity = updateServerConnection!!.getInstreamCapacityLeft()
                if (capacity < 0) {
                    throw IOException()
                }

                if (capacity == 0) {
                    break
                }

                super.lastConnectionInteraction = 0
                var expectedHeaderSize: Byte = 0
                if (lastRemoteRequest == null) {
                    expectedHeaderSize = 10
                } else if (lastRemoteRequest!!.blockBytesRead == 0) {
                    expectedHeaderSize = 1
                }

                if (expectedHeaderSize > 0) {
                    var readByteCount = expectedHeaderSize - incoming.currentPosition
                    if (readByteCount > capacity) {
                        readByteCount = capacity
                    }

                    updateServerConnection!!.read(incoming.buffer, incoming.currentPosition, readByteCount)
                    if (encryption.toInt() != 0) {
                        for (dataIndex in 0 until readByteCount) {
                            incoming.buffer[dataIndex + incoming.currentPosition] = (incoming.buffer[dataIndex + incoming.currentPosition].toInt() xor encryption.toInt()).toByte()
                        }
                    }

                    incoming.currentPosition += readByteCount
                    if (incoming.currentPosition >= expectedHeaderSize) {
                        if (lastRemoteRequest == null) {
                            incoming.currentPosition = 0
                            val cacheIndex = incoming.readUByte()
                            val archiveId = incoming.readInt()
                            val bitpacketSettings = incoming.readUByte()
                            val payloadSize = incoming.readInt()
                            val compressed = bitpacketSettings and 0x7f
                            val regularRequest = (bitpacketSettings and 0x80) != 0
                            val reference = archiveId + (cacheIndex.toLong() shl 32)
                            var request: Js5NetQueueRequest?
                            if (regularRequest) {
                                request = super.waitingRegularRequests.getNext() as Js5NetQueueRequest?
                                while (request != null && request.indexReference != reference) {
                                    request = super.waitingRegularRequests.getDirectNext() as Js5NetQueueRequest?
                                }
                            } else {
                                request = super.waitingPriorityRequests.getNext() as Js5NetQueueRequest?
                                while (request != null && request.indexReference != reference) {
                                    request = super.waitingPriorityRequests.getDirectNext() as Js5NetQueueRequest?
                                }
                            }

                            if (request == null) {
                                throw IOException()
                            }

                            val headerSize = if (compressed == 0) 5 else 9
                            lastRemoteRequest = request
                            lastRemoteRequest!!.stream = Packet(lastRemoteRequest!!.padding + headerSize + payloadSize)
                            lastRemoteRequest!!.stream!!.writeByte(compressed)
                            lastRemoteRequest!!.stream!!.writeInt(payloadSize)
                            lastRemoteRequest!!.blockBytesRead = 10
                            incoming.currentPosition = 0
                        } else {
                            if (lastRemoteRequest!!.blockBytesRead != 0) {
                                throw IOException()
                            }

                            if (incoming.buffer[0] == (-1).toByte()) {
                                lastRemoteRequest!!.blockBytesRead = 1
                                incoming.currentPosition = 0
                            } else {
                                lastRemoteRequest = null
                            }
                        }
                    }
                } else {
                    val requestSize = lastRemoteRequest!!.stream!!.buffer.size - lastRemoteRequest!!.padding
                    var readByteCount = 512 - lastRemoteRequest!!.blockBytesRead
                    if (readByteCount > requestSize - lastRemoteRequest!!.stream!!.currentPosition) {
                        readByteCount = requestSize - lastRemoteRequest!!.stream!!.currentPosition
                    }

                    if (readByteCount > capacity) {
                        readByteCount = capacity
                    }

                    updateServerConnection!!.read(lastRemoteRequest!!.stream!!.buffer, lastRemoteRequest!!.stream!!.currentPosition, readByteCount)
                    if (encryption.toInt() != 0) {
                        for (dataIndex in 0 until readByteCount) {
                            lastRemoteRequest!!.stream!!.buffer[dataIndex + lastRemoteRequest!!.stream!!.currentPosition] =
                                (lastRemoteRequest!!.stream!!.buffer[dataIndex + lastRemoteRequest!!.stream!!.currentPosition].toInt() xor encryption.toInt()).toByte()
                        }
                    }

                    lastRemoteRequest!!.stream!!.currentPosition += readByteCount
                    lastRemoteRequest!!.blockBytesRead += readByteCount
                    if (lastRemoteRequest!!.stream!!.currentPosition == requestSize) {
                        lastRemoteRequest!!.unlinkSub()
                        lastRemoteRequest!!.waiting = false
                        lastRemoteRequest = null
                    } else if (lastRemoteRequest!!.blockBytesRead == 512) {
                        lastRemoteRequest!!.blockBytesRead = 0
                    }
                }
            }
            return true
        } catch (ioexception: IOException) {
            try {
                updateServerConnection!!.close()
            } catch (exception: Exception) {
                // NOOP
            }
            updateServerConnection = null
            failedRequests++
            connectionStatus = -2
            return priorityRequestCount() == 0 && regularRequestCount() == 0
        }
    }
}
