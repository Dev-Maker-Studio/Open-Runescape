package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.Node

class ClickzoneMEC(
    @JvmField val category: WorldMapCategory
) : Node<ClickzoneMEC>() {
    @JvmField var spriteLeft: Int = Int.MAX_VALUE
    @JvmField var spriteRight: Int = Int.MIN_VALUE
    @JvmField var spriteBottom: Int = Int.MAX_VALUE
    @JvmField var spriteTop: Int = Int.MIN_VALUE
    @JvmField var textLeft: Int = Int.MAX_VALUE
    @JvmField var textRight: Int = Int.MIN_VALUE
    @JvmField var textBottom: Int = Int.MAX_VALUE
    @JvmField var textTop: Int = Int.MIN_VALUE

    fun mouseWithin(mouseX: Int, mouseY: Int): Boolean {
        return (mouseX >= this.spriteLeft && mouseX <= this.spriteRight && mouseY >= this.spriteBottom && mouseY <= this.spriteTop
                || mouseX >= this.textLeft && mouseX <= this.textRight && mouseY >= this.textBottom && mouseY <= this.textTop)
    }
}
