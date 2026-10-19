package com.jagex.game.runetek5.config.sequence.frameset

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node

class AnimBase(id: Int, baseData: ByteArray) : Node<AnimBase>() {

    @JvmField val id: Int
    @JvmField val numTransformation: Int
    @JvmField val transformationType: IntArray
    @JvmField val labels: Array<IntArray>
    @JvmField val shadowed: BooleanArray
    @JvmField val submeshes: IntArray

    init {
        this.id = id
        val stream = Packet(baseData)
        this.numTransformation = stream.readUByte()
        this.transformationType = IntArray(this.numTransformation)
        this.labels = Array(this.numTransformation) { IntArray(0) }
        this.shadowed = BooleanArray(this.numTransformation)
        this.submeshes = IntArray(this.numTransformation)
        for (index in 0 until this.numTransformation) {
            this.transformationType[index] = stream.readUByte()
            if (this.transformationType[index] == 6) {
                this.transformationType[index] = 2
            }
        }
        for (index in 0 until this.numTransformation) {
            this.shadowed[index] = stream.readUByte() == 1
        }
        for (index in 0 until this.numTransformation) {
            this.submeshes[index] = stream.readUShort()
        }
        for (index in 0 until this.numTransformation) {
            this.labels[index] = IntArray(stream.readUByte())
        }
        for (index in 0 until this.numTransformation) {
            for (subIndex in this.labels[index].indices) {
                this.labels[index][subIndex] = stream.readUByte()
            }
        }
    }
}
