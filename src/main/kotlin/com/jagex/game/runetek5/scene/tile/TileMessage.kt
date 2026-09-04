package com.jagex.game.runetek5.scene.tile

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.entity.EntityNode

class TileMessage : EntityNode() {
    @JvmField var absoluteY: Int = 0
    @JvmField var tileHeight: Int = 0
    @JvmField var minimenuColor: Int = 0
    @JvmField var plane: Int = 0
    @JvmField var lastCycle: Int = 0
    @JvmField var absoluteX: Int = 0
    @JvmField var minimenuText: String? = null

    companion object {
        @JvmStatic
        fun offer(plane: Int, absoluteX: Int, absoluteY: Int, tileHeight: Int, cycleDuration: Int, minimenuColor: Int, minimenuText: String?) {
            val tileMessage = TileMessage()
            tileMessage.plane = plane
            tileMessage.absoluteX = absoluteX
            tileMessage.absoluteY = absoluteY
            tileMessage.lastCycle = cycleDuration + Client.cycles
            tileMessage.tileHeight = tileHeight
            tileMessage.minimenuText = minimenuText
            tileMessage.minimenuColor = minimenuColor
            Client.TILE_MESSAGES.offer(tileMessage)
        }
    }
}
