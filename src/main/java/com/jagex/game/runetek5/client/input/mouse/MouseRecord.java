package com.jagex.game.runetek5.client.input.mouse;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.client;

public abstract class MouseRecord extends Node {

    public static AbstractMouseManager recorder;

    public static void record(MouseRecord record) {
        if (!GameState.isRenderingScene(client.gameState)) {
            record.pushToTop();
        } else {
            MouseActionServer.MOVE_PACKET.append(record);
        }
    }

    public abstract int getY();

    public abstract int getClickType();

    public abstract long getTime();

    public abstract int getMeta();

    public abstract void pushToTop();

    public abstract int getX();

	public static boolean clicktypeRequiresRecord(int clickType) {
	    return clickType == 0 || clickType == 1 || clickType == 2;
	}
}
