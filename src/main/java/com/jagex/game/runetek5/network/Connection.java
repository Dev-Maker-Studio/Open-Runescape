package com.jagex.game.runetek5.network;

import java.io.IOException;

public abstract class Connection {

    public static Connection updateConnection;

    public abstract void write(byte[] buffer, int length) throws IOException;

    public abstract int getInstreamCapacityLeft() throws IOException;

    public abstract boolean available(int byteCount) throws IOException;

    public abstract void close();

    public abstract void drop();

    public abstract int read(byte[] buffer, int src, int length) throws IOException;
}
