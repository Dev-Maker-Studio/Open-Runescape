package com.jagex.game.runetek5.graphics.model.particle

import com.jagex.game.runetek5.entity.EntityNode

open class ParticleNode : EntityNode() {

    @JvmField var child: ParticleNode? = null
    @JvmField var treeNext: ParticleNode? = null

    fun unlinkTree() {
        val n = treeNext ?: return
        n.child = child
        child?.treeNext = n
        child = null
        treeNext = null
    }
}