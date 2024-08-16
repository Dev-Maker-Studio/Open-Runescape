package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.cache.node.Node;

public class ClickzoneMEC extends Node {

    public int spriteLeft = Integer.MAX_VALUE;
    public int spriteRight = Integer.MIN_VALUE;
    public int spriteBottom = Integer.MAX_VALUE;
    public int spriteTop = Integer.MIN_VALUE;
    public int textLeft = Integer.MAX_VALUE;
    public int textRight = Integer.MIN_VALUE;
    public int textBottom = Integer.MAX_VALUE;
    public int textTop = Integer.MIN_VALUE;
    public final WorldMapCategory category;

    public ClickzoneMEC(WorldMapCategory category) {
        this.category = category;
    }

    public boolean mouseWithin(int mouseX, int mouseY) {
        return mouseX >= this.spriteLeft && mouseX <= this.spriteRight && mouseY >= this.spriteBottom && mouseY <= this.spriteTop
        		|| mouseX >= this.textLeft && mouseX <= this.textRight && mouseY >= this.textBottom && mouseY <= this.textTop;
    }
}
