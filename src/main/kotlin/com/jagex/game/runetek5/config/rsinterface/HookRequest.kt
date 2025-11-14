package com.jagex.game.runetek5.config.rsinterface

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
class HookRequest : Node<Any?>() {
    @JvmField
    var anInt8051: Int = 0

    @JvmField
    var hasMousePosition: Boolean = false

    @JvmField
    var source: Component? = null

    @JvmField
    var params: Array<Any?>? = null

    @JvmField
    var mouseY: Int = 0

    @JvmField
    var typedKeyChar: Int = 0

    @JvmField
    var aClass1188057: Component? = null

    @JvmField
    var typedKeyCode: Int = 0

    @JvmField
    var mouseX: Int = 0

    @JvmField
    var optionName: String? = null

    @JvmField
    var requestId: Int = 0
}
