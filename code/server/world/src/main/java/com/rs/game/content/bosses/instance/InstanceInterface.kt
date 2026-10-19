package com.rs.game.content.bosses.instance

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.cache.BASDefinitions
import org.darkan.core.cache.NPCDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varcstr
import org.darkan.core.model.IFEvents

private class SettingRow(
    val label: String,
    val value: (InstanceSettings, BossInstanceDef) -> String,
    val step: (InstanceSettings, BossInstanceDef, Int) -> InstanceSettings,
)

private val ROWS = listOf(
    SettingRow("Max players:", { s, _ -> s.maxPlayers.toString() }) { s, def, d ->
        s.copy(maxPlayers = (s.maxPlayers + d).coerceIn(1, def.maxPlayers))
    },
    SettingRow("Min combat:", { s, _ -> s.minCombat.toString() }) { s, _, d ->
        s.copy(minCombat = (s.minCombat + d * 5).coerceIn(3, 138))
    },
    SettingRow("Spawn speed:", { s, _ -> s.spawnSpeed.label }) { s, def, d ->
        val speeds = def.spawnSpeeds
        s.copy(spawnSpeed = speeds[(speeds.indexOf(s.spawnSpeed) + d).coerceIn(0, speeds.lastIndex)])
    },
    SettingRow("Protection:", { s, _ -> s.protection.label }) { s, _, d ->
        val all = InstanceProtection.entries
        s.copy(protection = all[(all.indexOf(s.protection) + d).coerceIn(0, all.lastIndex)])
    },
)

private const val PENDING = "instancePending"
private const val PENDING_BOSS = "instancePendingBoss"
private const val REVISION = "instanceRevision"

/** Ops are addressed by cc slot, and every cc a row or button draws needs to be inside the range. */
private const val SLOT_CEILING = 128
private const val CCS_PER_ROW = 8
private const val MINUS_SLOT = 2
private const val PLUS_SLOT = 7
private const val CCS_PER_BUTTON = 4
private const val BASE_PORTRAIT_ZOOM = 1200

object InstanceInterface {
    fun open(player: Player, def: BossInstanceDef) {
        val settings = InstanceSettings(
            maxPlayers = 1,
            minCombat = def.minCombatDefault,
            spawnSpeed = def.spawnSpeeds.first(),
            protection = InstanceProtection.FFA,
        )
        player.tempAttribs.setO<InstanceSettings>(PENDING, settings)
        player.tempAttribs.setO<BossInstanceDef>(PENDING_BOSS, def)
        player.interfaceManager.sendInterface(Interface.instance_system)
        player.packets.setIFText(Component.instance_system.boss_name, def.displayName)
        player.packets.setIFText(Component.instance_system.requirements_value, requirementsText(def))
        val portrait = Utils.componentIdFromHash(Component.instance_system.portrait)
        val npc = NPCDefinitions.getDefs(def.portraitNpc)
        player.packets.sendVarc(Varc.instance_boss_npc, def.portraitNpc)
        player.packets.setIFAngle(Interface.instance_system, portrait, 0, 0, portraitZoom(def, npc.size))
        val idle = BASDefinitions.getDefs(npc.renderEmote).standAnimation
        if (idle != -1) player.packets.setIFAnimation(idle, Interface.instance_system, portrait)
        player.packets.setIFEvents(IFEvents(Component.instance_system.rows, 0, SLOT_CEILING).enableRightClickOptions(0))
        player.packets.setIFEvents(IFEvents(Component.instance_system.buttons, 0, SLOT_CEILING).enableRightClickOptions(0))
        refresh(player)
    }

    private fun portraitZoom(def: BossInstanceDef, npcSize: Int): Int =
        if (def.portraitZoom > 0) def.portraitZoom else BASE_PORTRAIT_ZOOM * npcSize.coerceAtLeast(1)

    fun applyPortrait(player: Player, zoom: Int, pitch: Int, roll: Int) {
        val portrait = Utils.componentIdFromHash(Component.instance_system.portrait)
        player.packets.setIFAngle(Interface.instance_system, portrait, pitch, roll, zoom)
    }

    private fun requirementsText(def: BossInstanceDef): String {
        if (def.cost <= 0) return def.entranceRequirements
        val coins = "${Utils.formatNumber(def.cost)} coins"
        return if (def.entranceRequirements == "None") coins else "${def.entranceRequirements}, $coins"
    }

    private fun refresh(player: Player) {
        val settings = pending(player) ?: return
        val def = pendingBoss(player) ?: return
        player.packets.sendVarc(Varc.instance_row_count, ROWS.size)
        ROWS.forEachIndexed { row, spec ->
            player.packets.sendVarcString(Varcstr.instance_row_label_0 + row, spec.label)
            player.packets.sendVarcString(Varcstr.instance_row_value_0 + row, spec.value(settings, def))
        }
        val revision = player.tempAttribs.getI(REVISION) + 1
        player.tempAttribs.setI(REVISION, revision)
        player.packets.sendVarc(Varc.instance_revision, revision)
    }

    private fun pending(player: Player): InstanceSettings? = player.tempAttribs.getO<InstanceSettings>(PENDING)

    private fun pendingBoss(player: Player): BossInstanceDef? = player.tempAttribs.getO<BossInstanceDef>(PENDING_BOSS)

    fun promptJoin(player: Player, def: BossInstanceDef) {
        player.sendInputName("Whose ${def.displayName} instance would you like to join?") { owner ->
            val username = Utils.formatPlayerNameForProtocol(owner)
            val target = BossInstances.owned(username)
            when {
                target == null || target.def.key != def.key -> player.sendMessage("They do not have a ${def.displayName} instance.")
                target.settings.protection == InstanceProtection.PIN -> player.sendInputInteger("Enter the instance PIN:") { pin ->
                    BossInstances.join(player, username, pin)?.let { player.sendMessage(it) }
                }
                else -> BossInstances.join(player, username)?.let { player.sendMessage(it) }
            }
        }
    }

    fun step(player: Player, slotId: Int) {
        val settings = pending(player) ?: return
        val def = pendingBoss(player) ?: return
        val row = slotId / CCS_PER_ROW
        val delta = when (slotId % CCS_PER_ROW) {
            MINUS_SLOT -> -1
            PLUS_SLOT -> 1
            else -> return
        }
        val spec = ROWS.getOrNull(row) ?: return
        player.tempAttribs.setO<InstanceSettings>(PENDING, spec.step(settings, def, delta))
        refresh(player)
    }

}

@ServerStartupEvent
fun mapInstanceInterface() {
    onButtonClick(Interface.instance_system) { e ->
        when (e.component) {
            Component.instance_system.rows -> InstanceInterface.step(e.player, e.slotId)
            Component.instance_system.close_button -> e.player.closeInterfaces()
            Component.instance_system.buttons -> handleAction(e.player, e.slotId / CCS_PER_BUTTON)
        }
    }
}

private fun handleAction(player: Player, button: Int) {
    val settings = player.tempAttribs.getO<InstanceSettings>(PENDING) ?: return
    val def = player.tempAttribs.getO<BossInstanceDef>(PENDING_BOSS) ?: return
    player.closeInterfaces()
    when (button) {
        0 -> startOrRejoin(player, def, settings)
        1 -> InstanceInterface.promptJoin(player, def)
        2 -> BossInstances.rejoin(player)?.let { player.sendMessage(it) }
    }
}

private fun startOrRejoin(player: Player, def: BossInstanceDef, settings: InstanceSettings) {
    val existing = BossInstances.owned(player.username)
    if (existing == null) {
        BossInstances.start(player, def, settings)
        return
    }
    val standing = if (existing.expired) "Your ${existing.def.displayName} instance has expired."
    else "You already have a ${existing.def.displayName} instance with ${existing.minutesLeft} minutes left."
    player.sendOptionDialogue(standing) { options ->
        options.add(if (existing.expired) "Rejoin it and pay to renew it." else "Rejoin it.") {
            BossInstances.rejoin(player)?.let { player.sendMessage(it) }
        }
        options.add("Start a new one and abandon it.") { BossInstances.start(player, def, settings) }
        options.add("Cancel.")
    }
}
