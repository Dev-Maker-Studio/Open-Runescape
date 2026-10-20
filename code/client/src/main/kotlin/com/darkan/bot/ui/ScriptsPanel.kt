package com.darkan.bot.ui

import com.darkan.bot.scripts.ConfigItem
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptConfigStore
import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.ScriptLoadException
import com.darkan.bot.scripts.ScriptMetadata
import com.darkan.bot.scripts.ScriptRepository
import com.darkan.bot.scripts.getScriptsDirectory
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.io.File
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

object ScriptFavorites {
    private val file = File(UiPreferences.dataDir, "script-favorites.txt")
    private val names = mutableSetOf<String>()

    init {
        try {
            if (file.exists()) names.addAll(file.readLines().filter { it.isNotBlank() })
        } catch (e: Exception) {
            println("Failed to load favorites: ${e.message}")
        }
    }

    fun contains(name: String) = name in names

    fun toggle(name: String) {
        if (!names.remove(name)) names.add(name)
        try {
            file.parentFile?.mkdirs()
            file.writeText(names.joinToString("\n"))
        } catch (e: Exception) {
            println("Failed to save favorites: ${e.message}")
        }
    }
}

object ScriptInstances {
    private val configured = HashMap<Class<*>, Script>()

    fun current(metadata: ScriptMetadata): Script {
        val scriptClass = metadata.load()
        return ScriptExecutor.getScriptInstance(scriptClass)
            ?: configured.getOrPut(scriptClass) { instantiate(scriptClass) }
    }

    fun startable(metadata: ScriptMetadata): Script {
        val scriptClass = metadata.load()
        val existing = configured[scriptClass]
        if (existing != null && !existing.stopped && !existing.started) return existing
        val fresh = instantiate(scriptClass)
        if (existing != null) copyConfig(existing, fresh)
        configured[scriptClass] = fresh
        return fresh
    }

    private fun instantiate(scriptClass: Class<out Script>): Script =
        scriptClass.getDeclaredConstructor().also { it.isAccessible = true }.newInstance()
            .also { ScriptConfigStore.applyTo(it) }

    @Suppress("UNCHECKED_CAST")
    fun copyConfig(from: Any, to: Any) {
        from::class.java.declaredFields
            .filter { ConfigItem::class.java.isAssignableFrom(it.type) }
            .forEach { field ->
                field.isAccessible = true
                val source = field.get(from) as? ConfigItem<Any?> ?: return@forEach
                val target = field.get(to) as? ConfigItem<Any?> ?: return@forEach
                target.value = source.value
            }
    }
}

class ScriptsPanel(
    private val openSettings: (ScriptMetadata, Script) -> Unit,
    private val onInstanceReplaced: (ScriptMetadata, Script) -> Unit = { _, _ -> },
) : JPanel(BorderLayout()) {
    private val search = searchField("Search by name or author", 14)
    private val categoryFilter = JComboBox<String>()
    private val statusFilter = JComboBox(arrayOf("All", "Running", "Stopped"))
    private val stopAllButton: JButton
    private val updateButton: JButton
    private val list = VerticalListPanel().apply { isOpaque = false }
    private val emptyState = EmptyState("No scripts found")
    private val centerLayout = CardLayout()
    private val center = JPanel(centerLayout)
    private val cards = LinkedHashMap<String, ScriptCard>()
    private val compiling = HashSet<String>()
    private val rebuildDebounce = Timer(200) { rebuild() }.apply { isRepeats = false }
    private var lastRunningSet: Set<String> = emptySet()
    private var restoringFilters = false

    val runningCount: Int
        get() = ScriptExecutor.scripts.values.count { it.running }

    init {
        border = Spacing.panelBorder()

        stopAllButton = button("Stop all", "Stop every running script") {
            ScriptExecutor.scripts.values
                .mapNotNull { it.runningClass() }
                .forEach { ScriptExecutor.deactivate(it) }
            refreshCards()
        }

        updateButton = button("Update scripts", "Fetch the latest scripts from GitLab, then reload") { updateScripts() }

        val filters = JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(headerRow(search).apply { alignmentX = LEFT_ALIGNMENT })
            add(Box.createVerticalStrut(Spacing.GAP))
            add(toolbarRow(
                label("Category", secondary = true), categoryFilter,
                label("Status", secondary = true), statusFilter,
            ).apply { alignmentX = LEFT_ALIGNMENT })
            add(toolbarRow(
                button("Reload scripts", "Reload every script from disk, including .kts files") { reloadScripts() },
                updateButton,
                stopAllButton,
                button("Open scripts folder", "Open the scripts folder in your file manager") { openScriptsFolder() },
            ).apply { alignmentX = LEFT_ALIGNMENT })
        }
        add(filters, BorderLayout.NORTH)

        center.isOpaque = false
        center.add(JScrollPane(list).tidy().apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            viewport.isOpaque = false
            isOpaque = false
        }, "list")
        center.add(emptyState, "empty")
        add(center, BorderLayout.CENTER)

        search.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = rebuildDebounce.restart()
            override fun removeUpdate(e: DocumentEvent) = rebuildDebounce.restart()
            override fun changedUpdate(e: DocumentEvent) = rebuildDebounce.restart()
        })
        categoryFilter.addActionListener { if (!restoringFilters) { persistFilters(); rebuildDebounce.restart() } }
        statusFilter.addActionListener { if (!restoringFilters) { persistFilters(); rebuildDebounce.restart() } }

        populateCategories()
        restoreFilters()
        rebuild()

        ScriptExecutor.addReloadListener {
            SwingUtilities.invokeLater {
                populateCategories()
                rebuild()
            }
        }
        Timer(1000) { refreshCards() }.start()
    }

    private fun populateCategories() {
        restoringFilters = true
        try {
            val selected = categoryFilter.selectedItem as? String
            categoryFilter.removeAllItems()
            categoryFilter.addItem(ALL_CATEGORIES)
            ScriptExecutor.scripts.values
                .map { it.category }
                .distinct()
                .sortedBy { it.readableName }
                .forEach { categoryFilter.addItem(it.readableName) }
            categoryFilter.selectedItem = selected?.takeIf { hasItem(categoryFilter, it) } ?: ALL_CATEGORIES
        } finally {
            restoringFilters = false
        }
    }

    private fun hasItem(combo: JComboBox<String>, item: String) = (0 until combo.itemCount).any { combo.getItemAt(it) == item }

    private fun restoreFilters() {
        restoringFilters = true
        try {
            UiPreferences.getString("scripts.category")?.let { if (hasItem(categoryFilter, it)) categoryFilter.selectedItem = it }
            UiPreferences.getString("scripts.status")?.let { if (hasItem(statusFilter, it)) statusFilter.selectedItem = it }
        } finally {
            restoringFilters = false
        }
    }

    private fun persistFilters() {
        UiPreferences.put("scripts.category", categoryFilter.selectedItem)
        UiPreferences.put("scripts.status", statusFilter.selectedItem)
    }

    private fun reloadScripts() {
        try {
            println("Reloading scripts...")
            ScriptExecutor.loadScripts()
            populateCategories()
            rebuild()
            JOptionPane.showMessageDialog(
                this, "Loaded ${ScriptExecutor.scripts.size} script(s).", "Scripts reloaded", JOptionPane.INFORMATION_MESSAGE
            )
        } catch (e: Exception) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(this, "Failed to reload scripts:\n${e.message}", "Reload failed", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun updateScripts() {
        updateButton.isEnabled = false
        rebuild()
        ScriptRepository.syncAsync { outcome ->
            println(outcome.message)
            ScriptExecutor.loadScripts()
            SwingUtilities.invokeLater {
                updateButton.isEnabled = true
                JOptionPane.showMessageDialog(
                    this, outcome.message, "Update scripts",
                    if (outcome.ok) JOptionPane.INFORMATION_MESSAGE else JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun openScriptsFolder() {
        try {
            val dir = getScriptsDirectory()
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir)
            else JOptionPane.showMessageDialog(this, "Scripts folder:\n${dir.absolutePath}", "Scripts folder", JOptionPane.INFORMATION_MESSAGE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun visibleScripts(): List<ScriptMetadata> {
        val query = search.text.trim().lowercase()
        val category = categoryFilter.selectedItem as? String ?: ALL_CATEGORIES
        val status = statusFilter.selectedItem as? String ?: "All"
        return ScriptExecutor.scripts.values
            .filter { script ->
                val running = script.running
                (query.isBlank() || script.name.lowercase().contains(query) || script.author.lowercase().contains(query)) &&
                    (category == ALL_CATEGORIES || script.category.readableName == category) &&
                    when (status) {
                        "Running" -> running
                        "Stopped" -> !running
                        else -> true
                    }
            }
            .sortedWith(
                compareByDescending<ScriptMetadata> { ScriptFavorites.contains(it.className) }
                    .thenByDescending { it.running }
                    .thenBy { it.name.lowercase() }
            )
    }

    fun rebuild() {
        val scripts = visibleScripts()
        list.removeAll()
        cards.clear()
        scripts.forEachIndexed { i, metadata ->
            val card = ScriptCard(metadata)
            cards[metadata.id] = card
            if (i > 0) list.add(Box.createVerticalStrut(Spacing.GAP))
            list.add(card)
        }
        list.add(Box.createVerticalGlue())
        lastRunningSet = runningClasses()

        if (scripts.isEmpty()) {
            emptyState.message = when {
                ScriptRepository.syncing -> "Updating scripts from GitLab..."
                ScriptExecutor.scripts.isEmpty() ->
                    "<html><center>No scripts loaded.<br>Press Update scripts to fetch the public ones,<br>or drop .kts or .jar files into<br>${getScriptsDirectory().absolutePath}<br>and press Reload scripts.</center></html>"
                else -> "No scripts match the current filters."
            }
            centerLayout.show(center, "empty")
        } else {
            centerLayout.show(center, "list")
        }
        list.revalidate()
        list.repaint()
    }

    private fun runningClasses(): Set<String> =
        ScriptExecutor.scripts.values.filter { it.running }.map { it.id }.toSet()

    private fun refreshCards() {
        val running = runningClasses()
        if (running != lastRunningSet) {
            lastRunningSet = running
            rebuild()
            return
        }
        cards.values.forEach { it.refresh() }
        stopAllButton.isEnabled = running.isNotEmpty()
        stopAllButton.text = if (running.isEmpty()) "Stop all" else "Stop all (${running.size})"
    }

    private inner class ScriptCard(private val metadata: ScriptMetadata) : CardPanel() {
        private val starButton = flatButton("☆", null) { ScriptFavorites.toggle(metadata.className); rebuild() }
        private val statusLabel = label("", size = 11f)
        private val toggleButton = JButton().apply {
            preferredSize = Dimension(96, 28)
            addActionListener { toggle() }
        }
        private val settingsButton: JButton? =
            if (metadata.configurable) {
                JButton("Settings").apply {
                    preferredSize = Dimension(96, 28)
                    toolTipText = "Configure this script"
                    addActionListener {
                        whenCompiled {
                            try {
                                openSettings(metadata, ScriptInstances.current(metadata))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } else null

        init {
            layout = BorderLayout(Spacing.GAP, 0)
            border = cardBorder()
            background = Palette.cardBackground
            isOpaque = true

            starButton.font = starButton.font.deriveFont(16f)
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                add(starButton, BorderLayout.NORTH)
            }, BorderLayout.WEST)

            val info = JPanel().apply {
                isOpaque = false
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0)).apply {
                    isOpaque = false
                    alignmentX = LEFT_ALIGNMENT
                    add(label(metadata.name, bold = true, size = 14f))
                    add(label("v${metadata.version}", secondary = true, size = 11f))
                })
                add(label("by ${metadata.author}  ·  ${metadata.category.readableName}", secondary = true, size = 11f).apply {
                    alignmentX = LEFT_ALIGNMENT
                })
                if (metadata.description.isNotBlank()) {
                    add(Box.createVerticalStrut(4))
                    add(WrappingText(metadata.description).apply {
                        alignmentX = LEFT_ALIGNMENT
                        foreground = Palette.text
                    })
                }
                add(Box.createVerticalStrut(4))
                add(statusLabel.apply { alignmentX = LEFT_ALIGNMENT })
            }
            add(info, BorderLayout.CENTER)

            val actions = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(JPanel(GridLayout(0, 1, 0, 4)).apply {
                    isOpaque = false
                    add(toggleButton)
                    settingsButton?.let { add(it) }
                }, BorderLayout.NORTH)
            }
            add(actions, BorderLayout.EAST)

            refresh()
        }

        private fun toggle() {
            val running = metadata.runningClass()
            if (running != null) {
                ScriptExecutor.deactivate(running)
                SwingUtilities.invokeLater { refreshCards() }
                return
            }
            whenCompiled {
                try {
                    val before = ScriptInstances.current(metadata)
                    val instance = ScriptInstances.startable(metadata)
                    ScriptExecutor.activate(instance)
                    if (instance !== before) onInstanceReplaced(metadata, instance)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                refreshCards()
            }
        }

        private fun whenCompiled(action: () -> Unit) {
            if (metadata.loaded) {
                action()
                return
            }
            if (!compiling.add(metadata.id)) return
            refresh()
            Thread({
                val failure = runCatching { metadata.load() }.exceptionOrNull()
                SwingUtilities.invokeLater {
                    compiling.remove(metadata.id)
                    if (failure == null) {
                        action()
                    } else {
                        if (failure !is ScriptLoadException) failure.printStackTrace()
                        JOptionPane.showMessageDialog(
                            this@ScriptsPanel,
                            "${metadata.name} failed to compile:\n${failure.message}",
                            "Script error",
                            JOptionPane.ERROR_MESSAGE,
                        )
                    }
                    rebuild()
                }
            }, "script-compile-${metadata.className}").apply { isDaemon = true }.start()
        }

        fun refresh() {
            val running = metadata.running
            val building = metadata.id in compiling
            val favourite = ScriptFavorites.contains(metadata.className)

            starButton.text = if (favourite) "★" else "☆"
            starButton.foreground = if (favourite) Palette.favourite else Palette.secondaryText
            starButton.toolTipText = if (favourite) "Remove from favourites" else "Add to favourites"

            toggleButton.text = if (running) "Stop" else "Start"
            toggleButton.toolTipText = if (running) "Stop ${metadata.name}" else "Start ${metadata.name}"
            toggleButton.background = if (running) Palette.dangerFill else Palette.successFill
            toggleButton.foreground = java.awt.Color.WHITE
            toggleButton.isEnabled = !building
            settingsButton?.isEnabled = !building

            if (running) {
                val runtime = metadata.runningClass()?.let { ScriptExecutor.getScriptRuntimeFormatted(it) } ?: "00:00:00"
                statusLabel.text = "●  Running  $runtime"
                statusLabel.foreground = Palette.success
                statusLabel.font = statusLabel.font.deriveFont(Font.BOLD)
            } else if (building) {
                statusLabel.text = "◌  Compiling..."
                statusLabel.foreground = Palette.secondaryText
                statusLabel.font = statusLabel.font.deriveFont(Font.ITALIC)
            } else {
                statusLabel.text = "○  Stopped"
                statusLabel.foreground = Palette.secondaryText
                statusLabel.font = statusLabel.font.deriveFont(Font.PLAIN)
            }
        }
    }

    private companion object {
        const val ALL_CATEGORIES = "All categories"
    }
}
