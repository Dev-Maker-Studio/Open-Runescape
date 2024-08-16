package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.ITipDetails;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;

public class TipsBackground implements ITipDetails {

    public final int color;

    public TipsBackground(int color) {
        this.color = color;
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.BACKGROUND;
    }

	public static TipsBackground decode(Packet buffer) {
	    int color = buffer.readInt();
	    return new TipsBackground(color);
	}
}
