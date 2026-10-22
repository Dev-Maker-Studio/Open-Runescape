import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.IndexReference
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.varp.VarpTypeList
import com.jagex.game.runetek5.config.varp.VarpbitType
import com.jagex.game.runetek5.config.varp.VarpbitTypeList
import com.jagex.game.runetek5.js5.Js5Compression
import com.jagex.game.runetek5.js5.Js5MasterIndex
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.js5.Whirlpool
import com.jagex.game.runetek5.js5.request.FileWorker
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.PlayerVarDomain
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.HexFormat
import java.util.zip.CRC32

private fun hex(bytes: ByteArray) = HexFormat.of().formatHex(bytes)
private fun bytes(hex: String) = HexFormat.of().parseHex(hex)
private fun crc(bytes: ByteArray) = CRC32().apply { update(bytes) }.value.toInt()
private fun digest(bytes: ByteArray) = Whirlpool.calculateDigest(bytes, 0, bytes.size)
private fun binary(write: DataOutputStream.() -> Unit): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use(write)
    return bytes.toByteArray()
}
private fun container(payload: ByteArray) = binary {
    writeByte(0)
    writeInt(payload.size)
    write(payload)
}.also { check(Js5Compression.decompress(it).contentEquals(payload)) }
private fun definition(varp: Int, start: Int, end: Int) = binary {
    writeByte(1)
    writeShort(varp)
    writeByte(start)
    writeByte(end)
    writeByte(0)
}
private fun fields(definition: VarpbitType) = buildJsonObject {
    put("varp", definition.configIndex)
    put("start", definition.startBit)
    put("end", definition.endBit)
}
private fun decoded(input: ByteArray): JsonObject {
    val definition = VarpbitType()
    val packet = Packet(input)
    return buildJsonObject {
        put("hex", hex(input))
        try {
            definition.decode(packet)
            put("definition", fields(definition))
            put("consumed", packet.currentPosition)
        } catch (error: Exception) {
            put("error", error.javaClass.simpleName)
        }
    }
}
private fun pack(files: Map<Int, ByteArray>) = if (files.size == 1) files.values.single() else binary {
    files.values.forEach { write(it) }
    var previous = 0
    files.values.forEach {
        writeInt(it.size - previous)
        previous = it.size
    }
    writeByte(1)
}

fun main() {
    val sources = sortedMapOf(
        0 to sortedMapOf(0 to definition(318, 0, 0), 1023 to definition(65535, 31, 31)),
        1 to sortedMapOf(0 to bytes("00"), 7 to definition(568, 10, 12), 12 to definition(2, 34, 41)),
        64 to sortedMapOf(1 to definition(2, 0, 31))
    )
    val archives = sources.mapValues { container(pack(it.value)) }
    val reference = container(binary {
        writeByte(6)
        writeInt(17)
        writeByte(2)
        writeShort(archives.size)
        var previous = 0
        archives.keys.forEach { writeShort(it - previous); previous = it }
        archives.values.forEach { writeInt(crc(it)) }
        archives.values.forEach { write(digest(it)) }
        archives.keys.forEach { writeInt(it + 20) }
        sources.values.forEach { writeShort(it.size) }
        sources.values.forEach { files ->
            previous = 0
            files.keys.forEach { writeShort(it - previous); previous = it }
        }
    })
    val index = Js5MasterIndex(reference, crc(reference), digest(reference))
    check(index.revision == 17 && index.validArchiveCount == sources.size)
    fun provider(available: Boolean) = Js5ResourceProvider(object : FileWorker() {
        override fun getMasterIndex() = index
        override fun requestArchive(archiveId: Int) = if (available) archives[archiveId] else null
        override fun prefetch(fileId: Int) = Unit
        override fun getProgress(fileId: Int) = if (available) 100 else 0
    }, false, 0)
    val loaded = provider(true)
    check(loaded.ensureMasterIndexLoaded())
    val loader = VarpbitTypeList(loaded)
    val report = buildJsonObject {
        put("index", IndexReference.INDEX_22.indexId)
        put("configTypeId", SharedConfigsType.VARPBIT.id)
        put("archiveBits", SharedConfigsType.VARPBIT.bit)
        put("mapping", buildJsonArray {
            for (id in listOf(0, 1, 1023, 1024, 1031, 65535, 65536, 0x03ffffff)) add(buildJsonObject {
                put("id", id)
                put("archive", SharedConfigsType.VARPBIT.archiveId(id))
                put("file", SharedConfigsType.VARPBIT.archiveFileId(id))
            })
        })
        put("definitions", buildJsonArray {
            add(decoded(bytes("00")))
            add(decoded(bytes("010002040701ffff202700")))
            var n = 0
            for (start in listOf(0, 1, 7, 31, 32, 34, 63, 128, 224, 254, 255)) {
                for (width in 0..31) {
                    if (start + width > 255) continue
                    add(decoded(definition(listOf(0, 255, 256, 65535)[n++ % 4], start, start + width)))
                }
            }
        })
        put("nonCanonical", buildJsonArray {
            for (value in listOf("", "01", "010001", "01000101", "0100010101", "02", "0200", "0001", "010001010000", "010001002000")) {
                add(decoded(bytes(value)))
            }
        })
        put("referenceHex", hex(reference))
        put("version", buildJsonObject {
            put("crc", crc(reference))
            put("revision", index.revision)
            put("whirlpoolHex", hex(digest(reference)))
        })
        put("archives", buildJsonArray {
            for ((archiveId, source) in sources) add(buildJsonObject {
                put("id", archiveId)
                put("containerHex", hex(archives.getValue(archiveId)))
                put("files", buildJsonArray {
                    for ((fileId, input) in source) {
                        val native = loaded.getFile(archiveId, fileId)
                        check(native != null && native.contentEquals(input))
                        add(buildJsonObject {
                            put("id", fileId)
                            put("hex", hex(native))
                            put("definition", fields(loader.list((archiveId shl 10) or fileId)))
                        })
                    }
                })
            })
        })
        put("lookups", buildJsonArray {
            for (available in listOf(false, true)) {
                for (id in listOf(0, 1, 1023, 1024, 1031, 1036, 2048, 65537)) {
                    val resource = provider(available)
                    val table = VarpbitTypeList(resource)
                    val archive = SharedConfigsType.VARPBIT.archiveId(id)
                    val file = SharedConfigsType.VARPBIT.archiveFileId(id)
                    add(buildJsonObject {
                        put("available", available)
                        put("id", id)
                        put("fileReturned", resource.getFile(archive, file) != null)
                        put("definition", fields(table.list(id)))
                    })
                }
            }
        })
        put("hostCases", buildJsonArray {
            val varps = VarpTypeList(null)
            VarpTypeList::class.java.getDeclaredField("size").apply { isAccessible = true; setInt(varps, 65536) }
            TypeLists.VAR = varps
            TypeLists.VARPBIT = loader
            for (id in listOf(0, 1, 1023, 1024, 1031, 1036, 2048, 65537)) {
                for (value in listOf(-1, 0, 1, 7, 255, Int.MIN_VALUE, Int.MAX_VALUE)) {
                    val domain = PlayerVarDomain()
                    val definition = loader.list(id)
                    domain.activeVars[definition.configIndex] = 0x12345678
                    domain.pendingVars[definition.configIndex] = -727
                    val before = domain.getVarBitValue(id)
                    Utils.lastSystemTime = 1L shl 60
                    Utils.timeOffset = 100 - (1L shl 60)
                    domain.setVarBitValue(id, value)
                    add(buildJsonObject {
                        put("id", id)
                        put("value", value)
                        put("varp", definition.configIndex)
                        put("before", before)
                        put("after", domain.getVarBitValue(id))
                        put("active", domain.getVarValue(definition.configIndex))
                        put("pending", domain.pendingVars[definition.configIndex])
                    })
                }
            }
        })
    }
    println(report)
}
