package com.rs.game.content.quests.watchtower.dialogues

import org.darkan.core.gameval.gen.Varbit
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
import com.rs.plugin.kts.onNpcDrop

private const val GREW_TASK_STARTED = "watchtowerGrewTaskStarted"
private const val GREW_TASK_COMPLETE = "watchtowerGrewTaskComplete"
private const val RELIC_PART_2_HANDED_IN = "watchtowerRelicPart2HandedIn"

@ServerStartupEvent
fun mapWatchtowerGrew() {
    onNpcClick(Npc.gorad, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            player(ANGRY, "I've come to knock your teeth out!")
            npc(npc, ANGRY, "You shut your face! I smack you till you dead and sorry!") {
                npc.setCombatTarget(player)
            }
        }
    }
    onNpcDrop(arrayOf<Any>(Npc.gorad), arrayOf<Any>(Obj.ogretooth)) { event ->
        event.deleteItem()
        val player = event.player
        if (!player.getBool(GREW_TASK_STARTED) || player.getBool(GREW_TASK_COMPLETE) ||
            player.getQuestStage(Quest.WATCHTOWER) < STAGE_FIND_WAY_INTO_GUTANOTH ||
            player.isQuestComplete(Quest.WATCHTOWER) ||
            player.inventory.containsItem(Obj.ogretooth, 1) || player.bank.containsItem(Obj.ogretooth, 1)
        ) return@onNpcDrop
        player.inventory.addItemDrop(Obj.ogretooth, 1)
        player.sendMessage("Gorad has gone.")
        player.sendMessage("He's dropped a tooth; I'll keep that!")
    }
    onNpcClick(Npc.grew, options = arrayOf("Talk-to")) { (player, npc) ->
        grewDialogue(player, npc)
    }
}

private fun grewDialogue(player: Player, npc: NPC) {
    player.startConversation {
        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_FIND_WAY_INTO_GUTANOTH) {
            npc(
                npc,
                ANGRY,
                "What do you want, little morsel? You would look good on my plate!"
            )
            return@startConversation
        }

        if (player.getBool(GREW_TASK_COMPLETE)) {
            if (!player.inventory.containsItem(Obj.relicpart2, 1) &&
                !player.bank.containsItem(Obj.relicpart2, 1) &&
                !player.getBool(RELIC_PART_2_HANDED_IN)
            ) {
                npc(
                    npc,
                    CALM_TALK,
                    "What, you lost it?"
                ) {
                    player.inventory.addItemDrop(Obj.relicpart2, 1)
                }

                npc(
                    npc,
                    CALM_TALK,
                    "Take another and don't lose it, morsel."
                )
                return@startConversation
            }

            if (!player.inventory.containsItem(Obj.powering_crystal1, 1) &&
                !player.bank.containsItem(Obj.powering_crystal1, 1) &&
                player.vars.getVarBit(Varbit.watchtower_pillar_2) == 0 &&
                !player.isQuestComplete(Quest.WATCHTOWER)
            ) {
                npc(
                    npc,
                    CALM_TALK,
                    "The morsel has lost the crystal?"
                )

                npc(
                    npc,
                    HAPPY_TALKING,
                    "Grew has another. Take it, morsel."
                ) {
                    player.inventory.addItemDrop(Obj.powering_crystal1, 1)
                }

                return@startConversation
            }

            npc(
                npc,
                HAPPY_TALKING,
                "The morsel has done well."
            )
            return@startConversation
        }

        if (!player.getBool(GREW_TASK_STARTED)) {
            npc(
                npc,
                ANGRY,
                "What do you want, little morsel? You would look good on my plate!"
            )

            player(
                CALM_TALK,
                "I want to enter the city of ogres."
            )

            npc(
                npc,
                ANGRY,
                "Hah! I should eat you instead!"
            )

            options {
                op("Don't eat me; I can help you.") {
                    player(
                        WORRIED,
                        "Don't eat me; I can help you."
                    )

                    npc(
                        npc,
                        CALM_TALK,
                        "What can a morsel like you do for me?"
                    )

                    player(
                        HAPPY_TALKING,
                        "I am a mighty adventurer, slayer of monsters and user of magic powers."
                    )

                    npc(
                        npc,
                        HAPPY_TALKING,
                        "Well, well, perhaps the morsel can help after all... If you t'ink you're tough, find Gorad, my enemy to the south-east, and knock one of his teeth out! Heheheheh!"
                    ) {
                        player.set(GREW_TASK_STARTED, true)

                        if (player.getQuestStage(Quest.WATCHTOWER) == STAGE_FIND_WAY_INTO_GUTANOTH) {
                            player.setQuestStage(
                                Quest.WATCHTOWER,
                                STAGE_COLLECT_OGRE_RELIC
                            )
                        }
                    }
                }

                op("You will have to kill me first.") {
                    player(
                        ANGRY,
                        "You will have to kill me first."
                    )

                    npc(
                        npc,
                        ANGRY,
                        "That's not tricky - guards!"
                    )
                }
            }

            return@startConversation
        }

        npc(
            npc,
            CALM_TALK,
            "The morsel is back. Does it have our tooth for us?"
        )

        if (!player.inventory.containsItem(Obj.ogretooth, 1)) {
            player(
                WORRIED,
                "Err, I don't have it."
            )

            npc(
                npc,
                ANGRY,
                "Morsel, you dare to return without the tooth! Either you are stoopid, or wants to be eaten!"
            )

            return@startConversation
        }

        player(
            HAPPY_TALKING,
            "I have it."
        ) {
            player.inventory.deleteItem(Obj.ogretooth, 1)

            player.inventory.addItemDrop(Obj.relicpart2, 1)
            player.inventory.addItemDrop(Obj.powering_crystal1, 1)

            player.set(GREW_TASK_COMPLETE, true)
        }

        npc(
            npc,
            HAPPY_TALKING,
            "It's got it; good, good. That'll annoy Gorad lots! Heheheheh. Here's a token of my gratitude: some old gem I stole from Gorad. And an old part of a statue."
        )

        simple(
            "You get part of a horrible statue... and one of the crystals!"
        )
    }
}
