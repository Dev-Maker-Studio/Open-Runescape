package com.jagex.game.runetek5.toolkit.vertexdeclaration

import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.utils.BufferUtils

class VertexDeclarationAction(
    @JvmField val openglAction: Int,
    @JvmField val slot: Int,
    @JvmField val componentCount: Int,
    @JvmField val actionFormatOpengl: GLActionFormat
) {
    @JvmField val pointerOffsetStep: Int = actionFormatOpengl.bytesPerEntry * componentCount

    init {
        if (slot >= 16) {
            throw RuntimeException()
        }
    }

    companion object {
        @JvmField val POSITION: VertexDeclarationAction
        @JvmField val NORMAL_POINTER_ACTION: VertexDeclarationAction
        @JvmField val COLOR: VertexDeclarationAction
        @JvmField val TEXCOORD_1F: VertexDeclarationAction
        @JvmField val TEXCOORD: VertexDeclarationAction
        @JvmField val TEXCOORD_3F: VertexDeclarationAction
        @JvmField val TEXCOORD_4F: VertexDeclarationAction
        @JvmField val BONE_INDICES: VertexDeclarationAction
        @JvmField val GENERIC_4F: VertexDeclarationAction
        @JvmField val BONE_WEIGHTS: VertexDeclarationAction
        @JvmField val TANGENT: VertexDeclarationAction
        @JvmField val BINORMAL: VertexDeclarationAction
        @JvmField val BITS_PER_SLOT: Int

        init {
            NORMAL_POINTER_ACTION = VertexDeclarationAction(0, 1, 3, GLActionFormat.FLOAT_32_ARB)
            BONE_INDICES = VertexDeclarationAction(1, 7, 4, GLActionFormat.FLOAT_32_ARB)
            TEXCOORD_4F = VertexDeclarationAction(2, 6, 4, GLActionFormat.FLOAT_32_ARB)
            BONE_WEIGHTS = VertexDeclarationAction(3, 9, 4, GLActionFormat.FLOAT_32_ARB)
            TEXCOORD = VertexDeclarationAction(4, 4, 2, GLActionFormat.FLOAT_32_ARB)
            TANGENT = VertexDeclarationAction(5, 10, 3, GLActionFormat.FLOAT_32_ARB)
            BINORMAL = VertexDeclarationAction(6, 11, 3, GLActionFormat.FLOAT_32_ARB)
            POSITION = VertexDeclarationAction(7, 0, 3, GLActionFormat.FLOAT_32_ARB)
            TEXCOORD_1F = VertexDeclarationAction(8, 3, 1, GLActionFormat.FLOAT_32_ARB)
            TEXCOORD_3F = VertexDeclarationAction(9, 5, 3, GLActionFormat.FLOAT_32_ARB)
            COLOR = VertexDeclarationAction(10, 2, 4, GLActionFormat.PIXEL_FORMAT)
            GENERIC_4F = VertexDeclarationAction(11, 8, 4, GLActionFormat.FLOAT_32_ARB)
            BITS_PER_SLOT = BufferUtils.ilog(16)
        }

        @JvmStatic
        fun forType(type: Int): VertexDeclarationAction? {
            return when (type) {
                0 -> POSITION
                1 -> NORMAL_POINTER_ACTION
                2 -> COLOR
                3 -> TEXCOORD_1F
                4 -> TEXCOORD
                5 -> TEXCOORD_3F
                6 -> TEXCOORD_4F
                else -> null
            }
        }
    }
}
