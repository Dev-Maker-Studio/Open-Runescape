package com.rs.game.content.quests.murdermystery

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private val QUEST_EVIDENCE_ITEMS = intArrayOf(Obj.murderweapon, Obj.murderweapondust, Obj.murderpot2, Obj.murderthreadr, Obj.murderthreadg, Obj.murderthreadb, Obj.murdernecklace, Obj.murdernecklacedust, Obj.murdercup, Obj.murdercupdust, Obj.murderbottle, Obj.murderbottledust, Obj.murderbook, Obj.murderbookdust, Obj.murderneedle, Obj.murderneedledust, Obj.murderpot, Obj.murderpotdust, Obj.murderpaper, Obj.murderfingerprint, Obj.murderfingerprinta, Obj.murderfingerprintb, Obj.murderfingerprintc, Obj.murderfingerprintd, Obj.murderfingerprinte, Obj.murderfingerprintf, Obj.murderfingerprint1)

@ServerStartupEvent
fun mapMurderMysteryGuard() {
    onNpcClick(Npc.murderguard, options = arrayOf("Talk-to")) { (player) -> murderMysteryGuardDialogue(player) }
}

private fun murderMysteryGuardDialogue(player: Player) {
    player.startConversation {
        when (player.getQuestStage(Quest.MURDER_MYSTERY)) {
            STAGE_UNSTARTED -> startMurderMystery(player)
            STAGE_COMPLETE -> postQuestGuardDialogue()
            else -> investigationGuardDialogue(player)
        }
    }
}

private fun DialogueBuilder.startMurderMystery(player: Player) {
    player(CALM_TALK, "What's going on here?")
    npc(Npc.murderguard, SAD, "Oh, it's terrible! Lord Sinclair has been murdered and we don't have any clues as to who or why. We're totally baffled!")
    npc(Npc.murderguard, CALM_TALK, "If you can help us we will be very grateful.")
    questStart(Quest.MURDER_MYSTERY)
    options {
        op("Sure, I'll help!") {
            player(CALM_TALK, "Sure, I'll help!")
            npc(Npc.murderguard, HAPPY_TALKING, "Thanks a lot!")
            player(CALM_TALK, "What should I be doing to help?")
            npc(Npc.murderguard, CALM_TALK, "Look around and investigate who might be responsible. The Sarge said every murder leaves clues to who done it, but frankly we're out of our depth here.") {
                clearQuestState(player)
                ensureMurderer(player)
                player.setQuestStage(Quest.MURDER_MYSTERY, STAGE_SOMETHINGS_AMISS)
                syncMurderSceneEvidence(player)
            }
        }
        op("You should do your own dirty work.") {
            player(CALM_TALK, "You should do your own dirty work.")
            npc(Npc.murderguard, ANGRY, "Get lost then, this is private property! ...Unless you'd like to be taken in for questioning yourself?")
        }
    }
}

private fun DialogueBuilder.investigationGuardDialogue(player: Player) {
    options {
        op("What should I be doing to help again?") {
            player(CALM_TALK, "What should I be doing to help?")
            npc(Npc.murderguard, CALM_TALK, "Look around and investigate who might be responsible. The Sarge said every murder leaves clues to who done it, but frankly we're out of our depth here.")
        }
        op("How did Lord Sinclair die?") {
            player(CALM_TALK, "How did Lord Sinclair die?")
            npc(Npc.murderguard, CALM_TALK, "Well, it's all very mysterious. Mary, the maid, found the body in the study next to his bedroom on the east wing of the ground floor.")
            npc(Npc.murderguard, CALM_TALK, "The door was found locked from the inside, and he seemed to have been stabbed, but there was an odd smell in the room. Frankly, I'm stumped.")
        }
        op("I know who did it!") {
            player(CALM_TALK, "I know who did it!")
            if (missingMurderMysteryInterviews(player).isNotEmpty())
                npc(Npc.murderguard, CALM_TALK, "You still need to question everyone before we can close this case. Speak to ${missingMurderMysteryInterviews(player).joinToString(", ")}.")
            else if (hasMurderMysteryEvidence(player))
                conclusiveEvidence(player)
            else if (hasAnyEvidence(player))
                partialEvidence(player)
            else
                accusationDialogue()
        }
    }
}

private fun DialogueBuilder.partialEvidence(player: Player) {
    options {
        if (hasThreadEvidence(player)) {
            op("I have proof that it wasn't any of the servants.") {
                player(CALM_TALK, "I have proof that it wasn't any of the servants!")
                player(CALM_TALK, "All the servants dress in black so it couldn't have been one of them.")
                npc(Npc.murderguard, CALM_TALK, "That's some good work there. I guess it wasn't a servant. You still haven't proved who did do it though.")
            }
        }
        if (player.getBool(MM_POISON_EVIDENCE_FOUND)) {
            op("I have proof one of the family lied about the poison.") {
                val murderer = ensureMurderer(player)
                val name = murdererName(murderer)
                player(CALM_TALK, "I have proof that $name is lying about the poison.")
                npc(Npc.murderguard, CALM_TALK, "Oh really? How did you get that?")
                when (murderer) {
                    MM_MURDERER_ANNA -> simple("You tell the guard about the compost heap.")
                    MM_MURDERER_BOB -> simple("You tell the guard about the beehive.")
                    MM_MURDERER_CAROL -> simple("You tell the guard about the drain.")
                    MM_MURDERER_DAVID -> simple("You tell the guard about the spiders' nest.")
                    MM_MURDERER_ELIZABETH -> simple("You tell the guard about the mosquitos at the fountain.")
                    MM_MURDERER_FRANK -> simple("You tell the guard about the family crest.")
                }
                npc(Npc.murderguard, CALM_TALK, "Hmm. That's some good detective work there. We need more evidence before we can close the case though. Keep up the good work!")
            }
        }
        if (player.inventory.containsItem(Obj.murderfingerprint, 1)) {
            op("I have the finger prints of the culprit.") {
                val name = murdererName(ensureMurderer(player))
                player(CALM_TALK, "I have the finger prints of the culprit!")
                player(CALM_TALK, "I have $name's finger prints here. You can see for yourself they match the finger prints on the murder weapon exactly.")
                npc(Npc.murderguard, AMAZED, "... I'm impressed. How on earth did you think of something like that? I've never heard of such a technique for finding criminals before!")
                npc(Npc.murderguard, CALM_TALK, "This will come in very handy in the future but we can't arrest someone on just this. I'm afraid you'll still need to find more evidence before we can close this case completely.")
            }
        }
    }
}

private fun DialogueBuilder.conclusiveEvidence(player: Player) {
    val murderer = ensureMurderer(player)
    val name = murdererName(murderer)
    player(CALM_TALK, "I have conclusive proof who the killer was.")
    npc(Npc.murderguard, HAPPY_TALKING, "You do? That's excellent work. Lets hear it then.")
    player(CALM_TALK, "I don't think it was an intruder, and I don't think Lord Sinclair was killed by being stabbed.")
    npc(Npc.murderguard, CONFUSED, "Hmmm? Really? Why not?")
    player(CALM_TALK, "Nobody heard the guard dog barking, which it would have if it had been an intruder who was responsible.")
    player(CALM_TALK, "Nobody heard any signs of a struggle either. I think the knife was there to throw suspicion away from the real culprit.")
    npc(Npc.murderguard, CALM_TALK, "Yes, that makes sense. But who did do it then?")
    simple("You prove to the guard the thread matches $name's clothes.")
    npc(Npc.murderguard, CALM_TALK, "Yes, I'd have to agree with that... but we need more evidence!")
    simple(poisonProofText(murderer))
    npc(Npc.murderguard, HAPPY_TALKING, "Excellent work - have you considered a career as a detective? But I'm afraid it's still not quite enough...")
    simple("You match $name's finger prints with those on the dagger found in the body of Lord Sinclair.")
    npc(Npc.murderguard, HAPPY_TALKING, "Yes. There's no doubt about it. It must have been $name who killed ${if (murderer == MM_MURDERER_ANNA || murderer == MM_MURDERER_CAROL || murderer == MM_MURDERER_ELIZABETH) "her" else "his"} father. All of the guards must congratulate you on your excellent work in helping us to solve this case.")
    val pronoun = if (murderer == MM_MURDERER_ANNA ||
        murderer == MM_MURDERER_CAROL ||
        murderer == MM_MURDERER_ELIZABETH
    ) "her" else "him"
    npc(Npc.murderguard, CALM_TALK, "We don't have many murders here in Gielinor and I'm afraid we wouldn't have been able to solve it by ourselves. We will hold $pronoun here under house arrest until such time as we bring $pronoun to trial.")
    npc(Npc.murderguard, CALM_TALK, "You have our gratitude, and I'm sure the rest of the family's as well, in helping to apprehend the murderer. I'll just take the evidence from you now.")
    simple("You hand over all the evidence.")
    npc(Npc.murderguard, HAPPY_TALKING, "Please accept this reward from the family!") {
        if (hasMurderMysteryEvidence(player)) {
            removeMurderSceneEvidence(player)
            removeQuestEvidence(player)
            clearQuestState(player)
            player.questManager.completeQuest(Quest.MURDER_MYSTERY)
        } else if (murderMysteryStarted(player)) {
            player.sendMessage("You must finish questioning everyone and bring all the evidence before the case can be closed.")
        }
    }
}

private fun DialogueBuilder.accusationDialogue() {
    npc(Npc.murderguard, CALM_TALK, "Really? That was quick work! Who?")
    options {
        op("It was an intruder!") {
            player(CALM_TALK, "It was an intruder!")
            npc(Npc.murderguard, CALM_TALK, "That's what we were thinking too. That someone broke in to steal something, was discovered by Lord Sinclair, stabbed him and ran.")
            npc(Npc.murderguard, CALM_TALK, "It's odd that apparently nothing was stolen though... Find out something has been stolen,")
            npc(Npc.murderguard, CALM_TALK, "and the case is closed, but the murdered man was a friend of the King, and it's more than my job's worth not to investigate fully.")
        }
        op("The butler did it!") {
            player(CALM_TALK, "The butler did it!")
            npc(Npc.murderguard, CALM_TALK, "I hope you have proof to that effect. We have to arrest someone for this and it seems to me that only the actual murderer would gain by falsely accusing someone.")
            npc(Npc.murderguard, CALM_TALK, "Although having said that the butler is kind of shifty looking...")
        }
        op("It was one of the servants!") {
            player(CALM_TALK, "It was one of the servants!")
            npc(Npc.murderguard, CALM_TALK, "Oh really? Which one?")
            options {
                op("It was one of the women...") {
                    player(CALM_TALK, "It was one of the women")
                    npc(Npc.murderguard, CALM_TALK, "Oh really? Which one?")
                    options {
                        op("It was SO obviously Louisa the cook.") { unsupportedAccusation() }
                        op("It MUST have been Mary the maid.") { unsupportedAccusation() }
                    }
                }
                op("It was one of the men...") {
                    player(CALM_TALK, "It was one of the men...")
                    npc(Npc.murderguard, CALM_TALK, "Oh really? Which one?")
                    options {
                        op("It can ONLY be Donovan the handyman.") { unsupportedAccusation() }
                        op("Pierre the dog handler. No question.") { unsupportedAccusation() }
                        op("Hobbes the butler. The butler ALWAYS did it.") { unsupportedAccusation() }
                        op("You MUST know it was Stanford The Gardener...") { unsupportedAccusation() }
                    }
                }
            }
        }
        op("It was one of his family!") {
            player(CALM_TALK, "It was one of his family!")
            npc(Npc.murderguard, CALM_TALK, "Oh really? Which one?")
            options {
                op("It was one of the women...") {
                    player(CALM_TALK, "It was one of the women...")
                    npc(Npc.murderguard, CALM_TALK, "Oh really? Which one?")
                    options {
                        op("I KNOW it was Anna.") { unsupportedAccusation() }
                        op("I am SO sure it was Carol.") { unsupportedAccusation() }
                        op("I'll bet you ANYTHING it was Elizabeth.") { unsupportedAccusation() }
                    }
                }
                op("It was one of the men...") {
                    player(CALM_TALK, "It was one of the men...")
                    npc(Npc.murderguard, CALM_TALK, "Oh really? Which one?")
                    options {
                        op("I'm certain it was Bob.") { unsupportedAccusation() }
                        op("It was David. No doubt about it.") { unsupportedAccusation() }
                        op("If it wasn't Frank I'll eat my shoes!") { unsupportedAccusation() }
                    }
                }
            }
        }
    }
}

private fun DialogueBuilder.unsupportedAccusation() {
    simple("You tell the guard who you suspect of the crime.")
    npc(Npc.murderguard, CALM_TALK, "Great work. Show me the evidence, and we'll take them to the dungeons.")
    npc(Npc.murderguard, CALM_TALK, "You DO have evidence of their crime, right?")
    player(CALM_TALK, "Uh....")
    npc(Npc.murderguard, CALM_TALK, "Tch. You wouldn't last a day in the guards with sloppy thinking like that. Come see me when you have some proof of your accusations.")
}

private fun DialogueBuilder.postQuestGuardDialogue() {
    npc(Npc.murderguard, HAPPY_TALKING, "Excellent work on solving the murder! All of the guards I know are very impressed, and don't worry, we have the murderer under guard until they can be taken to trial.")
    player(CALM_TALK, "Is there anything else I can do? Seems awfully quiet up at the house, considering their sibling has just been arrested.")
    npc(Npc.murderguard, CALM_TALK, "Nothing right now, we have it all under control.")
}

private fun hasAnyEvidence(player: Player): Boolean =
    hasThreadEvidence(player) ||
        player.getBool(MM_POISON_EVIDENCE_FOUND) ||
        player.inventory.containsItem(Obj.murderfingerprint, 1)

private fun hasThreadEvidence(player: Player): Boolean =
    player.inventory.containsItem(criminalThreadFor(player), 1)

private fun murdererName(murderer: Int): String = when (murderer) {
    MM_MURDERER_ANNA -> "Anna"
    MM_MURDERER_BOB -> "Bob"
    MM_MURDERER_CAROL -> "Carol"
    MM_MURDERER_DAVID -> "David"
    MM_MURDERER_ELIZABETH -> "Elizabeth"
    MM_MURDERER_FRANK -> "Frank"
    else -> error("Invalid Murder Mystery murderer state.")
}

private fun poisonProofText(murderer: Int): String = when (murderer) {
    MM_MURDERER_ANNA -> "You prove to the guard Anna did not use poison on the compost heap."
    MM_MURDERER_BOB -> "You prove to the guard Bob did not use poison on the beehive."
    MM_MURDERER_CAROL -> "You prove to the guard Carol did not use poison on the drain."
    MM_MURDERER_DAVID -> "You prove to the guard David did not use poison on the spiders nest."
    MM_MURDERER_ELIZABETH -> "You prove to the guard Elizabeth did not use poison on the fountain."
    MM_MURDERER_FRANK -> "You prove to the guard Frank did not use poison on the Sinclair Crest."
    else -> error("Invalid Murder Mystery murderer state.")
}

private fun clearQuestState(player: Player) {
    player.delete(MM_INTERVIEWS_KEY)
    player.delete(MM_POISON_CLAIMS_KEY)
    player.delete(MM_MURDERER_KEY)
    player.delete(MM_THREAD_TAKEN_KEY)
    player.delete(MM_POISON_EVIDENCE_FOUND)
    player.delete(MM_POISON_BUYERS_KNOWN)
}

private fun removeQuestEvidence(player: Player) {
    QUEST_EVIDENCE_ITEMS.forEach { itemId ->
        while (player.inventory.containsItem(itemId, 1))
            player.inventory.deleteItem(itemId, 1)
    }
}
