package com.jagex.game.runetek5.network;

import java.io.IOException;
import java.net.Socket;

public class AsyncConnection extends Connection implements AutoCloseable {

    public final Socket socket;

    public AsyncInputStream inputStream;

    public AsyncOutputStream outputStream;

    public AsyncConnection(Socket socket, int capacity) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(30000);
        this.socket.setTcpNoDelay(true);
        this.inputStream = new AsyncInputStream(this.socket.getInputStream(), capacity);
        this.outputStream = new AsyncOutputStream(this.socket.getOutputStream(), capacity);
    }

    public static Connection createAsyncConnection(Socket socket, int capacity) throws IOException {
        return new AsyncConnection(socket, capacity);
    }

    @Override
    public int getInstreamCapacityLeft() throws IOException {
        return this.inputStream.capacityLeft();
    }

    @Override
    public boolean available(int capacityRequired) throws IOException {
        return this.inputStream.available(capacityRequired);
    }

    @Override
    public int read(byte[] buffer, int destinationPosition, int copyLength) throws IOException {
        return this.inputStream.read(buffer, destinationPosition, copyLength);
    }

    @Override
    public void write(byte[] buffer, int size) throws IOException {
        this.outputStream.write(buffer, 0, size);
    }

    @Override
    public void drop() {
        this.inputStream.drop();
        this.outputStream.drop();
    }

    @Override
    public void close() {
        this.outputStream.shutdown();
        try {
            this.socket.close();
        } catch (IOException ioexception) {
        	// NOOP
        }
        this.inputStream.shutdown();
    }
}
