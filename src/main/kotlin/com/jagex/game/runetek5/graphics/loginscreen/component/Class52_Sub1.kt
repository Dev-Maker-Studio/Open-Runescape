package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.Class350_Sub1
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers

class Class52_Sub1(
    index1: Js5ResourceProvider,
    index2: Js5ResourceProvider,
    class350Sub13: Class350_Sub1
) : LBC(index1, index2, class350Sub13) {

    @JvmField var aNativeSprite9150: NativeSprite? = null

    override fun drawOutline(bool1: Boolean, i_2: Int, i_3: Int) {
        Renderers.activeToolkit!!.drawRectangleOutline(i_2 - 2, i_3, super.anchoredElement.width + 4, super.anchoredElement.height + 2, (super.anchoredElement as Class350_Sub1).backgroundColor, 0)
        Renderers.activeToolkit!!.drawRectangleOutline(i_2 - 1, i_3 + 1, super.anchoredElement.width + 2, super.anchoredElement.height, 0, 0)
    }

    override fun drawBackground(x: Int, y: Int) {
        val width = loadPercentDecimalFullout() * super.anchoredElement.width / 10000
        val clipping = IntArray(4)
        Renderers.activeToolkit!!.getClipping(clipping)
        Renderers.activeToolkit!!.setClipping(x, y + 2, width + x, y + super.anchoredElement.height)
        this.aNativeSprite9150!!.method2756(x, y + 2, super.anchoredElement.width, super.anchoredElement.height)
        Renderers.activeToolkit!!.setClipping(clipping[0], clipping[1], clipping[2], clipping[3])
    }

    override fun initialize() {
        super.initialize()
        this.aNativeSprite9150 = NativeSprite.fromJs5(super.spriteResource, (super.anchoredElement as Class350_Sub1).spriteFileId)
    }

    override fun isCached(): Boolean {
        return super.isCached() && super.spriteResource.isFileCached((super.anchoredElement as Class350_Sub1).spriteFileId)
    }
}
