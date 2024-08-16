package com.jagex.game.runetek5.js5.request.local;

import com.jagex.game.runetek5.cache.Decompressor;
import com.jagex.game.runetek5.js5.request.Js5WorkerRequestMessage;

/**
 * Represents a request to fetch the reference table from the update server
 * @author Brainpower - 21/05/2020
 *
 */
public class LocalRequest extends Js5WorkerRequestMessage {

	public Decompressor cacheFile;
	public int type;
	public byte[] payload;

    @Override
    public int getCompletion() {
        return super.waiting ? 0 : 100;
    }

    @Override
    public byte[] getPayload() {
        if (super.waiting) {
            throw new RuntimeException();
        } 
        return this.payload;
    }
}
