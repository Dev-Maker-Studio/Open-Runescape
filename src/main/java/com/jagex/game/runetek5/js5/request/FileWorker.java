package com.jagex.game.runetek5.js5.request;

import com.jagex.game.runetek5.js5.Js5MasterIndex;

public abstract class FileWorker {

    public abstract Js5MasterIndex getMasterIndex();

    public abstract byte[] requestArchive(int archiveId);

    public abstract void prefetch(int fileId);

    public abstract int getProgress(int fileId);
}
