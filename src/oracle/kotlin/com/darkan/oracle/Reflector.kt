package com.darkan.oracle

import com.jagex.game.runetek5.cache.node.HashTable
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.LongNode
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import org.tomlj.Toml
import org.tomlj.TomlTable
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.TreeMap

class ClassNames(val skip: Set<String>, val fields: Map<String, String>)

class NameTable(val kind: String, val root: String, val classes: Map<String, ClassNames>) {

    companion object {
        fun load(dir: File, kind: String): NameTable {
            val file = File(dir, "$kind.toml")
            require(file.isFile) { "no name table $file" }
            val toml = Toml.parse(file.toPath())
            require(!toml.hasErrors()) { "$file: ${toml.errors().joinToString()}" }
            val root = toml.getString("class") ?: error("$file: missing class")
            val classes = HashMap<String, ClassNames>()
            classes[root] = classNames(toml)
            val nested = toml.getTable("nested")
            if (nested != null) {
                for (name in nested.keySet()) {
                    classes[name] = classNames(nested.getTable(listOf(name))!!)
                }
            }
            return NameTable(kind, root, classes)
        }

        private fun classNames(table: TomlTable): ClassNames {
            val skip = table.getArray("skip")?.toList()?.map { it as String }?.toSet() ?: emptySet()
            val fields = table.getTable("fields")?.let { f -> f.keySet().associateWith { f.getString(listOf(it))!! } } ?: emptyMap()
            return ClassNames(skip, fields)
        }
    }
}

class Reflector(private val names: NameTable) {

    fun record(value: Any): TreeMap<String, Any> {
        require(value.javaClass.name == names.root) { "${names.kind}: expected ${names.root}, got ${value.javaClass.name}" }
        return fields(value)
    }

    private fun fields(value: Any): TreeMap<String, Any> {
        val className = value.javaClass.name
        val table = names.classes[className] ?: error("${names.kind}: no names for class $className")
        val out = TreeMap<String, Any>()
        for (field in dataFields(value.javaClass)) {
            if (field.name in table.skip) {
                continue
            }
            val canonical = table.fields[field.name] ?: error("${names.kind}: unmapped field $className.${field.name}")
            field.isAccessible = true
            val encoded = encode(field.get(value)) ?: continue
            require(out.put(canonical, encoded) == null) { "${names.kind}: duplicate canonical name $canonical" }
        }
        val present = dataFields(value.javaClass).map { it.name }.toSet()
        val stale = (table.fields.keys + table.skip) - present
        require(stale.isEmpty()) { "${names.kind}: $className has no fields $stale" }
        return out
    }

    fun encode(value: Any?): Any? = when (value) {
        null -> null
        is Boolean -> value
        is Byte -> value.toLong()
        is Short -> value.toLong()
        is Int -> value.toLong()
        is Char -> value.code.toLong()
        is Long -> value.toString()
        is Float -> Canonical.f32(value)
        is Double -> Canonical.f64(value)
        is String -> value
        is Enum<*> -> value.name
        is Array<*> -> Canonical.array(value.map { encode(it) ?: JsonNull })
        is List<*> -> Canonical.array(value.map { encode(it) ?: JsonNull })
        is Map<*, *> -> keyed(value.entries.map { it.key to it.value })
        is IterableNodeMap<*> -> keyed(value.nodes.flatMap { chain(it) })
        is HashTable -> keyed(value.buckets.flatMap { chain(it) })
        is IntNode -> value.value.toLong()
        is LongNode -> value.bitpacked.toString()
        is ObjectNode -> encode(value.value)
        else -> Canonical.primitiveArray(value) ?: fields(value)
    }

    private fun chain(head: Node<*>): List<Pair<Any?, Any?>> {
        val out = ArrayList<Pair<Any?, Any?>>()
        var node = head.next
        while (node != null && node !== head) {
            out.add(node.pointer to node)
            node = node.next
        }
        return out
    }

    private fun keyed(entries: List<Pair<Any?, Any?>>): TreeMap<String, Any> {
        val out = TreeMap<String, Any>()
        for ((key, entry) in entries) {
            val encoded = encode(entry) ?: JsonNull
            require(out.put(key.toString(), encoded) == null) { "${names.kind}: duplicate map key $key" }
        }
        return out
    }

    companion object {
        fun dataFields(type: Class<*>): List<Field> {
            val out = ArrayList<Field>()
            var current: Class<*>? = type
            while (current != null && !isBase(current)) {
                for (field in current.declaredFields) {
                    if (!Modifier.isStatic(field.modifiers) && !field.isSynthetic) {
                        out.add(field)
                    }
                }
                current = current.superclass
            }
            return out
        }

        private fun isBase(type: Class<*>): Boolean =
            type.name.startsWith("java.") || type.name.startsWith("kotlin.") ||
                type.name.startsWith("com.jagex.game.runetek5.cache.node.")

        fun skeleton(kind: String, root: Class<*>): String {
            val out = StringBuilder()
            out.append("class = \"${root.name}\"\nskip = []\n\n[fields]\n")
            for (field in dataFields(root).sortedBy { it.name }) {
                out.append("${field.name} = \"${snake(field.name)}\"  # ${field.type.simpleName}\n")
            }
            return out.toString()
        }

        fun snake(name: String): String =
            name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2").lowercase()
    }
}
