package com.darkan.bot.ui

import com.darkan.bot.format
import com.darkan.bot.formatElapsedTime
import com.darkan.bot.getUnitsPerHour
import com.darkan.bot.utils.Skill
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.SkillConstants
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * Session XP tracker. Rates and time-to-level refresh every second rather
 * than only when an XP drop arrives, so idle skills decay honestly.
 */
class SkillsPanel : JPanel(BorderLayout()) {
    private class Entry(val skill: Skill) {
        var startTime = System.currentTimeMillis()
        var gained = 0
    }

    private val entries = mutableListOf<Entry>()
    private var sessionStart = System.currentTimeMillis()
    private val sessionLabel = label("Session 00:00:00", secondary = true)
    private val centerLayout = CardLayout()
    private val center = JPanel(centerLayout)
    private val emptyState = EmptyState("No XP gained yet this session.")

    private val columns = arrayOf("Skill", "Level", "XP gained", "XP / h", "XP to level", "Time to level")

    private val model = object : AbstractTableModel() {
        override fun getRowCount() = entries.size
        override fun getColumnCount() = columns.size
        override fun getColumnName(column: Int) = columns[column]
        override fun getValueAt(row: Int, column: Int): Any {
            val entry = entries[row]
            val perHour = getUnitsPerHour(entry.gained, entry.startTime)
            return when (column) {
                0 -> entry.skill.name.lowercase().replaceFirstChar { it.uppercase() }
                1 -> currentLevel(entry.skill).takeIf { it > 0 }?.toString() ?: "—"
                2 -> format(entry.gained)
                3 -> format(perHour)
                4 -> xpToNextLevel(entry.skill)?.let { format(it) } ?: "—"
                5 -> timeToLevel(entry.skill, perHour)
                else -> ""
            }
        }
    }

    private val table = AdaptiveTable(model).apply {
        autoCreateRowSorter = false
        columnWidths(96, 46, 84, 84, 84, 90)
        setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(t: JTable, value: Any?, sel: Boolean, focus: Boolean, row: Int, col: Int): java.awt.Component {
                val c = super.getTableCellRendererComponent(t, value, sel, focus, row, col)
                horizontalAlignment = if (col == 0) SwingConstants.LEFT else SwingConstants.RIGHT
                return c
            }
        })
    }

    init {
        border = Spacing.panelBorder()
        add(headerRow(
            sessionLabel,
            button("Reset all", "Start every skill's counters from zero") { resetAll() },
        ).padded(0, 0, Spacing.GAP, 0), BorderLayout.NORTH)

        center.isOpaque = false
        center.add(JScrollPane(table).tidy(), "table")
        center.add(emptyState, "empty")
        centerLayout.show(center, "empty")
        add(center, BorderLayout.CENTER)

        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = maybeShowMenu(e)
            override fun mouseReleased(e: MouseEvent) = maybeShowMenu(e)
            private fun maybeShowMenu(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                val row = table.rowAtPoint(e.point)
                if (row < 0 || row >= entries.size) return
                table.setRowSelectionInterval(row, row)
                val entry = entries[row]
                JPopupMenu().apply {
                    add(JMenuItem("Reset ${columnValue(entry)}").apply { addActionListener { reset(entry) } })
                    add(JMenuItem("Remove ${columnValue(entry)}").apply { addActionListener { remove(entry) } })
                }.show(e.component, e.x, e.y)
            }
        })

        Timer(1000) {
            sessionLabel.text = "Session ${formatElapsedTime(System.currentTimeMillis(), sessionStart)}"
            if (entries.isNotEmpty()) model.fireTableRowsUpdated(0, entries.size - 1)
        }.start()
    }

    private fun columnValue(entry: Entry) = entry.skill.name.lowercase().replaceFirstChar { it.uppercase() }

    /** Records an XP drop. Safe to call from any thread. */
    fun addXp(skill: Skill, gained: Int) {
        SwingUtilities.invokeLater {
            val entry = entries.firstOrNull { it.skill == skill } ?: Entry(skill).also { entries.add(it) }
            entry.gained += gained
            entries.sortByDescending { it.gained }
            model.fireTableDataChanged()
            centerLayout.show(center, "table")
        }
    }

    private fun reset(entry: Entry) {
        entry.startTime = System.currentTimeMillis()
        entry.gained = 0
        model.fireTableDataChanged()
    }

    private fun remove(entry: Entry) {
        entries.remove(entry)
        model.fireTableDataChanged()
        if (entries.isEmpty()) centerLayout.show(center, "empty")
    }

    private fun resetAll() {
        sessionStart = System.currentTimeMillis()
        entries.clear()
        model.fireTableDataChanged()
        centerLayout.show(center, "empty")
    }

    // ---- skill maths ----

    private fun currentLevel(skill: Skill): Int = Client.SKILL_LEVEL_ACTUAL.getOrNull(skill.ordinal) ?: 0

    private fun xpToNextLevel(skill: Skill): Int? {
        val level = currentLevel(skill)
        val max = SkillConstants.SKILL_MAX_LEVEL.getOrNull(skill.ordinal) ?: 99
        if (level < 1 || level >= max) return null
        val xp = Client.SKILL_XP.getOrNull(skill.ordinal) ?: return null
        // SKILL_XP_FOR_LEVEL[i] is the XP required for level i + 2.
        val needed = SkillConstants.SKILL_XP_FOR_LEVEL.getOrNull(level - 1) ?: return null
        return (needed - xp).coerceAtLeast(0)
    }

    private fun timeToLevel(skill: Skill, perHour: Int): String {
        val remaining = xpToNextLevel(skill) ?: return "—"
        if (perHour <= 0) return "—"
        val totalMinutes = (remaining.toDouble() / perHour * 60).toLong()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
