package com.darkan.bot.scripts

import com.darkan.bot.ui.Palette
import com.darkan.bot.ui.ScrollableColumn
import com.darkan.bot.ui.Spacing
import com.darkan.bot.ui.WrappingText
import com.darkan.bot.ui.button
import com.darkan.bot.ui.label
import com.darkan.bot.ui.tidy
import com.darkan.bot.ui.toolbarRow
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// Base interface for script configuration
interface ScriptConfig {
    val configPanel: JScrollPane
    fun updateConfig()
}

// Interface for config items
interface ConfigItem<T> {
    val name: String
    val description: String
    var value: T
    fun createComponent(): Component

    /** Restores the value the item was declared with. Built-in items implement this; custom ones may leave it a no-op. */
    fun reset() {}
}

// Marker interface for scripts that have configuration
interface ConfigurableScript

/**
 * Builds the settings form for a script from its [ConfigItem] fields: one
 * labelled row per item with the description underneath, laid out in a single
 * column so it stays readable at any sidebar width.
 */
class ScriptConfigWrapper(val script: Any) : ScriptConfig {
    private val items: List<ConfigItem<*>> = script::class.java.declaredFields
        .filter { ConfigItem::class.java.isAssignableFrom(it.type) }
        .mapNotNull { field ->
            field.isAccessible = true
            field.get(script) as? ConfigItem<*>
        }

    private val form = JPanel(GridBagLayout()).apply { isOpaque = false }

    private val content = ScrollableColumn(BorderLayout(0, Spacing.GAP)).apply {
        border = Spacing.panelBorder()
        add(JPanel(BorderLayout(0, 2)).apply {
            isOpaque = false
            add(toolbarRow(button("Reset to defaults", "Put every setting back to the value the script declares") { resetAll() }), BorderLayout.NORTH)
            add(WrappingText("Changes apply immediately, even while the script is running.").apply {
                foreground = Palette.secondaryText
                font = font.deriveFont(11f)
            }, BorderLayout.CENTER)
        }, BorderLayout.NORTH)
        add(form, BorderLayout.CENTER)
    }

    override val configPanel: JScrollPane = JScrollPane(content).tidy().apply {
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    init {
        rebuild()
    }

    override fun updateConfig() {
        // Values are bound directly to the components; nothing to push.
    }

    private fun resetAll() {
        items.forEach { it.reset() }
        rebuild()
    }

    private fun rebuild() {
        form.removeAll()
        var row = 0
        if (items.isEmpty()) {
            form.add(label("This script has no settings.", secondary = true), GridBagConstraints().apply {
                gridx = 0; gridy = row++; weightx = 1.0; anchor = GridBagConstraints.WEST
            })
        }
        for (item in items) {
            val component = item.createComponent()
            (component as? JComponent)?.toolTipText = item.description

            form.add(label(item.name, bold = true), GridBagConstraints().apply {
                gridx = 0; gridy = row++; weightx = 1.0; anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(Spacing.GAP, 0, 2, 0)
            })
            form.add(component, GridBagConstraints().apply {
                gridx = 0; gridy = row++; weightx = 1.0; anchor = GridBagConstraints.WEST
                fill = if (component is JTextField) GridBagConstraints.HORIZONTAL else GridBagConstraints.NONE
                insets = Insets(0, 0, 2, 0)
            })
            if (item.description.isNotBlank()) {
                form.add(WrappingText(item.description).apply {
                    foreground = Palette.secondaryText
                    font = font.deriveFont(11f)
                }, GridBagConstraints().apply {
                    gridx = 0; gridy = row++; weightx = 1.0; anchor = GridBagConstraints.WEST
                    fill = GridBagConstraints.HORIZONTAL
                    insets = Insets(0, 0, 2, 0)
                })
            }
            form.add(JPanel().apply {
                isOpaque = false
                border = BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.border)
                preferredSize = java.awt.Dimension(0, 1)
            }, GridBagConstraints().apply {
                gridx = 0; gridy = row++; weightx = 1.0; fill = GridBagConstraints.HORIZONTAL
                insets = Insets(Spacing.GAP, 0, 0, 0)
            })
        }
        form.add(JPanel().apply { isOpaque = false }, GridBagConstraints().apply {
            gridx = 0; gridy = row; weightx = 1.0; weighty = 1.0; fill = GridBagConstraints.BOTH
        })
        form.revalidate()
        form.repaint()
    }
}

class BooleanConfigItem(
    override val name: String,
    override val description: String,
    private val initialValue: Boolean = false
) : ConfigItem<Boolean> {
    override var value = initialValue

    override fun reset() {
        value = initialValue
    }

    override fun createComponent(): Component {
        return JCheckBox("Enabled").apply {
            toolTipText = description
            isSelected = value
            addActionListener { value = isSelected }
        }
    }
}

class IntConfigItem(
    override val name: String,
    override val description: String,
    private val initialValue: Int = 0,
    private val min: Int = Int.MIN_VALUE,
    private val max: Int = Int.MAX_VALUE
) : ConfigItem<Int> {
    override var value = initialValue

    override fun reset() {
        value = initialValue
    }

    override fun createComponent(): Component {
        return JSpinner(SpinnerNumberModel(value, min, max, 1)).apply {
            toolTipText = description
            (editor as? JSpinner.DefaultEditor)?.textField?.columns = 8
            addChangeListener { value = this.value as Int }
        }
    }
}

class StringConfigItem(
    override val name: String,
    override val description: String,
    private val initialValue: String = ""
) : ConfigItem<String> {
    override var value = initialValue

    override fun reset() {
        value = initialValue
    }

    override fun createComponent(): Component {
        return JTextField(value, 20).apply {
            toolTipText = description
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) { value = text }
                override fun removeUpdate(e: DocumentEvent) { value = text }
                override fun changedUpdate(e: DocumentEvent) { value = text }
            })
        }
    }
}

class OptionsConfigItem<T>(
    override val name: String,
    override val description: String,
    private val options: Array<T>,
    private val initialValue: T
) : ConfigItem<T> {
    override var value = initialValue

    override fun reset() {
        value = initialValue
    }

    override fun createComponent(): Component {
        return JComboBox(options).apply {
            toolTipText = description
            selectedItem = value
            @Suppress("UNCHECKED_CAST")
            addActionListener { value = selectedItem as T }
        }
    }
}

class EnumConfigItem<T : Enum<T>>(
    override val name: String,
    override val description: String,
    private val enumValues: Array<T>,
    private val initialValue: T
) : ConfigItem<T> {
    override var value = initialValue

    override fun reset() {
        value = initialValue
    }

    override fun createComponent(): Component {
        return JComboBox(enumValues).apply {
            toolTipText = description
            selectedItem = value
            @Suppress("UNCHECKED_CAST")
            addActionListener { value = selectedItem as T }
        }
    }
}

/* Examples
     // Boolean examples
    private val enableDebug = BooleanConfigItem(
        name = "Enable Debug",
        description = "Enables debug output",
        initialValue = true
    )

    private val showTimestamp = BooleanConfigItem(
        name = "Show Timestamp",
        description = "Shows timestamp in output",
        initialValue = false
    )

    // String examples
    private val debugPrefix = StringConfigItem(
        name = "Debug Prefix",
        description = "Prefix for debug messages",
        initialValue = "[DEBUG]"
    )

    private val filterText = StringConfigItem(
        name = "Filter Text",
        description = "Text to filter debug messages",
        initialValue = ""
    )

    // String options example
    private val debugModes = arrayOf("All", "Chat Only", "XP Only", "Varbit Only", "None")
    private val debugMode = OptionsConfigItem(
        name = "Debug Mode",
        description = "What type of debug info to show",
        options = debugModes,
        initialValue = "All"
    )

    // Int options example
    private val radiusOptions = arrayOf(5, 10, 15, 20, 25, 30)
    private val debugRadius = OptionsConfigItem(
        name = "Debug Radius",
        description = "Radius for debug area",
        options = radiusOptions,
        initialValue = 10
    )

    // More examples with common game values
    private val skillLevels = (1..99).toList().toTypedArray()
    private val minLevel = OptionsConfigItem(
        name = "Minimum Level",
        description = "Minimum level to start at",
        options = skillLevels,
        initialValue = 1
    )

    private val intervals = arrayOf(100, 500, 1000, 2000, 5000)
    private val updateInterval = OptionsConfigItem(
        name = "Update Interval",
        description = "How often to update (ms)",
        options = intervals,
        initialValue = 1000
    )
 */
