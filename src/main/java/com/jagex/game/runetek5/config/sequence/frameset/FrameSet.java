package com.jagex.game.runetek5.config.sequence.frameset;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class FrameSet extends CacheableNode {

	public static Js5ResourceProvider framebaseResource;
    public static Js5ResourceProvider frameskeletonResource;
    public final int groupId;
    public byte[][] frameData;
    public AnimFrame[] frames;

    public FrameSet(int groupId) {
        this.groupId = groupId;
    }

    public static void initialize(Js5ResourceProvider skeletonResource, Js5ResourceProvider baseResource) {
        frameskeletonResource = skeletonResource;
        framebaseResource = baseResource;
    }

    public boolean modifiesAlpha(int frameIndex) {
        return this.frames[frameIndex].modifiesAlpha;
    }

    public boolean modifiesColors(int frameIndex) {
        return this.frames[frameIndex].modifiesColor;
    }

    public boolean modifiesBillboards(int frameIndex) {
        return this.frames[frameIndex].modifiesBillboard;
    }

    public boolean decodeFrames() {
        if (this.frames != null) {
            return true;
        } 
        if (this.frameData == null) {
            synchronized (frameskeletonResource) {
                if (!frameskeletonResource.loadArchive(this.groupId)) {
                    return false;
                }
                int[] fileIds = frameskeletonResource.getValidFileIds(this.groupId);
                this.frameData = new byte[fileIds.length][];
                for (int index = 0; index < fileIds.length; index++) {
                    this.frameData[index] = frameskeletonResource.getFile(this.groupId, fileIds[index]);
                }
            }
        }
        boolean loaded = true;
        for (int index = 0; index < this.frameData.length; index++) {
            byte[] data = this.frameData[index];
            Packet buffer = new Packet(data);
            buffer.currentPosition = 1;
            int frameId = buffer.readUnsignedShort();
            synchronized (framebaseResource) {
                loaded &= framebaseResource.isFileCached(frameId);
            }
        }
        if (!loaded) {
            return false;
        } 
        LinkedList frameBaseList = new LinkedList();
        int[] fileIds;
        synchronized (frameskeletonResource) {
            int fileCount = frameskeletonResource.getArchiveFileCount(this.groupId);
            this.frames = new AnimFrame[fileCount];
            fileIds = frameskeletonResource.getValidFileIds(this.groupId);
        }
        for (int fileIndex = 0; fileIndex < fileIds.length; fileIndex++) {
            byte[] data = this.frameData[fileIndex];
            Packet buffer = new Packet(data);
            buffer.currentPosition = 1;
            int frameId = buffer.readUnsignedShort();
            AnimBase frameBase = null;
            for (AnimBase sequenceBase = (AnimBase) frameBaseList.getTail(); sequenceBase != null; sequenceBase = (AnimBase) frameBaseList.getPrevious()) {
                if (frameId == sequenceBase.id) {
                    frameBase = sequenceBase;
                    break;
                }
            }
            if (frameBase == null) {
                synchronized (framebaseResource) {
                    frameBase = new AnimBase(frameId, framebaseResource.getFile(frameId));
                }
                frameBaseList.insertTail(frameBase);
            }
            this.frames[fileIds[fileIndex]] = new AnimFrame(data, frameBase);
        }
        this.frameData = null;
        return true;
    }
}