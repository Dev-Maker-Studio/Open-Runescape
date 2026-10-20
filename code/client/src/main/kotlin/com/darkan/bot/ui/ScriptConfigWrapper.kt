package com.darkan.bot.ui

import com.darkan.bot.scripts.ActionableConfig
import com.darkan.bot.scripts.BooleanConfigItem
import com.darkan.bot.scripts.ConfigItem
import com.darkan.bot.scripts.ConfigSection
import com.darkan.bot.scripts.ConfigVisibilityProvider
import com.darkan.bot.scripts.EnumConfigItem
import com.darkan.bot.scripts.InfoDisplayConfigItem
import com.darkan.bot.scripts.IntConfigItem
import com.darkan.bot.scripts.OptionsConfigItem
import com.darkan.bot.scripts.ScriptConfigStore
import com.darkan.bot.scripts.StringConfigItem
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
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

interface ScriptConfig {
    val configPanel: JScrollPane
    fun updateConfig()
}

class ScriptConfigWrapper(val script: Any) : ScriptConfig {
    private val entries: List<Pair<String, ConfigItem<*>>> = script::class.java.declaredFields
        .filter { ConfigItem::class.java.isAssignableFrom(it.type) }
        .mapNotNull { field ->
            field.isAccessible = true
            (field.get(script) as? ConfigItem<*>)?.let { field.name to it }
        }

    private val openSections = HashMap<String, Boolean>()
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
    }

    private fun onChanged(rebuildForm: Boolean = false) {
        ScriptConfigStore.save(script)
        invokeOnConfigUpdated(script)
        if (rebuildForm || script is ConfigVisibilityProvider) rebuild()
    }

    private fun resetAll() {
        entries.forEach { (_, item) -> item.reset() }
        onChanged(rebuildForm = true)
    }

    private fun visibleEntries(): List<Pair<String, ConfigItem<*>>> {
        val provider = script as? ConfigVisibilityProvider ?: return entries
        return entries.filter { (fieldName, item) ->
            try {
                provider.isConfigItemVisible(fieldName, item)
            } catch (e: Exception) {
                e.printStackTrace()
                true
            }
        }
    }

    private fun rebuild() {
        form.removeAll()
        val visible = visibleEntries()
        var row = 0
        if (visible.isEmpty()) {
            form.add(label("This script has no settings.", secondary = true), constraints(0, row++))
        }

        var index = 0
        while (index < visible.size) {
            val (fieldName, item) = visible[index]
            if (item is ConfigSection) {
                var end = index + 1
                while (end < visible.size && visible[end].second !is ConfigSection) end++
                form.add(sectionPanel(fieldName, item, visible.subList(index + 1, end)), constraints(0, row++))
                index = end
            } else {
                form.add(itemPanel(fieldName, item), constraints(0, row++))
                index++
            }
        }

        form.add(JPanel().apply { isOpaque = false }, GridBagConstraints().apply {
            gridx = 0; gridy = row; weightx = 1.0; weighty = 1.0; fill = GridBagConstraints.BOTH
        })
        form.revalidate()
        form.repaint()
    }

    private fun constraints(x: Int, y: Int) = GridBagConstraints().apply {
        gridx = x; gridy = y; weightx = 1.0; anchor = GridBagConstraints.WEST
        fill = GridBagConstraints.HORIZONTAL
    }

    private fun sectionPanel(fieldName: String, section: ConfigSection, items: List<Pair<String, ConfigItem<*>>>): JComponent {
        val open = openSections.getOrPut(fieldName) { section.defaultOpen }
        val body = JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(0, Spacing.PANEL, 0, 0)
            isVisible = open
            items.forEach { (itemField, item) ->
                add(itemPanel(itemField, item).apply { alignmentX = Component.LEFT_ALIGNMENT })
            }
        }
        val header = flatButton(headerText(section.name, open), section.description.ifBlank { null }) {}
        header.font = header.font.deriveFont(java.awt.Font.BOLD)
        header.horizontalAlignment = javax.swing.SwingConstants.LEFT
        header.addActionListener {
            val nowOpen = !body.isVisible
            openSections[fieldName] = nowOpen
            body.isVisible = nowOpen
            header.text = headerText(section.name, nowOpen)
            form.revalidate()
            form.repaint()
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(Spacing.GAP, 0, 0, 0)
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
    }

    private fun headerText(name: String, open: Boolean) = if (open) "▾ $name" else "▸ $name"

    private fun itemPanel(fieldName: String, item: ConfigItem<*>): JComponent {
        val panel = JPanel(GridBagLayout()).apply { isOpaque = false }
        var row = 0

        if (item is InfoDisplayConfigItem) {
            panel.add(infoRow(item), constraints(0, row++))
        } else {
            panel.add(label(item.name, bold = true), constraints(0, row++).apply {
                insets = Insets(Spacing.GAP, 0, 2, 0)
            })
            val editor = editorFor(fieldName, item)
            (editor as? JComponent)?.toolTipText = item.description
            panel.add(withActionButton(item, editor), constraints(0, row++).apply {
                insets = Insets(0, 0, 2, 0)
            })
        }

        if (item.description.isNotBlank()) {
            panel.add(WrappingText(item.description).apply {
                foreground = Palette.secondaryText
                font = font.deriveFont(11f)
            }, constraints(0, row++).apply { insets = Insets(0, 0, 2, 0) })
        }

        panel.add(JPanel().apply {
            isOpaque = false
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.border)
            preferredSize = Dimension(0, 1)
        }, constraints(0, row).apply { insets = Insets(Spacing.GAP, 0, 0, 0) })
        return panel
    }

    private fun infoRow(item: InfoDisplayConfigItem): JComponent {
        val value = label(item.value.ifEmpty { "-" })
        val row = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, Spacing.GAP, 0)).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(Spacing.GAP, 0, 0, 0)
            add(label("${item.name}:", bold = true))
            add(value)
        }
        item.action?.let { action ->
            row.add(button(action.label, item.description.ifBlank { null }) {
                action.run()?.let { produced ->
                    item.value = produced
                    value.text = produced.ifEmpty { "-" }
                    onChanged()
                }
            })
        }
        return row
    }

    private fun withActionButton(item: ConfigItem<*>, editor: Component): JComponent {
        @Suppress("UNCHECKED_CAST")
        val action = (item as? ActionableConfig<Any?>)?.action
            ?: return JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = false
                add(editor)
            }
        return JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, Spacing.GAP, 0)).apply {
            isOpaque = false
            add(editor)
            add(button(action.label, item.description.ifBlank { null }) {
                action.run()?.let { produced ->
                    @Suppress("UNCHECKED_CAST")
                    (item as ConfigItem<Any?>).value = produced
                    onChanged(rebuildForm = true)
                }
            })
        }
    }

    private fun editorFor(fieldName: String, item: ConfigItem<*>): Component = when (item) {
        is BooleanConfigItem -> JCheckBox("Enabled", item.value).apply {
            addActionListener { item.value = isSelected; onChanged() }
        }

        is IntConfigItem -> JSpinner(SpinnerNumberModel(item.value, item.min, item.max, 1)).apply {
            (editor as? JSpinner.DefaultEditor)?.textField?.columns = 8
            addChangeListener { item.value = value as Int; onChanged() }
        }

        is StringConfigItem -> JTextField(item.value, 20).apply {
            document.addDocumentListener(object : DocumentListener {
                private fun push() { item.value = text; onChanged() }
                override fun insertUpdate(e: DocumentEvent) = push()
                override fun removeUpdate(e: DocumentEvent) = push()
                override fun changedUpdate(e: DocumentEvent) = push()
            })
        }

        is OptionsConfigItem<*> -> JComboBox(item.options).apply {
            selectedItem = item.value
            addActionListener {
                @Suppress("UNCHECKED_CAST")
                (item as ConfigItem<Any?>).value = selectedItem
                onChanged()
            }
        }

        is EnumConfigItem<*> -> JComboBox(item.enumValues).apply {
            selectedItem = item.value
            addActionListener {
                @Suppress("UNCHECKED_CAST")
                (item as ConfigItem<Any?>).value = selectedItem
                onChanged()
            }
        }

        else -> label("${item.name}: ${item.value}", secondary = true)
    }

    companion object {
        fun invokeOnConfigUpdated(script: Any) {
            try {
                script.javaClass.getMethod("onConfigUpdated").invoke(script)
            } catch (_: NoSuchMethodException) {
            } catch (e: Exception) {
                println("Error invoking onConfigUpdated: ${e.message}")
            }
        }
    }
}
