package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.AnimatedLoadingBarElement
import com.jagex.game.runetek5.graphics.loginscreen.LegacyAssetLoadingScreen
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class AnimatedLoadingBar(
    spriteResource: Js5ResourceProvider,
    fontResource: Js5ResourceProvider,
    loadingBarElement: AnimatedLoadingBarElement
) : LoadingBar(spriteResource, fontResource, loadingBarElement) {

    override fun drawFillArea(x: Int, y: Int, width: Int, height: Int) {
        val spriteWidth = super.fillSprite!!.getSpriteWidth()
        val scrollOffset = (anchoredElement as AnimatedLoadingBarElement).scrollSpeed * LegacyAssetLoadingScreen.getCandyBarCycle() / 10 % spriteWidth
        super.fillSprite!!.drawTiled(scrollOffset + (x - spriteWidth), y, width + spriteWidth - scrollOffset, height)
    }
}
