package com.darkan.bot

import com.darkan.bot.scripts.ConfigurableScript
import com.darkan.bot.scripts.Script
import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.ScriptMetadata
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ScriptSelectionPanel : JPanel(BorderLayout()) {
    private val searchField = JTextField(20)
    private val statusFilter = JComboBox(arrayOf("All", "Running", "Stopped"))
    private val runtimeLabels = mutableMapOf<Class<*>, JLabel>()
    private val configuredInstances = mutableMapOf<Class<*>, Any>()
    private val favoritesFile = File(System.getProperty("user.home"), ".cheddarcheese/script-favorites.txt")
    private val favorites = mutableSetOf<String>()

    private val debounceTimer = Timer(300) { updateFilteredCards() }.apply { isRepeats = false }

    private val scriptCardsPanel = object : JPanel() {
        override fun getPreferredSize(): Dimension {
            val parentWidth = parent?.width ?: 600
            return Dimension(parentWidth, super.getPreferredSize().height)
        }
    }.apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    private val scriptCardsScrollPane = JScrollPane(scriptCardsPanel).apply {
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBar.unitIncrement = 24
        scriptCardsPanel.isDoubleBuffered = true
        border = EmptyBorder(0, 0, 0, 0)
        viewportBorder = null
    }

    override fun getPreferredSize(): Dimension {
        val parentWidth = parent?.width ?: 600
        return Dimension(parentWidth, super.getPreferredSize().height)
    }

    init {
        try {
            loadFavorites()

            border = EmptyBorder(10, 0, 10, 0)

            val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Search:"))
                add(searchField)
                add(JLabel("Status:"))
                add(statusFilter)
                add(JButton("Stop All").apply {
                    addActionListener {
                        ScriptExecutor.scripts.values.forEach {
                            if (ScriptExecutor.isScriptRunning(it.scriptClass)) {
                                ScriptExecutor.deactivate(it.scriptClass)
                            }
                        }
                        updateFilteredCards()
                    }
                })
            }

            add(filterPanel, BorderLayout.NORTH)
            add(scriptCardsScrollPane, BorderLayout.CENTER)

            val runtimeTimer = Timer(1000) {
                for ((clazz, label) in runtimeLabels) {
                    if (ScriptExecutor.isScriptRunning(clazz)) {
                        ScriptExecutor.getScriptRuntimeFormatted(clazz)?.let { formatted ->
                            label.text = "Runtime: $formatted"
                        }
                    }
                }
            }
            runtimeTimer.start()

            searchField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = debounceTimer.restart()
                override fun removeUpdate(e: DocumentEvent?) = debounceTimer.restart()
                override fun changedUpdate(e: DocumentEvent?) = debounceTimer.restart()
            })

            statusFilter.addActionListener { debounceTimer.restart() }
        } catch (e: Exception) {
            println("ScriptSelectionPanel failed to initialize:")
            e.printStackTrace()
        }
    }

    private fun loadFavorites() {
        try {
            if (favoritesFile.exists()) {
                favorites.clear()
                favorites.addAll(favoritesFile.readLines().filter { it.isNotBlank() })
            }
        } catch (e: Exception) {
            println("Failed to load favorites: ${e.message}")
        }
    }

    private fun saveFavorites() {
        try {
            favoritesFile.parentFile?.mkdirs()
            favoritesFile.writeText(favorites.joinToString("\n"))
        } catch (e: Exception) {
            println("Failed to save favorites: ${e.message}")
        }
    }

    private fun toggleFavorite(scriptClass: Class<*>) {
        val scriptName = scriptClass.simpleName
        if (favorites.contains(scriptName)) {
            favorites.remove(scriptName)
        } else {
            favorites.add(scriptName)
        }
        saveFavorites()
        updateFilteredCards()
    }

    private fun isFavorite(scriptClass: Class<*>): Boolean {
        return favorites.contains(scriptClass.simpleName)
    }

    private fun updateFilteredCards() {
        val query = searchField.text.trim().lowercase()
        val selectedStatus = statusFilter.selectedItem as String

        scriptCardsPanel.removeAll()

        val scripts = ScriptExecutor.scripts.values
            .sortedWith(
                compareByDescending<ScriptMetadata> { isFavorite(it.scriptClass) }
                .thenByDescending { ScriptExecutor.isScriptRunning(it.scriptClass) }
                .thenBy { it.name.lowercase() }
            )

        for (script in scripts) {
            val isRunning = ScriptExecutor.isScriptRunning(script.scriptClass)

            val matchesQuery = query.isBlank() ||
                    script.name.lowercase().contains(query) ||
                    script.author.lowercase().contains(query)

            val matchesStatus = when (selectedStatus) {
                "All" -> true
                "Running" -> isRunning
                "Stopped" -> !isRunning
                else -> true
            }

            if (matchesQuery && matchesStatus) {
                val cardWrapper = JPanel(BorderLayout()).apply {
                    maximumSize = Dimension(scriptCardsScrollPane.viewport.width - 20, Int.MAX_VALUE)
                    preferredSize = Dimension(scriptCardsScrollPane.viewport.width - 20, 100)
                    isOpaque = false
                    add(createScriptCard(script, isRunning), BorderLayout.CENTER)
                }
                scriptCardsPanel.add(cardWrapper)
                scriptCardsPanel.add(Box.createVerticalStrut(6))
            }
        }

        scriptCardsPanel.revalidate()
        scriptCardsPanel.repaint()
    }

    private fun createScriptCard(metadata: ScriptMetadata, isRunning: Boolean): JPanel {
        val card = JPanel().apply {
            layout = BorderLayout()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                EmptyBorder(5, 5, 5, 5)
            )
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }

        val titleLabel = JLabel("${metadata.name} v${metadata.version}").apply {
            font = font.deriveFont(Font.BOLD, 14f)
        }

        val starButton = JButton().apply {
            val isFav = isFavorite(metadata.scriptClass)
            text = if (isFav) "★" else "☆"
            foreground = if (isFav) Color.ORANGE else Color.GRAY
            isOpaque = false
            isBorderPainted = false
            isContentAreaFilled = false
            border = BorderFactory.createEmptyBorder(2, 6, 2, 6)
            preferredSize = Dimension(30, 25)
            toolTipText = if (isFav) "Remove from favorites" else "Add to favorites"
            addActionListener {
                toggleFavorite(metadata.scriptClass)
            }
        }

        val descriptionLabel = JLabel("<html>${metadata.description}</html>")

        val authorLabel = JLabel("Author: ${metadata.author}").apply {
            font = font.deriveFont(11f)
        }

        val statusLabel = JLabel("Status: ${if (isRunning) "Running" else "Stopped"}").apply {
            foreground = if (isRunning) Color(0, 180, 0) else Color(200, 0, 0)
            font = font.deriveFont(11f)
        }

        val runtimeLabel = JLabel().apply {
            foreground = Color.GRAY
            text = if (isRunning) {
                ScriptExecutor.getScriptRuntimeFormatted(metadata.scriptClass) ?: "Runtime: --:--:--"
            } else {
                "Runtime: --:--:--"
            }
        }

        runtimeLabels[metadata.scriptClass] = runtimeLabel

        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(titleLabel)
            add(descriptionLabel)
            add(authorLabel)
            add(statusLabel)
            add(runtimeLabel)
        }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            preferredSize = Dimension(100, 60)

            add(starButton)
            add(Box.createVerticalStrut(5))

            val startStopButton = JButton(if (isRunning) "Stop" else "Start").apply {
                background = if (isRunning) Color(220, 50, 50) else Color(60, 160, 75)
                foreground = Color.WHITE
                maximumSize = Dimension(100, 30)
                alignmentX = Component.CENTER_ALIGNMENT
                addActionListener {
                    try {
                        if (isRunning) {
                            ScriptExecutor.deactivate(metadata.scriptClass)
                        } else {
                            val instance = configuredInstances.remove(metadata.scriptClass)
                                ?: metadata.scriptClass.getDeclaredConstructor().newInstance()

                            ScriptExecutor.activate(instance as Script)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    SwingUtilities.invokeLater { updateFilteredCards() }
                }
            }

            val settingsButton = if (ConfigurableScript::class.java.isAssignableFrom(metadata.scriptClass)) {
                JButton("Settings").apply {
                    background = Color(100, 100, 255)
                    foreground = Color.WHITE
                    maximumSize = Dimension(100, 30)
                    alignmentX = Component.CENTER_ALIGNMENT
                    toolTipText = "Configure this script's settings prior to running."
                    addActionListener {
                        try {
                            val instance = ScriptExecutor.getScriptInstance(metadata.scriptClass)
                                ?: configuredInstances.getOrPut(metadata.scriptClass) {
                                    metadata.scriptClass.getDeclaredConstructor().newInstance()
                                }
                            UI.addConfigTab(metadata, instance)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else null

            add(startStopButton)
            if (settingsButton != null) {
                add(Box.createVerticalStrut(5))
                add(settingsButton)
            }
        }

        card.add(infoPanel, BorderLayout.CENTER)
        card.add(buttonPanel, BorderLayout.EAST)

        return card
    }

    override fun addNotify() {
        super.addNotify()
        SwingUtilities.invokeLater { updateFilteredCards() }
    }
}
