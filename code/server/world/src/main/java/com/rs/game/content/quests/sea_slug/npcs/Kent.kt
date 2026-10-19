package com.rs.game.content.quests.sea_slug.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapKentSeaSlug() {
    onNpcClick(Npc.kent) { (player, npc) ->
        if (!player.validateSeaSlugNpcOwnership(npc)) return@onNpcClick
        player.talkToKent(npc)
    }
}

private fun Player.talkToKent(npc: NPC) {
    val stage = getQuestStage(Quest.SEA_SLUG)
    startConversation {
        when {
            stage in STAGE_FOUND_KENNITH until STAGE_RESCUE_KENNITH && isAtKentsIsland() -> {
                npc(npc, HAPPY_TALKING, "Oh thank Saradomin! I thought I'd be left out here forever.") {
                    if (getQuestStage(Quest.SEA_SLUG) < STAGE_FOUND_KENT)
                        setQuestStage(Quest.SEA_SLUG, STAGE_FOUND_KENT)
                }
                player(CALM, "Your wife sent me out to find you and your boy. Kennith's fine by the way, he's on the platform.")
                npc(npc, WORRIED, "I knew the row boat wasn't sea worthy. I couldn't risk bringing him along but you must get him off that platform.")
                player(CONFUSED, "What's going on here?")
                npc(npc, WORRIED, "Five days ago we pulled in a huge catch. As well as fish we caught small slug like creatures, hundreds of them.")
                npc(npc, WORRIED, "That's when the fishermen began to act strange.")
                npc(npc, SCARED, "It was the sea slugs, they attach themselves to your body and somehow take over the mind of the carrier.")
                npc(npc, WORRIED, "I told Kennith to hide until I returned but I was washed up here.")
                npc(npc, WORRIED, "Please go back and get my boy, you can send help for me later.")
                npc(npc, AMAZED, "${displayName} wait!")
                simple("*sloop*<br><br>He pulls a sea slug from under your top.") {
                    npc.anim(Seq.seaslug_grab_n_destroy)
                    inventory.deleteItem(Obj.seasluginv, 1)
                }
                npc(npc, WORRIED, "A few more minutes and that thing would have full control of your body.")
                player(SCARED, "Yuck! Thanks Kent.") {
                    setQuestStage(Quest.SEA_SLUG, STAGE_RESCUE_KENNITH)
                }
            }

            stage < STAGE_REPORTED_TO_CAROLINE -> {
                player(CALM, "Hello.")
                npc(npc, WORRIED, "Oh my, I must get back to shore.")
            }

            else -> {
                npc(npc, HAPPY_TALKING, "Hello there, ${displayName}!")
                player(CALM, "Hello again Kent.")
                npc(npc, HAPPY_TALKING, "I never did get the chance to thank you properly for saving Kennith and myself.")
                player(CALM, "Oh, don't be silly, it was nothing really.")
                npc(npc, CALM_TALK, "Play it down if you will. It was a truly brave thing you did.")
                player(CALM, "I only did what anyone would have done in my position. I have to go now, take care.")
                npc(npc, HAPPY_TALKING, "You too, ${displayName}. Goodbye.")
            }
        }
    }
}
