package com.jagex.game.runetek5.js5.request.remote;

import java.io.IOException;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.js5.request.Js5NetQueue;
import com.jagex.game.runetek5.network.Connection;
import com.jagex.game.runetek5.utils.Utils;

public class RemoteFileRequester extends Js5NetQueue {

    public Connection updateServerConnection;

    /**
     * Resets all waiting requests back to pending state so they can be retried.
     * This should be called whenever a connection error occurs to prevent requests
     * from being stuck in waiting state forever.
     */
    private void resetWaitingRequests() {
        // Remove all waiting requests completely - they will be recreated when needed
        // We need to remove them from both the queue (unlinkSub) and the main map (unlink)
        Js5NetQueueRequest request;
        while ((request = (Js5NetQueueRequest) super.waitingPriorityRequests.getNext()) != null) {
            request.unlinkSub(); // Remove from queue
            request.unlink();     // Remove from RemoteFileWorker.pendingRequests
        }
        while ((request = (Js5NetQueueRequest) super.waitingRegularRequests.getNext()) != null) {
            request.unlinkSub(); // Remove from queue
            request.unlink();     // Remove from RemoteFileWorker.pendingRequests
        }
        this.lastRemoteRequest = null;
    }

    @Override
    public void init(Connection updateServerConnection, boolean loggedIn) {
        if (this.updateServerConnection != null) {
            try {
                this.updateServerConnection.close();
            } catch (Exception ignored) {
            	// NOOP
            }
            this.updateServerConnection = null;
        }

        this.updateServerConnection = updateServerConnection;
        initializeSession();
        sendLoginState(loggedIn);
        this.incoming.currentPosition = 0;
        this.lastRemoteRequest = null;

        while (true) {
            Js5NetQueueRequest request = (Js5NetQueueRequest) super.waitingPriorityRequests.getFirst();
            if (request == null) {
                while (true) {
                    request = (Js5NetQueueRequest) super.waitingRegularRequests.getFirst();
                    if (request == null) {
                        if (this.encryption != 0) {
                            try {
                                this.outgoing.currentPosition = 0;
                                this.outgoing.writeByte(4);
                                this.outgoing.writeByte(this.encryption);
                                this.outgoing.writeInt(0);
                                this.updateServerConnection.write(this.outgoing.buffer, this.outgoing.buffer.length);
                            } catch (IOException ioexception) {
                                try {
                                    this.updateServerConnection.close();
                                } catch (Exception exception) {
                                	// NOOP
                                }
                                this.updateServerConnection = null;
                                this.failedRequests++;
                                this.connectionStatus = -2;
                                resetWaitingRequests();
                            }
                        }
                        super.lastConnectionInteraction = 0;
                        super.cycleTime = Utils.time();
                        return;
                    }
                    super.regularRequests.insert(request);
                }
            }
            super.priorityRequests.insert(request);
        }
    }

    @Override
    public void sendLoginState(boolean loggedIn) {
        if (this.updateServerConnection != null) {
            try {
                this.outgoing.currentPosition = 0;
                this.outgoing.writeByte(loggedIn ? 2 : 3);
                this.outgoing.write40BitLong(0L);
                this.updateServerConnection.write(this.outgoing.buffer, this.outgoing.buffer.length);
            } catch (IOException ioexception) {
                try {
                    this.updateServerConnection.close();
                } catch (Exception exception) {
                	// NOOP
                }
                this.updateServerConnection = null;
                this.failedRequests++;
                this.connectionStatus = -2;
                resetWaitingRequests();
            }
        }
    }

    @Override
    public void requestTermination() {
        if (this.updateServerConnection != null) {
            try {
                this.outgoing.currentPosition = 0;
                this.outgoing.writeByte(7);
                this.outgoing.write40BitLong(0L);
                this.updateServerConnection.write(this.outgoing.buffer, this.outgoing.buffer.length);
            } catch (IOException ioexception) {
                try {
                    this.updateServerConnection.close();
                } catch (Exception exception) {
                	// NOOP
                }
                this.updateServerConnection = null;
                this.failedRequests++;
                this.connectionStatus = -2;
                resetWaitingRequests();
            }
        }
    }

    @Override
    public void closeStream() {
        if (this.updateServerConnection != null) {
            this.updateServerConnection.close();
        }
    }

    @Override
    public void dropConnection() {
        if (this.updateServerConnection != null) {
            this.updateServerConnection.drop();
        }
    }

    /**
     * Sends a request to the update server to start
     * a new file update session with this client
     */
    public void initializeSession() {
        if (this.updateServerConnection != null) {
            try {
                this.outgoing.currentPosition = 0;
                this.outgoing.writeByte(6);
                this.outgoing.writeMedium(3);
                this.outgoing.writeShort(0);
                this.updateServerConnection.write(this.outgoing.buffer, this.outgoing.buffer.length);
            } catch (IOException ioexception) {
                try {
                    this.updateServerConnection.close();
                } catch (Exception ignored) {
                	// NOOP
                }
                this.updateServerConnection = null;
                this.failedRequests++;
                this.connectionStatus = -2;
                resetWaitingRequests();
            }
        }
    }

    /**
     * Performs a cycle communicating with the update server
     * @return All requests processed
     */
    @Override
    public boolean pulse() {
        if (this.updateServerConnection != null) {
            long currentTime = Utils.time();
            int timeDelta = (int) (currentTime - super.cycleTime);
            super.cycleTime = currentTime;
            if (timeDelta > 200) {
                timeDelta = 200;
            }

            super.lastConnectionInteraction += timeDelta;
            if (super.lastConnectionInteraction > 30000) {
                try {
                    this.updateServerConnection.close();
                } catch (Exception ignored) {
                	// NOOP
                }
                this.updateServerConnection = null;
                resetWaitingRequests();
            }
        }

        if (this.updateServerConnection == null) {
            return priorityRequestCount() == 0 && regularRequestCount() == 0;
        } 
        try {
            for (Js5NetQueueRequest priorityRequest = (Js5NetQueueRequest) super.priorityRequests.getNext(); priorityRequest != null; priorityRequest = (Js5NetQueueRequest) super.priorityRequests.getDirectNext()) {
                this.outgoing.currentPosition = 0;
                this.outgoing.writeByte(1);
                this.outgoing.write40BitLong(priorityRequest.indexReference);
                this.updateServerConnection.write(this.outgoing.buffer, this.outgoing.buffer.length);
                super.waitingPriorityRequests.insert(priorityRequest);
            }

            for (Js5NetQueueRequest regularRequest = (Js5NetQueueRequest) super.regularRequests.getNext(); regularRequest != null; regularRequest = (Js5NetQueueRequest) super.regularRequests.getDirectNext()) {
                this.outgoing.currentPosition = 0;
                this.outgoing.writeByte(0);
                this.outgoing.write40BitLong(regularRequest.indexReference);
                this.updateServerConnection.write(this.outgoing.buffer, this.outgoing.buffer.length);
                super.waitingRegularRequests.insert(regularRequest);
            }

            for (int loop = 0; loop < 100; loop++) {
                int capacity = this.updateServerConnection.getInstreamCapacityLeft();
                if (capacity < 0) {
                    throw new IOException();
                }

                if (capacity == 0) {
                    break;
                }

                super.lastConnectionInteraction = 0;
                byte expectedHeaderSize = 0;
                if (this.lastRemoteRequest == null) {
                    expectedHeaderSize = 10;
                } else if (this.lastRemoteRequest.blockBytesRead == 0) {
                    expectedHeaderSize = 1;
                }

                if (expectedHeaderSize > 0) {
                    int readByteCount = expectedHeaderSize - this.incoming.currentPosition;
                    if (readByteCount > capacity) {
                        readByteCount = capacity;
                    }

                    this.updateServerConnection.read(this.incoming.buffer, this.incoming.currentPosition, readByteCount);
                    if (this.encryption != 0) {
                        for (int dataIndex = 0; dataIndex < readByteCount; dataIndex++) {
                            this.incoming.buffer[dataIndex + this.incoming.currentPosition] ^= this.encryption;
                        }
                    }

                    this.incoming.currentPosition += readByteCount;
                    if (this.incoming.currentPosition >= expectedHeaderSize) {
                        if (this.lastRemoteRequest == null) {
                            this.incoming.currentPosition = 0;
                            int cacheIndex = this.incoming.readUByte();
                            int archiveId = this.incoming.readInt();
                            int bitpacketSettings = this.incoming.readUByte();
                            int payloadSize = this.incoming.readInt();
                            int compressed = bitpacketSettings & 0x7f;
                            //System.out.println("Incoming file: " + cacheIndex + ", " + archiveId + ", " + bitpacketSettings + ", " + payloadSize);
                            boolean regularRequest = (bitpacketSettings & 0x80) != 0;
                            long reference = archiveId + ((long) cacheIndex << 32);
                            Js5NetQueueRequest request;
                            if (regularRequest) {
                                for (request = (Js5NetQueueRequest) super.waitingRegularRequests.getNext(); request != null && request.indexReference != reference; request = (Js5NetQueueRequest) super.waitingRegularRequests.getDirectNext()) { }
                            } else {
                                for (request = (Js5NetQueueRequest) super.waitingPriorityRequests.getNext(); request != null && request.indexReference != reference; request = (Js5NetQueueRequest) super.waitingPriorityRequests.getDirectNext()) { }
                            }

                            if (request == null) {
                                throw new IOException();
                            }

                            int headerSize = compressed == 0 ? 5 : 9;
                            this.lastRemoteRequest = request;
                            this.lastRemoteRequest.stream = new Packet(this.lastRemoteRequest.padding + headerSize + payloadSize);
                            this.lastRemoteRequest.stream.writeByte(compressed);
                            this.lastRemoteRequest.stream.writeInt(payloadSize);
                            this.lastRemoteRequest.blockBytesRead = 10;
                            this.incoming.currentPosition = 0;
                        } else {
                            if (this.lastRemoteRequest.blockBytesRead != 0) {
                                throw new IOException();
                            }
                            
                            if (this.incoming.buffer[0] == -1) {
                                this.lastRemoteRequest.blockBytesRead = 1;
                                this.incoming.currentPosition = 0;
                            } else {
                                this.lastRemoteRequest = null;
                            }
                        }
                    }
                } else {
                	int requestSize = this.lastRemoteRequest.stream.buffer.length - this.lastRemoteRequest.padding;
                	int readByteCount = 512 - this.lastRemoteRequest.blockBytesRead;
                    if (readByteCount > requestSize - this.lastRemoteRequest.stream.currentPosition) {
                        readByteCount = requestSize - this.lastRemoteRequest.stream.currentPosition;
                    }

                    if (readByteCount > capacity) {
                        readByteCount = capacity;
                    }

                    this.updateServerConnection.read(this.lastRemoteRequest.stream.buffer, this.lastRemoteRequest.stream.currentPosition, readByteCount);
                    if (this.encryption != 0) {
                        for (int dataIndex = 0; dataIndex < readByteCount; dataIndex++) {
                            this.lastRemoteRequest.stream.buffer[dataIndex + this.lastRemoteRequest.stream.currentPosition] ^= this.encryption;
                        }
                    }

                    this.lastRemoteRequest.stream.currentPosition += readByteCount;
                    this.lastRemoteRequest.blockBytesRead += readByteCount;
                    if (this.lastRemoteRequest.stream.currentPosition == requestSize) {
                        this.lastRemoteRequest.unlinkSub();
                        this.lastRemoteRequest.waiting = false;
                        this.lastRemoteRequest = null;
                    } else if (this.lastRemoteRequest.blockBytesRead == 512) {
                        this.lastRemoteRequest.blockBytesRead = 0;
                    }
                }
            }
            return true;
        } catch (IOException ioexception) {
            try {
                this.updateServerConnection.close();
            } catch (Exception exception) {
            	// NOOP
            }
            this.updateServerConnection = null;
            this.failedRequests++;
            this.connectionStatus = -2;
            resetWaitingRequests();
            return priorityRequestCount() == 0 && regularRequestCount() == 0;
        }
    }
}
