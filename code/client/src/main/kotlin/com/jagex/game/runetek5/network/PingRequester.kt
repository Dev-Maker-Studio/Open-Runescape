package com.jagex.game.runetek5.network

import jaclib.ping.Ping
import java.io.IOException
import java.net.InetAddress
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer

class PingRequester : Runnable {

    private val pingThreadPool: ExecutorService
    val activeRequest = Any()
    val requestQueue: Queue<Any> = LinkedList()
    var executingThread: Thread? = Thread(this)
    private val activeOperations = AtomicInteger(0)

    init {
        pingThreadPool = Executors.newFixedThreadPool(MAX_CONCURRENT_PINGS) { r ->
            val thread = Thread(r)
            thread.isDaemon = true
            thread
        }
        executingThread!!.isDaemon = true
        executingThread!!.start()
    }

    fun createPingRequest(ip: String?): PingRequest {
        if (executingThread == null) {
            throw IllegalStateException("PingRequester has been shut down")
        } else if (ip == null) {
            throw IllegalArgumentException("IP address cannot be null")
        }
        val request = PingRequest(ip)
        queueRequest(request)
        return request
    }

    fun queueRequest(request: Any) {
        synchronized(requestQueue) {
            requestQueue.add(request)
            (requestQueue as Object).notify()
        }
    }

    fun shutdown() {
        if (executingThread != null) {
            queueRequest(activeRequest)
            try {
                executingThread!!.join()
            } catch (ie: InterruptedException) {
            }
            executingThread = null
            pingThreadPool.shutdown()
        }
    }

    override fun run() {
        while (true) {
            var waitingRequest: Any?

            synchronized(requestQueue) {
                waitingRequest = requestQueue.poll()
                while (waitingRequest == null) {
                    try {
                        (requestQueue as Object).wait()
                    } catch (ie: InterruptedException) {
                    }
                    waitingRequest = requestQueue.poll()
                }
                if (waitingRequest == activeRequest)
                    return
            }

            if (activeOperations.get() < MAX_CONCURRENT_PINGS) {
                val executedRequest = waitingRequest as PingRequest
                activeOperations.incrementAndGet()
                pingThreadPool.submit {
                    try {
                        processPingRequest(executedRequest)
                    } finally {
                        activeOperations.decrementAndGet()
                    }
                }
            } else {
                queueRequest(waitingRequest!!)
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                }
            }
        }
    }

    private fun processPingRequest(request: PingRequest) {
        val ping: Int
        try {
            val addr = InetAddress.getByName(request.ip).address
            ping = Ping.ping(addr[0], addr[1], addr[2], addr[3], 1000L)
        } catch (throwable: Throwable) {
            request.ping = 1000
            return
        }
        request.ping = ping
    }

    companion object {
        @JvmField
        var cameraTargetAbsX: Int = 0

        private const val MAX_CONCURRENT_PINGS = 10

        @JvmField
        var CURRENT_REQUEST: PingRequest? = null

        @JvmField
        var NEXT_PING_UPDATE_TO_SERVER: Long = 0

        @JvmStatic
        fun keepalive(force: Boolean) {
            SoundEffectPlayer.updatePlayers()
            if (GameState.isRenderingScene(Client.gameState)) {
                val connectionContexts = Client.connectionContexts
                for (index in connectionContexts.indices) {
                    val connectionContext = connectionContexts[index]
                    if (++connectionContext.lastFlush < 50 && !force) {
                        return
                    }
                    connectionContext.lastFlush = 0
                    if (!connectionContext.dropped && connectionContext.getConnection() != null) {
                        val clientPingPacket = PacketEncoder.createPacket(ClientProt.PING, connectionContext.isaac)
                        connectionContext.queuePacket(clientPingPacket)
                        try {
                            connectionContext.flush()
                        } catch (ioe: IOException) {
                            connectionContext.dropped = true
                        }
                    }
                }
                SoundEffectPlayer.updatePlayers()
            }
        }
    }
}
