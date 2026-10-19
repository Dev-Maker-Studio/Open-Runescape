package com.darkan.crash

import com.darkan.bot.Bot
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.jar.Manifest

/** Version and commit the build stamped into the jar manifest. */
object ClientBuild {
    val version: String
    val commit: String

    init {
        val attributes = runCatching {
            ClientBuild::class.java.getResource("/META-INF/MANIFEST.MF")?.openStream()?.use { Manifest(it).mainAttributes }
        }.getOrNull()
        version = attributes?.getValue("Implementation-Version") ?: "development"
        commit = attributes?.getValue("Git-Commit") ?: "unknown"
    }

    /** `2.1.0 (f33cbf08)`, or `development` when run from an IDE. */
    fun describe(): String = if (version == "development") version else "$version ($commit)"
}

/**
 * A crash report in two sizes: [discordText] fits in one Discord message, and
 * [fullText] is the file on disk with the whole stack trace and console tail.
 * The file starts with the Discord text so the launcher can show the same
 * summary without understanding the rest of the format.
 */
class CrashReport private constructor(
    val fileName: String,
    val discordText: String,
    val fullText: String,
) {
    fun save(directory: File = CrashHandler.logsDir()): File? = runCatching {
        directory.mkdirs()
        File(directory, fileName).also { it.writeText(fullText) }
    }.getOrNull()

    companion object {
        /** Discord rejects messages over 2000 characters; leave room for the user's own sentence. */
        private const val DISCORD_BUDGET = 1800
        const val FULL_DETAILS_MARKER = "==== Full details below (the part above is enough for Discord) ===="

        private val fileStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
        private val readableStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC)
        private val startedAt = System.currentTimeMillis()

        fun build(throwable: Throwable?, context: String, recentErrors: List<String>, fatal: Boolean): CrashReport {
            val now = Instant.now()
            val header = header(context, now, fatal)
            val console = ConsoleRecorder.snapshot()
            val trace = throwable?.let { fullTrace(it) }

            val discord = buildString {
                append("```\n")
                append(header).append('\n')
                if (throwable != null) {
                    append('\n').append(compactTrace(throwable)).append('\n')
                }
                val used = length + 4
                val tail = fitConsoleTail(console, DISCORD_BUDGET - used)
                if (tail.isNotEmpty()) {
                    append("\nLast console lines:\n")
                    tail.forEach { append(it).append('\n') }
                }
                append("```")
            }

            val full = buildString {
                append(discord).append("\n\n")
                append(FULL_DETAILS_MARKER).append("\n\n")
                append("JVM arguments: ").append(jvmArguments()).append('\n')
                append("Java home: ").append(System.getProperty("java.home")).append('\n')
                append("Working directory: ").append(System.getProperty("user.dir")).append('\n')
                append("Processors: ").append(Runtime.getRuntime().availableProcessors()).append('\n')
                append("Client uptime: ").append(uptime()).append('\n')
                if (trace != null) {
                    append("\nFull stack trace:\n").append(trace).append('\n')
                }
                if (recentErrors.isNotEmpty()) {
                    append("\nRecent non-fatal errors (oldest first):\n")
                    recentErrors.forEach { append(it).append('\n') }
                }
                append("\nConsole (last ${console.size} lines):\n")
                console.forEach { append(it).append('\n') }
            }

            val kind = if (fatal) "crash" else "report"
            val name = "$kind-${fileStamp.format(now)}.txt"
            return CrashReport(name, scrub(discord), scrub(full))
        }

        private fun header(context: String, now: Instant, fatal: Boolean): String {
            val runtime = Runtime.getRuntime()
            val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            val maxMb = runtime.maxMemory() / (1024 * 1024)
            val toolkit = toolkitDescription()
            val gpu = gpuDescription()
            val state = runCatching { Client.gameState.name }.getOrDefault("unknown")
            val lobby = "${Loader.IP_ADDRESS ?: "?"}:${Loader.LOBBY_PORT}"
            return buildString {
                append(if (fatal) "Darkan client crash" else "Darkan client report").append('\n')
                append("Client ${ClientBuild.describe()} · $toolkit · bot ${if (Bot.ENABLED) "on" else "off"}\n")
                append("Java ${System.getProperty("java.version")} (${System.getProperty("java.vendor")}) · ")
                append("${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")} · ")
                append("heap $usedMb/$maxMb MB\n")
                append("${readableStamp.format(now)} · up ${uptime()} · state $state · lobby $lobby\n")
                if (gpu != null) append("GPU: $gpu\n")
                append("Where: $context")
            }
        }

        private fun toolkitDescription(): String {
            val requested = System.getProperty("toolkit")?.trim()
            val active = runCatching { PreferenceManager.preferences?.activeToolkit?.getValue() }.getOrNull()
            val id = active ?: requested?.toIntOrNull()
            val name = when (id) {
                3 -> "modern OpenGL"
                1 -> "legacy OpenGL"
                0 -> "safe mode"
                null -> "renderer unknown"
                else -> "renderer $id"
            }
            return if (id != null) "$name (toolkit $id)" else name
        }

        private fun gpuDescription(): String? = runCatching {
            val toolkit = Renderers.activeToolkit as? OpenGlToolkit ?: return null
            val vendor = toolkit.gpuCompanyString ?: return null
            val name = toolkit.gpuNameString ?: return null
            val major = toolkit.openglVersion / 10
            val minor = toolkit.openglVersion % 10
            "$vendor / $name (GL $major.$minor)"
        }.getOrNull()

        private fun jvmArguments(): String = runCatching {
            ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" ")
        }.getOrDefault("unknown")

        private fun uptime(): String {
            val seconds = (System.currentTimeMillis() - startedAt) / 1000
            return when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
                else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            }
        }

        fun fullTrace(throwable: Throwable): String {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            return writer.toString().trimEnd()
        }

        /**
         * The exception chain with only the frames that matter: the first few of the
         * top-level exception, fewer for each cause, and the long engine package
         * names shortened. `rt5.` is `com.jagex.game.runetek5.`, `darkan.` is `com.darkan.`.
         */
        fun compactTrace(throwable: Throwable, topFrames: Int = 10, causeFrames: Int = 4): String {
            val out = StringBuilder()
            var current: Throwable? = throwable
            var depth = 0
            val seen = HashSet<Throwable>()
            while (current != null && seen.add(current) && depth < 5) {
                if (depth > 0) out.append("Caused by: ")
                out.append(current.toString().take(400)).append('\n')
                val frames = current.stackTrace
                val keep = if (depth == 0) topFrames else causeFrames
                frames.take(keep).forEach { out.append("    at ").append(shortenFrame(it.toString())).append('\n') }
                if (frames.size > keep) out.append("    ... ${frames.size - keep} more\n")
                current = current.cause
                depth++
            }
            return out.toString().trimEnd()
        }

        private fun shortenFrame(frame: String): String =
            frame.replace("com.jagex.game.runetek5.", "rt5.").replace("com.darkan.", "darkan.")

        /** The newest console lines that fit in [budget] characters, oldest first. */
        private fun fitConsoleTail(lines: List<String>, budget: Int): List<String> {
            if (budget < 80) return emptyList()
            var remaining = budget - 24
            val picked = ArrayList<String>()
            for (line in lines.asReversed()) {
                val cost = line.length + 1
                if (cost > remaining) break
                remaining -= cost
                picked.add(line)
                if (picked.size >= 25) break
            }
            return picked.asReversed()
        }

        /** Keeps account names and passwords out of anything a player is asked to paste publicly. */
        private fun scrub(text: String): String {
            val secrets = listOf(Bot.password, Bot.username, PlayerUpdate.MY_PLAYER_USERNAME)
                .filterNotNull()
                .filter { it.length >= 3 }
                .sortedByDescending { it.length }
            var result = text
            secrets.forEach { result = result.replace(it, "[hidden]", ignoreCase = true) }
            return result
        }
    }
}
