package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.holidayevents.halloween.hw10.GrimState
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Task
import com.rs.game.content.holidayevents.halloween.hw10.grimState
import com.rs.game.content.holidayevents.halloween.hw10.hasEek
import com.rs.game.content.holidayevents.halloween.hw10.hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.inPartyHouse
import com.rs.game.content.holidayevents.halloween.hw10.zabethSigned
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

private const val GRIM = Npc.hw10_grim
private const val AUTOGRAPH = Obj.obj_19845

@ServerStartupEvent
fun mapGrimReaper() {
    onNpcClick(GRIM, options = arrayOf("Talk-to")) { (player) ->
        player.startConversation {
            when {
                player.inPartyHouse || player.hw10Stage >= Hw10Stage.PARTY -> afterHandIn(player)
                player.hw10Stage < Hw10Stage.PREPARING -> npc(GRIM, HeadE.NERVOUS, "Not now, mortal. Speak to that pumpkin-headed fellow downstairs; he seems to be in charge of this...festivity.")
                else -> when (player.grimState) {
                    GrimState.NOT_ASKED -> firstTalk(player)
                    GrimState.AUTOGRAPH_REQUESTED -> when {
                        player.inventory.containsItem(AUTOGRAPH) -> deliverAutograph(player)
                        player.zabethSigned -> npc(GRIM, HeadE.ANGRY, "You had his autograph and you LOST it? Go back to Zabeth Corvid at once, mortal!")
                        else -> npc(GRIM, HeadE.NERVOUS, "Have you spoken to him yet? Did he mention me? No, don't tell me. Just bring me that autograph, mortal.")
                    }
                    GrimState.AUTOGRAPH_DELIVERED -> afterHandIn(player)
                }
            }
        }
    }
}

private fun DialogueBuilder.firstTalk(player: Player) {
    options {
        op("Hi, Grim.") {
            player(HeadE.CALM_TALK, "Hi, Grim.")
            greeting(player)
        }
        op("Hello Your Majesty...Your Highness.") {
            player(HeadE.NERVOUS, "Hello Your Majesty...Your Highness.")
            greeting(player)
        }
    }
}

private fun DialogueBuilder.greeting(player: Player) {
    npc(GRIM, HeadE.NERVOUS, "...What if Muncher bites him?...")
    npc(GRIM, HeadE.CALM_TALK, "Ahem! Greetings, mortal.")
    label("g1")
    options {
        op("Who were you talking about?") {
            player(HeadE.CONFUSED, "Who were you talking about?")
            zabethIsHere(player)
        }
        op("Looking forward to the party?") {
            player(HeadE.HAPPY_TALKING, "Looking forward to the party?")
            npc(GRIM, HeadE.CALM_TALK, "I do not have time for parties, mortal. There is a rock god downstairs.")
            label("g2")
            options {
                op("There's a god of stones?") {
                    player(HeadE.CONFUSED, "There's a god of stones?")
                    npc(GRIM, HeadE.ANGRY, "Wrong kind of rock, mortal.")
                    goto("g2")
                }
                op("What do you mean by 'rock'?") {
                    player(HeadE.CONFUSED, "What do you mean by 'rock'?")
                    npc(GRIM, HeadE.CALM_TALK, "'Rock' is a style of music. It's is popular in certain other planes of existence.")
                    rockGodMenu(player)
                }
            }
        }
        op("Why is this room undecorated?") {
            player(HeadE.CONFUSED, "Why is this room undecorated?")
            npc(GRIM, HeadE.CALM_TALK, "Pumpkin Pete may decorate the rest of my house as he sees fit, but this study stays as gloomy as the day I moved in.")
            goto("g1")
        }
    }
}

private fun DialogueBuilder.rockGodMenu(player: Player) {
    label("g3")
    options {
        op("So, who is this rock god?") {
            player(HeadE.CONFUSED, "So, who is this rock god?")
            zabethIsHere(player)
        }
        op("Why is this room not decorated?") {
            player(HeadE.CONFUSED, "Why is this room not decorated?")
            npc(GRIM, HeadE.CALM_TALK, "I told Pumpkin Pete to keep his cobwebs and candles out of here. One cannot fret properly in a room full of bunting.")
            goto("g3")
        }
    }
}

private fun DialogueBuilder.zabethIsHere(player: Player) {
    npc(GRIM, HeadE.HAPPY_TALKING, "Who else, mortal? Zabeth Corvid, the most talented musical artist since...well...ever.")
    npc(GRIM, HeadE.AMAZED, "He's here, with Adamantite Damsel...in MY house!")
    npc(GRIM, HeadE.NERVOUS, "Wait, are your shoes clean? Are you going to leave your hair like that? You must look presentable while Zabeth Corvid is in the house.")
    npc(GRIM, HeadE.NERVOUS, "Quick, show me your teeth...")
    player(HeadE.SKEPTICAL, "I think you're slightly over-reacting.")
    npc(GRIM, HeadE.ANGRY, "Mortal. A musical legend is downstairs in my sitting room, and his band is setting up to play a live gig in my house, for ME. How would you react?")
    options {
        op("Panic!") {
            player(HeadE.SCARED, "Oh my god, oh my god! PANIC!")
            npc(GRIM, HeadE.ANGRY, "You're not funny, you know.")
            options {
                op("Go on, I am a little bit.") {
                    player(HeadE.LAUGH, "Go on, I am a little bit.")
                    npc(GRIM, HeadE.EVIL_LAUGH, "Remember, comedians are known to die onstage. I would be very careful.")
                    player(HeadE.NERVOUS, "Thinly veiled threat noted.")
                    fanMenu(player, "So, if you're a fan, why are you hiding up here?")
                }
                op("Sorry.") {
                    player(HeadE.SAD, "Sorry.")
                    npc(GRIM, HeadE.CALM_TALK, "Hmph. Apology accepted, mortal.")
                    fanMenu(player, "So, if you're a fan, why are you hiding up here?")
                }
            }
        }
        op("I don't think I'd be quite this anxious.") {
            player(HeadE.CALM_TALK, "I don't think I'd be quite this anxious.")
            npc(GRIM, HeadE.ANGRY, "Then you cannot be a true fan.")
            fanMenu(player, "If you're a fan, why are you hiding up here?")
        }
        op("Okay, I can see your point.") {
            player(HeadE.CALM_TALK, "Okay, I can see your point.")
            fanMenu(player, "If you're a fan, why are you hiding up here?")
        }
    }
}

private fun DialogueBuilder.fanMenu(player: Player, hidingOption: String) {
    options {
        op(hidingOption) {
            player(HeadE.CONFUSED, "If you're a fan, why are you hiding up here?")
            npc(GRIM, HeadE.NERVOUS, "Well...I mean, he's obviously busy; I couldn't possibly disturb him.")
            options {
                op("Are you shy?") {
                    player(HeadE.CONFUSED, "Are you shy?")
                    npc(GRIM, HeadE.ANGRY, "I'm the Grim Reaper, I do not get shy.")
                    options {
                        op("So, you're a big fan, I take it?") { bigFan(player) }
                        op("Shall I talk to him for you?") { offerAutograph(player) }
                    }
                }
                op("Shall I talk to him for you?") { offerAutograph(player) }
            }
        }
        op("You're a big fan, I take it?") { bigFan(player) }
    }
}

private fun DialogueBuilder.bigFan(player: Player) {
    player(HeadE.CONFUSED, "So, you're a big fan, I take it?")
    npc(GRIM, HeadE.CALM_TALK, "I have followed his work for decades, mortal, and it is, without a doubt, the finest music I have ever heard.")
    npc(GRIM, HeadE.SAD, "I have collected several pieces of memorabilia but, alas, I miss that all-important autograph.")
    offerAutograph(player)
}

private fun DialogueBuilder.offerAutograph(player: Player) {
    player(HeadE.CALM_TALK, "Shall I talk to him for you?")
    npc(GRIM, HeadE.CONFUSED, "For what purpose?")
    player(HeadE.CALM_TALK, "Maybe I could get you an autograph.")
    npc(GRIM, HeadE.AMAZED, "OHMYWORDITOTALLYWOULDLOVEANAUTOGRAPH!")
    npc(GRIM, HeadE.CALM_TALK, "Ahem, I mean: an autograph would be appreciated mortal, thank you. I should warn you, I believe that conversations with Zabeth Corvid can be...difficult.")
    player(HeadE.CONFUSED, "How so?")
    npc(GRIM, HeadE.CALM_TALK, "When Zabeth Corvid died, he was the most inebriated person on the planet. He was so drunk that, when I harvested him, he stumbled into the after-afterlife.")
    player(HeadE.CONFUSED, "I don't quite get it.")
    npc(GRIM, HeadE.CALM_TALK, "Basically, he was so drunk that he wandered a few seconds into the future. It makes dealing with him...complicated.")
    player(HeadE.CALM_TALK, "Noted. Okay, I'll go and see if I can get you that autograph.")
    npc(GRIM, HeadE.CALM_TALK, "Thank you, mortal.", Runnable { player.grimState = GrimState.AUTOGRAPH_REQUESTED })
}

private fun DialogueBuilder.deliverAutograph(player: Player) {
    player(HeadE.CALM_TALK, "Hi, Grim.")
    label("g8")
    options {
        op("'Every rose has its thorn.'") {
            player(HeadE.CALM_TALK, "'Every rose has its thorn.'")
            npc(GRIM, HeadE.NO_EXPRESSION, "...")
            npc(GRIM, HeadE.SAD, "Will I ever live that down?")
            player(HeadE.CONFUSED, "Live what down?")
            npc(GRIM, HeadE.CALM_TALK, "It is better that you do not know.")
            goto("g8")
        }
        op("I have your autograph for you.") {
            player(HeadE.HAPPY_TALKING, "I have your autograph for you.")
            npc(GRIM, HeadE.CALM_TALK, "Your efforts are appreciated.", Runnable {
                if (!player.inventory.containsItem(AUTOGRAPH))
                    return@Runnable
                player.inventory.deleteItem(AUTOGRAPH, 1)
                player.anim(Seq.hw10_player_give_or_receive)
                player.grimState = GrimState.AUTOGRAPH_DELIVERED
            })
            player(HeadE.CONFUSED, "So, are you going to be able to enjoy the party now?")
            npc(GRIM, HeadE.CALM_TALK, "Who has the time for a party? I have to make sure that this autograph gets the perfect place in this room...maybe over the fireplace. Oh no, it could catch fire.")
            player(HeadE.CALM_TALK, "I'll leave you to it.")
            npc(GRIM, HeadE.CALM_TALK, "Thank you, mortal.")
            npc(GRIM, HeadE.HAPPY_TALKING, "Oooh, it would look awesome on the wall.")
            npc(GRIM, HeadE.CALM_TALK, "Ahem, I mean it would look most agreeable on the wall.")
            checkInWithPete(player, Hw10Task.AUTOGRAPH)
        }
    }
}

private enum class GrimTopic { DOWNSTAIRS, ROSE }

private fun DialogueBuilder.afterHandIn(player: Player) {
    npc(GRIM, HeadE.HAPPY_TALKING, "Thank you for the autograph, mortal. It will take pride of place on my wall when I get it framed.")
    if (player.hasEek) {
        eek("I could web it to the wall for you? Spiderweb makes for an excellent frame.")
        npc(GRIM, HeadE.CALM_TALK, "I'd rather not. This will be a priceless piece of memorabilia.")
        eek("But it'll be a limited edition in a spiderweb frame!")
    }
    afterHandInMenu(emptySet())
}

private fun DialogueBuilder.afterHandInMenu(asked: Set<GrimTopic>) {
    options {
        if (GrimTopic.DOWNSTAIRS !in asked)
            op("Are you going to come downstairs now?") {
                player(HeadE.CONFUSED, "Are you going to come downstairs now?")
                npc(GRIM, HeadE.NERVOUS, "I couldn't possibly be seen in this state. I must make sure I am properly dressed for the occasion.")
                npc(GRIM, HeadE.NERVOUS, "I have to make sure that my bones are properly polished, my scythe suitably sharpened and my robes...well I think they have to be burned and new ones bought to replace them. So much to do.")
                player(HeadE.CONFUSED, "So, getting you the autograph hasn't really made you enjoy yourself any more?")
                npc(GRIM, HeadE.HAPPY_TALKING, "Are you joking, mortal? This is the best day EVER!")
                afterHandInMenu(asked + GrimTopic.DOWNSTAIRS)
            }
        if (GrimTopic.ROSE !in asked)
            op("Can you tell me about this 'every rose has its thorn' thing?") {
                player(HeadE.CONFUSED, "Can you tell me about this 'every rose has its thorn' thing?")
                npc(GRIM, HeadE.SAD, "I'd really rather not.")
                player(HeadE.HAPPY_TALKING, "Oh go on, I got you that autograph, after all.")
                npc(GRIM, HeadE.CALM_TALK, "It was a long time ago in a dimension far, far away. It certainly wasn't an excellent adventure.")
                afterHandInMenu(asked + GrimTopic.ROSE)
            }
        op("I'll be going now.")
    }
}
