package com.darkan.bot

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.config.enumtype.EnumType
import com.jagex.game.runetek5.config.struct.StructType
import com.jagex.game.runetek5.js5.Js5MasterIndex
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.js5.request.FileWorker
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32

class Cs2DefinitionDiagnosticsTest {
    private val emptySource = JsonObject(emptyMap())

    @Test
    fun commandRequiresAnExplicitDiagnosticSwitch() {
        val key = "darkan.cs2.definitionDiagnostics"
        val previous = System.getProperty(key)
        try {
            System.clearProperty(key)
            assertThrows(IllegalStateException::class.java) {
                Cs2DefinitionDiagnostics.read(listOf("struct", "508", "61"))
            }
        } finally {
            if (previous == null) System.clearProperty(key) else System.setProperty(key, previous)
        }
    }

    @Test
    fun requestGrammarAllowsOnlyFixedNumericDefinitionReads() {
        assertEquals(Cs2DefinitionDiagnostics.Request("struct", 508, 61), Cs2DefinitionDiagnostics.parse(listOf("struct", "508", "61")))
        assertEquals(Cs2DefinitionDiagnostics.Request("enum", 2252), Cs2DefinitionDiagnostics.parse(listOf("enum", "2252")))
        for (args in listOf(emptyList(), listOf("script", "2169"), listOf("enum", "2252", "extra"),
            listOf("enum", "-1"), listOf("enum", "2147483648"), listOf("enum", "player text"),
            listOf("struct", "508"), listOf("struct", "508", "-1"), listOf("struct", "508", "16777216"))) {
            assertThrows(IllegalArgumentException::class.java) { Cs2DefinitionDiagnostics.parse(args) }
        }
    }

    @Test
    fun structReadDistinguishesAnIntegerFromMissingOrStringParameters() {
        val definition = StructType().apply {
            nodeMap = IterableNodeMap(4)
            nodeMap!!.put(IntNode(2252), 61)
            nodeMap!!.put(ObjectNode("private player text"), 62)
        }
        val found = Cs2DefinitionDiagnostics.structSummary(508, 61, definition, emptySource)
        assertEquals(2252, found["value"]!!.jsonPrimitive.int)
        assertTrue(found["integerParameter"]!!.jsonPrimitive.boolean)
        val text = Cs2DefinitionDiagnostics.structSummary(508, 62, definition, emptySource)
        assertEquals(JsonNull, text["value"])
        assertTrue(text["parameterPresent"]!!.jsonPrimitive.boolean)
        assertFalse(text.toString().contains("private"))
        val missing = Cs2DefinitionDiagnostics.structSummary(508, 63, definition, emptySource)
        assertFalse(missing["parameterPresent"]!!.jsonPrimitive.boolean)
        assertEquals(JsonNull, missing["value"])
        val unloaded = Cs2DefinitionDiagnostics.structSummary(508, 61, null, emptySource)
        assertFalse(unloaded["definitionReady"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun emptyEnumsAreNotConfusedWithUncachedDefinitionsAndNeverExposeText() {
        val definition = EnumType().apply {
            keyType = 'i'
            valueType = 'J'
            values = arrayOfNulls(0)
            defaultStringValue = "private default"
        }
        val empty = Cs2DefinitionDiagnostics.enumSummary(2252, definition, emptySource)
        assertTrue(empty["definitionReady"]!!.jsonPrimitive.boolean)
        assertEquals(0, empty["outputCount"]!!.jsonPrimitive.int)
        assertEquals(0, empty["arrayCapacity"]!!.jsonPrimitive.int)
        assertEquals(105, empty["keyType"]!!.jsonPrimitive.int)
        assertEquals(74, empty["valueType"]!!.jsonPrimitive.int)
        definition.values = arrayOf("private enum entry")
        definition.parameters = mutableMapOf(1 to "private enum parameter")
        definition.size = 1
        assertFalse(Cs2DefinitionDiagnostics.enumSummary(2252, definition, emptySource).toString().contains("private"))
        val absent = Cs2DefinitionDiagnostics.enumSummary(2252, null, emptySource)
        assertFalse(absent["cached"]!!.jsonPrimitive.boolean)
        assertEquals(JsonNull, absent["outputCount"])
    }

    private fun provider(): Js5ResourceProvider = Js5ResourceProvider(object : FileWorker() {
        override fun getMasterIndex(): Js5MasterIndex? = error("diagnostics must not request a table")
        override fun requestArchive(archiveId: Int): ByteArray? = error("diagnostics must not request an archive")
        override fun prefetch(fileId: Int) = error("diagnostics must not prefetch")
        override fun getProgress(fileId: Int): Int = error("diagnostics must not poll requests")
    }, false, 1)

    private fun sparseTable(): Js5MasterIndex {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use {
            it.writeByte(6)
            it.writeInt(1)
            it.writeByte(0)
            it.writeShort(1)
            it.writeShort(26)
            it.writeInt(0)
            it.writeInt(1)
            it.writeShort(1)
            it.writeShort(508)
        }
        val table = bytes.toByteArray()
        val container = ByteArrayOutputStream()
        DataOutputStream(container).use {
            it.writeByte(0)
            it.writeInt(table.size)
            it.write(table)
        }
        val payload = container.toByteArray()
        return Js5MasterIndex(payload, CRC32().apply { update(payload) }.value.toInt(), null)
    }

    @Test
    fun sourceReadinessUsesExistingNativeMetadataWithoutRequestingAssets() {
        val provider = provider()
        val missing = Cs2DefinitionDiagnostics.source(provider, 2, 26, 508)
        assertFalse(missing["tableReady"]!!.jsonPrimitive.boolean)
        assertFalse(missing["filePresent"]!!.jsonPrimitive.boolean)
        provider.masterIndex = sparseTable()
        provider.archives = arrayOfNulls(27)
        provider.archiveFiles = arrayOfNulls(27)
        val defined = Cs2DefinitionDiagnostics.source(provider, 2, 26, 508)
        assertTrue(defined["filePresent"]!!.jsonPrimitive.boolean)
        assertFalse(defined["archiveReady"]!!.jsonPrimitive.boolean)
        assertFalse(Cs2DefinitionDiagnostics.source(provider, 2, 26, 507)["filePresent"]!!.jsonPrimitive.boolean)
        provider.archives!![26] = byteArrayOf(0)
        provider.archiveFiles!![26] = arrayOfNulls(509)
        provider.archiveFiles!![26]!![508] = byteArrayOf(0)
        val ready = Cs2DefinitionDiagnostics.source(provider, 2, 26, 508)
        assertTrue(ready["archiveReady"]!!.jsonPrimitive.boolean)
        assertTrue(ready["fileReady"]!!.jsonPrimitive.boolean)
        assertEquals(2, ready["sourceIndex"]!!.jsonPrimitive.int)
        assertEquals(26, ready["sourceArchive"]!!.jsonPrimitive.int)
        assertEquals(508, ready["sourceFile"]!!.jsonPrimitive.int)
        assertEquals(0, provider.archiveFiles!![26]!![508]!!.let { (it as ByteArray).single().toInt() })
    }
}
