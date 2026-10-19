package com.rs.game.model.entity.npc

import com.google.gson.reflect.TypeToken
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.JsonFileManager
import org.darkan.core.Logger
import org.darkan.core.Utils
import org.darkan.core.gameval.Gameval
import org.darkan.core.gameval.GamevalCatalog
import world.gregs.voidps.type.Tile
import java.io.File

private val WANDER_RANGES_FILE = File("data/npcs/wander.json")

private var wanderRanges: Map<Int, Int> = emptyMap()

@ServerStartupEvent(ServerStartupEvent.Priority.FILE_IO)
fun loadNpcWanderRanges() {
    wanderRanges = parseWanderRanges(WANDER_RANGES_FILE.readText(), Gameval)
    Logger.info(NPC::class.java, "loadNpcWanderRanges", "Loaded ${wanderRanges.size} NPC wander ranges")
}

fun parseWanderRanges(json: String, catalog: GamevalCatalog): Map<Int, Int> {
    val entries: Map<String, Int> = JsonFileManager.getGSON().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type)
    return buildMap {
        for ((npc, range) in entries) {
            val id = npc.toIntOrNull() ?: catalog.id(Gameval.NPC, npc)
            if (id == null) Logger.warn(NPC::class.java, "parseWanderRanges", "no npc gameval named '$npc'")
            else put(id, range)
        }
    }
}

fun wanderRange(npcId: Int): Int? = wanderRanges[npcId]

fun wanderDestination(respawnTile: Tile, range: Int): Tile =
    respawnTile.transform(Utils.randomInclusive(-range, range), Utils.randomInclusive(-range, range), 0)
