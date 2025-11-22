package com.jagex.game.runetek5.js5.request.remote;

import static com.jagex.game.runetek5.js5.request.RequestStatus.*;

import java.util.Arrays;
import java.util.zip.CRC32;

import com.jagex.game.runetek5.cache.Decompressor;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Whirlpool;
import com.jagex.game.runetek5.js5.request.Js5WorkerRequestMessage;
import com.jagex.game.runetek5.js5.request.Js5NetQueue;
import com.jagex.game.runetek5.js5.request.FileWorker;
import com.jagex.game.runetek5.js5.request.local.LocalRequest;
import com.jagex.game.runetek5.js5.request.local.LocalRequester;
import com.jagex.game.runetek5.utils.Utils;

public class RemoteFileWorker extends FileWorker {

    public static final CRC32 CRC32 = new CRC32();

    public Js5WorkerRequestMessage masterIndexRequest;

    public Js5MasterIndex masterIndex;

    public byte[] fileRequestStatuses;

    public int filesSuccessfulyReceived;

    public boolean hasRegularRequests;

    public NodeCollection generalRequests;

    public final IterableNodeMap<Js5WorkerRequestMessage> pendingRequests = new IterableNodeMap<Js5WorkerRequestMessage>(16);

    public int requestedFileId;

    public final NodeCollection prefetchEntries = new NodeCollection();

    public long cleanupCycle;

    public final int indexId;

    public final Decompressor cacheDataFile;

    public boolean hasPriorityRequests;

    public final Decompressor masterIndexFile;

    public final Js5NetQueue standardRequester;

    public final LocalRequester localRequester;

    public final int tableCrc;

    public final byte[] tableDigest;

    public final int tableRevision;

    public final boolean initialized;

    public RemoteFileWorker(int indexId, Decompressor dataFile, Decompressor masterIndex, Js5NetQueue standardRequester, LocalRequester localRequester, int tableCrc, byte[] tableDigest, int tableRevision) {
        this.indexId = indexId;
        this.cacheDataFile = dataFile;
        if (this.cacheDataFile == null) {
        	this.hasPriorityRequests = false;
        } else {
        	this.hasPriorityRequests = true;
        	this.generalRequests = new NodeCollection();
        }
        this.masterIndexFile = masterIndex;
        this.standardRequester = standardRequester;
        this.localRequester = localRequester;
        this.tableCrc = tableCrc;
        this.tableDigest = tableDigest;
        this.tableRevision = tableRevision;
        this.initialized = true;
        if (this.masterIndexFile != null) {
        	this.masterIndexRequest = localRequester.createPriorityRequest(this.indexId, this.masterIndexFile);
        }
    }

    @Override
    public int getProgress(int fileId) {
        Js5WorkerRequestMessage fileRequest = this.pendingRequests.get(fileId);
        return fileRequest != null ? fileRequest.getCompletion() : 0;
    }

    @Override
    public Js5MasterIndex getMasterIndex() {
        if (this.masterIndex != null) {
            return this.masterIndex;
        } 
        if (this.masterIndexRequest == null) {
            if (this.standardRequester.priorityRequestUnavailable()) {
                return null;
            }
            this.masterIndexRequest = this.standardRequester.request(255, this.indexId, (byte) 0, true);
        }
        if (this.masterIndexRequest.waiting) {
            return null;
        }
        byte[] referenceTablePayload = this.masterIndexRequest.getPayload();
        if (this.masterIndexRequest instanceof LocalRequest) {
        	/** ReferenceTable from disk **/
            if (referenceTablePayload == null) {
                throw new RuntimeException();
            }
            this.masterIndex = new Js5MasterIndex(referenceTablePayload, this.tableCrc, this.tableDigest);
            if (this.masterIndex.revision != this.tableRevision) {
                throw new RuntimeException();
            }
        } else {
        	/** ReferenceTable from update server **/
            if (referenceTablePayload == null) {
                throw new RuntimeException();
            }
            this.masterIndex = new Js5MasterIndex(referenceTablePayload, this.tableCrc, this.tableDigest);
            if (this.masterIndexFile != null) {
                this.localRequester.createStoreRequest(this.indexId, referenceTablePayload, this.masterIndexFile);
            }
        }
        this.masterIndexRequest = null;
        if (this.cacheDataFile != null) {
            this.fileRequestStatuses = new byte[this.masterIndex.archiveCount];
            this.filesSuccessfulyReceived = 0;
        }
        return this.masterIndex;
    }

    @Override
    public byte[] requestArchive(int archiveId) {
        Js5WorkerRequestMessage fileRequest = request(archiveId, 0);
        if (67900 == archiveId) {
            System.out.println("[RemoteFileWorker] " + archiveId + " - " + fileRequest);
        }
        if (fileRequest == null) {
            return null;
        } 
        byte[] archivePayload = fileRequest.getPayload();
        fileRequest.unlink();
        return archivePayload;
    }

    public void processPrefetch() {
        if (this.generalRequests != null && getMasterIndex() != null) {
            for (Node prefetchEntry = this.prefetchEntries.getFirst(); prefetchEntry != null; prefetchEntry = this.prefetchEntries.next()) {
                int archiveId = (int) prefetchEntry.pointer;
                if (archiveId >= 0 && archiveId < this.masterIndex.archiveCount && this.masterIndex.archiveFileCount[archiveId] != 0) {
                    if (this.fileRequestStatuses[archiveId] == PRIORITY) {
                        request(archiveId, 1);
                    }
                    if (this.fileRequestStatuses[archiveId] == REGULAR) {
                        request(archiveId, 2);
                    }
                    if (this.fileRequestStatuses[archiveId] == RECEIVED) {
                        prefetchEntry.unlink();
                    }
                } else {
                    prefetchEntry.unlink();
                }
            }
        }
    }

    @Override
    public void prefetch(int fileId) {
        if (this.cacheDataFile != null) {
            for (Node node = this.prefetchEntries.getFirst(); node != null; node = this.prefetchEntries.next()) {
                if (node.pointer == fileId) {
                    return;
                }
            }
            Node prefetchRequest = new Node();
            prefetchRequest.pointer = fileId;
            this.prefetchEntries.addFirst(prefetchRequest);
        }
    }

    public int getTotalArchives() {
        return this.masterIndex == null ? 0 : this.masterIndex.validArchiveCount;
    }

    public int filesReceived() {
        return this.filesSuccessfulyReceived;
    }

    public int getReferenceTableCompletion() {
        return getMasterIndex() == null ? (this.masterIndexRequest == null ? 0 : this.masterIndexRequest.getCompletion()) : 100;
    }

    public void reset() {
        if (this.cacheDataFile != null) {
            this.hasRegularRequests = true;
            if (this.generalRequests == null) {
                this.generalRequests = new NodeCollection();
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
    public Js5WorkerRequestMessage request(int fileId, int actionType) {
    	Js5WorkerRequestMessage fileRequest = this.pendingRequests.get(fileId);
        if (67900 == fileId) {
            System.out.println("[RemoteFileWorker.request] " + fileId + " - " + fileRequest);
            if (fileRequest != null) {
                System.out.println("\t[RemoteFileWorker.request] " + fileRequest.highPriority + ", " + fileRequest.waiting + ", " + fileRequest.isCompleted + ", " + fileRequest.getCompletion());
            }
        }
        if (fileRequest != null && actionType == 0 && !fileRequest.highPriority && fileRequest.waiting) {
            fileRequest.unlink();
            fileRequest = null;
        }
        if (fileRequest == null) {
            if (actionType == 0) {
                if (this.cacheDataFile != null && this.fileRequestStatuses[fileId] != REGULAR) {
                    fileRequest = this.localRequester.createPriorityRequest(fileId, this.cacheDataFile);
                } else {
                    if (this.standardRequester.priorityRequestUnavailable()) {
                        return null;
                    }
                    fileRequest = this.standardRequester.request(this.indexId, fileId, (byte) 2, true);
                }
            } else if (actionType == 1) {
                if (this.cacheDataFile == null) {
                    throw new RuntimeException();
                }
                fileRequest = this.localRequester.createNormalRequest(fileId, this.cacheDataFile);
            } else {
                if (actionType != 2) {
                    throw new RuntimeException();
                }
                if (this.cacheDataFile == null) {
                    throw new RuntimeException();
                }
                if (this.fileRequestStatuses[fileId] != REGULAR) {
                    throw new RuntimeException();
                }
                if (this.standardRequester.regularRequestUnavailable()) {
                    return null;
                }
                fileRequest = this.standardRequester.request(this.indexId, fileId, (byte) 2, false);
            }
            this.pendingRequests.put(fileRequest, fileId);
        }
        if (fileRequest.waiting) {
            return null;
        } 
        byte[] payload = fileRequest.getPayload();
        if (fileRequest instanceof LocalRequest) {
            try {
                if (payload != null && payload.length > 2) {
                    CRC32.reset();
                    CRC32.update(payload, 0, payload.length - 2);
                    int crc = (int) CRC32.getValue();
                    if (crc != this.masterIndex.crcs[fileId]) {
                        throw new RuntimeException();
                    } 
                    if (this.masterIndex.whirlpool != null && this.masterIndex.whirlpool[fileId] != null) {
                    	byte[] expectedWhirlpool = this.masterIndex.whirlpool[fileId];
                        byte[] payloadWhirlpool = Whirlpool.calculateDigest(payload, 0, payload.length - 2);
                        for (int idx = 0; idx < 64; idx++) {
                            if (payloadWhirlpool[idx] != expectedWhirlpool[idx]) {
                                throw new RuntimeException();
                            }
                        }
                    }
                    int version = (payload[payload.length - 1] & 0xff) + ((payload[payload.length - 2] & 0xff) << 8);
                    if (version != (this.masterIndex.versions[fileId] & 0xffff)) {
                        throw new RuntimeException();
                    } 
                    if (this.fileRequestStatuses[fileId] != RECEIVED) {
                        this.filesSuccessfulyReceived++;
                        this.fileRequestStatuses[fileId] = RECEIVED;
                    }
                    if (!fileRequest.highPriority) {
                        fileRequest.unlink();
                    }
                    return fileRequest;
                } 
                throw new RuntimeException();
            } catch (Exception exception) {
                this.fileRequestStatuses[fileId] = REGULAR;
                fileRequest.unlink();
                if (fileRequest.highPriority && !this.standardRequester.priorityRequestUnavailable()) {
                    Js5NetQueueRequest request = this.standardRequester.request(this.indexId, fileId, (byte) 2, true);
                    this.pendingRequests.put(request, fileId);
                }
                return null;
            }
        } else if (payload != null && payload.length > 2) {
            CRC32.reset();
            CRC32.update(payload, 0, payload.length - 2);
            int receivedCrc = (int) CRC32.getValue();
            if (receivedCrc != this.masterIndex.crcs[fileId]) {
                throw new RuntimeException();
            } 
            if (this.masterIndex.whirlpool != null && this.masterIndex.whirlpool[fileId] != null) {
                byte[] pool = this.masterIndex.whirlpool[fileId];
                byte[] expectedWhirlpool = Whirlpool.calculateDigest(payload, 0, payload.length - 2);
                for (int idx = 0; idx < 64; idx++) {
                    if (expectedWhirlpool[idx] != pool[idx]) {
                        throw new RuntimeException();
                    }
                }
            }
            this.standardRequester.failedRequests = 0;
            this.standardRequester.connectionStatus = 0;
            payload[payload.length - 2] = (byte) (this.masterIndex.versions[fileId] >>> 8);
            payload[payload.length - 1] = (byte) this.masterIndex.versions[fileId];
            if (this.cacheDataFile != null) {
                this.localRequester.createStoreRequest(fileId, payload, this.cacheDataFile);
                if (this.fileRequestStatuses[fileId] != RECEIVED) {
                    this.filesSuccessfulyReceived++;
                    this.fileRequestStatuses[fileId] = RECEIVED;
                }
            }
            if (!fileRequest.highPriority) {
                fileRequest.unlink();
            }
            return fileRequest;
        } else {
            throw new RuntimeException();
        }
    }


    public void processGeneralRequests() {
        if (this.generalRequests != null) {
            if (getMasterIndex() == null) {
                return;
            }
            if (this.hasPriorityRequests) {
            	boolean completed = true;
                for (Node request = this.generalRequests.getFirst(); request != null; request = this.generalRequests.next()) {
                	int fileId = (int) request.pointer;
                    if (this.fileRequestStatuses[fileId] == PRIORITY) {
                        request(fileId, 1);
                    }
                    if (this.fileRequestStatuses[fileId] != PRIORITY) {
                        request.unlink();
                    } else {
                        completed = false;
                    }
                }
                while (this.requestedFileId < this.masterIndex.archiveFileCount.length) {
                    if (this.masterIndex.archiveFileCount[this.requestedFileId] != 0) {
                        if (this.localRequester.requestCount >= 250) {
                            completed = false;
                            break;
                        }
                        if (this.fileRequestStatuses[this.requestedFileId] == PRIORITY) {
                            request(this.requestedFileId, 1);
                        }
                        if (this.fileRequestStatuses[this.requestedFileId] == PRIORITY) {
                            Node request = new Node();
                            request.pointer = this.requestedFileId;
                            this.generalRequests.addFirst(request);
                            completed = false;
                        }
                    }
                    this.requestedFileId++;
                }
                if (completed) {
                    this.hasPriorityRequests = false;
                    this.requestedFileId = 0;
                }
            } else if (this.hasRegularRequests) {
            	boolean completed = true;
                for (Node request = this.generalRequests.getFirst(); request != null; request = this.generalRequests.next()) {
                	int fileId = (int) request.pointer;
                    if (this.fileRequestStatuses[fileId] != RECEIVED) {
                        request(fileId, 2);
                    }
                    if (this.fileRequestStatuses[fileId] == RECEIVED) {
                        request.unlink();
                    } else {
                        completed = false;
                    }
                }
                while (this.requestedFileId < this.masterIndex.archiveFileCount.length) {
                    if (this.masterIndex.archiveFileCount[this.requestedFileId] != 0) {
                        if (this.standardRequester.regularRequestUnavailable()) {
                            completed = false;
                            break;
                        }
                        if (this.fileRequestStatuses[this.requestedFileId] != RECEIVED) {
                            request(this.requestedFileId, 2);
                        }
                        if (this.fileRequestStatuses[this.requestedFileId] != RECEIVED) {
                        	Node request = new Node();
                            request.pointer = this.requestedFileId;
                            this.generalRequests.addFirst(request);
                            completed = false;
                        }
                    }
                    this.requestedFileId++;
                }
                if (completed) {
                    this.hasRegularRequests = false;
                    this.requestedFileId = 0;
                }
            } else {
                this.generalRequests = null;
            }
        }
     
        /** Cleanup high priority requests **/
        if (this.initialized && Utils.time() >= this.cleanupCycle) {
            for (Js5WorkerRequestMessage request = this.pendingRequests.first(); request != null; request = this.pendingRequests.next()) {
                if (!request.waiting) {
                    if (request.isCompleted) {
                        if (!request.highPriority) {
                            throw new RuntimeException();
                        }
                        request.unlink();
                    } else {
                        request.isCompleted = true;
                    }
                }
            }
            this.cleanupCycle = Utils.time() + 1000L;
        }
    }

    public int currentRequestId() {
        if (this.masterIndex == null) {
            return 0;
        } 
        if (!this.hasPriorityRequests) {
            return this.masterIndex.validArchiveCount;
        } 
        Node currentRequest = this.generalRequests.getFirst();
        return currentRequest == null ? 0 : (int) currentRequest.pointer;
    }
}
