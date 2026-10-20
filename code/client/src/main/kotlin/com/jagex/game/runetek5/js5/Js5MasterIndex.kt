package com.jagex.game.runetek5.js5

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.tools.Hashing

class Js5MasterIndex(tablePayload: ByteArray, expectedCrc: Int, expectedWhirlpool: ByteArray?) {

    @JvmField var revision: Int = 0

    @JvmField var validArchiveCount: Int = 0

    @JvmField var validArchiveIds: IntArray = IntArray(0)

    @JvmField var archiveCount: Int = 0

    @JvmField var crcs: IntArray = IntArray(0)

    @JvmField var whirlpool: Array<ByteArray?>? = null

    @JvmField var versions: IntArray = IntArray(0)

    @JvmField var archiveFileCount: IntArray = IntArray(0)

    @JvmField var validFileIds: Array<IntArray?>? = null

    @JvmField var fileCounts: IntArray = IntArray(0)

    @JvmField var nameHashes: IntArray? = null

    @JvmField var archiveNames: NamedFileReference? = null

    @JvmField var fileNameHashes: Array<IntArray?>? = null

    @JvmField var namedFiles: Array<NamedFileReference?>? = null

    @JvmField val headerCrc: Int

    @JvmField var headerWhirlpool: ByteArray? = null

    init {
        headerCrc = Hashing.getCrc32(tablePayload, tablePayload.size)
        if (expectedCrc != headerCrc) {
            throw RuntimeException()
        }
        if (expectedWhirlpool != null) {
            if (expectedWhirlpool.size != 64) {
                throw RuntimeException()
            }
            headerWhirlpool = Whirlpool.calculateDigest(tablePayload, 0, tablePayload.size)
            for (i in 0 until 64) {
                if (expectedWhirlpool[i] != headerWhirlpool!![i]) {
                    throw RuntimeException()
                }
            }
        }
        decodeTable(tablePayload)
    }

    fun decodeTable(tablePayload: ByteArray) {
        val stream = Packet(Js5Compression.decompress(tablePayload))
        val protocol = stream.readUByte()
        if (protocol in 5..7) {
            if (protocol >= 6) {
                revision = stream.readInt()
            } else {
                revision = 0
            }
            val flags = stream.readUByte()
            val named = (flags and 0x1) != 0
            val hasWhirlpool = (flags and 0x2) != 0
            if (protocol >= 7) {
                validArchiveCount = stream.readUBigSmart()
            } else {
                validArchiveCount = stream.readUShort()
            }
            var offset = 0
            var lastArchiveId = -1
            validArchiveIds = IntArray(validArchiveCount)
            if (protocol >= 7) {
                for (index in 0 until validArchiveCount) {
                    offset += stream.readUBigSmart()
                    validArchiveIds[index] = offset
                    if (validArchiveIds[index] > lastArchiveId) {
                        lastArchiveId = validArchiveIds[index]
                    }
                }
            } else {
                for (index in 0 until validArchiveCount) {
                    offset += stream.readUShort()
                    validArchiveIds[index] = offset
                    if (validArchiveIds[index] > lastArchiveId) {
                        lastArchiveId = validArchiveIds[index]
                    }
                }
            }
            archiveCount = lastArchiveId + 1
            crcs = IntArray(archiveCount)
            if (hasWhirlpool) {
                whirlpool = arrayOfNulls(archiveCount)
            }
            versions = IntArray(archiveCount)
            archiveFileCount = IntArray(archiveCount)
            validFileIds = arrayOfNulls(archiveCount)
            fileCounts = IntArray(archiveCount)
            if (named) {
                nameHashes = IntArray(archiveCount)
                for (archiveId in 0 until archiveCount) {
                    nameHashes!![archiveId] = -1
                }
                for (index in 0 until validArchiveCount) {
                    nameHashes!![validArchiveIds[index]] = stream.readInt()
                }
                archiveNames = NamedFileReference(nameHashes!!)
            }
            for (index in 0 until validArchiveCount) {
                crcs[validArchiveIds[index]] = stream.readInt()
            }
            if (hasWhirlpool) {
                for (index in 0 until validArchiveCount) {
                    val archiveWhirlpool = ByteArray(64)
                    stream.readBytes(archiveWhirlpool, 0, 64)
                    whirlpool!![validArchiveIds[index]] = archiveWhirlpool
                }
            }
            for (index in 0 until validArchiveCount) {
                versions[validArchiveIds[index]] = stream.readInt()
            }
            if (protocol >= 7) {
                for (index in 0 until validArchiveCount) {
                    archiveFileCount[validArchiveIds[index]] = stream.readUBigSmart()
                }
                for (index in 0 until validArchiveCount) {
                    val archiveId = validArchiveIds[index]
                    val fileCount = archiveFileCount[archiveId]
                    offset = 0
                    var maxFileId = -1
                    validFileIds!![archiveId] = IntArray(fileCount)
                    for (archiveFileIndex in 0 until fileCount) {
                        offset += stream.readUBigSmart()
                        val fileId = offset
                        validFileIds!![archiveId]!![archiveFileIndex] = fileId
                        if (fileId > maxFileId) {
                            maxFileId = fileId
                        }
                    }
                    fileCounts[archiveId] = maxFileId + 1
                    if (fileCount == maxFileId + 1) {
                        validFileIds!![archiveId] = null
                    }
                }
            } else {
                for (index in 0 until validArchiveCount) {
                    archiveFileCount[validArchiveIds[index]] = stream.readUShort()
                }
                for (index in 0 until validArchiveCount) {
                    val archiveId = validArchiveIds[index]
                    val fileCount = archiveFileCount[archiveId]
                    offset = 0
                    var maxFileId = -1
                    validFileIds!![archiveId] = IntArray(fileCount)
                    for (archiveFileIndex in 0 until fileCount) {
                        offset += stream.readUShort()
                        val fileId = offset
                        validFileIds!![archiveId]!![archiveFileIndex] = fileId
                        if (fileId > maxFileId) {
                            maxFileId = fileId
                        }
                    }
                    fileCounts[archiveId] = maxFileId + 1
                    if (fileCount == maxFileId + 1) {
                        validFileIds!![archiveId] = null
                    }
                }
            }
            if (named) {
                fileNameHashes = arrayOfNulls(lastArchiveId + 1)
                namedFiles = arrayOfNulls(lastArchiveId + 1)
                for (index in 0 until validArchiveCount) {
                    val archiveId = validArchiveIds[index]
                    val fileCount = archiveFileCount[archiveId]
                    fileNameHashes!![archiveId] = IntArray(fileCounts[archiveId])
                    for (fileId in 0 until fileCounts[archiveId]) {
                        fileNameHashes!![archiveId]!![fileId] = -1
                    }
                    for (archiveFileIndex in 0 until fileCount) {
                        val fileId: Int
                        if (validFileIds!![archiveId] != null) {
                            fileId = validFileIds!![archiveId]!![archiveFileIndex]
                        } else {
                            fileId = archiveFileIndex
                        }
                        fileNameHashes!![archiveId]!![fileId] = stream.readInt()
                    }
                    namedFiles!![archiveId] = NamedFileReference(fileNameHashes!![archiveId]!!)
                }
            }
        } else {
            throw RuntimeException()
        }
    }
}
