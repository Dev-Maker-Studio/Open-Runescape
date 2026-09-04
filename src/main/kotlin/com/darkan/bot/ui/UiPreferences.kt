package com.darkan.bot.ui

import java.awt.Rectangle
import java.io.File
import java.util.Properties
import javax.swing.Timer

/**
 * Persisted layout and behaviour preferences for the bot sidebar.
 *
 * Backed by `~/.cheddarcheese/ui.properties`. Writes are coalesced so that a
 * divider drag or a window move does not hammer the disk; the file is flushed
 * half a second after the last change and again when the client exits.
 */
object UiPreferences {
    val dataDir: File = File(System.getProperty("user.home"), ".cheddarcheese")
    private val file = File(dataDir, "ui.properties")
    private val props = Properties()
    private val saveTimer = Timer(500) { flush() }.apply { isRepeats = false }

    init {
        load()
        Runtime.getRuntime().addShutdownHook(Thread { flush() })
    }

    @Synchronized
    private fun load() {
        try {
            if (file.exists()) file.inputStream().use { props.load(it) }
        } catch (e: Exception) {
            System.err.println("Failed to load UI preferences: ${e.message}")
        }
    }

    @Synchronized
    fun flush() {
        try {
            dataDir.mkdirs()
            file.outputStream().use { props.store(it, "Darkan bot sidebar layout") }
        } catch (e: Exception) {
            System.err.println("Failed to save UI preferences: ${e.message}")
        }
    }

    @Synchronized
    fun getString(key: String, default: String? = null): String? = props.getProperty(key) ?: default

    fun getInt(key: String, default: Int): Int = getString(key)?.toIntOrNull() ?: default

    fun getBoolean(key: String, default: Boolean): Boolean = getString(key)?.toBooleanStrictOrNull() ?: default

    fun getRectangle(key: String): Rectangle? {
        val parts = getString(key)?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: return null
        if (parts.size != 4) return null
        return Rectangle(parts[0], parts[1], parts[2], parts[3])
    }

    fun getList(key: String): List<String> =
        getString(key)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    @Synchronized
    fun put(key: String, value: Any?) {
        val encoded = when (value) {
            null -> null
            is Rectangle -> "${value.x},${value.y},${value.width},${value.height}"
            is Collection<*> -> value.joinToString(",")
            else -> value.toString()
        }
        val previous = props.getProperty(key)
        if (encoded == null) props.remove(key) else props.setProperty(key, encoded)
        if (previous != encoded) saveTimer.restart()
    }

    /** Removes every key that starts with [prefix]. */
    @Synchronized
    fun clear(prefix: String) {
        val doomed = props.keys.map { it.toString() }.filter { it.startsWith(prefix) }
        doomed.forEach { props.remove(it) }
        if (doomed.isNotEmpty()) saveTimer.restart()
    }

    /** Wipes every stored preference; the next launch starts from the built-in defaults. */
    @Synchronized
    fun resetAll() {
        props.clear()
        flush()
    }
}
