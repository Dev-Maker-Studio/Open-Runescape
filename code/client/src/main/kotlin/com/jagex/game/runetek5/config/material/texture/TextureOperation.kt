package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node

/**
 * Abstract base class for texture operations.
 *
 * @author Steven - 29/11/2023
 * @author OpenOSRS - Nearly all naming
 */
abstract class TextureOperation(
    count: Int,
    @JvmField var monochrome: Boolean
) : Node<TextureOperation>() {

    lateinit var colorImageCache: ColorImageCache
    lateinit var monochromeImageCache: MonochromeImageCache
    @JvmField var imageCacheCapacity: Int = 0
    @JvmField val operations: Array<TextureOperation?> = arrayOfNulls(count)

    /**
     * Creates the image cache with the specified dimensions.
     *
     * @param width The width of the cache
     * @param height The height of the cache
     */
    open fun createImageCache(width: Int, height: Int) {
        val capacity = if (imageCacheCapacity == 255) height else imageCacheCapacity
        if (monochrome) {
            monochromeImageCache = MonochromeImageCache(capacity, height, width)
        } else {
            colorImageCache = ColorImageCache(capacity, height, width)
        }
    }

    /**
     * Gets the monochrome output from a child operation.
     *
     * @param operationIndex The child operation index
     * @param y The y coordinate
     * @return The monochrome output row
     */
    protected fun getChildMonochromeOutput(operationIndex: Int, y: Int): IntArray {
        return if (!operations[operationIndex]!!.monochrome) {
            operations[operationIndex]!!.getColorOutput(y)[0]
        } else {
            operations[operationIndex]!!.getMonochromeOutput(y)
        }
    }

    /**
     * Gets the monochrome output for this operation.
     *
     * @param y The y coordinate
     * @return The monochrome output row
     */
    open fun getMonochromeOutput(y: Int): IntArray {
        throw IllegalStateException()
    }

    /**
     * Gets the color output for this operation.
     *
     * @param y The y coordinate
     * @return The color output channels (RGB)
     */
    open fun getColorOutput(y: Int): Array<IntArray> {
        throw IllegalStateException()
    }

    /**
     * Post-processing after decoding is complete.
     */
    open fun postDecode() {
    }

    /**
     * Decodes an opcode from the packet buffer.
     *
     * @param opcode The opcode to decode
     * @param buffer The packet buffer
     */
    open fun decodeOpcode(opcode: Int, buffer: Packet) {
    }

    /**
     * Gets the texture ID for this operation.
     *
     * @return The texture ID, or -1 if not applicable
     */
    open fun getTextureId(): Int {
        return -1
    }

    /**
     * Gets the material ID for this operation.
     *
     * @return The material ID, or -1 if not applicable
     */
    open fun getMaterialId(): Int {
        return -1
    }

    /**
     * Clears and releases the image cache.
     */
    open fun clearImageCache() {
        if (monochrome) {
            monochromeImageCache.reset()
        } else {
            colorImageCache.clear()
        }
    }

    /**
     * Gets the color output from a child operation.
     *
     * @param operationIndex The child operation index
     * @param y The y coordinate
     * @return The color output channels (RGB)
     */
    open fun getChildColorOutput(operationIndex: Int, y: Int): Array<IntArray> {
        if (operations[operationIndex]!!.monochrome) {
            val monochromeRow = operations[operationIndex]!!.getMonochromeOutput(y)
            return arrayOf(monochromeRow, monochromeRow, monochromeRow)
        }
        return operations[operationIndex]!!.getColorOutput(y)
    }
}
