package com.darkan.bot

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.ChatLine
import com.jagex.game.runetek5.cache.node.HashTableIterator
import kotlinx.serialization.json.*
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object Footprints {
    private val dir: Path? = System.getProperty("bot.footprints")?.let { Path.of(it).also(Files::createDirectories) }
    private val writer: BufferedWriter? = dir?.let { Files.newBufferedWriter(it.resolve("trail.jsonl"), StandardOpenOption.CREATE, StandardOpenOption.APPEND) }
    val shots: Path = (dir ?: Path.of(System.getenv("XDG_RUNTIME_DIR") ?: "/tmp", "openrs")).resolve("shots")

    @JvmField
    @Volatile
    var gameTick = 0

    private var tick = Int.MIN_VALUE
    private var gameState: String? = null
    private var position: Triple<Int, Int, Int>? = null
    private var xp: IntArray? = null
    private var interfaces: Set<Int>? = null
    private var inventory: Map<Int, Pair<Int, Int>>? = null
    private var lastChat: ChatLine? = null
    private var chatSeen = false

    @Synchronized
    fun log(kind: String, vararg fields: Pair<String, JsonElement>) {
        val out = writer ?: return
        out.write(JsonObject(mapOf("t" to JsonPrimitive(System.currentTimeMillis()), "tick" to number(Footprints.gameTick), "kind" to text(kind)) + fields).toString())
        out.newLine()
        out.flush()
    }

    fun openInterfaces(): Set<Int> {
        val ids = HashSet<Int>()
        for (open in HashTableIterator<SubInterface>(Client.OPEN_INTERFACES!!)) open?.let { ids.add(it.subInterfaceId) }
        return ids
    }

    fun freshChat(since: ChatLine?): List<ChatLine> = ChatBox.CHAT_LINES.takeWhile { it != null && it !== since }.take(10).filterNotNull().reversed()

    fun observe() {
        if (writer == null || Footprints.gameTick == tick) return
        tick = Footprints.gameTick
        runCatching(::diff)
    }

    private fun diff() {
        val state = Client.gameState.toString()
        if (state != gameState) {
            log("state", "to" to text(state), "from" to text(gameState))
            gameState = state
        }
        if (Bot.isLoggedOut()) return
        val here = Bot.getMyPlayerPosition().let { Triple(it.x, it.y, it.z) }
        if (here != position) {
            log("pos", "x" to number(here.first), "y" to number(here.second), "z" to number(here.third))
            position = here
        }
        val levels = Client.SKILL_XP
        xp?.let { old ->
            for (i in 0 until 25) if (levels[i] != old[i]) log("xp", "skill" to text(com.darkan.bot.utils.Skill.entries[i].name), "delta" to number(levels[i] - old[i]), "xp" to number(levels[i]), "level" to number(Bot.getLevel(i)))
        }
        xp = levels.copyOf(25)
        val open = openInterfaces()
        interfaces?.let { old ->
            if (open != old) log("interfaces", "opened" to list((open - old).map(::number)), "closed" to list((old - open).map(::number)))
        }
        interfaces = open
        val container = Bot.inventory.container
        val items = container.itemIds.indices.associateWith { container.itemIds[it] to container.amounts[it] }
        inventory?.let { old ->
            val changes = items.filter { (old[it.key] ?: (-1 to 0)) != it.value }
            if (changes.isNotEmpty()) log("inv", "changes" to list(changes.map { (slot, now) ->
                entry("slot" to number(slot), "id" to number(now.first), "amount" to number(now.second), "was" to number(old[slot]?.first ?: -1), "wasAmount" to number(old[slot]?.second ?: 0))
            }))
        }
        inventory = items
        if (!chatSeen) {
            chatSeen = true
        } else {
            freshChat(lastChat).forEach { log("chat", "type" to text(it.type.toString()), "name" to text(it.simpleName), "message" to text(it.message)) }
        }
        lastChat = ChatBox.CHAT_LINES[0]
    }
}
