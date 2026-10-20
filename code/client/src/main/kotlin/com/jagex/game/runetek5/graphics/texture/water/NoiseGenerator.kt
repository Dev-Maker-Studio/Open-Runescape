package com.jagex.game.runetek5.graphics.texture.water

internal abstract class NoiseGenerator {
    internal abstract fun generateNoise3D(layer: Int, scaleX: Float, scaleY: Float, scaleZ: Float, amplitude: Float, output: FloatArray)
}
