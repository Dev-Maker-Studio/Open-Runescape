package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.sequence.frameset.FrameSet;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class SeqTypeList {

    public static boolean forceTween;
    public final LRUCache sequenceCache = new LRUCache(64);

    public final LRUCache framesetCache = new LRUCache(100);

    public final Js5ResourceProvider sequenceResource;
	public static SeqTypeList seqTypeList;

    public SeqTypeList(Js5ResourceProvider sequenceResource, Js5ResourceProvider skeletonResource, Js5ResourceProvider baseResource) {
        this.sequenceResource = sequenceResource;
        if (this.sequenceResource != null) {
            int lastArchiveId = this.sequenceResource.getArchiveCount() - 1;
            SharedConfigsType.SEQ.archiveFileCapacity();
            this.sequenceResource.getArchiveFileCount(lastArchiveId);
        }
        FrameSet.initialize(skeletonResource, baseResource);
    }

    public static void initialize(SeqTypeList seqtypelist) {
        SeqTypeList.seqTypeList = seqtypelist;
    }

    public void resetCaches() {
        synchronized (this.sequenceCache) {
            this.sequenceCache.reset();
        }
        synchronized (this.framesetCache) {
            this.framesetCache.reset();
        }
    }

    public SeqType list(int sequenceId) {
        SeqType seqtype;
        synchronized (this.sequenceCache) {
            seqtype = (SeqType) this.sequenceCache.get(sequenceId);
        }
        if (seqtype != null) {
            return seqtype;
        } 
        byte[] sequenceData;
        synchronized (this.sequenceResource) {
            sequenceData = this.sequenceResource.getFile(SharedConfigsType.SEQ.archiveId(sequenceId), SharedConfigsType.SEQ.archiveFileId(sequenceId));
        }
        seqtype = new SeqType();
        seqtype.id = sequenceId;
        seqtype.seqtypelist = this;
        if (sequenceData != null) {
            seqtype.decode(new Packet(sequenceData));
        }
        seqtype.postDecode();
        synchronized (this.sequenceCache) {
            this.sequenceCache.put(seqtype, sequenceId);
            return seqtype;
        }
    }

    public FrameSet listFrameset(int groupId) {
        synchronized (this.framesetCache) {
            FrameSet frameset = (FrameSet) this.framesetCache.get(groupId);
            if (frameset == null) {
                frameset = new FrameSet(groupId);
                this.framesetCache.put(frameset, groupId);
            }
            return !frameset.decodeFrames() ? null : frameset;
        }
    }

    public void clearQueues() {
        synchronized (this.sequenceCache) {
            this.sequenceCache.resize();
        }
        synchronized (this.framesetCache) {
            this.framesetCache.resize();
        }
    }

    public void processCacheCycle() {
        synchronized (this.sequenceCache) {
            this.sequenceCache.clean(5);
        }
        synchronized (this.framesetCache) {
            this.framesetCache.clean(5);
        }
    }
}
