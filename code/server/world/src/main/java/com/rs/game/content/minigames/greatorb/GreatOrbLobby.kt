package com.rs.game.content.minigames.greatorb

import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item
import kotlin.math.abs
import kotlin.math.ceil

private const val REQUIRED_LEVEL = 50
private const val MAX_TEAM = 5
private const val FREE_SLOTS = 2
private const val HUD_REFRESH = 5
private val COUNTDOWN = Ticks.fromMinutes(1)

object GreatOrbLobby {
    const val MIN_TEAM = 2
    const val EXCLUDED_UNTIL = "greatOrbExcludedUntil"

    private val teams = OrbTeam.entries.associateWith { LinkedHashSet<Player>() }
    private var game: GreatOrbGame? = null
    private var countdown = 0
    private var generation = 0

    fun join(player: Player, preferred: OrbTeam?) {
        val excluded = player.tempAttribs.getL(EXCLUDED_UNTIL) - World.getServerTicks()
        val refusal = when {
            player.greatOrbController != null -> "You are already on a team."
            game != null -> "The wizards are busy with a game at the moment. Wait for it to finish, then join a team."
            excluded > 0 -> "You left the last game early. You can join another in ${ceil(excluded / Ticks.fromMinutes(1).toDouble()).toInt()} minute(s)."
            player.skills.getLevelForXp(Skills.RUNECRAFTING) < REQUIRED_LEVEL -> "You need a Runecrafting level of $REQUIRED_LEVEL to take part in the Great Orb Project."
            !player.isQuestComplete(Quest.RUNE_MYSTERIES) -> "You need to complete Rune Mysteries to take part in the Great Orb Project."
            player.equipment.hatId != -1 || player.equipment.weaponId != -1 || player.inventory.freeSlots < FREE_SLOTS ->
                "You need an empty hat slot, an empty weapon slot and $FREE_SLOTS free inventory spaces to join a team."
            player.familiar != null || player.pet != null -> "You can't take a follower into the Great Orb Project."
            else -> null
        }
        if (refusal != null) {
            player.simpleDialogue(refusal)
            return
        }
        val team = preferred ?: OrbTeam.entries.minWith(compareBy<OrbTeam> { teams.getValue(it).size }.thenBy { Math.random() })
        val size = teams.getValue(team).size
        when {
            size >= MAX_TEAM -> player.simpleDialogue("The ${team.displayName} team is full.")
            size > teams.getValue(team.opponent).size -> player.simpleDialogue("The ${team.displayName} team has too many players already. Try joining the ${team.opponent.displayName} team.")
            else -> enrol(player, team)
        }
    }

    private fun enrol(player: Player, team: OrbTeam) {
        player.stopAll()
        player.equipment.setSlot(Equipment.HEAD, Item(team.hat))
        player.equipment.setSlot(Equipment.WEAPON, Item(team.attractor))
        player.inventory.addItem(team.repeller, 1)
        player.inventory.addItem(team.barrierGenerator, 1)
        player.appearance.generateAppearanceData()
        player.vars.setVarBit(Varbit.rcguild_team, team.varbitValue)
        player.vars.setVarBit(Varbit.rcguild_activity, ACTIVITY_MAX)
        teams.getValue(team) += player
        player.controllerManager.startController(GreatOrbController(team))
        player.sendMessage("You join the ${team.displayName} team.")
        refreshCountdown()
    }

    fun leave(player: Player) {
        if (teams.values.none { it.remove(player) }) return
        refreshCountdown()
    }

    fun gameOver(finished: GreatOrbGame) {
        if (game == finished) game = null
    }

    private fun ready() = teams.values.all { it.size >= MIN_TEAM } && abs(teams.getValue(OrbTeam.GREEN).size - teams.getValue(OrbTeam.YELLOW).size) <= 1

    private fun refreshCountdown() {
        if (!ready()) {
            countdown = 0
            generation++
        } else if (countdown == 0) {
            countdown = COUNTDOWN
            val clock = ++generation
            WorldTasks.scheduleTimer(1, 0) {
                if (clock != generation) return@scheduleTimer false
                if (--countdown <= 0) {
                    launch()
                    return@scheduleTimer false
                }
                if (countdown % HUD_REFRESH == 0) refreshAll()
                true
            }
        }
        refreshAll()
    }

    private fun launch() {
        countdown = 0
        generation++
        val roster = teams.mapValues { it.value.toList() }
        teams.values.forEach { it.clear() }
        game = GreatOrbGame(roster)
    }

    fun sendHud(player: Player) {
        player.interfaceManager.sendOverlay(Interface.interface_781)
        refresh(player)
    }

    private fun refreshAll() = teams.values.flatten().forEach(::refresh)

    private fun refresh(player: Player) {
        OrbTeam.entries.forEach { player.hide(it.panel, true) }
        player.hide(Component.interface_781.component_781_1, true)
        player.hide(Component.interface_781.component_781_2, false)
        val status = when {
            countdown > 0 -> "The portal opens in ${ceil(countdown * 0.6).toInt()} seconds"
            teams.values.any { it.size < MIN_TEAM } -> "Waiting for players to join"
            else -> "Waiting for the teams to even out"
        }
        player.packets.setIFText(Component.interface_781.component_781_56, status)
    }
}
