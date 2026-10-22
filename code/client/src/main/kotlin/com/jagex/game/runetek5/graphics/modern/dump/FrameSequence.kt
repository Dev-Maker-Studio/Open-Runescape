package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FrameSequence(val dir: Path, val count: Int, val every: Int) {
    class Entry(val index: Int, val frame: Int, val tick: Int, val nanos: Long, val state: String)

    val entries = ArrayList<Entry>()
    private var seen = 0

    init {
        require(count in 1..MAX_COUNT) { "count must be 1..$MAX_COUNT" }
        require(every >= 1) { "every must be at least 1" }
    }

    val done get() = entries.size >= count

    fun due() = !done && seen++ % every == 0

    fun orf(index: Int = entries.size): Path = dir.resolve("frame-%04d.orf".format(index))

    fun png(index: Int = entries.size): Path = dir.resolve("frame-%04d.png".format(index))

    fun record(frame: Int, tick: Int, nanos: Long, state: String = "") {
        entries += Entry(entries.size, frame, tick, nanos, state)
    }

    fun json(error: String? = null): String {
        val frames = entries.map {
            JsonObject(
                mapOf(
                    "index" to JsonPrimitive(it.index),
                    "frame" to JsonPrimitive(it.frame),
                    "tick" to JsonPrimitive(it.tick),
                    "nanos" to JsonPrimitive(it.nanos),
                    "gameState" to JsonPrimitive(it.state),
                    "orf" to JsonPrimitive(orf(it.index).fileName.toString()),
                    "png" to JsonPrimitive(png(it.index).fileName.toString()),
                ),
            )
        }
        val head = mapOf("count" to JsonPrimitive(frames.size), "requested" to JsonPrimitive(count), "every" to JsonPrimitive(every), "frames" to JsonArray(frames))
        return JsonObject(error?.let { head + ("error" to JsonPrimitive(it)) } ?: head).toString()
    }

    fun discardUnrecorded() {
        Files.deleteIfExists(orf())
        Files.deleteIfExists(png())
    }

    fun save(error: String? = null) {
        val path = dir.resolve("frames.json")
        val temp = dir.resolve("frames.json.part")
        try {
            Files.createDirectories(dir)
            Files.writeString(temp, json(error))
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    companion object {
        const val MAX_COUNT = 600
    }
}
