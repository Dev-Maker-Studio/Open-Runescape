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

private const val OG_TASK_STARTED = "watchtowerOgTaskStarted"
private const val OG_TASK_COMPLETE = "watchtowerOgTaskComplete"
private const val RELIC_PART_1_HANDED_IN = "watchtowerRelicPart1HandedIn"

@ServerStartupEvent
fun mapWatchtowerOg() {
    onNpcClick(Npc.og, options = arrayOf("Talk-to")) { (player, npc) ->
        ogDialogue(player, npc)
    }
}

private fun ogDialogue(player: Player, npc: NPC) {
    player.startConversation {
        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_FIND_WAY_INTO_GUTANOTH) {
            npc(
                npc,
                ANGRY,
                "Why you here, little t'ing?"
            )
            return@startConversation
        }

        if (player.getBool(OG_TASK_COMPLETE)) {
            if (!player.inventory.containsItem(Obj.relicpart1, 1) &&
                !player.bank.containsItem(Obj.relicpart1, 1) &&
                !player.getBool(RELIC_PART_1_HANDED_IN)
            ) {
                npc(
                    npc,
                    CALM_TALK,
                    "What, you lost it?"
                ) {
                    player.inventory.addItemDrop(Obj.relicpart1, 1)
                }

                npc(
                    npc,
                    CALM_TALK,
                    "Here, take another and don't lose it!"
                )
                return@startConversation
            }

            npc(
                npc,
                HAPPY_TALKING,
                "Well, well, the little t'ing has got it!"
            )
            return@startConversation
        }

        if (!player.getBool(OG_TASK_STARTED)) {
            npc(
                npc,
                CALM_TALK,
                "Why you here, little t'ing?"
            )

            options {
                op("I seek entrance to the city of ogres.") {
                    player(
                        CALM_TALK,
                        "I seek entrance to the city of ogres."
                    )

                    npc(
                        npc,
                        CALM_TALK,
                        "You got no business there! Just a minute... Maybe if you did somet'ing for me, I might help you get in."
                    )

                    player(
                        CALM_TALK,
                        "What can I do to help an ogre?"
                    )

                    npc(
                        npc,
                        CALM_TALK,
                        "South-east of here der is more ogres - the name of the chieftain is Toban. He stole gold from me and I want it back! Here is a key to the chest it's in. If you bring it here, I may reward you..."
                    ) {
                        player.inventory.addItemDrop(Obj.toban_key, 1)
                        player.set(OG_TASK_STARTED, true)

                        if (player.getQuestStage(Quest.WATCHTOWER) == STAGE_FIND_WAY_INTO_GUTANOTH) {
                            player.setQuestStage(
                                Quest.WATCHTOWER,
                                STAGE_COLLECT_OGRE_RELIC
                            )
                        }
                    }
                }

                op("I have come to kill you.") {
                    player(
                        ANGRY,
                        "I have come to kill you!"
                    )

                    npc(
                        npc,
                        ANGRY,
                        "Kill me, eh? You shall be crushed. Guards!"
                    )
                }
            }

            return@startConversation
        }

        npc(
            npc,
            CALM_TALK,
            "Where my gold from dat dirty Toban?"
        )

        if (player.inventory.containsItem(Obj.stolen_gold, 1)) {
            player(
                HAPPY_TALKING,
                "I have your gold."
            )

            if (!player.inventory.hasFreeSlots()) {
                npc(
                    npc,
                    HAPPY_TALKING,
                    "Hah! This little t'ing is pretty smart for such a little t'ing! Pity you don't have any space for your reward."
                )
            } else {

                npc(
                    npc,
                    HAPPY_TALKING,
                    "Well, well, the little t'ing has got it! Take dis to show little t'ing is a friend to da ogres. Hahahahaha!"
                ) {
                    player.inventory.deleteItem(Obj.stolen_gold, 1)
                    player.inventory.addItem(Obj.relicpart1, 1)
                    player.set(OG_TASK_COMPLETE, true)
                }

                simple(
                    "The ogre gives you part of a horrible statue."
                )
            }

            return@startConversation
        }

        options {
            op("I have your gold.") {
                player(
                    CALM_TALK,
                    "I have your gold."
                )

                npc(
                    npc,
                    ANGRY,
                    "Dat is not what I want! If you want to impress me, get da gold I asked for!"
                )
            }

            op("I haven't got it yet.") {
                player(
                    CALM_TALK,
                    "I haven't got it yet."
                )

                npc(
                    npc,
                    ANGRY,
                    "Don't come back until you have it, unless you wanna be on tonight's menu!"
                )
            }

            op("I have lost the key!") {
                player(
                    WORRIED,
                    "I have lost the key!"
                )

                if (player.inventory.containsItem(Obj.toban_key, 1)) {
                    npc(
                        npc,
                        CALM_TALK,
                        "Oh yeah? What's dat den?"
                    )

                    simple(
                        "It seems you still have the key..."
                    )
                } else {

                    npc(
                        npc,
                        ANGRY,
                        "Stoopid! Take another and don't lose it!"
                    ) {
                        player.inventory.addItemDrop(Obj.toban_key, 1)
                    }
                }
            }
        }
    }
}
