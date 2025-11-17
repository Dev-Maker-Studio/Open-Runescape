package com.jagex.game.runetek5.toolkit.opengl

data class GLActionFormat(
    @JvmField val anInt757: Int,
    @JvmField val bytesPerEntry: Int
) {
    companion object {
        @JvmField val PIXEL_FORMAT = GLActionFormat(4, 1)
        @JvmField val TEXTURE_FORMAT = GLActionFormat(5, 2)
        @JvmField val DEPTH_COMPONENT_24 = GLActionFormat(1, 3)
        @JvmField val FLOAT_16_ARB = GLActionFormat(8, 2)
        @JvmField val FLOAT_32_ARB = GLActionFormat(3, 4)
        @JvmField var aClass76753 = GLActionFormat(6, 1)
        @JvmField var aClass76756 = GLActionFormat(0, 2)
        @JvmField var aClass76750 = GLActionFormat(7, 4)
        @JvmField var aClass76754 = GLActionFormat(2, 4)
    }
}
