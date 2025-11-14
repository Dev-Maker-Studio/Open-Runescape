package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.entity.EntityNode

open class ParticleNode : EntityNode() {
    @JvmField
    var child: ParticleNode? = null

    @JvmField
    var next: ParticleNode? = null

    fun unlink() {
        if (next != null) {
            next!!.child = child
            child!!.next = next
            child = null
            next = null
        }
    }
}
