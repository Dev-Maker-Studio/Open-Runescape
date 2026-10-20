package com.darkan.crash

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.WindowConstants

object CrashDialog {
    enum class Mode { CRASH, REPORT }

    private val heading = Font(Font.SANS_SERIF, Font.BOLD, 17)
    private val body = Font(Font.SANS_SERIF, Font.PLAIN, 14)
    private val small = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    private val mono = Font(Font.MONOSPACED, Font.PLAIN, 12)

    fun show(report: CrashReport, file: File?, mode: Mode, onClose: () -> Unit = {}) {
        val title = if (mode == Mode.CRASH) "Darkan has crashed" else "Report a problem"
        val dialog = JDialog(null as Frame?, title, false)
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        dialog.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = onClose()
        })

        val root = JPanel(BorderLayout(0, 12)).apply {
            border = BorderFactory.createEmptyBorder(18, 22, 16, 22)
        }

        val intro = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel(if (mode == Mode.CRASH) "Sorry, Darkan ran into a problem and had to close." else "Something not working right? Tell us about it.").apply { font = heading; alignmentX = JComponent.LEFT_ALIGNMENT })
            add(Box.createVerticalStrut(6))
            add(JLabel("Help us fix it. It takes three clicks:").apply { font = body; alignmentX = JComponent.LEFT_ALIGNMENT })
            add(Box.createVerticalStrut(10))
            add(steps(report, dialog).apply { alignmentX = JComponent.LEFT_ALIGNMENT })
        }
        root.add(intro, BorderLayout.NORTH)

        val preview = JTextArea(report.discordText).apply {
            isEditable = false
            font = mono
            lineWrap = false
            caretPosition = 0
            background = Color(0x1e, 0x1e, 0x1e)
            foreground = Color(0xdc, 0xdc, 0xdc)
            border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
        }
        val previewPanel = JPanel(BorderLayout(0, 4)).apply {
            add(JLabel("This is what gets shared (no account name or password):").apply { font = small }, BorderLayout.NORTH)
            add(JScrollPane(preview).apply { preferredSize = Dimension(640, 220) }, BorderLayout.CENTER)
        }
        root.add(previewPanel, BorderLayout.CENTER)

        val footer = JPanel(BorderLayout()).apply {
            val fileNote = if (file != null) "Full report saved as ${file.name}. If a developer asks for it, drag that file into Discord." else "The full report could not be saved to disk."
            add(JLabel("<html>$fileNote</html>").apply { font = small; foreground = Color.GRAY }, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
                if (file != null) add(JButton("Show file").apply { font = small; addActionListener { reveal(file) } })
                add(JButton(if (mode == Mode.CRASH) "Close game" else "Close").apply { font = small; addActionListener { dialog.dispose() } })
            }, BorderLayout.EAST)
        }
        root.add(footer, BorderLayout.SOUTH)

        dialog.contentPane = root
        dialog.pack()
        dialog.minimumSize = dialog.size
        dialog.setLocationRelativeTo(null)
        dialog.isAlwaysOnTop = mode == Mode.CRASH
        dialog.isVisible = true
        dialog.toFront()
    }

    private fun steps(report: CrashReport, dialog: JDialog): JPanel {
        val copy = JButton("1.  Copy crash report").apply {
            font = body.deriveFont(Font.BOLD)
            preferredSize = Dimension(300, 40)
            addActionListener {
                val copied = copyToClipboard(report.discordText)
                text = if (copied) "1.  Copied!  Now open Discord" else "1.  Copy failed, use the text below"
                isEnabled = copied.not()
            }
        }
        val discord = JButton("2.  Open Discord").apply {
            font = body.deriveFont(Font.BOLD)
            preferredSize = Dimension(300, 40)
            addActionListener {
                if (!openUrl(CrashHandler.DISCORD_INVITE)) {
                    text = "2.  Go to ${CrashHandler.DISCORD_INVITE}"
                }
                dialog.isAlwaysOnTop = false
            }
        }
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(row(copy, "puts a short report on your clipboard"))
            add(Box.createVerticalStrut(6))
            add(row(discord, "then go to the ${CrashHandler.DISCORD_CHANNEL} channel"))
            add(Box.createVerticalStrut(6))
            add(row(JLabel("3.  Paste it there (Ctrl+V) and say what you were doing.").apply {
                font = body.deriveFont(Font.BOLD)
                horizontalAlignment = SwingConstants.LEFT
                border = BorderFactory.createEmptyBorder(10, 14, 10, 0)
            }, "that's it, thank you!"))
        }
    }

    private fun row(control: Component, hint: String): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply {
        alignmentX = JComponent.LEFT_ALIGNMENT
        add(control)
        add(JLabel(hint).apply { font = small; foreground = Color.GRAY })
    }

    private fun copyToClipboard(text: String): Boolean = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        true
    }.getOrDefault(false)

    private fun openUrl(url: String): Boolean = runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            true
        } else {
            openWithSystemCommand(url)
        }
    }.getOrElse { openWithSystemCommand(url) }

    private fun reveal(file: File) {
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.parentFile)
            } else {
                openWithSystemCommand(file.parentFile.absolutePath)
            }
        }.onFailure { openWithSystemCommand(file.parentFile.absolutePath) }
    }

    private fun openWithSystemCommand(target: String): Boolean = runCatching {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.startsWith("windows") -> listOf("rundll32", "url.dll,FileProtocolHandler", target)
            os.startsWith("mac") -> listOf("open", target)
            else -> listOf("xdg-open", target)
        }
        ProcessBuilder(command).start()
        true
    }.getOrDefault(false)
}
