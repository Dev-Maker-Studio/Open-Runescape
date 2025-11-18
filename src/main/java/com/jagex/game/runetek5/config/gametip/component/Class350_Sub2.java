package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

import static com.jagex.game.runetek5.config.gametip.component.Class350_Sub3_Sub1Kt.decodeAnchored;

public class Class350_Sub2 extends AnchoredElement {

    public final int anInt7813;

    public final int anInt7814;

    public Class350_Sub2(AnchorX class3561, AnchorY class3532, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int i_9, int i_10, int i_11) {
        super(class3561, class3532, i_3, i_4, i_5, i_6, i_7, i_8, i_9);
        this.anInt7813 = i_10;
        this.anInt7814 = i_11;
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.aClass60602;
    }

	public static AnchoredElement decode(Packet rsbytebuffer0) {
	    AnchoredElement anchoredElement = decodeAnchored(rsbytebuffer0);
	    int i_3 = rsbytebuffer0.readInt();
	    int i_4 = rsbytebuffer0.readInt();
	    return new Class350_Sub2(anchoredElement.horizontalAnchor, anchoredElement.verticalAnchor,
	    		anchoredElement.offsetX, anchoredElement.offsetY,
	    		anchoredElement.width, anchoredElement.height,
	    		anchoredElement.anInt4092, anchoredElement.fileId, anchoredElement.anInt4091, i_3, i_4);
	}
}
