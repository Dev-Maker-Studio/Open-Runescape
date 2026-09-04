package com.jagex.game.runetek5.graphics.model.billboard

class BillboardSurfaceGL {

    @JvmField var scaleX: Int = 128
    @JvmField var scaleY: Int = 128
    @JvmField var diffuseColor: Int = 0
    @JvmField var offsetX: Int = 0
    @JvmField var offsetY: Int = 0
    @JvmField var rotation: Int = 0

    constructor(diffuseColor: Int) {
        this.diffuseColor = diffuseColor
    }

    constructor(diffuseColor: Int, scaleX: Int, scaleY: Int, offsetX: Int, offsetY: Int, rotation: Int) {
        this.diffuseColor = diffuseColor
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.rotation = rotation
    }

    fun copyFrom(source: BillboardSurfaceGL) {
        this.scaleX = source.scaleX
        this.scaleY = source.scaleY
        this.offsetX = source.offsetX
        this.offsetY = source.offsetY
        this.diffuseColor = source.diffuseColor
        this.rotation = source.rotation
    }

    fun copy(): BillboardSurfaceGL {
        return BillboardSurfaceGL(this.diffuseColor, this.scaleX, this.scaleY, this.offsetX, this.offsetY, this.rotation)
    }
}
