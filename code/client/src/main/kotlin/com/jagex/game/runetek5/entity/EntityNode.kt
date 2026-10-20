package com.jagex.game.runetek5.entity

open class EntityNode {

    @JvmField var next: EntityNode? = null
    @JvmField var previous: EntityNode? = null

    open fun unlinkNode() {
        if (previous != null) {
            previous!!.next = next
            next!!.previous = previous
            next = null
            previous = null
        }
    }

    companion object {
        @JvmStatic
        fun replace(dst: EntityNode, src: EntityNode) {
            if (dst.previous != null) {
                dst.unlinkNode()
            }
            dst.previous = src
            dst.next = src.next
            dst.previous!!.next = dst
            dst.next!!.previous = dst
        }
    }
}
