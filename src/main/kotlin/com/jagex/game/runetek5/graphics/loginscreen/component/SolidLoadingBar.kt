package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.ColorLoadingBarElement
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers

class SolidLoadingBar(
    spriteResource: Js5ResourceProvider,
    fontResource: Js5ResourceProvider,
    class350Sub23: ColorLoadingBarElement
) : AbstractLoadingBar(spriteResource, fontResource, class350Sub23) {

    override fun drawBackground(x: Int, y: Int) {
        val width = loadPercentDecimalFullout() * super.anchoredElement.width / 10000
        Renderers.activeToolkit!!.drawFilledRectangle(x, y + 2, width, super.anchoredElement.height - 2, (super.anchoredElement as ColorLoadingBarElement).fillColor, 0)
        Renderers.activeToolkit!!.drawFilledRectangle(width + x, y + 2, super.anchoredElement.width - width, super.anchoredElement.height - 2, 0, 0)
    }

    override fun drawOutline(draw: Boolean, x: Int, y: Int) {
        Renderers.activeToolkit!!.drawRectangleOutline(x - 2, y, super.anchoredElement.width + 4, super.anchoredElement.height + 2, (super.anchoredElement as ColorLoadingBarElement).outlineColor, 0)
        Renderers.activeToolkit!!.drawRectangleOutline(x - 1, y + 1, super.anchoredElement.width + 2, super.anchoredElement.height, 0, 0)
    }
}
