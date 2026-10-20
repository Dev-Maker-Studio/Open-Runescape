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

class RenderState {

    private var depthTestEnabled = false
    private var depthWriteEnabled = true
    private var depthFunc = DepthFunc.LESS

    private var blendEnabled = false
    private var blendMode = BlendMode.NONE

    private var cullFaceEnabled = false
    private var cullFace = CullFace.BACK
    private var frontFace = FrontFace.CCW

    private var scissorEnabled = false
    private var scissorX = 0
    private var scissorY = 0
    private var scissorWidth = 0
    private var scissorHeight = 0

    private var colorMaskR = true
    private var colorMaskG = true
    private var colorMaskB = true
    private var colorMaskA = true

    private var viewportX = 0
    private var viewportY = 0
    private var viewportWidth = 0
    private var viewportHeight = 0

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

    fun setDepthWrite(enabled: Boolean) {
        if (depthWriteEnabled != enabled) {
            depthWriteEnabled = enabled
            GLWrapper.depthMask(enabled)
        }
    }

    fun setDepthFunc(func: DepthFunc) {
        if (depthFunc != func) {
            depthFunc = func
            GLWrapper.depthFunc(func.glValue)
        }
    }

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

    fun setBlendFunc(srcFactor: Int, dstFactor: Int) {
        if (!blendEnabled) {
            blendEnabled = true
            GLWrapper.enable(BLEND)
        }
        blendMode = BlendMode.CUSTOM
        GLWrapper.blendFunc(srcFactor, dstFactor)
    }

    fun setBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        if (!blendEnabled) {
            blendEnabled = true
            GLWrapper.enable(BLEND)
        }
        blendMode = BlendMode.CUSTOM
        GLWrapper.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

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

    fun setFrontFace(face: FrontFace) {
        if (frontFace != face) {
            frontFace = face
            GLWrapper.frontFace(face.glValue)
        }
    }

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

    fun clearScissor() {
        if (scissorEnabled) {
            scissorEnabled = false
            GLWrapper.disable(SCISSOR_TEST)
        }
    }

    fun setColorMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean) {
        if (colorMaskR != r || colorMaskG != g || colorMaskB != b || colorMaskA != a) {
            colorMaskR = r
            colorMaskG = g
            colorMaskB = b
            colorMaskA = a
            GLWrapper.colorMask(r, g, b, a)
        }
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        if (viewportX != x || viewportY != y || viewportWidth != width || viewportHeight != height) {
            viewportX = x
            viewportY = y
            viewportWidth = width
            viewportHeight = height
            GLWrapper.viewport(x, y, width, height)
        }
    }

    fun getViewport(): IntArray = intArrayOf(viewportX, viewportY, viewportWidth, viewportHeight)

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

    fun setOpaqueState() {
        setDepthTest(true)
        setDepthWrite(true)
        setBlendMode(BlendMode.NONE)
        setCullFace(CullFace.BACK)
    }

    fun setTransparentState() {
        setDepthTest(true)
        setDepthWrite(false)
        setBlendMode(BlendMode.ALPHA)
        setCullFace(CullFace.BACK)
    }

    fun setUIState() {
        setDepthTest(false)
        setDepthWrite(false)
        setBlendMode(BlendMode.ALPHA)
        setCullFace(CullFace.NONE)
    }

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

enum class DepthFunc(val glValue: Int) {
    LESS(GLWrapper.Constants.LESS),
    LEQUAL(GLWrapper.Constants.LEQUAL),
    GREATER(GLWrapper.Constants.GREATER),
    ALWAYS(GLWrapper.Constants.ALWAYS)
}

enum class BlendMode {
    NONE,
    ALPHA,
    ADDITIVE,
    MULTIPLY,
    PREMULTIPLIED,
    CUSTOM
}

enum class CullFace(val glValue: Int) {
    NONE(0),
    FRONT(GLWrapper.Constants.FRONT),
    BACK(GLWrapper.Constants.BACK),
    FRONT_AND_BACK(GLWrapper.Constants.FRONT_AND_BACK)
}

enum class FrontFace(val glValue: Int) {
    CCW(GLWrapper.Constants.CCW),
    CW(GLWrapper.Constants.CW)
}

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
