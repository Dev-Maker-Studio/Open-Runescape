package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.ITipDetails;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class SpritePositioning implements ITipDetails {

    public final int spriteId;

    public final AnchorX horizontalAnchor;

    public final AnchorY verticalAnchor;

    public final int spriteOffsetX;

    public final int spriteOffsetY;

    SpritePositioning(int spriteId, AnchorX anchorX, AnchorY anchorY, int offsetX, int offsetY) {
        this.spriteId = spriteId;
        this.horizontalAnchor = anchorX;
        this.verticalAnchor = anchorY;
        this.spriteOffsetX = offsetX;
        this.spriteOffsetY = offsetY;
    }

    public static SpritePositioning decode(Packet buffer) {
        int spriteId = buffer.readBigSmart();
        AnchorX anchorX = AnchorX.values()[buffer.readUnsignedByte()];
        AnchorY anchorY = AnchorY.values()[buffer.readUnsignedByte()];
        int offsetX = buffer.readShort();
        int offsetY = buffer.readShort();
        return new SpritePositioning(spriteId, anchorX, anchorY, offsetX, offsetY);
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.SPRITE;
    }
}
