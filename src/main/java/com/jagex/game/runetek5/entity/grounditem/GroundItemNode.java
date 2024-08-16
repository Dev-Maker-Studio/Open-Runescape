package com.jagex.game.runetek5.entity.grounditem;

import com.jagex.game.runetek5.cache.node.Node;

public class GroundItemNode extends Node {

    public final int id;
    public int amount;

    public GroundItemNode(int i_1, int i_2) {
        this.id = i_1;
        this.amount = i_2;
    }

}
