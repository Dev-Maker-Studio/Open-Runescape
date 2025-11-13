package com.jagex.game.runetek5.graphics.model.normal

class VertexNormal {
    @JvmField
    var x: Int = 0

    @JvmField
    var y: Int = 0

    @JvmField
    var z: Int = 0

    @JvmField
    var magnitude: Int = 0

    constructor() {
        // NOOP
    }

    constructor(other: VertexNormal) {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        this.magnitude = other.magnitude
    }
}
