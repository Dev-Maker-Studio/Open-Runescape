package com.jagex.game.runetek5.entity.pathingentity.npc;

import com.jagex.game.runetek5.config.Identifiable;

public enum NPCDirection implements Identifiable {
    NORTH(0, 0),
    EAST(2, 1),
    SOUTH_EAST(3, 2),
    NORTH_WEST(7, 3),
    SOUTH(6, 4),
    WEST(4, 5),
    NORTH_EAST(5, 6),
    SOUTH_WEST(1, 7);

    public final int value;
    public final int direction;

    NPCDirection(int value, int direction) {
        this.value = value;
        this.direction = direction;
    }


    public NPCDirection getRotation() {
        switch (this.value) {
            case 0:
                return SOUTH;
            case 1:
                return NORTH_WEST;
            case 2:
                return WEST;
            case 3:
                return NORTH_EAST;
            case 4:
                return EAST;
            case 5:
                return SOUTH_EAST;
            case 6:
                return NORTH;
            case 7:
                return SOUTH_WEST;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public int getValue() {
        return this.direction;
    }
}
