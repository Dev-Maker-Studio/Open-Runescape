package com.rs.game.content.quests.sea_slug.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.getInteractionDistance
import com.rs.plugin.kts.onNpcClick

internal const val SPOKE_TO_KENNITH_ABOUT_ESCAPE = "spokeToKennithAboutEscape"

@ServerStartupEvent
fun mapKennithSeaSlug() {
    getInteractionDistance(Npc.kennith_platform) { player, npc ->
        if (npc.tile == KENNITH_PLATFORM_TILE && player.isSeaSlugOwnedNpc(npc)) 3 else 0
    }
    onNpcClick(Npc.npc_697, Npc.kennith_platform) { (player, npc) ->
        if (!player.isSeaSlugOwnedNpc(npc)) {
            player.sendMessage("That Kennith is not part of your quest state.")
            return@onNpcClick
        }
        player.talkToKennith(npc)
    }
}

private fun Player.talkToKennith(npc: NPC) {
    val stage = getQuestStage(Quest.SEA_SLUG)
    startConversation {
        when {
            stage < STAGE_FOUND_KENNITH -> {
                player(WORRIED, "Are you okay young one?")
                npc(npc, SAD, "No, I want my daddy!")
                player(CONFUSED, "Where is your father?")
                npc(npc, SAD, "He went to get help days ago.")
                npc(npc, SCARED, "The nasty fishermen tried to throw me and daddy into the sea. So he told me to hide here.")
                player(CALM, "That's good advice, you stay here and I'll go try and find your father.") {
                    setQuestStage(Quest.SEA_SLUG, STAGE_FOUND_KENNITH)
                }
            }

            stage < STAGE_RESCUE_KENNITH -> {
                player(WORRIED, "Are you okay?")
                npc(npc, SAD, "I want to see daddy!")
                player(CALM, "I'm working on it.")
            }

            stage < STAGE_OPENED_WALL -> {
                player(WORRIED, "Hello Kennith, are you okay?")
                npc(npc, SAD, "No, I want my daddy.")
                player(CALM, "You'll be able to see him soon. First we need to get you back to land, come with me to the boat.")
                npc(npc, SCARED, "No!")
                player(CONFUSED, "What, why not?")
                npc(npc, SCARED, "I'm scared of those nasty sea slugs. I won't go near them.")
                player(CALM, "Okay, you wait here and I'll go figure another way to get you out.") {
                    questManager.getAttribs(Quest.SEA_SLUG).setB(SPOKE_TO_KENNITH_ABOUT_ESCAPE, true)
                }
            }

            stage == STAGE_OPENED_WALL -> {
                player(CALM, "Kennith, I've made an opening in the wall. You can come out through there.")
                npc(npc, WORRIED, "Are there any sea slugs on the other side?")
                player(CALM, "Not one.")
                npc(npc, CONFUSED, "How will I get downstairs?")
                player(CALM, "I'll figure that out in a moment.")
                npc(npc, CALM_TALK, "Ok, when you have I'll come out.") {
                    setQuestStage(Quest.SEA_SLUG, STAGE_NEEDS_CRANE)
                }
            }

            stage < STAGE_KENNITH_RESCUED -> {
                player(CALM, "Hold on Kennith. I'll get the crane into position.")
                npc(npc, WORRIED, "Okay, but please hurry!")
            }

            else -> {
                player(CALM, "Hello there, Kennith. How are you today?")
                npc(npc, HAPPY_TALKING, "Great ${getPronoun("mister", "miss")}! Thanks again for rescuing me.")
                player(HAPPY_TALKING, "It was my pleasure Kennith. You take care now.")
                npc(npc, HAPPY_TALKING, "Sure ${getPronoun("mister", "miss")}. Goodbye now.")
            }
        }
    }
}
