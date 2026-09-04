package com.darkan.bot.utils

class Area(var bottomLeft: Tile, var topRight: Tile) {

    fun within(tile: Tile): Boolean {
        return ((tile.x >= bottomLeft.x) && (tile.y >= bottomLeft.y) &&
                (tile.x <= topRight.x) && (tile.y <= topRight.y))
    }
}
