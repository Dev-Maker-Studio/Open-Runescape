package com.jagex.game.runetek5.graphics.model.normal

class FaceNormal {
    @JvmField
    var x: Int = 0

    @JvmField
    var y: Int = 0

    @JvmField
    var z: Int = 0

    constructor()

    constructor(other: FaceNormal) {
        this.x = other.x
        this.y = other.y
        this.z = other.z
    }
}
