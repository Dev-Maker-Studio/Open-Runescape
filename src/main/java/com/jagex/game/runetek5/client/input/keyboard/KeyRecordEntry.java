package com.jagex.game.runetek5.client.input.keyboard;

import com.jagex.game.runetek5.cache.node.Node;

/**
 * 
 * @author Brainpower - 18/04/2022
 *
 */
public class KeyRecordEntry extends Node implements KeyRecord {

	public long timeSubmitted;
    public char keyCharacter;
    public int keyCode;
    public int type;
    public int flags;

    @Override
    public int getType() {
        return this.type;
    }

    @Override
    public char getCharacter() {
        return this.keyCharacter;
    }

    @Override
    public int getCode() {
        return this.keyCode;
    }

    @Override
    public int getFlags() {
        return this.flags;
    }

    @Override
    public long getTimeSubmitted() {
        return this.timeSubmitted;
    }
}
