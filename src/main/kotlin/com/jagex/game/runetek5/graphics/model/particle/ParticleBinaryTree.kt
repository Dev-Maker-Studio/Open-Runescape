package com.jagex.game.runetek5.graphics.model.particle

class ParticleBinaryTree {

    @JvmField val current: ParticleNode = ParticleNode()
    @JvmField var aParticleNode: ParticleNode? = null

    init {
        current.child = current
        current.treeNext = current
    }

    fun getNext(): ParticleNode? {
        val child = current.child
        return if (child == current) {
            aParticleNode = null
            null
        } else {
            aParticleNode = child!!.child
            child
        }
    }

    fun method7734(): ParticleNode? {
        val node = aParticleNode
        return if (node == null || node == current) {
            aParticleNode = null
            null
        } else {
            aParticleNode = node.child
            node
        }
    }

    fun insert(node: ParticleNode) {
        if (node.treeNext != null) {
            node.unlinkTree()
        }
        node.treeNext = current.treeNext
        node.child = current
        node.treeNext!!.child = node
        node.child!!.treeNext = node
    }

    fun getNodeCount(): Int {
        var count = 0
        var n = current.child
        while (n != current) {
            count++
            n = n!!.child
        }
        return count
    }

    fun unlinkAll() {
        while (true) {
            val particleNode = current.child
            if (particleNode == current) {
                aParticleNode = null
                return
            }
            particleNode!!.unlinkTree()
        }
    }
}