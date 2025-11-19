package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.js5.Js5ResourceProvider

open class NewsSprite(
    @JvmField val spriteResource: Js5ResourceProvider,
    @JvmField val spritePositioning: SpritePositioning
) : DrawableItem {

    @JvmField protected var sprite: NativeSprite? = null

    override fun isCached(): Boolean {
        return this.spriteResource.isFileCached(this.spritePositioning.spriteId)
    }

    override fun initialize() {
        this.sprite = NativeSprite.fromJs5(this.spriteResource, this.spritePositioning.spriteId)
    }

    override fun draw(draw: Boolean) {
        if (draw) {
            val x = this.spritePositioning.horizontalAnchor.calculateDrawX(this.sprite!!.getSpriteWidth(), client.GAME_WIDTH) + this.spritePositioning.spriteOffsetX
            val y = this.spritePositioning.verticalAnchor.calculateDrawY(this.sprite!!.getSpriteHeight(), client.GAME_HEIGHT) + this.spritePositioning.spriteOffsetY
            this.sprite!!.draw(x, y)
        }
    }
}
