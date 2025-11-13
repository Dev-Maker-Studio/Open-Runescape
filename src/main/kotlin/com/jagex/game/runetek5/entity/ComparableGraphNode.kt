package com.jagex.game.runetek5.entity

interface ComparableGraphNode {
    fun matches(interactive: AbstractInteractiveEntity): Boolean
}
