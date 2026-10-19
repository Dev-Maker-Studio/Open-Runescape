package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick

private const val TEMP_UNDERGROUND_NARNODE = "grandTreeUndergroundNarnode"

internal val UNDERGROUND_NARNODE = Tile.of(2464, 9897, 0)

private val INCORRECT_TRANSLATIONS = listOf(
    "King Narnode must be stopped, he is a madman!",
    "Praise be to the great Zamorak!",
    "Do you have any bread? I do like bread.",
    "The time has come to attack!",
    "The tree is fine, you have nothing to fear.",
    "You must come and see me!",
    "The tree needs watering as there has been drought.",
    "Grave danger lies ahead, only the bravest will linger.",
    "Time is of the essence! We must move quickly.",
    "There is no need for haste, just send a runner.",
    "You must act now or we will all die!",
    "Time passes us by.",
    "Only Adamantite is any use.",
    "You must use force!",
    "Use a bucket of milk from a sacred cow.",
    "Take this banana to him, he will understand.",
    "All will be fine on the third night.",
    "You must wait till the second night.",
    "Nothing will help us now!"
)

@ServerStartupEvent
fun mapKingNarnodeGrandTree() {
    onNpcClick(Npc.grandtree_narnode, options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc is OwnedNPC && npc.owner !== player)
            return@onNpcClick
        player.startConversation {
            options {
                op("Talk about The Grand Tree.") { exec { kingNarnodeGrandTreeDialogue(player, npc) } }
                op("Nothing, thanks.") {
                    player(HeadE.CALM_TALK, "Nothing, thanks.")
                    npc(npc, HeadE.HAPPY_TALKING, "Enjoy your stay with us. There are many things to see in my kingdom.")
                }
            }
        }
    }
    onLogin { (player) ->
        if (player.isInGrandTreeDungeon() &&
            (player.grandTreeStage in STAGE_DEMON_DEFEATED..STAGE_COMPLETE || player.grandTreeStage == STAGE_STARTED))
            spawnUndergroundNarnode(player)
    }
}

internal fun spawnUndergroundNarnode(player: Player): NPC {
    val existing = player.tempAttribs.getO<OwnedNPC>(TEMP_UNDERGROUND_NARNODE)
    if (existing != null && !existing.hasFinished())
        return existing
    return object : OwnedNPC(player, Npc.grandtree_narnode, UNDERGROUND_NARNODE, true) {
        override fun processNPC() {
            if (!owner.isInGrandTreeDungeon()) {
                finish()
                return
            }
            super.processNPC()
        }
    }.also {
        it.isAutoDespawnAtDistance = false
        it.setRandomWalk(false)
        player.tempAttribs.setO<OwnedNPC>(TEMP_UNDERGROUND_NARNODE, it)
    }
}

private fun kingNarnodeGrandTreeDialogue(player: Player, npc: NPC) {
    when (player.grandTreeStage) {
        STAGE_UNSTARTED -> startQuestDialogue(player, npc)
        STAGE_STARTED -> startedReminder(player, npc)
        STAGE_HAZELMERE_MESSAGE -> translateHazelmeresMessage(player, npc)
        STAGE_DACONIA_WARNING -> giveGloughInstructions(player, npc)
        STAGE_FIND_GLOUGH -> remindAboutGlough(player, npc)
        STAGE_GLOUGH_BLAMES_HUMANS -> reportGloughsCapture(player, npc)
        STAGE_FIND_CHARLIE -> remindAboutCharlie(player, npc)
        STAGE_SEARCH_GLOUGHS_HOME -> kingDismissesSuspicions(player, npc)
        STAGE_ESCAPED_PRISON -> prisonEscapeReminder(player, npc)
        STAGE_REACHED_SHIPYARD -> kingWhileBanished(player, npc)
        STAGE_LUMBER_ORDER -> reportShipyardPlot(player, npc)
        STAGE_GLOUGHS_KEY -> kingRejectsFleetAccusation(player, npc)
        STAGE_INVASION_PLANS -> exchangePlansForTwigs(player, npc)
        STAGE_OPENED_TRAPDOOR -> kingBeforeDemonIsDefeated(player, npc)
        STAGE_DEMON_DEFEATED -> exposeGlough(player, npc)
        STAGE_FIND_DACONIA -> daconiaReminder(player, npc)
        STAGE_DACONIA_FOUND -> completeGrandTree(player, npc)
        STAGE_COMPLETE -> postQuestDialogue(player, npc)
    }
}

private fun startQuestDialogue(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.HAPPY_TALKING, "Welcome Traveller. I am King Narnode. It's nice to see an outsider.")
        player(HeadE.HAPPY_TALKING, "Hi! It seems to be a busy settlement.")
        npc(npc, HeadE.WORRIED, "For now.")
        options {
            op("You seem worried, what's up?") {
                player(HeadE.CONFUSED, "You seem worried, what's up?")
                npc(npc, HeadE.WORRIED, "Traveller, can I speak to you in strictest confidence?")
                player(HeadE.CALM_TALK, "Of course sire.")
                npc(npc, HeadE.CALM_TALK, "Not here, follow me.")
                npc(npc, HeadE.CALM_TALK, "Down here.") {
                    player.fadeScreen {
                        player.tele(UNDERGROUND_NARNODE.transform(1, 0))
                        spawnUndergroundNarnode(player)
                    }
                }
                player(HeadE.CONFUSED, "So what is this place?")
                npc(npc, HeadE.CALM_TALK, "These, my friend, are the foundations of the stronghold.")
                player(HeadE.CONFUSED, "They look like roots to me.")
                npc(npc, HeadE.HAPPY_TALKING, "Not just any roots Traveller! These were created by gnome mages eons ago, since then they have grown to become a mighty stronghold!")
                player(HeadE.AMAZED_MILD, "Impressive. What exactly is the problem?")
                npc(npc, HeadE.WORRIED, "In the last two months our tree guardians have reported continuing deterioration of the Grand Tree's health. I've never seen this before! It could be the end for us all!")
                player(HeadE.CONFUSED, "You mean the tree is ill?")
                npc(npc, HeadE.WORRIED, "In effect yes. Would you be willing to help us discover what is happening to the tree?")
                if (!Quest.GRAND_TREE.meetsReqs(player, "before you can help the king.")) {
                    simple("You do not meet the requirements to start: 'The Grand Tree'.")
                } else {
                    questStart(Quest.GRAND_TREE, accepted = {
                        exec { player.setGrandTreeStage(STAGE_STARTED) }
                        player(HeadE.HAPPY_TALKING, "I'd be happy to help!")
                        npc(npc, HeadE.HAPPY_TALKING, "Thank Guthix for your arrival!")
                        if (player.inventory.freeSlots < 2) {
                            npc(npc, HeadE.WORRIED, "Hmm. I'll need to give you a couple of items, but your backpack's too full. Put a couple of things down and come back to me.") {
                                player.setGrandTreeStage(STAGE_STARTED)
                            }
                        } else {
                            npc(npc, HeadE.CALM_TALK, "The first task is to find out what's killing the tree.")
                            player(HeadE.CONFUSED, "Do you have an idea?")
                            npc(npc, HeadE.CALM_TALK, "My top tree guardian, Glough, believes it's human sabotage. I'm not so sure! The only way to know for sure is to talk to Hazelmere.")
                            player(HeadE.CONFUSED, "Who's Hazelmere?")
                            npc(npc, HeadE.CALM_TALK, "Hazelmere is one of the mages that created the Grand Tree! He is the only one that has survived from that time. Take this bark sample to him, he will be able to help!")
                            item(Obj.grandtree_barksample, "The king shows you a sample of bark.")
                            npc(npc, HeadE.CALM_TALK, "The mage only talks in the old tongue, you'll need this.")
                            item(Obj.grandtree_translationbook, "The king shows you a translation book.")
                            player(HeadE.CONFUSED, "What is it?")
                            npc(npc, HeadE.CALM_TALK, "It's a translation book, you'll need it to translate what Hazelmere says. Do this carefully! His words are our only hope!")
                            npc(npc, HeadE.CALM_TALK, "You'll find his dwellings high upon a towering hill, on an island east of Yanille. Please make haste.") {
                                player.inventory.addItem(Obj.grandtree_barksample)
                                player.inventory.addItem(Obj.grandtree_translationbook)
                                player.setGrandTreeStage(STAGE_STARTED)
                            }
                        }
                    }, declined = {
                        player(HeadE.CALM_TALK, "I'll be off now.")
                    })
                }
            }
            op("I'll be off now.") {
                player(HeadE.CALM_TALK, "I'll be off now.")
                npc(npc, HeadE.HAPPY_TALKING, "Enjoy your stay with us. There are many things to see in my kingdom.")
            }
        }
    }
}

private fun startedReminder(player: Player, npc: NPC) {
    val missing = listOf(Obj.grandtree_barksample, Obj.grandtree_translationbook).count { !player.inventory.containsOneItem(it) }
    if (player.inventory.freeSlots < missing) {
        player.sendMessage("You need $missing free inventory spaces for the bark sample and translation book.")
        return
    }
    player.startConversation {
        npc(npc, HeadE.WORRIED, "Traveller, any word from Hazelmere?")
        player(HeadE.SAD_MILD_LOOK_DOWN, "Not yet.")
        if (!player.inventory.containsOneItem(Obj.grandtree_barksample)) {
            player(HeadE.SAD_MILD_LOOK_DOWN, "I've lost the bark sample.")
            if (!player.inventory.hasFreeSlots())
                npc(npc, HeadE.WORRIED, "Hmm. Looks like you can't carry anymore. Put something down and come back to me.")
            else
                npc(npc, HeadE.CALM_TALK, "Here's another sample, hang on to it this time!") { player.inventory.addItem(Obj.grandtree_barksample) }
        }
        if (!player.inventory.containsOneItem(Obj.grandtree_translationbook)) {
            player(HeadE.SAD_MILD_LOOK_DOWN, "I've lost the translation book.")
            if (!player.inventory.hasFreeSlots())
                npc(npc, HeadE.WORRIED, "Hmm. Looks like you can't carry anymore. Put something down and come back to me.")
            else
                npc(npc, HeadE.CALM_TALK, "Don't worry, I have more.") { player.inventory.addItem(Obj.grandtree_translationbook) }
        }
        npc(npc, HeadE.CALM_TALK, "Hazelmere lives on an island just south of the Khazard Fight Arena. Give him the sample and translate his reply.")
        npc(npc, HeadE.WORRIED, "I just hope he can help us in our hour of need!")
    }
}

private fun translateHazelmeresMessage(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello again, Your Highness.")
        npc(npc, HeadE.HAPPY_TALKING, "Hello Traveller, did you speak to Hazelmere?")
        player(HeadE.HAPPY_TALKING, "Yes! I managed to find him.")
        npc(npc, HeadE.CONFUSED, "Do you understand what he said?")
        options {
            op("I think so!") {
                player(HeadE.HAPPY_TALKING, "I think so!")
                npc(npc, HeadE.CALM_TALK, "So what did he say?")
                exec { translationPageOne(player, npc) }
            }
            op("No, I need to go back.") {
                player(HeadE.SAD_MILD_LOOK_DOWN, "No, I need to go back.")
                if (!player.inventory.containsOneItem(Obj.grandtree_scroll)) {
                    player(HeadE.SAD_MILD_LOOK_DOWN, "I've lost the bark sample.")
                    if (player.inventory.hasFreeSlots()) {
                        npc(npc, HeadE.CALM_TALK, "Here's another sample, hang on to it this time!") { player.inventory.addItem(Obj.grandtree_barksample) }
                    }
                }
                npc(npc, HeadE.WORRIED, "Time is of the essence Traveller!")
            }
        }
    }
}

private fun translationPageOne(player: Player, npc: NPC) {
    translationOptions(player, npc, "A man came to me with the King's seal.") {
        translationPageTwo(player, npc)
    }
}

private fun translationPageTwo(player: Player, npc: NPC) {
    translationOptions(player, npc, "I gave the man Daconia rocks.") {
        translationPageThree(player, npc)
    }
}

private fun translationPageThree(player: Player, npc: NPC) {
    translationOptions(player, npc, "And Daconia rocks will kill the tree!") {
        successfulTranslation(player, npc)
    }
}

private fun translationOptions(player: Player, npc: NPC, correctLine: String, next: () -> Unit) {
    val lines = (INCORRECT_TRANSLATIONS.shuffled().take(3) + correctLine).shuffled()
    player.startConversation {
        options {
            lines.forEach { line ->
                op(line) {
                    player(HeadE.CALM_TALK, line)
                    exec {
                        if (line == correctLine)
                            next()
                        else
                            failedTranslation(player, npc)
                    }
                }
            }
        }
    }
}

private fun failedTranslation(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.CONFUSED, "Wait a minute, that doesn't sound like Hazelmere! Are you sure you translated correctly?")
        player(HeadE.NERVOUS, "Erm...I think so.")
        npc(npc, HeadE.WORRIED, "I'm sorry Traveller but this is no good. The translation must be perfect or the infomation's no use. Please come back when you know exactly what Hazelmere said.")
    }
}

private fun successfulTranslation(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.AMAZED_MILD, "Of course! I should've known! Someone must've forged my royal seal. Hazelmere thought I sent him for the Daconia stones!")
        player(HeadE.CONFUSED, "What are Daconia stones?")
        npc(npc, HeadE.CALM_TALK, "Hazelmere created the Daconia stones. They are a safety measure, in case the tree grew out of control. They're the only thing that can kill the tree.")
        npc(npc, HeadE.WORRIED, "This is terrible! The stones must be recovered!")
        player(HeadE.CALM_TALK, "Can I help?")
        npc(npc, HeadE.CALM_TALK, "First I must warn the tree guardians. Please, could you tell the chief tree guardian Glough. He lives in a tree house just in front of the Grand Tree.")
        npc(npc, HeadE.CALM_TALK, "If he's not there he will be at his girlfriend Anita's place. Meet me back here once you've told him.")
        player(HeadE.HAPPY_TALKING, "OK! I'll be back soon.") {
            player.inventory.deleteItem(Obj.grandtree_scroll, 1)
            player.setGrandTreeStage(STAGE_DACONIA_WARNING)
        }
    }
}

private fun giveGloughInstructions(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.CALM_TALK, "Please tell the chief tree guardian Glough that the Daconia rocks have been taken.")
        npc(npc, HeadE.CALM_TALK, "He lives in a tree house just in front of the Grand Tree.") { player.setGrandTreeStage(STAGE_FIND_GLOUGH) }
    }
}

private fun remindAboutGlough(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.CALM_TALK, "Hello Traveller, did you speak to Glough?")
        player(HeadE.SAD_MILD_LOOK_DOWN, "Not yet.")
        npc(npc, HeadE.CALM_TALK, "OK. He lives just in front of the Grand Tree. Let me know when you've talked to him.")
    }
}

private fun reportGloughsCapture(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello, Your Highness. Have you any news on the Daconia stones?")
        npc(npc, HeadE.HAPPY_TALKING, "It's OK Traveller, thanks to Glough! He found a human sneaking around! He had three Daconia rocks on him!")
        player(HeadE.AMAZED_MILD, "Wow! That was quick!")
        npc(npc, HeadE.HAPPY_TALKING, "Yes Glough really knows what he's doing. The human has been detained until we know who else is involved.")
        npc(npc, HeadE.WORRIED, "Maybe Glough was right, maybe humans are invading!")
        player(HeadE.SKEPTICAL, "I doubt it, can I speak to the prisoner?")
        npc(npc, HeadE.CALM_TALK, "Certainly. He's on the top level of the tree. Be careful, it's a long way down!") {
            player.setGrandTreeStage(STAGE_FIND_CHARLIE)
        }
    }
}

private fun remindAboutCharlie(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.CALM_TALK, "Hello Traveller. If you wish to talk to the prisoner go to the top of the tree, you'll find him there.")
        player(HeadE.HAPPY_TALKING, "Thanks.")
    }
}

private fun kingDismissesSuspicions(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.WORRIED, "Hello Traveller. So, did you speak to the culprit?")
        player(HeadE.WORRIED, "Yes I did and something's not right!")
        npc(npc, HeadE.CONFUSED, "What do you mean?")
        player(HeadE.WORRIED, "The prisoner said he was paid by Glough to get the Daconia stones!")
        npc(npc, HeadE.SKEPTICAL, "That's absurd! He's just trying to save himself!")
        npc(npc, HeadE.CALM_TALK, "Since Glough's wife died he's been a little strange. He would never wrongly imprison someone though! Now that the culprit is locked up we can relax. It's sad but I think Glough was right.")
        npc(npc, HeadE.WORRIED, "Humans are planning to invade and wipe out the tree gnomes!")
        player(HeadE.CONFUSED, "But why?")
        npc(npc, HeadE.WORRIED, "Who knows? You may have to leave soon Traveller! I trust you but the local gnomes are getting paranoid.")
    }
}

private fun prisonEscapeReminder(player: Player, npc: NPC) {
    player.startConversation { npc(npc, HeadE.WORRIED, "Glough is looking for you! Leave on the glider now before it's too late!") }
}

private fun kingWhileBanished(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.WORRIED, "King Narnode, I need to talk!")
        npc(npc, HeadE.WORRIED, "Traveller, what are you doing here? The stronghold has been put on full alert! It's not safe for you here!")
        player(HeadE.WORRIED, "Your Highness, I believe Glough is killing the trees in order to make a mass fleet of warships!")
        npc(npc, HeadE.SKEPTICAL, "That's an absurd accusation!")
        player(HeadE.WORRIED, "His hatred for humanity is stronger than you know!")
        npc(npc, HeadE.FRUSTRATED, "That's enough Traveller, you sound as paranoid as him! Traveller please leave! It's bad enough having one human locked up.")
    }
}

private fun reportShipyardPlot(player: Player, npc: NPC) {
    if (!player.grandTreeAttribs().getB(ATTR_CONFRONTED_GLOUGH_ABOUT_FLEET)) {
        player.startConversation {
            npc(npc, HeadE.WORRIED, "Traveller, what are you doing here? The stronghold has been put on full alert! It's not safe for you here!")
            player(HeadE.CALM_TALK, "I need to confront Glough about what I found at the shipyard first.")
        }
        return
    }
    player.startConversation {
        player(HeadE.WORRIED, "King Narnode, I need to talk!")
        npc(npc, HeadE.WORRIED, "Traveller, what are you doing here? The stronghold has been put on full alert! It's not safe for you here!")
        player(HeadE.WORRIED, "Your Highness, I believe Glough is killing the trees in order to make a mass fleet of warships!")
        npc(npc, HeadE.SKEPTICAL, "That's an absurd accusation!")
        player(HeadE.WORRIED, "His hatred for humanity is stronger than you know!")
        npc(npc, HeadE.FRUSTRATED, "That's enough Traveller, you sound as paranoid as him! Traveller please leave! It's bad enough having one human locked up.") {
            player.grandTreeAttribs().setB(ATTR_WARNED_KING_ABOUT_FLEET, true)
        }
    }
}

private fun kingRejectsFleetAccusation(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello, Your Highness.")
        npc(npc, HeadE.WORRIED, "Please Traveller, if the gnomes see me talking to you they'll revolt against me.")
        player(HeadE.FRUSTRATED, "That's crazy!")
        npc(npc, HeadE.WORRIED, "Glough's scared the whole town, he expects the humans to attack any day. He's even begun to recruit hundreds of gnome soldiers.")
        player(HeadE.WORRIED, "Don't you understand he's creating his own army?!")
        npc(npc, HeadE.WORRIED, "Please Traveller, leave before it's too late!")
    }
}

private fun exchangePlansForTwigs(player: Player, npc: NPC) {
    val received = player.grandTreeAttribs().getB(ATTR_RECEIVED_TWIGS)
    player.startConversation {
        if (!received && player.inventory.containsOneItem(Obj.grandtree_invasionplans)) {
            player(HeadE.CALM_TALK, "Hi, Your Highness, did you think about what I said?")
            npc(npc, HeadE.SKEPTICAL, "Look, if you're right about Glough I would have him arrested but there's no reason for me to think he's lying.")
            player(HeadE.WORRIED, "Look, I found this at Glough's home!")
            simple("You give the King the invasion plans.")
            npc(npc, HeadE.WORRIED, "If these are to be believed then this is terrible!")
            npc(npc, HeadE.SKEPTICAL, "But it's not proof, any one could have made these. Traveller, I understand your concern, I had guards search Glough's house but they found nothing suspicious, just these odd twigs.")
            if (player.inventory.freeSlots < 3) {
                npc(npc, HeadE.WORRIED, "Hmm. Looks like you can't carry anymore. Put 4 things down and come back to me.")
            } else {
                simple("The King has given you some twigs lashed together.")
                npc(npc, HeadE.CALM_TALK, "On the other hand, if Glough's right about the humans we will need an army of gnomes to protect ourselves. So I've decided to allow Glough to raise a mighty gnome army.")
                npc(npc, HeadE.WORRIED, "The Grand Tree's still slowly dying, if it is human sabotage we must respond!") {
                    player.inventory.deleteItem(Obj.grandtree_invasionplans, 1)
                    GRAND_TREE_TWIGS.forEach { player.inventory.addItem(it) }
                    player.grandTreeAttribs().setB(ATTR_RECEIVED_TWIGS, true)
                }
            }
        } else if (received) {
            npc(npc, HeadE.WORRIED, "Please Traveller, take my advice and leave!")
            if (GRAND_TREE_TWIGS.any { !player.inventory.containsOneItem(it) && !twigWasPlaced(player, it) }) {
                player(HeadE.SAD_MILD_LOOK_DOWN, "I've lost those twigs you gave me.")
                if (player.replaceMissingTwigs())
                    npc(npc, HeadE.CALM_TALK, "Here take these, I don't see how it will help though.")
                else
                    npc(npc, HeadE.WORRIED, "Hmm. Looks like you can't carry anymore. Put 4 things down and come back to me.")
            }
        } else {
            npc(npc, HeadE.WORRIED, "Please Traveller, leave before it's too late!")
        }
    }
}

private fun twigWasPlaced(player: Player, itemId: Int): Boolean = when (itemId) {
    Obj.grandtree_twigt -> player.grandTreeAttribs().getB(ATTR_TWIG_T)
    Obj.grandtree_twigu -> player.grandTreeAttribs().getB(ATTR_TWIG_U)
    Obj.grandtree_twigz -> player.grandTreeAttribs().getB(ATTR_TWIG_Z)
    Obj.grandtree_twigo -> player.grandTreeAttribs().getB(ATTR_TWIG_O)
    else -> false
}

private fun kingBeforeDemonIsDefeated(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.WORRIED, "Your Highness, it's true about Glough I tell you! He's planning to take over Gielinor!")
        npc(npc, HeadE.SKEPTICAL, "I'm sorry Traveller but it's just not realistic. How could Glough, even with a gnome army, take over?")
        player(HeadE.WORRIED, "He plans to make a fleet of warships from the Grand Tree's wood!")
        npc(npc, HeadE.FRUSTRATED, "That's enough Traveller! I've no time for make believe. The tree's still dying. I must get to the truth of this!")
    }
}

private fun exposeGlough(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.WORRIED, "Traveller you're wounded! What happened?")
        player(HeadE.WORRIED, "It's Glough! He set a demon on me!")
        npc(npc, HeadE.AMAZED_MILD, "What?! Glough?! With a demon?!")
        player(HeadE.WORRIED, "Glough has a store of Daconia rocks further up the passage! He's been accessing the roots from a secret passage at his home.")
        npc(npc, HeadE.SKEPTICAL, "Never! Not Glough! He's a good gnome at heart!")
        npc(npc, HeadE.FRUSTRATED, "Guard!")
        npc(Npc.gnomekingsguard, HeadE.CALM_TALK, "Sire!")
        npc(npc, HeadE.CALM_TALK, "Go and check out that passage!")
        npc(Npc.gnomekingsguard, HeadE.WORRIED, "We found Glough hiding under a horde of Daconia rocks!")
        player(HeadE.FRUSTRATED, "That's what I've been trying to tell you! Glough's been fooling you!")
        npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "I..I don't know what to say! How could I have been so blind?!")
        npc(npc, HeadE.FRUSTRATED, "Guard! Call off the military training!")
        npc(npc, HeadE.FRUSTRATED, "The humans are not attacking!")
        npc(Npc.gnomekingsguard, HeadE.CALM_TALK, "Yes sir!")
        npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "You have my full apologies Traveller!")
        npc(npc, HeadE.HAPPY_TALKING, "And my gratitude!")
        npc(npc, HeadE.WORRIED, "A reward will have to wait though, the tree is still dying! The guards are clearing Glough's rock supply now but there must be more Daconia hidden somewhere in the roots! Help us search, we have little time!") {
            player.setGrandTreeStage(STAGE_FIND_DACONIA)
            player.ensureDaconiaRoot()
        }
    }
}

private fun daconiaReminder(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.WORRIED, "Traveller, have you managed to find the Daconia?")
        player(HeadE.SAD_MILD_LOOK_DOWN, "No sign of it so far.")
        npc(npc, HeadE.WORRIED, "The tree will still die if we don't find it! It could be anywhere!")
        player(HeadE.HAPPY_TALKING, "Don't worry, Your Highness! We'll find it!")
    }
}

private fun completeGrandTree(player: Player, npc: NPC) {
    if (!player.inventory.containsOneItem(Obj.grandtree_daconiarock)) {
        player.setGrandTreeStage(STAGE_FIND_DACONIA)
        daconiaReminder(player, npc)
        return
    }
    player.startConversation {
        npc(npc, HeadE.WORRIED, "Traveller, have you managed to find the Daconia?")
        player(HeadE.CONFUSED, "Is this it?")
        npc(npc, HeadE.HAPPY_TALKING, "Yes! Excellent, well done!")
        simple("You give the King the Daconia rock.")
        npc(npc, HeadE.HAPPY_TALKING, "It's incredible, the tree's health is improving already! I don't know what to say, we owe you so much!")
        npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "To think Glough had me fooled all along!")
        player(HeadE.HAPPY_TALKING, "All that matters now is that humans and gnomes can live together in peace!")
        npc(npc, HeadE.HAPPY_TALKING, "I'll drink to that!")
        npc(npc, HeadE.HAPPY_TALKING, "From now on I vow to make this stronghold a welcoming place for all! I'll grant you access to all our facilities.")
        player(HeadE.HAPPY_TALKING, "Thanks!")
        player(HeadE.CONFUSED, "I think!")
        npc(npc, HeadE.CALM_TALK, "It should make your stay here easier. You can use the spirit tree to transport yourself, as well as the gnome glider. I also give you access to our mine.")
        player(HeadE.CONFUSED, "Mine?")
        npc(npc, HeadE.CALM_TALK, "Very few know of the secret mine under the Grand Tree. If you push on the roots just to my north they will separate and let you pass.")
        player(HeadE.CONFUSED, "Strange!")
        npc(npc, HeadE.HAPPY_TALKING, "That's magic trees for you!")
        npc(npc, HeadE.HAPPY_TALKING, "All the best Traveller and thanks again!")
        player(HeadE.HAPPY_TALKING, "You too, Your Highness!") {
            player.inventory.deleteItem(Obj.grandtree_daconiarock, 1)
            player.questManager.completeQuest(Quest.GRAND_TREE)
        }
    }
}

private fun postQuestDialogue(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello, Your Highness!")
        npc(npc, HeadE.HAPPY_TALKING, "Well, hello again Traveller! How are you?")
        player(HeadE.HAPPY_TALKING, "I'm good thanks, how's the tree?")
        npc(npc, HeadE.HAPPY_TALKING, "Better than ever, thanks to you!")
    }
}
