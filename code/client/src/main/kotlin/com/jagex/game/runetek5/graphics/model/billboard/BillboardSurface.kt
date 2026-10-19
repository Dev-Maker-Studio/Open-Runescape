package com.jagex.game.runetek5.graphics.model.billboard

/**
 * Represents a billboard surface in 3D space.
 * Billboards are 2D images that always face the camera (like sprites in 3D).
 */
class BillboardSurface {

    @JvmField var z: Float = 0f
    @JvmField var x: Int = 0
    @JvmField var y: Int = 0
    @JvmField var size2d: Int = 0
    @JvmField var size3d: Int = 0

    /**
     * Horizontal scale factor (size_2d / size_x)
     */
    @JvmField var scaleX: Float = 1.0f

    /**
     * Vertical scale factor (size_3d / size_y)
     */
    @JvmField var scaleY: Float = 1.0f

    /**
     * Horizontal offset from the model origin
     * (the gap between the model and the billboard on the x-axis)
     */
    @JvmField var offsetX: Int = 0

    /**
     * Vertical offset from the model origin
     * (the gap between the model and the billboard on the y-axis)
     */
    @JvmField var offsetY: Int = 0

    /** Diffuse color in RGB format */
    @JvmField var diffuseColor: Int = 0

    /** Rotation angle */
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

    /**
     * Copies properties from another billboard surface
     * @param source The source billboard to copy from
     */
    fun copy(source: BillboardSurface) {
        this.scaleX = source.scaleX
        this.scaleY = source.scaleY
        this.offsetX = source.offsetX
        this.offsetY = source.offsetY
        this.diffuseColor = source.diffuseColor
        this.rotation = source.rotation
    }

    /**
     * Creates a copy of this billboard surface
     * @return A new billboard with the same properties
     */
    fun copy(): BillboardSurface {
        return BillboardSurface(diffuseColor, scaleX, scaleY, offsetX, offsetY, rotation)
    }
}
