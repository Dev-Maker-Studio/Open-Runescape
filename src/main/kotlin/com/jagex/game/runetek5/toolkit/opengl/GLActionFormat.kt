package com.jagex.game.runetek5.toolkit.opengl

data class GLActionFormat(
    @JvmField val dataTypeId: Int,
    @JvmField val bytesPerEntry: Int
) {
    companion object {
        @JvmField val PIXEL_FORMAT = GLActionFormat(4, 1)
        @JvmField val TEXTURE_FORMAT = GLActionFormat(5, 2)
        @JvmField val DEPTH_COMPONENT_24 = GLActionFormat(1, 3)
        @JvmField val FLOAT_16_ARB = GLActionFormat(8, 2)
        @JvmField val FLOAT_32_ARB = GLActionFormat(3, 4)
        @JvmField var BYTE = GLActionFormat(6, 1)
        @JvmField var SHORT = GLActionFormat(0, 2)
        @JvmField var INT = GLActionFormat(7, 4)
        @JvmField var UNSIGNED_INT = GLActionFormat(2, 4)
    }
}
