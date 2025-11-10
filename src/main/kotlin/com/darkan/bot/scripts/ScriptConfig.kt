package com.darkan.bot.scripts

import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout

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
}

// Marker interface for scripts that have configuration
interface ConfigurableScript

class ScriptConfigWrapper(private val script: Any) : ScriptConfig {
    private val contentPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = EmptyBorder(8, 10, 8, 10)

        // Find and process ConfigItem fields
        script::class.java.declaredFields
            .filter { ConfigItem::class.java.isAssignableFrom(it.type) }
            .forEach { field ->
                field.isAccessible = true
                val configItem = field.get(script) as? ConfigItem<*>
                if (configItem != null) {
                    // Create a panel with balanced spacing
                    val panel = JPanel().apply {
                        layout = FlowLayout(FlowLayout.LEFT, 5, 2)  // Added small vertical gap
                        maximumSize = Dimension(Int.MAX_VALUE, 30)  // Increased height a bit
                        add(JLabel(configItem.name + ": "))
                        add(configItem.createComponent())
                    }
                    add(panel)
                    add(Box.createVerticalStrut(3))  // Small gap between items
                }
            }
    }

    override val configPanel = JScrollPane(contentPanel).apply {
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        border = null
    }

    override fun updateConfig() {
        // No need to update since values are bound directly
    }
}

class BooleanConfigItem(
    override val name: String,
    override val description: String,
    initialValue: Boolean = false
) : ConfigItem<Boolean> {
    override var value = initialValue

    override fun createComponent(): Component {
        return JCheckBox().apply {
            toolTipText = description
            isSelected = value
            addActionListener { value = isSelected }
        }
    }
}

class IntConfigItem(
    override val name: String,
    override val description: String,
    initialValue: Int = 0,
    private val min: Int = Int.MIN_VALUE,
    private val max: Int = Int.MAX_VALUE
) : ConfigItem<Int> {
    override var value = initialValue

    override fun createComponent(): Component {
        return JSpinner(SpinnerNumberModel(value, min, max, 1)).apply {
            toolTipText = description
            addChangeListener { value = this.value as Int }
        }
    }
}

class StringConfigItem(
    override val name: String,
    override val description: String,
    initialValue: String = ""
) : ConfigItem<String> {
    override var value = initialValue

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
    initialValue: T
) : ConfigItem<T> {
    override var value = initialValue

    override fun createComponent(): Component {
        return JComboBox(options).apply {
            toolTipText = description
            selectedItem = value
            addActionListener { value = selectedItem as T }
        }
    }
}

class EnumConfigItem<T : Enum<T>>(
    override val name: String,
    override val description: String,
    private val enumValues: Array<T>,
    initialValue: T
) : ConfigItem<T> {
    override var value = initialValue

    override fun createComponent(): Component {
        return JComboBox(enumValues).apply {
            toolTipText = description
            selectedItem = value
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