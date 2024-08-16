package com.jagex.game.runetek5.scene.tile;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.entity.EntityNode;

public class TileMessage extends EntityNode {

	public int absoluteY;
	public int tileHeight;
	public int minimenuColor;
    public int plane;
    public int lastCycle;
    public int absoluteX;
    public String minimenuText;

    public static void offer(int plane, int absoluteX, int absoluteY, int tileHeight, int cycleDuration, int minimenuColor, String minimenuText) {
        TileMessage tileMessage = new TileMessage();
        tileMessage.plane = plane;
        tileMessage.absoluteX = absoluteX;
        tileMessage.absoluteY = absoluteY;
        tileMessage.lastCycle = cycleDuration + client.cycles;
        tileMessage.tileHeight = tileHeight;
        tileMessage.minimenuText = minimenuText;
        tileMessage.minimenuColor = minimenuColor;
        client.TILE_MESSAGES.offer(tileMessage);
    }
}
