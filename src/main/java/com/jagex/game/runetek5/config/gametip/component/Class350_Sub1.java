package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class Class350_Sub1 extends AnchoredElement {

    public final int backgroundColor;
    public final int spriteFileId;

    public Class350_Sub1(AnchorX class3561, AnchorY class3532, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_11, int i_12) {
        super(class3561, class3532, i_3, i_4, i_5, i_6, i_7, i_8, i_9);
        this.backgroundColor = i_11;
        this.spriteFileId = i_12;
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.aClass60603;
    }
}
