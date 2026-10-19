package com.rs.game.content.bosses.godwars.factions.zaros

import com.rs.engine.cutscene.Cutscene
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile

private const val CAMERA_HEIGHT = 2500

class NexCutScene(private val camera: Tile, private val lookAt: Tile) : Cutscene() {
    override fun construct(player: Player) {
        camPos(camera.x, camera.y, CAMERA_HEIGHT, -1)
        camLook(lookAt.x, lookAt.y, CAMERA_HEIGHT, 3)
    }
}
