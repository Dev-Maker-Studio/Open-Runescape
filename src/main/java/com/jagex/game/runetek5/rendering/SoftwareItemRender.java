package com.jagex.game.runetek5.rendering;

import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.graphics.sprite.IItemRenderer;
import com.jagex.game.runetek5.tools.Hashing;

public class SoftwareItemRender implements IItemRenderer {

	public int outlineSize;

	public int itemAmount;

	public int itemId;

	public int rendererId;

	public int shadowColor;

	public Component.StackMode renderStack;

	public boolean hasPlayerAppearance;

    @Override
    public boolean matches(IItemRenderer itemRenderer) {
        if (!(itemRenderer instanceof SoftwareItemRender softwareItemRenderer)) {
            return false;
        } else {
            return softwareItemRenderer.rendererId == this.rendererId
            		&& (softwareItemRenderer.itemId == this.itemId
            		&& (softwareItemRenderer.itemAmount == this.itemAmount
            		&& (softwareItemRenderer.outlineSize == this.outlineSize
            		&& (softwareItemRenderer.shadowColor == this.shadowColor
            		&& (softwareItemRenderer.renderStack == this.renderStack
            		&& softwareItemRenderer.hasPlayerAppearance == this.hasPlayerAppearance)))));
        }
    }

    @Override
    public long getUid() {
        long[] table = Hashing.CRC64_TABLE;
        long hash = -1L;
        hash = hash >>> 8 ^ table[(int) ((hash ^ this.rendererId) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.itemId >> 8)) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ this.itemId) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.itemAmount >> 24)) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.itemAmount >> 16)) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.itemAmount >> 8)) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ this.itemAmount) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ this.outlineSize) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.shadowColor >> 24)) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.shadowColor >> 16)) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.shadowColor >> 8)) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ this.shadowColor) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ this.renderStack.ordinal()) & 0xffL)];
        hash = hash >>> 8 ^ table[(int) ((hash ^ (this.hasPlayerAppearance ? 1 : 0)) & 0xffL)];
        return hash;
    }
}
