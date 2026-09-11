package com.jagex.game.runetek5.client.input.mouse

import org.lwjgl.system.JNI
import org.lwjgl.system.Library
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Pointer
import org.lwjgl.system.SharedLibrary
import org.lwjgl.system.linux.X11
import java.awt.Component
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.lang.reflect.Field
import java.lang.reflect.Method

class X11ArgbCursors private constructor(
    private val xcursor: SharedLibrary,
    private val display: Long,
    private val setPData: Method,
    private val componentPeer: Field?,
    private val peerWindow: Method?,
) {
    private val imageCreate = xcursor.getFunctionAddress("XcursorImageCreate")
    private val imageLoadCursor = xcursor.getFunctionAddress("XcursorImageLoadCursor")
    private val imageDestroy = xcursor.getFunctionAddress("XcursorImageDestroy")
    private val sync = X11.getLibrary().getFunctionAddress("XSync")
    private val flush = X11.getLibrary().getFunctionAddress("XFlush")
    private val defineCursor = X11.getLibrary().getFunctionAddress("XDefineCursor")
    private val undefineCursor = X11.getLibrary().getFunctionAddress("XUndefineCursor")

    fun create(argb: IntArray, width: Int, height: Int, hotspot: Point): Cursor? {
        if (width <= 0 || height <= 0 || hotspot.x !in 0 until width || hotspot.y !in 0 until height) {
            return null
        }
        val image = JNI.invokeP(width, height, imageCreate)
        if (image == MemoryUtil.NULL) {
            return null
        }
        val handle = try {
            MemoryUtil.memPutInt(image + XHOT_OFFSET, hotspot.x)
            MemoryUtil.memPutInt(image + YHOT_OFFSET, hotspot.y)
            val pixels = MemoryUtil.memIntBuffer(MemoryUtil.memGetAddress(image + PIXELS_OFFSET), width * height)
            for (i in 0 until width * height) {
                pixels.put(i, premultiply(argb[i]))
            }
            JNI.invokePPN(display, image, imageLoadCursor)
        } finally {
            JNI.invokePV(image, imageDestroy)
        }
        if (handle == 0L) {
            return null
        }
        JNI.invokePI(display, 0, sync)
        val cursor = ArgbCursor(handle)
        setPData.invoke(cursor, handle)
        return cursor
    }

    fun define(component: Component, cursor: Cursor?) {
        val window = windowOf(component)
        if (window == 0L) {
            return
        }
        when (cursor) {
            is ArgbCursor -> JNI.invokePNNI(display, window, cursor.handle, defineCursor)
            null -> JNI.invokePJI(display, window, undefineCursor)
            else -> return
        }
        JNI.invokePI(display, flush)
    }

    private fun windowOf(component: Component): Long {
        val peer = componentPeer?.get(component) ?: return 0L
        return try {
            peerWindow?.invoke(peer) as? Long ?: 0L
        } catch (exception: ReflectiveOperationException) {
            0L
        }
    }

    private class ArgbCursor(val handle: Long) : Cursor("darkan-argb")

    companion object {
        private const val XHOT_OFFSET = 16L
        private const val YHOT_OFFSET = 20L
        private const val PIXELS_OFFSET = 32L

        fun createOrNull(): X11ArgbCursors? {
            if (!Pointer.BITS64 || Toolkit.getDefaultToolkit().javaClass.name != "sun.awt.X11.XToolkit") {
                return null
            }
            return try {
                val setPData = Cursor::class.java.getDeclaredMethod("setPData", Long::class.javaPrimitiveType)
                setPData.isAccessible = true
                val xcursor = Library.loadNative(X11ArgbCursors::class.java, "org.lwjgl", "libXcursor.so.1")
                val display = X11.XOpenDisplay(null as CharSequence?)
                if (display == MemoryUtil.NULL) {
                    xcursor.free()
                    null
                } else {
                    X11ArgbCursors(xcursor, display, setPData, peerField(), peerWindowMethod())
                }
            } catch (throwable: Throwable) {
                System.err.println("ARGB cursors unavailable, using AWT's two-colour cursors: $throwable")
                null
            }
        }

        private fun peerField(): Field? = try {
            Component::class.java.getDeclaredField("peer").apply { isAccessible = true }
        } catch (throwable: RuntimeException) {
            null
        }

        private fun peerWindowMethod(): Method? = try {
            Class.forName("sun.awt.X11.XBaseWindow").getMethod("getWindow").also { it.isAccessible = true }
        } catch (throwable: Exception) {
            System.err.println("Cursor updates follow AWT only, the canvas window is not reachable: $throwable")
            null
        }

        private fun premultiply(argb: Int): Int {
            val alpha = argb ushr 24
            if (alpha == 0xff) {
                return argb
            }
            if (alpha == 0) {
                return 0
            }
            val red = ((argb shr 16) and 0xff) * alpha / 0xff
            val green = ((argb shr 8) and 0xff) * alpha / 0xff
            val blue = (argb and 0xff) * alpha / 0xff
            return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
}
