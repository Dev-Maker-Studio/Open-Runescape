package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.runetek5.cache.node.Node

open class ProgramAttribute : Node<ProgramAttribute>() {
    @JvmField
    var attributeName: String? = null
    @JvmField
    var programIndex: Int = 0
}
