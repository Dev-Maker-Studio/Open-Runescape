package com.jagex.game.runetek5.toolkit.opengl

data class GLFormat(
    @JvmField val formatType: Int,
    @JvmField val bytesPerPixel: Int
) {
    companion object {
        @JvmField val RGB = GLFormat(5, 3)
        @JvmField val HDR = GLFormat(6, 4)
        @JvmField val RGBA = GLFormat(1, 1)
        @JvmField val LUMINANCE = GLFormat(0, 1)
        @JvmField val LUMINANCE_ALPHA = GLFormat(4, 2)
        @JvmField val DEPTH = GLFormat(8, 1)
        @JvmField val COMPRESSED_DXT1 = GLFormat(7, 1)
        @JvmField val COMPRESSED_DXT5 = GLFormat(9, 1)
    }
}
