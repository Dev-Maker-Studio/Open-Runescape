package com.jagex.game.runetek5.icmp

interface ICmpServiceListener {
    fun onMouseEvent(bitpackedPosition: Int, meta: Int, constant: Int, unused: Int)
    fun onNotify(bool: Boolean, i: Int)
}
