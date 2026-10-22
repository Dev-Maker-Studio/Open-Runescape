package com.jagex.game.runetek5.graphics.modern.dump

class SpriteAtlas(pixels: Map<Int, GlRead.Pixels?>) {
    val width = pixels.values.maxOf { it?.width ?: 1 }
    val height = pixels.values.maxOf { it?.height ?: 1 }
    val layers = pixels.keys.sorted().withIndex().associate { it.value to it.index }
    val sizes = pixels.mapValues { (it.value?.width ?: width) to (it.value?.height ?: height) }
    val rgba = ByteArray(Math.multiplyExact(Math.multiplyExact(width, height), Math.multiplyExact(layers.size, 4)))

    init {
        for ((slot, p) in pixels) {
            if (p == null) continue
            val layer = layers.getValue(slot)
            for (y in 0 until p.height) {
                System.arraycopy(p.rgba, y * p.width * 4, rgba, ((layer * height + y) * width) * 4, p.width * 4)
            }
        }
    }
}
