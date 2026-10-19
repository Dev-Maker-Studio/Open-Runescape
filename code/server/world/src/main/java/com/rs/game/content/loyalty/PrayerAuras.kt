package com.rs.game.content.loyalty

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.AuraManager.Aura
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onXpDrop
import org.darkan.core.Constants

private class PrayerAuraTier(val xpBonus: Double, val restorePercent: Double)

private const val RESTORE_XP_INTERVAL = 500.0
private const val RESTORE_XP_ATTRIB = "prayerAuraXp"

private val PRAYER_AURAS = mapOf(
    Aura.SALVATION to PrayerAuraTier(0.01, 0.03),
    Aura.CORRUPTION to PrayerAuraTier(0.01, 0.03),
    Aura.GREATER_SALVATION to PrayerAuraTier(0.015, 0.05),
    Aura.GREATER_CORRUPTION to PrayerAuraTier(0.015, 0.05),
    Aura.MASTER_SALVATION to PrayerAuraTier(0.02, 0.07),
    Aura.MASTER_CORRUPTION to PrayerAuraTier(0.02, 0.07),
    Aura.SUPREME_SALVATION to PrayerAuraTier(0.025, 0.10),
    Aura.SUPREME_CORRUPTION to PrayerAuraTier(0.025, 0.10),
)

@ServerStartupEvent
fun mapPrayerAuras() {
    onXpDrop { e ->
        if (e.skillId != Constants.PRAYER || e.player.isXpLocked) return@onXpDrop
        val tier = e.player.activePrayerAura() ?: return@onXpDrop
        e.multiplier += tier.xpBonus
        e.player.restorePrayerPerInterval(e.xp, tier.restorePercent)
    }
}

private fun Player.activePrayerAura(): PrayerAuraTier? = PRAYER_AURAS.entries.firstOrNull { auraManager.isActivated(it.key) }?.value

private fun Player.restorePrayerPerInterval(baseXp: Double, restorePercent: Double) {
    var accumulated = tempAttribs.getD(RESTORE_XP_ATTRIB) + baseXp
    while (accumulated >= RESTORE_XP_INTERVAL) {
        accumulated -= RESTORE_XP_INTERVAL
        prayer.restorePrayer(skills.getLevelForXp(Constants.PRAYER) * 10 * restorePercent)
    }
    tempAttribs.setD(RESTORE_XP_ATTRIB, accumulated)
}
