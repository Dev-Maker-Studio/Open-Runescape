package com.darkan.oracle

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.tomlj.Toml
import org.tomlj.TomlTable
import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """usage: DecoderDump --cache <dir> --samples <samples.toml> --names <dir> --out <dir> [--xtea <xteaKeys.json>] [--schema <snapshot.schema.json>]
       DecoderDump --skeleton <kind> <class>
       DecoderDump --kinds"""

fun main(args: Array<String>) {
    val options = parse(args)
    when {
        options.containsKey("kinds") -> {
            Kinds.all.keys.forEach { println(it) }
            Kinds.deferred.forEach { (kind, why) -> println("$kind deferred: $why") }
        }
        options.containsKey("skeleton") -> {
            val (kind, className) = options.getValue("skeleton").split(' ', limit = 2)
            print(Reflector.skeleton(kind, Class.forName(className)))
        }
        else -> exitProcess(dump(options))
    }
}

private fun parse(args: Array<String>): Map<String, String> {
    val out = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--kinds" -> out["kinds"] = ""
            "--skeleton" -> {
                require(i + 2 < args.size) { USAGE }
                out["skeleton"] = "${args[i + 1]} ${args[i + 2]}"
                i += 2
            }
            else -> {
                require(arg.startsWith("--") && i + 1 < args.size) { USAGE }
                out[arg.removePrefix("--")] = args[++i]
            }
        }
        i++
    }
    return out
}

private fun dump(options: Map<String, String>): Int {
    fun path(name: String) = File(options[name] ?: throw IllegalArgumentException("missing --$name\n$USAGE"))
    val cache = DiskCache(path("cache"))
    val names = path("names")
    val out = path("out")
    val xtea = options["xtea"]?.let { loadXtea(File(it)) } ?: emptyMap()
    val schema = options["schema"]?.let { loadSchema(File(it)) }
    val samples = loadSamples(path("samples"))
    val mapper = ObjectMapper()
    val written = LinkedHashSet<File>()
    var failures = 0
    for (sample in samples) {
        val target = File(File(out, sample.suite), fileName(sample))
        try {
            require(written.add(target)) { "two samples write $target" }
            Kinds.deferred[sample.kind]?.let { error("kind ${sample.kind} is deferred: $it") }
            val kind = Kinds.all[sample.kind] ?: error("unknown kind ${sample.kind}")
            val text = Canonical.write(envelope(sample, kind.dump(sample, Context(cache, names, xtea)))) + "\n"
            if (schema != null) {
                val errors = schema.validate(mapper.readTree(text))
                require(errors.isEmpty()) { "schema: ${errors.joinToString { it.message }}" }
            }
            target.parentFile.mkdirs()
            target.writeText(text, Charsets.UTF_8)
        } catch (failure: Exception) {
            failures++
            System.err.println("${sample.suite} ${sample.kind} ${sample.index}/${sample.group}/${sample.file}: ${failure.message ?: failure.javaClass.name}")
        }
    }
    out.walkTopDown().filter { it.isFile && it.extension == "json" && it !in written }.forEach { it.delete() }
    System.err.println("${samples.size - failures}/${samples.size} samples dumped to $out")
    return if (failures == 0) 0 else 1
}

private fun fileName(sample: Sample): String {
    val parts = mutableListOf(sample.index, sample.group)
    sample.file?.let { parts.add(it) }
    sample.id?.let { parts.add(it) }
    return parts.joinToString("-") + ".json"
}

private fun envelope(sample: Sample, snapshot: Snapshot): Map<String, Any> = Canonical.obj(
    "t" to snapshot.kind,
    "id" to snapshot.id.toLong(),
    "src" to Canonical.obj(
        "idx" to sample.index.toLong(),
        "group" to sample.group.toLong(),
        "file" to snapshot.file?.toLong(),
        "sha256" to Canonical.sha256(snapshot.input),
    ),
    "v" to snapshot.value,
    "unknown_ops" to snapshot.unknownOps.map { it.toLong() },
)

private fun loadSamples(file: File): List<Sample> {
    val toml = Toml.parse(file.toPath())
    require(!toml.hasErrors()) { "$file: ${toml.errors().joinToString()}" }
    val entries = toml.getArray("sample") ?: error("$file: no [[sample]] entries")
    return (0 until entries.size()).map { i ->
        val entry: TomlTable = entries.getTable(i)
        fun int(key: String): Int? = entry.getLong(key)?.toInt()
        Sample(
            suite = entry.getString("suite") ?: error("sample $i: missing suite"),
            kind = entry.getString("kind") ?: error("sample $i: missing kind"),
            index = int("index") ?: error("sample $i: missing index"),
            group = int("group") ?: error("sample $i: missing group"),
            file = int("file"),
            id = int("id"),
            xtea = entry.getArray("xtea")?.let { a -> IntArray(a.size()) { a.getLong(it).toInt() } },
        )
    }
}

private fun loadXtea(file: File): Map<Int, IntArray> =
    Json.parseToJsonElement(file.readText()).jsonObject.entries.associate { (region, key) ->
        region.toInt() to key.jsonArray.map { it.jsonPrimitive.int }.toIntArray()
    }.toSortedMap()

private fun loadSchema(file: File): JsonSchema =
    file.inputStream().use { JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(it) }
