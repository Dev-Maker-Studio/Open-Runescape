package com.jagex.game.runetek5.js5;

import com.jagex.game.runetek5.buffer.DataBuffer;
import com.jagex.game.runetek5.js5.request.FileWorker;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

import java.util.Arrays;

/**
 * [J]
 * 
 * @author Steven - 11/10/2023
 *
 */
public class Js5ResourceProvider {

    public static boolean debugMode;
    public Js5MasterIndex masterIndex;
    public Object[] archives;
    public Object[][] archiveFiles;
    public FileWorker fileWorker;
    public boolean directMemoryMode;
    public int loadType;

    public Js5ResourceProvider(FileWorker fileWorker, boolean directMemoryMode, int loadType) {
        if (loadType >= 0 && loadType <= 2) {
            this.fileWorker = fileWorker;
            this.directMemoryMode = directMemoryMode;
            this.loadType = loadType;
        } else {
            throw new IllegalArgumentException("");
        }
    }

    public int getCrc() {
        if (!ensureMasterIndexLoaded()) {
            throw new IllegalStateException("");
        }
        return this.masterIndex.headerCrc;
    }

    public synchronized boolean archiveExists(int archiveId) {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } else if (archiveId >= 0 && archiveId < this.masterIndex.fileCounts.length && this.masterIndex.fileCounts[archiveId] != 0) {
            return true;
        } else if (debugMode) {
            throw new IllegalArgumentException(Integer.toString(archiveId));
        } else {
            return false;
        }
    }

    public synchronized boolean fileExists(int archiveId, int fileId) {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } else if (archiveId >= 0 && fileId >= 0 && archiveId < this.masterIndex.fileCounts.length && fileId < this.masterIndex.fileCounts[archiveId]) {
            return true;
        } else if (debugMode) {
            throw new IllegalArgumentException(archiveId + " " + fileId);
        } else {
            return false;
        }
    }

    public synchronized void requestArchive(int archiveId) {
        if (this.directMemoryMode) {
            this.archives[archiveId] = this.fileWorker.requestArchive(archiveId);
        } else {
            byte[] file = this.fileWorker.requestArchive(archiveId);
            if (67900 == archiveId) {
                System.out.println("Requesting archive (" + this.fileWorker + "): " + archiveId + " - " + Arrays.toString(file));
            }
            this.archives[archiveId] = DataBuffer.createDataBuffer(file);
        }
    }

    public void prefetchFile(int file) {
        this.fileWorker.prefetch(file);
    }

    public byte[] getFile(int archiveId, int fileId) {
        return getFile(archiveId, fileId, null);
    }

    public boolean validFile(String fileName) {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } 
        fileName = fileName.toLowerCase();
        int fileId = this.masterIndex.archiveNames.getGroupId(jaghash(fileName));
        return fileId >= 0;
    }
    
    public static int jaghash(CharSequence charSequence) {
        int charCount = charSequence.length();
        int hash = 0;
        for (int position = 0; position < charCount; position++) {
            hash = (hash << 5) - hash + Utils.byteForChar(charSequence.charAt(position));
        }
        return hash;
    }

    public int getArchiveId(String archiveName) {
        if (!ensureMasterIndexLoaded()) {
            return -1;
        } 
        archiveName = archiveName.toLowerCase();
        int archiveId = this.masterIndex.archiveNames.getGroupId(jaghash(archiveName));
        return !archiveExists(archiveId) ? -1 : archiveId;
    }

    public synchronized boolean fullyLoaded() {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } 
        boolean loaded = true;
        for (int archiveIndex = 0; archiveIndex < this.masterIndex.validArchiveIds.length; archiveIndex++) {
            int archiveId = this.masterIndex.validArchiveIds[archiveIndex];
            if (this.archives[archiveId] == null) {
                requestArchive(archiveId);
                if (this.archives[archiveId] == null) {
                    loaded = false;
                }
            }
        }
        return loaded;
    }

    public synchronized int getArchiveLoadPercentage(int archiveId) {
        return !archiveExists(archiveId) ? 0 : (this.archives[archiveId] != null ? 100 : this.fileWorker.getProgress(archiveId));
    }

    public synchronized int getLoadingPercentage() {
        if (!ensureMasterIndexLoaded()) {
            return 0;
        } 
        int divider = 0;
        int archiveLoadPercent = 0;
        for (int archiveIndex = 0; archiveIndex < this.archives.length; archiveIndex++) {
            if (this.masterIndex.archiveFileCount[archiveIndex] > 0) {
                divider += 100;
                archiveLoadPercent += getArchiveLoadPercentage(archiveIndex);
            }
        }
        if (divider == 0) {
            return 100;
        } 
        int loadPercent = archiveLoadPercent * 100 / divider;
        return loadPercent;
    }

    public synchronized byte[] getFile(int fileId) {
        if (!ensureMasterIndexLoaded()) {
            return null;
        } 
        if (this.masterIndex.fileCounts.length == 1) {
            return getFile(0, fileId);
        } 
        if (!archiveExists(fileId)) {
            return null;
        } 
        if (this.masterIndex.fileCounts[fileId] == 1) {
            return getFile(fileId, 0);
        } 
        throw new RuntimeException();
    }

    public synchronized int[] getValidFileIds(int archiveId) {
        if (!archiveExists(archiveId)) {
            return null;
        } 
        int[] validFileIds = this.masterIndex.validFileIds[archiveId];
        if (validFileIds == null) {
            validFileIds = new int[this.masterIndex.archiveFileCount[archiveId]];
            for (int fileId = 0; fileId < validFileIds.length; validFileIds[fileId] = fileId++) { }
        }
        return validFileIds;
    }

    public int getArchiveCount() {
        return !ensureMasterIndexLoaded() ? -1 : this.masterIndex.fileCounts.length;
    }

    public synchronized void clearArchiveFiles(int archiveId) {
        if (archiveExists(archiveId) && this.archiveFiles != null) {
            this.archiveFiles[archiveId] = null;
        }
    }

    public int getFileId(int hash) {
        if (!ensureMasterIndexLoaded()) {
            return -1;
        } 
        int archiveId = this.masterIndex.archiveNames.getGroupId(hash);
        return !archiveExists(archiveId) ? -1 : archiveId;
    }

    public int getArchiveFileCount(int archiveId) {
        return !archiveExists(archiveId) ? 0 : this.masterIndex.fileCounts[archiveId];
    }

    public boolean namedFileExists(String archiveName, String fileName) {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } 
        archiveName = archiveName.toLowerCase();
        fileName = fileName.toLowerCase();
        int archiveId = this.masterIndex.archiveNames.getGroupId(jaghash(archiveName));
        if (archiveId < 0) {
            return false;
        } 
        int fileId = this.masterIndex.namedFiles[archiveId].getGroupId(jaghash(fileName));
        return fileId >= 0;
    }

    public synchronized byte[] getNamedFile(String archiveName, String fileName) {
        if (!ensureMasterIndexLoaded()) {
            return null;
        } 
        archiveName = archiveName.toLowerCase();
        fileName = fileName.toLowerCase();
        int archiveId = this.masterIndex.archiveNames.getGroupId(jaghash(archiveName));
        if (!archiveExists(archiveId)) {
            return null;
        } 
        int fileId = this.masterIndex.namedFiles[archiveId].getGroupId(jaghash(fileName));
        return getFile(archiveId, fileId);
    }

    public boolean isNamedCached(String archiveName, String fileName) {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } 
        archiveName = archiveName.toLowerCase();
        fileName = fileName.toLowerCase();
        int archiveId = this.masterIndex.archiveNames.getGroupId(jaghash(archiveName));
        if (!archiveExists(archiveId)) {
            return false;
        } 
        int fileId = this.masterIndex.namedFiles[archiveId].getGroupId(jaghash(fileName));
        return isCached(archiveId, fileId);
    }

    public boolean isNamedCached(String archiveName) {
        int archiveId = getArchiveId("");
        return archiveId != -1 ? isNamedCached("", archiveName) : isNamedCached(archiveName, "");
    }

    public boolean isCached(String fileName) {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } 
        fileName = fileName.toLowerCase();
        int archiveId = this.masterIndex.archiveNames.getGroupId(jaghash(fileName));
        return loadArchive(archiveId);
    }

    public void prefetch(String fileName) {
        if (ensureMasterIndexLoaded()) {
            fileName = fileName.toLowerCase();
            int fileId = this.masterIndex.archiveNames.getGroupId(jaghash(fileName));
            prefetchFile(fileId);
        }
    }

    public int getArchiveLoadingPercent(String archiveName) {
        if (!ensureMasterIndexLoaded()) {
            return 0;
        } 
        archiveName = archiveName.toLowerCase();
        int archiveId = this.masterIndex.archiveNames.getGroupId(jaghash(archiveName));
        return getArchiveLoadPercentage(archiveId);
    }

    public synchronized boolean ensureMasterIndexLoaded() {
        if (this.masterIndex == null) {
            this.masterIndex = this.fileWorker.getMasterIndex();
            if (this.masterIndex == null) {
                return false;
            }
            this.archives = new Object[this.masterIndex.archiveCount];
            this.archiveFiles = new Object[this.masterIndex.archiveCount][];
        }
        return true;
    }

    public synchronized boolean unpackGroup(int archiveId, int fileId, int[] xteas) {
        if (!archiveExists(archiveId)) {
            return false;
        } 
        if (this.archives[archiveId] == null) {
            return false;
        } 
        int archiveFileCount = this.masterIndex.archiveFileCount[archiveId];
        int[] archiveFileIds = this.masterIndex.validFileIds[archiveId];
        if (this.archiveFiles[archiveId] == null) {
            this.archiveFiles[archiveId] = new Object[this.masterIndex.fileCounts[archiveId]];
        }
        Object[] fileData = this.archiveFiles[archiveId];
        boolean fullyCached = true;
        for (int fileIndex = 0; fileIndex < archiveFileCount; fileIndex++) {
            int FileId;
            if (archiveFileIds == null) {
                FileId = fileIndex;
            } else {
                FileId = archiveFileIds[fileIndex];
            }
            if (fileData[FileId] == null) {
                fullyCached = false;
                break;
            }
        }
        if (!fullyCached) {
            byte[] containerData;
            if (xteas == null || xteas[0] == 0 && xteas[1] == 0 && xteas[2] == 0 && xteas[3] == 0) {
                containerData = DataBuffer.unpackDataBuffer(this.archives[archiveId], false);
            } else {
                containerData = DataBuffer.unpackDataBuffer(this.archives[archiveId], true);
                Packet containerBuffer = new Packet(containerData);
                containerBuffer.decryptXtea(xteas, containerBuffer.buffer.length);
            }
            byte[] archiveData = Js5Compression.decompress(containerData);
            if (this.directMemoryMode) {
                this.archives[archiveId] = null;
            }
            if (archiveFileCount > 1) {
                if (this.loadType == 2) {
                    int archiveDataLength = archiveData.length;
                    int blockCount = archiveData[--archiveDataLength] & 0xff;
                    archiveDataLength -= blockCount * archiveFileCount * 4;
                    Packet archiveBuffer = new Packet(archiveData);
                    int blockSizeSum = 0;
                    int blockLastFileId = 0;
                    archiveBuffer.currentPosition = archiveDataLength;
                    for (int blockId = 0; blockId < blockCount; blockId++) {
                    	int archiveSizeSum = 0;
                        for (int fileIndex = 0; fileIndex < archiveFileCount; fileIndex++) {
                            archiveSizeSum += archiveBuffer.readInt();
                            int FileId;
                            if (archiveFileIds == null) {
                                FileId = fileIndex;
                            } else {
                                FileId = archiveFileIds[fileIndex];
                            }
                            if (FileId == fileId) {
                                blockSizeSum += archiveSizeSum;
                                blockLastFileId = FileId;
                            }
                        }
                    }
                    if (blockSizeSum == 0) {
                        return true;
                    }
                    byte[] blockBuffer = new byte[blockSizeSum];
                    int destinationPosition = 0;
                    archiveBuffer.currentPosition = archiveDataLength;
                    int sourcePosition = 0;
                    for (int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
                    	int blockCopyLength = 0;
                        for (int fileIndex = 0; fileIndex < archiveFileCount; fileIndex++) {
                            blockCopyLength += archiveBuffer.readInt();
                            int FileId;
                            if (archiveFileIds == null) {
                                FileId = fileIndex;
                            } else {
                                FileId = archiveFileIds[fileIndex];
                            }
                            if (FileId == fileId) {
                                System.arraycopy(archiveData, sourcePosition, blockBuffer, destinationPosition, blockCopyLength);
                                destinationPosition += blockCopyLength;
                            }
                            sourcePosition += blockCopyLength;
                        }
                    }
                    fileData[blockLastFileId] = blockBuffer;
                } else {
                    int archiveDataLength = archiveData.length;
                    int blockCount = archiveData[--archiveDataLength] & 0xff;
                    archiveDataLength -= blockCount * archiveFileCount * 4;
                    Packet archiveBuffer = new Packet(archiveData);
                    int[] fileEntryLengths = new int[archiveFileCount];
                    archiveBuffer.currentPosition = archiveDataLength;
                    for (int blockId = 0; blockId < blockCount; blockId++) {
                    	int fileEntryLengthBuffer = 0;
                        for (int fileEntryId = 0; fileEntryId < archiveFileCount; fileEntryId++) {
                            fileEntryLengthBuffer += archiveBuffer.readInt();
                            fileEntryLengths[fileEntryId] += fileEntryLengthBuffer;
                        }
                    }
                    byte[][] arrangedArchiveData = new byte[archiveFileCount][];
                    for (int fileIndex = 0; fileIndex < archiveFileCount; fileIndex++) {
                        arrangedArchiveData[fileIndex] = new byte[fileEntryLengths[fileIndex]];
                        fileEntryLengths[fileIndex] = 0;
                    }
                    archiveBuffer.currentPosition = archiveDataLength;
                    int copyPosition = 0;
                    for (int blockId = 0; blockId < blockCount; blockId++) {
                    	int fileLength = 0;
                        for (int fileIndex = 0; fileIndex < archiveFileCount; fileIndex++) {
                            fileLength += archiveBuffer.readInt();
                            System.arraycopy(archiveData, copyPosition, arrangedArchiveData[fileIndex], fileEntryLengths[fileIndex], fileLength);
                            fileEntryLengths[fileIndex] += fileLength;
                            copyPosition += fileLength;
                        }
                    }
                    for (int fileIndex = 0; fileIndex < archiveFileCount; fileIndex++) {
                    	int FileId;
                        if (archiveFileIds == null) {
                            FileId = fileIndex;
                        } else {
                            FileId = archiveFileIds[fileIndex];
                        }
                        if (this.loadType == 0) {
                            fileData[FileId] = DataBuffer.createDataBuffer(arrangedArchiveData[fileIndex]);
                        } else {
                            fileData[FileId] = arrangedArchiveData[fileIndex];
                        }
                    }
                }
            } else {
            	int loadedFileId;
                if (archiveFileIds == null) {
                    loadedFileId = 0;
                } else {
                    loadedFileId = archiveFileIds[0];
                }
                if (this.loadType == 0) {
                    fileData[loadedFileId] = DataBuffer.createDataBuffer(archiveData);
                } else {
                    fileData[loadedFileId] = archiveData;
                }
            }
        }
        return true;
    }

    public synchronized byte[] getFile(int archiveId, int fileId, int[] xteas) {
        if (!fileExists(archiveId, fileId)) {
            return null;
        } 
        if (this.archiveFiles[archiveId] == null || this.archiveFiles[archiveId][fileId] == null) {
            boolean successful = unpackGroup(archiveId, fileId, xteas);
            if (!successful) {
                requestArchive(archiveId);
                successful = unpackGroup(archiveId, fileId, xteas);
                if (!successful) {
                    return null;
                }
            }
        }
        if (this.archiveFiles[archiveId] == null) {
            throw new RuntimeException("");
        } 
        byte[] containerData = null;
        if (this.archiveFiles[archiveId][fileId] != null) {
            containerData = DataBuffer.unpackDataBuffer(this.archiveFiles[archiveId][fileId], false);
            if (containerData == null) {
                throw new RuntimeException("");
            }
        }
        if (containerData != null) {
            if (this.loadType == 1) {
                this.archiveFiles[archiveId][fileId] = null;
                if (this.masterIndex.fileCounts[archiveId] == 1) {
                    this.archiveFiles[archiveId] = null;
                }
            } else if (this.loadType == 2) {
                this.archiveFiles[archiveId] = null;
            }
        }
        return containerData;
    }

    public synchronized boolean loadArchive(int archiveId) {
        if (!archiveExists(archiveId)) {
            return false;
        } 
        if (this.archives[archiveId] != null) {
            return true;
        } 
        requestArchive(archiveId);
        return this.archives[archiveId] != null;
    }

    public synchronized boolean isFileCached(int archiveOrFileId) {
        if (!ensureMasterIndexLoaded()) {
            return false;
        } 
        if (this.masterIndex.fileCounts.length == 1) {
            return isCached(0, archiveOrFileId);
        } 
        if (!archiveExists(archiveOrFileId)) {
            return false;
        } 
        if (this.masterIndex.fileCounts[archiveOrFileId] == 1) {
            return isCached(archiveOrFileId, 0);
        } 
        throw new RuntimeException();
    }

    public void discardUnpacked(boolean discardArchiveNames) {
        if (ensureMasterIndexLoaded()) {
            if (discardArchiveNames) {
                this.masterIndex.nameHashes = null;
                this.masterIndex.archiveNames = null;
            }
            this.masterIndex.fileNameHashes = null;
            this.masterIndex.namedFiles = null;
        }
    }

    public synchronized boolean isCached(int archiveId, int fileId) {
        if (!fileExists(archiveId, fileId)) {
            System.out.println("File doesn't exist: " + archiveId + "/" + fileId);
            return false;
        } 
        if (this.archiveFiles[archiveId] != null && this.archiveFiles[archiveId][fileId] != null) {
            return true;
        } 
        if (this.archives[archiveId] != null) {
            return true;
        } 
        requestArchive(archiveId);
        return this.archives[archiveId] != null;
    }
}
