package com.jagex.game.runetek5.graphics.model.billboard

class BillboardSurface {

    @JvmField var z: Float = 0f
    @JvmField var x: Int = 0
    @JvmField var y: Int = 0
    @JvmField var size2d: Int = 0
    @JvmField var size3d: Int = 0

    @JvmField var scaleX: Float = 1.0f

    @JvmField var scaleY: Float = 1.0f

    @JvmField var offsetX: Int = 0

    @JvmField var offsetY: Int = 0

    @JvmField var diffuseColor: Int = 0

    @JvmField var rotation: Int = 0

    constructor(colorRgb: Int, scaleX: Float, scaleY: Float, x: Int, y: Int, rotation: Int) {
        this.diffuseColor = colorRgb
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.offsetX = x
        this.offsetY = y
        this.rotation = rotation
    }

    constructor(diffuseColorRgb: Int) {
        this.diffuseColor = diffuseColorRgb
    }

    fun copy(source: BillboardSurface) {
        this.scaleX = source.scaleX
        this.scaleY = source.scaleY
        this.offsetX = source.offsetX
        this.offsetY = source.offsetY
        this.diffuseColor = source.diffuseColor
        this.rotation = source.rotation
    }

    fun copy(): BillboardSurface {
        return BillboardSurface(diffuseColor, scaleX, scaleY, offsetX, offsetY, rotation)
    }
}
