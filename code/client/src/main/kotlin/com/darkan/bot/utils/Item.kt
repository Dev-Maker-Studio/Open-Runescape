package com.darkan.bot.utils

import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.item.ItemType

class Item {

    val id: Int
    val amount: Int
    val position: Tile

    constructor(id: Int, amount: Int, position: Tile) {
        this.id = id
        this.amount = amount
        this.position = position
    }

    constructor(id: Int, position: Tile) {
        this.id = id
        this.position = position
        this.amount = 1
    }

    fun getDefinitions(): ItemType {
        return TypeLists.OBJ!!.list(id)
    }

    fun getName(): String {
        return TypeLists.OBJ!!.list(id).name
    }

    override fun toString(): String {
        return "[$id@(${position.x}, ${position.y})"
    }
}
