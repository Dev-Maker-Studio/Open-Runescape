package com.jagex.game.runetek5.config.rsinterface.cs2

class CS2CallFrame {
    @JvmField
    var savedStringLocals: Array<Any?>? = null
    @JvmField
    var savedIntLocals: IntArray? = null
    @JvmField
    var savedLongLocals: LongArray? = null
    @JvmField
    var savedScript: CS2Script? = null
    @JvmField
    var savedInstrPtr: Int = -1
}
