package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapCerilCarnillean() {
    onNpcClick(Npc.sir_ceril_carnillean) { (player, npc) -> cerilDialogue(player, npc) }
}

private fun cerilDialogue(player: Player, npc: NPC) {
    val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
    when {
        stage == STAGE_UNSTARTED -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello there.")
            npc(npc, HeadE.ANGRY, "Blooming, thieving, weirdo cultists! Why don't they leave me alone? String 'em all up, that's what I say!")
            options {
                op("What's wrong?") {
                    player(HeadE.CONFUSED, "What's wrong?")
                    npc(npc, HeadE.FRUSTRATED, "There's some kind of crazy cult, dressed all in black, who hang out near the cave entrance in the forest south of here. The freaks keep breaking into my house!")
                    player(HeadE.CONFUSED, "Have they taken much?")
                    npc(npc, HeadE.CALM_TALK, "They first broke in months ago and stole a suit of armour. The strange thing is that they've broken in four times since but taken nothing.")
                    player(HeadE.CONFUSED, "And you are...?")
                    npc(npc, HeadE.CALM_TALK, "Why, I am Sir Ceril Carnillean! We are quite a famous bloodline who've played a vital role in the politics of Ardougne for many generations.")
                    npc(npc, HeadE.CALM_TALK, "Perhaps you would be able to assist me in returning the stolen armour? For a modest cash reward, of course!")
                    options {
                        op("Accept Quest") {
                            player(HeadE.HAPPY_TALKING, "Yes, of course. I'd be happy to help.")
                            npc(npc, HeadE.HAPPY_TALKING, "Excellent! I suggest you start looking near the cave entrance, in the forest south of here. That's where they hang out.")
                            player(HeadE.CONFUSED, "How do you know that?")
                            npc(npc, HeadE.CALM_TALK, "My old butler, Higson, once followed them to their hideout there. Unfortunately, the next night he died in his sleep.")
                            player(HeadE.WORRIED, "That's awful!")
                            npc(npc, HeadE.CALM_TALK, "No, it's okay. A replacement arrived the next day. He's been great: cooks an excellent broth!")
                            player(HeadE.CALM_TALK, "Um, right, yes. I'll get to work.") {
                                player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_FIND_CLIVET)
                            }
                        }
                        op("Not Right Now") {
                            npc(npc, HeadE.FRUSTRATED, "No wonder I'm the one with the big house, and you're the one on the streets.")
                        }
                    }
                }
                op("You probably deserve it.") {
                    player(HeadE.FRUSTRATED, "You probably deserve it.")
                    npc(npc, HeadE.ANGRY, "And who are YOU to judge ME? You look like a peasant anyway. I'm wasting my time talking to you.")
                }
                op("You seem uptight. I'll leave you alone.") {
                    player(HeadE.CALM_TALK, "You seem uptight. I'll leave you alone.")
                    npc(npc, HeadE.FRUSTRATED, "Yes, I doubt you could help anyway.")
                }
            }
        }

        stage in STAGE_FIND_CLIVET..STAGE_CHOOSE_SIDE -> player.startConversation {
            npc(npc, HeadE.CONFUSED, "Ah, it's you again. Have you found my armour yet?")
            options {
                op("Where did you say the cultists lived?") {
                    npc(npc, HeadE.CALM_TALK, "They hang out near the cave entrance, in the forest south of here, dressed all in black.")
                    player(HeadE.CALM_TALK, "Oh! Well, I'll keep looking.")
                }
                op("Nope. Still looking.") { npc(npc, HeadE.FRUSTRATED, "Well, get a blooming move on!") }
            }
        }

        !player.isCultSide() && stage == STAGE_BRANCH_PROGRESS -> returnArmourToCeril(player, npc)
        !player.isCultSide() && stage == STAGE_ENTER_HIDEOUT -> player.startConversation {
            npc(npc, HeadE.ANGRY, "What do you want now, you scoundrel?")
            player(HeadE.CALM_TALK, "I need evidence that Jones is involved with the cult.")
            npc(npc, HeadE.FRUSTRATED, "Then don't return until you have some!")
        }

        player.isCultSide() && stage in STAGE_BRANCH_PROGRESS..STAGE_RETURN_SCROLL -> player.startConversation {
            if (stage >= STAGE_RETURN_SCROLL) {
                player(HeadE.CALM_TALK, "Hello, Ceril. How's things?")
                npc(npc, HeadE.FRUSTRATED, "SIR Ceril! SIR! And I'm not sure, but I think the thieves have been back in my house.")
                player(HeadE.CALM_TALK, "Oh - heh - okay then.")
            } else {
                npc(npc, HeadE.SAD, "Oh, cruel world! Scruffy...I knew you well. I just don't think I can go on without...")
                player(HeadE.FRUSTRATED, "Loosen up already! It was only a dog.")
            }
        }

        stage == STAGE_COMPLETE && player.isCultSide() -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello Ceril.")
            npc(npc, HeadE.ANGRY, "Oooooh, I may be wrong, but ever since I asked for your help, things around here have gone from bad to worse.")
            npc(npc, HeadE.FRUSTRATED, "I think you'd better keep out of my way. And, for the LAST time, it's SIR Ceril. SIR!")
        }

        stage == STAGE_COMPLETE -> player.startConversation {
            player(HeadE.CALM_TALK, "Hello Ceril.")
            npc(npc, HeadE.HAPPY_TALKING, "Well, hello again adventurer! If it weren't for you, the treacherous Jones would have poisoned my family by now! We are in your debt.")
            player(HeadE.HAPPY_TALKING, "Don't worry about it, a good deed is its own reward. And that 2,000 gold didn't hurt, either.")
        }

        else -> player.startConversation {
            npc(npc, HeadE.CONFUSED, "Have you had any luck yet?")
            player(HeadE.CALM_TALK, "I'm still working on it.")
        }
    }
}

private fun returnArmourToCeril(player: Player, npc: NPC) {
    player.startConversation {
        if (!player.inventory.containsOneItem(Obj.carnillean_armour)) {
            npc(npc, HeadE.CONFUSED, "Have you recovered my armour?")
            player(HeadE.WORRIED, "I defeated Alomone, but I don't have the armour with me.")
            return@startConversation
        }
        player(HeadE.HAPPY_TALKING, "I've killed the cult leader, and he dropped your armour!")
        npc(npc, HeadE.HAPPY_TALKING, "Well done! I must say that I am very impressed. Come on, hand it over.")
        npc(npc, HeadE.CALM_TALK, "Before we send you on your way with your payment, I'll just get Jones to whip you up a batch of his special broth.")
        player(HeadE.WORRIED, "I'd rather not. I overheard the cultists talking, and apparently Jones is in league with them.")
        npc(npc, HeadE.ANGRY, "JONES! Get over here! We're going to blooming-well sort this out, once and for all!")
        npc(Npc.hazeelcult_butlerjones, HeadE.CALM_TALK, "It wasn't me, m'lud. I am, as you know, a loyal servant.")
        npc(npc, HeadE.FRUSTRATED, "I cannot fathom why this scoundrel would accuse you of such a crime without evidence to back it up.")
        npc(npc, HeadE.CALM_TALK, "I gave my word to reward you, but I must also compensate Jones for this terrible slander.")
        simple("Sir Ceril gives you 5 coins.<br>Sir Ceril gives Jones 1,995 coins.") {
            player.inventory.deleteItem(Obj.carnillean_armour, 1)
            player.inventory.addItemDrop(Obj.coins, 5)
            player.questManager.setStage(Quest.HAZEEL_CULT, STAGE_ENTER_HIDEOUT)
        }
        npc(npc, HeadE.ANGRY, "Now take it and leave, you scoundrel! Don't darken my doorstep again!")
        npc(Npc.hazeelcult_butlerjones, HeadE.HAPPY_TALKING, "Don't worry, m'lud, this fool won't be bothering us any more.")
        simple("Jones smirks at you.<br>You are going to need more than words to prove his treachery.")
    }
}
