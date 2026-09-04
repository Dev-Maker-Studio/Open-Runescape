package com.jagex.game.runetek5.graphics.model.billboard

class BillboardEffectGL(
    @JvmField val appliesOnFace: Int,
    @JvmField val faceA: Int,
    @JvmField val faceB: Int,
    @JvmField val faceC: Int,
    sizeX: Int,
    sizeY: Int,
    materialId: Int,
    shape: Int,
    @JvmField val stationary: Boolean,
    @JvmField val distance: Int
) {
    @JvmField val sizeX: Short = sizeX.toShort()
    @JvmField val sizeY: Short = sizeY.toShort()
    @JvmField val materialId: Short = materialId.toShort()
    @JvmField val shape: Byte = shape.toByte()
}
