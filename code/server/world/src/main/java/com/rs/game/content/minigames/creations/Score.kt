package com.rs.game.content.minigames.creations

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varcstr
import org.darkan.core.model.Item

private const val POINTS_PER_CLAY_CLASS = 15
private const val DAMAGE_POINTS = 5
private const val DAMAGE_PER_POINTS = 10
private const val WIN_BONUS = 1.1
private val REWARD_THRESHOLDS = intArrayOf(20_000, 22_000, 26_000, 34_000, 50_000, 82_000, 146_000)
private const val REWARD_POINTS_PER_SCORE = 1000
private const val FIRST_TIERED_REWARD = 20

fun scoreUnits(item: Item) = if (StealingCreation.crateFor(item.id) == null) 0 else item.definitions.cost * item.amount

class Score(val name: String, val team: Boolean) {
    var gathering = 0
        private set
    var processing = 0
        private set
    var depositing = 0
        private set
    var withdrawing = 0
        private set
    var killed = 0
        private set
    var died = 0
        private set
    private var damageDealt = 0

    val teamId get() = teamNumber(team)
    val damaging get() = damageDealt * DAMAGE_POINTS / DAMAGE_PER_POINTS
    val deposited get() = (depositing - withdrawing) * 2

    fun gather(clayClass: Int) {
        gathering += POINTS_PER_CLAY_CLASS * clayClass
    }

    fun process(clayClass: Int, clayUsed: Int) {
        processing += POINTS_PER_CLAY_CLASS * clayClass * clayUsed
    }

    fun deposit(item: Item) {
        depositing += scoreUnits(item)
    }

    fun canWithdraw() = total(0) > 0

    fun withdraw(units: Int) {
        withdrawing += units
    }

    fun dealDamage(lifepoints: Int) {
        damageDealt += lifepoints
    }

    fun kill() {
        killed++
    }

    fun die() {
        died++
    }

    fun total(winnerTeam: Int): Int {
        val total = gathering + processing + deposited + damaging
        return if (teamId == winnerTeam) (total * WIN_BONUS).toInt() else total
    }

    fun rewardPoints(winnerTeam: Int): Int {
        val total = total(winnerTeam).coerceAtLeast(0)
        val tier = REWARD_THRESHOLDS.indexOfLast { total >= it }
        return if (tier == -1) total / REWARD_POINTS_PER_SCORE else FIRST_TIERED_REWARD + tier
    }
}

private class Award(val name: Int, val team: Int, val value: Int, val best: Boolean = true, val stat: (Score, Int) -> Int)

private val AWARDS = listOf(
    Award(Varcstr.varcstr_45, Varc.varc_608, Varc.varc_600) { s, _ -> s.gathering },
    Award(Varcstr.varcstr_44, Varc.varc_607, Varc.varc_599) { s, _ -> s.processing },
    Award(Varcstr.varcstr_49, Varc.varc_612, Varc.varc_604) { s, _ -> s.deposited },
    Award(Varcstr.varcstr_48, Varc.varc_611, Varc.varc_603) { s, _ -> s.damaging },
    Award(Varcstr.varcstr_46, Varc.varc_609, Varc.varc_601) { s, _ -> s.killed },
    Award(Varcstr.varcstr_47, Varc.varc_610, Varc.varc_602) { s, _ -> s.died },
    Award(Varcstr.varcstr_50, Varc.varc_613, Varc.varc_605) { s, winner -> s.total(winner) },
    Award(Varcstr.varcstr_51, Varc.varc_614, Varc.varc_606, best = false) { s, winner -> s.total(winner) },
)

fun winningTeam(scores: Collection<Score>): Int {
    val blue = scores.filter { !it.team }.sumOf { it.total(0) }
    val red = scores.filter { it.team }.sumOf { it.total(0) }
    return if (blue > red) teamNumber(false) else if (red > blue) teamNumber(true) else 0
}

fun Player.showScoreboard(personal: Score, all: Collection<Score>, winner: Int) {
    interfaceManager.sendInterface(Interface.interface_810)
    for (award in AWARDS) {
        val holder = if (award.best) all.maxByOrNull { award.stat(it, winner) } else all.minByOrNull { award.stat(it, winner) }
        packets.sendVarcString(award.name, holder?.name ?: "")
        packets.sendVarc(award.team, holder?.teamId ?: 0)
        packets.sendVarc(award.value, holder?.let { award.stat(it, winner) } ?: 0)
    }
    val blueTotal = all.filter { !it.team }.sumOf { it.total(winner) }
    val redTotal = all.filter { it.team }.sumOf { it.total(winner) }
    packets.sendVarc(Varc.varc_595, personal.died)
    packets.sendVarc(Varc.varc_590, personal.gathering)
    packets.sendVarc(Varc.varc_591, personal.processing)
    packets.sendVarc(Varc.varc_592, personal.deposited)
    packets.sendVarc(Varc.varc_593, personal.damaging)
    packets.sendVarc(Varc.varc_594, personal.killed)
    packets.sendVarc(Varc.varc_596, personal.total(winner))
    packets.sendVarc(Varc.varc_597, blueTotal)
    packets.sendVarc(Varc.varc_598, redTotal)
    packets.sendVarc(Varc.varc_588, personal.teamId)
    packets.sendVarc(Varc.varc_589, winner)

    val team = personal.teamId
    val enemy = teamNumber(!personal.team)
    sendMessage("Team 1's score(blue team): $blueTotal - Team 2's score(red team): $redTotal")
    sendMessage(when (winner) {
        team -> "Your team, Team $team, is victorious! You gain a 10% bonus to your score!"
        0 -> "Your team, Team $team, ties with Team $enemy!"
        else -> "Your team, Team $team, is defeated! Team $enemy is victorious!"
    })
    sendMessage("YOUR TOTAL SCORE: ${personal.total(winner)} points.")
}

fun Player.awardStealingCreationPoints(personal: Score, winner: Int) {
    val points = personal.rewardPoints(winner)
    scPoints = scPoints.coerceAtLeast(0) + points
    vars.setVarBit(Varbit.sc_points, scPoints)
    incrementCount("Stealing Creation points earned", points)
    incrementCount("Stealing creation games played")
    when (winner) {
        personal.teamId -> incrementCount("Stealing Creation games won")
        0 -> Unit
        else -> incrementCount("Stealing Creation games lost")
    }
    sendMessage("You earn: $points reward point${if (points != 1) "s" else ""}.")
}
