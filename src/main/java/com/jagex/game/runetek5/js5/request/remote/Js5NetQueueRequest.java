package com.jagex.game.runetek5.js5.request.remote;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.js5.request.Js5WorkerRequestMessage;

/**
 * A request to fetch a file from the update server
 * @author Brainpower - 21/05/2020
 *
 */
public class Js5NetQueueRequest extends Js5WorkerRequestMessage {

	public byte padding;
	
	/**
	 * The request processing status
	 */
    public int blockBytesRead;
    
    /**
     * The stream holding the data of the request
     */
    public Packet stream;

    @Override
    public byte[] getPayload() {
        if (!super.waiting && this.stream.currentPosition >= this.stream.buffer.length - this.padding) {
            return this.stream.buffer;
        } 
        throw new RuntimeException();
    }

    @Override
    public int getCompletion() {
        return this.stream == null ? 0 : this.stream.currentPosition * 100 / (this.stream.buffer.length - this.padding);
    }
}
