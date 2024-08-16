package com.jagex.game.runetek5.network;

import jaclib.ping.Ping;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;

import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;

public class PingRequester implements Runnable {

    public static int anInt5864;

    public final Object activeRequest = new Object();
    public final Queue<Object> requestQueue = new LinkedList<Object>();
    public Thread executingThread = new Thread(this);

	public static PingRequest CURRENT_REQUEST;

	public static long NEXT_PING_UPDATE_TO_SERVER;

	public PingRequester() {
        this.executingThread.setDaemon(true);
        this.executingThread.start();
    }

    public PingRequest createPingRequest(String ip) {
        if (this.executingThread == null) {
            throw new IllegalStateException("");
        } else if (ip == null) {
            throw new IllegalArgumentException("");
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
        }
    }

    @Override
    public void run() {
        while (true) {
            PingRequest executedRequest;
            synchronized (this.requestQueue) {
                Object waitingRequest;
                for (waitingRequest = this.requestQueue.poll(); waitingRequest == null; waitingRequest = this.requestQueue.poll()) {
                    try {
                        this.requestQueue.wait();
                    } catch (InterruptedException ie) {
                    	// NOOP
                    }
                }
                if (waitingRequest == this.activeRequest) {
                    return;
                }
                executedRequest = (PingRequest) waitingRequest;
            }
            int ping;
            try {
                byte[] addr = InetAddress.getByName(executedRequest.ip).getAddress();
                ping = Ping.ping(addr[0], addr[1], addr[2], addr[3], 1000L);
            } catch (Throwable throwable) {
                ping = 1000;
            }
            executedRequest.ping = ping;
        }
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
