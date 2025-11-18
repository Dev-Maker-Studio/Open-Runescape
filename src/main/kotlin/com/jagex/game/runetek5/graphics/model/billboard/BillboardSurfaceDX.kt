package com.jagex.game.runetek5.graphics.model.billboard

class BillboardSurfaceDX {

    @JvmField var scaleX: Int = 128
    @JvmField var scaleY: Int = 128
    @JvmField var diffuseColor: Int = 0
    @JvmField var offsetX: Int = 0
    @JvmField var offsetY: Int = 0
    @JvmField var rotation: Int = 0

    constructor(diffuseColor: Int, scaleX: Int, scaleY: Int, offsetX: Int, offsetY: Int, rotation: Int) {
        this.diffuseColor = diffuseColor
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.rotation = rotation
    }

    constructor(diffuseColor: Int) {
        this.diffuseColor = diffuseColor
    }

    fun copyFrom(copyFrom: BillboardSurfaceDX) {
        this.scaleX = copyFrom.scaleX
        this.scaleY = copyFrom.scaleY
        this.offsetX = copyFrom.offsetX
        this.offsetY = copyFrom.offsetY
        this.diffuseColor = copyFrom.diffuseColor
        this.rotation = copyFrom.rotation
    }

    fun copy(): BillboardSurfaceDX {
        return BillboardSurfaceDX(this.diffuseColor, this.scaleX, this.scaleY, this.offsetX, this.offsetY, this.rotation)
    }
}
