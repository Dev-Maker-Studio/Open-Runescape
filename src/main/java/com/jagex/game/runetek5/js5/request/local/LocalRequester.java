package com.jagex.game.runetek5.js5.request.local;

import com.jagex.game.runetek5.cache.Decompressor;
import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.network.packet.PacketEncoder;

/**
 * < Fully refactored >
 * @author Brainpower - 21/05/2020
 * 
 * Handles local file fetching and storage
 */
public class LocalRequester implements Runnable {

	/**
	 * The queue holding all multi threaded cache requests
	 */
    public final Queue<LocalRequest> queue = new Queue<LocalRequest>();
    
    /**
     * The request executing thread
     */
    public Thread executingThread = new Thread(this);
    
    /**
     * The amount of requests in our {@link #queue}
     */
    public int requestCount;
    
    /**
     * Has shutdown been requested
     */
    public boolean shutdown;

    /**
     * Start the thread and set it's priority
     */
    public LocalRequester() {
        this.executingThread.setDaemon(true);
        this.executingThread.start();
        this.executingThread.setPriority(1);
    }

    /**
     * The main threaded cycle which stores / fetches files
     */
    @Override
    public void run() {
        while (!this.shutdown) {
            LocalRequest requestNode;
            synchronized (this.queue) {
                requestNode = (LocalRequest) this.queue.getFirst();
                if (requestNode == null) {
                    try {
                        this.queue.wait();
                    } catch (InterruptedException interruptedException) {
                    	// NOOP
                    }
                    continue;
                }
                this.requestCount--;
            }
            try {
                if (requestNode.type == 2) {
                    requestNode.cacheFile.put((int) requestNode.indexReference, requestNode.payload, requestNode.payload.length);
                } else if (requestNode.type == 3) {
                    requestNode.payload = requestNode.cacheFile.decompress((int) requestNode.indexReference);
                }
            } catch (Exception exception) {
                PacketEncoder.sendErrorToServer(null, exception);
            }
            requestNode.waiting = false;
        }
    }
    
    /**
     * (Blocking call)
     * Requests a directly needed file to be loaded into memory
     */
    public LocalRequest createPriorityRequest(int indexReference, Decompressor cacheFile) {
        LocalRequest node = new LocalRequest();
        node.type = 1;
        synchronized (this.queue) {
            LocalRequest anotherNode = (LocalRequest) this.queue.getNext();
            while (anotherNode != null) {
                if (indexReference == anotherNode.indexReference && cacheFile == anotherNode.cacheFile && anotherNode.type == 2) {
                    node.payload = anotherNode.payload;
                    node.waiting = false;
                    return node;
                }
                anotherNode = (LocalRequest) this.queue.getDirectNext();
            }
        }
        node.payload = cacheFile.decompress(indexReference);
        node.waiting = false;
        node.highPriority = true;
        return node;
    }
    
    /**
     * (Non blocking call)
     * Requests the payload from an already received file to be stored in our cache
     */
    public LocalRequest createStoreRequest(int key, byte[] payload, Decompressor cacheFile) {
        LocalRequest request = new LocalRequest();
        request.type = 2;
        request.indexReference = key;
        request.payload = payload;
        request.cacheFile = cacheFile;
        request.highPriority = false;
        addRequest(request);
        return request;
    }

    /**
     * (Non blocking call)
     * Requests an indirectly needed file to be loaded into memory
     */
    public LocalRequest createNormalRequest(int fileId, Decompressor cacheFile) {
        LocalRequest request = new LocalRequest();
        request.type = 3;
        request.indexReference = fileId;
        request.cacheFile = cacheFile;
        request.highPriority = false;
        addRequest(request);
        return request;
    }

    /**
     * Requests a file to be fetched from our cache
     * on a separate thread
     */
    public void addRequest(LocalRequest request) {
        synchronized (this.queue) {
            this.queue.insert(request);
            this.requestCount++;
            this.queue.notifyAll();
        }
    }
    
    /**
     * Cleans up the queue and kills the executing thread
     */
    public void shutdown() {
        this.shutdown = true;
        synchronized (this.queue) {
            this.queue.notifyAll();
        }
        try {
            this.executingThread.join();
        } catch (InterruptedException interruptedException) {
        	// NOOP
        }
        this.executingThread = null;
    }
}
