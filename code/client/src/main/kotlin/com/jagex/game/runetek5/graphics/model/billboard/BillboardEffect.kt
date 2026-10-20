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

    @JvmField val size2d: Short = size2d.toShort()

    @JvmField val size3d: Short = size3d.toShort()

    @JvmField val materialId: Short = materialId.toShort()

    @JvmField val blendType: Byte = blendType.toByte()

    @JvmField val shape: Byte = shape.toByte()
}
