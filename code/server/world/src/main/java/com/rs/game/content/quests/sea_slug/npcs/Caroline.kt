package com.rs.game.content.quests.sea_slug.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapCarolineSeaSlug() {
    onNpcClick(Npc.caroline) { (player, npc) -> player.talkToCaroline(npc) }
}

private fun Player.talkToCaroline(npc: NPC) {
    startConversation {
        when (getQuestStage(Quest.SEA_SLUG)) {
            STAGE_UNSTARTED -> {
                player(CALM, "Hello there.")
                npc(npc, WORRIED, "Is there any chance you could help me?")
                player(CONFUSED, "What's wrong?")
                npc(npc, WORRIED, "It's my husband, he works on a fishing platform. Once a month he takes our son, Kennith, out with him.")
                npc(npc, WORRIED, "They usually write to me regularly, but I've heard nothing all week. It's very strange.")
                player(SKEPTICAL, "Maybe the post was lost!")
                npc(npc, WORRIED, "Maybe, but no-one's heard from the other fishermen on the platform. Their families are becoming quite concerned.")
                npc(npc, WORRIED, "Is there any chance you could visit the platform and find out what's going on?")
                questStart(
                    Quest.SEA_SLUG,
                    accepted = {
                        if (!Quest.SEA_SLUG.meetsReqs(this@talkToCaroline)) {
                            simple("You do not meet the requirements to start: 'Sea Slug'.")
                        } else {
                            player(CALM, "I suppose so, how do I get there?")
                            npc(npc, HAPPY_TALKING, "That's very good of you ${displayName}. My friend Holgart will take you there.")
                            player(CALM, "Ok, I'll go and see if they're ok.")
                            npc(npc, CALM_TALK, "I'll reward you for your time. It'll give me peace of mind to know Kennith and my husband, Kent, are safe.") {
                                setQuestStage(Quest.SEA_SLUG, STAGE_FIND_HOLGART)
                            }
                        }
                    },
                    declined = {
                        player(SAD, "I'm sorry, I'm too busy.")
                        npc(npc, SAD, "That's a shame.")
                        player(CALM, "Bye.")
                        npc(npc, CALM_TALK, "Bye.")
                    }
                )
            }

            in STAGE_FIND_HOLGART until STAGE_KENNITH_RESCUED -> {
                player(CALM, "Hello Caroline.")
                npc(npc, WORRIED, "Brave ${displayName}, have you any news about my son and his father?")
                player(CALM, "I'm working on it now Caroline.")
                npc(npc, WORRIED, "Please bring them back safe and sound.")
                player(CALM, "I'll do my best.")
            }

            STAGE_KENNITH_RESCUED, STAGE_REPORTED_TO_CAROLINE -> {
                player(CALM, "Hello.")
                npc(npc, HAPPY_TALKING, "Brave ${displayName}, you've returned!")
                npc(npc, WORRIED, "Kennith told me about the strange goings-on at the platform. I had no idea it was so serious.")
                npc(npc, WORRIED, "I could have lost my son and my husband if it wasn't for you.")
                player(CALM, "We found Kent stranded on an island.")
                npc(npc, CALM_TALK, "Yes, Holgart told me and sent a rescue party out. Kent's back home now, resting with Kennith. I don't think he'll be doing any fishing for a while.")
                npc(npc, HAPPY_TALKING, "Here, take these Oyster pearls as a reward. They're worth quite a bit and can be used to make lethal crossbow bolts.") {
                    setQuestStage(Quest.SEA_SLUG, STAGE_REPORTED_TO_CAROLINE)
                }
                player(HAPPY_TALKING, "Thanks!")
                npc(npc, HAPPY_TALKING, "Thank you. Take care of yourself ${displayName}.") {
                    questManager.completeQuest(Quest.SEA_SLUG)
                }
            }

            else -> {
                player(CALM, "Hello again.")
                npc(npc, CALM_TALK, "Hello traveller, how are you?")
                player(CALM, "Not bad thanks, yourself?")
                npc(npc, HAPPY_TALKING, "I'm good. Busy as always looking after Kent and Kennith but no complaints.")
            }
        }
    }
}
