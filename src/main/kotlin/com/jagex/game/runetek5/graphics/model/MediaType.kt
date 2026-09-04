package com.jagex.game.runetek5.graphics.model

enum class MediaType(
    @JvmField val id: Int
) {
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

    fun getId(): Int {
        return id
    }

    companion object {
        @JvmStatic
        fun forId(id: Int): MediaType? {
            for (t in entries) {
                if (t.id == id)
                    return t
            }
            return null
        }
    }
}
