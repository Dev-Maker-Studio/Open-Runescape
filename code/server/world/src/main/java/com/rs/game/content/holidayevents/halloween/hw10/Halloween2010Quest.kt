package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Varbit

@QuestHandler(
    quest = Quest.HALLOWEEN_2010,
    startText = "Speak to Pumpkin Pete in Varrock Square.",
    itemsText = "5 free inventory spaces.",
    combatText = "None.",
    rewardsText = "",
    completedStage = 20
)
class Halloween2010Quest : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> = when (Hw10Stage.entries.firstOrNull { it.value == stage }) {
        null -> emptyList()
        Hw10Stage.NOT_STARTED -> listOf("I can start this quest by speaking to ${red("Pumpkin Pete")} beside the dark portal in ${red("Varrock Square")}.")
        Hw10Stage.STARTED -> listOf("I should go through the dark portal and talk to Pumpkin Pete on the other side.")
        Hw10Stage.PREPARING -> listOf("I should see what I can do to make the ${red("party")} run smoothly.", "") +
            player.spiderJournal() + player.autographJournal() + player.moonshineJournal()
        Hw10Stage.PARTY -> listOf("Everything is ready, and the ${red("party")} is about to begin!")
        Hw10Stage.COMPLETE -> listOf("QUEST COMPLETE!")
    }

    override fun getRewardsString(player: Player): String = HW10_EDITION.rewardsText

    override fun complete(player: Player) {
        grantHalloween2010Rewards(player)
        sendQuestCompleteInterface(player, HW10_EDITION.scrollObj, HW10_EDITION.rewardsText)
    }

    override fun updateStage(player: Player, stage: Int) {
        player.vars.setVarBit(Varbit.hw10_main, stage)
    }
}

private fun red(text: String) = "<col=800000>$text</col>"

private fun struck(done: Boolean, lines: List<String>) = if (done) lines.map { "<str>$it" } else lines

private fun Player.spiderJournal(): List<String> {
    if (webState == WebState.NOT_ASKED)
        return emptyList()
    val asked = "I have spoken to ${red("Minstrel Web")}. She would like me to find ${red("her spider assistants")}, who are ${red("hiding")} around ${red("Grim's house")}."
    val left = Hw10Spider.entries.size - spidersFound
    if (left > 0)
        return listOf(asked, "I have ${red("$left")} ${if (left == 1) "spider" else "spiders"} still to find.", "")
    val found = "I have found all the ${red("spider assistants")}. I now need to get them onto the right notes on the ${red("dancefloor")}. I should speak to ${red("Minstrel Web")} once I am done."
    return struck(true, listOf(asked)) + struck(webState == WebState.SOLVED, listOf(found)) + ""
}

private fun Player.autographJournal(): List<String> {
    if (grimState == GrimState.NOT_ASKED)
        return emptyList()
    val getIt = "I should go and speak to the ${red("lead singer")} of ${red("Adamantite Damsel")} and get an ${red("autograph")}."
    if (!zabethSigned && grimState != GrimState.AUTOGRAPH_DELIVERED)
        return listOf(getIt, "")
    return struck(true, listOf(getIt)) + struck(grimState == GrimState.AUTOGRAPH_DELIVERED, listOf("I should go and give the autograph to the Grim Reaper.")) + ""
}

private fun Player.moonshineJournal(): List<String> {
    if (werewolfState == WerewolfState.NOT_ASKED)
        return emptyList()
    return struck(werewolfState == WerewolfState.MOONSHINE_DELIVERED, listOf("I need to make some moonshine for the backing singers. I can talk to them for advice on how to make it. I have been warned that Helga always lies."))
}

fun completeHalloween2010(player: Player) {
    player.musicsManager.unlockMusic(Midi.midi_903)
    player.musicsManager.unlockMusic(Midi.midi_908)
    player.questManager.completeQuest(Quest.HALLOWEEN_2010)
}
