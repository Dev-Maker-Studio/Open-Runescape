package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.tile.TileMessage;

/**
 * 
 * @author Brainpower - 06/04/2022
 *
 */
public class CutsceneTileMessage extends CutsceneAction {

    public final int absX;

    public final int absY;

    public final String minimenuText;

    public final int minimenuColor;

    public final int cycleDuration;

    public CutsceneTileMessage(Packet buffer) {
        super(buffer);
        this.absX = buffer.readUShort();
        this.absY = buffer.readUShort();
        this.minimenuText = buffer.readString();
        this.minimenuColor = buffer.readInt();
        this.cycleDuration = buffer.readUShort();
    }

    @Override
    public void perform() {
        TileMessage.offer(PlayerUpdate.MY_PLAYER_PLANE, this.absX, this.absY, MapRegion.tileheightForAbsolutePosition(this.absX, this.absY, PlayerUpdate.MY_PLAYER_PLANE), this.cycleDuration, this.minimenuColor, this.minimenuText);
    }
}
