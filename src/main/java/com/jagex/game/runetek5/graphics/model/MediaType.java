package com.jagex.game.runetek5.graphics.model;

public enum MediaType {
    UNDEFINED(0),
    RAW_MODEL(1),
    NPC_HEAD(2),
    PLAYER_HEAD(3),
    ITEM(4),
    PLAYER_MODEL(5),
    NPC_MODEL(6),
    PLAYER_HEAD_IGNOREWORN(7),
    ITEM_CONTAINER_MALE(8),
    ITEM_CONTAINER_FEMALE(9);

    public final int id;

    MediaType(int id) {
        this.id = id;
    }

    public static MediaType forId(int id) {
        for (MediaType t : MediaType.values()) {
            if (t.id == id)
                return t;
        }
        return null;
    }

    public int getId() {
        return this.id;
    }
}
