package com.jagex.game.runetek5.graphics.ifcomponent

import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.ColorUtil

object ColorPicker {
    @JvmStatic
    fun drawColorPicker(toolkit: AbstractToolkit, x: Int, y: Int, component: Component) {
        val lightness: Byte = 63
        val saturation: Byte = 7
        for (hue in 63 downTo 0) {
            val hsl = (hue and 0x3f shl 10) or (saturation.toInt() and 0x7 shl 7) or (lightness.toInt() and 0x7f)
            ColorUtil.initializeTables(false, true)
            val rgb = ColorUtil.hslToRgb!![hsl]
            ColorUtil.resetTables(false, true)
            toolkit.drawFilledRectangle(x, ((63 - hue) * component.height shr 6) + y, component.width, (component.height shr 6) + 1, rgb, 0)
        }
    }
}
