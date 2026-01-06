package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.gametip.component.TipsSprite
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import kotlin.math.max

class Class43(
    @JvmField val spriteResource: Js5ResourceProvider,
    @JvmField val tipsSprite: TipsSprite
) : DrawableItem {

    @JvmField var nativeSprite: NativeSprite? = null

    override fun initialize() {
        this.nativeSprite = NativeSprite.fromJs5(this.spriteResource, this.tipsSprite.spriteFileId)
    }

    override fun isCached(): Boolean {
        return this.spriteResource.isFileCached(this.tipsSprite.spriteFileId)
    }

    override fun draw(draw: Boolean) {
        if (draw) {
            val width = max(GameShell.screenCanvasWidth, Client.GAME_WIDTH)
            val height = max(GameShell.screenCanvasHeight * -969250379, Client.GAME_HEIGHT)
            val scaleWidth = this.nativeSprite!!.getSpriteWidth()
            val scaleHeight = this.nativeSprite!!.getSpriteHeight()
            var x = 0
            var drawWidth = width
            var drawLength = width * scaleHeight / scaleWidth
            var y = (height - drawLength) / 2
            if (drawLength > height) {
                y = 0
                drawLength = height
                drawWidth = height * scaleWidth / scaleHeight
                x = (width - drawWidth) / 2
            }
            this.nativeSprite!!.drawCircle(x, y, drawWidth, drawLength)
        }
    }
}
