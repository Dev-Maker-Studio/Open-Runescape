package com.jagex.game.runetek5.js5

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.tools.Hashing

/**
 * [J] Generally known as ReferenceTable
 *
 * @author Steven - 11/10/2023
 *
 */
class Js5MasterIndex(data: ByteArray, crcCheck: Int, whirlpool: ByteArray?) {

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
        headerCrc = Hashing.getCrc32(data, data.size)
        if (crcCheck != headerCrc) {
            throw RuntimeException()
        }
        if (whirlpool != null) {
            if (whirlpool.size != 64) {
                throw RuntimeException()
            }
            headerWhirlpool = Whirlpool.calculateDigest(data, 0, data.size)
            for (i in 0 until 64) {
                if (whirlpool[i] != headerWhirlpool!![i]) {
                    throw RuntimeException()
                }
            }
        }
        decodeTable(data)
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
                for (archiveIndex in 0 until validArchiveCount) {
                    offset += stream.readUBigSmart()
                    validArchiveIds[archiveIndex] = offset
                    if (validArchiveIds[archiveIndex] > lastArchiveId) {
                        lastArchiveId = validArchiveIds[archiveIndex]
                    }
                }
            } else {
                for (archiveIndex in 0 until validArchiveCount) {
                    offset += stream.readUShort()
                    validArchiveIds[archiveIndex] = offset
                    if (validArchiveIds[archiveIndex] > lastArchiveId) {
                        lastArchiveId = validArchiveIds[archiveIndex]
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
                for (index in 0 until archiveCount) {
                    nameHashes!![index] = -1
                }
                for (index in 0 until validArchiveCount) {
                    nameHashes!![validArchiveIds[index]] = stream.readInt()
                }
                archiveNames = NamedFileReference(nameHashes!!)
            }
            for (i_9 in 0 until validArchiveCount) {
                crcs[validArchiveIds[i_9]] = stream.readInt()
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
                    for (file in 0 until fileCounts[archiveId]) {
                        fileNameHashes!![archiveId]!![file] = -1
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
