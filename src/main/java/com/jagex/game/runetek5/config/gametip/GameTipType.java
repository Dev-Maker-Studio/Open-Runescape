package com.jagex.game.runetek5.config.gametip;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub1;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub2;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3_Sub1;
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem;
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning;
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning;
import com.jagex.game.runetek5.config.gametip.component.TipsBackground;
import com.jagex.game.runetek5.config.gametip.component.TipsSprite;

public class GameTipType {

    public ITipDetails[] tipDetailsCache;

    public void decode(Packet buffer) {
        this.tipDetailsCache = new ITipDetails[buffer.readUByte()];
        TipRenderDetails[] renderDetails = TipRenderDetails.values();
        for (int index = 0; index < this.tipDetailsCache.length; index++) {
            this.tipDetailsCache[index] = decode(buffer, renderDetails[buffer.readUByte()]);
        }
    }

    public ITipDetails decode(Packet buffer, TipRenderDetails details) {
        if (details == TipRenderDetails.BACKGROUND)
            return TipsBackground.decode(buffer);
        if (details == TipRenderDetails.aClass60602)
            return Class350_Sub2.decode(buffer);
        if (details == TipRenderDetails.OUTLINED)
            return OutlinedNewsItem.decode(buffer);
        if (details == TipRenderDetails.ROTATED_SPRITE)
            return RotatableSpritePositioning.decode(buffer);
        if (details == TipRenderDetails.SPRITE)
            return SpritePositioning.decode(buffer);
        if (details == TipRenderDetails.aClass60603)
            return Class350_Sub1.decode(buffer);
        if (details == TipRenderDetails.aClass60608)
            return Class350_Sub3.decode(buffer);
        if (details == TipRenderDetails.ANCHORED_TEXT)
            return AnchoredTextMeta.decode(buffer);
        if (details == TipRenderDetails.aClass60601)
            return TipsSprite.decode(buffer);
        if (details == TipRenderDetails.aClass60610)
            return Class350_Sub3_Sub1.decode(buffer);
        return null;
    }
}
