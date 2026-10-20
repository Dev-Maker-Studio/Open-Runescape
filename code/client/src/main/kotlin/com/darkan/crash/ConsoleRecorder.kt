package com.darkan.crash

import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ConsoleRecorder {
    private const val CAPACITY = 400
    private const val MAX_LINE_LENGTH = 300

    private val lines = ArrayDeque<String>(CAPACITY)
    private val stamp = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        System.setOut(PrintStream(Tee(System.out, isError = false), true))
        System.setErr(PrintStream(Tee(System.err, isError = true), true))
    }

    fun record(text: String, isError: Boolean) {
        val trimmed = text.trimEnd('\n', '\r')
        if (trimmed.isEmpty()) return
        val alreadyStamped = trimmed.length > 10 && trimmed[4] == '-' && trimmed[7] == '-' && trimmed[10] == ' '
        val prefix = if (alreadyStamped) "" else stamp.format(LocalTime.now()) + " "
        val marker = if (isError) "! " else "  "
        val line = (prefix + marker + trimmed).take(MAX_LINE_LENGTH)
        synchronized(lines) {
            if (lines.size >= CAPACITY) lines.removeFirst()
            lines.addLast(line)
        }
    }

    fun snapshot(): List<String> = synchronized(lines) { lines.toList() }

    private class Tee(private val target: PrintStream, private val isError: Boolean) : OutputStream() {
        private val buffer = StringBuilder()

        override fun write(b: Int) {
            target.write(b)
            val ch = b.toChar()
            if (ch == '\n') flushLine() else buffer.append(ch)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            target.write(b, off, len)
            val text = String(b, off, len, Charsets.UTF_8)
            var start = 0
            while (true) {
                val newline = text.indexOf('\n', start)
                if (newline == -1) {
                    buffer.append(text, start, text.length)
                    break
                }
                buffer.append(text, start, newline)
                flushLine()
                start = newline + 1
            }
        }

        override fun flush() = target.flush()

        private fun flushLine() {
            if (buffer.isNotEmpty()) {
                record(buffer.toString(), isError)
                buffer.setLength(0)
            }
        }
    }
}
