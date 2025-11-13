package com.jagex.game.runetek5.graphics.loginscreen

interface DrawableItem {
    fun isCached(): Boolean
    fun draw(var1: Boolean)
    fun initialize()
}
