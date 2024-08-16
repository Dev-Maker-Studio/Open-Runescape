package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class Class350_Sub3_Sub1 extends Class350_Sub3 {

    public final int anInt10160;

    public Class350_Sub3_Sub1(AnchorX class3561, AnchorY class3532, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10, int i_11, int i_12, int i_13, int i_14, int i_15, int i_16) {
        super(class3561, class3532, i_3, i_4, i_5, i_6, i_7, i_8, i_9, i_10, i_11, i_12, i_13, i_14, i_15);
        this.anInt10160 = i_16;
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.aClass60610;
    }

	public static AnchoredElement decode(Packet rsbytebuffer0) {
	    Class350_Sub3 class350Sub32 = (Class350_Sub3) Class350_Sub3.decode(rsbytebuffer0);
	    int i_3 = rsbytebuffer0.readShort();
	    return new Class350_Sub3_Sub1(class350Sub32.horizontalAnchor, class350Sub32.verticalAnchor,
	    		class350Sub32.offsetX, class350Sub32.offsetY,
	    		class350Sub32.width, class350Sub32.height,
	    		class350Sub32.anInt4092, class350Sub32.fileId, class350Sub32.anInt4091,
	    		class350Sub32.spriteA, class350Sub32.spriteB, class350Sub32.spriteC,
	    		class350Sub32.spriteD, class350Sub32.spriteE, class350Sub32.spriteF, i_3);
	}
}
