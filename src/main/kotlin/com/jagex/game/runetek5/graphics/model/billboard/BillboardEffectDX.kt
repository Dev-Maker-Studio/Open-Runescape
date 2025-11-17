package com.jagex.game.runetek5.graphics.model.billboard

class BillboardEffectDX(
    @JvmField val appliesOnFace: Int,
    @JvmField val faceA: Int,
    @JvmField val faceB: Int,
    @JvmField val faceC: Int,
    size2d: Int,
    size3d: Int,
    materialId: Int,
    @JvmField val stationary: Boolean,
    @JvmField val distance: Int
) {
    @JvmField val size2d: Short = size2d.toShort()
    @JvmField val size3d: Short = size3d.toShort()
    @JvmField val materialId: Short = materialId.toShort()
}
