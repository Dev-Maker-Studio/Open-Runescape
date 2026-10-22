package com.jagex.game.runetek5.graphics.modern.dump

enum class RecordedTextureFormat(val id: Int, val componentBytes: Int) {
    RGBA8(0, 1),
    RGBA16F(1, 2);

    val texelBytes: Int get() = componentBytes * 4
}
