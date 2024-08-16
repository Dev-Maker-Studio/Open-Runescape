package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3_Sub1;
import com.jagex.game.runetek5.graphics.loginscreen.LegacyAssetLoadingScreen;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class LoadingBarSub1 extends LoadingBar {

    public LoadingBarSub1(Js5ResourceProvider index1, Js5ResourceProvider index2, Class350_Sub3_Sub1 class350Sub3Sub13) {
        super(index1, index2, class350Sub3Sub13);
    }

    @Override
    void method14498(int i_1, int i_2, int i_3, int i_4) {
        int i_6 = super.aNativeSprite9162.getSpriteWidth();
        int i_7 = ((Class350_Sub3_Sub1) super.anchoredElement).anInt10160 * LegacyAssetLoadingScreen.method8740() / 10 % i_6;
        super.aNativeSprite9162.method2756(i_7 + (i_1 - i_6), i_2, i_3 + i_6 - i_7, i_4);
    }

}
