package com.rs.game.content.commands.bot

import com.rs.engine.command.Commands
import com.rs.engine.quest.Quest
import com.rs.game.content.minigames.barbarianassault.HIGHEST_WAVE
import com.rs.game.content.minigames.barbarianassault.Wave
import com.rs.game.content.randomizeLook
import com.rs.game.content.combat.CombatDefinitions
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.Constants
import org.darkan.core.model.Rights
import world.gregs.voidps.type.Tile

private val botCommands: Map<String, (Player, List<String>) -> Unit> = mapOf(
    "tele" to { p, args ->
        p.resetWalkSteps()
        p.tele(Tile.of(args[0].toInt(), args[1].toInt(), args.getOrNull(2)?.toInt() ?: 0))
    },
    "item" to { p, args -> p.inventory.addItem(args[0].toInt(), args.getOrNull(1)?.toInt() ?: 1) },
    "equip" to { p, args ->
        val item = args[0].toInt()
        if (p.inventory.addItem(item, 1)) Equipment.sendWear(p, p.inventory.items.getThisItemSlot(item), item, true)
    },
    "level" to { p, args -> setLevel(p, args[0].toInt(), args[1].toInt()) },
    "quest" to { p, args -> completeWithRequirements(p, Quest.valueOf(args[0].uppercase()), mutableSetOf()) },
    "spellbook" to { p, args -> p.combatDefinitions.setSpellbook(CombatDefinitions.Spellbook.valueOf(args[0].uppercase())) },
    "prayers" to { p, args -> p.prayer.setPrayerBook(args[0].equals("curses", ignoreCase = true)) },
    "appearance" to { p, _ -> randomizeLook(p) },
    "bawave" to { p, args -> p.set(HIGHEST_WAVE, args[0].toInt().coerceIn(0, Wave.entries.size)) },
    "reset" to { p, _ ->
        p.inventory.reset()
        p.equipment.reset()
        p.appearance.generateAppearanceData()
    },
)

private fun setLevel(p: Player, skill: Int, level: Int) {
    if (skill !in Constants.SKILL_NAME.indices || level !in 1..(if (skill == Constants.DUNGEONEERING) 120 else 99)) return
    p.skills.set(skill, level)
    p.skills.setXp(skill, Skills.getXPForLevel(level).toDouble())
    p.appearance.generateAppearanceData()
}

private fun completeWithRequirements(p: Player, quest: Quest, done: MutableSet<Quest>) {
    if (!done.add(quest)) return
    val info = quest.defs.extraInfo
    info.preReqs.forEach { completeWithRequirements(p, it, done) }
    info.preReqSkillReqs.forEach { (skill, level) -> if (p.skills.getLevelForXp(skill) < level) setLevel(p, skill, level) }
    p.questManager.completeQuest(quest)
}

@ServerStartupEvent
fun mapBotCommands() {
    Commands.add(Rights.PLAYER, "bot [${botCommands.keys.joinToString("|")}] (args)", "Account setup commands for verified bots.") { p, args ->
        if (!p.isBot) return@add
        val command = botCommands[args.firstOrNull()] ?: return@add p.sendMessage("Bot commands: ${botCommands.keys.joinToString()}")
        if (p.controllerManager.controller != null || p.isLocked) return@add p.sendMessage("Bot commands are unavailable during an activity.")
        command(p, args.drop(1))
    }
}
