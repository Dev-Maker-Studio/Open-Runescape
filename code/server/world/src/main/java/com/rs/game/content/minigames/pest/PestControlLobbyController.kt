package com.rs.game.content.minigames.pest

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.gameobject.GameObject

class PestControlLobbyController(tier: LanderTier) : Controller() {
    private val tier: LanderTier? = tier

    override fun start() = sendInterfaces()

    override fun sendInterfaces() {
        tier?.let { Lander.of(it).sendHud(player) }
    }

    override fun processObjectClick1(`object`: GameObject): Boolean {
        val tier = tier ?: return true
        if (LanderTier.forLadder(`object`.id) != tier) return true
        player.controllerManager.forceStop()
        player.useStairs(-1, tier.exitTile, 1, 2, "You leave the lander.")
        return false
    }

    override fun processTeleport(tele: Teleport): Boolean {
        if (tele.type() != TeleType.OBJECT) player.controllerManager.forceStop()
        return true
    }

    override fun onTeleported(type: TeleType) = player.controllerManager.forceStop()

    override fun forceClose() {
        tier?.let { Lander.of(it).remove(player) }
    }

    override fun logout(): Boolean {
        player.tile = (tier ?: LanderTier.NOVICE).exitTile
        return true
    }

    override fun login(): Boolean {
        player.tile = (tier ?: LanderTier.NOVICE).exitTile
        return true
    }

    override fun canSummonFamiliar(): Boolean {
        player.sendMessage("You feel it's best to keep your familiar away during this game.")
        return false
    }
}
