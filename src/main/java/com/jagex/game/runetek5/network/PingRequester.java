package com.jagex.game.runetek5.network;

import jaclib.ping.Ping;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;

public class PingRequester implements Runnable {

    public static int anInt5864;

    private static final int MAX_CONCURRENT_PINGS = 10;
    private final ExecutorService pingThreadPool;
    public final Object activeRequest = new Object();
    public final Queue<Object> requestQueue = new LinkedList<>();
    public Thread executingThread = new Thread(this);
    private final AtomicInteger activeOperations = new AtomicInteger(0);

    public static PingRequest CURRENT_REQUEST;
    public static long NEXT_PING_UPDATE_TO_SERVER;

    public PingRequester() {
        this.pingThreadPool = Executors.newFixedThreadPool(MAX_CONCURRENT_PINGS, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        this.executingThread.setDaemon(true);
        this.executingThread.start();
    }

    public PingRequest createPingRequest(String ip) {
        if (this.executingThread == null) {
            throw new IllegalStateException("PingRequester has been shut down");
        } else if (ip == null) {
            throw new IllegalArgumentException("IP address cannot be null");
        }
        PingRequest request = new PingRequest(ip);
        queueRequest(request);
        return request;
    }

    public void queueRequest(Object request) {
        synchronized (this.requestQueue) {
            this.requestQueue.add(request);
            this.requestQueue.notify();
        }
    }

    public void shutdown() {
        if (this.executingThread != null) {
            queueRequest(this.activeRequest);
            try {
                this.executingThread.join();
            } catch (InterruptedException ie) {
                // NOOP
            }
            this.executingThread = null;
            pingThreadPool.shutdown();
        }
    }

    @Override
    public void run() {
        while (true) {
            Object waitingRequest;

            synchronized (this.requestQueue) {
                for (waitingRequest = this.requestQueue.poll(); waitingRequest == null; waitingRequest = this.requestQueue.poll()) {
                    try {
                        this.requestQueue.wait();
                    } catch (InterruptedException ie) {
                        // NOOP
                    }
                }
                if (waitingRequest == this.activeRequest)
                    return;
            }

            if (activeOperations.get() < MAX_CONCURRENT_PINGS) {
                final PingRequest executedRequest = (PingRequest) waitingRequest;
                activeOperations.incrementAndGet();
                pingThreadPool.submit(() -> {
                    try {
                        processPingRequest(executedRequest);
                    } finally {
                        activeOperations.decrementAndGet();
                    }
                });
            } else {
                queueRequest(waitingRequest);
                try {
                    Thread.sleep(50); // Small delay to prevent CPU spinning
                } catch (InterruptedException e) {
                    // NOOP
                }
            }
        }
    }

    /**
     * Processes an individual ping request in a worker thread
     */
    private void processPingRequest(PingRequest request) {
        int ping;
        try {
            byte[] addr = InetAddress.getByName(request.ip).getAddress();
            ping = Ping.ping(addr[0], addr[1], addr[2], addr[3], 1000L);
        } catch (Throwable throwable) {
            ping = 1000;
        }
        request.ping = ping;
    }

    public static void keepalive(boolean force) {
        SoundEffectPlayer.updatePlayers();
        if (GameState.isRenderingScene(client.gameState)) {
            BufferedConnectionContext[] connectionContexts = client.connectionContexts;
            for (int index = 0; index < connectionContexts.length; index++) {
                BufferedConnectionContext connectionContext = connectionContexts[index];
                if (++connectionContext.lastFlush < 50 && !force) {
                    return;
                }
                connectionContext.lastFlush = 0;
                if (!connectionContext.dropped && connectionContext.getConnection() != null) {
                    TcpPacket clientPingPacket = PacketEncoder.createPacket(ClientProt.PING, connectionContext.isaac);
                    connectionContext.queuePacket(clientPingPacket);
                    try {
                        connectionContext.flush();
                    } catch (IOException ioe) {
                        connectionContext.dropped = true;
                    }
                }
            }
            SoundEffectPlayer.updatePlayers();
        }
    }
}