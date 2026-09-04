package com.jagex.game.runetek5.toolkit.companion

enum class PrimitiveDrawMode(
    @JvmField val mode: Int
) {
    LINE_STRIP(0),
    LINES(1),
    TRIANGLE_STRIP(2),
    TRIANGLE_FAN(3),
    TRIANGLES(4),
    GL_POINTS(5)
}
