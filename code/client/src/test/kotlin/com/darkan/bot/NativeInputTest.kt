package com.darkan.bot

import org.junit.Assert.*
import org.junit.Test
import java.awt.Canvas
import java.awt.event.*

class NativeInputTest {
    private fun event(seq: Int, kind: Int, button: Int = 0, x: Double = 0.0, y: Double = 0.0, code: String = "") =
        RemoteInput.parse(listOf(seq.toString(), kind.toString(), button.toString(), x.toString(), y.toString(), code))

    @Test fun mapsKeyboardCodesWithoutImplicitTyping() {
        val canvas = Canvas()
        val pressed = ArrayList<KeyEvent>()
        val released = ArrayList<KeyEvent>()
        val typed = StringBuilder()
        canvas.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) { pressed.add(e) }
            override fun keyReleased(e: KeyEvent) { released.add(e) }
            override fun keyTyped(e: KeyEvent) { typed.append(e.keyChar) }
        })
        val input = CanvasInput { canvas }
        input.apply(event(1, 0, code = "ShiftLeft"))
        input.apply(event(2, 0, code = "KeyA"))
        assertEquals(KeyEvent.VK_A, pressed.last().keyCode)
        assertTrue(pressed.last().isShiftDown)
        assertEquals("", typed.toString())
        input.apply(event(3, 0, code = "Text:A"))
        assertEquals("A", typed.toString())
        input.apply(event(4, 1, code = "KeyA"))
        assertEquals(KeyEvent.VK_A, released.last().keyCode)
        assertEquals(KeyEvent.VK_LEFT, RemoteInput.key("ArrowLeft"))
        assertEquals(KeyEvent.VK_ENTER, RemoteInput.key("Enter"))
        assertEquals(KeyEvent.VK_BACK_SPACE, RemoteInput.key("Backspace"))
        assertEquals(KeyEvent.VK_CONTROL, RemoteInput.key("ControlRight"))
        assertEquals(KeyEvent.VK_F12, RemoteInput.key("F12"))
        assertEquals(4L, input.appliedSequence)
    }

    @Test fun coordinatesScaleToNativeSurfaceAndWheelUsesLastPosition() {
        val canvas = Canvas().apply { setSize(800, 600) }
        val moves = ArrayList<MouseEvent>()
        val clicks = ArrayList<MouseEvent>()
        val wheels = ArrayList<MouseWheelEvent>()
        canvas.addMouseMotionListener(object : MouseMotionAdapter() { override fun mouseMoved(e: MouseEvent) { moves.add(e) } })
        canvas.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { clicks.add(e) }
            override fun mouseReleased(e: MouseEvent) { clicks.add(e) }
        })
        canvas.addMouseWheelListener { wheels.add(it) }
        val input = CanvasInput { canvas }
        input.apply(event(1, 6, x = 1600.0, y = 1200.0))
        input.apply(event(2, 2, x = 1000.0, y = 700.0))
        assertEquals(500, moves.single().x)
        assertEquals(350, moves.single().y)
        input.apply(event(3, 3, button = 2, x = 1000.0, y = 700.0))
        assertEquals(MouseEvent.BUTTON3, clicks.single().button)
        assertEquals(500, clicks.single().x)
        input.apply(event(4, 5, y = -120.0))
        assertEquals(-1, wheels.single().wheelRotation)
        assertEquals(500, wheels.single().x)
        input.apply(event(5, 7))
        assertEquals(MouseEvent.MOUSE_RELEASED, clicks.last().id)
    }

    @Test fun focusAndSequencesAdvanceOnlyAfterApplication() {
        val canvas = Canvas()
        val input = CanvasInput { canvas }
        var seen = -1L
        canvas.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) { seen = input.appliedSequence }
        })
        input.apply(event(1, 7, x = 1.0))
        assertEquals(0L, seen)
        assertEquals(1L, input.appliedSequence)
        assertThrows(IllegalArgumentException::class.java) { input.apply(event(1, 7)) }
        input.reset()
        assertEquals(0L, input.appliedSequence)
        canvas.addFocusListener(object : FocusAdapter() { override fun focusGained(e: FocusEvent) { error("listener failed") } })
        assertThrows(IllegalStateException::class.java) { input.apply(event(1, 7, x = 1.0)) }
        assertEquals(0L, input.appliedSequence)
    }

    @Test fun malformedInputIsRejected() {
        val invalid = listOf(
            listOf("0", "0", "0", "0", "0", "KeyA"),
            listOf("1", "8", "0", "0", "0"),
            listOf("1", "2", "0", "NaN", "0"),
            listOf("1", "3", "7", "0", "0"),
            listOf("1", "6", "0", "0", "600"),
            listOf("1", "7", "0", "2", "0"),
            listOf("1", "0", "0", "0", "0", "Unknown"),
            listOf("1", "1", "0", "0", "0", "Text:a"),
            listOf("1", "0", "0", "0", "0", "Text:" + "a".repeat(129)),
            listOf("1", "0", "0", "0", "0", "Text:😀")
        )
        invalid.forEach { args -> assertThrows(RuntimeException::class.java) { RemoteInput.parse(args) } }
    }
}
