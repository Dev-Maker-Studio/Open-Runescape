package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3_Sub1
import com.jagex.game.runetek5.graphics.loginscreen.LegacyAssetLoadingScreen
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class LoadingBarSub1(
    index1: Js5ResourceProvider,
    index2: Js5ResourceProvider,
    class350Sub3Sub13: Class350_Sub3_Sub1
) : LoadingBar(index1, index2, class350Sub3Sub13) {

    override fun method14498(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        val i_6 = super.aNativeSprite9162.getSpriteWidth()
        val i_7 = (anchoredElement as Class350_Sub3_Sub1).anInt10160 * LegacyAssetLoadingScreen.method8740() / 10 % i_6
        super.aNativeSprite9162.method2756(i_7 + (i_1 - i_6), i_2, i_3 + i_6 - i_7, i_4)
    }
}
