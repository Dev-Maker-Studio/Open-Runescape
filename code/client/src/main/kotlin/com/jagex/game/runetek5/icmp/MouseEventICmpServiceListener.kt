package com.jagex.game.runetek5.icmp

import com.jagex.game.runetek5.client.input.mouse.MouseActionServer

class MouseEventICmpServiceListener : ICmpServiceListener {

    override fun onNotify(unused1: Boolean, unused2: Int) {
    }

    override fun onMouseEvent(bitpackedPosition: Int, meta: Int, constant: Int, unused: Int) {
        val positionX = bitpackedPosition shr 16
        val positionY = bitpackedPosition and 0xffff
        synchronized(MouseActionServer.MOUSE_ACTIONS) {
            MouseActionServer.MOUSE_ACTIONS.add(MouseActionServer.createMouseRecord(positionX, positionY, meta, constant))
        }
    }
}
