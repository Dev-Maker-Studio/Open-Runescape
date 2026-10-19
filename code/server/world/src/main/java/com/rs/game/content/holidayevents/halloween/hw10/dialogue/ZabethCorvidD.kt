package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.OptionsBuilder
import com.rs.engine.dialogue.startConversation
import com.rs.engine.dialogue.statements.NPCStatement
import com.rs.engine.dialogue.statements.Statement
import com.rs.game.content.holidayevents.halloween.hw10.GrimState
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.grimState
import com.rs.game.content.holidayevents.halloween.hw10.hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.inPartyHouse
import com.rs.game.content.holidayevents.halloween.hw10.zabethSigned
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val ZABETH = Npc.hw10_black_zabeth
private const val AUTOGRAPH = Obj.obj_19845
private const val START = "start"

private val WRONG_ANSWER_REPLIES = listOf(
    "*Hic* S'luverly party. *Hic* You's ma bestest friend. *Hic*",
    "Wait, if I see everything one step into the future, how come I can't hear your response if you make a mistake? Erm...I mean...ooooh, unicorns tap-dancing...",
    "Aaaaaaaargh! Oh, wait, you're not a ten-foot tall dragon. My mistake.",
    "Zzzzzzzzzz eat the bat zzzzzzzzzzzzzzzzzzzzzz.",
    "Ha ha ha, best joke ever.",
)

private class WrongAnswerReply : Statement {
    private var shown = NPCStatement(ZABETH, HeadE.DRUNK, WRONG_ANSWER_REPLIES.first())

    override fun send(player: Player) {
        shown = NPCStatement(ZABETH, HeadE.DRUNK, WRONG_ANSWER_REPLIES.random())
        shown.send(player)
    }

    override fun getOptionId(componentId: Int) = shown.getOptionId(componentId)

    override fun close(player: Player) = shown.close(player)
}

@ServerStartupEvent
fun mapZabethCorvid() {
    onNpcClick(ZABETH, options = arrayOf("Talk-To")) { (player) ->
        player.startConversation {
            when {
                player.inPartyHouse || player.hw10Stage >= Hw10Stage.PARTY -> zabeth("*Hic* Thank you! You've been a *hic* wonderful audience! You three are alright.")
                player.hw10Stage < Hw10Stage.PREPARING || player.grimState == GrimState.NOT_ASKED -> {
                    zabeth("*Hic* Woooo! Great *hic* party man!")
                    player(HeadE.CONFUSED, "Erm, the party hasn't started yet.")
                    zabeth("*Hic* Wait, who are you three? I didn't order *hic* room service.")
                }
                player.grimState == GrimState.AUTOGRAPH_DELIVERED -> zabeth("*Hic* You're welcome, man! *Hic* Tell Grim he's welcome too.")
                !player.zabethSigned -> futureConversation(player)
                player.inventory.containsItem(AUTOGRAPH) || player.bank.containsItem(AUTOGRAPH) -> zabeth("*Hic* Yeah, you're welcome. Go on, Grim's *hic* waiting for that.")
                else -> {
                    zabeth("*Hic* No worries, man, I've got loads of pens. *Hic*")
                    player(HeadE.CONFUSED, "I've lost the autograph. Could you sign another one for Grim?")
                    item(AUTOGRAPH, "Zabeth gives you his autograph.", Runnable { player.inventory.addItemDrop(AUTOGRAPH, 1) })
                }
            }
        }
    }
}

private fun DialogueBuilder.zabeth(text: String) = npc(ZABETH, HeadE.DRUNK, text)

private fun DialogueBuilder.futureConversation(player: Player) {
    label(START)
    zabeth("*Hic* Hello there, ${player.displayName}. *Hic* Awesome PARTAY!")
    zabeth("I'm having a BLAST! Man, *hic* thanks for asking. You three are alright. *Hic*")
    options {
        op("Hello, enjoying the party?") {
            player(HeadE.HAPPY_TALKING, "Hello, enjoying the party?")
            zabeth("Oh yeah! I'm the dark *hic* master of music!")
            options {
                wrong("How are you?")
                op("So, what do people call you?") {
                    player(HeadE.CONFUSED, "So, what do people call you?")
                    zabeth("Woooah, you're well rude, dude!")
                    options {
                        op("Sheesh, you're an idiot.") {
                            player(HeadE.ANGRY, "Sheesh, you're an idiot.")
                            player(HeadE.FRUSTRATED, "Why am I wasting my time?")
                            zabeth("Nah, it's alright, I forgive you.")
                            zabeth("*Hic* Sounds good to me. *Hic* You three are alright.")
                            anotherDrink(player)
                        }
                        wrong("So, I hear you're a famous singer.")
                        leave()
                    }
                }
                leave()
            }
        }
        wrong("Are you drunk?")
        wrong("The party hasn't really started yet.")
        leave()
    }
}

private fun DialogueBuilder.anotherDrink(player: Player) {
    options {
        wrong("Sorry, but you are quite annoying.")
        op("Sorry, how about I make it up to you with another drink?") {
            player(HeadE.CALM_TALK, "Sorry.")
            player(HeadE.CALM_TALK, "How about I make it up to you with another drink?")
            zabeth("*Hic* I am a space kebbit: look at my shiny green fur! *Hic*")
            options {
                wrong("Wait...what?")
                wrong("What would you say if...")
                op("Exactly how drunk are you?") {
                    player(HeadE.CONFUSED, "Exactly how drunk are you?")
                    zabeth("That must have hurt!")
                    player(HeadE.SKEPTICAL, "This conversation is all a bit of a joke. Speaking of which...")
                    jokes(player)
                }
                leave()
            }
        }
        leave()
    }
}

private fun DialogueBuilder.jokes(player: Player) {
    options {
        op("A vampire and a werewolf walk into a bar...") {
            player(HeadE.LAUGH, "Okay, so a vampire and a werewolf walk into a bar...")
            zabeth("Woah, yeah: course I know Grim. I met him once. *Hic* Top guy, if a little on the skinny side.")
            options {
                wrong("Did you know that Grim's a fan?")
                op("Do you know Grim?") {
                    player(HeadE.CONFUSED, "Do you know Grim?")
                    zabeth("Sure, no problem. *hic*")
                    options {
                        op("Can I have an autograph for Grim?") { autograph(player) }
                        op("Can you give me a million XP?") {
                            player(HeadE.HAPPY_TALKING, "Can you give me a million XP?")
                            zabeth("Nice try.")
                            player(HeadE.CALM_TALK, "Worth a shot. I was wondering if we could break the cycle of pre-determination surrounding this whole conversation.")
                            player(HeadE.SKEPTICAL, "It's faintly sinister knowing that this conversation is pre-destined.")
                            player(HeadE.SKEPTICAL, "...")
                            player(HeadE.ANGRY, "Wait a minute: you just broke out of the predetermined conversation. Are you putting this on just to annoy me?")
                            simple("[He pretends not to hear the question]")
                            player(HeadE.FRUSTRATED, "...")
                            options {
                                op("Can I have an autograph for Grim?") { autograph(player) }
                                leave()
                            }
                        }
                        leave()
                    }
                }
                wrong("Can I get an autograph?")
                leave()
            }
        }
        wrong("A unicorn sits at a bar...") {
            player(HeadE.LAUGH, "A unicorn sits down at the bar, the barman asks 'Why the long face?'")
            player(HeadE.LAUGH, "Ha ha ha ha!")
        }
        wrong("Knock knock.") {
            zabeth("Who's there?")
            player(HeadE.CALM_TALK, "Knock, knock.")
            player(HeadE.FRUSTRATED, "Gah, it didn't really work, did it? Stupid precognition.")
        }
        leave()
    }
}

private fun DialogueBuilder.autograph(player: Player) {
    player(HeadE.HAPPY_TALKING, "Can I get an autograph for Grim? He'd REALLY appreciate it.")
    zabeth("You're *hic* welcome, man.")
    item(AUTOGRAPH, "Zabeth gives you his autograph.", Runnable {
        if (player.zabethSigned)
            return@Runnable
        player.inventory.addItemDrop(AUTOGRAPH, 1)
        player.zabethSigned = true
    })
    player(HeadE.HAPPY_TALKING, "Thanks for the autograph.")
    zabeth("*Hic* Bye! Give my best to Grim. Tell him that every rose has its thorn.")
    zabeth("He'll know what it means.")
}

private fun OptionsBuilder.wrong(option: String, lines: DialogueBuilder.() -> Unit = { player(HeadE.CALM_TALK, option) }) = op(option) {
    lines()
    statement(WrongAnswerReply())
    player(HeadE.CONFUSED, "I'm lost in this conversation now.")
    goto(START)
}

private fun OptionsBuilder.leave() = op("I have to leave.") {
    player(HeadE.FRUSTRATED, "I have to leave. I've had enough of this madness.")
}
