package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class RotatedNewsSprite(
    spriteResource: Js5ResourceProvider,
    spritePositioning: RotatableSpritePositioning
) : NewsSprite(spriteResource, spritePositioning) {
    @JvmField var angle: Int = 0

    override fun draw(draw: Boolean) {
        val x = spritePositioning.horizontalAnchor.calculateDrawX(sprite.getSpriteWidth(), client.GAME_WIDTH) + spritePositioning.spriteOffsetX
        val y = spritePositioning.verticalAnchor.calculateDrawY(sprite.getSpriteHeight(), client.GAME_HEIGHT) + spritePositioning.spriteOffsetY
        sprite.method2758((x + sprite.getSpriteWidth() / 2).toFloat(), (y + sprite.getSpriteHeight() / 2).toFloat(), 4096, angle)
        angle += (spritePositioning as RotatableSpritePositioning).utAngle
    }
}
