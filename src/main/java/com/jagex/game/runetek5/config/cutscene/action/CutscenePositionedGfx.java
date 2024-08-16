package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.sequence.PositionedGfx;
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation;
import com.jagex.game.runetek5.scene.MapRegion;

/**
 * 
 * @author Brainpower - 01/02/2023
 *
 */
public class CutscenePositionedGfx extends AbstractCutsceneSpotAnimation {

    public final int x;
    public final int y;
    public final int plane;

    public CutscenePositionedGfx(Packet buffer) {
        super(buffer);
        int bitpackedPosition = buffer.readInt();
        this.x = bitpackedPosition >>> 16;
        this.y = bitpackedPosition & 0xffff;
        this.plane = buffer.readUnsignedByte();
    }

    @Override
    public void perform() {
        int absX = this.x * 512 + 256;
        int absY = this.y * 512 + 256;
        int height = this.plane;
        if (height < 3 && Resource.mapRegion.getSettingsBits().areRoofsHidden(this.x, this.y)) {
            height++;
        }

        SpotAnimation gfx = new SpotAnimation(Resource.mapRegion.getScene(), super.gfxId, 0, this.plane, height, absX,
        		MapRegion.tileheightForAbsolutePosition(absX, absY, this.plane) - super.displayHeight, absY, this.x, this.x, this.y, this.y, super.rotation, false);
        client.ACTIVE_GFX.put(new PositionedGfx(gfx), (long) this.x << 16 | this.y);
    }

}
