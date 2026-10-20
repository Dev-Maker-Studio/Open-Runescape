package com.jagex.game.runetek5.js5

import com.jagex.game.runetek5.buffer.DataBuffer
import com.jagex.game.runetek5.js5.request.FileWorker
import com.jagex.game.runetek5.js5.request.local.LocalFileWorker
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

class Js5ResourceProvider(
    @JvmField var fileWorker: FileWorker,
    @JvmField var directMemoryMode: Boolean,
    @JvmField var loadType: Int
) {

    @JvmField var masterIndex: Js5MasterIndex? = null
    @JvmField var archives: Array<Any?>? = null
    @JvmField var archiveFiles: Array<Array<Any?>?>? = null

    init {
        if (loadType < 0 || loadType > 2) {
            throw IllegalArgumentException("")
        }
    }

    fun getCrc(): Int {
        if (!ensureMasterIndexLoaded()) {
            throw IllegalStateException("")
        }
        return (fileWorker as? LocalFileWorker)?.loginCrc() ?: masterIndex!!.headerCrc
    }

    @Synchronized
    fun archiveExists(archiveId: Int): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        } else if (archiveId >= 0 && archiveId < masterIndex!!.fileCounts.size && masterIndex!!.fileCounts[archiveId] != 0) {
            return true
        } else if (debugMode) {
            throw IllegalArgumentException(archiveId.toString())
        } else {
            return false
        }
    }

    @Synchronized
    fun fileExists(archiveId: Int, fileId: Int): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        } else if (archiveId >= 0 && fileId >= 0 && archiveId < masterIndex!!.fileCounts.size && fileId < masterIndex!!.fileCounts[archiveId]) {
            return true
        } else if (debugMode) {
            throw IllegalArgumentException("$archiveId $fileId")
        } else {
            return false
        }
    }

    @Synchronized
    fun requestArchive(archiveId: Int) {
        if (directMemoryMode) {
            archives!![archiveId] = fileWorker.requestArchive(archiveId)
        } else {
            archives!![archiveId] = DataBuffer.createDataBuffer(fileWorker.requestArchive(archiveId))
        }
    }

    fun prefetchFile(file: Int) {
        fileWorker.prefetch(file)
    }

    fun getFile(archiveId: Int, fileId: Int): ByteArray? {
        return getFile(archiveId, fileId, null)
    }

    fun validFile(fileName: String): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        }
        val lowerFileName = fileName.lowercase()
        val fileId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerFileName))
        return fileId >= 0
    }

    fun getArchiveId(archiveName: String): Int {
        if (!ensureMasterIndexLoaded()) {
            return -1
        }
        val lowerArchiveName = archiveName.lowercase()
        val archiveId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerArchiveName))
        return if (!archiveExists(archiveId)) -1 else archiveId
    }

    @Synchronized
    fun fullyLoaded(): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        }
        if (fileWorker.isLocal) {
            return true
        }
        var loaded = true
        for (archiveIndex in masterIndex!!.validArchiveIds.indices) {
            val archiveId = masterIndex!!.validArchiveIds[archiveIndex]
            if (archives!![archiveId] == null) {
                requestArchive(archiveId)
                if (archives!![archiveId] == null) {
                    loaded = false
                }
            }
        }
        return loaded
    }

    @Synchronized
    fun getArchiveLoadPercentage(archiveId: Int): Int {
        return if (!archiveExists(archiveId)) 0 else if (archives!![archiveId] != null) 100 else fileWorker.getProgress(archiveId)
    }

    @Synchronized
    fun getLoadingPercentage(): Int {
        if (!ensureMasterIndexLoaded()) {
            return 0
        }
        var divider = 0
        var archiveLoadPercent = 0
        for (archiveIndex in archives!!.indices) {
            if (masterIndex!!.archiveFileCount[archiveIndex] > 0) {
                divider += 100
                archiveLoadPercent += getArchiveLoadPercentage(archiveIndex)
            }
        }
        if (divider == 0) {
            return 100
        }
        val loadPercent = archiveLoadPercent * 100 / divider
        return loadPercent
    }

    @Synchronized
    fun getFile(fileId: Int): ByteArray? {
        if (!ensureMasterIndexLoaded()) {
            return null
        }
        if (masterIndex!!.fileCounts.size == 1) {
            return getFile(0, fileId)
        }
        if (!archiveExists(fileId)) {
            return null
        }
        if (masterIndex!!.fileCounts[fileId] == 1) {
            return getFile(fileId, 0)
        }
        throw RuntimeException()
    }

    @Synchronized
    fun getValidFileIds(archiveId: Int): IntArray? {
        if (!archiveExists(archiveId)) {
            return null
        }
        var validFileIds = masterIndex!!.validFileIds!![archiveId]
        if (validFileIds == null) {
            validFileIds = IntArray(masterIndex!!.archiveFileCount[archiveId])
            for (fileId in validFileIds.indices) {
                validFileIds[fileId] = fileId
            }
        }
        return validFileIds
    }

    fun getArchiveCount(): Int {
        return if (!ensureMasterIndexLoaded()) -1 else masterIndex!!.fileCounts.size
    }

    @Synchronized
    fun clearArchiveFiles(archiveId: Int) {
        if (archiveExists(archiveId) && archiveFiles != null) {
            archiveFiles!![archiveId] = null
        }
    }

    fun getFileId(hash: Int): Int {
        if (!ensureMasterIndexLoaded()) {
            return -1
        }
        val archiveId = masterIndex!!.archiveNames!!.getGroupId(hash)
        return if (!archiveExists(archiveId)) -1 else archiveId
    }

    fun getArchiveFileCount(archiveId: Int): Int {
        return if (!archiveExists(archiveId)) 0 else masterIndex!!.fileCounts[archiveId]
    }

    fun namedFileExists(archiveName: String, fileName: String): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        }
        val lowerArchiveName = archiveName.lowercase()
        val lowerFileName = fileName.lowercase()
        val archiveId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerArchiveName))
        if (archiveId < 0) {
            return false
        }
        val fileId = masterIndex!!.namedFiles!![archiveId]!!.getGroupId(jaghash(lowerFileName))
        return fileId >= 0
    }

    @Synchronized
    fun getNamedFile(archiveName: String, fileName: String): ByteArray? {
        if (!ensureMasterIndexLoaded()) {
            return null
        }
        val lowerArchiveName = archiveName.lowercase()
        val lowerFileName = fileName.lowercase()
        val archiveId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerArchiveName))
        if (!archiveExists(archiveId)) {
            return null
        }
        val fileId = masterIndex!!.namedFiles!![archiveId]!!.getGroupId(jaghash(lowerFileName))
        return getFile(archiveId, fileId)
    }

    fun isNamedCached(archiveName: String, fileName: String): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        }
        val lowerArchiveName = archiveName.lowercase()
        val lowerFileName = fileName.lowercase()
        val archiveId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerArchiveName))
        if (!archiveExists(archiveId)) {
            return false
        }
        val fileId = masterIndex!!.namedFiles!![archiveId]!!.getGroupId(jaghash(lowerFileName))
        return isCached(archiveId, fileId)
    }

    fun isNamedCached(archiveName: String): Boolean {
        val archiveId = getArchiveId("")
        return if (archiveId != -1) isNamedCached("", archiveName) else isNamedCached(archiveName, "")
    }

    fun isCached(fileName: String): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        }
        val lowerFileName = fileName.lowercase()
        val archiveId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerFileName))
        return loadArchive(archiveId)
    }

    fun prefetch(fileName: String) {
        if (ensureMasterIndexLoaded()) {
            val lowerFileName = fileName.lowercase()
            val fileId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerFileName))
            prefetchFile(fileId)
        }
    }

    fun getArchiveLoadingPercent(archiveName: String): Int {
        if (!ensureMasterIndexLoaded()) {
            return 0
        }
        val lowerArchiveName = archiveName.lowercase()
        val archiveId = masterIndex!!.archiveNames!!.getGroupId(jaghash(lowerArchiveName))
        return getArchiveLoadPercentage(archiveId)
    }

    @Synchronized
    fun ensureMasterIndexLoaded(): Boolean {
        if (masterIndex == null) {
            masterIndex = fileWorker.getMasterIndex()
            if (masterIndex == null) {
                return false
            }
            archives = arrayOfNulls(masterIndex!!.archiveCount)
            archiveFiles = arrayOfNulls(masterIndex!!.archiveCount)
        }
        return true
    }

    @Synchronized
    fun unpackGroup(archiveId: Int, fileId: Int, xteas: IntArray?): Boolean {
        if (!archiveExists(archiveId)) {
            return false
        }
        if (archives!![archiveId] == null) {
            return false
        }
        val archiveFileCount = masterIndex!!.archiveFileCount[archiveId]
        val archiveFileIds = masterIndex!!.validFileIds!![archiveId]
        if (archiveFiles!![archiveId] == null) {
            archiveFiles!![archiveId] = arrayOfNulls(masterIndex!!.fileCounts[archiveId])
        }
        val fileData = archiveFiles!![archiveId]!!
        var fullyCached = true
        for (fileIndex in 0 until archiveFileCount) {
            val FileId: Int
            if (archiveFileIds == null) {
                FileId = fileIndex
            } else {
                FileId = archiveFileIds[fileIndex]
            }
            if (fileData[FileId] == null) {
                fullyCached = false
                break
            }
        }
        if (!fullyCached) {
            val containerData: ByteArray
            if (xteas == null || xteas[0] == 0 && xteas[1] == 0 && xteas[2] == 0 && xteas[3] == 0) {
                containerData = DataBuffer.unpackDataBuffer(archives!![archiveId]!!, false)!!
            } else {
                containerData = DataBuffer.unpackDataBuffer(archives!![archiveId]!!, true)!!
                val containerBuffer = Packet(containerData)
                containerBuffer.decryptXtea(xteas, containerBuffer.buffer.size)
            }
            val archiveData = Js5Compression.decompress(containerData)
            if (directMemoryMode) {
                archives!![archiveId] = null
            }
            if (archiveFileCount > 1) {
                if (loadType == 2) {
                    var archiveDataLength = archiveData.size
                    val blockCount = archiveData[--archiveDataLength].toInt() and 0xff
                    archiveDataLength -= blockCount * archiveFileCount * 4
                    val archiveBuffer = Packet(archiveData)
                    var blockSizeSum = 0
                    var blockLastFileId = 0
                    archiveBuffer.currentPosition = archiveDataLength
                    for (blockId in 0 until blockCount) {
                        var archiveSizeSum = 0
                        for (fileIndex in 0 until archiveFileCount) {
                            archiveSizeSum += archiveBuffer.readInt()
                            val FileId: Int
                            if (archiveFileIds == null) {
                                FileId = fileIndex
                            } else {
                                FileId = archiveFileIds[fileIndex]
                            }
                            if (FileId == fileId) {
                                blockSizeSum += archiveSizeSum
                                blockLastFileId = FileId
                            }
                        }
                    }
                    if (blockSizeSum == 0) {
                        return true
                    }
                    val blockBuffer = ByteArray(blockSizeSum)
                    var destinationPosition = 0
                    archiveBuffer.currentPosition = archiveDataLength
                    var sourcePosition = 0
                    for (blockIndex in 0 until blockCount) {
                        var blockCopyLength = 0
                        for (fileIndex in 0 until archiveFileCount) {
                            blockCopyLength += archiveBuffer.readInt()
                            val FileId: Int
                            if (archiveFileIds == null) {
                                FileId = fileIndex
                            } else {
                                FileId = archiveFileIds[fileIndex]
                            }
                            if (FileId == fileId) {
                                System.arraycopy(archiveData, sourcePosition, blockBuffer, destinationPosition, blockCopyLength)
                                destinationPosition += blockCopyLength
                            }
                            sourcePosition += blockCopyLength
                        }
                    }
                    fileData[blockLastFileId] = blockBuffer
                } else {
                    var archiveDataLength = archiveData.size
                    val blockCount = archiveData[--archiveDataLength].toInt() and 0xff
                    archiveDataLength -= blockCount * archiveFileCount * 4
                    val archiveBuffer = Packet(archiveData)
                    val fileEntryLengths = IntArray(archiveFileCount)
                    archiveBuffer.currentPosition = archiveDataLength
                    for (blockId in 0 until blockCount) {
                        var fileEntryLengthBuffer = 0
                        for (fileEntryId in 0 until archiveFileCount) {
                            fileEntryLengthBuffer += archiveBuffer.readInt()
                            fileEntryLengths[fileEntryId] += fileEntryLengthBuffer
                        }
                    }
                    val arrangedArchiveData = Array(archiveFileCount) { ByteArray(0) }
                    for (fileIndex in 0 until archiveFileCount) {
                        arrangedArchiveData[fileIndex] = ByteArray(fileEntryLengths[fileIndex])
                        fileEntryLengths[fileIndex] = 0
                    }
                    archiveBuffer.currentPosition = archiveDataLength
                    var copyPosition = 0
                    for (blockId in 0 until blockCount) {
                        var fileLength = 0
                        for (fileIndex in 0 until archiveFileCount) {
                            fileLength += archiveBuffer.readInt()
                            System.arraycopy(archiveData, copyPosition, arrangedArchiveData[fileIndex], fileEntryLengths[fileIndex], fileLength)
                            fileEntryLengths[fileIndex] += fileLength
                            copyPosition += fileLength
                        }
                    }
                    for (fileIndex in 0 until archiveFileCount) {
                        val FileId: Int
                        if (archiveFileIds == null) {
                            FileId = fileIndex
                        } else {
                            FileId = archiveFileIds[fileIndex]
                        }
                        if (loadType == 0) {
                            fileData[FileId] = DataBuffer.createDataBuffer(arrangedArchiveData[fileIndex])
                        } else {
                            fileData[FileId] = arrangedArchiveData[fileIndex]
                        }
                    }
                }
            } else {
                val loadedFileId: Int
                if (archiveFileIds == null) {
                    loadedFileId = 0
                } else {
                    loadedFileId = archiveFileIds[0]
                }
                if (loadType == 0) {
                    fileData[loadedFileId] = DataBuffer.createDataBuffer(archiveData)
                } else {
                    fileData[loadedFileId] = archiveData
                }
            }
        }
        return true
    }

    @Synchronized
    fun getFile(archiveId: Int, fileId: Int, xteas: IntArray?): ByteArray? {
        if (!fileExists(archiveId, fileId)) {
            return null
        }
        if (archiveFiles!![archiveId] == null || archiveFiles!![archiveId]!![fileId] == null) {
            var successful = unpackGroup(archiveId, fileId, xteas)
            if (!successful) {
                requestArchive(archiveId)
                successful = unpackGroup(archiveId, fileId, xteas)
                if (!successful) {
                    return null
                }
            }
        }
        if (archiveFiles!![archiveId] == null) {
            throw RuntimeException("")
        }
        var containerData: ByteArray? = null
        if (archiveFiles!![archiveId]!![fileId] != null) {
            containerData = DataBuffer.unpackDataBuffer(archiveFiles!![archiveId]!![fileId]!!, false)
            if (containerData == null) {
                throw RuntimeException("")
            }
        }
        if (containerData != null) {
            if (loadType == 1) {
                archiveFiles!![archiveId]!![fileId] = null
                if (masterIndex!!.fileCounts[archiveId] == 1) {
                    archiveFiles!![archiveId] = null
                }
            } else if (loadType == 2) {
                archiveFiles!![archiveId] = null
            }
        }
        return containerData
    }

    @Synchronized
    fun loadArchive(archiveId: Int): Boolean {
        if (!archiveExists(archiveId)) {
            return false
        }
        if (archives!![archiveId] != null) {
            return true
        }
        requestArchive(archiveId)
        return archives!![archiveId] != null
    }

    @Synchronized
    fun isFileCached(archiveOrFileId: Int): Boolean {
        if (!ensureMasterIndexLoaded()) {
            return false
        }
        if (masterIndex!!.fileCounts.size == 1) {
            return isCached(0, archiveOrFileId)
        }
        if (!archiveExists(archiveOrFileId)) {
            return false
        }
        if (masterIndex!!.fileCounts[archiveOrFileId] == 1) {
            return isCached(archiveOrFileId, 0)
        }
        throw RuntimeException()
    }

    fun discardUnpacked(discardArchiveNames: Boolean) {
        if (ensureMasterIndexLoaded()) {
            if (discardArchiveNames) {
                masterIndex!!.nameHashes = null
                masterIndex!!.archiveNames = null
            }
            masterIndex!!.fileNameHashes = null
            masterIndex!!.namedFiles = null
        }
    }

    @Synchronized
    fun isCached(archiveId: Int, fileId: Int): Boolean {
        if (!fileExists(archiveId, fileId)) {
            println("File doesn't exist: $archiveId/$fileId")
            return false
        }
        if (archiveFiles!![archiveId] != null && archiveFiles!![archiveId]!![fileId] != null) {
            return true
        }
        if (archives!![archiveId] != null) {
            return true
        }
        requestArchive(archiveId)
        return archives!![archiveId] != null
    }

    companion object {
        @JvmField
        var debugMode: Boolean = false

        @JvmStatic
        fun jaghash(charSequence: CharSequence): Int {
            val charCount = charSequence.length
            var hash = 0
            for (position in 0 until charCount) {
                hash = (hash shl 5) - hash + Utils.byteForChar(charSequence[position])
            }
            return hash
        }
    }
}
