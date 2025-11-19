package com.jagex.game.runetek5.entity

/**
 *
 * @author Brainpower - 01/05/2022
 *
 */
open class EntityNode {

    @JvmField var next: EntityNode? = null
    @JvmField var previous: EntityNode? = null

    open fun unlinkNode() {
        if (this.previous != null) {
            this.previous!!.next = this.next
            this.next!!.previous = this.previous
            this.next = null
            this.previous = null
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
