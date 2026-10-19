package org.darkan.core.map.pathfinder

class WalkStep(@JvmField val dir: Direction, @JvmField val x: Int, @JvmField val y: Int, private var clip: Boolean) {
    fun checkClip(): Boolean {
        return clip
    }

    fun setCheckClip(clip: Boolean) {
        this.clip = clip
    }

    override fun toString(): String {
        return "[$x, $y, $dir, $clip]"
    }
}
