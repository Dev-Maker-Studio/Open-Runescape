package com.rs.game.content.minigames.barbarianassault

import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import org.darkan.core.model.GroundItem
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item

private const val DEATH_TICKS = 3

val Player.baGameController get() = controllerManager.controller as? BarbarianAssaultController
val Player.baGame get() = baGameController?.game

class BarbarianAssaultController(@Transient var game: BarbarianAssaultGame?) : Controller() {
    override fun start() {
        player.setForceMultiArea(true)
        sendInterfaces()
    }

    override fun sendInterfaces() {
        game?.sendHud(player)
    }

    override fun processOutgoingHit(hit: Hit, target: Entity) {
        game?.validateHit(player, hit, target)
    }

    override fun canAttack(target: Entity) = game?.canAttack(player, target) ?: false

    override fun canTakeItem(item: GroundItem) = game?.takeGroundItem(player, item) ?: true

    override fun canDropItem(item: Item): Boolean {
        if (item.id !in ARENA_ONLY_OBJS) return true
        player.sendMessage("You can't drop that here.")
        return false
    }

    override fun canSummonFamiliar(): Boolean {
        player.sendMessage("You can't bring a familiar into the arena.")
        return false
    }

    override fun canTrade() = false

    override fun allowsBotItemTransfer() = true

    override fun processTeleport(tele: Teleport): Boolean {
        player.sendMessage("You can't teleport out of the arena; climb the ladder to leave.")
        return false
    }

    override fun sendDeath(): Boolean {
        val game = game ?: return true
        player.lock()
        player.stopAll()
        player.schedule {
            player.anim(Seq.human_death)
            wait(DEATH_TICKS)
            player.sendMessage("Oh dear, you have died.")
            game.playerDied(player)
        }
        return false
    }

    override fun logout(): Boolean {
        val game = game
        this.game = null
        game?.leave(player)
        BarbarianAssaultTeams.leave(player)
        player.tile = LOBBY_ARRIVAL
        return true
    }

    override fun login(): Boolean {
        player.tile = LOBBY_ARRIVAL
        ARENA_ONLY_OBJS.forEach { player.inventory.deleteItem(it, Int.MAX_VALUE) }
        return true
    }

    override fun forceClose() {
        val game = game
        this.game = null
        game?.leave(player)
        BarbarianAssaultTeams.leave(player)
        player.setForceMultiArea(false)
        player.interfaceManager.removeOverlay()
    }
}
