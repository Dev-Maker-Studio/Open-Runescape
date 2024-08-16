package com.jagex.game.runetek5.graphics.hint;

public class HintDetails {

    public int ticksVisible;
    public int colorType;
    public String text;
    public int drawType;
    public int ticksLeft;

    public String getText() {
        return this.text;
    }

    public int getColorType() {
        return this.colorType;
    }

    public int getDrawType() {
        return this.drawType;
    }

    public int getTotalTicksVisible() {
        return this.ticksVisible;
    }

    public int getTicksLeft() {
        return this.ticksLeft;
    }
}
