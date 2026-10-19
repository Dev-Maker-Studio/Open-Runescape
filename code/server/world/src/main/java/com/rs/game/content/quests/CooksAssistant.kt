package com.rs.game.content.quests

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.Utils.strikeThroughIf
import com.rs.plugin.annotations.PluginEventHandler

@QuestHandler(
		quest = Quest.COOKS_ASSISTANT,
		startText = "Speak to the cook in the kitchen of Lumbridge Castle.",
		itemsText = "Empty pot (can be found in flour mill). Empty bucket (can be found near dairy cow).",
		combatText = "None.",
		rewardsText = "300 Cooking XP<br>500 coins<br>20 sardines<br>Access to the cook's range",
		completedStage = 2
)
@PluginEventHandler
class CooksAssistant : QuestOutline() {
	override fun getJournalLines(player: Player, stage: Int) = when (stage) {
		0 -> listOf("I can start this quest by speaking to the cook in the Lumbridge castle kitchen.")
		1 -> listOf("The cook is having problems baking a cake for the Duke's birthday party. He needs the following 3 items:",
			strikeThroughIf("An egg") { player.inventory.containsItem(1944, 1) },
			strikeThroughIf("A bucket of milk") { player.inventory.containsItem(1927, 1) },
			strikeThroughIf("A pot of flour") { player.inventory.containsItem(1933, 1) }
		)
		2 -> listOf("QUEST COMPLETE!")
		else -> listOf("Invalid quest stage. Report this to an administrator.")
	}

	override fun complete(player: Player) {
		player.skills.addXpQuest(Constants.COOKING, 300.0)
		player.inventory.addCoins(500)
		player.inventory.addItemDrop(326, 20)
		sendQuestCompleteInterface(player, 1891)
	}
}