package com.jagex.game.runetek5.toolkit.opengl

data class GLFormat(
    @JvmField val formatType: Int,
    @JvmField val bytesPerPixel: Int
) {
    companion object {
        @JvmField val aClass1501950 = GLFormat(5, 3)
        @JvmField val HDR = GLFormat(6, 4)
        @JvmField val RGBA = GLFormat(1, 1)
        @JvmField val aClass1501948 = GLFormat(0, 1)
        @JvmField val aClass1501952 = GLFormat(4, 2)
        @JvmField val aClass1501953 = GLFormat(8, 1)
        @JvmField val aClass1501956 = GLFormat(7, 1)
        @JvmField val aClass1501957 = GLFormat(9, 1)
    }
}
