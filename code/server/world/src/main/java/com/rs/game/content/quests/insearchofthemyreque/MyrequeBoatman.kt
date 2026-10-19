package com.rs.game.content.quests.insearchofthemyreque

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

internal fun Player.talkCyreg() {
    val owner = this
    startConversation {
        when {
            owner.myrequeStage == COMPLETE -> {
                player(CALM_TALK, "Thanks for your help in finding the Myreque.")
                npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Well, I'd like to say that you're welcome.")
                npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "But I heard that you led Vanstrom straight to their hideout and he killed Sani and Harold. I feel so guilty.")
                player(CALM_TALK, "Hmm, yeah, I know how you feel.")
            }
            owner.myrequeStage == UNSTARTED -> npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Hello there, friend.")
            owner.myrequeStage == WEAPONS -> {
                npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Hello there, friend.")
                player(CALM_TALK, "Hello there. I have some weapons for you to give to the 'Myreque'.")
                if (!owner.hasMyrequeWeapons()) npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Hmm, well, it doesn't look as if you've got the right sort of weapons to me!")
                else {
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Hmm, I'm sure I don't know what you're talking about.")
                    player(CALM_TALK, "Come on, I know you're in cahoots with them; just take these weapons to them.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Ok, seriously, I did some work for them before, it's just too dangerous. I won't take the weapons to them, I'm sorry, it's just too dangerous.")
                }
                player(CALM_TALK, "Can you tell me how to find the Myreque?")
                npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Their base is well hidden, and I'm sorry but I can't reveal the directions. Sorry, but I guess you're all out of luck.")
                cyregPersuasion(owner)
            }
            owner.myrequeStage == PERSUADED -> {
                player(CALM_TALK, "Will you still take me to the Myreque?")
                cyregPreparation(owner)
            }
            else -> cyregReady(owner)
        }
    }
}

private fun DialogueBuilder.cyregPersuasion(owner: Player) {
    label("persuade1")
    options {
        op("Oh come on, you can tell me!") { cyregEyes(); goto("persuade1") }
        op("Well, I guess they'll just die without weapons.") {
            player(CALM_TALK, "Well, I guess they'll just die without weapons.")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Hmm, you don't seem too concerned about their welfare... I'm glad I didn't tell you where they were.")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "In any case, they're resourceful. They can look after themselves.")
            player(CALM_TALK, "What's that supposed to mean?")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "They're resourceful folks, that's all I'm saying. Their leader, Veliaf, looks after them well.")
            label("persuade2")
            options {
                op("Oh come on, you can tell me!") { cyregEyes(); goto("persuade2") }
                op("Resourceful enough to get their own steel weapons?") {
                    player(CALM_TALK, "Resourceful enough to get their own steel weapons?")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Maybe they are... what do you care anyway? They've been up against it ever since they got started.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "All of 'em have suffered more loss and heartache than you'll ever know. Now, leave me be!")
                    label("persuade3")
                    options {
                        op("What have they been up against?") {
                            player(CALM_TALK, "What have they been up against?")
                            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "You're not from around here or you wouldn't be asking such foolish questions.")
                            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Morytania is ruled by a cruel dark overlord by the name of Drakan. His reign over Morytania means we all live in fear.")
                            goto("persuade3")
                        }
                        op("What kind of loss and heartache?") {
                            player(CALM_TALK, "What kind of loss and heartache?")
                            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "The worst kind, most have lost members of their family. Little Sani Piliu, she was orphaned overnight")
                            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "when a vampyre went on the rampage... Imagine that, losing your entire family in one night? Terrible!")
                            player(CALM_TALK, "It sounds awful... Who is Sani Piliu?")
                            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "She's the only female member of the Myreque. She's already proven herself with her agility and light fingers, if you know what I mean!")
                            goto("persuade3")
                        }
                        op("If you don't tell me, their deaths are on your head!") {
                            player(CALM_TALK, "If you don't tell me, their deaths are on your head!")
                            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "There's death aplenty in this forsaken place... what do I care that some foolhardy vigilantes decided to go it alone against the Drakans?")
                            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Stupidity of youth is to blame, I shan't carry it on my shoulders!")
                            label("persuade4")
                            options {
                                op("One man's vigilante is another man's freedom fighter!") {
                                    player(CALM_TALK, "One man's vigilante is another man's freedom fighter!")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Aye, you can see it from both sides I suppose. But many of us consider it fool hardy to fight for something we'll never get,")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "even Polmafi, a scholar, such as he was, agrees that the chances are slim.")
                                    player(CALM_TALK, "Polmafi? Who's he?")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Polmafi Ferdygris is one of the Myreque. He's a technical sort and advises on all sorts of things to Veliaf.")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "He was a scholar before he became a renegade.")
                                    goto("persuade4")
                                }
                                op("Who are the Drakans?") {
                                    player(CALM_TALK, "Who are the Drakans?")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "The Drakans are the family of overlords that rule Morytania. They're the ones to whom the blood tithes are paid.")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Too much I have told you already! Ignorance is better than these truths, I tell you!")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "I can pretend once more that I am a free man, and some relief from this gloom can I feel again. Be gone with you now and leave me with my dreams.")
                                    goto("persuade4")
                                }
                                op("What kind of a man are you to say that you don't care?") {
                                    player(CALM_TALK, "What kind of a man are you to say that you don't care?")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Don't dare to judge me, young fool... what do you know of the heartache I carry? Can you not see the anchor of woe that holds me fast?")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Very well, if you would take your chance to help these strangers, who am I to stop you?")
                                    player(CALM_TALK, "But will you help me? Will you take me to them?")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "No, I won't take you, but you can use my boat. You'll be going through Mort Myre,")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "though, so I won't be letting you go unless you've some defence against the Ghasts.")
                                    exec { if (owner.myrequeStage == WEAPONS) owner.advanceMyreque(PERSUADED); owner.startConversation { cyregPreparation(owner) } }
                                }
                                op("Why do you say that this place is 'forsaken'?") {
                                    player(CALM_TALK, "Why do you say that this place is 'forsaken'?")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "All of these lands are forsaken of Saradomin's kindness, only cold death from the evil gods do we now feel.")
                                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Those lucky ones to the west of the Salve little realise their fate if the river should one day become tainted.")
                                    goto("persuade4")
                                }
                                op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
                            }
                        }
                        op("I just want to help them, I think they need help.") { cyregIvan(); goto("persuade3") }
                        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
                    }
                }
                op("I'll give you some cash if you tell me.") { cyregBribe(); goto("persuade2") }
                op("I just want to help them, I think they need help.") { cyregIvan(); goto("persuade2") }
                op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
            }
        }
        op("I'll give you some cash if you tell me.") { cyregBribe(); goto("persuade1") }
        op("I just want to help them, I think they need help.") { cyregIvan(); goto("persuade1") }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

private fun DialogueBuilder.cyregEyes() {
    player(CALM_TALK, "Oh come on, you can tell me!")
    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "I'm sorry, I can't. I just can't... people are watching... eyes everywhere!")
}
private fun DialogueBuilder.cyregBribe() {
    player(CALM_TALK, "I'll give you some cash if you tell me.")
    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "You think you can buy me!")
    player(CALM_TALK, "Er, no, I just want to compensate you for your trouble!")
    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "You keep your money and I'll keep my secrets.")
}
private fun DialogueBuilder.cyregIvan() {
    player(CALM_TALK, "I just want to help them, I think they need help.")
    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Aye, well, they may do... but it's just not safe and it's not likely to get safer any time soon.")
    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Though I do feel sorry for Ivan, the baby of the group. He's seen too few winters to be involved in such toil.")
}

private fun DialogueBuilder.cyregPreparation(owner: Player) {
    if (!owner.inventory.containsItem(Obj.druid_pouch, 1)) {
        player(CALM_TALK, "I don't have anything which I can use against them at this time.")
        return
    }
    simple("You show the boatman your druid pouch.")
    player(CALM_TALK, "I have this druid pouch! This turns the Ghasts visible and I can kill them once I can see them.")
    if (!owner.inventory.containsItem(Obj.woodplank, 3)) {
        npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Very well, but I still cannot let you pass unprepared! My boat needs some work, so I'll need three wooden planks. The bridge you'll need")
        npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "to cross later is rotten and may need to be mended, so you'll need three more planks as well as a hammer, and I'd say around 75 nails.")
        return
    }
    player(CALM_TALK, "I also have the three wooden planks you asked for.")
    options("Do you want to give Cyreg three wooden planks?") {
        op("Give wooden planks to Cyreg.") {
            exec {
                if (owner.myrequeStage != PERSUADED || !owner.inventory.containsItem(Obj.woodplank, 3) || !owner.inventory.containsItem(Obj.druid_pouch, 1)) { owner.simpleDialogue("You need three wooden planks and a charged druid pouch."); return@exec }
                owner.inventory.deleteItem(Obj.woodplank, 3)
                owner.advanceMyreque(BOAT)
                owner.sendMessage("The boatman takes 3 wooden planks from you.")
                if (!owner.inventory.containsItem(Obj.woodplank, 3)) owner.startConversation {
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Thank you, but I still cannot let you pass unprepared.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "The bridge you'll need to cross later is rotten and may need to be mended.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "You'll need three more planks as well as a hammer, and I'd say around 75 nails.")
                }
            }
        }
        op("Don't give any wooden planks to Cyreg.") { player(CALM_TALK, "Not just yet, sorry.") }
    }
}

private fun DialogueBuilder.cyregReady(owner: Player) {
    if (!owner.myreque.getB("travelled") && owner.myrequeStage == BOAT) {
        val planks = owner.inventory.containsItem(Obj.woodplank, 3)
        val nails = owner.inventory.containsItem(Obj.nails, 75)
        if (!planks || !nails) npc(Npc.route_cyreg_paddlehorn, CALM_TALK, when {
            !planks && !nails -> "I cannot let you pass unprepared. You have the hammer for the bridge, but you'll still need 3 wooden planks and I'd say around 75 nails."
            !planks -> "I cannot let you pass unprepared. You have the hammer and nails for the bridge, but you'll still need 3 wooden planks."
            else -> "I cannot let you pass unprepared. You have the planks and hammer for the bridge, but you'll still need around 75 nails."
        })
    }
    options {
        op("Can I ask some more questions?") {
            player(CALM_TALK, "Can I ask some more questions?")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Sure you can.")
            cyregQuestions(owner)
        }
        op("Ok") { }
    }
}

private fun DialogueBuilder.cyregQuestions(owner: Player) {
    label("cyregQuestions")
    options {
        op("Where do I go after I get to the hollows?") {
            player(CALM_TALK, "Where do I go after I get to the hollows?")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "You should head north and look for an unusual tree.")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "You look like you know a thing or two about exploring so I guess I don't need to tell you to keep your eyes peeled!")
            goto("cyregQuestions")
        }
        op("Tell me about the 'Myreque'.") {
            player(CALM_TALK, "Tell me about the 'Myreque'.")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "What do you want to know?")
            label("myrequeDetails")
            options {
                op("Why are they called 'the Myreque'?") {
                    player(CALM_TALK, "Why are they called 'the Myreque'?")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "The locals just called them after the place where they hide out, in Mort Myre. They are Myreque, 'hidden in the myre'.")
                    goto("myrequeDetails")
                }
                op("Tell me about the members of the Myreque") {
                    player(CALM_TALK, "Tell me about the members of the Myreque")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Well, you have Radigad Ponfit... he's your average mercenary from Asgarnia, ready to slice anyone's head off for a price.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "He's got a personal score to settle with the Drakans, though no one knows what it is.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Then there's Veliaf, he's the leader.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "and then there's Ivan, he's the baby of the group.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "There's Sani Piliu, she's a lovely girl, though a bit shady if you know what I mean.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "You've also got Harold Evans, he's a bit hot-headed, always straight into the fray.")
                    npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "And the brains of the operation, reporting directly to Veliaf, is Polmafi Ferdygris; he used to be quite a clever scholar!")
                    goto("myrequeDetails")
                }
                op("--- Ask a different question. ---") { exec { owner.startConversation { cyregQuestions(owner) } } }
            }
        }
        op("Tell me about the Drakans.") {
            player(CALM_TALK, "Tell me about the Drakans")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "Well it's rumoured that they're controlling the whole of Morytania. They live in the Sanguinesti region,")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "but I've never been, like most villagers in Morytania, I'm afraid to leave the village and dread what lies beyond.")
            goto("cyregQuestions")
        }
        op("Tell me about yourself.") {
            player(CALM_TALK, "Tell me about yourself.")
            npc(Npc.route_cyreg_paddlehorn, CALM_TALK, "I'm just a humble boatman, Cyreg Paddlehorn is my name. Like most of the Paddlehorns before me, I make my living by tacking the swamps of Mort Myre.")
            goto("cyregQuestions")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}
