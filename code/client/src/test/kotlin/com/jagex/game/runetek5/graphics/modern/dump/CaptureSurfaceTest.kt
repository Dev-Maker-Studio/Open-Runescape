package com.jagex.game.runetek5.graphics.modern.dump

import java.awt.Canvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CaptureSurfaceTest {
    private fun canvas(width: Int = 1280, height: Int = 720) = Canvas().apply { setSize(width, height) }

    @Test
    fun capturesWholeDrawableDimensionsRatherThanTheLastViewport() {
        val canvas = canvas()
        val surface = CaptureSurface(canvas, 0)
        assertEquals(1280, surface.width)
        assertEquals(720, surface.height)
        canvas.setLocation(50, 100)
        surface.validate(canvas)
    }

    @Test
    fun rejectsReplacementEvenWhenDimensionsAreIdentical() {
        val canvas = canvas()
        val surface = CaptureSurface(canvas, 0)
        surface.validate(canvas)
        assertThrows(IllegalArgumentException::class.java) { surface.validate(canvas()) }
        assertThrows(IllegalArgumentException::class.java) { surface.validate(null) }
    }

    @Test
    fun rejectsResizedDrawableBeforeAnySparseReadback() {
        val canvas = canvas()
        val surface = CaptureSurface(canvas, 0)
        canvas.setSize(1281, 720)
        assertThrows(IllegalArgumentException::class.java) { surface.validate(canvas) }
        assertEquals(1280, surface.width)
        assertEquals(720, surface.height)
        canvas.setSize(1280, 721)
        assertThrows(IllegalArgumentException::class.java) { surface.validate(canvas) }
    }

    @Test
    fun rejectsMissingCanvasAndOffscreenStartWithoutViewportFallback() {
        assertThrows(IllegalArgumentException::class.java) { CaptureSurface(null, 0) }
        val failure = assertThrows(IllegalArgumentException::class.java) { CaptureSurface(canvas(), 7) }
        assertEquals("capture requires the default drawable framebuffer", failure.message)
    }

    @Test
    fun rejectsEmptyAndOversizedDrawablesBeforeWriterAllocation() {
        for ((width, height) in listOf(0 to 720, 1280 to 0, 8193 to 1, 1 to 8193)) {
            assertThrows(IllegalArgumentException::class.java) { CaptureSurface(canvas(width, height), 0) }
        }
        val limit = CaptureSurface(canvas(8192, 8192), 0)
        assertEquals(8192, limit.width)
        assertEquals(8192, limit.height)
    }
}
