package com.jagex.game.runetek5.framebuffer

import com.jagex.game.runetek5.toolkit.companion.Binding

interface BoundSurface : Binding {
    fun getUseHeight(): Int
    fun getUseWidth(): Int
}
