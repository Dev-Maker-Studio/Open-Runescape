package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.runetek5.entity.EntityNode

class EntityOverhead : EntityNode() {
    @JvmField
    var pathingEntity: PathingEntity? = null

    @JvmField
    var endY: Int = 0

    @JvmField
    var startX: Int = 0

    @JvmField
    var startY: Int = 0

    @JvmField
    var endX: Int = 0
}
