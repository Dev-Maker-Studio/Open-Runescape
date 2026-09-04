package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.SpriteLoadingBarElement
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers

class SpriteLoadingBar(
    spriteResource: Js5ResourceProvider,
    fontResource: Js5ResourceProvider,
    element: SpriteLoadingBarElement
) : AbstractLoadingBar(spriteResource, fontResource, element) {

    @JvmField var fillTileSprite: NativeSprite? = null

    override fun drawOutline(draw: Boolean, x: Int, y: Int) {
        Renderers.activeToolkit!!.drawRectangleOutline(x - 2, y, super.anchoredElement.width + 4, super.anchoredElement.height + 2, (super.anchoredElement as SpriteLoadingBarElement).backgroundColor, 0)
        Renderers.activeToolkit!!.drawRectangleOutline(x - 1, y + 1, super.anchoredElement.width + 2, super.anchoredElement.height, 0, 0)
    }

    override fun drawBackground(x: Int, y: Int) {
        val width = loadPercentDecimalFullout() * super.anchoredElement.width / 10000
        val clipping = IntArray(4)
        Renderers.activeToolkit!!.getClipping(clipping)
        Renderers.activeToolkit!!.setClipping(x, y + 2, width + x, y + super.anchoredElement.height)
        this.fillTileSprite!!.drawTiled(x, y + 2, super.anchoredElement.width, super.anchoredElement.height)
        Renderers.activeToolkit!!.setClipping(clipping[0], clipping[1], clipping[2], clipping[3])
    }

    override fun initialize() {
        super.initialize()
        this.fillTileSprite = NativeSprite.fromJs5(super.spriteResource, (super.anchoredElement as SpriteLoadingBarElement).spriteFileId)
    }

    override fun isCached(): Boolean {
        return super.isCached() && super.spriteResource.isFileCached((super.anchoredElement as SpriteLoadingBarElement).spriteFileId)
    }
}
