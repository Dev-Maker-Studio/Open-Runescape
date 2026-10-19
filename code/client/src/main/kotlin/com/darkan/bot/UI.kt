package com.darkan.bot

import com.darkan.bot.scripts.ScriptMetadata
import com.darkan.bot.ui.BotWorkspace
import com.darkan.bot.ui.Dockable
import com.darkan.bot.ui.DockablePane
import com.darkan.bot.ui.LogBus
import com.darkan.bot.ui.LogsPanel
import com.darkan.bot.ui.ScriptConfigWrapper
import com.darkan.bot.ui.ScriptsPanel
import com.darkan.bot.ui.SettingsPanel
import com.darkan.bot.ui.SkillsPanel
import com.darkan.bot.ui.UiPreferences
import com.darkan.bot.ui.VarsPanel
import com.darkan.bot.utils.Skill
import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.ToolTipManager

lateinit var UI: BotUI

var SETTING_VARP_DEBUG_ENABLED = false
var SETTING_VARC_DEBUG_ENABLED = false

fun initializeUI() {
    runOnEdt {
        LafManager.install(DarculaTheme())
        // The game canvas is a heavyweight AWT component; lightweight popups and
        // tooltips would otherwise be painted underneath it.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false)
        ToolTipManager.sharedInstance().isLightWeightPopupEnabled = false
        ToolTipManager.sharedInstance().initialDelay = 400
        UI = BotUI()
    }
}

/** Runs [block] on the event dispatch thread, synchronously, whichever thread calls it. */
fun runOnEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeAndWait(block)
}

/**
 * Root of the bot sidebar: a dockable tab strip holding the Scripts, Logs,
 * Skills, Vars and Settings tools plus one closable tab per open script
 * configuration.
 */
class BotUI : JPanel(BorderLayout()) {
    val originalOut: PrintStream = System.out
    var logFile: File? = null
        private set

    /** Set by [BotWorkspace.install]; null when the sidebar is shown on its own. */
    var workspace: BotWorkspace? = null

    val dock = DockablePane { SwingUtilities.getWindowAncestor(this) }
    val scriptsPanel: ScriptsPanel
    val logsPanel: LogsPanel
    val skillsPanel = SkillsPanel()
    val varsPanel = VarsPanel()
    val settingsPanel: SettingsPanel

    private val configTabs = HashMap<String, ScriptConfigWrapper>()
    private val scriptsDockable: Dockable

    init {
        preferredSize = Dimension(520, 600)
        redirectSystemOut()
        LogBus.capacity = UiPreferences.getInt("logs.maxLines", LogBus.capacity)

        scriptsPanel = ScriptsPanel(openSettings = ::addConfigTab, onInstanceReplaced = ::rebindConfigTab)
        logsPanel = LogsPanel { logFile }
        settingsPanel = SettingsPanel(this)

        scriptsDockable = Dockable("scripts", "Scripts", scriptsPanel, tooltip = "Browse, configure and run scripts")
        dock.add(scriptsDockable)
        dock.add(Dockable("logs", "Logs", logsPanel, tooltip = "Console output"))
        dock.add(Dockable("skills", "Skills", skillsPanel, tooltip = "XP gained this session"))
        dock.add(Dockable("vars", "Vars", varsPanel, tooltip = "Watch varp / varc changes"))
        dock.add(Dockable("settings", "Settings", settingsPanel, tooltip = "Sidebar layout and preferences"))
        dock.onClosed = { configTabs.remove(it.id) }
        dock.restoreLayout()
        add(dock, BorderLayout.CENTER)

        Timer(1000) {
            val running = scriptsPanel.runningCount
            scriptsDockable.title = if (running > 0) "Scripts ($running)" else "Scripts"
        }.start()
    }

    // ---- script configuration tabs ----

    fun addConfigTab(scriptMetadata: ScriptMetadata, script: Any) {
        val id = configTabId(scriptMetadata)
        val existing = configTabs[id]
        if (existing != null) {
            if (existing.script === script) {
                dock.select(id)
                return
            }
            // The script was restarted with a fresh instance; rebind the form to it.
            dock.close(id)
        }
        val wrapper = ScriptConfigWrapper(script)
        configTabs[id] = wrapper
        dock.add(
            Dockable(id, "${scriptMetadata.name} settings", wrapper.configPanel, closable = true, tooltip = "Settings for ${scriptMetadata.name}"),
            select = true,
        )
    }

    fun closeConfigTab(scriptMetadata: ScriptMetadata) = dock.close(configTabId(scriptMetadata))

    /** If a settings tab is open for this script, points it at [script] (a restarted script gets a fresh instance). */
    fun rebindConfigTab(scriptMetadata: ScriptMetadata, script: Any) {
        val id = configTabId(scriptMetadata)
        val existing = configTabs[id] ?: return
        if (existing.script === script) return
        val wasSelected = dock.current()?.id == id
        val wasPoppedOut = dock.isPoppedOut(id)
        dock.close(id)
        addConfigTab(scriptMetadata, script)
        if (wasPoppedOut) dock.popOut(id) else if (!wasSelected) scriptsDockable.let { dock.select(it.id) }
    }

    private fun configTabId(scriptMetadata: ScriptMetadata) = "config:${scriptMetadata.id}"

    // ---- data feeds (safe from any thread) ----

    fun updateXpTable(skill: Skill, xpGained: Int) = skillsPanel.addXp(skill, xpGained)

    fun addVarcTableEntry(type: String, id: Int, prevValue: Int, newValue: Int) = varsPanel.record(type, id, prevValue, newValue)

    /** Rates now refresh on a timer; kept so older callers keep compiling. */
    fun updateAllXPPerHour() {}

    // ---- logging ----

    private fun redirectSystemOut() {
        try {
            val logDir = File(UiPreferences.dataDir, "logs")
            if (!logDir.exists()) logDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(Date())
            val pid = ProcessHandle.current().pid()
            logFile = File(logDir, "cheddarcheese-$timestamp-$pid.log")
            println("Attaching to logfile at $logFile")
            if (!logFile!!.exists()) logFile!!.createNewFile()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        System.setOut(PrintStream(TextPaneOutputStream(originalOut, logFile, isError = false), true))
        System.setErr(PrintStream(TextPaneOutputStream(System.err, logFile, isError = true), true))
    }
}

private var fileOutputStream: OutputStream? = null

/**
 * Tees stdout/stderr to the original stream, the session log file and
 * [LogBus] (which feeds the Logs tab) with a timestamp on every line.
 */
class TextPaneOutputStream(
    private val standardStream: PrintStream,
    logFile: File?,
    private val isError: Boolean = false,
) : OutputStream() {
    private val buffer = StringBuilder()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    init {
        if (fileOutputStream == null)
            fileOutputStream = logFile?.let { FileOutputStream(it, false) }
    }

    override fun write(b: Int) {
        val c = b.toChar()
        buffer.append(c)
        if (c == '\n') flushBuffer()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        buffer.append(String(b, off, len))
        if (buffer.contains("\n")) flushBuffer()
    }

    private fun flushBuffer() {
        if (buffer.isEmpty()) return
        val text = buffer.toString()
        buffer.clear()
        val timestampedLine = "[${LocalDateTime.now().format(formatter)}] $text"
        standardStream.print(timestampedLine)
        try {
            fileOutputStream?.write(timestampedLine.toByteArray())
        } catch (_: Exception) {
        }
        text.split('\n').filter { it.isNotEmpty() }.forEach { LogBus.publish(it, isError) }
    }

    override fun flush() {
        flushBuffer()
        standardStream.flush()
        fileOutputStream?.flush()
    }

    override fun close() {
        flush()
        standardStream.close()
        fileOutputStream?.close()
    }
}
