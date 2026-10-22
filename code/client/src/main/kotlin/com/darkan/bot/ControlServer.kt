package com.darkan.bot

import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.utils.Item
import com.darkan.bot.utils.Skill
import com.darkan.bot.utils.Tile
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.ChatLine
import com.jagex.game.runetek5.vardomain.VarProviders
import kotlinx.serialization.json.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object ControlServer {
    private val queue = ConcurrentLinkedQueue<FutureTask<*>>()

    private fun itemName(id: Int) = TypeLists.OBJ!!.list(id).name
    private fun tile(x: Int, y: Int) = entry("x" to number(x), "y" to number(y))

    private fun container(wrapper: com.darkan.bot.utils.ContainerWrapper) = list(
        wrapper.container.itemIds.indices.filter { wrapper.container.itemIds[it] != -1 }.map {
            entry("slot" to number(it), "id" to number(wrapper.container.itemIds[it]), "amount" to number(wrapper.container.amounts[it]), "name" to text(itemName(wrapper.container.itemIds[it])))
        }
    )

    private fun groundEntry(item: Item) = entry("id" to number(item.id), "amount" to number(item.amount), "name" to text(item.getName()), "x" to number(item.position.x), "y" to number(item.position.y))

    private fun say(message: String): JsonElement {
        if (!DisplayToggle.intercept(message)) Bot.chat(message, 0, 0)
        return text("ok")
    }

    private val reads = mapOf<String, (List<String>) -> JsonElement>(
        "tick" to { number(Footprints.gameTick) },
        "worlds" to {
            val worlds = com.jagex.game.runetek5.network.world.WorldMeta.WORLD_LIST_DESCRIPTORS
                ?: com.jagex.game.runetek5.network.WorldConnection.WORLD_DESCRIPTORS_BYID
            list(worlds.filterNotNull().sortedBy { it.worldNumber }.map {
                entry("id" to number(it.worldNumber), "host" to text(it.ipAddress), "port" to number(it.port), "flags" to number(it.flags))
            })
        },
        "describe" to { text(Describe.render()) },
        "state" to { entry("tick" to number(Footprints.gameTick), "gameState" to text(Client.gameState.toString()), "loggedOut" to JsonPrimitive(Bot.isLoggedOut()), "scripts" to list(ScriptExecutor.activeScripts.map { text(it.javaClass.simpleName) })) },
        "pos" to { Bot.getMyPlayerPosition().let { entry("x" to number(it.x), "y" to number(it.y), "z" to number(it.z)) } },
        "skills" to { list(Skill.entries.map { entry("skill" to text(it.name), "level" to number(Bot.getLevel(it.ordinal)), "xp" to number(Bot.getXp(it.ordinal))) }) },
        "inventory" to { container(Bot.inventory) },
        "bank" to { container(Bot.bank) },
        "varp" to { number(VarProviders.PLAYER_VAR_PROVIDER.getVarValue(it[0].toInt())) },
        "varbit" to { number(VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(it[0].toInt())) },
        "cs2-definition" to { Cs2DefinitionDiagnostics.read(it) },
        "interface" to { a -> a.firstOrNull()?.let { JsonPrimitive(Bot.interfaceOpen(it.toInt())) } ?: list(Footprints.openInterfaces().sorted().map(::number)) },
        "chat" to { a ->
            if (a.isNotEmpty() && a[0].toIntOrNull() == null) say(a.joinToString(" ")) else list(ChatBox.CHAT_LINES.take(a.firstOrNull()?.toInt() ?: 10).filterNotNull().map {
                entry("type" to text(it.type.toString()), "name" to text(it.simpleName), "message" to text(it.message))
            })
        },
        "npcs" to { list(Bot.getNPCs().map { entry("index" to number(it.playerIndex), "id" to number(it.definitions!!.type), "name" to text(it.definitions!!.name), "x" to number(it.movementBaseX[0] + Bot.getBaseX()), "y" to number(it.movementBaseY[0] + Bot.getBaseY())) }) },
        "objects" to { list(Bot.getNearbyObjects().map { entry("id" to number(it.id), "name" to text(it.getDefinitions().name), "x" to number(it.x), "y" to number(it.y)) }) },
        "ground" to { list(Bot.getGroundItems().map(::groundEntry)) },
    )

    private val actions = mapOf<String, (List<String>) -> JsonElement>(
        "walk" to { Bot.walkTo(Tile(it[0].toInt(), it[1].toInt(), 0)); text("ok") },
        "npc" to { a ->
            val name = a.dropLast(1).joinToString(" ")
            val npc = name.toIntOrNull()?.let { Bot.getClosestNPC(it) } ?: Bot.getClosestNPC(name)
            Bot.clickNPC(npc, a.last().toInt())
            JsonPrimitive(npc != null)
        },
        "object" to { a ->
            val name = a.dropLast(1).joinToString(" ")
            val op = a.last()
            val obj = name.toIntOrNull()?.let { Bot.getClosestObject(it) } ?: Bot.getClosestObject(name)
            op.toIntOrNull()?.let { Bot.clickObject(obj, it) } ?: Bot.clickObject(obj, op)
            JsonPrimitive(obj != null)
        },
        "take" to { Bot.takeGroundItem(it[0].toInt(), Tile(it[1].toInt(), it[2].toInt(), 0)); text("ok") },
        "widget" to { a ->
            val n = { i: Int, d: Int -> a.getOrNull(i)?.toInt() ?: d }
            Bot.clickButton(n(0, 0), n(1, 0), n(3, -1), n(4, -1), n(2, 1))
            text("ok")
        },
        "command" to { Bot.command(it.joinToString(" ")); text("ok") },
        "console" to { Bot.handleConsoleCommand(it.toTypedArray()); text("ok") },
        "logout" to { Bot.sendLogout(it.firstOrNull()?.toBoolean() ?: true); text("ok") },
    )

    private fun scriptNamed(name: String) = ScriptExecutor.scripts.values.firstOrNull {
        it.id.equals(name, true) || it.name.equals(name, true) || it.className.equals(name, true)
    } ?: error("no script $name")

    private val direct: Map<String, (List<String>) -> JsonElement> = mapOf(
        "screenshot" to { a ->
            val path = a.firstOrNull()?.let(Path::of) ?: Footprints.shots.resolve("${System.currentTimeMillis()}-t${Footprints.gameTick}.png")
            text(FrameCapture.request(path).also { Footprints.log("shot", "path" to text(it)) })
        },
        "stream-start" to { NativeStream.start(it) },
        "stream-stop" to { require(it.isEmpty()) { "usage: stream-stop" }; NativeStream.stop() },
        "stream-state" to { require(it.isEmpty()) { "usage: stream-state" }; NativeStream.state() },
        "remote-input" to { NativeInput.receive(it) },
        "dump-frame" to { a ->
            val path = a.firstOrNull()?.let(Path::of) ?: Footprints.shots.resolve("${System.currentTimeMillis()}-t${Footprints.gameTick}.orf")
            text(com.jagex.game.runetek5.graphics.modern.dump.FrameDump.request(path))
        },
        "dump-frames" to { a ->
            val dir = Path.of(a.getOrNull(0) ?: error("usage: dump-frames <dir> <count> [every=1]"))
            val count = a.getOrNull(1)?.toIntOrNull() ?: error("usage: dump-frames <dir> <count> [every=1]")
            require(a.size in 2..3) { "usage: dump-frames <dir> <count> [every=1]" }
            val every = a.getOrNull(2)?.let { it.removePrefix("every=").toInt() } ?: 1
            text(com.jagex.game.runetek5.graphics.modern.dump.FrameDump.sequence(dir, count, every) { Footprints.gameTick })
        },
        "help" to { list((reads.keys + actions.keys + direct.keys + listOf("wait-tick", "wait-idle", "wait-until")).sorted().map(::text)) },
        "scripts" to {
            if (ScriptExecutor.scripts.isEmpty()) ScriptExecutor.loadScripts()
            list(ScriptExecutor.scripts.values.map { entry("id" to text(it.id), "name" to text(it.name), "running" to JsonPrimitive(it.running)) })
        },
        "run" to { a ->
            if (ScriptExecutor.scripts.isEmpty()) ScriptExecutor.loadScripts()
            val md = scriptNamed(a.joinToString(" "))
            md.runningClass()?.let { ScriptExecutor.deactivate(it) }
            ScriptExecutor.activate(md.load().getDeclaredConstructor().also { it.isAccessible = true }.newInstance() as Script)
            text("started ${md.name}")
        },
        "stop" to { a ->
            if (a.isEmpty() || a[0] == "all") ScriptExecutor.stopAll() else scriptNamed(a.joinToString(" ")).runningClass()?.let { ScriptExecutor.deactivate(it) }
            text("ok")
        },
    )

    private class Waiter(val check: () -> JsonElement?, val deadline: Long, val result: CompletableFuture<JsonElement> = CompletableFuture())

    private val waiters = ConcurrentLinkedQueue<Waiter>()

    private fun snapshot() = entry("tick" to number(Footprints.gameTick), "x" to number(Bot.getMyPlayerX()), "y" to number(Bot.getMyPlayerY()), "z" to number(Bot.getMyPlayer().plane.toInt()))

    private fun busy() = Bot.getMyPlayer().movementStepsLeft > 0 || Bot.isAnimating()

    private fun total(item: String) = Bot.inventory.container.let { c ->
        c.itemIds.indices.filter { c.itemIds[it] != -1 && (c.itemIds[it] == item.toIntOrNull() || item.toIntOrNull() == null && itemName(c.itemIds[it]).contains(item, true)) }.sumOf { c.amounts[it] }
    }

    private fun after(ticks: Int): () -> JsonElement? {
        var target = -1
        return { if (target < 0) target = Footprints.gameTick + ticks; if (Footprints.gameTick >= target) snapshot() else null }
    }

    private fun idle(ticks: Int): () -> JsonElement? {
        var since = -1
        return {
            if (busy()) since = -1 else if (since < 0) since = Footprints.gameTick
            if (since >= 0 && Footprints.gameTick - since >= ticks) snapshot() else null
        }
    }

    private fun chatMatching(needle: String): () -> JsonElement? {
        var baseline: ChatLine? = null
        var started = false
        return {
            if (!started) { started = true; baseline = ChatBox.CHAT_LINES[0] }
            Footprints.freshChat(baseline).firstOrNull { it.message?.contains(needle, true) == true }?.let { entry("message" to text(it.message), "tick" to number(Footprints.gameTick)) }
        }
    }

    private fun xpGain(skill: Int): () -> JsonElement? {
        var base = -1
        return { if (base < 0) base = Bot.getXp(skill); if (Bot.getXp(skill) > base) snapshot() else null }
    }

    private fun condition(a: List<String>): () -> JsonElement? {
        val rest = a.drop(1)
        return when (a.firstOrNull()) {
            "pos" -> { { if (Bot.getMyPlayerPosition().let { kotlin.math.abs(it.x - rest[0].toInt()) <= (rest.getOrNull(2)?.toInt() ?: 0) && kotlin.math.abs(it.y - rest[1].toInt()) <= (rest.getOrNull(2)?.toInt() ?: 0) }) snapshot() else null } }
            "idle" -> idle(rest.firstOrNull()?.toInt() ?: 2)
            "tick" -> after(rest.firstOrNull()?.toInt() ?: 1)
            "interface" -> { { if (Bot.interfaceOpen(rest[0].toInt())) snapshot() else null } }
            "closed" -> { { if (!Bot.interfaceOpen(rest[0].toInt())) snapshot() else null } }
            "chat" -> chatMatching(rest.joinToString(" "))
            "inv" -> { { if (total(rest[0]) >= (rest.getOrNull(1)?.toInt() ?: 1)) snapshot() else null } }
            "state" -> { { if (Client.gameState.toString().equals(rest[0], true)) snapshot() else null } }
            "xp" -> xpGain(Skill.entries.first { it.name.equals(rest[0], true) }.ordinal)
            "npc" -> { { if (Bot.getClosestNPC(rest.joinToString(" ")) != null) snapshot() else null } }
            else -> error("conditions: pos x y [r], idle [n], tick [n], interface id, closed id, chat text, inv item [min], state name, xp skill, npc name")
        }
    }

    private fun wait(check: () -> JsonElement?, timeout: Long): JsonElement {
        val waiter = Waiter(check, System.currentTimeMillis() + timeout)
        waiters.add(waiter)
        return try {
            waiter.result.get(timeout + 2000, TimeUnit.MILLISECONDS)
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause ?: e
        }
    }

    private fun timed(args: List<String>) = args.partition { it.startsWith("timeout=") }.let { (t, rest) -> rest to (t.firstOrNull()?.substringAfter('=')?.toLong() ?: 30000L) }

    private val waits: Map<String, (List<String>) -> JsonElement> = mapOf(
        "wait-tick" to { a -> timed(a).let { (r, t) -> wait(after(r.firstOrNull()?.toInt() ?: 1), t) } },
        "wait-idle" to { a -> timed(a).let { (r, t) -> wait(idle(r.firstOrNull()?.toInt() ?: 2), t) } },
        "wait-until" to { a -> timed(a).let { (r, t) -> wait(condition(r), t) } },
    )

    private fun poll(waiter: Waiter): Boolean = try {
        val value = waiter.check()
        when {
            value != null -> waiter.result.complete(value)
            System.currentTimeMillis() > waiter.deadline -> waiter.result.completeExceptionally(java.util.concurrent.TimeoutException("wait timed out"))
            else -> false
        }
    } catch (e: Throwable) {
        waiter.result.completeExceptionally(e)
    }

    fun drain() {
        NativeInput.pulse()
        Footprints.observe()
        while (true) (queue.poll() ?: break).run()
        waiters.removeIf(::poll)
    }

    private fun respond(handler: (List<String>) -> JsonElement, args: List<String>) = try {
        entry("ok" to JsonPrimitive(true), "data" to handler(args))
    } catch (e: Throwable) {
        entry("ok" to JsonPrimitive(false), "error" to text(e.message ?: e.javaClass.simpleName))
    }

    private fun queued(cmd: String) = reads[cmd] ?: actions[cmd]

    private fun logged(cmd: String, args: List<String>, response: JsonObject) = response.also {
        if (cmd !in setOf("help", "tick", "remote-input", "stream-state")) Footprints.log("cmd", "cmd" to text(cmd), "args" to list(args.map(::text)), "ok" to it["ok"]!!, *(it["error"]?.let { e -> arrayOf("error" to e) } ?: emptyArray()))
    }

    private fun single(cmd: String, args: List<String>): JsonObject {
        val handler = direct[cmd] ?: waits[cmd]
        if (handler != null) return logged(cmd, args, respond(handler, args))
        val queuedHandler = queued(cmd) ?: return entry("ok" to JsonPrimitive(false), "error" to text("unknown command $cmd"))
        val task = FutureTask { respond(queuedHandler, args) }
        queue.add(task)
        return logged(cmd, args, try {
            task.get(10, TimeUnit.SECONDS)
        } catch (e: Throwable) {
            entry("ok" to JsonPrimitive(false), "error" to text(e.message ?: "timed out, client not pulsing"))
        })
    }

    private fun parse(item: JsonElement) = item.jsonObject.let { it["cmd"]!!.jsonPrimitive.content to (it["args"]?.jsonArray?.map { a -> a.jsonPrimitive.content } ?: emptyList()) }

    private fun batch(items: List<Pair<String, List<String>>>): JsonArray {
        val results = ArrayList<JsonElement>()
        var i = 0
        while (i < items.size && results.none { it.jsonObject["ok"] == JsonPrimitive(false) }) {
            val group = items.drop(i).takeWhile { queued(it.first) != null }
            if (group.size > 1) {
                val task = FutureTask {
                    val out = ArrayList<JsonObject>()
                    for ((cmd, args) in group) {
                        val response = respond(queued(cmd)!!, args)
                        out.add(logged(cmd, args, response))
                        if (response["ok"] == JsonPrimitive(false)) break
                    }
                    out
                }
                queue.add(task)
                results.addAll(task.get(10, TimeUnit.SECONDS))
                i += group.size
            } else {
                results.add(items[i].let { single(it.first, it.second) })
                i++
            }
        }
        return JsonArray(results)
    }

    private fun handle(line: String): JsonObject = try {
        val request = Json.parseToJsonElement(line).jsonObject
        request["batch"]?.let { entry("ok" to JsonPrimitive(true), "data" to batch(it.jsonArray.map(::parse))) } ?: parse(request).let { single(it.first, it.second) }
    } catch (e: Throwable) {
        entry("ok" to JsonPrimitive(false), "error" to text(e.message ?: e.javaClass.simpleName))
    }

    private fun serve(channel: SocketChannel) = channel.use {
        val reader = Channels.newReader(it, "UTF-8").buffered()
        val writer = Channels.newWriter(it, "UTF-8").buffered()
        reader.lineSequence().filter(String::isNotBlank).forEach { line ->
            writer.write(handle(line).toString())
            writer.newLine()
            writer.flush()
        }
    }

    fun start(spec: String) {
        val server = if (spec.all(Char::isDigit)) {
            ServerSocketChannel.open().bind(InetSocketAddress(InetAddress.getLoopbackAddress(), spec.toInt()))
        } else {
            val path = Path.of(spec)
            Files.deleteIfExists(path)
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(UnixDomainSocketAddress.of(path)).also {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
            }
        }
        thread(isDaemon = true, name = "control-accept") {
            runCatching { while (true) server.accept().let { c -> thread(isDaemon = true, name = "control-client") { runCatching { serve(c) } } } }
        }
        println("Control server listening on $spec")
    }
}
