package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.AnimatedLoadingBarElement
import com.jagex.game.runetek5.graphics.loginscreen.LegacyAssetLoadingScreen
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class LoadingBarSub1(
    index1: Js5ResourceProvider,
    index2: Js5ResourceProvider,
    class350Sub3Sub13: AnimatedLoadingBarElement
) : LoadingBar(index1, index2, class350Sub3Sub13) {

    override fun drawFillArea(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        val i_6 = super.fillSprite!!.getSpriteWidth()
        val i_7 = (anchoredElement as AnimatedLoadingBarElement).scrollSpeed * LegacyAssetLoadingScreen.getCandyBarCycle() / 10 % i_6
        super.fillSprite!!.drawTiled(i_7 + (i_1 - i_6), i_2, i_3 + i_6 - i_7, i_4)
    }
}
