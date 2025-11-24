package com.jagex.game.runetek5.toolkit.vertexdeclaration

import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.utils.BufferUtils

class VertexDeclarationAction(
    @JvmField val openglAction: Int,
    @JvmField val slot: Int,
    @JvmField val anInt705: Int,
    @JvmField val actionFormatOpengl: GLActionFormat
) {
    @JvmField val pointerOffsetStep: Int = actionFormatOpengl.bytesPerEntry * anInt705

    init {
        if (slot >= 16) {
            throw RuntimeException()
        }
    }

    companion object {
        @JvmField val aClass69695: VertexDeclarationAction
        @JvmField val NORMAL_POINTER_ACTION: VertexDeclarationAction
        @JvmField val aClass69690: VertexDeclarationAction
        @JvmField val aClass69691: VertexDeclarationAction
        @JvmField val aClass69692: VertexDeclarationAction
        @JvmField val aClass69693: VertexDeclarationAction
        @JvmField val aClass69694: VertexDeclarationAction
        @JvmField val aClass69696: VertexDeclarationAction
        @JvmField val aClass69700: VertexDeclarationAction
        @JvmField val aClass69699: VertexDeclarationAction
        @JvmField val aClass69688: VertexDeclarationAction
        @JvmField val aClass69697: VertexDeclarationAction
        @JvmField val anInt702: Int

        init {
            NORMAL_POINTER_ACTION = VertexDeclarationAction(0, 1, 3, GLActionFormat.FLOAT_32_ARB)
            aClass69696 = VertexDeclarationAction(1, 7, 4, GLActionFormat.FLOAT_32_ARB)
            aClass69694 = VertexDeclarationAction(2, 6, 4, GLActionFormat.FLOAT_32_ARB)
            aClass69699 = VertexDeclarationAction(3, 9, 4, GLActionFormat.FLOAT_32_ARB)
            aClass69692 = VertexDeclarationAction(4, 4, 2, GLActionFormat.FLOAT_32_ARB)
            aClass69688 = VertexDeclarationAction(5, 10, 3, GLActionFormat.FLOAT_32_ARB)
            aClass69697 = VertexDeclarationAction(6, 11, 3, GLActionFormat.FLOAT_32_ARB)
            aClass69695 = VertexDeclarationAction(7, 0, 3, GLActionFormat.FLOAT_32_ARB)
            aClass69691 = VertexDeclarationAction(8, 3, 1, GLActionFormat.FLOAT_32_ARB)
            aClass69693 = VertexDeclarationAction(9, 5, 3, GLActionFormat.FLOAT_32_ARB)
            aClass69690 = VertexDeclarationAction(10, 2, 4, GLActionFormat.PIXEL_FORMAT)
            aClass69700 = VertexDeclarationAction(11, 8, 4, GLActionFormat.FLOAT_32_ARB)
            anInt702 = BufferUtils.ilog(16)
        }

        @JvmStatic
        fun forType(type: Int): VertexDeclarationAction? {
            return when (type) {
                0 -> aClass69695
                1 -> NORMAL_POINTER_ACTION
                2 -> aClass69690
                3 -> aClass69691
                4 -> aClass69692
                5 -> aClass69693
                6 -> aClass69694
                else -> null
            }
        }
    }
}
