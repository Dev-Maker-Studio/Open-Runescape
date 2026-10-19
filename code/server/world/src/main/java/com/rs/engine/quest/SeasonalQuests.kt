package com.rs.engine.quest

import com.rs.Settings
import com.rs.engine.command.Commands
import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.Logger
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Rights

object SeasonalQuests {
    var sectionEnabled = true
        private set

    private val enabled = linkedSetOf<Quest>()

    val all: List<Quest> get() = Quest.entries.filter { it.isSeasonal }

    fun enable(quest: Quest) {
        if (!quest.isSeasonal) {
            Logger.warn(SeasonalQuests::class.java, "enable", "$quest has no seasonal quest bit in its quest list struct")
            return
        }
        if (enabled.add(quest))
            refreshAll()
    }

    fun disable(quest: Quest) {
        if (enabled.remove(quest))
            refreshAll()
    }

    fun setSectionEnabled(on: Boolean) {
        if (sectionEnabled == on)
            return
        sectionEnabled = on
        refreshAll()
    }

    fun isEnabled(quest: Quest) = quest in enabled

    fun isShown(quest: Quest) = sectionEnabled && quest in enabled

    fun sendVars(player: Player) {
        player.vars.setVarBit(Varbit.ql4_seasonal, if (sectionEnabled) 1 else 0)
        player.vars.setVarBit(Varbit.ql4_seasonal_quests, enabled.fold(0) { mask, quest -> mask or (1 shl quest.seasonalBit) })
    }

    private fun refreshAll() {
        for (player in World.players)
            if (player.hasStarted())
                player.questManager.updateOptions()
    }
}

@ServerStartupEvent
fun mapSeasonalQuestCommands() {
    val rights = if (Settings.getConfig().isDebug) Rights.PLAYER else Rights.ADMIN
    Commands.add(rights, "seasonal [on|off|enable (quest)|disable (quest)]", "Shows or hides the quest list's Seasonal event section and its quests.") { p, args ->
        val quest = args.getOrNull(1)?.let { name -> SeasonalQuests.all.firstOrNull { it.name.equals(name, ignoreCase = true) } }
        when (args.getOrNull(0)) {
            null -> {}
            "on" -> SeasonalQuests.setSectionEnabled(true)
            "off" -> SeasonalQuests.setSectionEnabled(false)
            "enable" -> quest?.let(SeasonalQuests::enable)
            "disable" -> quest?.let(SeasonalQuests::disable)
        }
        val quests = SeasonalQuests.all.joinToString { "${it.name}=${if (SeasonalQuests.isEnabled(it)) "on" else "off"}" }
        p.sendMessage("Seasonal event section: ${if (SeasonalQuests.sectionEnabled) "on" else "off"}; quests: $quests")
    }
}
