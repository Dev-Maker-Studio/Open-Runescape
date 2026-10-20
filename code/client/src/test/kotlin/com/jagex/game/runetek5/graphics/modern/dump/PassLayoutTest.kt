package com.jagex.game.runetek5.graphics.modern.dump

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class PassLayoutTest {
    @Test
    fun passesMatchRendererFixture() {
        val text = javaClass.getResourceAsStream("/pass-layouts.json")!!.readBytes().decodeToString()
        val fixture = Json.parseToJsonElement(text).jsonArray.associate { it.jsonObject["pass"]!!.jsonPrimitive.content to it.jsonObject }
        assertEquals(fixture.keys, Passes.byName.keys)
        for ((name, pass) in Passes.byName) {
            val f = fixture[name]!!
            assertEquals(name, f["id"]!!.jsonPrimitive.int, pass.id)
            assertEquals(name, f["stride"]!!.jsonPrimitive.int, pass.stride)
            assertEquals(name, f["size"]!!.jsonPrimitive.int, pass.block)
            assertEquals(name, f["attrs"]!!.jsonArray.map { a -> a.jsonArray.map { it.jsonPrimitive.int } }, pass.attrs.map { listOf(it.location, it.offset, it.bytes) })
            val members = f["members"]!!.jsonArray.associate { it.jsonArray[0].jsonPrimitive.content.removeSuffix("_u") to it.jsonArray[1].jsonPrimitive.int }
            assertEquals(name, members.keys, pass.slots.keys)
            for ((slot, s) in pass.slots) assertEquals("$name.$slot", members[slot], s.offset)
        }
    }
}
