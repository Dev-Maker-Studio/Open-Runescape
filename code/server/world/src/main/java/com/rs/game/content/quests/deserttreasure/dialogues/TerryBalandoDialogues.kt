package com.rs.game.content.quests.deserttreasure

import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj

class TerryBalandoDialogues(val player: Player, val npc: NPC) {
    init {
        player.startConversation {
            when (player.getQuestStage(Quest.DESERT_TREASURE)) {
                STAGE_TERRY_BALANDO -> {
                    player(SKEPTICAL, "Hello, are you Terry Balando?")
                    npc(npc, SKEPTICAL, "That's right, who wants to know...?")
                    npc(npc, CALM_TALK, "Ah yes, I recognise you! You're the ${if (player.appearance.isMale) "fellow" else "lass"} who found that strange artefact for the museum, aren't you? What can I do for you now?")

                    if (!player.inventory.containsOneItem(Obj.four_diamonds_etchings)) {
                        player(CALM_TALK, "I had some etchings to deliver to you, but I don't have them on me...")
                        npc(npc, CALM_TALK, "Then... you better go get them for me?")
                    } else {
                        player(CALM_TALK, "That's right. I was in the desert down by the Bedabin Camp, and I found an archaeologist who asked me to deliver this to you.")
                        npc(npc, AMAZED, "You spoke to the legendary Asgarnia Smith??? Quickly, let me see what he had to give you! He is always at the forefront of archaeological breakthroughs!") {
                            player.inventory.deleteItem(Obj.four_diamonds_etchings, 1)
                        }
                        player(CALM_TALK, "So what does the inscription say? Anything interesting?")
                        npc(npc, AMAZED, "This is fascinating! These cuneiforms seem to predate even the settlement we are excavating here. This is most interesting indeed!")
                        player(SKEPTICAL, "Can you translate it for me?")
                        npc(npc, SKEPTICAL, "I am not familiar with this particular language, but the similarities in the pictographs may allow me to work out a rough translation.")
                        player(SKEPTICAL_HEAD_SHAKE, "Um... so, can you translate it for me or not?")
                        npc(npc, CALM_TALK, "I may be able to make a rough translation. It may be obscure in places, but it should give you the general meaning of what was written.")
                        npc(npc, CALM_TALK, "Please wait a moment. I will write down what I can translate into a journal for you. Then take it back to Asgarnia; I think he will be extremely interested in it.")
                        if (player.inventory.hasFreeSlots()) {
                            item(Obj.four_diamonds_translation_primer, "Terry Balando gives you the translation he prepared.") {
                                player.inventory.addItem(Obj.four_diamonds_translation_primer, 1)
                                player.setQuestStage(Quest.DESERT_TREASURE, STAGE_RETURN_TO_ASGARNIA)
                            }
                        } else {
                            simple("You need at least 1 free inventory slot to receive the translation.")
                        }
                    }
                }

                STAGE_RETURN_TO_ASGARNIA -> {
                    npc(npc, CALM_TALK, "Hello again.")
                    if (!player.containsItem(Obj.four_diamonds_translation_primer)) {
                        player(SAD, "I lost that translation you gave me...")
                        npc(npc, CHEERFUL, "Oh, no matter. I mostly remember what it said anyway. Here you go!")
                        if (player.inventory.hasFreeSlots()) {
                            item(Obj.four_diamonds_translation_primer, "Terry Balando gives you the translation he prepared.") {
                                player.inventory.addItem(Obj.four_diamonds_translation_primer, 1)
                            }
                        } else {
                            simple("You need at least 1 free inventory slot to receive the translation.")
                        }
                    } else {
                        player(CALM_TALK, "I think the translation was useful, thanks!")
                        npc(npc, CALM_TALK, "Excellent. I hope Asgarnia finds something of great archaeological importance out there.")
                    }
                }

                else -> {
                    npc(npc, CALM_TALK, "Hello again. How goes the archaeology?")
                    player(CALM_TALK, "Pretty well, thanks.")
                    npc(npc, CALM_TALK, "Glad to hear it!")
                }
            }
        }
    }

    fun receiveEtchings() {
        if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_TERRY_BALANDO)
            return

        if (!player.inventory.containsOneItem(Obj.four_diamonds_etchings))
            return

        player.startConversation {
            player(CALM_TALK, "I was asked to deliver these etchings to you.")
            npc(npc, AMAZED, "These are from Asgarnia Smith? Quickly, let me have a look!") {
                player.inventory.deleteItem(Obj.four_diamonds_etchings, 1)
            }
            npc(npc, AMAZED, "Fascinating! Give me a moment and I will see what I can make of these inscriptions.")
            if (player.inventory.hasFreeSlots()) {
                item(Obj.four_diamonds_translation_primer, "Terry Balando gives you the translation he prepared.") {
                    player.inventory.addItem(Obj.four_diamonds_translation_primer, 1)
                    player.setQuestStage(Quest.DESERT_TREASURE, STAGE_RETURN_TO_ASGARNIA)
                }
            } else {
                simple("You need at least 1 free inventory slot to receive the translation.")
            }
        }
    }
}
