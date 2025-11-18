package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.ITipDetails;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class AnchoredElement implements ITipDetails {

    public final AnchorX horizontalAnchor;

    public final AnchorY verticalAnchor;

    public final int offsetX;

    public final int offsetY;

    public final int width;

    public final int height;

    public final int anInt4092;

    public final int fileId;

    public final int anInt4091;

    protected AnchoredElement(AnchorX horizontalAnchor, AnchorY verticalAnchor, int offX, int offY, int width, int height, int i_7, int fileId, int i_9) {
        this.horizontalAnchor = horizontalAnchor;
        this.verticalAnchor = verticalAnchor;
        this.offsetX = offX;
        this.offsetY = offY;
        this.width = width;
        this.height = height;
        this.anInt4092 = i_7;
        this.fileId = fileId;
        this.anInt4091 = i_9;
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return null;
    }
}
