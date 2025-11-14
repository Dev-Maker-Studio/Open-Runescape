package com.jagex.game.runetek5.scene.tile

class ShapedTile {
    @JvmField
    var offsetY: ShortArray? = null

    @JvmField
    var count: Short = 0

    @JvmField
    var triangleB: ShortArray? = null

    @JvmField
    var offsetX: ShortArray? = null

    @JvmField
    var offsetZ: ShortArray? = null

    @JvmField
    var triangleCount: Short = 0

    @JvmField
    var primaryMaterialIds: ShortArray? = null

    @JvmField
    var triangleA: ShortArray? = null

    @JvmField
    var primaryColors: IntArray? = null

    @JvmField
    var triangleC: ShortArray? = null

    @JvmField
    var light: ShortArray? = null

    /**
     * Unused
     */
    @JvmField
    var secondaryMaterialIds: ShortArray? = null

    @JvmField
    var functionMask: Byte = 0

    @JvmField
    var secondaryColors: IntArray? = null
}
