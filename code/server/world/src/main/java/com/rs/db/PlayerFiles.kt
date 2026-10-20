package com.rs.db

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rs.game.model.entity.player.Player
import org.darkan.core.EnvVars
import org.darkan.core.JsonFileManager
import org.darkan.core.model.AccountView
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object PlayerFiles {
    private val dir: Path? = EnvVars.playerFileDir?.let { Path.of(it, "players").also(Files::createDirectories) }

    @JvmStatic
    val enabled get() = dir != null

    private fun file(username: String) = dir!!.resolve(username.replace('/', '_') + ".json")

    @JvmStatic
    fun read(username: String): String? = file(username).takeIf(Files::exists)?.let(Files::readString)

    @JvmStatic
    @Synchronized
    fun write(username: String, json: String) {
        val target = file(username)
        val tmp = target.resolveSibling(target.fileName.toString() + ".tmp")
        Files.writeString(tmp, json)
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    @JvmStatic
    fun stamp(username: String) = runCatching { Files.getLastModifiedTime(file(username)).toMillis() }.getOrDefault(0L)

    @JvmStatic
    fun complete(json: String) = JsonParser.parseString(json).asJsonObject.has("appearence")

    @JvmStatic
    fun fresh(account: AccountView): Player {
        val player = Player(account)
        val overlay = if (enabled) read(account.username) else null
        return if (overlay == null) player else overlay(player, overlay)
    }

    @JvmStatic
    fun overlay(player: Player, overlay: String): Player {
        val gson = JsonFileManager.getGSON()
        return gson.fromJson(merge(gson.toJsonTree(player), JsonParser.parseString(overlay)), Player::class.java)
    }

    private fun merge(base: JsonElement, over: JsonElement): JsonElement = when {
        over.isJsonNull -> base
        base is JsonObject && over is JsonObject -> base.also { over.entrySet().forEach { (k, v) -> it.add(k, base.get(k)?.let { b -> merge(b, v) } ?: v) } }
        base is JsonArray && over is JsonArray -> JsonArray().also { out ->
            for (i in 0 until maxOf(base.size(), over.size())) out.add(if (i < over.size()) base.getOrNull(i)?.let { merge(it, over[i]) } ?: over[i] else base[i])
        }
        else -> over
    }

    private fun JsonArray.getOrNull(i: Int) = if (i < size()) get(i) else null
}
