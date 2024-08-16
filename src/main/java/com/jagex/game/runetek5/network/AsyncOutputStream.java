package com.jagex.game.runetek5.network;

import java.io.IOException;
import java.io.OutputStream;

public class AsyncOutputStream implements Runnable {

    public IOException occurredException;
    public int writeOffset;
    public int readOffset;
    public OutputStream outputstream;
    public boolean shutdown;
    public final int bufferSize;
    public final byte[] buffer;
    public final Thread executingThread;

    public AsyncOutputStream(OutputStream outputstream, int capacity) {
        this.outputstream = outputstream;
        this.bufferSize = capacity + 1;
        this.buffer = new byte[this.bufferSize];
        this.executingThread = new Thread(this);
        this.executingThread.setDaemon(true);
        this.executingThread.start();
    }

    public boolean closed() {
        if (this.shutdown) {
            try {
                this.outputstream.close();
                if (this.occurredException == null) {
                    this.occurredException = new IOException("");
                }
            } catch (IOException ioexception) {
                if (this.occurredException == null) {
                    this.occurredException = new IOException(ioexception);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        do {
            int length;
            synchronized (this) {
                while (true) {
                    if (this.occurredException != null) {
                        return;
                    }
                    if (this.writeOffset <= this.readOffset) {
                        length = this.readOffset - this.writeOffset;
                    } else {
                        length = this.bufferSize - this.writeOffset + this.readOffset;
                    }
                    if (length > 0) {
                        break;
                    }
                    try {
                    	this.outputstream.flush();
                    } catch (IOException ioexception) {
                        this.occurredException = ioexception;
                        return;
                    }
                    if (closed()) {
                        return;
                    }
                    try {
                        wait();
                    } catch (InterruptedException exception) {
                    	// NOOP
                    }
                }
            }
            try {
                if (length + this.writeOffset <= this.bufferSize) {
                	this.outputstream.write(this.buffer, this.writeOffset, length);
                } else {
                    int sizeOffset = this.bufferSize - this.writeOffset;
                    this.outputstream.write(this.buffer, this.writeOffset, sizeOffset);
                    this.outputstream.write(this.buffer, 0, length - sizeOffset);
                }
            } catch (IOException ioexception) {
                synchronized (this) {
                	this.occurredException = ioexception;
                    return;
                }
            }
            synchronized (this) {
            	this.writeOffset = (length + this.writeOffset) % this.bufferSize;
            }
        } while (!closed());
    }

    public void write(byte[] readingBuffer, int srcPosition, int size) throws IOException {
        if (size >= 0 && srcPosition >= 0 && size + srcPosition <= readingBuffer.length) {
            synchronized (this) {
                if (this.occurredException != null) {
                    throw new IOException(this.occurredException.toString());
                } 
                int sizeLeft;
                if (this.writeOffset <= this.readOffset) {
                    sizeLeft = this.bufferSize - this.readOffset + this.writeOffset - 1;
                } else {
                    sizeLeft = this.writeOffset - this.readOffset - 1;
                }
                if (sizeLeft < size) {
                    throw new IOException("");
                } 
                if (size + this.readOffset <= this.bufferSize) {
                    System.arraycopy(readingBuffer, srcPosition, this.buffer, this.readOffset, size);
                } else {
                    int length = this.bufferSize - this.readOffset;
                    System.arraycopy(readingBuffer, srcPosition, this.buffer, this.readOffset, length);
                    System.arraycopy(readingBuffer, length + srcPosition, this.buffer, 0, size - length);
                }
                this.readOffset = (size + this.readOffset) % this.bufferSize;
                notifyAll();
            }
        } else {
            throw new IOException();
        }
    }

    public void drop() {
        this.outputstream = new DroppedOutputStream();
    }

    public void shutdown() {
        synchronized (this) {
        	this.shutdown = true;
            notifyAll();
        }
        try {
        	this.executingThread.join();
        } catch (InterruptedException exception) {
        	// NOOP
        }
    }
}