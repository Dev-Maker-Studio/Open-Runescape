package com.jagex.game.runetek5.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * < Fully refactored >
 * @author Brainpower - 30/05/2020
 * 
 * Manages an inputstream on a seperate thread
 *
 */
public class AsyncInputStream implements Runnable {

    public IOException readError;
    public int readOffset;
    public InputStream inputStream;
    public int readPosition;
    public final int capacity;
    public final byte[] buffer;
    public final Thread executingThread;

    /**
     * Creates all required resourced to manage this inputstream
     */
    public AsyncInputStream(InputStream inputstream, int size) {
        this.inputStream = inputstream;
        this.capacity = size + 1;
        this.buffer = new byte[this.capacity];
        this.executingThread = new Thread(this);
        this.executingThread.setDaemon(true);
        this.executingThread.start();
    }


    /**
     * Reads the inputstream and manages the offset
     */
    @Override
    public void run() {
        while (true) {
            int length;
            synchronized (this) {
                while (true) {
                    if (this.readError != null) {
                        return;
                    }
                    if (this.readPosition == 0) {
                        length = this.capacity - this.readOffset - 1;
                    } else if (this.readPosition <= this.readOffset) {
                        length = this.capacity - this.readOffset;
                    } else {
                        length = this.readPosition - this.readOffset - 1;
                    }
                    if (length > 0) {
                        break;
                    }
                    try {
                        wait();
                    } catch (InterruptedException interruptedException) {
                    	// NOOP
                    }
                }
            }
            int readResult;
            try {
                readResult = this.inputStream.read(this.buffer, this.readOffset, length);
                if (readResult == -1) {
                    throw new EOFException();
                }
            } catch (IOException ioexception) {
                synchronized (this) {
                	this.readError = ioexception;
                    return;
                }
            }
            synchronized (this) {
            	this.readOffset = (readResult + this.readOffset) % this.capacity;
            }
        }
    }
    
    /**
     * Reads a part of the inputstream's buffer into given buffer
     */
    public int read(byte[] destinationBuffer, int destinationPosition, int copyLength) throws IOException {
        if (copyLength >= 0 && destinationPosition >= 0 && copyLength + destinationPosition <= destinationBuffer.length) {
            synchronized (this) {
                int maxLength;
                if (this.readPosition <= this.readOffset) {
                    maxLength = this.readOffset - this.readPosition;
                } else {
                    maxLength = this.capacity - this.readPosition + this.readOffset;
                }
                if (copyLength > maxLength) {
                    copyLength = maxLength;
                }
                if (copyLength == 0 && this.readError != null) {
                    throw new IOException(this.readError.toString());
                } 
                if (copyLength + this.readPosition <= this.capacity) {
                    System.arraycopy(this.buffer, this.readPosition, destinationBuffer, destinationPosition, copyLength);
                } else {
                    int secondaryReadOffset = this.capacity - this.readPosition;
                    System.arraycopy(this.buffer, this.readPosition, destinationBuffer, destinationPosition, secondaryReadOffset);
                    System.arraycopy(this.buffer, 0, destinationBuffer, secondaryReadOffset + destinationPosition, copyLength - secondaryReadOffset);
                }
                this.readPosition = (copyLength + this.readPosition) % this.capacity;
                notifyAll();
                return copyLength;
            }
        } else {
            throw new IOException();
        }
    }
    
    /**
     * Checks if the inputstream has enough capacity to fit a certain amount of bytes
     */
    public boolean available(int capacityRequired) throws IOException {
        if (capacityRequired > 0 && capacityRequired < this.capacity) {
            synchronized (this) {
                int length;
                if (this.readPosition <= this.readOffset) {
                    length = this.readOffset - this.readPosition;
                } else {
                    length = this.capacity - this.readPosition + this.readOffset;
                }
                if (length < capacityRequired) {
                    if (this.readError != null) {
                        throw new IOException(this.readError.toString());
                    } 
                    notifyAll();
                    return false;
                } 
                return true;
            }
        } 
        throw new IOException();
    }

    /**
     * Checks how many bytes are left to be read
     */
    public int capacityLeft() throws IOException {
        synchronized (this) {
            int length;
            if (this.readPosition <= this.readOffset) {
                length = this.readOffset - this.readPosition;
            } else {
                length = this.capacity - this.readPosition + this.readOffset;
            }
            if (this.readError != null) {
                throw new IOException(this.readError.toString());
            } 
            notifyAll();
            return length;
        }
    }
    
    /**
     * Drops the inputstream to be shutdown automatically in 30 seconds
     */
    public void drop() {
    	this.inputStream = new DroppedInputStream();
    }

    /**
     * Wakes up all the threads and joins them
     */
    public void shutdown() {
        synchronized (this) {
            if (this.readError == null) {
            	this.readError = new IOException("");
            }
            notifyAll();
        }
        try {
        	this.executingThread.join();
        } catch (InterruptedException interruptedException) {
        	// NOOP
        }
    }
}
