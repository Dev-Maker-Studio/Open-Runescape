package com.jagex.game.runetek5.js5;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.tools.Hashing;

/**
 * [J] Generally known as ReferenceTable
 * 
 * @author Steven - 11/10/2023
 *
 */
public class Js5MasterIndex {

	public int revision;

	public int validArchiveCount;

	public int[] validArchiveIds;

	public int archiveCount;

	public int[] crcs;

	public byte[][] whirlpool;

	public int[] versions;

	public int[] archiveFileCount;

	public int[][] validFileIds;

	public int[] fileCounts;

	public int[] nameHashes;

	public NamedFileReference archiveNames;

	public int[][] fileNameHashes;

	public NamedFileReference[] namedFiles;

	public final int headerCrc;

    public byte[] headerWhirlpool;

    public Js5MasterIndex(byte[] data, int crcCheck, byte[] whirlpool) {
        this.headerCrc = Hashing.getCrc32(data, data.length);
        if (crcCheck != this.headerCrc) {
            throw new RuntimeException();
        } 
        if (whirlpool != null) {
            if (whirlpool.length != 64) {
                throw new RuntimeException();
            }
            this.headerWhirlpool = Whirlpool.calculateDigest(data, 0, data.length);
            for (int i = 0; i < 64; i++) {
                if (whirlpool[i] != this.headerWhirlpool[i]) {
                    throw new RuntimeException();
                }
            }
        }
        decodeTable(data);
    }

    public void decodeTable(byte[] tablePayload) {
        Packet stream = new Packet(Js5Compression.decompress(tablePayload));
        int protocol = stream.readUnsignedByte();
        if (protocol >= 5 && protocol <= 7) {
            if (protocol >= 6) {
                this.revision = stream.readInt();
            } else {
                this.revision = 0;
            }
            int flags = stream.readUnsignedByte();
            boolean named = (flags & 0x1) != 0;
            boolean hasWhirlpool = (flags & 0x2) != 0;
            if (protocol >= 7) {
                this.validArchiveCount = stream.readUnsignedBigSmart();
            } else {
                this.validArchiveCount = stream.readUnsignedShort();
            }
            int offset = 0;
            int lastArchiveId = -1;
            this.validArchiveIds = new int[this.validArchiveCount];
            if (protocol >= 7) {
                for (int archiveIndex = 0; archiveIndex < this.validArchiveCount; archiveIndex++) {
                    this.validArchiveIds[archiveIndex] = offset += stream.readUnsignedBigSmart();
                    if (this.validArchiveIds[archiveIndex] > lastArchiveId) {
                        lastArchiveId = this.validArchiveIds[archiveIndex];
                    }
                }
            } else {
                for (int archiveIndex = 0; archiveIndex < this.validArchiveCount; archiveIndex++) {
                    this.validArchiveIds[archiveIndex] = offset += stream.readUnsignedShort();
                    if (this.validArchiveIds[archiveIndex] > lastArchiveId) {
                        lastArchiveId = this.validArchiveIds[archiveIndex];
                    }
                }
            }
            this.archiveCount = lastArchiveId + 1;
            this.crcs = new int[this.archiveCount];
            if (hasWhirlpool) {
                this.whirlpool = new byte[this.archiveCount][];
            }
            this.versions = new int[this.archiveCount];
            this.archiveFileCount = new int[this.archiveCount];
            this.validFileIds = new int[this.archiveCount][];
            this.fileCounts = new int[this.archiveCount];
            if (named) {
                this.nameHashes = new int[this.archiveCount];
                for (int index = 0; index < this.archiveCount; index++) {
                    this.nameHashes[index] = -1;
                }
                for (int index = 0; index < this.validArchiveCount; index++) {
                    this.nameHashes[this.validArchiveIds[index]] = stream.readInt();
                }
                this.archiveNames = new NamedFileReference(this.nameHashes);
            }
            for (int i_9 = 0; i_9 < this.validArchiveCount; i_9++) {
                this.crcs[this.validArchiveIds[i_9]] = stream.readInt();
            }
            if (hasWhirlpool) {
                for (int index = 0; index < this.validArchiveCount; index++) {
                    byte[] archiveWhirlpool = new byte[64];
                    stream.readBytes(archiveWhirlpool, 0, 64);
                    this.whirlpool[this.validArchiveIds[index]] = archiveWhirlpool;
                }
            }
            for (int index = 0; index < this.validArchiveCount; index++) {
                this.versions[this.validArchiveIds[index]] = stream.readInt();
            }
            if (protocol >= 7) {
                for (int index = 0; index < this.validArchiveCount; index++) {
                    this.archiveFileCount[this.validArchiveIds[index]] = stream.readUnsignedBigSmart();
                }
                for (int index = 0; index < this.validArchiveCount; index++) {
                    int archiveId = this.validArchiveIds[index];
                    int fileCount = this.archiveFileCount[archiveId];
                    offset = 0;
                    int maxFileId = -1;
                    this.validFileIds[archiveId] = new int[fileCount];
                    for (int archiveFileIndex = 0; archiveFileIndex < fileCount; archiveFileIndex++) {
                        int fileId = this.validFileIds[archiveId][archiveFileIndex] = offset += stream.readUnsignedBigSmart();
                        if (fileId > maxFileId) {
                            maxFileId = fileId;
                        }
                    }
                    this.fileCounts[archiveId] = maxFileId + 1;
                    if (fileCount == maxFileId + 1) {
                        this.validFileIds[archiveId] = null;
                    }
                }
            } else {
                for (int index = 0; index < this.validArchiveCount; index++) {
                    this.archiveFileCount[this.validArchiveIds[index]] = stream.readUnsignedShort();
                }
                for (int index = 0; index < this.validArchiveCount; index++) {
                    int archiveId = this.validArchiveIds[index];
                    int fileCount = this.archiveFileCount[archiveId];
                    offset = 0;
                    int maxFileId = -1;
                    this.validFileIds[archiveId] = new int[fileCount];
                    for (int archiveFileIndex = 0; archiveFileIndex < fileCount; archiveFileIndex++) {
                    	int fileId = this.validFileIds[archiveId][archiveFileIndex] = offset += stream.readUnsignedShort();
                        if (fileId > maxFileId) {
                            maxFileId = fileId;
                        }
                    }
                    this.fileCounts[archiveId] = maxFileId + 1;
                    if (fileCount == maxFileId + 1) {
                        this.validFileIds[archiveId] = null;
                    }
                }
            }
            if (named) {
                this.fileNameHashes = new int[lastArchiveId + 1][];
                this.namedFiles = new NamedFileReference[lastArchiveId + 1];
                for (int index = 0; index < this.validArchiveCount; index++) {
                	int archiveId = this.validArchiveIds[index];
                	int fileCount = this.archiveFileCount[archiveId];
                    this.fileNameHashes[archiveId] = new int[this.fileCounts[archiveId]];
                    for (int file = 0; file < this.fileCounts[archiveId]; file++) {
                        this.fileNameHashes[archiveId][file] = -1;
                    }
                    for (int archiveFileIndex = 0; archiveFileIndex < fileCount; archiveFileIndex++) {
                    	int fileId;
                        if (this.validFileIds[archiveId] != null) {
                            fileId = this.validFileIds[archiveId][archiveFileIndex];
                        } else {
                            fileId = archiveFileIndex;
                        }
                        this.fileNameHashes[archiveId][fileId] = stream.readInt();
                    }
                    this.namedFiles[archiveId] = new NamedFileReference(this.fileNameHashes[archiveId]);
                }
            }
        } else {
            throw new RuntimeException();
        }
    }
}
