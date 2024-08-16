package com.jagex.game.runetek5.graphics.ifcomponent;

import com.jagex.game.runetek5.cache.node.Node;

public class Item extends Node<Item> {

    public final int id;
	public final int amount;
	public final int outlineSize;
	public final int shadowColor;
	public final int anInt7819;
	public final boolean hasPlayerAppearance;

    public Item(int id, int amount, int outlineSize, int shadowColor, int i_5, boolean hasCustomisation) {
        this.id = id;
        this.amount = amount;
        this.outlineSize = outlineSize;
        this.shadowColor = shadowColor;
        this.anInt7819 = i_5;
        this.hasPlayerAppearance = hasCustomisation;
    }

}
