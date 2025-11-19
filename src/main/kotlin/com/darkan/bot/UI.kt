package com.darkan.bot

import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.ScriptConfigWrapper
import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.ScriptMetadata
import com.darkan.bot.utils.Skill
import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
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
    private val xpTableModel = DefaultTableModel()
    private val xpData = mutableMapOf<Skill, Pair<Long, Int>>()
    val originalOut: PrintStream = System.out
    var logFile: File? = null
    private var lastLogModifiedTime = 0L
    private val configTabs = mutableMapOf<ScriptMetadata, ScriptConfigWrapper>()
    var isUIInitialized = false
    private val varcTableModel = DefaultTableModel()
    private val varcSearchField = JTextField()
    private val allVarcData = mutableListOf<Array<Any>>()
    private val maxVarcEntries = 100

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
            addTab("Scripts", ScriptSelectionPanel())
            addTab("Logs", JScrollPane(logTextPane))
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

    private fun createSettingsTab(): JPanel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(10, 10, 10, 10)

        val topPanel = JPanel()
        topPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val togglePanel = JPanel(FlowLayout(FlowLayout.LEFT))
            listOf("Toggle Varp Debug", "Toggle Varc Debug").forEach { setting ->
                togglePanel.add(JRadioButton(setting).apply {
                    addActionListener { handleSettingToggle(setting, isSelected) }
                })
            }
            add(togglePanel)

            val searchPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            searchPanel.add(JLabel("Search:"))
            varcSearchField.preferredSize = Dimension(200, 25)
            varcSearchField.addActionListener { filterVarcTable(varcSearchField.text) }
            searchPanel.add(varcSearchField)
            searchPanel.add(JButton("Clear Search").apply {
                addActionListener {
                    varcSearchField.text = ""
                    filterVarcTable("")
                }
            })
            searchPanel.add(JButton("Clear Table").apply {
                addActionListener { clearVarcTable() }
            })
            add(searchPanel)
        }
        add(topPanel, BorderLayout.NORTH)

        varcTableModel.setColumnIdentifiers(arrayOf("Type", "ID", "Previous Value", "New Value"))
        val varcTable = JTable(varcTableModel).apply {
            setDefaultRenderer(Object::class.java, object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
                ): Component {
                    val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    if (!isSelected && row < table.rowCount && table.columnCount >= 4) {
                        val prevValue = table.getValueAt(row, 2)?.toString() ?: ""
                        val newValue = table.getValueAt(row, 3)?.toString() ?: ""
                        if (column == 3 && prevValue != newValue && prevValue.isNotEmpty() && newValue.isNotEmpty()) {
                            c.background = table.background
                            c.font = c.font.deriveFont(java.awt.Font.BOLD)
                        } else {
                            c.background = table.background
                            c.font = c.font.deriveFont(java.awt.Font.PLAIN)
                        }
                    }
                    return c
                }
            })
        }
        add(JScrollPane(varcTable), BorderLayout.CENTER)
        isUIInitialized = true
    }

    private fun handleSettingToggle(settingName: String, isSelected: Boolean) {
        when(settingName) {
            "Toggle Varp Debug" -> SETTING_VARP_DEBUG_ENABLED = isSelected
            "Toggle Varc Debug" -> SETTING_VARC_DEBUG_ENABLED = isSelected
        }
    }

    private fun filterVarcTable(searchText: String) {
        if (!isUIInitialized) return
        SwingUtilities.invokeLater {
            varcTableModel.rowCount = 0
            if (searchText.isEmpty()) allVarcData.forEach { varcTableModel.addRow(it) }
            else allVarcData.filter { row ->
                row[0].toString().lowercase().contains(searchText.lowercase()) ||
                        row[1].toString().contains(searchText) ||
                        row[2].toString().contains(searchText) ||
                        row[3].toString().contains(searchText)
            }.forEach { varcTableModel.addRow(it) }
        }
    }

    fun addVarcTableEntry(type: String, id: Int, prevValue: Int, newValue: Int) {
        if (!isUIInitialized || prevValue == newValue) return
        SwingUtilities.invokeLater {
            val newRow: Array<Any> = arrayOf(type, id, prevValue, newValue)
            val existingIndex = allVarcData.indexOfFirst { it[0] == type && it[1] == id }
            if (existingIndex != -1) allVarcData[existingIndex] = newRow
            else {
                allVarcData.add(0, newRow)
                if (allVarcData.size > maxVarcEntries) allVarcData.removeAt(allVarcData.size - 1)
            }
            filterVarcTable(varcSearchField.text)
            for (i in 0 until varcTableModel.rowCount) {
                val rowType = varcTableModel.getValueAt(i, 0) as? String
                val rowId = varcTableModel.getValueAt(i, 1) as? Int
                if (rowType == type && rowId == id) {
                    varcTableModel.setValueAt(prevValue, i, 2)
                    varcTableModel.setValueAt(newValue, i, 3)
                    return@invokeLater
                }
            }
            varcTableModel.addRow(arrayOf<Any>(type, id, prevValue, newValue))
        }
    }

    private fun clearVarcTable() = SwingUtilities.invokeLater {
        allVarcData.clear()
        varcTableModel.rowCount = 0
        varcSearchField.text = ""
    }

    fun addConfigTab(scriptMetadata: ScriptMetadata, script: Any) {
        val tabTitle = "${scriptMetadata.name} Settings"
        val existingIndex = tabbedPane.indexOfTab(tabTitle)
        if (existingIndex != -1) {
            tabbedPane.selectedIndex = existingIndex
            return
        }

        val configWrapper = ScriptConfigWrapper(script)
        val panel = configWrapper.configPanel
        configTabs[scriptMetadata] = configWrapper
        tabbedPane.addTab(tabTitle, panel)
        val tabIndex = tabbedPane.indexOfComponent(panel)
        val tabComponent = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(JLabel(tabTitle))
            add(JButton("x").apply {
                margin = Insets(0, 5, 0, 5)
                isBorderPainted = false
                isFocusPainted = false
                isContentAreaFilled = false
                toolTipText = "Close tab"
                addActionListener {
                    tabbedPane.remove(panel)
                    configTabs.remove(scriptMetadata)
                }
            })
        }
        tabbedPane.setTabComponentAt(tabIndex, tabComponent)
        tabbedPane.selectedIndex = tabIndex
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
            val logDir = File(userHome, ".cheddarcheese/logs")
            if (!logDir.exists())
                logDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(Date())
            val pid = ProcessHandle.current().pid()
            logFile = File(logDir, "cheddarcheese-${timestamp}-${pid}.log")
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