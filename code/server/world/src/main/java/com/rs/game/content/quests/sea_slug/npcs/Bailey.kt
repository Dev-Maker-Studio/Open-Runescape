package com.rs.game.content.quests.sea_slug.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapBaileySeaSlug() {
    onNpcClick(Npc.bailey) { (player, npc) -> player.talkToBailey(npc) }
}

private fun Player.talkToBailey(npc: NPC) {
    val stage = getQuestStage(Quest.SEA_SLUG)
    startConversation {
        when {
            stage < STAGE_RESCUE_KENNITH -> {
                player(CALM, "Hello there.")
                npc(npc, SCARED, "What? Who are you? Come inside quickly!")
                npc(npc, WORRIED, "What are you doing here?")
                player(CALM, "I'm trying to find out what happened to a boy named Kennith.")
                npc(npc, WORRIED, "Oh you mean Kent's son. He's around somewhere, probably hiding if he knows what's good for him.")
                player(CONFUSED, "Hiding from what? What's got you so frightened?")
                npc(npc, SCARED, "Haven't you seen all those things out there?")
                player(CONFUSED, "The sea slugs?")
                npc(npc, WORRIED, "It all began about a week ago. We pulled up a haul of deep sea flatfish. Mixed in with them we found those slug things, but thought nothing of it.")
                npc(npc, WORRIED, "Not long after that my friends began to change, now they spend all day pulling in hauls of fish, only to throw back the fish and keep those nasty sea slugs.")
                npc(npc, FRUSTRATED, "What am I supposed to do with those? I haven't figured out how to kill one yet, if I put them near the stove they squirm and jump away.")
                player(SKEPTICAL, "I doubt they would taste too good.")
                npc(npc, FRUSTRATED, "This is no time for humour.")
                player(SAD, "I'm sorry, I didn't mean to upset you.")
                npc(npc, WORRIED, "That's okay. I just can't shake the feeling that this is the start of something... Terrible.")
            }

            stage in STAGE_RESCUE_KENNITH until STAGE_KENNITH_RESCUED && !inventory.containsItem(Obj.torch_lit) -> {
                player(CALM, "Hello.")
                npc(npc, SCARED, "Oh, thank the gods it's you. They've all gone mad I tell you, one of the fishermen tried to throw me into the sea!")
                player(CALM, "They're all being controlled by the sea slugs.")
                npc(npc, WORRIED, "I figured as much.")
                player(WORRIED, "I need to get Kennith off this platform, but I can't get past the fishermen.")
                npc(npc, CALM_TALK, "The sea slugs are scared of heat, I figured that out when I tried to cook them.") {
                    if (getQuestStage(Quest.SEA_SLUG) == STAGE_RESCUE_KENNITH)
                        setQuestStage(Quest.SEA_SLUG, STAGE_LEARNED_ABOUT_HEAT)
                }
                if (!inventory.containsItem(Obj.torch_unlit)) {
                    npc(npc, CALM_TALK, "Here.")
                    item(Obj.torch_unlit, "Bailey gives you a torch.") {
                        if (!inventory.containsOneItem(Obj.torch_unlit, Obj.torch_lit))
                            inventory.addItemDrop(Obj.torch_unlit, 1)
                        if (getQuestStage(Quest.SEA_SLUG) < STAGE_NEEDS_LIT_TORCH)
                            setQuestStage(Quest.SEA_SLUG, STAGE_NEEDS_LIT_TORCH)
                    }
                    npc(npc, CALM_TALK, "I doubt the fishermen will come near you if you can get this torch lit.")
                    npc(npc, WORRIED, "The only problem is all the wood and flint are damp... I can't light a thing!")
                } else {
                    player(WORRIED, "I'd better find a way to light this torch, and soon.") {
                        if (getQuestStage(Quest.SEA_SLUG) < STAGE_NEEDS_LIT_TORCH)
                            setQuestStage(Quest.SEA_SLUG, STAGE_NEEDS_LIT_TORCH)
                    }
                }
            }

            stage in STAGE_RESCUE_KENNITH..STAGE_LEARNED_ABOUT_HEAT -> {
                player(CALM, "I've found a way to make some light.")
                npc(npc, CALM_TALK, "Good. The sea slugs are scared of heat; I figured that out when I tried to cook them.") {
                    setQuestStage(Quest.SEA_SLUG, STAGE_NEEDS_LIT_TORCH)
                }
                npc(npc, HAPPY_TALKING, "That flame should keep the fishermen away from you.")
            }

            stage < STAGE_KENNITH_RESCUED -> {
                player(HAPPY_TALKING, "I've managed to light the torch.")
                npc(npc, HAPPY_TALKING, "Well done traveller, you'd better get Kennith out of here soon.")
                npc(npc, WORRIED, "The fishermen are becoming stranger by the minute, and they keep pulling up those blasted sea slugs.")
                player(CALM, "Don't worry I'm working on it.")
                npc(npc, WORRIED, "Just be sure to watch your back.")
                simple("The fishermen seem to have taken notice of you.")
            }

            else -> {
                player(CALM, "Hello Bailey.")
                npc(npc, HAPPY_TALKING, "Hello again. I saw you managed to get Kennith off the platform. Well done. He wasn't safe around those slugs.")
                player(CONFUSED, "Are you going to come back with us?")
                npc(npc, CALM_TALK, "No, these fishermen are my friends, I'm sure they can be saved. I'm going to stay and try to get rid of all these slugs.")
                player(CONFUSED, "Do you have a way of removing the slugs once they've attached themselves to someone?")
                npc(npc, SAD, "Not yet but I'll keep working on it.")
                player(WORRIED, "I only hope they stay confined to the fishing platform.")
                npc(npc, WORRIED, "Yes, I dread to think what would happen if they got to Witchaven nearby.")
                npc(npc, CALM_TALK, "I guess I'll just have to keep an eye on things here until a way of removing the slugs can be found.")
                player(HAPPY_TALKING, "You're braver than most. Take care of yourself Bailey.")
                npc(npc, HAPPY_TALKING, "You too ${displayName}.")
            }
        }
    }
}
