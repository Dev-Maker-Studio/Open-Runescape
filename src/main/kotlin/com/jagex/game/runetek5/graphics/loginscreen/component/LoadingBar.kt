package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.MultiSpriteLoadingBarElement
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers

open class LoadingBar(
    index1: Js5ResourceProvider,
    index2: Js5ResourceProvider,
    class350Sub33: MultiSpriteLoadingBarElement
) : AbstractLoadingBar(index1, index2, class350Sub33) {

    @JvmField var fillSprite: NativeSprite? = null
    @JvmField var emptySprite: NativeSprite? = null
    @JvmField var leftCapSprite: NativeSprite? = null
    @JvmField var rightCapSprite: NativeSprite? = null
    @JvmField var topBorderSprite: NativeSprite? = null
    @JvmField var bottomBorderSprite: NativeSprite? = null

    override fun initialize() {
        super.initialize()
        val class350Sub32 = anchoredElement as MultiSpriteLoadingBarElement
        fillSprite = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteA)
        emptySprite = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteB)
        leftCapSprite = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteC)
        rightCapSprite = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteD)
        topBorderSprite = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteE)
        bottomBorderSprite = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteF)
    }

    override fun drawOutline(draw: Boolean, x: Int, y: Int) {
        if (draw) {
            val clipping = IntArray(4)
            Renderers.activeToolkit!!.getClipping(clipping)
            Renderers.activeToolkit!!.setClipping(x, y, anchoredElement.width + x, y + anchoredElement.height)
            val width = leftCapSprite!!.getSpriteWidth()
            val leftHeight = leftCapSprite!!.getSpriteHeight()
            val rightWidth = rightCapSprite!!.getSpriteWidth()
            val rightHeight = rightCapSprite!!.getSpriteHeight()
            leftCapSprite!!.draw(x, y + (anchoredElement.height - leftHeight) / 2)
            rightCapSprite!!.draw(anchoredElement.width + x - rightWidth, y + (anchoredElement.height - rightHeight) / 2)
            Renderers.activeToolkit!!.setClipping(x, y, anchoredElement.width + x, y + topBorderSprite!!.getSpriteHeight())
            topBorderSprite!!.drawTiled(width + x, y, anchoredElement.width - width - rightWidth, anchoredElement.height)
            val height = bottomBorderSprite!!.getSpriteHeight()
            Renderers.activeToolkit!!.setClipping(x, y + anchoredElement.height - height, anchoredElement.width + x, y + anchoredElement.height)
            bottomBorderSprite!!.drawTiled(width + x, y + anchoredElement.height - height, anchoredElement.width - width - rightWidth, anchoredElement.height)
            Renderers.activeToolkit!!.setClipping(clipping[0], clipping[1], clipping[2], clipping[3])
        }
    }

    override fun drawBackground(x: Int, y: Int) {
        val clipLeft = x + leftCapSprite!!.getSpriteWidth()
        val clipRight = anchoredElement.width + x - rightCapSprite!!.getSpriteWidth()
        val clipTop = y + topBorderSprite!!.getSpriteHeight()
        val clipBottom = y + anchoredElement.height - bottomBorderSprite!!.getSpriteHeight()
        val width = clipRight - clipLeft
        val height = clipBottom - clipTop
        val loadedPartWidth = loadPercentDecimalFullout() * width / 10000
        val clipping = IntArray(4)
        Renderers.activeToolkit!!.getClipping(clipping)
        Renderers.activeToolkit!!.setClipping(clipLeft, clipTop, clipLeft + loadedPartWidth, clipBottom)
        drawFillArea(clipLeft, clipTop, width, height)
        Renderers.activeToolkit!!.setClipping(clipLeft + loadedPartWidth, clipTop, clipRight, clipBottom)
        emptySprite!!.drawTiled(clipLeft, clipTop, width, height)
        Renderers.activeToolkit!!.setClipping(clipping[0], clipping[1], clipping[2], clipping[3])
    }

    open fun drawFillArea(x: Int, y: Int, width: Int, height: Int) {
        fillSprite!!.drawTiled(x, y, width, height)
    }

    override fun isCached(): Boolean {
        if (!super.isCached()) {
            return false
        } else {
            val class350Sub32 = anchoredElement as MultiSpriteLoadingBarElement
            return spriteResource.isFileCached(class350Sub32.spriteA) &&
                    spriteResource.isFileCached(class350Sub32.spriteB) &&
                    spriteResource.isFileCached(class350Sub32.spriteC) &&
                    spriteResource.isFileCached(class350Sub32.spriteD) &&
                    spriteResource.isFileCached(class350Sub32.spriteE) &&
                    spriteResource.isFileCached(class350Sub32.spriteF)
        }
    }
}
