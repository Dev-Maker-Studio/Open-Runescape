package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.entity.gameobject.GameObject

class ComparableInteractiveEntity : ComparableGraphNode {
    override fun matches(interactive: AbstractInteractiveEntity): Boolean {
        return interactive is GameObject
    }
}
