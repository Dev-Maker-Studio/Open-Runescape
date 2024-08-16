package com.jagex.game.runetek5.config.skybox;

import com.jagex.game.runetek5.config.Identifiable;

public enum BackgroundMode implements Identifiable {

    SINGLE_COLOR(0),
    COLOR_TRANSITION(1);

    public final int value;

    BackgroundMode(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return this.value;
    }
}
