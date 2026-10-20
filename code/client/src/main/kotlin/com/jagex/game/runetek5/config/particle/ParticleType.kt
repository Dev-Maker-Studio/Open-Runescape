package com.jagex.game.runetek5.config.particle

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.sqrt

class ParticleType {

    @JvmField var partileFileId: Int = 0
    @JvmField var type: Int = 0
    @JvmField var sizeMultiplier: Int = 0
    @JvmField var offsetX: Int = 0
    @JvmField var offsetY: Int = 0
    @JvmField var offsetZ: Int = 0
    @JvmField var particleHandlingType: Int = 0
    @JvmField var verticeCalculationType: Int = 0
    @JvmField var currentOffset: Int = 0
    @JvmField var zan: Int = 0
    @JvmField var size3d: Int = 0
    @JvmField var uid: Long = 0
    @JvmField var rotation: Int = 0
    @JvmField var invert: Boolean = false

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> rotation = stream.readUShort()
            2 -> stream.readUByte()
            3 -> {
                offsetX = stream.readInt()
                offsetY = stream.readInt()
                offsetZ = stream.readInt()
            }
            4 -> {
                type = stream.readUByte()
                sizeMultiplier = stream.readInt()
            }
            6 -> particleHandlingType = stream.readUByte()
            8 -> currentOffset = 1
            9 -> verticeCalculationType = 1
            10 -> invert = true
        }
    }

    fun setDefaults() {
        zan = MathUtils.COSINE[rotation shl 3]
        val x = offsetX.toLong()
        val y = offsetY.toLong()
        val z = offsetZ.toLong()
        size3d = sqrt((x * x + y * y + z * z).toDouble()).toInt()
        if (sizeMultiplier == 0) {
            sizeMultiplier = 1
        }
        when (type) {
            0 -> uid = 2147483647L
            1 -> {
                uid = size3d * 8L / sizeMultiplier
                uid *= uid
            }
            2 -> uid = size3d * 8L / sizeMultiplier
        }
        if (invert) {
            size3d *= -1
        }
    }

    companion object {
        @JvmField val particleTypes = arrayOfNulls<ParticleType>(16)
        @Suppress("UNCHECKED_CAST", "RAWTYPES")
        @JvmField val particleTypeCache: IterableNodeMap<*> = (IterableNodeMap::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType).newInstance(16) as IterableNodeMap<*>)
        @JvmField var particleType: Int = 0
        @JvmField val particleCache = LRUCache(64)

        @JvmStatic
        fun resetCache() {
            particleCache.reset()
        }
    }
}
