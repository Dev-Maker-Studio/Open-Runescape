package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.config.Identifiable;

public enum MovementType implements Identifiable {
    STATIONARY((byte) -1),
    HALF_WALK((byte) 0),
    WALKING((byte) 1),
    RUNNING((byte) 2);

    public final byte id;

    MovementType(byte b_1) {
        this.id = b_1;
    }

    @Override
    public int getValue() {
        return this.id;
    }

    public int getValueIncr() {
        return this.id + 1;
    }
}
