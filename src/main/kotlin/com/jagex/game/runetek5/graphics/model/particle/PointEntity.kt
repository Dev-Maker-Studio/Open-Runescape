package com.jagex.game.runetek5.graphics.model.particle

open class PointEntity : ParticleNode() {
    @JvmField
    var z: Int = 0

    @JvmField
    var x: Int = 0

    @JvmField
    var size: Int = 0

    @JvmField
    var materialId: Int = 0

    @JvmField
    var y: Int = 0

    @JvmField
    var colorArgb: Int = 0

    @JvmField
    var lightIntensityAdjustment: Boolean = false

    @JvmField
    var priority: Byte = 5
}
