package com.darkan.bot.utils

import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectType

open class WorldObject : Tile {

    var id: Int = 0
        private set
    var rotation: Int = 0
    var type: Int = 0
        private set
    private var actions: Array<String>? = null

    constructor(id: Int, rotation: Int, type: Int, x: Int, y: Int, actions: Array<String>?) : super(x, y, 0) {
        this.id = id
        this.rotation = rotation
        this.actions = actions
        this.type = type
    }

    constructor(id: Int, rotation: Int, x: Int, y: Int) : super(x, y, 0) {
        this.id = id
        this.rotation = rotation
    }

    constructor(id: Int, x: Int, y: Int) : super(x, y, 0) {
        this.id = id
    }

    fun setId(id: Int) {
        this.id = id
    }

    override fun toString(): String {
        return "[$id@(${x}, ${y},$z)]"
    }

    fun containsOption(option: String): Int {
        if (actions == null)
            return -1
        for (i in actions!!.indices) {
            if (actions!![i].equals(option, ignoreCase = true))
                return i + 1
        }
        return -1
    }

    fun getDefinitions(): ObjectType {
        return TypeLists.LOC!!.list(id)
    }
}
