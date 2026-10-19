package com.rs.game.content.world.areas.braindeath_island

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Controller
import org.darkan.core.gameval.gen.Bas

class BraindeathUnderwaterController : Controller() {
    override fun start() {
        player.appearance.setBAS(Bas.human_swim)
    }

    override fun onTeleported(type: TeleType) {
        player.controllerManager.forceStop()
    }

    override fun login(): Boolean {
        player.appearance.setBAS(Bas.human_swim)
        return false
    }

    override fun logout() = false

    override fun sendDeath(): Boolean {
        player.controllerManager.forceStop()
        return true
    }

    override fun forceClose() {
        player.appearance.setBAS(-1)
    }

    override fun onRemove() {
        player.appearance.setBAS(-1)
    }
}
