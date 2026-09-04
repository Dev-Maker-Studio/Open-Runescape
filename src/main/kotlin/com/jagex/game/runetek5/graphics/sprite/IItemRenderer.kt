package com.jagex.game.runetek5.graphics.sprite

interface IItemRenderer {
    fun getUid(): Long
    fun matches(other: IItemRenderer): Boolean
}
