package com.rs.game.content.minigames.pest

import com.rs.game.content.minigames.checkAndDeleteFoodAndPotions
import com.rs.game.content.minigames.giveFoodAndPotions
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.content.world.doors.Doors.handleDoubleDoor
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.map.pathfinder.Direction

private const val MUSIC = 588
private const val DEATH_JINGLE = 90
private const val REPAIR_DELAY = 3
private val REPAIR_LOGS = intArrayOf(Obj.logs, Obj.oak_logs, Obj.willow_logs, Obj.maple_logs, Obj.yew_logs, Obj.magic_logs)

class PestControlGameController(@Transient val game: PestControlGame?) : Controller() {
    @Transient
    private var loggingOut = false

    override fun playAmbientOnControllerRegionEnter() = false

    override fun playAmbientStrictlyBackgroundMusic() = true

    override fun start() {
        player.setForceMultiArea(true)
        player.musicsManager.playSongAndUnlock(MUSIC)
        sendInterfaces()
    }

    override fun sendInterfaces() {
        game?.sendHud(player)
    }

    override fun canMove(dir: Direction): Boolean {
        val game = game ?: return true
        return !game.blocksMovement(player.tile.transform(dir.dx, dir.dy))
    }

    override fun processObjectClick1(`object`: GameObject): Boolean {
        if (`object`.id in FortificationPart.gateLocs) {
            val gate = game?.fortifications?.at(`object`.tile)
            if (gate?.destroyed == true) {
                player.sendMessage("There's nothing left of this gate to open.")
                return false
            }
            handleDoubleDoor(player, `object`)
            gate?.removeRevealedTemplate()
            return false
        }
        return takeSupplies(`object`)
    }

    override fun processObjectClick2(`object`: GameObject) = takeSupplies(`object`)

    override fun processObjectClick3(`object`: GameObject): Boolean {
        val fortification = game?.fortifications?.at(`object`.tile) ?: return true
        repair(fortification)
        return false
    }

    private fun takeSupplies(table: GameObject): Boolean {
        if (table.id != Loc.snp_counter_bandages_blue) return true
        giveFoodAndPotions(player)
        return false
    }

    private fun repair(fortification: Fortification) {
        val log = REPAIR_LOGS.firstOrNull { player.inventory.containsItem(it, 1, false) }
        when {
            player.isLocked -> return
            !fortification.damaged -> player.sendMessage("It doesn't need repairing.")
            !fortification.closed -> player.sendMessage("You need to close the gate before you can repair it.")
            !player.inventory.containsItem(Obj.hammer) -> player.sendMessage("You need a hammer to repair this.")
            log == null -> player.sendMessage("You need some logs to repair this.")
            else -> {
                player.lock(REPAIR_DELAY)
                player.anim(Seq.pest_repair)
                player.inventory.deleteItem(log, 1)
                fortification.repair()
                game?.creditRepair(player)
            }
        }
    }

    override fun processTeleport(tele: Teleport): Boolean {
        if (tele.type() == TeleType.OBJECT) return true
        player.simpleDialogue("You can't leave the pest control area like this.")
        return false
    }

    override fun canSummonFamiliar(): Boolean {
        player.sendMessage("You feel it's best to keep your familiar away during this game.")
        return false
    }

    override fun sendDeath(): Boolean {
        val game = game ?: return true
        player.lock()
        player.schedule {
            player.anim(Seq.human_death)
            wait(1)
            player.sendMessage("Oh dear, you have died.")
            wait(2)
            player.tele(game.deckTile())
            player.anim(-1)
            player.jingle(DEATH_JINGLE)
            player.unlock()
            player.reset()
        }
        return false
    }

    override fun logout(): Boolean {
        loggingOut = true
        checkAndDeleteFoodAndPotions(player)
        game?.leave(player)
        player.tile = (game?.tier ?: LanderTier.NOVICE).exitTile
        return true
    }

    override fun login(): Boolean {
        checkAndDeleteFoodAndPotions(player)
        player.tile = LanderTier.NOVICE.exitTile
        return true
    }

    override fun forceClose() {
        game?.leave(player)
        checkAndDeleteFoodAndPotions(player)
        player.setForceMultiArea(false)
        player.interfaceManager.removeOverlay()
        player.reset()
        if (!loggingOut) player.useStairs(-1, (game?.tier ?: LanderTier.NOVICE).exitTile, 1, 2)
    }
}
