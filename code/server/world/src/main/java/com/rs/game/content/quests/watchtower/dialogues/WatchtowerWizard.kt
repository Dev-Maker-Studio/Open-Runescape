package com.rs.game.content.quests.watchtower.dialogues

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.dialogue.statements.QuestStartStatement
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.watchtower.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private const val MAD_SKAVID_COMPLETE = "watchtowerMadSkavidComplete"

private const val RELIC_PART_1_HANDED_IN = "watchtowerRelicPart1HandedIn"
private const val RELIC_PART_2_HANDED_IN = "watchtowerRelicPart2HandedIn"
private const val RELIC_PART_3_HANDED_IN = "watchtowerRelicPart3HandedIn"

@ServerStartupEvent
fun mapWatchtowerWizard() {
    onNpcClick(Npc.watchtower_wizard, options = arrayOf("Talk-to")) { (player, npc) ->
        watchtowerWizardDialogue(player, npc)
    }
    onNpcClick(Npc.qip_watchtower_wizard_02, Npc.qip_watchtower_wizard_03, Npc.qip_watchtower_wizard_04,
        options = arrayOf("Talk-to")) { (player, npc) ->
        val stage = player.getQuestStage(Quest.WATCHTOWER)
        player.startConversation {
            when (stage) {
                STAGE_COMPLETE -> npc(npc, HAPPY_TALKING, "All's well that ends well.")
                STAGE_UNSTARTED -> {
                    player(CALM_TALK, "What's going on here?")
                    npc(npc, CALM_TALK, "You'll have to speak with the Watchtower Wizard about that.")
                }
                STAGE_SEARCH_WATCHTOWER -> {
                    player(CALM_TALK, "I've got some questions about the search for evidence.")
                    npc(npc, CALM_TALK, "You'll have to speak with the Watchtower Wizard about that.")
                }
                in STAGE_LEARN_SKAVID_LANGUAGE..STAGE_FIND_SHAMAN_ENCLAVE -> {
                    player(CALM_TALK, "I have gained access to the skavid caves.")
                    npc(npc, CALM_TALK, "You'll have to speak to the Watchtower Wizard about that.")
                    player(CALM_TALK, "'You'll have to speak to the Watchtower Wizard about that.'")
                    player(ANGRY, "I'd get more help out of a-")
                    npc(npc, CONFUSED, "What was that?")
                    player(CALM_TALK, "Nothing.")
                }
                STAGE_DISCOVER_SHAMANS -> {
                    player(CALM_TALK, "The shamans are too powerful for me to attack!")
                    npc(npc, CALM_TALK, "You'll have to speak with the Watchtower Wizard about that.")
                }
                STAGE_MAKE_OGRE_POTION -> {
                    player(CALM_TALK, "Any chance of advice about this potion?")
                    npc(npc, CALM_TALK, "Nope. Try the Watchtower Wizard.")
                }
                STAGE_DEFEAT_SHAMANS -> {
                    player(CALM_TALK, "Any chance of a hand?")
                    npc(npc, CALM_TALK, "Well, you could ask the Watchtower Wizard for one.")
                }
                in STAGE_RECOVER_CRYSTALS..STAGE_ACTIVATE_WATCHTOWER -> {
                    player(CALM_TALK, "What should I do now?")
                    npc(npc, CALM_TALK, "You will need to speak to the Watchtower Wizard.")
                    npc(npc, CALM_TALK, "Seriously, he's the one in charge here.")
                }
                else -> npc(npc, CALM_TALK, "You'll have to speak with the Watchtower Wizard about that.")
            }
        }
    }
}

private fun watchtowerWizardDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (player.getQuestStage(Quest.WATCHTOWER)) {
            STAGE_UNSTARTED ->
                startQuest(player, npc)

            STAGE_SEARCH_WATCHTOWER ->
                searchWatchtower(player, npc)

            STAGE_FIND_WAY_INTO_GUTANOTH ->
                findingGuTanoth(player, npc)

            STAGE_COLLECT_OGRE_RELIC ->
                ogreRelic(player, npc)

            STAGE_ENTER_GUTANOTH ->
                enterGuTanoth(player, npc)

            STAGE_GET_SKAVID_MAP ->
                getSkavidMap(player, npc)

            STAGE_LEARN_SKAVID_LANGUAGE ->
                skavidMapProgress(player, npc)

            STAGE_SEARCH_SKAVID_CAVES ->
                searchingSkavidCaves(player, npc)

            STAGE_FIND_SHAMAN_ENCLAVE ->
                findingShamanEnclave(player, npc)

            STAGE_DISCOVER_SHAMANS ->
                discoveredShamans(player, npc)

            STAGE_MAKE_OGRE_POTION ->
                makeOgrePotion(player, npc)

            STAGE_DEFEAT_SHAMANS ->
                defeatShamans(player, npc)

            STAGE_RECOVER_CRYSTALS ->
                recoverCrystals(player, npc)

            STAGE_RESTORE_WATCHTOWER ->
                restoreWatchtower(npc)

            STAGE_ACTIVATE_WATCHTOWER ->
                activateWatchtower(npc)

            STAGE_COMPLETE ->
                postQuest(player, npc)

            else ->
                npc(npc, CALM_TALK, "Hello.")
        }
    }
}

private fun DialogueBuilder.startQuest(
    player: Player,
    npc: NPC
) {
    npc(
        npc,
        CALM_TALK,
        "Who are you? Are you one of the new guards?"
    )

    player(
        CALM_TALK,
        "No, I'm an adventurer."
    )

    npc(
        npc,
        CALM_TALK,
        "Well what are you doing here?"
    )

    player(
        CALM_TALK,
        "Looking for adventures - what else?"
    )

    npc(
        npc,
        SAD,
        "Oh my, oh my! What does it all matter in the end, anyway?"
    )

    options {
        op("What's the matter?") {
            player(
                CALM_TALK,
                "What's the matter?"
            )

            npc(
                npc,
                SAD,
                "Oh dear, oh dear. Darn and drat!"
            )

            npc(
                npc,
                CALM_TALK,
                "We try hard to keep this town protected."
            )

            npc(
                npc,
                SAD,
                "But how can we do that when the Watchtower isn't working?"
            )

            player(
                CALM_TALK,
                "What do you mean it isn't working?"
            )

            npc(
                npc,
                CALM_TALK,
                "The Watchtower here works by the power of magic. An ancient spell designed to ward off ogres that has been in place here for many moons."
            )

            npc(
                npc,
                CALM_TALK,
                "The exact knowledge of the spell is lost to us now, but the essence of the spell has been infused into four powering crystals that keep the tower protected from the hordes in the Feldips."
            )

            options {
                op("So how come the spell doesn't work?") {
                    player(
                        CALM_TALK,
                        "So how come the spell doesn't work?"
                    )

                    npc(
                        npc,
                        WORRIED,
                        "The crystals! The crystals have been taken!"
                    )

                    player(
                        CALM_TALK,
                        "Taken?"
                    )

                    npc(
                        npc,
                        WORRIED,
                        "Stolen!"
                    )

                    player(
                        CALM_TALK,
                        "Stolen?"
                    )

                    npc(
                        npc,
                        ANGRY,
                        "Yes, yes! Do I have to repeat myself?"
                    )

                    goto("questOffer")
                }

                op("I'm not interested in the rantings of an old wizard.") {
                    player(
                        CALM_TALK,
                        "I'm not interested in the rantings of an old wizard."
                    )

                    npc(
                        npc,
                        ANGRY,
                        "Hmph! Suit yourself."
                    )
                }
            }
        }

        op("You wizards are always complaining.") {
            player(
                CALM_TALK,
                "You wizards are always complaining."
            )

            npc(
                npc,
                ANGRY,
                "Complaining? Complaining!"
            )

            npc(
                npc,
                ANGRY,
                "What folks these days don't realise is that if it weren't for us wizards, this entire world would be overrun with every creature you could possibly imagine. And some you couldn't even conceive of!"
            )
        }
    }

    label("questOffer")

    if (!Watchtower.meetsRequirements(player) || !Quest.WATCHTOWER.meetsReqs(player)) {
        simple("You do not meet the requirements to start : 'Watchtower'.")
        return
    }

    statementWithOptions(QuestStartStatement(Quest.WATCHTOWER), {
        player(
            CALM_TALK,
            "Can I be of help?"
        ) { startWatchtower(player) }

        npc(
            npc,
            HAPPY_TALKING,
            "Help? Oh wonderful, dear traveller!"
        )

        npc(
            npc,
            HAPPY_TALKING,
            "Yes I could do with an extra pair of eyes here."
        )

        player(
            CONFUSED,
            "???"
        )

        npc(
            npc,
            CALM_TALK,
            "There must be some evidence of what has happened here. Perhaps you could assist me in searching for clues?"
        )

        player(
            CALM_TALK,
            "I would be happy to."
        )

        npc(
            npc,
            CALM_TALK,
            "Try searching the surrounding area. If you find anything unusual, bring it here."
        )

        npc(
            npc,
            CALM_TALK,
            "Try the bushes - I've read enough adventure stories to know that clues get caught in bushes all the time."
        )

        npc(
            npc,
            CALM_TALK,
            "I will tell the guards to let you past - that way, you can just use the ladder to get in and out."
        )

    }, {
        player(
            CALM_TALK,
            "I'm not interested."
        )

        npc(
            npc,
            SAD,
            "That's typical, nowadays. It's left to us wizards to do all the work."
        )
    })
}

private fun DialogueBuilder.searchWatchtower(
    player: Player,
    npc: NPC
) {
    npc(
        npc,
        CALM_TALK,
        "Hello again. Did you find anything of interest?"
    )

    if (player.inventory.containsItem(Obj.fingernails, 1)) {
        player(
            CALM_TALK,
            "Have a look at these..."
        )

        npc(
            npc,
            CALM_TALK,
            "Interesting, very interesting. Long nails...grey in colour...well chewed... Of course! They belong to a skavid!"
        ) {
            if (player.getQuestStage(Quest.WATCHTOWER) == STAGE_SEARCH_WATCHTOWER &&
                player.inventory.containsItem(Obj.fingernails, 1)
            ) {
                player.inventory.deleteItem(Obj.fingernails, 1)
                player.setQuestStage(Quest.WATCHTOWER, STAGE_FIND_WAY_INTO_GUTANOTH)
            }
        }

        player(
            CALM_TALK,
            "A skavid?"
        )

        npc(
            npc,
            CALM_TALK,
            "A servant race to the ogres: grey, depressed-looking creatures, always losing nails, teeth and hair! They inhabit the caves in the Feldip Hills. They normally keep to themselves, though. It's unusual for them to venture from their caves."
        )

        fingernailOptions(player, npc)
        return
    }

    when {
        player.inventory.containsItem(Obj.watchtowerrobe, 1) -> {
            player(CALM_TALK, "I have this robe...")
            npc(npc, CALM_TALK, "Let me see...")
            npc(
                npc,
                CALM_TALK,
                "No, sorry, this is not evidence. You need to keep searching, I'm afraid."
            )
        }

        player.inventory.containsItem(Obj.watchtowerdagger, 1) -> {
            player(CALM_TALK, "I have this dagger...")
            npc(npc, CALM_TALK, "Let me see...")
            npc(
                npc,
                CALM_TALK,
                "No, sorry, this is not evidence. You need to keep searching, I'm afraid."
            )
        }

        player.inventory.containsItem(Obj.watchtowerarmour, 1) -> {
            player(CALM_TALK, "I have this armour...")
            npc(npc, CALM_TALK, "Let me see...")
            npc(
                npc,
                CALM_TALK,
                "No, sorry, this is not evidence. You need to keep searching, I'm afraid."
            )
        }

        player.inventory.containsItem(Obj.watch_eye_patch, 1) -> {
            player(CALM_TALK, "I have this eyepatch...")
            npc(npc, CALM_TALK, "Let me see...")
            npc(
                npc,
                CALM_TALK,
                "No, sorry, this is not evidence. You need to keep searching, I'm afraid."
            )
        }

        else -> {
            player(CALM_TALK, "No, sorry, nothing yet.")
            npc(
                npc,
                SAD,
                "Oh dear, oh dear, there must be something, somewhere..."
            )
        }
    }
}

private fun DialogueBuilder.fingernailOptions(
    player: Player,
    npc: NPC
) {
    options {
        op("What do you suggest I do?") {
            player(CALM_TALK, "What do you suggest I do?")
            skavidExplanation(player, npc)
        }

        op("Shall I search the caves?") {
            player(CALM_TALK, "Shall I search the caves?")
            skavidExplanation(player, npc)
        }
    }
}

private fun DialogueBuilder.skavidExplanation(
    player: Player,
    npc: NPC
) {
    npc(
        npc,
        CALM_TALK,
        "It's no good searching the caves. Well, not yet, anyway."
    )

    player(CALM_TALK, "Why not?")

    npc(
        npc,
        CALM_TALK,
        "They are deep and complex. The only way you will navigate the caves is to have a map or something. It may be that the ogres have one..."
    )

    player(CALM_TALK, "And how do you know that?")

    npc(npc, CALM_TALK, "Well... I don't.")

    options {
        op("So what do I do?") {
            player(CALM_TALK, "So what do I do?")

            npc(
                npc,
                CALM_TALK,
                "You need to be fearless and gain entrance to Gu'Tanoth, the city of the ogres, then find out how to navigate the caves."
            )

            player(WORRIED, "That sounds scary...")

            npc(
                npc,
                CALM_TALK,
                "Ogres are nasty creatures, yes. Only a strong warrior, and a clever one at that, can get the better of them."
            )

            player(
                CALM_TALK,
                "What do I need to do to get into the city?"
            )

            npc(
                npc,
                CALM_TALK,
                "Well, the guards need to be dealt with. You could start by checking out the ogre settlements found around here. Tribal ogres often dislike their neighbours..."
            )

            npc(
                npc,
                CALM_TALK,
                "In the meantime, I'll throw those fingernails out for you."
            )
        }

        op("I won't bother then.") {
            player(CALM_TALK, "I won't bother, then.")
            npc(
                npc,
                ANGRY,
                "Won't bother? Won't bother! Perhaps this quest is too hard for you?"
            )
        }
    }
}

private fun DialogueBuilder.findingGuTanoth(
    player: Player,
    npc: NPC
) {
    if (hasAnyRelicPart(player) || relicPartsHandedIn(player) == 3) {
        handleRelicParts(player, npc)
        return
    }

    npc(npc, CALM_TALK, "Hello again. How do you fare?")
    player(
        CALM_TALK,
        "I am still trying to find a way into Gu'Tanoth."
    )
    npc(
        npc,
        CALM_TALK,
        "You could start by checking out the ogre settlements found around here. Tribal ogres often dislike their neighbours..."
    )
}

private fun DialogueBuilder.ogreRelic(
    player: Player,
    npc: NPC
) {
    if (hasAnyRelicPart(player) || relicPartsHandedIn(player) == 3) {
        handleRelicParts(player, npc)
        return
    }

    npc(npc, CALM_TALK, "Hello again. How do you fare?")
    player(CALM_TALK, "I am still dealing with the ogres.")
    npc(
        npc,
        CALM_TALK,
        "Keep searching. Anything the ogres give you may prove useful."
    )
}

private fun DialogueBuilder.handleRelicParts(
    player: Player,
    npc: NPC
) {
    val heldParts = relicPartsHeld(player)
    val handedParts = relicPartsHandedIn(player)

    val hasAllParts = listOf(
        Obj.relicpart1 to RELIC_PART_1_HANDED_IN,
        Obj.relicpart2 to RELIC_PART_2_HANDED_IN,
        Obj.relicpart3 to RELIC_PART_3_HANDED_IN
    ).all { (itemId, attribute) -> player.inventory.containsItem(itemId, 1) || player.getBool(attribute) }

    if (hasAllParts) {
        if (handedParts > 0 && heldParts == 1) {
            player(CALM_TALK, "An ogre gave me this...")

            val lastPart = when {
                player.inventory.containsItem(Obj.relicpart1, 1) -> Obj.relicpart1
                player.inventory.containsItem(Obj.relicpart2, 1) -> Obj.relicpart2
                player.inventory.containsItem(Obj.relicpart3, 1) -> Obj.relicpart3
                else -> return
            }

            npc(
                npc,
                CALM_TALK,
                "Ah, it's part of an old ogre statue. I'll see if I can fix it up for you. It might come in handy. There may be more parts to find... I'll keep this for later."
            ) {
                handInRelicPart(player, lastPart)
            }
        } else {
            player(CALM_TALK, "An ogre gave me these...")
        }

        npc(
            npc,
            HAPPY_TALKING,
            "Excellent! That seems to be all the pieces. Now I can assemble it..."
        ) {
            consumeRelicParts(player)
        }

        simple("The Watchtower Wizard casts a spell.") {
            player.inventory.addItemDrop(Obj.ogrerelic, 1)
            player.setQuestStage(
                Quest.WATCHTOWER,
                STAGE_ENTER_GUTANOTH
            )
        }

        npc(
            npc,
            CALM_TALK,
            "Hmm, yes, it is as I thought... A statue symbolising an ogre warrior of old. Well, if you ever want to make friends with an ogre, this is the item to have!"
        )

        simple("The wizard gives you a complete statue.")
        return
    }

    val part = when {
        player.inventory.containsItem(Obj.relicpart1, 1) ->
            Obj.relicpart1

        player.inventory.containsItem(Obj.relicpart2, 1) ->
            Obj.relicpart2

        player.inventory.containsItem(Obj.relicpart3, 1) ->
            Obj.relicpart3

        else ->
            return
    }

    player(CALM_TALK, "An ogre gave me this...") {
        handInRelicPart(player, part)

        player.setQuestStage(
            Quest.WATCHTOWER,
            STAGE_COLLECT_OGRE_RELIC
        )
    }

    npc(
        npc,
        CALM_TALK,
        "Ah, it's part of an old ogre statue. I'll see if I can fix it up for you. It might come in handy. There may be more parts to find... I'll keep this for later."
    )
}

private fun DialogueBuilder.enterGuTanoth(
    player: Player,
    npc: NPC
) {
    npc(npc, CALM_TALK, "Hello again. How do you fare?")

    npc(
        npc,
        CALM_TALK,
        "That statue should help you make friends with the ogres. You need to gain entrance to Gu'Tanoth and find out how to navigate the skavid caves."
    ) {
        if (!player.inventory.containsItem(Obj.ogrerelic, 1) && !player.bank.containsItem(Obj.ogrerelic, 1)) {
            player.inventory.addItemDrop(Obj.ogrerelic, 1)
        }
    }
}

private fun DialogueBuilder.getSkavidMap(
    player: Player,
    npc: NPC
) {
    if (player.inventory.containsItem(Obj.skavidmap, 1)) {
        player(
            CALM_TALK,
            "I have worked out the guard's puzzle."
        )

        npc(
            npc,
            HAPPY_TALKING,
            "My, my. A wordsmith as well as a hero."
        ) {
            player.setQuestStage(
                Quest.WATCHTOWER,
                STAGE_LEARN_SKAVID_LANGUAGE
            )
        }

        skavidMapOptions(npc)
        return
    }

    npc(npc, CALM_TALK, "How is the quest going?")

    player(
        CALM_TALK,
        "I am still trying to find a way to navigate the skavid caves."
    )

    npc(
        npc,
        CALM_TALK,
        "The only way you will navigate the caves is to have a map or something. It may be that the ogres have one..."
    )
}

private fun DialogueBuilder.skavidMapProgress(
    player: Player,
    npc: NPC
) {
    npc(npc, CALM_TALK, "How is the quest going?")

    if (player.inventory.containsItem(Obj.skavidmap, 1)) {
        player(
            CALM_TALK,
            "I have worked out the guard's puzzle."
        )

        npc(
            npc,
            HAPPY_TALKING,
            "My, my. A wordsmith as well as a hero."
        )

        skavidMapOptions(npc)
    } else {
        player(
            CALM_TALK,
            "I am still trying to navigate the skavid caves."
        )

        npc(
            npc,
            CALM_TALK,
            "Take some illumination with you or else it will be dark."
        )
    }
}

private fun DialogueBuilder.skavidMapOptions(
    npc: NPC
) {
    options {
        op("I am still trying to navigate the skavid caves.") {
            player(
                CALM_TALK,
                "I am still trying to navigate the skavid caves."
            )

            npc(
                npc,
                CALM_TALK,
                "Take some illumination with you or else it will be dark."
            )
        }

        op("I am trying to get into the shamans' cave.") {
            player(
                CALM_TALK,
                "I am trying to get into the shamans' cave."
            )

            npc(
                npc,
                CALM_TALK,
                "Yes, it will be well guarded. Hmmm, let me see... Ah yes, I gather some ogres are allergic to certain herbs. Now what was it? It had white berries and blue leaves.... Cave nightshade, thats it! You should try looking through some of the caves."
            )

            player(CALM_TALK, "Alright!")

            npc(
                npc,
                CALM_TALK,
                "And don't forget to look out for the dragons!"
            )

            player(AMAZED, "The WHAT?")

            npc(npc, CALM_TALK, "Dragons.")

            npc(
                npc,
                CALM_TALK,
                "Didn't you know that the ogres keep blue dragons as pets and for magical ingredients? Well, regardless, I'm sure a bold, brave adventurer such as yourself won't have any problems."
            )

            player(WORRIED, "Great...")
        }

        op("It is going well.") {
            player(CALM_TALK, "It is going well.")

            npc(
                npc,
                HAPPY_TALKING,
                "That's good to hear. We are much closer to fixing the tower now."
            )
        }
    }
}

private fun DialogueBuilder.searchingSkavidCaves(
    player: Player,
    npc: NPC
) {
    npc(npc, CALM_TALK, "Hello again. How do you fare?")

    options {
        op("It goes well. I can now navigate the skavid caves.") {
            player(
                CALM_TALK,
                "It goes well. I can now navigate the skavid caves."
            )

            npc(
                npc,
                HAPPY_TALKING,
                "That is good news. Let me know if you find anything of interest."
            )
        }

        if (
            player.questManager.getAttribs(Quest.WATCHTOWER).getB(MAD_SKAVID_COMPLETE) &&
            !player.inventory.containsItem(Obj.powering_crystal2, 1)
        ) {
            op("I had a crystal but I lost it!") {
                player(
                    WORRIED,
                    "I had a crystal, but I lost it!"
                )

                npc(
                    npc,
                    SAD,
                    "Oh no. Well, you had better go back there, then!"
                )
            }
        }

        op("I am now ready for the shamans.") {
            player(
                CALM_TALK,
                "I am now ready for the shamans."
            )

            shamanCaveAdvice(npc)
        }
    }
}

private fun DialogueBuilder.findingShamanEnclave(
    player: Player,
    npc: NPC
) {
    npc(npc, CALM_TALK, "Hello again. How do you fare?")

    options {
        op("It goes well. I can now navigate the skavid caves.") {
            player(
                CALM_TALK,
                "It goes well. I can now navigate the skavid caves."
            )

            npc(
                npc,
                HAPPY_TALKING,
                "That is good news. Let me know if you find anything of interest."
            )
        }

        if (
            player.questManager.getAttribs(Quest.WATCHTOWER).getB(MAD_SKAVID_COMPLETE) &&
            !player.inventory.containsItem(Obj.powering_crystal2, 1)
        ) {
            op("I had a crystal but I lost it!") {
                player(
                    WORRIED,
                    "I had a crystal, but I lost it!"
                )

                npc(
                    npc,
                    SAD,
                    "Oh no. Well, you had better go back there, then!"
                )
            }
        }

        op("I am now ready for the shamans.") {
            player(
                CALM_TALK,
                "I am now ready for the shamans."
            )

            shamanCaveAdvice(npc)
        }
    }
}

private fun DialogueBuilder.shamanCaveAdvice(
    npc: NPC
) {
    npc(
        npc,
        CALM_TALK,
        "Remember all I told you. You must distract the guard somehow. Cave nightshade is what you are looking for - try searching the skavid caves for it."
    )

    npc(
        npc,
        CALM_TALK,
        "This herb is very poisonous, however, so handle it with care. Also, be on your guard in that cave. Who knows what monsters may be in that awful place..."
    )

    player(CALM_TALK, "Alright!")

    npc(
        npc,
        CALM_TALK,
        "And don't forget to look out for the dragons!"
    )

    player(AMAZED, "The WHAT?")

    npc(npc, CALM_TALK, "Dragons.")

    npc(
        npc,
        CALM_TALK,
        "Didn't you know that the ogres keep blue dragons as pets and for magical ingredients?"
    )

    npc(
        npc,
        CALM_TALK,
        "Well, regardless, I'm sure a bold, brave adventurer such as yourself won't have any problems."
    )

    player(WORRIED, "Great...")
}

private fun DialogueBuilder.discoveredShamans(
    player: Player,
    npc: NPC
) {
    player(
        CALM_TALK,
        "I have found the cave of ogre shaman, but I cannot touch them!"
    )

    npc(
        npc,
        CALM_TALK,
        "That is because of their magical powers. We must fight them with their own methods; do not speak to them! I suggest a potion..."
    )

    npc(
        npc,
        CALM_TALK,
        "Collect a guam leaf, add some jangerberries, then mix in some ground bat bones."
    )

    npc(
        npc,
        CALM_TALK,
        "It is essential to return it to me before you use it, so that I can empower it with my magic."
    )

    npc(
        npc,
        CALM_TALK,
        "Be very careful how you mix it as it is extremely volatile. Mixing ingredients of this type in the wrong order can cause explosions!"
    )

    npc(
        npc,
        CALM_TALK,
        "I hope you've been brushing up Herblore and Magic? I must warn you, only experienced magicians can use the potion. It is too dangerous in the hands of the unskilled."
    ) {
        player.setQuestStage(
            Quest.WATCHTOWER,
            STAGE_MAKE_OGRE_POTION
        )
    }
}

private fun DialogueBuilder.makeOgrePotion(
    player: Player,
    npc: NPC
) {
    if (player.inventory.containsItem(Obj.ogre_potion, 1)) {
        npc(npc, CALM_TALK, "Any more news?") {
            if (player.inventory.containsItem(Obj.ogre_potion, 1)) {
                player.inventory.deleteItem(Obj.ogre_potion, 1)
                player.inventory.addItemDrop(Obj.magic_ogre_potion, 1)
                player.setQuestStage(Quest.WATCHTOWER, STAGE_DEFEAT_SHAMANS)
            }
        }

        player(CALM_TALK, "I have made the potion.")

        npc(
            npc,
            HAPPY_TALKING,
            "That's great news; let me infuse it with magic..."
        )

        simple("The wizard mumbles strange words over the liquid...")

        npc(
            npc,
            CALM_TALK,
            "Here it is - a dangerous substance. I must remind you that this potion can only be used if your Magic level is high enough."
        )

        player(CALM_TALK, "How high is high enough?")

        npc(
            npc,
            CALM_TALK,
            "Well, if you can cast Bones to Bananas, or any spells beyond that, you should be alright."
        )

        return
    }

    npc(npc, CALM_TALK, "Any more news?")

    player(
        CALM_TALK,
        "Can you tell me again what I need for the potion?"
    )

    npc(
        npc,
        CALM_TALK,
        "Yes indeed, let me see, what was it again..."
    )

    if (player.inventory.containsItem(Obj.guam_leaf, 1)) {
        npc(npc, CALM_TALK, "You need a guam leaf.")
        player(CALM_TALK, "I have one right here.")
        npc(
            npc,
            HAPPY_TALKING,
            "Wonderful! You will need to add that first."
        )
    } else {
        npc(
            npc,
            CALM_TALK,
            "You will also need a guam leaf. That goes into the mix first."
        )

        player(CALM_TALK, "What is the final ingredient?")
    }

    if (player.inventory.containsItem(Obj.jangerberries, 1)) {
        npc(npc, CALM_TALK, "You need some jangerberries.")
        player(CALM_TALK, "I have some of those.")
        npc(npc, HAPPY_TALKING, "Wonderful!")

        npc(
            npc,
            CALM_TALK,
            "You will need to add those after the guam leaf has gone in, but before the other ingredient."
        )
    } else {
        npc(
            npc,
            CALM_TALK,
            "You will also need some jangerberries. Those go into the mix before the final ingredient."
        )
    }

    player(CALM_TALK, "What is the final ingredient?")

    npc(
        npc,
        CALM_TALK,
        "The final ingredient is ground bat bones."
    )

    when {
        player.inventory.containsItem(Obj.ground_bat_bones, 1) -> {
            player(
                CALM_TALK,
                "Are these ground up well enough?"
            )

            npc(
                npc,
                HAPPY_TALKING,
                "Yes, those will do nicely."
            )
        }

        player.inventory.containsItem(Obj.bat_bones, 1) -> {
            player(
                CALM_TALK,
                "I have some bat bones; how can I grind them?"
            )

            npc(
                npc,
                CALM_TALK,
                "A pestle and mortar will more than do the trick."
            )
        }
    }

    npc(
        npc,
        CALM_TALK,
        "When you have mixed the potion correctly, then I can empower the potion with magic and the ogre shamans can be destroyed."
    )
}

private fun DialogueBuilder.defeatShamans(
    player: Player,
    npc: NPC
) {
    if (player.inventory.containsItem(Obj.ogre_potion, 1)) {
        makeOgrePotion(player, npc)
        return
    }

    npc(
        npc,
        CALM_TALK,
        "Hello again. Did the potion work?"
    )

    player(
        CALM_TALK,
        "I am still working to rid us of the shamans..."
    )

    npc(
        npc,
        CALM_TALK,
        "May you have success in your task."
    )
}

private fun DialogueBuilder.recoverCrystals(
    player: Player,
    npc: NPC
) {
    if (hasAllWatchtowerCrystals(player)) {
        npc(
            npc,
            CALM_TALK,
            "Hello again. Did the potion work?"
        )

        player(
            HAPPY_TALKING,
            "Indeed it did! I wiped out those ogre shamans!"
        )

        npc(
            npc,
            HAPPY_TALKING,
            "Magnificent! At last you've brought all the crystals."
        )

        npc(
            npc,
            CALM_TALK,
            "Now the shield generator can be activated and, once again, Yanille will be safe from the threat of the ogres."
        )

        npc(
            npc,
            CALM_TALK,
            "Put the crystals on the pillars there and throw the lever to activate the system."
        ) {
            player.setQuestStage(
                Quest.WATCHTOWER,
                STAGE_RESTORE_WATCHTOWER
            )
        }

        return
    }

    npc(
        npc,
        CALM_TALK,
        "Hello again. Did the potion work?"
    )

    player(
        CALM_TALK,
        "Indeed it did! I wiped out those ogre shamans!"
    )

    npc(
        npc,
        CALM_TALK,
        "You still need to recover the crystals before the shield generator can be restored."
    )
}

private fun DialogueBuilder.restoreWatchtower(
    npc: NPC
) {
    npc(
        npc,
        CALM_TALK,
        "The system is not activated yet. Throw the switch to start it."
    )
}

private fun DialogueBuilder.activateWatchtower(
    npc: NPC
) {
    npc(
        npc,
        CALM_TALK,
        "The system is not activated yet. Throw the switch to start it."
    )
}

private fun DialogueBuilder.postQuest(
    player: Player,
    npc: NPC
) {
    npc(
        npc,
        HAPPY_TALKING,
        "Greetings, friend. I trust all is well with you? Yanille is safe at last!"
    )

    options {
        if (player.getBool(WATCHTOWER_SCROLL_READ)) {
            op("Do you have any more quests for me?") {
                player(
                    CALM_TALK,
                    "Do you have any more quests for me?"
                )

                npc(
                    npc,
                    CALM_TALK,
                    "More quests? No, indeed, adventurer. You have done us a great service, already."
                )
            }
        } else {
            op("I lost the scroll you gave me.") {
                player(
                    CALM_TALK,
                    "I lost the scroll you gave me."
                )

                if (player.inventory.containsItem(Obj.watchtowerspell, 1)) {
                    npc(
                        npc,
                        HAPPY_TALKING,
                        "Ho, ho, ho! A comedian to the finish. There it is, in your backpack!"
                    )
                } else if (!player.inventory.hasFreeSlots()) {
                    npc(
                        npc,
                        CALM_TALK,
                        "Never mind, have another..."
                    )

                    simple(
                        "You don't have enough inventory space to take another scroll."
                    )
                } else {
                    npc(
                        npc,
                        CALM_TALK,
                        "Never mind, have another..."
                    ) {
                        player.inventory.addItem(Obj.watchtowerspell, 1)
                    }
                }
            }
        }

        op("That's okay.") {
            player(
                CALM_TALK,
                "That's okay."
            )

            npc(
                npc,
                HAPPY_TALKING,
                "We are always in your debt; do come and visit us again."
            )
        }
    }
}

private fun hasAnyRelicPart(player: Player): Boolean {
    return player.inventory.containsItem(Obj.relicpart1, 1) ||
            player.inventory.containsItem(Obj.relicpart2, 1) ||
            player.inventory.containsItem(Obj.relicpart3, 1)
}

private fun relicPartsHeld(player: Player): Int {
    var count = 0

    if (player.inventory.containsItem(Obj.relicpart1, 1))
        count++

    if (player.inventory.containsItem(Obj.relicpart2, 1))
        count++

    if (player.inventory.containsItem(Obj.relicpart3, 1))
        count++

    return count
}

private fun relicPartsHandedIn(player: Player): Int {
    var count = 0

    if (player.getBool(RELIC_PART_1_HANDED_IN))
        count++

    if (player.getBool(RELIC_PART_2_HANDED_IN))
        count++

    if (player.getBool(RELIC_PART_3_HANDED_IN))
        count++

    return count
}

private fun handInRelicPart(
    player: Player,
    itemId: Int
) {
    player.inventory.deleteItem(itemId, 1)

    when (itemId) {
        Obj.relicpart1 ->
            player.set(RELIC_PART_1_HANDED_IN, true)

        Obj.relicpart2 ->
            player.set(RELIC_PART_2_HANDED_IN, true)

        Obj.relicpart3 ->
            player.set(RELIC_PART_3_HANDED_IN, true)
    }
}

private fun consumeRelicParts(player: Player) {
    if (player.inventory.containsItem(Obj.relicpart1, 1)) {
        player.inventory.deleteItem(Obj.relicpart1, 1)
        player.set(RELIC_PART_1_HANDED_IN, true)
    }

    if (player.inventory.containsItem(Obj.relicpart2, 1)) {
        player.inventory.deleteItem(Obj.relicpart2, 1)
        player.set(RELIC_PART_2_HANDED_IN, true)
    }

    if (player.inventory.containsItem(Obj.relicpart3, 1)) {
        player.inventory.deleteItem(Obj.relicpart3, 1)
        player.set(RELIC_PART_3_HANDED_IN, true)
    }
}

private fun hasAllWatchtowerCrystals(player: Player): Boolean {
    return player.inventory.containsItem(Obj.powering_crystal1, 1) &&
            player.inventory.containsItem(Obj.powering_crystal2, 1) &&
            player.inventory.containsItem(Obj.powering_crystal3, 1) &&
            player.inventory.containsItem(Obj.powering_crystal4, 1)
}
