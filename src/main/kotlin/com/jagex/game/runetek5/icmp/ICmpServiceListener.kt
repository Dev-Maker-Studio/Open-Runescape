package com.jagex.game.runetek5.icmp

interface ICmpServiceListener {
    fun onMouseMoved(bitpackedPosition: Int, meta: Int, constant: Int, unused: Int)
    fun dummy(bool: Boolean, i: Int)
}
