package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node

abstract class TextureOperation(
    count: Int,
    @JvmField var monochrome: Boolean
) : Node<TextureOperation>() {

    lateinit var colorImageCache: ColorImageCache
    lateinit var monochromeImageCache: MonochromeImageCache
    @JvmField var imageCacheCapacity: Int = 0
    @JvmField val operations: Array<TextureOperation?> = arrayOfNulls(count)

    open fun createImageCache(width: Int, height: Int) {
        val capacity = if (imageCacheCapacity == 255) height else imageCacheCapacity
        if (monochrome) {
            monochromeImageCache = MonochromeImageCache(capacity, height, width)
        } else {
            colorImageCache = ColorImageCache(capacity, height, width)
        }
    }

    protected fun getChildMonochromeOutput(operationIndex: Int, y: Int): IntArray {
        return if (!operations[operationIndex]!!.monochrome) {
            operations[operationIndex]!!.getColorOutput(y)[0]
        } else {
            operations[operationIndex]!!.getMonochromeOutput(y)
        }
    }

    open fun getMonochromeOutput(y: Int): IntArray {
        throw IllegalStateException()
    }

    open fun getColorOutput(y: Int): Array<IntArray> {
        throw IllegalStateException()
    }

    open fun postDecode() {
    }

    open fun decodeOpcode(opcode: Int, buffer: Packet) {
    }

    open fun getTextureId(): Int {
        return -1
    }

    open fun getMaterialId(): Int {
        return -1
    }

    open fun clearImageCache() {
        if (monochrome) {
            monochromeImageCache.reset()
        } else {
            colorImageCache.clear()
        }
    }

    open fun getChildColorOutput(operationIndex: Int, y: Int): Array<IntArray> {
        if (operations[operationIndex]!!.monochrome) {
            val monochromeRow = operations[operationIndex]!!.getMonochromeOutput(y)
            return arrayOf(monochromeRow, monochromeRow, monochromeRow)
        }
        return operations[operationIndex]!!.getColorOutput(y)
    }
}
