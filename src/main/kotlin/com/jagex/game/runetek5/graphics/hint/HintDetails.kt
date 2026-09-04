package com.jagex.game.runetek5.graphics.hint

class HintDetails {
    @JvmField
    var ticksVisible: Int = 0

    @JvmField
    var colorType: Int = 0

    @JvmField
    var text: String? = null

    @JvmField
    var drawType: Int = 0

    @JvmField
    var ticksLeft: Int = 0

    fun getText(): String? {
        return text
    }

    fun getColorType(): Int {
        return colorType
    }

    fun getDrawType(): Int {
        return drawType
    }

    fun getTotalTicksVisible(): Int {
        return ticksVisible
    }

    fun getTicksLeft(): Int {
        return ticksLeft
    }
}
