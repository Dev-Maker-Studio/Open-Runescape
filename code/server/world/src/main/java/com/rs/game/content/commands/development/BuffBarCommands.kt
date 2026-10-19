package com.rs.game.content.commands.development

import com.rs.engine.command.Commands
import com.rs.game.content.AntipoisonTier
import com.rs.game.content.Effect
import com.rs.game.content.applyAntipoison
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks
import org.darkan.core.model.Rights
import com.rs.plugin.annotations.ServerStartupEvent

@ServerStartupEvent
fun loadBuffBarCommands() {
    Commands.add(Rights.ADMIN, "buff [name|all|clear|list] (seconds)", "Puts a buff bar effect on yourself without going through the content that grants it.") { p, args ->
        val seconds = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_SECONDS
        when (val arg = args.getOrNull(0)?.lowercase()) {
            null, "list" -> sendList(p)
            "all" -> {
                for (effect in barEffects()) apply(p, effect, seconds)
                p.poison.makePoisoned(POISON_DAMAGE)
                p.sendMessage("Applied ${barEffects().size} effects plus poison for ${seconds}s.")
            }
            "clear" -> {
                // clearEffects keeps anything flagged isRemoveOnDeath = false, which is the death
                // semantic rather than the reset a test wants.
                Effect.entries.filter { p.hasEffect(it) }.forEach { p.removeEffect(it) }
                p.poison.reset()
                p.sendMessage("Cleared every effect.")
            }
            "poison" -> {
                p.poison.makePoisoned(POISON_DAMAGE)
                p.sendMessage("Poisoned.")
            }
            else -> {
                val tier = AntipoisonTier.entries.firstOrNull { it.name.equals(arg, true) }
                val effect = Effect.entries.firstOrNull { it.name.equals(arg, true) && it.isOnBuffBar }
                when {
                    tier != null -> {
                        p.applyAntipoison(tier, Ticks.fromSeconds(seconds).toLong())
                        p.sendMessage("Applied ${tier.name} for ${seconds}s.")
                    }
                    effect != null -> {
                        apply(p, effect, seconds)
                        p.sendMessage("Applied ${effect.name} for ${seconds}s.")
                    }
                    else -> p.sendMessage("No bar effect called '$arg'. Use ::buff list.")
                }
            }
        }
    }

    Commands.add(Rights.ADMIN, "kill", "Kills you, to check that effects and their icons clear on death.") { p, _ ->
        p.applyHit(Hit(p, p.hitpoints, Hit.HitLook.TRUE_DAMAGE))
    }
}

private const val DEFAULT_SECONDS = 120
private const val POISON_DAMAGE = 60

private fun barEffects() = Effect.entries.filter { it.isOnBuffBar && it != Effect.ANTIPOISON }

private fun apply(player: Player, effect: Effect, seconds: Int) {
    if (effect == Effect.ANTIPOISON) player.applyAntipoison(AntipoisonTier.REGULAR, Ticks.fromSeconds(seconds).toLong())
    else player.addEffect(effect, Ticks.fromSeconds(seconds).toLong())
}

private fun sendList(player: Player) {
    player.sendMessage("::buff [name|all|clear|poison] (seconds)")
    barEffects().chunked(8).forEach { chunk -> player.sendMessage(chunk.joinToString(", ") { it.name }) }
    player.sendMessage("Antipoison tiers: ${AntipoisonTier.entries.joinToString(", ") { it.name }}")
}
