package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class Class350_Sub3 extends AnchoredElement {

    public final int spriteA;
    public final int spriteB;
    public final int spriteC;
    public final int spriteD;
    public final int spriteE;
    public final int spriteF;

    Class350_Sub3(AnchorX horizontalAnchor, AnchorY verticalAnchor, int offX, int offY, int width, int height, int i_7, int fileId, int i_9, int i_10, int i_11, int i_12, int i_13, int i_14, int i_15) {
        super(horizontalAnchor, verticalAnchor, offX, offY, width, height, i_7, fileId, i_9);
        this.spriteA = i_10;
        this.spriteB = i_11;
        this.spriteC = i_12;
        this.spriteD = i_13;
        this.spriteE = i_14;
        this.spriteF = i_15;
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.aClass60608;
    }

	public static AnchoredElement decode(Packet rsbytebuffer0) {
	    AnchoredElement class3502 = AnchoredElement.decode(rsbytebuffer0);
	    int i_3 = rsbytebuffer0.readBigSmart();
	    int i_4 = rsbytebuffer0.readBigSmart();
	    int i_5 = rsbytebuffer0.readBigSmart();
	    int i_6 = rsbytebuffer0.readBigSmart();
	    int i_7 = rsbytebuffer0.readBigSmart();
	    int i_8 = rsbytebuffer0.readBigSmart();
	    return new Class350_Sub3(class3502.horizontalAnchor, class3502.verticalAnchor, class3502.offsetX, class3502.offsetY, class3502.width, class3502.height, class3502.anInt4092, class3502.fileId, class3502.anInt4091, i_3, i_4, i_5, i_6, i_7, i_8);
	}

}
