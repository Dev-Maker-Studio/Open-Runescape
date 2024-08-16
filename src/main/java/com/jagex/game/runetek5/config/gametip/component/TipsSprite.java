package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.ITipDetails;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;

public class TipsSprite implements ITipDetails {

    public final int spriteFileId;

    TipsSprite(int spriteFileId) {
        this.spriteFileId = spriteFileId;
    }

    public static TipsSprite decode(Packet buffer) {
        int fileId = buffer.readBigSmart();
        return new TipsSprite(fileId);
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.aClass60601;
    }
}
