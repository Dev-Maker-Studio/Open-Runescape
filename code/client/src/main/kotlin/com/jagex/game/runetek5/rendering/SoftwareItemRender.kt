package com.jagex.game.runetek5.rendering

import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.graphics.sprite.IItemRenderer
import com.jagex.game.runetek5.tools.Hashing

class SoftwareItemRender : IItemRenderer {

    @JvmField var outlineSize: Int = 0
    @JvmField var itemAmount: Int = 0
    @JvmField var itemId: Int = 0
    @JvmField var rendererId: Int = 0
    @JvmField var shadowColor: Int = 0
    @JvmField var renderStack: Component.StackMode? = null
    @JvmField var hasPlayerAppearance: Boolean = false

    override fun matches(itemRenderer: IItemRenderer): Boolean {
        if (itemRenderer !is SoftwareItemRender) {
            return false
        }
        return itemRenderer.rendererId == this.rendererId &&
                itemRenderer.itemId == this.itemId &&
                itemRenderer.itemAmount == this.itemAmount &&
                itemRenderer.outlineSize == this.outlineSize &&
                itemRenderer.shadowColor == this.shadowColor &&
                itemRenderer.renderStack == this.renderStack &&
                itemRenderer.hasPlayerAppearance == this.hasPlayerAppearance
    }

    override fun getUid(): Long {
        val table = Hashing.CRC64_TABLE
        var hash = -1L
        hash = hash ushr 8 xor table[((hash xor this.rendererId.toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor (this.itemId shr 8).toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor this.itemId.toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor (this.itemAmount shr 24).toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor (this.itemAmount shr 16).toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor (this.itemAmount shr 8).toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor this.itemAmount.toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor this.outlineSize.toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor (this.shadowColor shr 24).toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor (this.shadowColor shr 16).toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor (this.shadowColor shr 8).toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor this.shadowColor.toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor this.renderStack!!.ordinal.toLong()) and 0xffL).toInt()]
        hash = hash ushr 8 xor table[((hash xor if (this.hasPlayerAppearance) 1L else 0L) and 0xffL).toInt()]
        return hash
    }
}
