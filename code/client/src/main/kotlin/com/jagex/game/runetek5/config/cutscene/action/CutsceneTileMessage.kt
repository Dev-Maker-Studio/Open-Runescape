package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.tile.TileMessage

class CutsceneTileMessage(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val absX: Int = buffer.readUShort()
    @JvmField val absY: Int = buffer.readUShort()
    @JvmField val minimenuText: String = buffer.readString()
    @JvmField val minimenuColor: Int = buffer.readInt()
    @JvmField val cycleDuration: Int = buffer.readUShort()

    override fun perform() {
        TileMessage.offer(PlayerUpdate.MY_PLAYER_PLANE, this.absX, this.absY, MapRegion.tileheightForAbsolutePosition(this.absX, this.absY, PlayerUpdate.MY_PLAYER_PLANE), this.cycleDuration, this.minimenuColor, this.minimenuText)
    }
}
