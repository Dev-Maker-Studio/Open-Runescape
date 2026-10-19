package com.jagex.game.runetek5.graphics.model.particle

class ParticleTriangle {
    @JvmField
    var verticeZC: Int = 0

    @JvmField
    var verticeYA: Int = 0

    @JvmField
    var verticeZA: Int = 0

    @JvmField
    var centerY: Int = 0

    @JvmField
    var verticeXA: Int = 0

    @JvmField
    var verticeZB: Int = 0

    @JvmField
    var verticeXC: Int = 0

    @JvmField
    var verticeYC: Int = 0

    @JvmField
    var centerZ: Int = 0

    @JvmField
    var verticeXB: Int = 0

    @JvmField
    var centerX: Int = 0

    @JvmField
    var verticeYB: Int = 0

    fun equals(other: ParticleTriangle): Boolean {
        return centerX == other.centerX && centerY == other.centerY && centerZ == other.centerZ
    }
}
