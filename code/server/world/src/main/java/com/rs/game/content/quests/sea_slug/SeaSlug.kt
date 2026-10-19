package com.rs.game.content.quests.sea_slug

import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin

internal const val STAGE_UNSTARTED = 0
internal const val STAGE_FIND_HOLGART = 1
internal const val STAGE_BOAT_REPAIRED = 2
internal const val STAGE_FOUND_KENNITH = 3
internal const val STAGE_FOUND_KENT = 4
internal const val STAGE_RESCUE_KENNITH = 5
internal const val STAGE_LEARNED_ABOUT_HEAT = 6
internal const val STAGE_NEEDS_LIT_TORCH = 7
internal const val STAGE_OPENED_WALL = 8
internal const val STAGE_NEEDS_CRANE = 9
internal const val STAGE_KENNITH_RESCUED = 10
internal const val STAGE_REPORTED_TO_CAROLINE = 11
internal const val STAGE_COMPLETE = 12

@QuestHandler(
    quest = Quest.SEA_SLUG,
    startText = "Speak to Caroline north of Witchaven, east of Ardougne.",
    itemsText = "Swamp paste",
    combatText = "None, but the sea slugs and fishermen can hurt you.",
    rewardsText =
        "7,175 Fishing XP<br>" +
        "Oyster pearls<br>" +
        "Access to the Fishing Platform",
    completedStage = STAGE_COMPLETE
)
class SeaSlug : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        STAGE_UNSTARTED -> listOf("I can start this quest by speaking to Caroline north of Witchaven.")
        STAGE_FIND_HOLGART -> listOf("I have spoken to Caroline and agreed to help.", "I should take some swamp paste to Holgart so he can repair his boat.")
        STAGE_BOAT_REPAIRED -> listOf("I have given Holgart the swamp paste and his boat is now ready to take me to the Fishing Platform.", "I should ask Holgart for a ride and look for Kennith upstairs on the platform.")
        STAGE_FOUND_KENNITH -> listOf("I've found Kennith, he's hiding behind some boxes.", "I should speak to Holgart about finding Kennith's father.")
        STAGE_FOUND_KENT -> listOf("I've found Kent on a small island.", "I should finish speaking to him to find out what happened.")
        STAGE_RESCUE_KENNITH -> listOf("Kent has asked me to help Kennith escape.", "I should return to the platform with Holgart and ask Bailey how to get past the fishermen.")
        STAGE_LEARNED_ABOUT_HEAT -> listOf("After speaking to Bailey, I found out that sea slugs are afraid of heat.", "Bailey can give me a torch to keep the fishermen away.")
        STAGE_NEEDS_LIT_TORCH -> if (player.inventory.containsItem(Obj.torch_lit))
            listOf("I have lit the torch. I can now climb the ladder and speak to Kennith about escaping.")
        else listOf("I should scrape the damp sticks with broken glass, then rub the dry sticks together to light my torch.")
        STAGE_OPENED_WALL -> listOf("I've created an opening to let Kennith escape.", "I should speak to Kennith again about coming through the opening.")
        STAGE_NEEDS_CRANE -> listOf("Kennith can't get downstairs without some help.", "I should rotate the crane outside his room to lower him to Holgart's boat.")
        STAGE_KENNITH_RESCUED -> listOf("I've used the crane to lower Kennith into the boat.", "I should return to Witchaven with Holgart and tell Caroline that her family is safe.")
        STAGE_REPORTED_TO_CAROLINE -> listOf("I've spoken to Caroline and she thanked me for rescuing her family from the sea slugs.", "I should finish speaking to Caroline to receive my reward.")
        STAGE_COMPLETE -> listOf("QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun updateStage(player: Player, stage: Int) {
        player.refreshSeaSlugState()
    }

    override fun complete(player: Player) {
        player.inventory.addItemDrop(Obj.bigoysterpearls, 1)
        player.skills.addXpQuest(Skills.FISHING, 7175.0)
        sendQuestCompleteInterface(player, Obj.bigoysterpearls)
        player.refreshSeaSlugState()
    }
}

@ServerStartupEvent
fun mapSeaSlugStateRefresh() {
    onLogin { (player) -> player.refreshSeaSlugState() }
    onChunkEnter { (entity, _) ->
        val player = entity as? Player ?: return@onChunkEnter
        player.refreshSeaSlugState()
    }
}
