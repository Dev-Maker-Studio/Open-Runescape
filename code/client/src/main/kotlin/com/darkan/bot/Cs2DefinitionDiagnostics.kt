package com.darkan.bot

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.enumtype.EnumType
import com.jagex.game.runetek5.config.struct.StructType
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import kotlinx.serialization.json.*

internal object Cs2DefinitionDiagnostics {
    internal data class Request(val kind: String, val id: Int, val parameter: Int? = null)

    fun read(args: List<String>): JsonObject {
        check(java.lang.Boolean.getBoolean("darkan.cs2.definitionDiagnostics")) {
            "cs2-definition requires -Ddarkan.cs2.definitionDiagnostics=true"
        }
        val request = parse(args)
        return when (request.kind) {
            "struct" -> {
                val types = TypeLists.STRUCT
                structSummary(request.id, request.parameter!!, types?.cache?.getOrNull(request.id),
                    source(types?.structProvider, 2, SharedConfigsType.STRUCTS.id, request.id))
            }
            else -> {
                val types = TypeLists.ENUM
                val definition = types?.let { synchronized(it.cache) { it.cache.get(request.id.toLong()) as EnumType? } }
                enumSummary(request.id, definition,
                    source(types?.enumTypeResource, 17, SharedConfigsType.ENUMS.archiveId(request.id), SharedConfigsType.ENUMS.archiveFileId(request.id)))
            }
        }
    }

    internal fun parse(args: List<String>): Request {
        val usage = "usage: cs2-definition struct <id> <parameter> | cs2-definition enum <id>"
        require(args.size == 3 && args[0] == "struct" || args.size == 2 && args[0] == "enum") { usage }
        val id = args[1].toIntOrNull()
        require(id != null && id >= 0) { usage }
        val parameter = if (args[0] == "struct") args[2].toIntOrNull() else null
        require(args[0] != "struct" || parameter != null && parameter in 0..0xffffff) { usage }
        return Request(args[0], id, parameter)
    }

    internal fun source(provider: Js5ResourceProvider?, index: Int, archive: Int, file: Int): JsonObject = buildJsonObject {
        put("sourceIndex", index)
        put("sourceArchive", archive)
        put("sourceFile", file)
        if (provider == null) {
            put("tableReady", false)
            put("filePresent", false)
            put("archiveReady", false)
            put("fileReady", false)
        } else synchronized(provider) {
            val table = provider.masterIndex
            val validIds = table?.validFileIds?.getOrNull(archive)
            put("localSource", provider.fileWorker.isLocal)
            put("tableReady", table != null)
            put("filePresent", file >= 0 && file < (table?.fileCounts?.getOrNull(archive) ?: 0) && (validIds == null || file in validIds))
            put("archiveReady", provider.archives?.getOrNull(archive) != null)
            put("fileReady", provider.archiveFiles?.getOrNull(archive)?.getOrNull(file) != null)
        }
    }

    internal fun structSummary(id: Int, parameter: Int, definition: StructType?, source: JsonObject): JsonObject = buildJsonObject {
        for ((key, value) in source) put(key, value)
        val node = definition?.nodeMap?.get(parameter.toLong())
        put("kind", "struct")
        put("id", id)
        put("parameter", parameter)
        put("definitionReady", definition != null)
        put("parameterPresent", node != null)
        put("integerParameter", node is IntNode)
        put("value", (node as? IntNode)?.value?.let(::JsonPrimitive) ?: JsonNull)
    }

    internal fun enumSummary(id: Int, definition: EnumType?, source: JsonObject): JsonObject = buildJsonObject {
        for ((key, value) in source) put(key, value)
        put("kind", "enum")
        put("id", id)
        put("definitionReady", definition != null)
        put("cached", definition != null)
        put("keyType", definition?.keyType?.code?.let(::JsonPrimitive) ?: JsonNull)
        put("valueType", definition?.valueType?.code?.let(::JsonPrimitive) ?: JsonNull)
        put("outputCount", definition?.size?.let(::JsonPrimitive) ?: JsonNull)
        put("arrayCapacity", definition?.values?.size?.let(::JsonPrimitive) ?: JsonNull)
        put("entryMapSize", definition?.parameters?.size?.let(::JsonPrimitive) ?: JsonNull)
    }
}
