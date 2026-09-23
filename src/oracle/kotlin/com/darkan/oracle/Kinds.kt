package com.darkan.oracle

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.ClientStartup
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.config.UnknownOpcodes
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.config.bas.BasTypeList
import com.jagex.game.runetek5.config.defaults.EntityDefaults
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType
import com.jagex.game.runetek5.config.defaults.InteractionDefaultsType
import com.jagex.game.runetek5.config.enumtype.EnumTypeList
import com.jagex.game.runetek5.config.flo.FloTypeList
import com.jagex.game.runetek5.config.flu.FluTypeList
import com.jagex.game.runetek5.config.identitykit.IdkTypeList
import com.jagex.game.runetek5.config.inputsubscriber.CutsceneDefaultsType
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.item.ItemTypeList
import com.jagex.game.runetek5.config.material.MaterialTypeList
import com.jagex.game.runetek5.config.npc.NpcTypeList
import com.jagex.game.runetek5.config.param.ParamTypeList
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script
import com.jagex.game.runetek5.config.sequence.SeqTypeList
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimTypeList
import com.jagex.game.runetek5.config.struct.StructTypeList
import com.jagex.game.runetek5.config.varp.VarpTypeList
import com.jagex.game.runetek5.config.varp.VarpbitTypeList
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.js5.Js5Compression
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.scene.environment.EnvironmentManager
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.tools.Language
import java.io.File
import java.util.TreeMap

class Sample(
    val suite: String,
    val kind: String,
    val index: Int,
    val group: Int,
    val file: Int?,
    val id: Int?,
    val xtea: IntArray?,
)

class Snapshot(val kind: String, val id: Int, val input: ByteArray, val file: Int?, val value: Map<String, Any>, val unknownOps: List<Int>)

class Context(val cache: DiskCache, private val namesDir: File, val xteaKeys: Map<Int, IntArray>) {
    private val providers = HashMap<Int, Js5ResourceProvider>()

    fun reflector(kind: String) = Reflector(NameTable.load(namesDir, kind))

    fun provider(index: Int): Js5ResourceProvider = providers.getOrPut(index) { cache.provider(index) }
}

fun interface Kind {
    fun dump(sample: Sample, ctx: Context): Snapshot
}

object Kinds {

    val all: Map<String, Kind> = linkedMapOf(
        "store" to Kind(::store),
        "container" to Kind(::container),
        "reftable" to Kind(::reftable),
        "xtea" to Kind(::xtea),
        "flu" to config(2, -1) { ctx, id -> FluTypeList(ctx.provider(2)).list(id) },
        "flo" to config(2, -1) { ctx, id -> FloTypeList(ctx.provider(2)).list(id) },
        "idk" to config(2, -1) { ctx, id -> IdkTypeList(ctx.provider(2), ctx.provider(7)).list(id) },
        "param" to config(2, -1) { ctx, id -> ParamTypeList(ctx.provider(2)).list(id) },
        "varp" to config(2, -1) { ctx, id -> VarpTypeList(ctx.provider(2)).list(id) },
        "struct" to config(2, -1) { ctx, id -> StructTypeList(ctx.provider(2)).getStruct(id) },
        "bas" to config(2, -1) { ctx, id -> BasTypeList(ctx.provider(2), EquipmentDefaultsType(ctx.provider(28))).list(id) },
        "loc" to config(16, 8) { ctx, id -> ObjectTypeList(Game.RUNESCAPE, Language.ENGLISH, ctx.provider(16), ctx.provider(7)).list(id) },
        "enum" to config(17, 8) { ctx, id -> EnumTypeList(ctx.provider(17)).list(id) },
        "npc" to config(18, 7) { ctx, id -> NpcTypeList(Game.RUNESCAPE, Language.ENGLISH, ctx.provider(18), ctx.provider(7)).list(id) },
        "obj" to Kind(::obj),
        "seq" to config(20, 7) { ctx, id -> SeqTypeList(ctx.provider(20), ctx.provider(0), ctx.provider(1)).list(id) },
        "spotanim" to config(21, 8) { ctx, id -> SpotAnimTypeList(ctx.provider(21), ctx.provider(7)).list(id) },
        "varbit" to config(22, 10) { ctx, id -> VarpbitTypeList(ctx.provider(22)).list(id) },
        "material" to Kind(::material),
        "defaults" to Kind(::defaults),
        "model" to groupRecord(7) { _, _, bytes -> Mesh(bytes) },
        "anim" to groupRecord(0) { ctx, group, _ ->
            SeqTypeList(ctx.provider(20), ctx.provider(0), ctx.provider(1)).listFrameset(group) ?: error("frameset $group did not decode")
        },
        "sprite" to Kind(::sprite),
        "font" to groupRecord(13) { _, _, bytes -> FontMetrics(bytes) },
        "texture" to Kind(::texture),
        "component" to Kind(::component),
        "cs2script" to groupRecord(12) { _, _, bytes -> CS2Script.decodeScript(bytes) },
    )

    val deferred = mapOf(
        "colortable" to "darkan-bot has no cache-backed colour table decoder; the HSL palette is computed, not decoded",
        "mapsquare" to "region decode runs only inside darkan-bot's scene builder, which needs a live toolkit and scene graph",
        "multiloc" to "multiloc resolution needs var state from a running client (H-12 world_state), not cache bytes alone",
        "worldmap" to "the world map decoder needs a loaded WorldMap area set and toolkit sprites",
    )

    private fun expect(sample: Sample, index: Int) =
        require(sample.index == index) { "${sample.kind} samples live in index $index, got ${sample.index}" }

    private fun fileOf(sample: Sample): Int = sample.file ?: error("${sample.kind} sample needs a file")

    private fun traced(block: () -> Any): Pair<Any, List<Int>> {
        val sink = ArrayList<Int>()
        UnknownOpcodes.sink = sink
        try {
            return block() to sink
        } finally {
            UnknownOpcodes.sink = null
        }
    }

    private fun fileBytes(ctx: Context, index: Int, group: Int, file: Int, xtea: IntArray? = null): ByteArray =
        ctx.provider(index).getFile(group, file, xtea) ?: error("no file $index/$group/$file")

    private fun config(index: Int, bits: Int, list: (Context, Int) -> Any) = Kind { sample, ctx ->
        expect(sample, index)
        val file = fileOf(sample)
        val id = if (bits < 0) file else (sample.group shl bits) or file
        val input = fileBytes(ctx, index, sample.group, file)
        val (record, ops) = traced { list(ctx, id) }
        Snapshot(sample.kind, id, input, file, ctx.reflector(sample.kind).record(record), ops)
    }

    private fun groupRecord(index: Int, decode: (Context, Int, ByteArray) -> Any) = Kind { sample, ctx ->
        expect(sample, index)
        val file = sample.file ?: 0
        val input = fileBytes(ctx, index, sample.group, file)
        val (record, ops) = traced { decode(ctx, sample.group, input) }
        Snapshot(sample.kind, sample.group, input, sample.file, ctx.reflector(sample.kind).record(record), ops)
    }

    private fun raw(ctx: Context, sample: Sample): ByteArray =
        ctx.cache.group(sample.index, sample.group) ?: error("no group ${sample.index}/${sample.group} in the store")

    private fun store(sample: Sample, ctx: Context): Snapshot {
        val bytes = ctx.cache.group(sample.index, sample.group)
        val header = if (sample.group > 65535) 10 else 8
        val v = if (bytes == null) {
            Canonical.obj("present" to false)
        } else {
            val chunk = 520 - header
            Canonical.obj(
                "present" to true,
                "len" to bytes.size.toLong(),
                "header" to header.toLong(),
                "sectors" to ((bytes.size + chunk - 1) / chunk).toLong(),
                "data" to Canonical.bytes(bytes),
            )
        }
        return Snapshot(sample.kind, sample.group, bytes ?: ByteArray(0), null, v, emptyList())
    }

    private fun containerFields(payload: ByteArray): TreeMap<String, Any> {
        val header = Packet(payload)
        val compression = header.readUByte()
        val compressedLength = header.readInt()
        val headerLength = if (compression == 0) 5 else 9
        val trailing = payload.size - headerLength - compressedLength
        val version = if (trailing == 2) {
            ((payload[payload.size - 2].toInt() and 0xff) shl 8) or (payload[payload.size - 1].toInt() and 0xff)
        } else {
            null
        }
        return Canonical.obj(
            "compression" to compression.toLong(),
            "compressed_len" to compressedLength.toLong(),
            "len" to payload.size.toLong(),
            "version" to version?.toLong(),
            "data" to Canonical.bytes(Js5Compression.decompress(payload)),
        )
    }

    private fun container(sample: Sample, ctx: Context): Snapshot {
        val payload = raw(ctx, sample)
        return Snapshot(sample.kind, sample.group, payload, null, containerFields(payload), emptyList())
    }

    private fun reftable(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 255)
        val payload = raw(ctx, sample)
        val table = ctx.cache.masterIndex(sample.group)
        val body = Js5Compression.decompress(payload)
        val format = body[0].toInt() and 0xff
        val v = ctx.reflector(sample.kind).record(table)
        v["format"] = format.toLong()
        v["flags"] = (body[if (format >= 6) 5 else 1].toInt() and 0xff).toLong()
        return Snapshot(sample.kind, sample.group, payload, null, v, emptyList())
    }

    private fun xtea(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 5)
        val payload = raw(ctx, sample)
        val key = sample.xtea ?: regionKey(ctx, sample.group)
        val keyed = key != null && key.any { it != 0 }
        val end = 5 + 8 * ((payload.size - 5) / 8)
        val decrypted = payload.copyOf()
        if (keyed) {
            Packet(decrypted).decryptXtea(key!!, decrypted.size)
        }
        val files = ctx.provider(5).getValidFileIds(sample.group)?.toList() ?: (0 until ctx.provider(5).getArchiveFileCount(sample.group)).toList()
        val data = ctx.provider(5).getFile(sample.group, files.first(), key)
        val v = Canonical.obj(
            "key" to key?.let { Canonical.primitiveArray(it) },
            "keyed" to keyed,
            "len" to payload.size.toLong(),
            "range_start" to 5L,
            "range_end" to (if (keyed) end else 5).toLong(),
            "trailer_in_range" to (keyed && end > payload.size - 2),
            "decrypted" to Canonical.bytes(decrypted),
            "data" to data?.let { Canonical.bytes(it) },
        )
        return Snapshot(sample.kind, sample.group, payload, null, v, emptyList())
    }

    private fun regionKey(ctx: Context, group: Int): IntArray? {
        val hash = ctx.cache.masterIndex(5).nameHashes?.getOrNull(group) ?: return null
        for ((region, key) in ctx.xteaKeys) {
            val x = region ushr 8
            val y = region and 0xff
            if (Js5ResourceProvider.jaghash("l${x}_$y") == hash || Js5ResourceProvider.jaghash("n${x}_$y") == hash) {
                return key
            }
        }
        return null
    }

    private fun obj(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 19)
        val file = fileOf(sample)
        val id = (sample.group shl 8) or file
        val input = fileBytes(ctx, 19, sample.group, file)
        val list = { ItemTypeList(Game.RUNESCAPE, Language.ENGLISH, ParamTypeList(ctx.provider(2)), ctx.provider(19), ctx.provider(7)) }
        val probe = list().list(id)
        val fresh = list()
        for (template in templates(probe)) {
            fresh.list(template)
        }
        val (record, ops) = traced { fresh.list(id) }
        return Snapshot(sample.kind, id, input, file, ctx.reflector(sample.kind).record(record), ops)
    }

    private fun templates(item: ItemType): List<Int> =
        listOf(item.certTemplateId, item.certId, item.lendTemplateId, item.lendId, item.bindTemplateId, item.bindId).filter { it != -1 }

    private fun material(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 26)
        val id = sample.id ?: error("material samples need an id")
        val input = fileBytes(ctx, 26, sample.group, fileOf(sample))
        val list = MaterialTypeList(ctx.provider(26), ctx.provider(9), ctx.provider(8))
        val record = list.materials.getOrNull(id) ?: error("material $id is not defined")
        return Snapshot(sample.kind, id, input, sample.file, ctx.reflector(sample.kind).record(record), emptyList())
    }

    private fun defaults(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 28)
        val file = fileOf(sample)
        val input = fileBytes(ctx, 28, sample.group, file)
        val (v, ops) = when (file) {
            1 -> {
                EnvironmentManager.cubeTextureIds = null
                val (_, ops) = traced { EnvironmentManager.decodeCubeTextures(input) }
                Canonical.obj("cube_texture_ids" to EnvironmentManager.cubeTextureIds?.let { Canonical.primitiveArray(it) }) to ops
            }
            4 -> {
                AreaSoundPlayer.titlescreenMusicArchive = -1
                val (_, ops) = traced { ClientStartup.decodeTitlescreenMusic(input) }
                Canonical.obj("titlescreen_music_archive" to AreaSoundPlayer.titlescreenMusicArchive.toLong()) to ops
            }
            else -> {
                val kind = when (file) {
                    3 -> "defaults-entity"
                    6 -> "defaults-equipment"
                    7 -> "defaults-interaction"
                    8 -> "defaults-cutscene"
                    else -> error("defaults file $file has no decoder")
                }
                val (record, ops) = traced {
                    when (file) {
                        3 -> EntityDefaults(ctx.provider(28))
                        6 -> EquipmentDefaultsType(ctx.provider(28))
                        7 -> InteractionDefaultsType(ctx.provider(28))
                        else -> CutsceneDefaultsType(ctx.provider(28))
                    }
                }
                ctx.reflector(kind).record(record) to ops
            }
        }
        return Snapshot(sample.kind, file, input, file, v, ops)
    }

    private fun sprite(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 8)
        val input = fileBytes(ctx, 8, sample.group, sample.file ?: 0)
        val reflector = ctx.reflector(sample.kind)
        val frames = SpriteType.decode(input).map { reflector.record(it) }
        return Snapshot(sample.kind, sample.group, input, sample.file, Canonical.obj("frames" to Canonical.array(frames)), emptyList())
    }

    private fun texture(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 9)
        val input = fileBytes(ctx, 9, sample.group, sample.file ?: 0)
        val list = MaterialTypeList(ctx.provider(26), ctx.provider(9), ctx.provider(8))
        val type = list.getTextureType(sample.group) ?: error("texture ${sample.group} did not decode")
        val v = ctx.reflector(sample.kind).record(type)
        for (size in intArrayOf(128, 64)) {
            v["argb_$size"] = Canonical.primitiveArray(list.getPixelsArgb(sample.group, size, size))!!
        }
        return Snapshot(sample.kind, sample.group, input, sample.file, v, emptyList())
    }

    private fun component(sample: Sample, ctx: Context): Snapshot {
        expect(sample, 3)
        val file = fileOf(sample)
        val input = fileBytes(ctx, 3, sample.group, file)
        val component = Component()
        component.idHash = file + (sample.group shl 16)
        val (record, ops) = traced { component.also { it.decode(Packet(input)) } }
        return Snapshot(sample.kind, component.idHash, input, file, ctx.reflector(sample.kind).record(record), ops)
    }
}
