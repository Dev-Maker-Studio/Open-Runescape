package com.rs.game.content.quests.watchtower.dialogues

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.watchtower.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapWatchtowerCityGuard() {
    onNpcClick(
        Npc.city_guard,
        options = arrayOf("Talk-to")
    ) { (player, npc) ->
        cityGuardDialogue(player, npc)
    }
}

private fun cityGuardDialogue(player: Player, npc: NPC) {
    player.startConversation {
        val stage = player.getQuestStage(Quest.WATCHTOWER)

        if (stage < STAGE_GET_SKAVID_MAP) {
            npc(
                npc,
                ANGRY,
                "Grrrr, what business you got here?"
            )
            return@startConversation
        }

        if (player.inventory.containsItem(Obj.skavidmap, 1) ||
            player.bank.containsItem(Obj.skavidmap, 1)
        ) {
            npc(
                npc,
                CALM_TALK,
                "Grrrr, what business you got here?"
            )

            player(
                CALM_TALK,
                "I am on an errand."
            )

            npc(
                npc,
                CALM_TALK,
                "So what you want with me?"
            )

            return@startConversation
        }

        if (player.inventory.containsItem(Obj.deathrune, 1)) {

            player(
                HAPPY_TALKING,
                "I worked it out!"
            ) {
                player.inventory.deleteItem(Obj.deathrune, 1)
            }

            npc(
                npc,
                HAPPY_TALKING,
                "Hur, hur. The imp has done it! Thanks for the rune; this is what you be needing..."
            ) {
                player.inventory.addItemDrop(Obj.skavidmap, 1)

                if (stage == STAGE_GET_SKAVID_MAP) {
                    player.setQuestStage(
                        Quest.WATCHTOWER,
                        STAGE_LEARN_SKAVID_LANGUAGE
                    )
                }
            }

            simple(
                "The guard gives you a map."
            )

            return@startConversation
        }

        npc(
            npc,
            ANGRY,
            "Grrrr, what business you got here?"
        )

        player(
            CALM_TALK,
            "I am on an errand."
        )

        npc(
            npc,
            CALM_TALK,
            "So what you want with me?"
        )

        options {
            op("I am an ogre killer come to destroy you!") {
                player(
                    ANGRY,
                    "I am an ogre killer come to destroy you!"
                )

                npc(
                    npc,
                    ANGRY,
                    "I would like to see you try!"
                ) {
                    npc.setCombatTarget(player)
                }
            }

            op("I seek passage into the skavid caves.") {
                player(
                    CALM_TALK,
                    "I seek passage into the skavid caves."
                )

                npc(
                    npc,
                    CALM_TALK,
                    "Is that so... You humour me small t'ing, answer this riddle and I will help you... I want you to bring me an item: I will give you all the letters of this item, you work out what it is... My first is in days, but not in years, My second is in evil, and also in tears, My third is in all, but not in none, My fourth is in hot, but not in sun."
                )

                npc(
                    npc,
                    CALM_TALK,
                    "My fifth is in heaven, and also in hate, My sixth is in fearing, but not in fate, My seventh is in plush, but not in place. My eighth is in nine, but not in eight, My last is in earth, and also in great. My whole is an object, that magic will make, It brings wrack and ruin to all in its wake... Now how long, I wonder, will this riddle take?"
                )
            }
        }
    }
}
