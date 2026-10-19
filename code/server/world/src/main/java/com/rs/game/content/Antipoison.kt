package com.rs.game.content

import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks
import org.darkan.core.gameval.gen.Obj

private const val IMMUNITY = "You are immune to poison."

enum class AntipoisonTier(val buffBar: BuffBarEntry, val ticks: Long) {
    REGULAR(objIcon(Obj._4doseantipoison, IMMUNITY), Ticks.fromSeconds(90).toLong()),
    SUPER(objIcon(Obj._4dose2antipoison, IMMUNITY), Ticks.fromMinutes(6).toLong()),
    ANTIDOTE_PLUS(objIcon(Obj.antidoteplus4, IMMUNITY), Ticks.fromMinutes(9).toLong()),
    ANTIDOTE_PLUS_PLUS(objIcon(Obj.antidoteplusplus4, IMMUNITY), Ticks.fromMinutes(12).toLong());

    companion object {
        fun forName(name: String?) = entries.firstOrNull { it.name == name }
    }
}

fun Player.applyAntipoison(tier: AntipoisonTier, ticks: Long = tier.ticks) {
    addEffect(Effect.ANTIPOISON, ticks)
    set(TIER_ATTRIBUTE, tier.name)
    set(TIER_GRANTED_TICKS_ATTRIBUTE, ticks)
}

// The record names the tier that granted the immunity now running, and it is dropped the moment
// that immunity ends. Antipoison from anywhere else - the thermal pool, a familiar's cure - runs
// longer than any tier grants or can only reach an unprotected player, so it never inherits a
// record; the bar then shows the plain vial rather than a tier it cannot prove.
fun activeAntipoisonEntry(player: Player): BuffBarEntry? {
    val ticks = player.getEffectTicks(Effect.ANTIPOISON)
    val tier = AntipoisonTier.forName(player.getO<String>(TIER_ATTRIBUTE))
    if (ticks <= 0L) {
        if (tier != null) {
            player.delete(TIER_ATTRIBUTE)
            player.delete(TIER_GRANTED_TICKS_ATTRIBUTE)
        }
        return null
    }
    if (tier == null || ticks > player.getL(TIER_GRANTED_TICKS_ATTRIBUTE, 0L)) return AntipoisonTier.REGULAR.buffBar
    return tier.buffBar
}

private const val TIER_ATTRIBUTE = "antipoisonTier"
private const val TIER_GRANTED_TICKS_ATTRIBUTE = "antipoisonTierGrantedTicks"
