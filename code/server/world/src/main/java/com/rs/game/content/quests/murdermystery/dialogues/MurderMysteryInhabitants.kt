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

@ServerStartupEvent
fun mapMurderMysteryInhabitants() {
    onNpcClick(Npc.anna_sinclair, Npc.bob_sinclair, Npc.carol_sinclair, Npc.david_sinclair, Npc.donovan_the_family_handyman, Npc.elizabeth_sinclair, Npc.frank_sinclair, Npc.hobbes_the_butler, Npc.louisa_the_cook, Npc.mary_the_maid, Npc.pierre_the_family_dog_handler, Npc.stanford_the_gardener, options = arrayOf("Talk-to")) { (player, npc) ->
        if (player.getQuestStage(Quest.MURDER_MYSTERY) == STAGE_UNSTARTED)
            return@onNpcClick
        if (player.getQuestStage(Quest.MURDER_MYSTERY) == STAGE_COMPLETE) {
            player.startConversation {
                when (npc.id) {
                    Npc.anna_sinclair, Npc.bob_sinclair, Npc.carol_sinclair, Npc.david_sinclair, Npc.elizabeth_sinclair, Npc.frank_sinclair ->
                        npc(npc.id, CALM_TALK, "Apparently you aren't as stupid as you look.")
                    Npc.donovan_the_family_handyman, Npc.hobbes_the_butler, Npc.louisa_the_cook, Npc.mary_the_maid, Npc.pierre_the_family_dog_handler, Npc.stanford_the_gardener ->
                        npc(npc.id, HAPPY_TALKING, "Thank you for all your help in solving the murder.")
                }
            }
            return@onNpcClick
        }
        if (player.getQuestStage(Quest.MURDER_MYSTERY) == STAGE_SOMETHINGS_AMISS)
            player.setQuestStage(Quest.MURDER_MYSTERY, STAGE_QUESTIONING_INHABITANTS)
        player.startConversation {
            when (npc.id) {
                Npc.anna_sinclair -> anna(player)
                Npc.bob_sinclair -> bob(player)
                Npc.carol_sinclair -> carol(player)
                Npc.david_sinclair -> david(player)
                Npc.donovan_the_family_handyman -> donovan(player)
                Npc.elizabeth_sinclair -> elizabeth(player)
                Npc.frank_sinclair -> frank(player)
                Npc.hobbes_the_butler -> hobbes(player)
                Npc.louisa_the_cook -> louisa(player)
                Npc.mary_the_maid -> mary(player)
                Npc.pierre_the_family_dog_handler -> pierre(player)
                Npc.stanford_the_gardener -> stanford(player)
            }
        }
    }
}

private fun DialogueBuilder.anna(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.anna_sinclair, CALM_TALK, "Oh really? What do you want to know then?")
    options {
        op("Who do you think was responsible?") {
            player(CALM_TALK, "Who do you think was responsible?")
            npc(Npc.anna_sinclair, CALM_TALK, "It was clearly an intruder.")
            player(CALM_TALK, "Well, I don't think it was.")
            npc(Npc.anna_sinclair, CALM_TALK, "It was one of our lazy servants then.") {
                recordMurderMysteryInterview(player, Npc.anna_sinclair)
            }
        }
        op("Where were you when the murder happened?") {
            player(CALM_TALK, "Where were you when the murder happened?")
            npc(Npc.anna_sinclair, CALM_TALK, "In the library. No one else was there so you'll just have to take my word for it.") {
                recordMurderMysteryInterview(player, Npc.anna_sinclair)
            }
        }
        if (hasCriminalThread(player)) {
            op("Do you recognise this thread?") {
                player(CALM_TALK, "Do you recognise this thread?")
                simple("You show Anna the thread from the study.")
                if (player.inventory.containsItem(Obj.murderthreadg, 1))
                    npc(Npc.anna_sinclair, CALM_TALK, "It's some Green thread. It's not exactly uncommon is it? My trousers are made of the same material.")
                else
                    npc(Npc.anna_sinclair, CALM_TALK, "Not really, no. Thread is fairly common.")
                    exec { recordMurderMysteryInterview(player, Npc.anna_sinclair) }
            }
        }
        if (player.getBool(MM_POISON_BUYERS_KNOWN)) {
            op("Why'd you buy poison the other day?") {
                player(CALM_TALK, "Why'd you buy poison the other day?")
                npc(Npc.anna_sinclair, CALM_TALK, "That useless Gardener Stanford has let his compost heap fester. It's an eyesore to the garden! So I bought some poison from a travelling salesman so that I could kill off some of the wildlife living in it.")
                exec {
                    recordMurderMysteryPoisonClaim(player, MM_MURDERER_ANNA)
                    recordMurderMysteryInterview(player, Npc.anna_sinclair)
                }
            }
        }
    }
}

private fun DialogueBuilder.bob(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.bob_sinclair, CALM_TALK, "I suppose I had better talk to you then.")
    options {
        op("Who do you think was responsible?") {
            player(CALM_TALK, "Who do you think was responsible?")
            npc(Npc.bob_sinclair, CALM_TALK, "I don't really care as long as no one thinks it's me. Maybe it was that strange poison seller who headed towards the seers village.") {
                recordMurderMysteryInterview(player, Npc.bob_sinclair)
            }
        }
        op("Where were you when the murder happened?") {
            player(CALM_TALK, "Where were you when the murder happened?")
            npc(Npc.bob_sinclair, CALM_TALK, "I was walking by myself in the garden.")
            player(CALM_TALK, "And can anyone vouch for that?")
            npc(Npc.bob_sinclair, CALM_TALK, "No. But I was.") {
                recordMurderMysteryInterview(player, Npc.bob_sinclair)
            }
        }
        if (hasCriminalThread(player)) {
            op("Do you recognise this thread?") {
                player(CALM_TALK, "Do you recognise this thread?")
                simple("You show him the thread you discovered.")
                if (player.inventory.containsItem(Obj.murderthreadr, 1))
                    npc(Npc.bob_sinclair, CALM_TALK, "It's some red thread. I suppose you think that's some kind of clue? It looks like the material my trousers are made of.")
                else
                    npc(Npc.bob_sinclair, CALM_TALK, "It's some thread. Great clue. No, really.")
                    exec { recordMurderMysteryInterview(player, Npc.bob_sinclair) }
            }
        }
        if (player.getBool(MM_POISON_BUYERS_KNOWN)) {
            op("Why'd you buy poison the other day?") {
                player(CALM_TALK, "Why'd you buy poison the other day?")
                npc(Npc.bob_sinclair, CALM_TALK, "What's it to you anyway?")
                npc(Npc.bob_sinclair, CALM_TALK, "If you absolutely must know, we had a problem with the beehive in the garden, and as all of our servants are so pathetically useless, I decided I would deal with it myself. So I did.")
                exec {
                    recordMurderMysteryPoisonClaim(player, MM_MURDERER_BOB)
                    recordMurderMysteryInterview(player, Npc.bob_sinclair)
                }
            }
        }
    }
}

private fun DialogueBuilder.carol(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.carol_sinclair, CALM_TALK, "Well, ask what you want to know then.")
    options {
        op("Who do you think was responsible?") {
            player(CALM_TALK, "Who do you think was responsible?")
            npc(Npc.carol_sinclair, CALM_TALK, "I don't know. I think it's very convenient that you have arrived here so soon after it happened. Maybe it was you.") {
                recordMurderMysteryInterview(player, Npc.carol_sinclair)
            }
        }
        op("Where were you when the murder happened?") {
            player(CALM_TALK, "Where were you when the murder happened?")
            npc(Npc.carol_sinclair, CALM_TALK, "Why? Are you accusing me of something? You seem to have a very high opinion of yourself I was in my room if you must know, alone.") {
                recordMurderMysteryInterview(player, Npc.carol_sinclair)
            }
        }
        if (hasCriminalThread(player)) {
            op("Do you recognise this thread?") {
                player(CALM_TALK, "Do you recognise this thread?")
                simple("You show Carol the thread found at the crime scene.")
                if (player.inventory.containsItem(Obj.murderthreadr, 1))
                    npc(Npc.carol_sinclair, CALM_TALK, "It's some red thread... it kind of looks like the Same material as my trousers. But obviously it's not.")
                else
                    npc(Npc.carol_sinclair, CALM_TALK, "It's some thread. Sorry, do you have a point here? Or do you just enjoy wasting peoples time?")
                    exec { recordMurderMysteryInterview(player, Npc.carol_sinclair) }
            }
        }
        if (player.getBool(MM_POISON_BUYERS_KNOWN)) {
            op("Why'd you buy poison the other day?") {
                player(CALM_TALK, "Why'd you buy poison the other day?")
                npc(Npc.carol_sinclair, CALM_TALK, "I don't see what on earth it has to do with you, but the drain outside was")
                npc(Npc.carol_sinclair, CALM_TALK, "blocked, and as nobody else here has the intelligence to even unblock a simple drain I felt I had to do it myself.")
                exec {
                    recordMurderMysteryPoisonClaim(player, MM_MURDERER_CAROL)
                    recordMurderMysteryInterview(player, Npc.carol_sinclair)
                }
            }
        }
    }
}

private fun DialogueBuilder.david(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.david_sinclair, CALM_TALK, "And? Make this quick, I have better things to do than be interrogated by halfwits all day.")
    options {
        op("Who do you think was responsible?") {
            player(CALM_TALK, "Who do you think was responsible?")
            npc(Npc.david_sinclair, CALM_TALK, "I don't really know or care. Frankly, the old man deserved to die.")
            npc(Npc.david_sinclair, CALM_TALK, "There was a suspicious red headed man who came to the house the other day selling poison now I think about it. Last I saw he was headed towards the tavern in the Seers village.") {
                recordMurderMysteryInterview(player, Npc.david_sinclair)
            }
        }
        op("Where were you when the murder happened?") {
            player(CALM_TALK, "Where were you when the murder happened?")
            npc(Npc.david_sinclair, CALM_TALK, "That is none of your business. Are we finished now, or are you just going to stand there irritating me with your idiotic questions all day?") {
                recordMurderMysteryInterview(player, Npc.david_sinclair)
            }
        }
        if (hasCriminalThread(player)) {
            op("Do you recognise this thread?") {
                player(CALM_TALK, "Do you recognise this thread?")
                simple("You show him the thread you found on the study window")
                npc(Npc.david_sinclair, CALM_TALK, "No. Can I go yet? Your face irritates me.") {
                    recordMurderMysteryInterview(player, Npc.david_sinclair)
                }
            }
        }
        if (player.getBool(MM_POISON_BUYERS_KNOWN)) {
            op("Why'd you buy poison the other day?") {
                player(CALM_TALK, "Why'd you buy poison the other day?")
                npc(Npc.david_sinclair, CALM_TALK, "There was a nest of spiders upstairs between the two servants' quarters. Obviously I had to kill them before our pathetic servants whined at my father some more.")
                npc(Npc.david_sinclair, CALM_TALK, "Honestly, it's like they expect to be treated like royalty! If I had my way I would fire the whole workshy lot of them!")
                exec {
                    recordMurderMysteryPoisonClaim(player, MM_MURDERER_DAVID)
                    recordMurderMysteryInterview(player, Npc.david_sinclair)
                }
            }
        }
    }
}

private fun DialogueBuilder.elizabeth(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.elizabeth_sinclair, CALM_TALK, "What's so important you need to bother me with then?")
    options {
        op("Who do you think was responsible?") {
            player(CALM_TALK, "Who do you think was responsible?")
            npc(Npc.elizabeth_sinclair, CALM_TALK, "Could have been anyone. The old man was an idiot. He's been asking for it for years.") {
                recordMurderMysteryInterview(player, Npc.elizabeth_sinclair)
            }
        }
        op("Where were you when the murder happened?") {
            player(CALM_TALK, "Where were you when the murder happened?")
            npc(Npc.elizabeth_sinclair, CALM_TALK, "I was out.")
            player(CALM_TALK, "Care to be any more specific?")
            npc(Npc.elizabeth_sinclair, CALM_TALK, "Not really. I don't have to justify myself to the likes of you, you know. I know the King personally you know. Now are we finished here?") {
                recordMurderMysteryInterview(player, Npc.elizabeth_sinclair)
            }
        }
        if (hasCriminalThread(player)) {
            op("Do you recognise this thread?") {
                player(CALM_TALK, "Do you recognise this thread?")
                simple("You show her the thread from the study window.")
                if (player.inventory.containsItem(Obj.murderthreadb, 1)) {
                    npc(Npc.elizabeth_sinclair, CALM_TALK, "Looks like Blue thread to me. If you can't work that out for yourself I don't hold much hope of you solving this crime.")
                    player(CALM_TALK, "It looks a lot like the material your trousers are made of doesn't it?")
                    npc(Npc.elizabeth_sinclair, CALM_TALK, "I suppose it does. So what?")
                } else
                    npc(Npc.elizabeth_sinclair, CALM_TALK, "It's some thread. You're not very good at this whole investigation thing are you?")
                    exec { recordMurderMysteryInterview(player, Npc.elizabeth_sinclair) }
            }
        }
        if (player.getBool(MM_POISON_BUYERS_KNOWN)) {
            op("Why'd you buy poison the other day?") {
                player(CALM_TALK, "Why'd you buy poison the other day?")
                npc(Npc.elizabeth_sinclair, CALM_TALK, "There was a nest of mosquitos under the fountain in the garden, which I killed with poison the other day. You can see for yourself if you're capable of managing that, which I somehow doubt.")
                player(CALM_TALK, "I hate mosquitos!")
                npc(Npc.elizabeth_sinclair, CALM_TALK, "Doesn't everyone?")
                exec {
                    recordMurderMysteryPoisonClaim(player, MM_MURDERER_ELIZABETH)
                    recordMurderMysteryInterview(player, Npc.elizabeth_sinclair)
                }
            }
        }
    }
}

private fun DialogueBuilder.frank(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.frank_sinclair, CALM_TALK, "Good for you. Now what do you want?")
    npc(Npc.frank_sinclair, CALM_TALK, "...And can you spare me any money? I'm a little short...")
    options {
        op("Who do you think was responsible?") {
            player(CALM_TALK, "Who do you think was responsible?")
            npc(Npc.frank_sinclair, CALM_TALK, "I don't know.")
            npc(Npc.frank_sinclair, CALM_TALK, "You don't know how long it takes an inheritance to come through do you? I could really use that money pretty soon...") {
                recordMurderMysteryInterview(player, Npc.frank_sinclair)
            }
        }
        op("Where were you when the murder happened?") {
            player(CALM_TALK, "Where were you when the murder happened?")
            npc(Npc.frank_sinclair, CALM_TALK, "I don't know, somewhere around here probably.")
            npc(Npc.frank_sinclair, CALM_TALK, "Could you spare me a few coins? I'll be able to pay you double tomorrow it's just there's this poker night tonight in town...") {
                recordMurderMysteryInterview(player, Npc.frank_sinclair)
            }
        }
        if (hasCriminalThread(player)) {
            op("Do you recognise this thread?") {
                player(CALM_TALK, "Do you recognise this thread?")
                simple("Frank examines the thread from the crime scene.")
                if (player.inventory.containsItem(Obj.murderthreadb, 1))
                    npc(Npc.frank_sinclair, CALM_TALK, "It kind of looks like the same material as my trousers are made of... same colour anyway. Think it's worth anything? Can I have it? Or just some money?")
                else
                    npc(Npc.frank_sinclair, CALM_TALK, "It looks like thread to me, but I'm not exactly an expert. Is it worth something? Can I have it? Actually, can you spare me a few gold?")
                    exec { recordMurderMysteryInterview(player, Npc.frank_sinclair) }
            }
        }
        if (player.getBool(MM_POISON_BUYERS_KNOWN)) {
            op("Why'd you buy poison the other day?") {
                player(CALM_TALK, "Why'd you buy poison the other day?")
                npc(Npc.frank_sinclair, CALM_TALK, "Would you like to buy some? I'm kind of strapped for cash right now, I'll sell it to you cheap, it's hardly been used at all.")
                npc(Npc.frank_sinclair, CALM_TALK, "I just used a bit to clean that family crest outside up a bit. Do you think I can get much money For the family crest, actually? It's cleaned up a bit now.")
                exec {
                    recordMurderMysteryPoisonClaim(player, MM_MURDERER_FRANK)
                    recordMurderMysteryInterview(player, Npc.frank_sinclair)
                }
            }
        }
    }
}

private fun DialogueBuilder.donovan(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.donovan_the_family_handyman, CALM_TALK, "How can I help?")
    servantOptions(
        player,
        Npc.donovan_the_family_handyman,
        responsible = {
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "Oh... I really couldn't say. I wouldn't really want to point any fingers at anybody. If I had to make a guess I'd have to say it was probably Bob though.")
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "I saw him arguing with Lord Sinclair about some missing silverware from the Kitchen. It was a very heated argument.")
        },
        whereabouts = {
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "Me? I was sound asleep here in the servants Quarters. It's very hard work as a handyman around here, There's always something to do!")
        },
        noises = {
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "Hmmm..... No, I didn't, but I sleep very soundly at night.")
            player(CALM_TALK, "So you didn't hear any sounds of a struggle or any barking from the guard dog next to his study window?")
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "Now you mention it, no. It is odd I didn't hear anything like that. But I do sleep very soundly as I said and wouldn't necessarily have heard it if there was any such noise.")
        },
        poison = {
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "Well, I do know Frank bought some poison recently to clean the family crest that's outside.")
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "It's very old and rusty, and I couldn't clean it myself, so he said he would buy some cleaner and clean it himself. He probably just got some from that Poison Salesman who came to the door the other day...")
            npc(Npc.donovan_the_family_handyman, CALM_TALK, "You'd really have to ask him though.")
        }
    )
}

private fun DialogueBuilder.hobbes(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.hobbes_the_butler, CALM_TALK, "How can I help?")
    servantOptions(
        player,
        Npc.hobbes_the_butler,
        responsible = {
            npc(Npc.hobbes_the_butler, CALM_TALK, "Well, in my considered opinion it must be David. The man is nothing more than a bully And I happen to know that poor Lord Sinclair and David had a massive argument in the living")
            npc(Npc.hobbes_the_butler, CALM_TALK, "room about the way he treats the staff, the other day. I did not intend to overhear their conversation, but they were shouting so loudly I could not help but Overhear it. David definitely used the words")
            npc(Npc.hobbes_the_butler, CALM_TALK, "'I am going to kill you!' as well. I think he should be the prime suspect. He has a nasty temper that one.")
        },
        whereabouts = {
            npc(Npc.hobbes_the_butler, CALM_TALK, "I was assisting the cook with the evening meal. I gave Mary His Lordships' dinner, and sent her to take it to him, then heard the scream as she found the body.")
        },
        noises = {
            npc(Npc.hobbes_the_butler, CALM_TALK, "How do you mean 'suspicious'?")
            player(CALM_TALK, "Any sounds of a struggle with Lord Sinclair?")
            npc(Npc.hobbes_the_butler, CALM_TALK, "No, I definitely didn't hear anything like that.")
            player(CALM_TALK, "How about the guard dog barking at all?")
            npc(Npc.hobbes_the_butler, CALM_TALK, "You know, now you come to mention it I don't believe I did. I suppose that is Proof enough that it could not have been an intruder who is responsible.")
        },
        poison = {
            npc(Npc.hobbes_the_butler, CALM_TALK, "Well, I do know that Elizabeth was extremely annoyed by the mosquito nest under the fountain in the garden, and was going to do something about it. I suspect any poison she bought would have been")
            npc(Npc.hobbes_the_butler, CALM_TALK, "enough to get rid of it. A Good job too, I hate mosquitos.")
            player(CALM_TALK, "Yeah, so do I.")
            npc(Npc.hobbes_the_butler, CALM_TALK, "You'd really have to ask her though.")
        }
    )
}

private fun DialogueBuilder.louisa(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.louisa_the_cook, CALM_TALK, "How can I help?")
    servantOptions(
        player,
        Npc.louisa_the_cook,
        responsible = {
            npc(Npc.louisa_the_cook, CALM_TALK, "Elizabeth.")
            npc(Npc.louisa_the_cook, CALM_TALK, "Her father confronted her about her constant petty thieving, and was devestated to find she had stolen a silver needle which had meant a lot to him.")
            npc(Npc.louisa_the_cook, CALM_TALK, "You could hear their argument from Lumbridge!")
        },
        whereabouts = {
            npc(Npc.louisa_the_cook, CALM_TALK, "I was right here with Hobbes and Mary. You can't suspect me surely!")
        },
        noises = {
            npc(Npc.louisa_the_cook, CALM_TALK, "Suspicious? What do you mean suspicious?")
            player(CALM_TALK, "Any sounds of a struggle with an intruder for example?")
            npc(Npc.louisa_the_cook, CALM_TALK, "No, I'm sure I don't recall any such thing.")
            player(CALM_TALK, "How about the guard dog barking at an intruder?")
            npc(Npc.louisa_the_cook, CALM_TALK, "No, I didn't. If you don't have anything else to ask can You go and leave me alone now? I have a lot of cooking to do for this evening.")
        },
        poison = {
            npc(Npc.louisa_the_cook, CALM_TALK, "I told Carol to buy some from that strange poison salesman and clean the drains before they began to smell any worse. She was the one who blocked them in the first place with a load")
            npc(Npc.louisa_the_cook, CALM_TALK, "of beans that she bought for some reason. There were far too many to eat, and they were almost rotten when she bought them anyway! You'd really have to ask her though.")
        }
    )
}

private fun DialogueBuilder.mary(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.mary_the_maid, CALM_TALK, "How can I help?")
    servantOptions(
        player,
        Npc.mary_the_maid,
        responsible = {
            npc(Npc.mary_the_maid, CALM_TALK, "Oh I don't know... Frank was acting kind of funny... After that big argument him and the Lord had the other day by the beehive... so")
            npc(Npc.mary_the_maid, CALM_TALK, "I guess maybe him... but it's really scary to think someone here might have been responsible. I actually hope it was a burglar...")
        },
        whereabouts = {
            npc(Npc.mary_the_maid, CALM_TALK, "I was with Hobbes and Louisa in the Kitchen helping to prepare Lord Sinclair's meal, and then when I took it to his study... I saw... oh, it was horrible... he was....")
            simple("She seems to be on the verge of crying. You decide not to push her anymore for details.")
        },
        noises = {
            npc(Npc.mary_the_maid, CALM_TALK, "I don't really remember hearing anything out of the ordinary.")
            player(CALM_TALK, "No sounds of a struggle then?")
            npc(Npc.mary_the_maid, CALM_TALK, "No, I don't remember hearing anything like that.")
            player(CALM_TALK, "How about the guard dog barking?")
            npc(Npc.mary_the_maid, CALM_TALK, "Oh that horrible dog is always barking at nothing but I don't think I did...")
        },
        poison = {
            npc(Npc.mary_the_maid, CALM_TALK, "I overheard Anna saying to Stanford that if he didn't do something about the state of his compost heap, she was going to.")
            npc(Npc.mary_the_maid, CALM_TALK, "She really doesn't get on well with Stanford. I really have no idea why. You'd really have to ask her though.")
        }
    )
}

private fun DialogueBuilder.pierre(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "How can I help?")
    servantOptions(
        player,
        Npc.pierre_the_family_dog_handler,
        responsible = {
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "Honestly? I think it was Carol.")
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "I saw her in a huge argument with Lord Sinclair in the library the other day. It was something to do with stolen books. She definitely seemed upset enough to have done it afterwards.")
        },
        whereabouts = {
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "I was in town at the Inn. When I got back the house was swarming with guards who told me what had happened. Sorry.")
        },
        noises = {
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "Well, like what?")
            player(CALM_TALK, "Any sounds of a struggle with Lord Sinclair?")
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "No, I don't remember hearing anything like that.")
            player(CALM_TALK, "How about the guard dog barking at all?")
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "I hear him bark all the time. It's one of his favourite things to do. I can't say I did the night of the murder though as I wasn't close enough to hear either way.")
        },
        poison = {
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "Well, I know David said that he was going to do something about the spiders' nest that's between the two servants' quarters upstairs.")
            npc(Npc.pierre_the_family_dog_handler, CALM_TALK, "He made a big deal about it to Mary the Maid, calling her useless and incompetent. I felt quite sorry for her actually. You'd really have to ask him though.")
        }
    )
}

private fun DialogueBuilder.stanford(player: Player) {
    player(CALM_TALK, "I'm here to help the guards with their investigation.")
    npc(Npc.stanford_the_gardener, CALM_TALK, "How can I help?")
    servantOptions(
        player,
        Npc.stanford_the_gardener,
        responsible = {
            npc(Npc.stanford_the_gardener, CALM_TALK, "It was Anna. She is seriously unbalanced. She trashed the garden once then tried to blame it on me! I bet it was her. It's just the kind of thing she'd do!")
            npc(Npc.stanford_the_gardener, CALM_TALK, "She really hates me and was arguing with Lord Sinclair about trashing the garden a few days ago.")
        },
        whereabouts = {
            npc(Npc.stanford_the_gardener, CALM_TALK, "Right here, by my little shed. It's very cosy to sit and think in.")
        },
        noises = {
            npc(Npc.stanford_the_gardener, CALM_TALK, "Not that I remember.")
            player(CALM_TALK, "So no sounds of a struggle between Lord Sinclair and an intruder?")
            npc(Npc.stanford_the_gardener, CALM_TALK, "Not to the best of my recollection.")
            player(CALM_TALK, "How about the guard dog barking?")
            npc(Npc.stanford_the_gardener, CALM_TALK, "Not that I can recall.")
        },
        poison = {
            npc(Npc.stanford_the_gardener, CALM_TALK, "Well, Bob mentioned to me the other day he wanted to get rid of the bees in that hive over there. I think I saw him buying poison")
            npc(Npc.stanford_the_gardener, CALM_TALK, "from that poison salesman the other day. I assume it was to sort out those bees. You'd really have to ask him though.")
        }
    )
}

private fun hasCriminalThread(player: Player): Boolean =
    player.inventory.containsItem(Obj.murderthreadr, 1) ||
        player.inventory.containsItem(Obj.murderthreadg, 1) ||
        player.inventory.containsItem(Obj.murderthreadb, 1)

private fun DialogueBuilder.servantOptions(player: Player, npcId: Int, responsible: DialogueBuilder.() -> Unit, whereabouts: DialogueBuilder.() -> Unit, noises: DialogueBuilder.() -> Unit, poison: DialogueBuilder.() -> Unit) {
    options {
        op("Who do you think is responsible?") {
            player(CALM_TALK, "Who do you think is responsible?")
            responsible()
            exec { recordMurderMysteryInterview(player, npcId) }
        }
        op("Where were you at the time of the murder?") {
            player(CALM_TALK, "Where were you at the time of the murder?")
            whereabouts()
            exec { recordMurderMysteryInterview(player, npcId) }
        }
        op("Did you hear any suspicious noises at all?") {
            player(CALM_TALK, "Did you hear any suspicious noises at all?")
            noises()
            exec { recordMurderMysteryInterview(player, npcId) }
        }
        if (player.getBool(MM_POISON_BUYERS_KNOWN)) {
            op("Do you know why so much poison was bought recently?") {
                player(CALM_TALK, "Do you know why so much poison was bought recently?")
                poison()
            exec { recordMurderMysteryInterview(player, npcId) }
            }
        }
    }
}
