package com.jagex.game.runetek5.cache;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * JagexFileSystem
 * @author Brainpower
 *
 */
public class JFS32 implements AutoCloseable {

    public RandomAccessFile rwFile;
    public long seekPosition;
    public long maxCapacity;

    public JFS32(File outputFile, long maxCapacity) throws IOException {
        if (maxCapacity == -1L) {
            maxCapacity = Long.MAX_VALUE;
        }
        if (outputFile.length() > maxCapacity) {
            outputFile.delete();
        }
        this.rwFile = new RandomAccessFile(outputFile, "rw");
        this.maxCapacity = maxCapacity;
        this.seekPosition = 0L;
        int dataPoint = this.rwFile.read();
        if (dataPoint != -1) {
        	this.rwFile.seek(0L);
        	this.rwFile.write(dataPoint);
        }
        this.rwFile.seek(0L);
    }

    @Override
    public void close() throws IOException {
        if (this.rwFile != null) {
        	this.rwFile.close();
        	this.rwFile = null;
        }
    }

    public void write(byte[] data, int writeOffset, int seekOffset) throws IOException {
        if (this.seekPosition + seekOffset > this.maxCapacity) {
        	this.rwFile.seek(this.maxCapacity);
        	this.rwFile.write(1);
            throw new EOFException();
        } else {
        	this.rwFile.write(data, writeOffset, seekOffset);
            this.seekPosition += seekOffset;
        }
    }

    public long length() throws IOException {
        return this.rwFile.length();
    }

    public int getSeekOffset(byte[] data, int offset, int length) throws IOException {
        int seekOffset = this.rwFile.read(data, offset, length);
        if (seekOffset > 0) {
        	this.seekPosition += seekOffset;
        }
        return seekOffset;
    }

    public void seek(long seekTo) throws IOException {
    	this.rwFile.seek(seekTo);
    	this.seekPosition = seekTo;
    }
}
