package com.jagex.game.runetek5.entity.hint

import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.ComparableGraphNode

class HintTrailComparable(
    @JvmField val hintTrailGraphNode: HintTrailGraphNode
) : ComparableGraphNode {
    override fun matches(interactive: AbstractInteractiveEntity): Boolean {
        return interactive === hintTrailGraphNode
    }
}
