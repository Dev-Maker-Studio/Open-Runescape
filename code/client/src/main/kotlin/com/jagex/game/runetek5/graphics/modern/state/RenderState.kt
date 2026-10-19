package com.jagex.game.runetek5.graphics.modern.state

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.BACK
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.BLEND
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.CCW
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.CULL_FACE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.CW
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DEPTH_TEST
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DST_COLOR
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FRONT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.LEQUAL
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.LESS
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.ONE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.ONE_MINUS_SRC_ALPHA
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.SCISSOR_TEST
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.SRC_ALPHA
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.ZERO

/**
 * OpenGL render state manager.
 *
 * Tracks current OpenGL state and minimizes redundant state changes.
 * All state changes go through this manager to ensure consistency
 * and optimal performance.
 *
 * Usage:
 * ```kotlin
 * val state = RenderState()
 * state.setDepthTest(true)
 * state.setBlendMode(BlendMode.ALPHA)
 * state.setCullFace(CullFace.BACK)
 * // ... render ...
 * state.reset() // Restore defaults
 * ```
 */
class RenderState {

    // Depth state
    private var depthTestEnabled = false
    private var depthWriteEnabled = true
    private var depthFunc = DepthFunc.LESS

    // Blend state
    private var blendEnabled = false
    private var blendMode = BlendMode.NONE

    // Cull state
    private var cullFaceEnabled = false
    private var cullFace = CullFace.BACK
    private var frontFace = FrontFace.CCW

    // Scissor state
    private var scissorEnabled = false
    private var scissorX = 0
    private var scissorY = 0
    private var scissorWidth = 0
    private var scissorHeight = 0

    // Color mask
    private var colorMaskR = true
    private var colorMaskG = true
    private var colorMaskB = true
    private var colorMaskA = true

    // Viewport
    private var viewportX = 0
    private var viewportY = 0
    private var viewportWidth = 0
    private var viewportHeight = 0

    // ===== Depth State =====

    /**
     * Enables or disables depth testing.
     */
    fun setDepthTest(enabled: Boolean) {
        if (depthTestEnabled != enabled) {
            depthTestEnabled = enabled
            if (enabled) {
                GLWrapper.enable(DEPTH_TEST)
            } else {
                GLWrapper.disable(DEPTH_TEST)
            }
        }
    }

    /**
     * Enables or disables depth buffer writing.
     */
    fun setDepthWrite(enabled: Boolean) {
        if (depthWriteEnabled != enabled) {
            depthWriteEnabled = enabled
            GLWrapper.depthMask(enabled)
        }
    }

    /**
     * Sets the depth comparison function.
     */
    fun setDepthFunc(func: DepthFunc) {
        if (depthFunc != func) {
            depthFunc = func
            GLWrapper.depthFunc(func.glValue)
        }
    }

    // ===== Blend State =====

    /**
     * Sets the blend mode.
     * Use BlendMode.NONE to disable blending.
     */
    fun setBlendMode(mode: BlendMode) {
        if (blendMode == mode) return

        blendMode = mode

        if (mode == BlendMode.NONE) {
            if (blendEnabled) {
                blendEnabled = false
                GLWrapper.disable(BLEND)
            }
        } else {
            if (!blendEnabled) {
                blendEnabled = true
                GLWrapper.enable(BLEND)
            }

            when (mode) {
                BlendMode.ALPHA -> {
                    GLWrapper.blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)
                }
                BlendMode.ADDITIVE -> {
                    GLWrapper.blendFunc(SRC_ALPHA, ONE)
                }
                BlendMode.MULTIPLY -> {
                    GLWrapper.blendFunc(DST_COLOR, ZERO)
                }
                BlendMode.PREMULTIPLIED -> {
                    GLWrapper.blendFunc(ONE, ONE_MINUS_SRC_ALPHA)
                }
                else -> {}
            }
        }
    }

    /**
     * Sets custom blend factors.
     */
    fun setBlendFunc(srcFactor: Int, dstFactor: Int) {
        if (!blendEnabled) {
            blendEnabled = true
            GLWrapper.enable(BLEND)
        }
        blendMode = BlendMode.CUSTOM
        GLWrapper.blendFunc(srcFactor, dstFactor)
    }

    /**
     * Sets separate blend factors for RGB and alpha.
     */
    fun setBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        if (!blendEnabled) {
            blendEnabled = true
            GLWrapper.enable(BLEND)
        }
        blendMode = BlendMode.CUSTOM
        GLWrapper.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    // ===== Cull State =====

    /**
     * Sets face culling mode.
     * Use CullFace.NONE to disable culling.
     */
    fun setCullFace(face: CullFace) {
        if (cullFace == face && (face == CullFace.NONE) == !cullFaceEnabled) return

        cullFace = face

        if (face == CullFace.NONE) {
            if (cullFaceEnabled) {
                cullFaceEnabled = false
                GLWrapper.disable(CULL_FACE)
            }
        } else {
            if (!cullFaceEnabled) {
                cullFaceEnabled = true
                GLWrapper.enable(CULL_FACE)
            }
            GLWrapper.cullFace(face.glValue)
        }
    }

    /**
     * Sets the front face winding order.
     */
    fun setFrontFace(face: FrontFace) {
        if (frontFace != face) {
            frontFace = face
            GLWrapper.frontFace(face.glValue)
        }
    }

    // ===== Scissor State =====

    /**
     * Enables scissor test with the specified rectangle.
     */
    fun setScissor(x: Int, y: Int, width: Int, height: Int) {
        if (!scissorEnabled) {
            scissorEnabled = true
            GLWrapper.enable(SCISSOR_TEST)
        }
        if (scissorX != x || scissorY != y || scissorWidth != width || scissorHeight != height) {
            scissorX = x
            scissorY = y
            scissorWidth = width
            scissorHeight = height
            GLWrapper.scissor(x, y, width, height)
        }
    }

    /**
     * Disables scissor test.
     */
    fun clearScissor() {
        if (scissorEnabled) {
            scissorEnabled = false
            GLWrapper.disable(SCISSOR_TEST)
        }
    }

    // ===== Color Mask =====

    /**
     * Sets the color write mask.
     */
    fun setColorMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean) {
        if (colorMaskR != r || colorMaskG != g || colorMaskB != b || colorMaskA != a) {
            colorMaskR = r
            colorMaskG = g
            colorMaskB = b
            colorMaskA = a
            GLWrapper.colorMask(r, g, b, a)
        }
    }

    // ===== Viewport =====

    /**
     * Sets the viewport.
     */
    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        if (viewportX != x || viewportY != y || viewportWidth != width || viewportHeight != height) {
            viewportX = x
            viewportY = y
            viewportWidth = width
            viewportHeight = height
            GLWrapper.viewport(x, y, width, height)
        }
    }

    /**
     * Gets the current viewport dimensions.
     */
    fun getViewport(): IntArray = intArrayOf(viewportX, viewportY, viewportWidth, viewportHeight)

    // ===== State Management =====

    /**
     * Resets all state to defaults.
     */
    fun reset() {
        setDepthTest(true)
        setDepthWrite(true)
        setDepthFunc(DepthFunc.LESS)
        setBlendMode(BlendMode.NONE)
        setCullFace(CullFace.BACK)
        setFrontFace(FrontFace.CCW)
        clearScissor()
        setColorMask(true, true, true, true)
    }

    /**
     * Applies common opaque rendering state.
     */
    fun setOpaqueState() {
        setDepthTest(true)
        setDepthWrite(true)
        setBlendMode(BlendMode.NONE)
        setCullFace(CullFace.BACK)
    }

    /**
     * Applies common transparent rendering state.
     */
    fun setTransparentState() {
        setDepthTest(true)
        setDepthWrite(false)
        setBlendMode(BlendMode.ALPHA)
        setCullFace(CullFace.BACK)
    }

    /**
     * Applies state for UI/2D rendering.
     */
    fun setUIState() {
        setDepthTest(false)
        setDepthWrite(false)
        setBlendMode(BlendMode.ALPHA)
        setCullFace(CullFace.NONE)
    }

    /**
     * Captures the current state for later restoration.
     */
    fun capture(): StateSnapshot {
        return StateSnapshot(
            depthTestEnabled = depthTestEnabled,
            depthWriteEnabled = depthWriteEnabled,
            depthFunc = depthFunc,
            blendEnabled = blendEnabled,
            blendMode = blendMode,
            cullFaceEnabled = cullFaceEnabled,
            cullFace = cullFace,
            frontFace = frontFace,
            scissorEnabled = scissorEnabled,
            scissorX = scissorX,
            scissorY = scissorY,
            scissorWidth = scissorWidth,
            scissorHeight = scissorHeight,
            colorMaskR = colorMaskR,
            colorMaskG = colorMaskG,
            colorMaskB = colorMaskB,
            colorMaskA = colorMaskA
        )
    }

    /**
     * Restores state from a snapshot.
     */
    fun restore(snapshot: StateSnapshot) {
        setDepthTest(snapshot.depthTestEnabled)
        setDepthWrite(snapshot.depthWriteEnabled)
        setDepthFunc(snapshot.depthFunc)
        setBlendMode(snapshot.blendMode)
        setCullFace(snapshot.cullFace)
        setFrontFace(snapshot.frontFace)
        if (snapshot.scissorEnabled) {
            setScissor(snapshot.scissorX, snapshot.scissorY, snapshot.scissorWidth, snapshot.scissorHeight)
        } else {
            clearScissor()
        }
        setColorMask(snapshot.colorMaskR, snapshot.colorMaskG, snapshot.colorMaskB, snapshot.colorMaskA)
    }
}

/**
 * Depth comparison functions.
 */
enum class DepthFunc(val glValue: Int) {
    LESS(GLWrapper.Constants.LESS),
    LEQUAL(GLWrapper.Constants.LEQUAL),
    GREATER(GLWrapper.Constants.GREATER),
    ALWAYS(GLWrapper.Constants.ALWAYS)
}

/**
 * Blend modes.
 */
enum class BlendMode {
    NONE,
    ALPHA,
    ADDITIVE,
    MULTIPLY,
    PREMULTIPLIED,
    CUSTOM
}

/**
 * Cull face modes.
 */
enum class CullFace(val glValue: Int) {
    NONE(0),
    FRONT(GLWrapper.Constants.FRONT),
    BACK(GLWrapper.Constants.BACK),
    FRONT_AND_BACK(GLWrapper.Constants.FRONT_AND_BACK)
}

/**
 * Front face winding order.
 */
enum class FrontFace(val glValue: Int) {
    CCW(GLWrapper.Constants.CCW),
    CW(GLWrapper.Constants.CW)
}

/**
 * Captured render state for save/restore.
 */
data class StateSnapshot(
    val depthTestEnabled: Boolean,
    val depthWriteEnabled: Boolean,
    val depthFunc: DepthFunc,
    val blendEnabled: Boolean,
    val blendMode: BlendMode,
    val cullFaceEnabled: Boolean,
    val cullFace: CullFace,
    val frontFace: FrontFace,
    val scissorEnabled: Boolean,
    val scissorX: Int,
    val scissorY: Int,
    val scissorWidth: Int,
    val scissorHeight: Int,
    val colorMaskR: Boolean,
    val colorMaskG: Boolean,
    val colorMaskB: Boolean,
    val colorMaskA: Boolean
)
