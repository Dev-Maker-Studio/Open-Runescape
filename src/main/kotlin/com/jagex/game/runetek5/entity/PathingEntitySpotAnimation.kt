package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.config.sequence.EntitySequence
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity

class PathingEntitySpotAnimation(entity: PathingEntity) {
    @JvmField
    var adjustmentType: Int = 0

    @JvmField
    var height: Int = 0

    @JvmField
    var rotation: Int = 0

    @JvmField
    var spotAnimationId: Int = -1

    @JvmField
    val sequence: Sequence = EntitySequence(entity)
}
