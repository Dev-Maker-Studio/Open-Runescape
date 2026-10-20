package com.jagex.game.runetek5.config.rsinterface

enum class ComponentContentType(
    @JvmField val id: Int
) {

    CONTENT_TYPE_328(328),

    GAME_WINDOW(1337),

    CONTENT_TYPE_1403(1403),

    MINIMAP(1338),

    COMPASS(1339),

    WORLDMAP(1400),

    WORLD_MAP_OVERVIEW(1401),

    DEBUG_INFORMATION(1405),

    CONTENT_TYPE_1406(1406),

    CUTSCENE_FADE(1407),

    COLOR_PICKER(1408),

    CONTENT_TYPE_1409(1409);

    companion object {
        @JvmStatic
        fun forId(id: Int): ComponentContentType? {
            return entries.firstOrNull { it.id == id }
        }
    }
}
