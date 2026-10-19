package com.rs.game.content.minigames.barbarianassault

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import kotlin.math.ceil

private const val SETTLE_TICKS = 3

val Player.baWaitingController get() = controllerManager.controller as? BarbarianAssaultWaitingController

class BarbarianAssaultWaitingController(val room: WaitingRoom) : Controller() {
    @Transient
    var countdown = 0

    @Transient
    private var settling = SETTLE_TICKS

    override fun start() = refreshHud()

    override fun sendInterfaces() = refreshHud()

    fun refreshHud() {
        if (countdown <= 0) {
            player.interfaceManager.removeOverlay()
            return
        }
        player.interfaceManager.sendOverlay(Interface.interface_494)
        player.packets.setIFText(Component.interface_494.component_494_1, "${ceil(countdown * 0.6).toInt()}")
    }

    override fun process() {
        if (settling > 0) {
            settling--
            return
        }
        if (!room.contains(player.tile)) player.controllerManager.forceStop()
    }

    override fun processItemOnPlayer(p2: Player, item: Item, slot: Int): Boolean {
        if (item.id != Obj.barbassault_scroll) return true
        BarbarianAssaultTeams.invite(player, p2)
        return false
    }

    override fun processTeleport(tele: Teleport) = true

    override fun onTeleported(type: TeleType) {
        player.controllerManager.forceStop()
    }

    override fun logout(): Boolean {
        BarbarianAssaultTeams.leave(player)
        player.tile = room.outside
        return true
    }

    override fun login(): Boolean {
        player.tile = room.outside
        return true
    }

    override fun forceClose() {
        BarbarianAssaultTeams.leave(player)
        player.interfaceManager.removeOverlay()
    }
}

object BarbarianAssaultWaiting {
    fun enterRoom(player: Player, room: WaitingRoom) {
        player.controllerManager.startController(BarbarianAssaultWaitingController(room))
    }

    fun startCountdown(team: BarbarianAssaultTeam, wave: Wave, ticks: Int) {
        if (team.members.isEmpty() || team.game != null) return
        var remaining = ticks
        team.members.forEach { it.baWaitingController?.let { controller -> controller.countdown = remaining; controller.refreshHud() } }
        WorldTasks.scheduleTimer(1, 1) {
            if (team.nextWave != wave || team.game != null || team.members.isEmpty()) {
                clearCountdown(team)
                return@scheduleTimer false
            }
            remaining--
            team.members.forEach { it.baWaitingController?.let { controller -> controller.countdown = remaining; if (remaining % 5 == 0) controller.refreshHud() } }
            if (remaining > 0) return@scheduleTimer true
            clearCountdown(team)
            val refusal = team.ready()
            if (refusal != null) {
                team.sendTeam("The next wave can't start: ${refusal.replaceFirstChar(Char::lowercase)}")
                team.sendTeam("The leader can climb down the ladder once the team is ready.")
                return@scheduleTimer false
            }
            BarbarianAssaultGame(team, wave)
            false
        }
    }

    private fun clearCountdown(team: BarbarianAssaultTeam) = team.members.forEach {
        it.baWaitingController?.let { controller ->
            controller.countdown = 0
            controller.refreshHud()
        }
    }

    fun climbDown(player: Player, room: WaitingRoom) {
        val team = BarbarianAssaultTeams.of(player)
        val refusal = when {
            team == null -> "You need to form a team with the recruitment scroll before you can enter the arena."
            team.leader != player -> "Only your team leader can lead the team down."
            team.game != null -> "Your team is already in the arena."
            team.room != room -> "Your team is waiting in room ${team.room.ordinal + 1}."
            room.wave.number > 1 && team.leader.highestBarbarianAssaultWave < room.wave.number - 1 -> "You need to complete wave ${room.wave.number - 1} before you can lead a team into wave ${room.wave.number}."
            else -> team.ready()
        }
        if (refusal != null) {
            player.sendMessage(refusal)
            return
        }
        team!!.nextWave = null
        clearCountdown(team)
        team.sendTeam("${player.displayName} leads the team into wave ${room.wave.number}.")
        BarbarianAssaultGame(team, room.wave)
    }
}
