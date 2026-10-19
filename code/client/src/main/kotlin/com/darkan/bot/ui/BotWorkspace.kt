package com.darkan.bot.ui

import com.darkan.bot.BotUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JSplitPane
import javax.swing.SwingUtilities

/**
 * Arranges the game canvas and the bot sidebar inside the client window.
 *
 * The sidebar keeps its width when the window is resized (the game absorbs the
 * change), can sit on either side, can be hidden behind a slim handle, and its
 * width, side, visibility and the window bounds all persist between launches.
 */
class BotWorkspace(private val frame: JFrame, private val clientPanel: JComponent, private val sidebar: BotUI) {
    enum class Side(val label: String) { LEFT("Left"), RIGHT("Right") }

    private val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
        isOneTouchExpandable = true
        isContinuousLayout = true
        resizeWeight = 1.0
        border = null
    }
    private val defaultDividerSize = split.dividerSize
    private val showHandle = JButton().apply {
        toolTipText = "Show bot sidebar"
        putClientProperty("JButton.variant", "borderless")
        putClientProperty("JButton.square", true)
        margin = java.awt.Insets(0, 0, 0, 0)
        preferredSize = Dimension(HANDLE_WIDTH, 0)
        isFocusable = false
        addActionListener { sidebarVisible = true }
    }
    private var applyingLayout = false

    // Saved values are captured up front: showing the frame fires resize and
    // divider events that would otherwise overwrite them before they are applied.
    private val savedSidebarWidth = UiPreferences.getInt("sidebar.width", DEFAULT_SIDEBAR_WIDTH).coerceAtLeast(MIN_SIDEBAR_WIDTH)
    private val savedWindowBounds = UiPreferences.getRectangle("window.bounds")
    private val savedMaximized = UiPreferences.getBoolean("window.maximized", false)
    private var sidebarWidth = savedSidebarWidth
    private var persistenceEnabled = false

    var side: Side = Side.entries.firstOrNull { it.name == UiPreferences.getString("sidebar.side") } ?: Side.RIGHT
        set(value) {
            if (field == value) return
            field = value
            UiPreferences.put("sidebar.side", value.name)
            layoutSplit()
        }

    var sidebarVisible: Boolean = UiPreferences.getBoolean("sidebar.visible", true)
        set(value) {
            if (field == value) return
            field = value
            UiPreferences.put("sidebar.visible", value)
            layoutSplit()
        }

    fun install() {
        sidebar.workspace = this
        sidebar.minimumSize = Dimension(MIN_SIDEBAR_WIDTH, 0)
        clientPanel.minimumSize = Dimension(MIN_GAME_WIDTH, 0)
        frame.contentPane.add(split, BorderLayout.CENTER)
        layoutSplit()

        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY) {
            if (!persistenceEnabled || applyingLayout || !sidebarVisible || split.width == 0) return@addPropertyChangeListener
            val width = currentSidebarWidth()
            if (width >= MIN_SIDEBAR_WIDTH) {
                sidebarWidth = width
                UiPreferences.put("sidebar.width", width)
            }
        }
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) = rememberWindow()
            override fun componentMoved(e: ComponentEvent) = rememberWindow()
        })

        sidebar.dock.extraMenuItems = { workspaceMenuItems() }
    }

    /** Restores window bounds and the divider; call once the frame is packed and visible. */
    fun onFrameShown() {
        if (savedWindowBounds != null && isOnScreen(savedWindowBounds)) frame.bounds = savedWindowBounds
        if (savedMaximized) frame.extendedState = frame.extendedState or Frame.MAXIMIZED_BOTH
        frame.validate()
        SwingUtilities.invokeLater {
            applyDivider()
            persistenceEnabled = true
            rememberWindow()
            sidebar.dock.restorePopOuts()
        }
    }

    fun resetLayout() {
        UiPreferences.clear("sidebar.")
        UiPreferences.clear("window.")
        sidebarWidth = DEFAULT_SIDEBAR_WIDTH
        sidebar.dock.resetLayout()
        side = Side.RIGHT
        sidebarVisible = true
        layoutSplit()
    }

    fun workspaceMenuItems(): List<JMenuItem> = listOf(
        JMenuItem(if (side == Side.RIGHT) "Move sidebar to the left" else "Move sidebar to the right").apply {
            addActionListener { side = if (side == Side.RIGHT) Side.LEFT else Side.RIGHT }
        },
        JMenuItem("Hide sidebar").apply { addActionListener { sidebarVisible = false } },
        JMenuItem("Reset layout").apply {
            toolTipText = "Restore the default sidebar side, width, tab order and window size"
            addActionListener { resetLayout() }
        },
    )

    // ---- internals ----

    private fun layoutSplit() {
        applyingLayout = true
        try {
            split.leftComponent = null
            split.rightComponent = null
            val sideComponent: JComponent = if (sidebarVisible) sidebar else showHandle
            showHandle.text = if (side == Side.RIGHT) "◀" else "▶"
            if (side == Side.RIGHT) {
                split.leftComponent = clientPanel
                split.rightComponent = sideComponent
            } else {
                split.leftComponent = sideComponent
                split.rightComponent = clientPanel
            }
            split.dividerSize = if (sidebarVisible) defaultDividerSize else 0
            split.isEnabled = sidebarVisible
            split.revalidate()
            split.repaint()
        } finally {
            applyingLayout = false
        }
        SwingUtilities.invokeLater { applyDivider() }
    }

    private fun applyDivider() {
        if (split.width == 0) return
        applyingLayout = true
        try {
            val width = if (sidebarVisible) sidebarWidth else HANDLE_WIDTH
            split.dividerLocation = if (side == Side.RIGHT) split.width - width - split.dividerSize else width
        } finally {
            applyingLayout = false
        }
    }

    private fun currentSidebarWidth(): Int =
        if (side == Side.RIGHT) split.width - split.dividerLocation - split.dividerSize else split.dividerLocation

    private fun rememberWindow() {
        if (!persistenceEnabled || !frame.isShowing) return
        val maximized = frame.extendedState and Frame.MAXIMIZED_BOTH != 0
        UiPreferences.put("window.maximized", maximized)
        if (!maximized) UiPreferences.put("window.bounds", frame.bounds)
    }

    private fun isOnScreen(r: Rectangle): Boolean =
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { it.defaultConfiguration.bounds.intersects(r) }

    private companion object {
        const val DEFAULT_SIDEBAR_WIDTH = 520
        const val MIN_SIDEBAR_WIDTH = 300
        const val MIN_GAME_WIDTH = 400
        const val HANDLE_WIDTH = 18
    }
}
