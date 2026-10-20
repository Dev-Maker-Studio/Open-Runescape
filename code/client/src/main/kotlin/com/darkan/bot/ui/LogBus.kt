package com.darkan.bot.ui

import java.util.concurrent.CopyOnWriteArrayList

object LogBus {
    class Line(val timestamp: Long, val text: String, val isError: Boolean)

    @Volatile
    var capacity = 2000
        set(value) {
            field = value.coerceIn(100, 50_000)
            synchronized(history) { trim() }
        }

    private val history = ArrayDeque<Line>()
    private val listeners = CopyOnWriteArrayList<(Line) -> Unit>()

    fun publish(text: String, isError: Boolean) {
        val line = Line(System.currentTimeMillis(), text.trimEnd('\n', '\r'), isError)
        synchronized(history) {
            history.addLast(line)
            trim()
        }
        listeners.forEach { it(line) }
    }

    fun subscribe(listener: (Line) -> Unit): List<Line> {
        val snapshot = synchronized(history) { history.toList() }
        listeners.add(listener)
        return snapshot
    }

    fun clear() = synchronized(history) { history.clear() }

    fun snapshot(): List<Line> = synchronized(history) { history.toList() }

    private fun trim() {
        while (history.size > capacity) history.removeFirst()
    }
}
