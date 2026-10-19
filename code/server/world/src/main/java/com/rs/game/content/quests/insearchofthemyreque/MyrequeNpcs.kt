package com.rs.game.content.quests.insearchofthemyreque

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onItemOnNpc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapMyrequeNpcs() {
    onNpcClick(Npc.npc_2020, Npc.route_vanstrom_klause_sitting, Npc.canafis_stranger, options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcClick
        player.talkMyrequeVanstrom()
    }
    onNpcClick(Npc.route_cyreg_paddlehorn, options = arrayOf("Talk-to")) { (player, _) -> player.talkCyreg() }
    onNpcClick(Npc.npc_1568, options = arrayOf("Talk-to")) { (player, _) -> player.talkCurpile() }
    onNpcClick(Npc.route_veliaf_hurtz, *MEMBERS.keys.toTypedArray(), options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcClick
        when {
            player.myrequeStage == HELLHOUND -> {
                player.simpleDialogue(if (npc.id == Npc.route_veliaf_hurtz) "Veliaf can't talk right now, he seems to be formulating a plan." else "This person isn't able to talk to you at this time.")
                spawnMyrequeHellhound(player)
            }
            npc.id == Npc.route_veliaf_hurtz -> player.talkVeliaf()
            else -> player.talkMyrequeMember(npc.id)
        }
    }
    onItemOnNpc(Npc.route_cyreg_paddlehorn) { (player, item, _) -> if (item.id == Obj.woodplank) player.talkCyreg() }
    onItemOnNpc(Npc.route_veliaf_hurtz) { (player, item, _) -> if (item.id in WEAPON_LIST) player.talkVeliaf() }
}

private fun Player.talkMyrequeVanstrom() {
    val owner = this
    startConversation {
        when {
            owner.myrequeStage == COMPLETE -> npc(Npc.canafis_stranger, CALM_TALK, "I think we've been through this before, haven't we?")
            owner.myrequeStage >= HELLHOUND -> {
                npc(Npc.canafis_stranger, CALM_TALK, "Hello, friend. What can I do for you?")
                player(CALM_TALK, "Oh, sorry. I thought you were someone else.")
                if (owner.myrequeStage == EXIT && owner.myreque.getB("ladderUsed")) {
                    npc(Npc.canafis_stranger, CALM_TALK, "Who did you think I was?")
                    player(CALM_TALK, "I thought you were that dirty murderer, Vanstrom.")
                    npc(Npc.canafis_stranger, CALM_TALK, "Whoa! It sounds like you've got a score to settle.")
                    player(CALM_TALK, "I definitely have!")
                    exec { if (owner.myrequeStage == EXIT && owner.myreque.getB("ladderUsed")) owner.advanceMyreque(COMPLETE) }
                }
            }
            owner.myrequeStage == UNSTARTED -> {
                vanstromIntro(owner)
                label("vanstrom")
                options {
                    op("What friends are these?") {
                        player(CALM_TALK, "What friends are these?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "It's a personal tragedy that I have yet to meet them in the flesh.")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "But their exploits make mouth watering hero stories...the real meat and drink of high adventure and daring...so they say.")
                        player(CALM_TALK, "What does that mean exactly? I mean, I have some stories. I'm quite a hero myself, you may actually be talking about me?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "They're regarded as heroes in Morytania, though some people see them as vigilantes.")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "The local villagers call them the 'Myreque'. Some people call them terrorists while others call them freedom fighters!")
                        goto("vanstrom")
                    }
                    op("Why do they need help? Are they in trouble?") {
                        player(CALM_TALK, "Why do they need help? Are they in trouble?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "I should imagine that heroes of such high calibre are almost always in some sort of trouble, wouldn't you?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "There's always some evil heel ready to grind the face of humanity into the dirt?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "However, the Myreque are almost certainly able to handle themselves...given the tools!")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "I hear they're short of weapons, I was hoping to do it myself but I find that I'm rather short of time and ability!")
                        player(CALM_TALK, "What help do you hope to give them?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "I'd have taken some weapons to them!")
                        player(CALM_TALK, "What kind of weapons do they need?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Steel, I believe. All six of them require steel weapons. I would have suggested a longsword, two swords, a dagger, a mace and a warhammer.")
                        goto("vanstrom")
                    }
                    op("Perhaps I could help you out here.") {
                        player(CALM_TALK, "Perhaps I could help you out here. Maybe I could take these weapons to the Myreque.")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Oh yes? Well, that would be very nice of you! Are you sure you want to help out?")
                        acceptMyreque(owner)
                    }
                    op("I wish I could help, but I'm busy at the moment.") {
                        player(CALM_TALK, "I wish I could help, but I'm busy at the moment.")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Hmm, the same as me I fear! Oh well, I'm sure they'll get on well by themselves, the little dears. Thanks for even considering it though.")
                    }
                    op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
                }
            }
            else -> {
                npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Hello again, how's the quest going?")
                label("vanstromHelp")
                options {
                    op("Not very well. I need some help.") {
                        player(CALM_TALK, "Not very well. I need some help.")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Oh dear... what do you need help with?")
                        vanstromHelp(owner)
                    }
                    op("What am I supposed to do again?") {
                        player(CALM_TALK, "What am I supposed to do again?")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "You've forgotten already? Ha! That's funny! But I do understand, my memory isn't what it was! It would be great if you could get some steel")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "weapons and take them to the Myreque, the boatman in Mort'ton should be able to help you find them. But you may need to be a bit persuasive!")
                        goto("vanstromHelp")
                    }
                    op("Ok, thanks.") {
                        player(CALM_TALK, "Ok, thanks.")
                        npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Well, if you have any problems please feel free to come and chat with me.")
                    }
                }
            }
        }
    }
}

private fun DialogueBuilder.acceptMyreque(owner: Player) {
    label("accept")
    options {
        op("What would I have to do?") {
            player(CALM_TALK, "What would I have to do?")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Well, some freedom fighters, called the Myreque, require some weapons.")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "You'd need to get the weapons they require and give them to the freedom fighters.")
            goto("accept")
        }
        op("What weapons do I need to get?") {
            player(CALM_TALK, "What weapons do I need to get?")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Well, I'm sure the following will be fine: a longsword, two swords, a dagger, a mace and a warhammer. All the items should be made of steel.")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "You would also have to fund the costs of this yourself and once you've found them, I'll be sure to give you a nice surprise in return for your help.")
            goto("accept")
        }
        op("Where do I need to take the weapons?") {
            player(CALM_TALK, "Where do I need to take the weapons?")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "I believe there's a boatman in Mort'ton who can show you the way.")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "I'm sure if you use your powers of persuasion you can get some information out of him! The Myreque have a lot of sympathisers.")
            goto("accept")
        }
        op("Yes, I'll do it!") {
            questStart(MYREQUE, accepted = {
                if (owner.isQuestComplete(Quest.NATURE_SPIRIT) && owner.skills.getLevelForXp(Skills.AGILITY) >= 25 && MYREQUE.meetsReqs(owner)) {
                    player(CALM_TALK, "Yes, I'll do it!")
                    npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "That's great news, my friend, really great news! Perhaps the many peoples of Morytania now have an additional hero that they can come to rely upon?")
                    exec { if (owner.myrequeStage == UNSTARTED && MYREQUE.meetsReqs(owner)) owner.advanceMyreque(WEAPONS) }
                } else simple("You need to complete Nature Spirit and have level 25 Agility to start In Search of the Myreque.")
            }, declined = { declineMyreque() })
        }
        op("Sorry, I can't do it!") { declineMyreque() }
    }
}

private fun DialogueBuilder.declineMyreque() {
    player(CALM_TALK, "Sorry, I can't do it!")
    npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Oh. Well, that is a shame, I'm sorry to hear it. But if you change your mind, please visit me again.")
}

private fun DialogueBuilder.vanstromHelp(owner: Player) {
    label("help")
    options {
        op("Where are the Myreque?") {
            player(CALM_TALK, "Where are the Myreque?")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "You know what? I'm not exactly sure. The only thing I've heard, and it is a rumour,")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "but the boatman in Mort'ton might be able to help. But again, it is only a rumour.")
            goto("help")
        }
        op("What weapons am I supposed to get again?") {
            player(CALM_TALK, "What weapons am I supposed to get again?")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Well, I'm sure the following will be fine: a longsword, two swords, a dagger, a mace and a warhammer. All the items should be made of steel.")
            goto("help")
        }
        op("How do you know the 'Myreque'?") {
            player(CALM_TALK, "How do you know the 'Myreque'?")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Actually you know, I don't know them at all! I've never actually met them, but would love to at some point!")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "However, I'm sure that you'll be much more able than I at tracking them down.")
            goto("help")
        }
        op("Is there anyone else who can help me?") {
            player(CALM_TALK, "Is there anyone else who can help me?")
            npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Hmm. Apart from the boatman at Mort'ton, I have no clue, my friend!")
            goto("help")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

internal fun Player.talkMyrequeMember(id: Int) {
    val owner = this
    startConversation {
        if (owner.myrequeStage >= VICTORY) {
            when (id) { Npc.npc_1572 -> mourningRadigad(owner); Npc.npc_1573 -> mourningPolmafi(owner); Npc.route_ivan_strom -> mourningIvan(owner) }
        } else if (owner.myrequeStage < INTRODUCTIONS) {
            when (id) { Npc.route_sani_piliu -> beforeSani(owner); Npc.route_harold_evans -> beforeHarold(owner); Npc.npc_1572 -> beforeRadigad(owner); Npc.npc_1573 -> beforePolmafi(owner); Npc.route_ivan_strom -> beforeIvan(owner) }
        } else {
            when (id) {
                Npc.route_sani_piliu -> if (owner.myreque.getB("met$id")) repeatSani(owner) else introduceSani(owner)
                Npc.route_harold_evans -> if (owner.myreque.getB("met$id")) repeatHarold(owner) else introduceHarold(owner)
                Npc.npc_1572 -> if (owner.myreque.getB("met$id")) repeatRadigad(owner) else introduceRadigad(owner)
                Npc.npc_1573 -> if (owner.myreque.getB("met$id")) repeatPolmafi(owner) else introducePolmafi(owner)
                Npc.route_ivan_strom -> if (owner.myreque.getB("met$id")) repeatIvan(owner) else introduceIvan(owner)
            }
            exec {
                if (owner.myrequeStage == INTRODUCTIONS) owner.myreque.setB("met$id", true)
                owner.startConversation { when (id) { Npc.route_sani_piliu -> questionsSani(owner); Npc.route_harold_evans -> questionsHarold(owner); Npc.npc_1572 -> questionsRadigad(owner); Npc.npc_1573 -> questionsPolmafi(owner); Npc.route_ivan_strom -> questionsIvan(owner) } }
            }
        }
    }
}

internal fun Player.talkVeliaf() {
    val owner = this
    startConversation {
        when {
            owner.myrequeStage < HIDEOUT -> beforeVeliaf(owner)
            owner.myrequeStage == HIDEOUT -> {
                beforeVeliaf(owner)
                exec { if (owner.myrequeStage == HIDEOUT) owner.advanceMyreque(INTRODUCTIONS); owner.startConversation { questionsVeliaf(owner) } }
            }
            owner.myrequeStage == INTRODUCTIONS && !owner.metAllMyreque() -> {
                npc(Npc.route_veliaf_hurtz, CALM_TALK, "Have you introduced yourself to the rest of the gang?")
                options {
                    op("I wanted to ask you some questions first.") {
                        player(CALM_TALK, "I wanted to ask some questions first.")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "Well, ok, but only briefly, I have things to do.")
                        questionsVeliaf(owner)
                    }
                    op("Who else do I have to introduce myself to?") {
                        MEMBERS.entries.chunked(3).forEach { members ->
                            simple(members.joinToString("<br>") { (id, name) -> if (owner.myreque.getB("met$id")) "<str>$name</str>" else name })
                        }
                    }
                    op("Not yet, I'll go do that now.") { player(CALM_TALK, "Not yet, I'll go do that now."); npc(Npc.route_veliaf_hurtz, CALM_TALK, "OK, great.") }
                }
            }
            owner.myrequeStage == INTRODUCTIONS -> {
                npc(Npc.route_veliaf_hurtz, CALM_TALK, "Hello again... so, you've introduced yourself to the team, have you? Good. Now, let's have a look at those weapons you brought us.")
                if (!owner.hasMyrequeWeapons()) {
                    npc(Npc.route_veliaf_hurtz, CALM_TALK, "Well, you don't seem to have what we need in your inventory. We really need specific weapons, we each have our own specialisms.")
                    npc(Npc.route_veliaf_hurtz, CALM_TALK, "If you're wielding one of the weapons which you want us to have, probably best to un-equip it and then come and have a chat.")
                    options {
                        op("Ok, I'll go get them.") { player(CALM_TALK, "Ok, I'll go get them."); npc(Npc.route_veliaf_hurtz, CALM_TALK, "Ok, I'd appreciate that.") }
                        op("Which weapons do you need?") {
                            player(CALM_TALK, "Which weapons do you need?")
                            npc(Npc.route_veliaf_hurtz, CALM_TALK, "Here, I've compiled a list.")
                            simple("1 steel longsword<br>2 steel swords<br>1 steel dagger")
                            simple("1 steel mace<br>1 steel warhammer")
                            npc(Npc.route_veliaf_hurtz, CALM_TALK, "Mind you, I've only checked your main inventory. If you're wielding one of those weapons, I won't take that into account.")
                        }
                    }
                } else exec { startMyrequeAmbush(owner) }
            }
            owner.myrequeStage >= VICTORY -> {
                npc(Npc.route_veliaf_hurtz, CALM_TALK, "That murderer Vanstrom has killed Sani and Harold! He came in and killed them right in front of our eyes.")
                npc(Npc.route_veliaf_hurtz, CALM_TALK, "And that beast he summoned probably would have killed the rest of us if it wasn't for you!")
                player(CALM_TALK, "It was the least I could do! After all, I was the one he followed here, I'm sort of responsible.")
                npc(Npc.route_veliaf_hurtz, CALM_TALK, "That is for your conscience to bear, but now perhaps you can understand why we fight for our freedom?")
                player(CALM_TALK, "Yes, I think I understand now.")
                label("afterAttack")
                options {
                    op("I want to join your organisation.") { player(CALM_TALK, "I want to join your organisation."); npc(Npc.route_veliaf_hurtz, CALM_TALK, "Hmm, well, thanks for your support. But I'll need to talk to my superior first."); goto("afterAttack") }
                    op("What do you plan to do now?") {
                        player(CALM_TALK, "What do you plan to do now?")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "Well, I guess Vanstrom will expect that we're dead. He's so arrogant that he doesn't really consider us a threat.")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "I can understand that; if I was that powerful I would probably feel the same.")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "I guess we'll regroup and then try to recruit more people.")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "We first need to know how we can fight against these monsters, then we'll take the war to them!")
                        goto("afterAttack")
                    }
                    op("How do I get out of here?") {
                        player(CALM_TALK, "How do I get out of here?")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "If you go back into the main corridor which leads into this room, it goes down towards the basement of an inn.")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "If you search the wall, you'll find that it is in fact moveable. You should be able to get into the room,")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "take the ladder to come out to the south of the 'Hair of the Dog Tavern' in Canifis.")
                        npc(Npc.route_veliaf_hurtz, CALM_TALK, "While you're there, you could see if that murderer Vanstrom is still hanging around.", extraFunctionality = { if (owner.myrequeStage == VICTORY) owner.advanceMyreque(EXIT) })
                        goto("afterAttack")
                    }
                    op("Let's get revenge on Vanstrom!") { player(CALM_TALK, "Let's get revenge on Vanstrom!"); npc(Npc.route_veliaf_hurtz, CALM_TALK, "Yes, of course, we'll certainly get our revenge on him and his superiors! Then they'll taste the bitter sorrow that we've had to endure so long!"); goto("afterAttack") }
                    op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
                }
            }
        }
    }
}

private data class CurpileQuestion(val question: String, val answers: List<String>, val correct: Int)
private val CURPILE_QUESTIONS = listOf(
    CurpileQuestion("Name the only female member of the Myreque.", listOf("Sani Piliu.", "Santi Peliou.", "Sani Peridou.", "Sandi Pherimou.", "Don't know!"), 0),
    CurpileQuestion("What family is rumoured to rule Morytania?", listOf("Drunken.", "Drakan.", "Draynor.", "Praymore.", "I don't know!"), 1),
    CurpileQuestion("What is the boatman's name?", listOf("Geof Paddleman.", "Cyreg Paddlebone.", "Gyrec Paddlehorn.", "Cyreg Paddlehorn.", "I don't know!"), 3),
    CurpileQuestion("Which member of the Myreque was originally a scholar?", listOf("Sani Piliu.", "Harold Evans.", "Radigad Ponfit.", "Polmafi Ferdygris.", "Don't know!"), 3),
    CurpileQuestion("Who is the leader of the Myreque?", listOf("Sani Piliu.", "Harold Evans.", "Veliaf Hurtz.", "Radigad Ponfit.", "Don't know!"), 2),
    CurpileQuestion("Who is the youngest member of the Myreque?", listOf("Sani Piliu.", "Ivan Strom.", "Veliaf Hurtz.", "Radigad Ponfit.", "Don't know!"), 1)
)

private fun Player.talkCurpile() {
    val owner = this
    startConversation {
        if (owner.myrequeStage >= HIDEOUT) { npc(Npc.npc_1568, CALM_TALK, "Hey, leave me be. I'm guardin' this place."); return@startConversation }
        npc(Npc.npc_1568, CALM_TALK, "Hey, what're you doin' here?")
        label("curpile")
        options {
            op("I've come to help the Myreque. I've brought weapons.") {
                player(CALM_TALK, "I've come to help the Myreque. I've brought weapons.")
                if (owner.myrequeStage < BOAT || !owner.myreque.getB("travelled") || !owner.hasMyrequeWeapons()) npc(Npc.npc_1568, CALM_TALK, "It doesn't look as if you have the right weapons to me. Get out of here, you fake.")
                else {
                    npc(Npc.npc_1568, CALM_TALK, "Okay, I see ya got da weapons, but how'd I know you're not gonna use 'em against my friends?")
                    player(CALM_TALK, "But I just want to help deliver these weapons.")
                    npc(Npc.npc_1568, CALM_TALK, "Well, dat's as maybe, and I'm not doubting your sincerity here, you seems all sincered up to me.")
                    npc(Npc.npc_1568, CALM_TALK, "It's choking me up right here, you're making me cry, but hey, I's godda do my job or da kids don't get fed!")
                    npc(Npc.npc_1568, CALM_TALK, "Okay, so say I asks you a few questions and you were to answer them all correct and so on; well,")
                    npc(Npc.npc_1568, CALM_TALK, "that'd make me believe yous. I'd get to feeling that you was the real deal an' all. How's dat sound?")
                    player(CALM_TALK, "Sounds fine to me. Go ahead, shoot!")
                    npc(Npc.npc_1568, CALM_TALK, "Hey...don't tempt me! You's dicing wiv death here, my friend.")
                    exec { curpileQuiz(owner, CURPILE_QUESTIONS.shuffled().take(3), 0, 0) }
                }
            }
            op("I don't have to answer to you!") {
                player(CALM_TALK, "I don't have to answer to you!")
                npc(Npc.npc_1568, CALM_TALK, "You do if you know what's good for you!")
                simple("Curpile punches you.", extraFunctionality = { owner.applyHit(Hit.flat(null, 30)) })
            }
            op("No reason.") { player(CALM_TALK, "No reason."); npc(Npc.npc_1568, CALM_TALK, "Come on... speak up! I may be able to help you. However, you's gotta know that you's trespassing and that,")
            npc(Npc.npc_1568, CALM_TALK, "by rights, I should slap you around a bit. You know: just like as a warning."); goto("curpile") }
            op("I'm lost, can you show me the way out?") { player(CALM_TALK, "I'm lost, can you show me the way out?"); npc(Npc.npc_1568, CALM_TALK, "You got yourself in here, now get yourself out. And quickly, if I were you."); goto("curpile") }
            op("I have to go.") { player(CALM_TALK, "I have to go."); npc(Npc.npc_1568, CALM_TALK, "Sure... be on your way, stranger.") }
        }
    }
}

private fun curpileQuiz(owner: Player, questions: List<CurpileQuestion>, index: Int, score: Int) {
    owner.startConversation {
        if (index == 3) {
            if (score == 3) {
                npc(Npc.npc_1568, CALM_TALK, "Okay, I believes ya, you can go on.")
                player(CALM_TALK, "What's the combination to the door?")
                npc(Npc.npc_1568, CALM_TALK, "Oh, there isn't one. I'll unlock it for you.")
                exec { if (owner.myrequeStage == BOAT && owner.hasMyrequeWeapons() && owner.myreque.getB("travelled")) owner.advanceMyreque(HIDEOUT) }
            } else {
                npc(Npc.npc_1568, CALM_TALK, when (score) { 0 -> "I'm sorry, but you got them all wrong! Go on, get outta here."; 1 -> "I'm sorry, but you only got one question correct! Go on, get outta here."; else -> "I'm sorry, but you only got 2 questions correct! Go on, get outta here." })
                exec { owner.myrequeTravel(Tile.of(3521, 3284, 0), "Curpile knocks you out. You wake up back in Mort'ton.") }
            }
        } else {
            val question = questions[index]
            npc(Npc.npc_1568, CALM_TALK, "Okay, ${listOf("first", "second", "third and final")[index]} question. ${question.question}")
            options(question.question) {
                question.answers.forEachIndexed { answer, text -> op(text) {
                    npc(Npc.npc_1568, CALM_TALK, listOf("Okay, interesting answer. First question answered.", "An interesting response. Second question answered.", "Hmm, a calculated retort. Third question answered.")[index])
                    exec { curpileQuiz(owner, questions, index + 1, score + if (answer == question.correct) 1 else 0) }
                } }
            }
        }
    }
}
