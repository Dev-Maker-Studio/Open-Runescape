package com.jagex.game.runetek5.config.sequence

import com.jagex.game.runetek5.entity.SceneGraphEntity

class PathingEntitySequence(entity: SceneGraphEntity) : EntitySequence(entity) {
    @JvmField
    var delayTicks: Int = 0
}
