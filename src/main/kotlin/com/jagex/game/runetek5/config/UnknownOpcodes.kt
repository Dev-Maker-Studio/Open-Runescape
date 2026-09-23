package com.jagex.game.runetek5.config

object UnknownOpcodes {

    @JvmField
    var sink: MutableList<Int>? = null

    @JvmStatic
    fun record(opcode: Int) {
        sink?.add(opcode)
    }
}
