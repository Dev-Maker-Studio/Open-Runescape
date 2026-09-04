package com.darkan.bot.ui

import com.darkan.bot.SETTING_VARC_DEBUG_ENABLED
import com.darkan.bot.SETTING_VARP_DEBUG_ENABLED
import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.event.Varc
import com.darkan.bot.scripts.event.Varcbit
import com.darkan.bot.scripts.event.Varp
import java.awt.BorderLayout
import java.awt.CardLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.RowFilter
import javax.swing.SortOrder
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

/**
 * Live watch of varp / varc changes for script authors. Events are captured on
 * the game thread and batched onto the EDT so the watcher never slows the
 * client down.
 */
class VarsPanel : JPanel(BorderLayout()) {
    private class Change(val time: Long, val type: String, val id: Int, val old: Int, val new: Int)

    private val columns = arrayOf("Time", "Type", "ID", "Old", "New")
    private val model = object : DefaultTableModel(columns, 0) {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(column: Int): Class<*> = if (column >= 2) Integer::class.java else String::class.java
    }
    private val sorter = TableRowSorter(model)
    private val table = AdaptiveTable(model).apply {
        rowSorter = sorter
        columnWidths(92, 58, 60, 64, 64)
    }
    private val search = searchField("Filter by type, id or value", 14)
    private val pending = ConcurrentLinkedQueue<Change>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS")
    private val centerLayout = CardLayout()
    private val center = JPanel(centerLayout)
    private val emptyState = EmptyState("Turn on Varp or Varc to start watching changes.")

    private val varpToggle = toggleSwitch("Varp", UiPreferences.getBoolean("vars.varp", false), "Record player variable (varp) changes") {
        SETTING_VARP_DEBUG_ENABLED = it
        UiPreferences.put("vars.varp", it)
        updateEmptyState()
    }
    private val varcToggle = toggleSwitch("Varc", UiPreferences.getBoolean("vars.varc", false), "Record client variable (varc) changes") {
        SETTING_VARC_DEBUG_ENABLED = it
        UiPreferences.put("vars.varc", it)
        updateEmptyState()
    }
    private val collapseToggle = toggleSwitch("Collapse repeats", UiPreferences.getBoolean("vars.collapse", true), "Keep one row per variable, updated in place") {
        UiPreferences.put("vars.collapse", it)
    }
    private val pauseToggle = toggleSwitch("Pause", false, "Stop adding rows; changes are discarded while paused") {}

    init {
        border = Spacing.panelBorder()
        SETTING_VARP_DEBUG_ENABLED = varpToggle.isSelected
        SETTING_VARC_DEBUG_ENABLED = varcToggle.isSelected

        add(JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(headerRow(search).apply { alignmentX = LEFT_ALIGNMENT })
            add(Box.createVerticalStrut(Spacing.GAP))
            add(toolbarRow(
                varpToggle, varcToggle, collapseToggle, pauseToggle,
                button("Clear", "Remove every recorded change") { clear() },
            ).apply { alignmentX = LEFT_ALIGNMENT })
        }, BorderLayout.NORTH)

        center.isOpaque = false
        center.add(JScrollPane(table).tidy(), "table")
        center.add(emptyState, "empty")
        add(center, BorderLayout.CENTER)
        updateEmptyState()

        sorter.sortKeys = listOf(javax.swing.RowSorter.SortKey(0, SortOrder.DESCENDING))
        search.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyFilter()
            override fun removeUpdate(e: DocumentEvent) = applyFilter()
            override fun changedUpdate(e: DocumentEvent) = applyFilter()
        })

        ScriptExecutor.addEventListener { event ->
            when (event) {
                is Varp -> if (SETTING_VARP_DEBUG_ENABLED) record("Varp", event.id, event.oldValue, event.newValue)
                is Varc -> if (SETTING_VARC_DEBUG_ENABLED) record("Varc", event.id, event.oldValue, event.newValue)
                is Varcbit -> if (SETTING_VARC_DEBUG_ENABLED) record("Varcbit", event.id, event.oldValue, event.newValue)
            }
        }
        Timer(150) { flushPending() }.start()
    }

    /** Records a change. Safe to call from any thread; ignored while paused or when nothing changed. */
    fun record(type: String, id: Int, old: Int, new: Int) {
        if (old == new || pauseToggle.isSelected) return
        pending.add(Change(System.currentTimeMillis(), type, id, old, new))
    }

    private fun flushPending() {
        if (pending.isEmpty()) return
        val collapse = collapseToggle.isSelected
        var added = false
        while (true) {
            val change = pending.poll() ?: break
            if (collapse) {
                val existing = (0 until model.rowCount).firstOrNull { model.getValueAt(it, 1) == change.type && model.getValueAt(it, 2) == change.id }
                if (existing != null) model.removeRow(existing)
            }
            model.insertRow(0, arrayOf<Any>(timeFormat.format(Date(change.time)), change.type, change.id, change.old, change.new))
            added = true
        }
        while (model.rowCount > MAX_ROWS) model.removeRow(model.rowCount - 1)
        if (added) updateEmptyState()
    }

    private fun applyFilter() {
        val query = search.text.trim()
        sorter.rowFilter = if (query.isEmpty()) null else RowFilter.regexFilter("(?i)" + Regex.escape(query))
    }

    private fun clear() {
        pending.clear()
        model.rowCount = 0
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val watching = varpToggle.isSelected || varcToggle.isSelected
        emptyState.message = if (watching) "Waiting for changes…" else "Turn on Varp or Varc to start watching changes."
        centerLayout.show(center, if (model.rowCount == 0) "empty" else "table")
    }

    private companion object {
        const val MAX_ROWS = 500
    }
}
