package com.darkan.bot.ui

import com.darkan.bot.BotUI
import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.getScriptsDirectory
import com.darkan.crash.ClientBuild
import com.darkan.crash.CrashDialog
import com.darkan.crash.CrashHandler
import com.jagex.game.launch.Loader
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class SettingsPanel(private val botUi: BotUI) : JPanel(BorderLayout()) {
    private val form = ScrollableColumn(GridBagLayout()).apply { isOpaque = false }
    private var row = 0

    init {
        border = Spacing.panelBorder()

        section("Sidebar")
        val sideCombo = JComboBox(BotWorkspace.Side.entries.map { it.label }.toTypedArray()).apply {
            selectedItem = (botUi.workspace?.side ?: BotWorkspace.Side.RIGHT).label
            addActionListener {
                val chosen = BotWorkspace.Side.entries.first { it.label == selectedItem }
                botUi.workspace?.side = chosen
            }
        }
        field("Position", sideCombo, "Which side of the game the sidebar sits on")
        field("Tabs", WrappingText("Drag tabs to reorder them. Right-click a tab, or use the ⋮ menu, to pop it out into its own window.").apply {
            foreground = Palette.secondaryText
            font = font.deriveFont(11f)
        })
        actions(
            button("Hide sidebar", "Collapse the sidebar to a slim handle; click the handle to bring it back") { botUi.workspace?.sidebarVisible = false },
            button("Dock all windows", "Return every popped-out panel to the sidebar") { botUi.dock.dockAll() },
            button("Reset layout", "Restore the default side, width, tab order and window size") { botUi.workspace?.resetLayout() },
        )

        section("Logs")
        val linesSpinner = JSpinner(SpinnerNumberModel(LogBus.capacity, 500, 20000, 500)).apply {
            addChangeListener {
                LogBus.capacity = value as Int
                UiPreferences.put("logs.maxLines", LogBus.capacity)
            }
        }
        field("Lines kept", linesSpinner, "How many recent lines the Logs tab keeps in memory")
        field("Log file", WrappingText(botUi.logFile?.absolutePath ?: "Not attached").apply { foreground = Palette.secondaryText })

        section("Folders")
        actions(
            button("Open data folder", UiPreferences.dataDir.absolutePath) { open(UiPreferences.dataDir) },
            button("Open scripts folder", getScriptsDirectory().absolutePath) { open(getScriptsDirectory()) },
            button("Open logs folder", File(UiPreferences.dataDir, "logs").absolutePath) { open(File(UiPreferences.dataDir, "logs")) },
        )

        section("About")
        field("Client", label("Darkan client build ${Loader.MAJOR_BUILD}.${Loader.MINOR_BUILD}", secondary = true))
        field("Build", label(ClientBuild.describe(), secondary = true))
        field("Scripts", label("${ScriptExecutor.scripts.size} loaded", secondary = true))
        actions(
            button("Report a bug…", "Copy a short report (version, system, recent console output) and open Discord") {
                val report = CrashHandler.userReport("Report a bug button in the bot sidebar")
                CrashDialog.show(report, report.save(), CrashDialog.Mode.REPORT)
            },
        )

        form.add(JPanel().apply { isOpaque = false }, GridBagConstraints().apply {
            gridx = 0; gridy = row; gridwidth = 2; weightx = 1.0; weighty = 1.0; fill = GridBagConstraints.BOTH
        })
        add(JScrollPane(form).tidy().apply { horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER }, BorderLayout.CENTER)
    }

    private fun section(title: String) {
        form.add(sectionHeader(title), GridBagConstraints().apply {
            gridx = 0; gridy = row++; gridwidth = 2; weightx = 1.0
            anchor = GridBagConstraints.WEST; fill = GridBagConstraints.HORIZONTAL
            insets = Insets(if (row == 1) 0 else Spacing.PANEL, 0, 2, 0)
        })
    }

    private fun field(name: String, component: JComponent, tooltip: String? = null) {
        component.toolTipText = tooltip ?: component.toolTipText
        form.add(label(name), GridBagConstraints().apply {
            gridx = 0; gridy = row; anchor = GridBagConstraints.NORTHWEST
            insets = Insets(4, 0, 4, Spacing.PANEL)
        })
        form.add(component, GridBagConstraints().apply {
            gridx = 1; gridy = row++; weightx = 1.0; anchor = GridBagConstraints.WEST
            fill = if (component is WrappingText || component is javax.swing.JLabel) GridBagConstraints.HORIZONTAL else GridBagConstraints.NONE
            insets = Insets(2, 0, 2, 0)
        })
    }

    private fun actions(vararg buttons: JComponent) {
        form.add(toolbarRow(*buttons), GridBagConstraints().apply {
            gridx = 0; gridy = row++; gridwidth = 2; weightx = 1.0
            anchor = GridBagConstraints.WEST; fill = GridBagConstraints.HORIZONTAL
            insets = Insets(2, -Spacing.GAP, 2, 0)
        })
    }

    private fun open(dir: File) {
        try {
            dir.mkdirs()
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir) else println("Folder: ${dir.absolutePath}")
        } catch (e: Exception) {
            println("Folder: ${dir.absolutePath}")
        }
    }
}
