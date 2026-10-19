package com.rs.game.content.minigames.creations

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Varc
import kotlin.math.ceil

private val SKILLING_STATS = intArrayOf(Constants.WOODCUTTING, Constants.MINING, Constants.FISHING, Constants.HUNTER, Constants.COOKING, Constants.HERBLORE, Constants.CRAFTING, Constants.SMITHING, Constants.FLETCHING, Constants.RUNECRAFTING, Constants.CONSTRUCTION)
private val COMBAT_STATS = intArrayOf(Constants.ATTACK, Constants.STRENGTH, Constants.DEFENSE, Constants.HITPOINTS, Constants.RANGE, Constants.MAGIC, Constants.PRAYER, Constants.SUMMONING)
private val COUNTDOWN = Ticks.fromMinutes(2)
private val COUNTDOWN_REFRESH = Ticks.fromMinutes(1)
private const val MIN_TEAM_SIZE = 5
private const val COMPONENT_OWN_SKILL = Component.interface_804.component_804_4
private const val COMPONENT_OWN_COMBAT = Component.interface_804.component_804_5
private const val COMPONENT_ENEMY_COMBAT = Component.interface_804.component_804_6
private const val COMPONENT_ENEMY_SKILL = Component.interface_804.component_804_7

object StealingCreationLobby {
    private val redTeam = ArrayList<Player>()
    private val blueTeam = ArrayList<Player>()
    private var countdown = 0
    private var countdownGeneration = 0

    fun isRed(player: Player) = player in redTeam

    fun enterTeamLobby(player: Player, red: Boolean): Boolean {
        if (!canEnter(player, red)) return false
        val team = if (red) redTeam else blueTeam
        if (player !in team) team += player
        player.controllerManager.startController(StealingCreationLobbyController())
        if (countdown == 0 && hasRequiredPlayers()) startCountdown()
        refreshAll()
        return true
    }

    fun removePlayer(player: Player) {
        if (!redTeam.remove(player)) blueTeam.remove(player)
        if (!hasRequiredPlayers()) countdown = 0
        player.interfaceManager.removeOverlay(false)
        refreshAll()
    }

    private fun hasRequiredPlayers() = redTeam.size >= MIN_TEAM_SIZE && blueTeam.size >= MIN_TEAM_SIZE

    private fun startCountdown() {
        countdown = COUNTDOWN
        val generation = ++countdownGeneration
        WorldTasks.scheduleTimer(1, 0) {
            if (generation != countdownGeneration || countdown <= 0) return@scheduleTimer false
            if (--countdown == 0) {
                StealingCreationGameController.start(blueTeam.toList(), redTeam.toList())
                blueTeam.clear()
                redTeam.clear()
                return@scheduleTimer false
            }
            if (countdown % COUNTDOWN_REFRESH == 0) refreshAll()
            true
        }
    }

    private fun refreshAll() {
        redTeam.forEach { updateTeamInterface(it, true) }
        blueTeam.forEach { updateTeamInterface(it, false) }
    }

    fun updateTeamInterface(player: Player, red: Boolean) {
        player.packets.setIFText(COMPONENT_OWN_SKILL, totalLevel(SKILLING_STATS, red).toString())
        player.packets.setIFText(COMPONENT_OWN_COMBAT, totalLevel(COMBAT_STATS, red).toString())
        player.packets.setIFText(COMPONENT_ENEMY_COMBAT, totalLevel(COMBAT_STATS, !red).toString())
        player.packets.setIFText(COMPONENT_ENEMY_SKILL, totalLevel(SKILLING_STATS, !red).toString())
        player.packets.sendVarc(Varc.varc_554, (if (red) redTeam else blueTeam).size)
        player.packets.sendVarc(Varc.varc_555, (if (red) blueTeam else redTeam).size)
        player.packets.sendVarc(Varc.varc_550, countdown)
    }

    private fun canEnter(player: Player, red: Boolean): Boolean {
        val deserterTicks = player.tempAttribs.getL(StealingCreation.DESERTER_KEY) - World.getServerTicks()
        val refusal = when {
            totalLevel(SKILLING_STATS, red) + totalLevel(COMBAT_STATS, red) > totalLevel(SKILLING_STATS, !red) + totalLevel(COMBAT_STATS, !red) ->
                "This team is too strong for you to join at present."
            player.equipment.wearingArmour() || player.inventory.freeSlots != player.inventory.itemsContainerSize || player.familiar != null || player.pet != null ->
                "You may not take any items into Stealing Creation. You can use the nearby bank deposit box to empty your inventory and worn items."
            deserterTicks > 0 -> {
                player.simpleDialogue("You have betrayed the mystics and must wait ${ceil(deserterTicks / Ticks.fromMinutes(1).toDouble()).toInt()} minutes.")
                return false
            }
            else -> return true
        }
        player.sendMessage(refusal)
        return false
    }

    private fun totalLevel(stats: IntArray, red: Boolean) = (if (red) redTeam else blueTeam).sumOf { player -> stats.sumOf { player.skills.getLevel(it) } }
}
