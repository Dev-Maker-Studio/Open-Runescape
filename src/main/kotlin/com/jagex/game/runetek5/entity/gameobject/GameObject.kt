package com.jagex.game.runetek5.entity.gameobject

import com.jagex.game.runetek5.toolkit.AbstractToolkit

interface GameObject {
    val id: Int
    val shape: Int
    val rotation: Int

    fun upload()

    fun hasRenderingShadow(): Boolean

    fun removeShadow(toolkit: AbstractToolkit)

    fun addShadow(toolkit: AbstractToolkit)
}
