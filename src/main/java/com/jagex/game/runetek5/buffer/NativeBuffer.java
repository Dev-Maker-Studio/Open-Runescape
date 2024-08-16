package com.jagex.game.runetek5.buffer;

public interface NativeBuffer {

    void unlock();

    int getCapacity();

    long getDataStorePointer(int offset, int var2);

    boolean upload(int offset, int size, long dataPointer);
}
