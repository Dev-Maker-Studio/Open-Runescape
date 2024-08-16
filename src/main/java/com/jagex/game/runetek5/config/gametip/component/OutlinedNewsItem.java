package com.jagex.game.runetek5.config.gametip.component;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.gametip.ITipDetails;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;

public class OutlinedNewsItem implements ITipDetails {

    public final int newsitemId;
    public final AnchorX horizontalAnchor;
    public final AnchorY verticalAnchor;
    public final int offsetX;
    public final int offsetY;
    public final int width;
    public final int height;
    public final int textColor;
    public final int shadeColor;
    public final int backgroundColor;
    public final boolean drawOutline;

    public OutlinedNewsItem(int newsitemId, AnchorX horizontalAnchor, AnchorY verticalAnchor, int offsetX, int offsetY, int width, int height, int textColor, int textShadeColor, int backgroundColor, boolean drawOutline) {
        this.newsitemId = newsitemId;
        this.horizontalAnchor = horizontalAnchor;
        this.verticalAnchor = verticalAnchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
        this.textColor = textColor;
        this.shadeColor = textShadeColor;
        this.backgroundColor = backgroundColor;
        this.drawOutline = drawOutline;
    }

    public static OutlinedNewsItem decode(Packet buffer) {
        int newsitemId = buffer.readUnsignedByte();
        AnchorX horizontalAnchor = AnchorX.values()[buffer.readUnsignedByte()];
        AnchorY verticalAnchor = AnchorY.values()[buffer.readUnsignedByte()];
        int offsetX = buffer.readShort();
        int offsetY = buffer.readShort();
        int width = buffer.readUnsignedShort();
        int height = buffer.readUnsignedShort();
        int textColor = buffer.readInt();
        int textShadeColor = buffer.readInt();
        int backgroundColor = buffer.readInt();
        boolean drawOutline = buffer.readUnsignedByte() == 1;
        return new OutlinedNewsItem(newsitemId, horizontalAnchor, verticalAnchor, offsetX, offsetY, width, height, textColor, textShadeColor, backgroundColor, drawOutline);
    }

    @Override
    public TipRenderDetails getRenderDetails() {
        return TipRenderDetails.OUTLINED;
    }

}
