package com.jagex.game.runetek5.config.rsinterface

enum class ComponentType(
    @JvmField val id: Int
) {
    CONTAINER(0),
    TYPE_1(1),
    TYPE_2(2),
    FIGURE(3),
    TEXT(4),
    SPRITE(5),
    MODEL(6),
    TYPE_7(7),
    TYPE_8(8),
    LINE(9);

    companion object {
        @JvmStatic
        fun forId(id: Int): ComponentType? {
            for (t in values()) {
                if (t.id == id)
                    return t
            }
            return null
        }
    }
}
