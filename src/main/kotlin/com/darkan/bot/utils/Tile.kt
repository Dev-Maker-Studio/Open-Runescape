package com.darkan.bot.utils

open class Tile(var x: Int, var y: Int, var z: Int = 0) {

    constructor(x: Int, y: Int) : this(x, y, 0)

    fun equals(x: Int, y: Int): Boolean {
        return this.x == x && this.y == y
    }
}
