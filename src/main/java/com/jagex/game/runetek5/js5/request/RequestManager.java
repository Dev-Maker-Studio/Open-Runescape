package com.jagex.game.runetek5.js5.request;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.Decompressor;
import com.jagex.game.runetek5.js5.Whirlpool;
import com.jagex.game.runetek5.js5.request.local.LocalRequester;
import com.jagex.game.runetek5.js5.request.remote.Js5NetQueueRequest;
import com.jagex.game.runetek5.js5.request.remote.RemoteFileWorker;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * < Fully refactored >
 * @author Brainpower - 22/05/2020
 * 
 * Manages remote and local file requests
 *
 */
public class RequestManager {
    /**
     * The workers retrieving the payload from requested files (one worker per index)
     */
    public RemoteFileWorker[] remoteFileWorkers;

    /**
     * The requester fetching remote files, from the update server
     */
    public final Js5NetQueue remoteRequester;
    
    /**
     * The requester fetching local files, directly from the cache
     */
    public final LocalRequester localRequester;
    
    /**
     * The master index (ReferenceTable index 255) request
     */
    public Js5NetQueueRequest masterIndexRequest;

    /**
     * The master index (ReferenceTable index 255) buffer
     */
    public Packet masterIndexBuffer;

    /**
     * The RSA public key pair for decrypting data sent from the update server
     */
    public final BigInteger updateServerExponent;
    public final BigInteger updateServerModulus;
    
    /**
     * Loads the encryption and prepares the requesters
     */
    public RequestManager(Js5NetQueue standartRequester, LocalRequester localRequester) {
    	this.remoteRequester = standartRequester;
    	this.localRequester = localRequester;
    	this.updateServerExponent = Loader.RSA_PUBLIC_EXPONENT;
    	this.updateServerModulus = Loader.RSA_PUBLIC_MODULUS;
        if (!this.remoteRequester.priorityRequestUnavailable()) {
        	this.masterIndexRequest = this.remoteRequester.request(255, 255, (byte) 0, true);
        }
    }
    
    /**
     * Proceeds the request cycle
     */
    public void pulse() {
        if (this.remoteFileWorkers != null) {
            for (int index = 0; index < this.remoteFileWorkers.length; index++) {
                if (this.remoteFileWorkers[index] != null) {
                	this.remoteFileWorkers[index].processPrefetch();
                }
            }
            for (int index = 0; index < this.remoteFileWorkers.length; index++) {
                if (this.remoteFileWorkers[index] != null) {
                	this.remoteFileWorkers[index].processGeneralRequests();
                }
            }
        }
    }

    /**
     * Load the reference from the server and prepare the remote file workers
     */
    public boolean initialize() {
        if (this.masterIndexBuffer != null) {
            return true;
        } 
        
        /** Request the master index to identify which files we do have, do not have or which are outdated **/
        if (this.masterIndexRequest == null) {
            if (this.remoteRequester.priorityRequestUnavailable()) {
                return false;
            }
            this.masterIndexRequest = this.remoteRequester.request(255, 255, (byte) 0, true);
        }
        if (this.masterIndexRequest.waiting) {
            return false;
        } 
        
    	/** Read and strip the header from the master index payload **/
        Packet referenceTableBuffer = new Packet(this.masterIndexRequest.getPayload());
        referenceTableBuffer.currentPosition = 5;
        int numIndices = referenceTableBuffer.readUByte();
        referenceTableBuffer.currentPosition += numIndices * 72;
        byte[] rsaEncrypted = new byte[referenceTableBuffer.buffer.length - referenceTableBuffer.currentPosition];
        referenceTableBuffer.readBytes(rsaEncrypted, 0, rsaEncrypted.length);
        
        /** Decrypt the stripped payload with the RSA key pair **/
        byte[] rsaDecrypted;
        if (this.updateServerExponent != null && this.updateServerModulus != null) {
            BigInteger encryptedReferenceTable = new BigInteger(rsaEncrypted);
            BigInteger decryptedReferenceTable = encryptedReferenceTable.modPow(this.updateServerExponent, this.updateServerModulus);
            rsaDecrypted = decryptedReferenceTable.toByteArray();
        } else {
            rsaDecrypted = rsaEncrypted;
        }
        
        if (rsaDecrypted.length != 65 && rsaDecrypted.length != 64) {
            System.out.println("Invalid length: " + rsaDecrypted.length);
            System.out.println(Arrays.toString(rsaDecrypted));
            throw new RuntimeException();
        }
        
        /** Decrypt the stripped payload with whirlpool **/
        byte[] digest = Whirlpool.calculateDigest(referenceTableBuffer.buffer, 5, referenceTableBuffer.currentPosition - rsaEncrypted.length - 5);
        for (int index = 0; index < 64; index++) {
            if (digest[index] != rsaDecrypted[index + (rsaDecrypted.length == 65 ? 1 : 0)]) { // original : rsa_decrypted[index + 1]
                throw new RuntimeException();
            }
        }
        this.remoteFileWorkers = new RemoteFileWorker[numIndices];
        this.masterIndexBuffer = referenceTableBuffer;
        return true;
    }
    
    /**
     * Gets a worker used to create the ResourceProvider
     */
    public RemoteFileWorker getWorker(int index, Decompressor dataFile, Decompressor masterIndex) {
        return getRemoteFileWorker(index, dataFile, masterIndex);
    }

    /**
     * Loads a remote file worker, used for requesting files from the update server
     */
    public RemoteFileWorker getRemoteFileWorker(int indexId, Decompressor dataFile, Decompressor masterIndex) {
        if (this.masterIndexBuffer == null) {
            throw new RuntimeException();
        } 
        if(indexId < 0 || indexId >= this.remoteFileWorkers.length) {
        	throw new RuntimeException();
        }        
        if (this.remoteFileWorkers[indexId] != null) {
            return this.remoteFileWorkers[indexId];
        }
        this.masterIndexBuffer.currentPosition = indexId * 72 + 6;
        int tableCrc = this.masterIndexBuffer.readInt();
        int tableRevision = this.masterIndexBuffer.readInt();
        byte[] tableDigest = new byte[64];
        this.masterIndexBuffer.readBytes(tableDigest, 0, 64);
        RemoteFileWorker fileWorker = new RemoteFileWorker(indexId, dataFile, masterIndex, this.remoteRequester, this.localRequester, tableCrc, tableDigest, tableRevision);
        this.remoteFileWorkers[indexId] = fileWorker;
        return fileWorker;
    }
}
