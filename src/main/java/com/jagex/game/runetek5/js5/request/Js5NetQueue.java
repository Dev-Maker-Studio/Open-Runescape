package com.jagex.game.runetek5.js5.request;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.js5.request.remote.Js5NetQueueRequest;
import com.jagex.game.runetek5.network.Connection;

/**
 * [J]
 * @author Steven - 11/10/2023
 *
 */
public abstract class Js5NetQueue {

	/**
	 * The amount of requests that had issues
	 * EG: can't get the data from the request
	 */
    public volatile int failedRequests;
    
    /**
     * 
     */
    public volatile int connectionStatus;
    protected long cycleTime;
    protected int lastConnectionInteraction;
    protected Js5NetQueueRequest lastRemoteRequest;
    protected final Queue<Js5NetQueueRequest> priorityRequests = new Queue<Js5NetQueueRequest>();
    protected final Queue<Js5NetQueueRequest> waitingPriorityRequests = new Queue<Js5NetQueueRequest>();
    protected final Queue<Js5NetQueueRequest> regularRequests = new Queue<Js5NetQueueRequest>();
    protected final Queue<Js5NetQueueRequest> waitingRegularRequests = new Queue<Js5NetQueueRequest>();
    protected final Packet outgoing = new Packet(6);
    protected byte encryption;
    protected final Packet incoming = new Packet(10);

    public Js5NetQueueRequest request(int index, int fileId, byte padding, boolean hasPriority) {
        long indexReference = ((long) index << 32) + fileId;
        Js5NetQueueRequest request = new Js5NetQueueRequest();
        request.indexReference = indexReference;
        request.padding = padding;
        request.highPriority = hasPriority;
        if (hasPriority) {
            if (priorityRequestCount() >= 50) {
                throw new RuntimeException();
            }
            this.priorityRequests.insert(request);
        } else {
            if (regularRequestCount() >= 20) {
                throw new RuntimeException();
            }
            this.regularRequests.insert(request);
        }
        return request;
    }

    public boolean regularRequestUnavailable() {
        return regularRequestCount() >= 20;
    }

    public boolean priorityRequestUnavailable() {
        return priorityRequestCount() >= 50;
    }

    protected int regularRequestCount() {
        return this.regularRequests.size() + this.waitingRegularRequests.size();
    }

    public abstract boolean pulse();

    public abstract void init(Connection connection, boolean loggedIn);

    public abstract void sendLoginState(boolean loginState);

    public abstract void closeStream();

    public abstract void requestTermination();

    public abstract void dropConnection();

    public int priorityRequestCount() {
        return this.priorityRequests.size() + this.waitingPriorityRequests.size();
    }
}
