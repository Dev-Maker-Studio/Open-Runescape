package com.rs.game.content.minigames.barbarianassault.penance

import com.rs.game.content.minigames.barbarianassault.BarbarianAssaultGame
import com.rs.game.content.minigames.barbarianassault.HEALERS_POISONED_COUNTER
import com.rs.game.content.minigames.barbarianassault.PenanceFamily
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

private const val POISON_GATE = 5
private const val PULSES_PER_STRENGTH = 5
private const val FULL_STRENGTH = 4
private const val DAMAGE_PER_STRENGTH = 10
private const val FOOD_DAMAGE = 40
private const val PLAYER_RANGE = 15
private const val RUNNER_RANGE = 5
private const val SPRAY_COOLDOWN = 8
private const val PLAYER_POISON = 28
private const val RUNNER_HEAL = 10
private const val WANDER_ODDS = 8
private const val WANDER_RADIUS = 6
private const val REGEN_INTERVAL = 100
private const val REGEN_AMOUNT = 10

class PenanceHealer(id: Int, tile: Tile, game: BarbarianAssaultGame, ordinal: Int) : Penance(id, tile, game, PenanceFamily.HEALER, ordinal) {
    private var poisoned = false
    private var strength = 0
    private var pulses = 0
    private var gateTick = game.tick
    private var lastPulse = game.tick
    private var sprayCounter = 0
    private var target: Entity? = null
    private var wanderTo: Tile? = null
    var lastFeeder: Player? = null
        private set

    override fun tickPenance() {
        tickPoison()
        if (dying || isDead) return
        if (game.tick % REGEN_INTERVAL == 0 && hitpoints < maxHitpoints) heal(REGEN_AMOUNT)
        sprayCounter++
        val current = target?.takeIf { valid(it) } ?: pickTarget().also { target = it }
        if (current == null) {
            wander()
            return
        }
        if (trySpray(current)) return
        if (!withinDistance(current.tile, 1) || current.tile == tile) stepTowards(current.tile)
        trySpray(current)
    }

    fun feed(player: Player, correct: Boolean) {
        lastFeeder = player
        if (!correct) return
        player.incrementCount(HEALERS_POISONED_COUNTER)
        applyHit(Hit(player, FOOD_DAMAGE.coerceAtMost(hitpoints), HitLook.POISON_DAMAGE))
        if (!poisoned || game.tick - gateTick >= POISON_GATE) gateTick = game.tick
        if (!poisoned) lastPulse = game.tick
        poisoned = true
        strength = FULL_STRENGTH
        pulses = 0
    }

    fun poisonStrength() = if (poisoned) strength else 0

    private fun tickPoison() {
        if (!poisoned || game.tick - gateTick < POISON_GATE || game.tick - lastPulse < POISON_GATE) return
        lastPulse = game.tick
        applyHit(Hit(lastFeeder ?: this, (strength * DAMAGE_PER_STRENGTH).coerceAtMost(hitpoints), HitLook.POISON_DAMAGE))
        if (++pulses >= PULSES_PER_STRENGTH) {
            pulses = 0
            if (--strength <= 0) poisoned = false
        }
    }

    private fun valid(entity: Entity) = !entity.isDead && !entity.hasFinished() && when (entity) {
        is PenanceRunner -> !entity.dying && withinDistance(entity.tile, RUNNER_RANGE)
        is Player -> game.isPlaying(entity) && withinDistance(entity.tile, PLAYER_RANGE)
        else -> false
    }

    private fun pickTarget(): Entity? {
        val runners = game.runners().filter { !it.dying && it.hitpoints < it.maxHitpoints && withinDistance(it.tile, RUNNER_RANGE) && lineOfSightTo(it, false) }
        if (runners.isNotEmpty() && sprayCounter > 2) return runners.random()
        return game.players.filter { game.isPlaying(it) && withinDistance(it.tile, PLAYER_RANGE) && lineOfSightTo(it, false) }.randomOrNull()
    }

    private fun trySpray(target: Entity): Boolean {
        if (sprayCounter < SPRAY_COOLDOWN || !withinDistance(target.tile, 1) || target.tile == tile) return false
        sprayCounter = 0
        faceEntity(target)
        spotAnim(Spotanim.barbassault_penance_healer_cloud)
        when (target) {
            is Player -> target.poison.makePoisoned(PLAYER_POISON)
            is PenanceRunner -> target.heal(RUNNER_HEAL)
        }
        this.target = null
        return true
    }

    private fun wander() {
        val destination = wanderTo
        if (destination != null && destination != tile && stepTowards(destination)) return
        wanderTo = if (Utils.random(WANDER_ODDS) == 0) respawnTile.transform(Utils.random(-WANDER_RADIUS, WANDER_RADIUS), Utils.random(-WANDER_RADIUS, WANDER_RADIUS)) else null
    }
}
