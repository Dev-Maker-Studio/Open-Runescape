package com.darkan.bot.ui

import com.github.weisj.darklaf.components.ClosableTabbedPane
import com.github.weisj.darklaf.components.TabEvent
import com.github.weisj.darklaf.components.TabListener
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.WindowConstants

/** One tool panel that can live in the sidebar tab strip or in its own floating window. */
class Dockable(
    val id: String,
    title: String,
    val component: JComponent,
    val closable: Boolean = false,
    val tooltip: String? = null,
) {
    internal val tabLabel = JLabel(title)

    var title: String
        get() = tabLabel.text
        set(value) {
            tabLabel.text = value
            (tabLabel.parent as? JComponent)?.revalidate()
        }
}

/**
 * Tab strip whose tabs can be reordered by drag-and-drop, closed, popped out
 * into floating windows and docked back. Order, selection and pop-out state
 * survive restarts through [UiPreferences].
 */
class DockablePane(private val ownerWindow: () -> Window?) : JPanel(BorderLayout()) {
    val tabs = ClosableTabbedPane().apply {
        putClientProperty("JTabbedPane.dndEnabled", true)
        tabLayoutPolicy = javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT
    }

    private val dockables = LinkedHashMap<String, Dockable>()
    private val popOuts = HashMap<String, PopOutWindow>()
    private var suppressCloseEvents = 0
    private val persistTimer = Timer(300) { persistLayout() }.apply { isRepeats = false }

    /** Extra entries appended to every tab context menu and the overflow menu. */
    var extraMenuItems: () -> List<JMenuItem> = { emptyList() }

    /** Called after a closable dockable is closed by the user. */
    var onClosed: (Dockable) -> Unit = {}

    init {
        add(tabs, BorderLayout.CENTER)

        tabs.addTabListener(object : TabListener {
            override fun tabOpened(e: TabEvent) = schedulePersist()
            override fun tabClosing(e: TabEvent) {}
            override fun tabClosed(e: TabEvent) {
                if (suppressCloseEvents > 0) return
                val component = e.component
                // Drag-and-drop reordering also removes and re-inserts the tab, so
                // only treat this as a close once the pane has settled.
                SwingUtilities.invokeLater {
                    if (tabs.indexOfComponent(component) != -1) return@invokeLater
                    val dockable = dockables.values.firstOrNull { it.component === component } ?: return@invokeLater
                    if (popOuts.containsKey(dockable.id)) return@invokeLater
                    dockables.remove(dockable.id)
                    onClosed(dockable)
                    schedulePersist()
                }
            }
        })
        tabs.addChangeListener { schedulePersist() }

        tabs.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = maybeShowMenu(e)
            override fun mouseReleased(e: MouseEvent) = maybeShowMenu(e)
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    val index = tabs.indexAtLocation(e.x, e.y)
                    if (index >= 0) dockableAt(index)?.let { if (it.closable) close(it.id) }
                }
            }

            private fun maybeShowMenu(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                val index = tabs.indexAtLocation(e.x, e.y)
                if (index < 0) return
                val dockable = dockableAt(index) ?: return
                tabs.selectedIndex = index
                buildMenu(dockable).show(tabs, e.x, e.y)
            }
        })

        val overflow = flatButton("⋮", "Sidebar menu") {}
        overflow.addActionListener {
            val menu = current()?.let { buildMenu(it) } ?: JPopupMenu().also { m -> extraMenuItems().forEach { m.add(it) } }
            menu.show(overflow, 0, overflow.height)
        }
        tabs.putClientProperty("JTabbedPane.trailingComponent", overflow)
    }

    // ---- public API ----

    fun all(): List<Dockable> = dockables.values.toList()

    fun get(id: String): Dockable? = dockables[id]

    fun current(): Dockable? = tabs.selectedIndex.takeIf { it >= 0 }?.let { dockableAt(it) }

    fun add(dockable: Dockable, select: Boolean = false, index: Int = -1) {
        val existing = dockables[dockable.id]
        if (existing != null) {
            select(dockable.id)
            return
        }
        dockables[dockable.id] = dockable
        insertTab(dockable, if (index < 0) tabs.tabCount else index.coerceIn(0, tabs.tabCount))
        if (select) select(dockable.id)
        schedulePersist()
    }

    fun select(id: String) {
        val dockable = dockables[id] ?: return
        popOuts[id]?.let { it.toFront(); it.requestFocus(); return }
        val index = tabs.indexOfComponent(dockable.component)
        if (index >= 0) tabs.selectedIndex = index
    }

    fun close(id: String) {
        val dockable = dockables.remove(id) ?: return
        popOuts.remove(id)?.discard()
        val index = tabs.indexOfComponent(dockable.component)
        if (index >= 0) {
            suppressCloseEvents++
            try {
                tabs.removeTabAt(index)
            } finally {
                suppressCloseEvents--
            }
        }
        onClosed(dockable)
        schedulePersist()
    }

    fun isPoppedOut(id: String): Boolean = popOuts.containsKey(id)

    fun popOut(id: String) {
        val dockable = dockables[id] ?: return
        if (popOuts.containsKey(id)) {
            select(id)
            return
        }
        val index = tabs.indexOfComponent(dockable.component)
        if (index >= 0) {
            suppressCloseEvents++
            try {
                tabs.removeTabAt(index)
            } finally {
                suppressCloseEvents--
            }
        }
        val window = PopOutWindow(dockable, ownerWindow()) { dock(id) }
        popOuts[id] = window
        window.isVisible = true
        UiPreferences.put("popout.$id.open", true)
        schedulePersist()
    }

    fun dock(id: String) {
        val dockable = dockables[id] ?: return
        val window = popOuts.remove(id) ?: return
        window.discard()
        UiPreferences.put("popout.$id.open", false)
        insertTab(dockable, dockIndexFor(id))
        select(id)
        schedulePersist()
    }

    fun dockAll() = popOuts.keys.toList().forEach { dock(it) }

    /** Applies the saved order, selection and pop-out state. Call once after the default tabs are added. */
    fun restoreLayout() {
        val savedOrder = UiPreferences.getList("tabs.order")
        if (savedOrder.isNotEmpty()) {
            val ordered = savedOrder.mapNotNull { dockables[it] } + dockables.values.filter { it.id !in savedOrder }
            suppressCloseEvents++
            try {
                tabs.removeAll()
                ordered.forEach { insertTab(it, tabs.tabCount) }
            } finally {
                suppressCloseEvents--
            }
        }
        UiPreferences.getString("tabs.selected")?.let { select(it) }
    }

    /** Re-opens the windows that were popped out last session. Call once the owning frame is showing so they stack above it. */
    fun restorePopOuts() {
        dockables.keys.toList().forEach { id ->
            if (UiPreferences.getBoolean("popout.$id.open", false)) popOut(id)
        }
    }

    /** Forgets every saved layout preference for the tab strip and re-docks all windows. */
    fun resetLayout() {
        dockAll()
        UiPreferences.clear("tabs.")
        UiPreferences.clear("popout.")
    }

    // ---- internals ----

    private fun insertTab(dockable: Dockable, index: Int) {
        tabs.insertTab(dockable.title, null, dockable.component, dockable.tooltip, index)
        tabs.setTabComponentAt(index, dockable.tabLabel)
        tabs.setTabClosable(index, dockable.closable)
    }

    private fun dockableAt(index: Int): Dockable? {
        val component = tabs.getComponentAt(index)
        return dockables.values.firstOrNull { it.component === component }
    }

    /** Where a docked-back tab should land: after the nearest surviving predecessor in the saved order. */
    private fun dockIndexFor(id: String): Int {
        val order = UiPreferences.getList("tabs.order").ifEmpty { dockables.keys.toList() }
        val position = order.indexOf(id)
        if (position < 0) return tabs.tabCount
        for (i in position - 1 downTo 0) {
            val predecessor = dockables[order[i]] ?: continue
            val tabIndex = tabs.indexOfComponent(predecessor.component)
            if (tabIndex >= 0) return tabIndex + 1
        }
        return 0
    }

    private fun buildMenu(dockable: Dockable): JPopupMenu = JPopupMenu().apply {
        if (popOuts.containsKey(dockable.id)) {
            add(JMenuItem("Dock \"${dockable.title}\" back into sidebar").apply { addActionListener { dock(dockable.id) } })
        } else {
            add(JMenuItem("Pop out \"${dockable.title}\" into a window").apply { addActionListener { popOut(dockable.id) } })
        }
        if (dockable.closable) {
            add(JMenuItem("Close \"${dockable.title}\"").apply { addActionListener { close(dockable.id) } })
        }
        if (popOuts.isNotEmpty()) {
            add(JMenuItem("Dock all windows").apply { addActionListener { dockAll() } })
        }
        val extras = extraMenuItems()
        if (extras.isNotEmpty()) {
            addSeparator()
            extras.forEach { add(it) }
        }
    }

    private fun schedulePersist() = persistTimer.restart()

    private fun persistLayout() {
        // Docked tabs in their current visual order, then popped-out ones in
        // their remembered order so they dock back to a sensible spot.
        val docked = (0 until tabs.tabCount).mapNotNull { dockableAt(it)?.id }
        val floating = UiPreferences.getList("tabs.order").filter { it in popOuts } + popOuts.keys.filter { it !in UiPreferences.getList("tabs.order") }
        UiPreferences.put("tabs.order", docked + floating.distinct())
        current()?.let { UiPreferences.put("tabs.selected", it.id) }
    }
}

/** Floating home for a [Dockable]. Closing the window docks the panel back. */
private class PopOutWindow(
    private val dockable: Dockable,
    owner: Window?,
    private val onDock: () -> Unit,
) : JDialog(owner, dockable.title) {
    private var discarded = false

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        modalityType = ModalityType.MODELESS
        contentPane.layout = BorderLayout()

        val header = headerRow(
            label(dockable.title, bold = true).padded(0, Spacing.GAP, 0, 0),
            button("Dock", "Return this panel to the sidebar") { onDock() },
        ).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.border),
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
            )
            background = Palette.headerBackground
            isOpaque = true
        }
        contentPane.add(header, BorderLayout.NORTH)
        contentPane.add(dockable.component, BorderLayout.CENTER)

        minimumSize = Dimension(280, 200)
        val saved = UiPreferences.getRectangle("popout.${dockable.id}.bounds")
        if (saved != null && isOnScreen(saved)) bounds = saved
        else {
            size = Dimension(460, 560)
            setLocationRelativeTo(owner)
        }

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) = onDock()
        })
        addComponentListener(object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent) = remember()
            override fun componentResized(e: ComponentEvent) = remember()
        })
    }

    private fun remember() {
        if (!discarded && isShowing) UiPreferences.put("popout.${dockable.id}.bounds", bounds)
    }

    fun discard() {
        discarded = true
        contentPane.remove(dockable.component)
        dispose()
    }

    private fun isOnScreen(r: Rectangle): Boolean {
        val screens = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        return screens.any { it.defaultConfiguration.bounds.intersects(r) }
    }
}
