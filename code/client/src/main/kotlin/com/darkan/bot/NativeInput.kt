package com.darkan.bot

import com.jagex.game.runetek5.client.GameShell
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.awt.Canvas
import java.awt.EventQueue
import java.awt.event.*
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.roundToInt

internal data class RemoteInput(val seq: Long, val kind: Int, val button: Int, val x: Double, val y: Double, val code: String) {
    companion object {
        fun parse(args: List<String>): RemoteInput {
            require(args.size in 5..6) { "usage: remote-input <seq> <kind> <button> <x> <y> [code]" }
            val seq = args[0].toLongOrNull()
            require(seq != null && seq in 1..0xffffffffL) { "seq must be a nonzero u32" }
            val kind = args[1].toIntOrNull()
            require(kind != null && kind in 0..7) { "kind must be from 0 to 7" }
            val button = args[2].toIntOrNull()
            require(button != null && button in 0..7) { "invalid button" }
            val x = args[3].toDoubleOrNull()
            val y = args[4].toDoubleOrNull()
            require(x != null && y != null && x.isFinite() && y.isFinite() && x in -32768.0..32768.0 && y in -32768.0..32768.0) { "invalid input coordinates" }
            val code = args.getOrElse(5) { "" }
            if (kind <= 1) {
                require(button == 0 && x == 0.0 && y == 0.0) { "invalid key fields" }
                if (code.startsWith("Text:")) {
                    val text = code.drop(5)
                    require(kind == 0 && text.isNotEmpty() && text.toByteArray(Charsets.UTF_8).size <= 128 && text.none { it.isISOControl() }) { "invalid Text: input (maximum 128 UTF-8 text bytes, key down only)" }
                    require(text.none { it.isSurrogate() }) { "supplementary Unicode is not supported by the native client" }
                    require(text.all { com.jagex.game.runetek5.client.input.keyboard.CanvasKeyRecorder.isValidKeyChar(it) }) { "text contains characters unsupported by the native client" }
                } else key(code)
            } else {
                require(code.isEmpty()) { "code is only valid for key events" }
                require(when (kind) { 3, 4 -> button <= 2; 2 -> true; else -> button == 0 }) { "invalid button for event kind" }
                if (kind == 6) require(x in 1.0..16384.0 && y in 1.0..16384.0) { "invalid viewport size" }
                if (kind == 7) require(x in listOf(0.0, 1.0) && y == 0.0) { "focus requires 0 or 1 and zero y" }
            }
            return RemoteInput(seq, kind, button, x, y, code)
        }

        fun key(code: String): Int = when {
            code.length == 4 && code.startsWith("Key") && code.last() in 'A'..'Z' -> code.last().code
            code.length == 6 && code.startsWith("Digit") && code.last() in '0'..'9' -> code.last().code
            code.length == 7 && code.startsWith("Numpad") && code.last() in '0'..'9' -> KeyEvent.VK_NUMPAD0 + code.last().digitToInt()
            code.startsWith("F") && code.drop(1).toIntOrNull() in 1..12 -> KeyEvent.VK_F1 + code.drop(1).toInt() - 1
            else -> keys[code] ?: error("unsupported KeyboardEvent.code $code")
        }

        private val keys = mapOf(
            "ArrowLeft" to KeyEvent.VK_LEFT, "ArrowRight" to KeyEvent.VK_RIGHT,
            "ArrowUp" to KeyEvent.VK_UP, "ArrowDown" to KeyEvent.VK_DOWN,
            "Enter" to KeyEvent.VK_ENTER, "NumpadEnter" to KeyEvent.VK_ENTER,
            "Backspace" to KeyEvent.VK_BACK_SPACE, "Tab" to KeyEvent.VK_TAB,
            "Escape" to KeyEvent.VK_ESCAPE, "Space" to KeyEvent.VK_SPACE,
            "ShiftLeft" to KeyEvent.VK_SHIFT, "ShiftRight" to KeyEvent.VK_SHIFT,
            "ControlLeft" to KeyEvent.VK_CONTROL, "ControlRight" to KeyEvent.VK_CONTROL,
            "AltLeft" to KeyEvent.VK_ALT, "AltRight" to KeyEvent.VK_ALT,
            "MetaLeft" to KeyEvent.VK_META, "MetaRight" to KeyEvent.VK_META,
            "Home" to KeyEvent.VK_HOME, "End" to KeyEvent.VK_END,
            "PageUp" to KeyEvent.VK_PAGE_UP, "PageDown" to KeyEvent.VK_PAGE_DOWN,
            "Insert" to KeyEvent.VK_INSERT, "Delete" to KeyEvent.VK_DELETE,
            "CapsLock" to KeyEvent.VK_CAPS_LOCK, "NumLock" to KeyEvent.VK_NUM_LOCK,
            "ScrollLock" to KeyEvent.VK_SCROLL_LOCK, "Pause" to KeyEvent.VK_PAUSE,
            "Minus" to KeyEvent.VK_MINUS, "Equal" to KeyEvent.VK_EQUALS,
            "BracketLeft" to KeyEvent.VK_OPEN_BRACKET, "BracketRight" to KeyEvent.VK_CLOSE_BRACKET,
            "Backslash" to KeyEvent.VK_BACK_SLASH, "Semicolon" to KeyEvent.VK_SEMICOLON,
            "Quote" to KeyEvent.VK_QUOTE, "Backquote" to KeyEvent.VK_BACK_QUOTE,
            "Comma" to KeyEvent.VK_COMMA, "Period" to KeyEvent.VK_PERIOD, "Slash" to KeyEvent.VK_SLASH,
            "NumpadAdd" to KeyEvent.VK_ADD, "NumpadSubtract" to KeyEvent.VK_SUBTRACT,
            "NumpadMultiply" to KeyEvent.VK_MULTIPLY, "NumpadDivide" to KeyEvent.VK_DIVIDE,
            "NumpadDecimal" to KeyEvent.VK_DECIMAL
        )
    }
}

internal class CanvasInput(private val canvas: () -> Canvas) {
    private var width = 0.0
    private var height = 0.0
    private var mouseX = 0
    private var mouseY = 0
    private var buttons = 0
    private val held = HashSet<String>()
    @Volatile var appliedSequence = 0L
        private set

    private fun modifiers(): Int = held.fold(0) { mask, code -> mask or when {
        code.startsWith("Shift") -> InputEvent.SHIFT_DOWN_MASK
        code.startsWith("Control") -> InputEvent.CTRL_DOWN_MASK
        code.startsWith("Alt") -> InputEvent.ALT_DOWN_MASK
        code.startsWith("Meta") -> InputEvent.META_DOWN_MASK
        else -> 0
    } }

    private fun mouse(c: Canvas, id: Int, button: Int, mask: Int = buttons): MouseEvent {
        var mods = modifiers()
        if (mask and 1 != 0) mods = mods or InputEvent.BUTTON1_DOWN_MASK
        if (mask and 2 != 0) mods = mods or InputEvent.BUTTON3_DOWN_MASK
        if (mask and 4 != 0) mods = mods or InputEvent.BUTTON2_DOWN_MASK
        return MouseEvent(c, id, System.currentTimeMillis(), mods, mouseX, mouseY, 0, 0, if (button == 0) 0 else 1, false, button)
    }

    fun reset() {
        val c = canvas()
        blur(c)
        width = 0.0
        height = 0.0
        appliedSequence = 0
    }

    private fun blur(c: Canvas) {
        for ((bit, button) in listOf(1 to 1, 2 to 3, 4 to 2)) if (buttons and bit != 0) {
            buttons = buttons and bit.inv()
            val e = mouse(c, MouseEvent.MOUSE_RELEASED, button)
            c.mouseListeners.forEach { it.mouseReleased(e) }
        }
        held.clear()
        val event = FocusEvent(c, FocusEvent.FOCUS_LOST)
        c.focusListeners.forEach { it.focusLost(event) }
    }

    fun apply(input: RemoteInput) {
        require(input.seq > appliedSequence) { "input sequence must increase (last $appliedSequence)" }
        val c = canvas()
        val now = System.currentTimeMillis()
        when (input.kind) {
            0, 1 -> {
                if (input.code.startsWith("Text:")) {
                    input.code.drop(5).forEach { ch ->
                        val event = KeyEvent(c, KeyEvent.KEY_TYPED, now, modifiers(), KeyEvent.VK_UNDEFINED, ch)
                        c.keyListeners.forEach { it.keyTyped(event) }
                    }
                } else {
                    val down = input.kind == 0
                    if (down) held.add(input.code) else held.remove(input.code)
                    val modifier = input.code.startsWith("Shift") || input.code.startsWith("Control") || input.code.startsWith("Alt") || input.code.startsWith("Meta")
                    val location = when { modifier && input.code.endsWith("Left") -> KeyEvent.KEY_LOCATION_LEFT; modifier && input.code.endsWith("Right") -> KeyEvent.KEY_LOCATION_RIGHT; input.code.startsWith("Numpad") -> KeyEvent.KEY_LOCATION_NUMPAD; else -> KeyEvent.KEY_LOCATION_STANDARD }
                    val event = KeyEvent(c, if (down) KeyEvent.KEY_PRESSED else KeyEvent.KEY_RELEASED, now, modifiers(), RemoteInput.key(input.code), KeyEvent.CHAR_UNDEFINED, location)
                    c.keyListeners.forEach { if (down) it.keyPressed(event) else it.keyReleased(event) }
                }
            }
            2, 3, 4 -> {
                check(c.width > 0 && c.height > 0) { "native canvas has no surface" }
                mouseX = (input.x * c.width / if (width > 0) width else c.width.toDouble()).roundToInt()
                mouseY = (input.y * c.height / if (height > 0) height else c.height.toDouble()).roundToInt()
                if (input.kind == 2) {
                    buttons = input.button
                    val event = mouse(c, if (buttons == 0) MouseEvent.MOUSE_MOVED else MouseEvent.MOUSE_DRAGGED, 0)
                    c.mouseMotionListeners.forEach { if (buttons == 0) it.mouseMoved(event) else it.mouseDragged(event) }
                } else {
                    val bit = intArrayOf(1, 4, 2)[input.button]
                    buttons = if (input.kind == 3) buttons or bit else buttons and bit.inv()
                    val event = mouse(c, if (input.kind == 3) MouseEvent.MOUSE_PRESSED else MouseEvent.MOUSE_RELEASED, input.button + 1)
                    c.mouseListeners.forEach { if (input.kind == 3) it.mousePressed(event) else it.mouseReleased(event) }
                }
            }
            5 -> {
                val rotation = if (input.y == 0.0) 0 else if (input.y > 0) 1 else -1
                val event = MouseWheelEvent(c, MouseEvent.MOUSE_WHEEL, now, modifiers(), mouseX, mouseY, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation)
                c.mouseWheelListeners.forEach { it.mouseWheelMoved(event) }
            }
            6 -> { width = input.x; height = input.y }
            7 -> if (input.x == 0.0) blur(c) else {
                val event = FocusEvent(c, FocusEvent.FOCUS_GAINED)
                c.focusListeners.forEach { it.focusGained(event) }
            }
        }
        appliedSequence = input.seq
    }
}

object NativeInput {
    private val input = CanvasInput { GameShell.gameCanvas ?: error("game canvas is not ready") }
    private val lock = ReentrantLock()
    private val pending = java.util.concurrent.atomic.AtomicReference<FutureTask<Unit>?>()
    @Volatile var appliedSequence = 0L
        private set
    private var previousPulse = 0L

    @Synchronized
    fun pulse() {
        appliedSequence = previousPulse
        previousPulse = input.appliedSequence
    }

    private fun onCanvas(action: () -> Unit) {
        check(lock.tryLock()) { "remote input busy" }
        try {
            if (EventQueue.isDispatchThread()) action() else {
                val task = FutureTask { action() }
                check(pending.compareAndSet(null, task)) { "native canvas event still pending" }
                try {
                    EventQueue.invokeLater { try { task.run() } finally { pending.compareAndSet(task, null) } }
                } catch (e: Throwable) {
                    pending.compareAndSet(task, null)
                    throw e
                }
                try {
                    task.get(2, TimeUnit.SECONDS)
                    pending.compareAndSet(task, null)
                } finally { task.cancel(false) }
            }
        } finally { lock.unlock() }
    }

    fun reset() = onCanvas {
        input.reset()
        synchronized(this) { appliedSequence = 0; previousPulse = 0 }
    }

    fun release() { if (GameShell.gameCanvas != null) reset() }

    fun receive(args: List<String>) = RemoteInput.parse(args).let { event ->
        try { onCanvas { input.apply(event) } } catch (e: java.util.concurrent.ExecutionException) { throw e.cause ?: e }
        buildJsonObject { put("inputSeq", appliedSequence) }
    }
}
