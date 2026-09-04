package com.jagex.game.runetek5.client.window

data class WindowSettings(
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val borderWidth: Int,
    @JvmField val borderHeight: Int,
    @JvmField val name: String
) {
    fun borderWidth(): Int = borderWidth

    fun getName(): String = name

    fun getWidth(): Int = width

    fun borderHeight(): Int = borderHeight

    fun getHeight(): Int = height
}
