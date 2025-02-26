package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class RotatableSpritePositioning extends SpritePositioning {

    public final int utAngle;

    public RotatableSpritePositioning(int spriteId, AnchorX anchorX, AnchorY anchorY, int offsetX, int offsetY, int angle) {
        super(spriteId, anchorX, anchorY, offsetX, offsetY);
        this.utAngle = angle;
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.ROTATED_SPRITE;
    }

	public static SpritePositioning decode(Packet buffer) {
	    SpritePositioning positioning = SpritePositioning.decode(buffer);
	    int angle = buffer.readMedium();
	    return new RotatableSpritePositioning(positioning.spriteId, positioning.horizontalAnchor, positioning.verticalAnchor, positioning.spriteOffsetX, positioning.spriteOffsetY, angle);
	}
}
