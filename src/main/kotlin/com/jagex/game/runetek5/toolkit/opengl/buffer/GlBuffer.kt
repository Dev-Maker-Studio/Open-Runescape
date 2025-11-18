package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import jaclib.memory.Buffer

/**
 *
 * @author Steven - 13/09/2023
 *
 */
open class GlBuffer {

    @JvmField val toolkit: OpenGlToolkit
    @JvmField var buffer: Buffer?

    constructor(toolkit: OpenGlToolkit, buffer: Buffer) {
        this.toolkit = toolkit
        this.buffer = buffer
    }

    constructor(toolkit: OpenGlToolkit, data: ByteArray?, initialCapacity: Int) {
        this.toolkit = toolkit
        this.buffer = this.toolkit.nativeHeap.createBuffer(initialCapacity, false)
        if (data != null) {
            this.buffer!!.put(data, 0, 0, initialCapacity)
        }
    }

    fun put(data: ByteArray, requiredSize: Int) {
        if (this.buffer == null || this.buffer!!.getSize() < requiredSize) {
            this.buffer = this.toolkit.nativeHeap.createBuffer(requiredSize, false)
        }

        this.buffer!!.put(data, 0, 0, requiredSize)
    }
}
