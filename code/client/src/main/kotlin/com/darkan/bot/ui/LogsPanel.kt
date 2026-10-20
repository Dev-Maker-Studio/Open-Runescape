package com.darkan.bot.ui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Desktop
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JToggleButton
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter

class LogsPanel(private val logFile: () -> File?) : JPanel(BorderLayout()) {
    private val filterField = searchField("Filter lines", 14)
    private val errorsOnly: JToggleButton = toggleSwitch("Errors only", false, "Show only lines that look like errors or stack traces") { rerender() }
    private val follow: JToggleButton = toggleSwitch("Follow", UiPreferences.getBoolean("logs.follow", true), "Keep the newest line in view") {
        UiPreferences.put("logs.follow", it)
        if (it) scrollToBottom()
    }
    private val wrap: JToggleButton = toggleSwitch("Wrap", UiPreferences.getBoolean("logs.wrap", true), "Wrap long lines") {
        UiPreferences.put("logs.wrap", it)
        area.lineWrap = it
        area.revalidate()
    }

    private val area = JTextArea().apply {
        isEditable = false
        lineWrap = wrap.isSelected
        wrapStyleWord = true
        tabSize = 4
        (caret as? DefaultCaret)?.updatePolicy = DefaultCaret.NEVER_UPDATE
    }
    private val scroll = JScrollPane(area).tidy()
    private val doc = area.document
    private val highlighter = area.highlighter as DefaultHighlighter
    private val errorPainter = DefaultHighlighter.DefaultHighlightPainter(Color(Palette.danger.red, Palette.danger.green, Palette.danger.blue, 46))
    private val warningPainter = DefaultHighlighter.DefaultHighlightPainter(Color(Palette.warning.red, Palette.warning.green, Palette.warning.blue, 36))

    private val lineTags = ArrayDeque<Any?>()
    private val pending = ConcurrentLinkedQueue<LogBus.Line>()
    private val flushTimer = Timer(120) { flushPending() }
    private val filterDebounce = Timer(200) { rerender() }.apply { isRepeats = false }
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS")
    private var fontSize = UiPreferences.getInt("logs.fontSize", 12).coerceIn(8, 28)
    private var suppressScrollTracking = false

    init {
        border = Spacing.panelBorder()

        add(JPanel().apply {
            isOpaque = false
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(headerRow(filterField).apply { alignmentX = LEFT_ALIGNMENT })
            add(Box.createVerticalStrut(Spacing.GAP))
            add(toolbarRow(
                follow, wrap, errorsOnly,
                button("Clear", "Clear the view (the log file is untouched)") { clearView() },
                button("Copy", "Copy the visible lines to the clipboard") { copyVisible() },
                button("Open file", "Open the log file on disk") { openLogFile() },
            ).apply { alignmentX = LEFT_ALIGNMENT })
        }, BorderLayout.NORTH)

        area.font = Font(Font.MONOSPACED, Font.PLAIN, fontSize)
        add(scroll, BorderLayout.CENTER)

        filterField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = filterDebounce.restart()
            override fun removeUpdate(e: DocumentEvent) = filterDebounce.restart()
            override fun changedUpdate(e: DocumentEvent) = filterDebounce.restart()
        })

        scroll.addMouseWheelListener { e ->
            if (e.isControlDown) {
                setFontSize(fontSize - e.wheelRotation)
                e.consume()
            } else {
                SwingUtilities.invokeLater { syncFollowWithScrollbar() }
            }
        }
        scroll.verticalScrollBar.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) = syncFollowWithScrollbar()
        })

        LogBus.subscribe { pending.add(it) }.forEach { pending.add(it) }
        flushTimer.start()
    }

    private fun setFontSize(size: Int) {
        fontSize = size.coerceIn(8, 28)
        area.font = Font(Font.MONOSPACED, Font.PLAIN, fontSize)
        UiPreferences.put("logs.fontSize", fontSize)
    }

    private fun matches(line: LogBus.Line): Boolean {
        if (errorsOnly.isSelected && !looksLikeError(line)) return false
        val query = filterField.text.trim()
        return query.isEmpty() || line.text.contains(query, ignoreCase = true)
    }

    private fun looksLikeError(line: LogBus.Line): Boolean =
        line.isError || line.text.contains("Exception") || line.text.startsWith("\tat ") || line.text.contains("Error")

    private fun looksLikeWarning(line: LogBus.Line): Boolean = line.text.contains("warn", ignoreCase = true)

    private fun append(line: LogBus.Line) {
        val start = doc.length
        doc.insertString(start, timeFormat.format(Date(line.timestamp)) + "  " + line.text + "\n", null)
        val painter = when {
            looksLikeError(line) -> errorPainter
            looksLikeWarning(line) -> warningPainter
            else -> null
        }
        lineTags.addLast(painter?.let { highlighter.addHighlight(start, doc.length - 1, it) })
    }

    private fun trimDocument() {
        val excess = lineTags.size - LogBus.capacity
        if (excess <= 0) return
        val root = doc.defaultRootElement
        repeat(excess) { lineTags.removeFirst()?.let { highlighter.removeHighlight(it) } }
        doc.remove(0, root.getElement(excess - 1).endOffset)
    }

    private fun flushPending() {
        if (pending.isEmpty()) return
        var appended = false
        while (true) {
            val line = pending.poll() ?: break
            if (matches(line)) {
                append(line)
                appended = true
            }
        }
        if (appended) {
            trimDocument()
            if (follow.isSelected) scrollToBottom()
        }
    }

    private fun clearDocument() {
        highlighter.removeAllHighlights()
        lineTags.clear()
        doc.remove(0, doc.length)
    }

    private fun rerender() {
        pending.clear()
        clearDocument()
        LogBus.snapshot().filter { matches(it) }.forEach { append(it) }
        scrollToBottom()
    }

    private fun scrollToBottom() {
        suppressScrollTracking = true
        SwingUtilities.invokeLater {
            val bar = scroll.verticalScrollBar
            bar.value = bar.maximum - bar.visibleAmount
            suppressScrollTracking = false
        }
    }

    private fun syncFollowWithScrollbar() {
        if (suppressScrollTracking) return
        val bar = scroll.verticalScrollBar
        val atBottom = bar.value + bar.visibleAmount >= bar.maximum - 4
        if (follow.isSelected != atBottom) {
            follow.isSelected = atBottom
            UiPreferences.put("logs.follow", atBottom)
        }
    }

    private fun clearView() {
        LogBus.clear()
        pending.clear()
        clearDocument()
    }

    private fun copyVisible() {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(area.text), null)
    }

    private fun openLogFile() {
        val file = logFile() ?: return
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file)
        } catch (e: Exception) {
            println("Log file: ${file.absolutePath}")
        }
    }
}
