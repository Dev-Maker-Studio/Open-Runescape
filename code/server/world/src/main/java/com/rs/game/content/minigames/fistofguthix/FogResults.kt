package com.rs.game.content.minigames.fistofguthix

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import kotlin.math.abs

private const val RATING_KEY = "fogRating"
private const val WINS_COUNTER = "Fist of Guthix wins"
private const val LOSSES_COUNTER = "Fist of Guthix losses"
private const val FORFEITS_COUNTER = "Fist of Guthix forfeits"
private const val MATCHES_COUNTER = "Fist of Guthix matches played"
private const val CHARGES_COUNTER = "Fist of Guthix charges gained"
private const val TOKENS_COUNTER = "Fist of Guthix tokens earned"
private const val HYBRID_GLOVES_COUNTER = "Fist of Guthix hybrid gloves won"
internal const val KILLS_COUNTER = "Fist of Guthix opponents killed"
internal const val STONES_COUNTER = "Fist of Guthix stones taken"
internal const val ORBS_COUNTER = "Fist of Guthix orbs used"
internal const val PORTALS_COUNTER = "Fist of Guthix portals used"
private const val HYBRID_OFFER = "fogHybridGlovesOffer"
private const val BASE_RATING_CHANGE = 10
private const val MARGIN_PER_RATING = 500
private const val MAX_RATING_CHANGE = 20
private const val WIN_BASE_TOKENS = 1
private const val CHARGES_PER_TOKEN = 500
private const val TOTAL_LEVEL_PER_TOKEN = 250
private const val MAX_TOKENS = 25
private const val CONSOLATION_THRESHOLD = 1000
private const val CONSOLATION_TOKENS = 1
private const val FORFEIT_RATING_MULTIPLIER = 2
private const val MAX_FORFEIT_MARKS = 3
private const val HYBRID_WINS = 150
private const val HYBRID_BASE_ODDS = 200
private const val HYBRID_MIN_ODDS = 25
private const val SETTLE_TICKS = 2
private val HYBRID_GLOVES = listOf(
    "Battle-mage gloves" to Obj.hlr4m_battlemage_gloves_stats,
    "Trickster gloves" to Obj.hlr4m_trickster_gloves_stats,
    "Vanguard gloves" to Obj.hlr4m_vanguard_gloves_stats,
)

class FogResult(val won: Boolean, val tokens: Int, val ratingChange: Int, val forfeited: Boolean, val charges: Int, val opponentCharges: Int)

object FogResults {
    fun rating(player: Player) = player.getL(RATING_KEY, 0L)

    fun normal(own: FogCombatant, opponent: FogCombatant, won: Boolean): FogResult {
        val margin = abs(own.score - opponent.score)
        val change = (BASE_RATING_CHANGE + margin / MARGIN_PER_RATING).coerceAtMost(MAX_RATING_CHANGE)
        val penalised = own.player.vars.getVarBit(Varbit.hvh_forfeiter) > 0
        val tokens = when {
            won -> (WIN_BASE_TOKENS + own.score / CHARGES_PER_TOKEN + own.player.skills.totalLevel / TOTAL_LEVEL_PER_TOKEN).coerceAtMost(MAX_TOKENS)
            own.score > CONSOLATION_THRESHOLD && opponent.score > CONSOLATION_THRESHOLD -> CONSOLATION_TOKENS
            else -> 0
        }.let { if (penalised) it / 2 else it }
        return FogResult(won, tokens, if (won) change else -change, false, own.score, opponent.score)
    }

    fun forfeited(quitter: FogCombatant) = FogResult(false, 0, -BASE_RATING_CHANGE * FORFEIT_RATING_MULTIPLIER, true, quitter.score, 0)

    fun apply(player: Player, result: FogResult) {
        val rating = (rating(player) + result.ratingChange).coerceAtLeast(0L)
        player.set(RATING_KEY, rating)
        val marks = player.vars.getVarBit(Varbit.hvh_forfeiter)
        player.incrementCount(MATCHES_COUNTER)
        if (result.charges > 0) player.incrementCount(CHARGES_COUNTER, result.charges)
        if (result.tokens > 0) player.incrementCount(TOKENS_COUNTER, result.tokens)
        when {
            result.forfeited -> {
                player.incrementCount(FORFEITS_COUNTER)
                player.vars.saveVarBit(Varbit.hvh_forfeiter, (marks + 1).coerceAtMost(MAX_FORFEIT_MARKS))
            }
            marks > 0 -> player.vars.saveVarBit(Varbit.hvh_forfeiter, marks - 1)
        }
        if (result.won) {
            player.incrementCount(WINS_COUNTER)
            rollHybridGloves(player)
        } else if (!result.forfeited) player.incrementCount(LOSSES_COUNTER)
        if (result.tokens > 0) player.inventory.addItemDrop(Obj.hvh_token, result.tokens)
        val summary = when {
            result.forfeited -> "You forfeited the match. You receive no tokens and your rating drops by ${-result.ratingChange}."
            result.won -> "You won with ${result.charges} charges to ${result.opponentCharges}!"
            result.charges == result.opponentCharges -> "The match is a tie at ${result.charges} charges; a tie counts as a loss for both players."
            else -> "You lost with ${result.charges} charges to ${result.opponentCharges}."
        }
        player.sendMessage("<col=ffff00>$summary")
        if (!result.forfeited) player.sendMessage("You receive ${Utils.formatNumber(result.tokens)} Fist of Guthix token${if (result.tokens == 1) "" else "s"}. Your rating is now $rating.")
        player.fogLobbyController?.refresh()
        WorldTasks.schedule(SETTLE_TICKS) { if (player.getBool(HYBRID_OFFER)) offerHybridGloves(player) }
    }

    private fun rollHybridGloves(player: Player) {
        val wins = player.getCounterValueL(WINS_COUNTER)
        if (wins < HYBRID_WINS) return
        val odds = (HYBRID_BASE_ODDS - (wins - HYBRID_WINS)).coerceAtLeast(HYBRID_MIN_ODDS.toLong()).toInt()
        if (Utils.random(odds) != 0) return
        player.set(HYBRID_OFFER, true)
        player.incrementCount(HYBRID_GLOVES_COUNTER)
    }

    fun offerHybridGloves(player: Player) {
        if (!player.getBool(HYBRID_OFFER)) return
        player.sendOptionsDialogue("Guthix's druids reward your prowess with a pair of gloves.") {
            for ((name, obj) in HYBRID_GLOVES)
                opExec(name) {
                    if (!player.getBool(HYBRID_OFFER)) return@opExec
                    player.delete(HYBRID_OFFER)
                    player.inventory.addItemDrop(obj, 1)
                }
        }
    }
}
