package com.jagex.game.runetek5.graphics.model.billboard

import com.jagex.game.runetek5.buffer.Packet

/**
 * Configuration type for billboards (2D sprites attached to 3D models).
 */
class BillboardType {

    /**
     * The material/texture ID to use for this billboard.
     */
    @JvmField var materialId = -1

    /**
     * The horizontal size / ingame width.
     */
    @JvmField var size2d = 64

    /**
     * The vertical size / ingame height.
     */
    @JvmField var size3d = 64

    /**
     * FIXME: incorrectly named? should be something related to color blending?
     *   0 = rectangle
     *   1 = circle
     *   2 = brightened rectangle
     *   > 2 = invalid, hides model billboard is attached to
     */
    @JvmField var shape = 2

    @JvmField var blendType = 1

    @JvmField var stationary = false

    @JvmField var hasUid = false

    /**
     * Decodes this billboard type from a packet buffer.
     *
     * @param buffer The packet to decode from
     */
    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    /**
     * Decodes a single opcode from the packet buffer.
     *
     * @param buffer The packet to decode from
     * @param opcode The opcode to process
     */
    fun decode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> {
                materialId = buffer.readUShort()
                if (materialId == 65535) {
                    materialId = -1
                }
            }
            2 -> {
                size2d = buffer.readUShort() + 1
                size3d = buffer.readUShort() + 1
            }
            3 -> buffer.readByte()
            4 -> shape = buffer.readUByte()
            5 -> blendType = buffer.readUByte()
            6 -> stationary = true
            7 -> hasUid = true
        }
    }
}
