package com.jagex.game.runetek5.graphics.model.billboard

class BillboardEffect(
    @JvmField val appliesOnFace: Int,
    size2d: Int,
    size3d: Int,
    materialId: Int,
    blendType: Int,
    shape: Int,
    @JvmField val hasUid: Boolean,
    @JvmField val distance: Int
) {

    /**
     * width / size_x_z
     */
    @JvmField val size2d: Short = size2d.toShort()

    /**
     * height / size_y
     */
    @JvmField val size3d: Short = size3d.toShort()

    /**
     * Material the billboard is made of
     */
    @JvmField val materialId: Short = materialId.toShort()

    /**
     * 0 = color mix
     * 1 = no color mix
     */
    @JvmField val blendType: Byte = blendType.toByte()

    /**
     * 0 = rectangle
     * 1 = circle
     * 2 = brightened rectangle
     */
    @JvmField val shape: Byte = shape.toByte()
}
