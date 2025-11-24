package com.jagex.game.runetek5.graphics.model.particle

class ParticleBinaryTree {

    @JvmField val current: ParticleNode = ParticleNode()
    @JvmField var aParticleNode: ParticleNode? = null

    init {
        this.current.child = this.current
        this.current.next = this.current
    }

    fun getNext(): ParticleNode? {
        val child = this.current.child
        return if (child == this.current) {
            this.aParticleNode = null
            null
        } else {
            this.aParticleNode = child!!.child
            child
        }
    }

    fun method7734(): ParticleNode? {
        val node = this.aParticleNode
        return if (node == this.current) {
            this.aParticleNode = null
            null
        } else {
            this.aParticleNode = node!!.child
            node
        }
    }

    fun insert(node: ParticleNode) {
        if (node.next != null) {
            node.unlink()
        }
        node.next = this.current.next
        node.child = this.current
        (node.next as ParticleNode).child = node
        node.child!!.next = node
    }

    fun getNodeCount(): Int {
        var count = 0
        var class275Sub13 = this.current.child
        while (class275Sub13 != this.current) {
            count++
            class275Sub13 = class275Sub13!!.child
        }
        return count
    }

    fun unlinkAll() {
        while (true) {
            val particleNode = this.current.child
            if (particleNode == this.current) {
                this.aParticleNode = null
                return
            }
            particleNode!!.unlink()
        }
    }
}
