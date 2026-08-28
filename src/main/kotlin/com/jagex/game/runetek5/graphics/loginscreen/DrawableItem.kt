package com.jagex.game.runetek5.graphics.loginscreen

interface DrawableItem {
    fun isCached(): Boolean
    fun draw(draw: Boolean)
    fun initialize()
}
