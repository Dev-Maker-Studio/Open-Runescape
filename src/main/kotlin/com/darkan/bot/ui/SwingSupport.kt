package com.darkan.bot.ui

import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Insets
import java.awt.Rectangle
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.Scrollable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.Border

/**
 * Theme-aware colours for the sidebar. Every accessor falls back to a sane
 * constant so the panels still look reasonable if DarkLaf is not installed.
 */
object Palette {
    val text: Color get() = UIManager.getColor("textForeground") ?: Color(0xBB, 0xBB, 0xBB)
    val secondaryText: Color get() = UIManager.getColor("textForegroundSecondary") ?: Color(0x91, 0x91, 0x91)
    val border: Color get() = UIManager.getColor("borderSecondary") ?: Color(0x55, 0x55, 0x55)
    val cardBackground: Color get() = UIManager.getColor("backgroundAlternative") ?: Color(0x41, 0x45, 0x47)
    val headerBackground: Color get() = UIManager.getColor("backgroundHeader") ?: Color(0x45, 0x48, 0x4A)
    val success = Color(0x5D, 0xC4, 0x8F)
    val successFill = Color(0x2E, 0x7D, 0x4F)
    val danger = Color(0xE7, 0x48, 0x48)
    val dangerFill = Color(0x9A, 0x33, 0x33)
    val warning = Color(0xE5, 0xC0, 0x7B)
    val accent = Color(0x58, 0x9D, 0xF6)
    val favourite = Color(0xF5, 0xB0, 0x41)
}

/** Standard control spacing used by every panel so the sidebar reads as one surface. */
object Spacing {
    const val GAP = 6
    const val PANEL = 8
    fun panelBorder(): Border = BorderFactory.createEmptyBorder(PANEL, PANEL, PANEL, PANEL)
}

/**
 * A [FlowLayout] that reports a preferred height tall enough for every row it
 * wraps into. Plain FlowLayout always claims a single row, so a wrapped
 * toolbar inside a BorderLayout gets its second row clipped; this is the
 * classic fix.
 */
class WrapLayout(align: Int = LEFT, hgap: Int = Spacing.GAP, vgap: Int = Spacing.GAP) : FlowLayout(align, hgap, vgap) {
    override fun preferredLayoutSize(target: Container): Dimension = layoutSize(target, true)
    override fun minimumLayoutSize(target: Container): Dimension = layoutSize(target, false).also { it.width -= hgap + 1 }

    private fun layoutSize(target: Container, preferred: Boolean): Dimension {
        synchronized(target.treeLock) {
            var targetWidth = target.width
            if (targetWidth == 0) {
                // Not laid out yet: use the first ancestor that has a width, or a
                // generous default so the first pass is a single row.
                var p: Container? = target.parent
                while (p != null && p.width == 0) p = p.parent
                targetWidth = p?.width ?: Int.MAX_VALUE
            }
            val insets = target.insets
            val horizontalInsets = insets.left + insets.right + hgap * 2
            val maxWidth = targetWidth - horizontalInsets

            val dim = Dimension(0, 0)
            var rowWidth = 0
            var rowHeight = 0
            for (i in 0 until target.componentCount) {
                val m = target.getComponent(i)
                if (!m.isVisible) continue
                val d = if (preferred) m.preferredSize else m.minimumSize
                if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                    addRow(dim, rowWidth, rowHeight)
                    rowWidth = 0
                    rowHeight = 0
                }
                if (rowWidth != 0) rowWidth += hgap
                rowWidth += d.width
                rowHeight = maxOf(rowHeight, d.height)
            }
            addRow(dim, rowWidth, rowHeight)
            dim.width += horizontalInsets
            dim.height += insets.top + insets.bottom + vgap * 2

            // When inside a scroll pane the viewport width, not the container's own
            // width, is the real constraint.
            val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, target)
            if (scrollPane != null && target.isValid) dim.width -= hgap + 1
            return dim
        }
    }

    private fun addRow(dim: Dimension, rowWidth: Int, rowHeight: Int) {
        dim.width = maxOf(dim.width, rowWidth)
        if (dim.height > 0) dim.height += vgap
        dim.height += rowHeight
    }
}

/**
 * Read-only, transparent, word-wrapping text block whose preferred height
 * follows the width it is given. A `<html>` JLabel needs an explicit width
 * hint to wrap, which is why the old cards wrapped so unpredictably.
 */
class WrappingText(text: String = "") : JTextArea(text) {
    init {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
        isFocusable = false
        isOpaque = false
        border = null
        cursor = null
        // Never let this block steal the scroll position of the list it sits in.
        (caret as? javax.swing.text.DefaultCaret)?.updatePolicy = javax.swing.text.DefaultCaret.NEVER_UPDATE
    }

    override fun scrollRectToVisible(aRect: Rectangle) {}

    override fun setBounds(x: Int, y: Int, w: Int, h: Int) {
        val widthChanged = w != width
        super.setBounds(x, y, w, h)
        if (widthChanged) {
            // Our preferred height depends on the width we were just given; ask the
            // scrollable ancestor to lay out again now that we know it.
            SwingUtilities.invokeLater {
                val host = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this) ?: parent
                (host as? JComponent)?.revalidate() ?: parent?.revalidate()
            }
        }
    }
}

/**
 * Scroll-pane content that always spans the viewport width and scrolls only
 * vertically, so forms and lists reflow instead of being clipped when the
 * sidebar is narrow.
 */
open class ScrollableColumn(layout: java.awt.LayoutManager? = null) : JPanel(layout), Scrollable {
    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
    override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int = 24
    override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int =
        if (orientation == SwingConstants.VERTICAL) visibleRect.height else visibleRect.width
    override fun getScrollableTracksViewportWidth(): Boolean = true
    override fun getScrollableTracksViewportHeight(): Boolean = false
}

/**
 * Vertical list of cards that always spans the viewport width. Combine with
 * [CardPanel] children so BoxLayout never stretches them vertically.
 */
open class VerticalListPanel : ScrollableColumn() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
}

/** A panel that fills the available width but never grows taller than its content. */
open class CardPanel : JPanel() {
    init {
        alignmentX = LEFT_ALIGNMENT
    }

    override fun getMaximumSize(): Dimension = Dimension(Int.MAX_VALUE, preferredSize.height)
}

fun cardBorder(): Border = BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(Palette.border, 1, true),
    BorderFactory.createEmptyBorder(Spacing.PANEL, Spacing.PANEL, Spacing.PANEL, Spacing.PANEL)
)

fun label(text: String, secondary: Boolean = false, bold: Boolean = false, size: Float? = null): JLabel =
    JLabel(text).apply {
        if (secondary) foreground = Palette.secondaryText
        var f = font
        if (bold) f = f.deriveFont(Font.BOLD)
        if (size != null) f = f.deriveFont(size)
        font = f
    }

fun sectionHeader(text: String): JLabel = label(text.uppercase(), secondary = true, bold = true, size = 11f).apply {
    border = BorderFactory.createEmptyBorder(Spacing.GAP, 0, 2, 0)
}

fun button(text: String, tooltip: String? = null, action: () -> Unit): JButton = JButton(text).apply {
    toolTipText = tooltip
    addActionListener { action() }
}

/** Borderless button for secondary actions such as icons in a card header. */
fun flatButton(text: String, tooltip: String? = null, action: () -> Unit): JButton = button(text, tooltip, action).apply {
    putClientProperty("JButton.variant", "borderless")
    putClientProperty("JButton.square", true)
    margin = Insets(2, 4, 2, 4)
    isFocusable = false
}

/** DarkLaf slider-style toggle. Falls back to a plain toggle button elsewhere. */
fun toggleSwitch(text: String, selected: Boolean, tooltip: String? = null, onChange: (Boolean) -> Unit): JToggleButton =
    JToggleButton(text, selected).apply {
        putClientProperty("JToggleButton.variant", "slider")
        toolTipText = tooltip
        isFocusable = false
        addActionListener { onChange(isSelected) }
    }

fun searchField(placeholder: String, columns: Int = 12): JTextField = JTextField(columns).apply {
    putClientProperty("JTextField.variant", "search")
    putClientProperty("JTextField.showClear", true)
    putClientProperty("JTextComponent.defaultText", placeholder)
    toolTipText = placeholder
}

fun JComponent.padded(top: Int, left: Int, bottom: Int, right: Int): JComponent = apply {
    border = BorderFactory.createEmptyBorder(top, left, bottom, right)
}

/** Lays [components] out in a row that wraps instead of clipping when the sidebar is narrow. */
fun toolbarRow(vararg components: Component): JPanel = JPanel(WrapLayout(FlowLayout.LEFT, Spacing.GAP, 2)).apply {
    isOpaque = false
    components.forEach { add(it) }
}

/** Header row: [left] hugs the leading edge and stretches, [right] items stay at the trailing edge. */
fun headerRow(left: Component, vararg right: Component): JPanel = JPanel(java.awt.BorderLayout(Spacing.GAP, 0)).apply {
    isOpaque = false
    add(left, java.awt.BorderLayout.CENTER)
    if (right.isNotEmpty()) {
        add(JPanel(FlowLayout(FlowLayout.RIGHT, Spacing.GAP, 0)).apply {
            isOpaque = false
            right.forEach { add(it) }
        }, java.awt.BorderLayout.EAST)
    }
}

/** Centered hint shown when a list or table has nothing to display. */
class EmptyState(message: String) : JPanel(java.awt.GridBagLayout()) {
    private val messageLabel = label(message, secondary = true).apply { horizontalAlignment = SwingConstants.CENTER }

    init {
        isOpaque = false
        add(messageLabel)
    }

    var message: String
        get() = messageLabel.text
        set(value) {
            messageLabel.text = value
        }
}

/**
 * Table that stretches its columns to the viewport when there is room and
 * otherwise keeps every column at its minimum width and scrolls horizontally,
 * so cells are never truncated to "..." in a narrow sidebar.
 */
open class AdaptiveTable(model: javax.swing.table.TableModel) : javax.swing.JTable(model) {
    init {
        fillsViewportHeight = true
        rowHeight = 22
        autoResizeMode = AUTO_RESIZE_ALL_COLUMNS
    }

    /** Sets minimum and preferred width for each column in order. */
    fun columnWidths(vararg widths: Int) {
        widths.forEachIndexed { i, w ->
            if (i < columnModel.columnCount) columnModel.getColumn(i).apply { minWidth = w; preferredWidth = w }
        }
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        val minimum = (0 until columnModel.columnCount).sumOf { columnModel.getColumn(it).minWidth }
        return (parent?.width ?: 0) >= minimum
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        if (!scrollableTracksViewportWidth) size.width = (0 until columnModel.columnCount).sumOf { columnModel.getColumn(it).minWidth }
        return size
    }
}

/** Makes a scroll pane feel native: no outer border, sensible wheel speed. */
fun JScrollPane.tidy(): JScrollPane = apply {
    border = null
    viewportBorder = null
    verticalScrollBar.unitIncrement = 24
    horizontalScrollBar.unitIncrement = 24
}
