package com.jagex.game.runetek5.graphics.texture.water

/**
 * Base class for noise generation algorithms
 * Used for procedural water texture generation
 */
internal abstract class Class261 {
    /**
     * Generates 3D Perlin noise values
     * @param layer The layer index (for multiple octaves)
     * @param scaleX Scale factor for X axis
     * @param scaleY Scale factor for Y axis
     * @param scaleZ Scale factor for Z axis
     * @param amplitude Amplitude/intensity of the noise
     * @param output Output array to store generated noise values
     */
    internal abstract fun method4634(layer: Int, scaleX: Float, scaleY: Float, scaleZ: Float, amplitude: Float, output: FloatArray)
}
