package com.jagex.game.runetek5.toolkit.context

import com.jagex.game.runetek5.client.input.mouse.X11ArgbCursors
import org.lwjgl.PointerBuffer
import org.lwjgl.opengl.GLX
import org.lwjgl.opengl.GLX13
import org.lwjgl.opengl.GLXARBCreateContext
import org.lwjgl.opengl.GLXEXTSwapControl
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.linux.X11
import java.awt.Canvas

class GlxContext {
    private var display = 0L
    private var context = 0L
    private var drawable = 0L

    fun surface(canvas: Canvas): Long = X11ArgbCursors.windowOf(canvas)

    fun create(canvas: Canvas, red: Int, green: Int, blue: Int, depth: Int, major: Int, minor: Int, debug: Boolean): Long {
        display = X11.XOpenDisplay(null as CharSequence?)
        val window = surface(canvas)
        if (display == NULL || window == 0L) {
            return 0L
        }
        val config = chooseConfig(red, green, blue, depth)
        context = MemoryStack.stackPush().use {
            val attribs = it.ints(
                GLX_CONTEXT_MAJOR_VERSION, major, GLX_CONTEXT_MINOR_VERSION, minor,
                GLX_CONTEXT_PROFILE_MASK, GLX_CONTEXT_CORE_PROFILE_BIT,
                GLX_CONTEXT_FLAGS, GLX_CONTEXT_FORWARD_COMPATIBLE_BIT or (if (debug) GLX_CONTEXT_DEBUG_BIT else 0), 0
            )
            GLXARBCreateContext.glXCreateContextAttribsARB(display, config, NULL, true, attribs)
        }
        if (context == NULL) {
            return 0L
        }
        drawable = window
        return window
    }

    private fun chooseConfig(red: Int, green: Int, blue: Int, depth: Int): Long = MemoryStack.stackPush().use {
        val attribs = it.ints(
            GLX13.GLX_X_RENDERABLE, 1, GLX13.GLX_DRAWABLE_TYPE, GLX13.GLX_WINDOW_BIT, GLX13.GLX_RENDER_TYPE, GLX13.GLX_RGBA_BIT,
            GLX.GLX_DOUBLEBUFFER, 1, GLX.GLX_RED_SIZE, red, GLX.GLX_GREEN_SIZE, green, GLX.GLX_BLUE_SIZE, blue, GLX.GLX_DEPTH_SIZE, depth, 0
        )
        val configs: PointerBuffer = GLX13.glXChooseFBConfig(display, X11.XDefaultScreen(display), attribs)
            ?: throw IllegalStateException("No GLX framebuffer configuration")
        configs.get(0)
    }

    fun makeCurrent(surface: Long): Boolean {
        drawable = surface
        return GLX.glXMakeCurrent(display, surface, context)
    }

    fun releaseCurrent() {
        GLX.glXMakeCurrent(display, 0L, NULL)
    }

    fun swap(surface: Long) {
        GLX.glXSwapBuffers(display, surface)
    }

    fun swapInterval(interval: Int) {
        runCatching { GLXEXTSwapControl.glXSwapIntervalEXT(display, drawable, interval) }
    }

    fun destroy() {
        GLX.glXDestroyContext(display, context)
        context = 0
    }

    private companion object {
        const val GLX_CONTEXT_MAJOR_VERSION = 0x2091
        const val GLX_CONTEXT_MINOR_VERSION = 0x2092
        const val GLX_CONTEXT_FLAGS = 0x2094
        const val GLX_CONTEXT_PROFILE_MASK = 0x9126
        const val GLX_CONTEXT_CORE_PROFILE_BIT = 1
        const val GLX_CONTEXT_DEBUG_BIT = 2
        const val GLX_CONTEXT_FORWARD_COMPATIBLE_BIT = 1
    }
}
