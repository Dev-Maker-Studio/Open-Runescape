package com.darkan.bot

import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.ScriptConfigWrapper
import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.ScriptMetadata
import com.darkan.bot.utils.Skill
import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Image
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel

lateinit var UI: CheddarCheeseUI

private const val LOG_LINE_MAX = 100

var SETTING_VARP_DEBUG_ENABLED = false
var SETTING_VARC_DEBUG_ENABLED = false

fun initializeUI() {
    SwingUtilities.invokeAndWait {
        LafManager.install(DarculaTheme())
        UI = CheddarCheeseUI()
    }
}

class CheddarCheeseUI : JPanel() {
    private val scriptComboBox = object : JComboBox<ScriptMetadata>() {
        override fun getToolTipText(): String? {
            val selectedItem = selectedItem as? ScriptMetadata
            return selectedItem?.description
        }
    }
    private val currentScriptLabel = JLabel("No script running")
    private val xpTableModel = DefaultTableModel()
    private val xpData = mutableMapOf<Skill, Pair<Long, Int>>()
    val originalOut: PrintStream = System.out
    var logFile: File? = null
    private var lastLogModifiedTime = 0L
    private val configTabs = mutableMapOf<ScriptMetadata, ScriptConfigWrapper>()

    val logTextPane = object : JTextPane() {
        override fun getScrollableTracksViewportWidth(): Boolean {
            return true
        }
    }

    private val tabbedPane = JTabbedPane()

    init {
        layout = BorderLayout()
        preferredSize = Dimension(600, 600)

        tabbedPane.apply {
            addTab("Scripts", createScriptsTab())
            addTab("Skills", createSkillsTab())
            addTab("Settings", createSettingsTab())
        }
        add(tabbedPane, BorderLayout.CENTER)

        redirectSystemOut()

        val logUpdateTimer = Timer(600) { updateLogTextPane() }
        logUpdateTimer.start()
    }

    private fun loadCustomIcon(): List<Image>? {
        return try {
            val iconStream = this::class.java.getResourceAsStream("/icons/logo.png")
            if (iconStream != null) {
                val originalImage = ImageIO.read(iconStream)
                listOf(
                    originalImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH),
                    originalImage.getScaledInstance(32, 32, Image.SCALE_SMOOTH),
                    originalImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH),
                    originalImage
                )
            } else {
                println("Icon resource not found")
                null
            }
        } catch (e: Throwable) {
            println(e.stackTraceToString())
            e.printStackTrace()
            null
        }
    }

    private fun createSettingsTab(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = EmptyBorder(10, 10, 10, 10)

        val settings = listOf("Toggle Varp Debug", "Toggle Varc Debug")
        settings.forEach { setting ->
            add(JRadioButton(setting).apply {
                addActionListener { handleSettingToggle(setting, isSelected) }
            })
        }
    }

    private fun handleSettingToggle(settingName: String, isSelected: Boolean) {
        when(settingName) {
            "Toggle Varp Debug" -> SETTING_VARP_DEBUG_ENABLED = isSelected
            "Toggle Varc Debug" -> SETTING_VARC_DEBUG_ENABLED = isSelected
        }
    }

    private fun createScriptsTab(): JPanel = JPanel().apply {
        layout = BorderLayout()
        border = EmptyBorder(10, 10, 10, 10)

        val controlsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            populateScriptComboBox()

            add(scriptComboBox.apply {
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
                toolTipText = ""
                renderer = createScriptRenderer()
            })

            val startButton = JButton("Start script").apply {
                addActionListener { startScript() }
            }
            val stopButton = JButton("Stop scripts").apply {
                addActionListener { stopScripts() }
            }

            add(Box.createRigidArea(Dimension(10, 0)))
            add(startButton)
            add(Box.createRigidArea(Dimension(5, 0)))
            add(stopButton)
        }

        val statusPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(Box.createRigidArea(Dimension(0, 10)))
            add(currentScriptLabel)
        }

        logTextPane.isEditable = false
        val logScrollPane = JScrollPane(logTextPane)

        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(controlsPanel)
            add(statusPanel)
        }

        add(topPanel, BorderLayout.NORTH)
        add(logScrollPane, BorderLayout.CENTER)
    }

    private fun populateScriptComboBox() {
        scriptComboBox.model = DefaultComboBoxModel(ScriptExecutor.scripts.values.toTypedArray())
        scriptComboBox.selectedIndex = -1
    }

    private fun createScriptRenderer(): ListCellRenderer<ScriptMetadata> {
        return object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                val scriptValue = value as? ScriptMetadata
                text = scriptValue?.toString() ?: "Select a script"
                toolTipText = scriptValue?.description
                return this
            }
        } as ListCellRenderer<ScriptMetadata>
    }

    private fun startScript() {
        val scriptMetadata = scriptComboBox.selectedItem as? ScriptMetadata
        if (scriptMetadata != null) {
            try {
                val script = scriptMetadata.scriptClass.getDeclaredConstructor().newInstance()
                if (script is ConfigurableScript)
                    addConfigTab(scriptMetadata, script)

                ScriptExecutor.activate(script)
                currentScriptLabel.text = "Running: ${scriptMetadata.name}"
                println("Starting script: ${scriptMetadata.name}")
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(this, "Failed to start script: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                e.printStackTrace()
            }
        } else
            JOptionPane.showMessageDialog(this, "Please select a script to start.", "No Script Selected", JOptionPane.WARNING_MESSAGE)
    }

    private fun addConfigTab(scriptMetadata: ScriptMetadata, script: Any) {
        if (configTabs.containsKey(scriptMetadata)) {
            SwingUtilities.invokeLater {
                configTabs[scriptMetadata]?.let { wrapper ->
                    tabbedPane.remove(wrapper.configPanel)
                }
                configTabs.remove(scriptMetadata)
                addNewConfigTab(scriptMetadata, script)
            }
        } else
            addNewConfigTab(scriptMetadata, script)
    }

    private fun addNewConfigTab(scriptMetadata: ScriptMetadata, script: Any) {
        val configWrapper = ScriptConfigWrapper(script)
        configTabs[scriptMetadata] = configWrapper

        tabbedPane.addTab("${scriptMetadata.name} Config", configWrapper.configPanel)
        tabbedPane.selectedComponent = configWrapper.configPanel
    }

    private fun removeConfigTab(scriptMetadata: ScriptMetadata) {
        if (configTabs.containsKey(scriptMetadata)) {
            SwingUtilities.invokeLater {
                configTabs[scriptMetadata]?.let { wrapper ->
                    tabbedPane.remove(wrapper.configPanel)
                    configTabs.remove(scriptMetadata)
                }
            }
        }
    }

    private fun stopScripts() {
        // Remove all config tabs
        configTabs.keys.toList().forEach { scriptMetadata ->
            removeConfigTab(scriptMetadata)
        }

        ScriptExecutor.stopAll()
        currentScriptLabel.text = "No script running"
    }

    private fun createSkillsTab(): JPanel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(10, 10, 10, 10)
        xpTableModel.setColumnIdentifiers(arrayOf("Skill", "XP Gained", "XP p/h"))
        val table = JTable(xpTableModel)
        addRightClickContextMenuToTable(table)
        add(JScrollPane(table), BorderLayout.CENTER)
    }

    private fun addRightClickContextMenuToTable(table: JTable) {
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) showContextMenu(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) showContextMenu(e)
            }

            private fun showContextMenu(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                if (row >= 0 && row < table.rowCount) {
                    table.setRowSelectionInterval(row, row)
                    val skill = table.getValueAt(row, 0) as Skill
                    val popupMenu = JPopupMenu()
                    val resetMenuItem = JMenuItem("Reset XP")
                    val removeMenuItem = JMenuItem("Remove Row")

                    resetMenuItem.addActionListener { resetXpValues(skill, row) }
                    removeMenuItem.addActionListener { removeRow(skill, row) }

                    popupMenu.add(resetMenuItem)
                    popupMenu.add(removeMenuItem)
                    popupMenu.show(e.component, e.x, e.y)
                }
            }
        })
    }

    private fun resetXpValues(skill: Skill, row: Int) {
        SwingUtilities.invokeLater {
            xpData[skill] = System.currentTimeMillis() to 0
            xpTableModel.setValueAt("0", row, 1)
            xpTableModel.setValueAt("0", row, 2)
        }
    }

    private fun removeRow(skill: Skill, row: Int) {
        SwingUtilities.invokeLater {
            xpData.remove(skill)
            xpTableModel.removeRow(row)
        }
    }

    fun updateAllXPPerHour() {
        SwingUtilities.invokeLater {
            Skill.entries.forEach { skill ->
                val totalXp = xpData.getOrDefault(skill, System.currentTimeMillis() to 0)
                val rowIndex = findSkillRowIndex(skill)
                if (rowIndex != -1)
                    xpTableModel.setValueAt(getFormattedUnitsPerHour(totalXp.second, totalXp.first), rowIndex, 2)
            }
        }
    }

    fun updateXpTable(skill: Skill, xpGained: Int) {
        val totalXp = xpData.getOrDefault(skill, System.currentTimeMillis() to 0)
        xpData[skill] = totalXp.first to totalXp.second + xpGained

        SwingUtilities.invokeLater {
            val rowIndex = findSkillRowIndex(skill)
            if (rowIndex != -1) {
                xpTableModel.setValueAt(format(totalXp.second), rowIndex, 1)
                xpTableModel.setValueAt(getFormattedUnitsPerHour(totalXp.second, totalXp.first), rowIndex, 2)
            } else
                xpTableModel.addRow(arrayOf(skill, format(totalXp.second), getFormattedUnitsPerHour(totalXp.second, totalXp.first)))
        }
    }

    private fun findSkillRowIndex(skill: Skill): Int {
        for (i in 0 until xpTableModel.rowCount)
            if (xpTableModel.getValueAt(i, 0) == skill)
                return i
        return -1
    }

    private fun redirectSystemOut() {
        try {
            val userHome = System.getProperty("user.home")
            val cheddarCheeseDir = File(userHome, ".cheddarcheese")
            if (!cheddarCheeseDir.exists())
                cheddarCheeseDir.mkdirs()
            logFile = File(cheddarCheeseDir, "cheddarcheese-log.txt")
            println("Attaching to logfile at $logFile")
            if (!logFile!!.exists())
                logFile!!.createNewFile()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        val outStream = TextPaneOutputStream(originalOut, logFile)
        val errStream = TextPaneOutputStream(System.err, logFile)

        System.setOut(PrintStream(outStream, true))
        System.setErr(PrintStream(errStream, true))
    }

    private fun updateLogTextPane() {
        if (logFile == null || !logFile!!.exists()) return

        val modifiedTime = logFile!!.lastModified()
        if (modifiedTime > lastLogModifiedTime) {
            lastLogModifiedTime = modifiedTime
            val lines = readLastLines(logFile!!)
            SwingUtilities.invokeLater {
                logTextPane.text = lines.joinToString("\n")
                logTextPane.caretPosition = logTextPane.document.length
            }
        }
    }

    private fun readLastLines(file: File): List<String> {
        val result = mutableListOf<String>()
        RandomAccessFile(file, "r").use { raf ->
            var filePointer = raf.length() - 1
            var line = ""
            while (filePointer >= 0 && result.size < LOG_LINE_MAX) {
                raf.seek(filePointer)
                val readByte = raf.readByte()
                if (readByte.toInt().toChar() == '\n' && line.isNotEmpty()) {
                    result.add(line.reversed())
                    line = ""
                } else
                    line += readByte.toInt().toChar()
                filePointer--
            }
            if (line.isNotEmpty() && result.size < LOG_LINE_MAX)
                result.add(line.reversed())
        }
        return result.reversed()
    }
}

private var fileOutputStream: OutputStream? = null

class TextPaneOutputStream(
    private val standardStream: PrintStream,
    logFile: File?
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
        if (c == '\n')
            flushBuffer()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        val text = String(b, off, len)
        buffer.append(text)
        if (buffer.contains("\n"))
            flushBuffer()
    }

    private fun flushBuffer() {
        if (buffer.isNotEmpty()) {
            val timestampedLine = "[${LocalDateTime.now().format(formatter)}] ${buffer.toString()}"
            standardStream.print(timestampedLine)
            fileOutputStream?.write(timestampedLine.toByteArray())
            buffer.clear()
        }
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