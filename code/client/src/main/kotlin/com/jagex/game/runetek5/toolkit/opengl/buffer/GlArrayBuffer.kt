package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import jaclib.memory.Buffer

/**
 *
 * @author Steven - 13/09/2023
 *
 */
class GlArrayBuffer : GlBuffer, ArrayBufferGL {

    /**
     * Padding between elements
     */
    @JvmField var stride: Int

    constructor(toolkit: OpenGlToolkit, stride: Int, buffer: ByteArray?, initialCapacity: Int) : super(toolkit, buffer, initialCapacity) {
        this.stride = stride
    }

    constructor(toolkit: OpenGlToolkit, stride: Int, buffer: Buffer) : super(toolkit, buffer) {
        this.stride = stride
    }

    override fun getId(): Int {
        return 0
    }

    override fun getBufferAddress(): Long {
        return super.buffer!!.getBufferAddress()
    }

    override fun putArray(stride: Int, data: ByteArray, requiredSize: Int) {
        put(data, requiredSize)
        this.stride = stride
    }

    override fun getStride(): Int {
        return this.stride
    }
}
