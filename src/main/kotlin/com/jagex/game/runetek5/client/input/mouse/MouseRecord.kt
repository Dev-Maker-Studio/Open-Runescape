package com.jagex.game.runetek5.client.input.mouse

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.Client

abstract class MouseRecord : Node<MouseRecord>() {

    abstract fun getY(): Int
    abstract fun getClickType(): Int
    abstract fun getTime(): Long
    abstract fun getMeta(): Int
    abstract fun pushToTop()
    abstract fun getX(): Int

    companion object {
        @JvmField var recorder: AbstractMouseManager? = null

        @JvmStatic
        fun record(record: MouseRecord) {
            if (!GameState.isRenderingScene(Client.gameState)) {
                record.pushToTop()
            } else {
                MouseActionServer.MOVE_PACKET.append(record)
            }
        }

        @JvmStatic
        fun clicktypeRequiresRecord(clickType: Int): Boolean {
            return clickType == 0 || clickType == 1 || clickType == 2
        }
    }
}
