package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers

open class LoadingBar(
    index1: Js5ResourceProvider,
    index2: Js5ResourceProvider,
    class350Sub33: Class350_Sub3
) : LBC(index1, index2, class350Sub33) {

    @JvmField var aNativeSprite9162: NativeSprite? = null
    @JvmField var aNativeSprite9161: NativeSprite? = null
    @JvmField var aNativeSprite9160: NativeSprite? = null
    @JvmField var aNativeSprite9163: NativeSprite? = null
    @JvmField var aNativeSprite9164: NativeSprite? = null
    @JvmField var aNativeSprite9165: NativeSprite? = null

    override fun initialize() {
        super.initialize()
        val class350Sub32 = anchoredElement as Class350_Sub3
        aNativeSprite9162 = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteA)
        aNativeSprite9161 = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteB)
        aNativeSprite9160 = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteC)
        aNativeSprite9163 = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteD)
        aNativeSprite9164 = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteE)
        aNativeSprite9165 = NativeSprite.fromJs5(spriteResource, class350Sub32.spriteF)
    }

    override fun drawOutline(draw: Boolean, x: Int, y: Int) {
        if (draw) {
            val clipping = IntArray(4)
            Renderers.activeToolkit!!.getClipping(clipping)
            Renderers.activeToolkit!!.setClipping(x, y, anchoredElement.width + x, y + anchoredElement.height)
            val width = aNativeSprite9160!!.getSpriteWidth()
            val leftHeight = aNativeSprite9160!!.getSpriteHeight()
            val rightWidth = aNativeSprite9163!!.getSpriteWidth()
            val rightHeight = aNativeSprite9163!!.getSpriteHeight()
            aNativeSprite9160!!.draw(x, y + (anchoredElement.height - leftHeight) / 2)
            aNativeSprite9163!!.draw(anchoredElement.width + x - rightWidth, y + (anchoredElement.height - rightHeight) / 2)
            Renderers.activeToolkit!!.setClipping(x, y, anchoredElement.width + x, y + aNativeSprite9164!!.getSpriteHeight())
            aNativeSprite9164!!.method2756(width + x, y, anchoredElement.width - width - rightWidth, anchoredElement.height)
            val height = aNativeSprite9165!!.getSpriteHeight()
            Renderers.activeToolkit!!.setClipping(x, y + anchoredElement.height - height, anchoredElement.width + x, y + anchoredElement.height)
            aNativeSprite9165!!.method2756(width + x, y + anchoredElement.height - height, anchoredElement.width - width - rightWidth, anchoredElement.height)
            Renderers.activeToolkit!!.setClipping(clipping[0], clipping[1], clipping[2], clipping[3])
        }
    }

    override fun drawBackground(x: Int, y: Int) {
        val clipLeft = x + aNativeSprite9160!!.getSpriteWidth()
        val clipRight = anchoredElement.width + x - aNativeSprite9163!!.getSpriteWidth()
        val clipTop = y + aNativeSprite9164!!.getSpriteHeight()
        val clipBottom = y + anchoredElement.height - aNativeSprite9165!!.getSpriteHeight()
        val width = clipRight - clipLeft
        val height = clipBottom - clipTop
        val loadedPartWidth = loadPercentDecimalFullout() * width / 10000
        val clipping = IntArray(4)
        Renderers.activeToolkit!!.getClipping(clipping)
        Renderers.activeToolkit!!.setClipping(clipLeft, clipTop, clipLeft + loadedPartWidth, clipBottom)
        method14498(clipLeft, clipTop, width, height)
        Renderers.activeToolkit!!.setClipping(clipLeft + loadedPartWidth, clipTop, clipRight, clipBottom)
        aNativeSprite9161!!.method2756(clipLeft, clipTop, width, height)
        Renderers.activeToolkit!!.setClipping(clipping[0], clipping[1], clipping[2], clipping[3])
    }

    open fun method14498(x: Int, y: Int, width: Int, height: Int) {
        aNativeSprite9162!!.method2756(x, y, width, height)
    }

    override fun isCached(): Boolean {
        if (!super.isCached()) {
            return false
        } else {
            val class350Sub32 = anchoredElement as Class350_Sub3
            return spriteResource.isFileCached(class350Sub32.spriteA) &&
                    spriteResource.isFileCached(class350Sub32.spriteB) &&
                    spriteResource.isFileCached(class350Sub32.spriteC) &&
                    spriteResource.isFileCached(class350Sub32.spriteD) &&
                    spriteResource.isFileCached(class350Sub32.spriteE) &&
                    spriteResource.isFileCached(class350Sub32.spriteF)
        }
    }
}
