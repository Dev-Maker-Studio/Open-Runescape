package com.rs.game.content.quests.bigchompybirdhunting.dialogues

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.bigchompybirdhunting.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapRantzBigChompyBirdHunting() {
    onNpcClick(Npc.npc_8659, options = arrayOf("Talk-to")) { (player, npc) ->
        rantzDialogue(player, npc)
    }
}

private fun rantzDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING)) {
            STAGE_UNSTARTED -> startQuest(player, npc)
            STAGE_MAKING_OGRE_ARROWS -> makingOgreArrows(player, npc)
            STAGE_GETTING_BLOATED_TOAD,
            STAGE_SHOWING_BLOATED_TOAD -> bloatedToad(player, npc)
            STAGE_BAITING_CHOMPY -> baitingChompy(npc)
            STAGE_RANTZ_FAILED_HUNT -> rantzFailedHunt(player, npc)
            STAGE_HUNTING_CHOMPY -> huntingChompy(player, npc)
            STAGE_COOKING_CHOMPY -> cookingChompy(player, npc)
            STAGE_COMPLETE -> postQuest(player, npc)
            else -> npc(npc, CALM_TALK, "Hey you creature!")
        }
    }
}

private fun DialogueBuilder.startQuest(player: Player, npc: NPC) {
    npc(npc, CALM_TALK, "Hey you creature! Make me some stabbers! I wanna hunt da chompy?")
    options {
        op("What are 'stabbers'?") {
            player(CALM_TALK, "What are stabbers?")
            npc(npc, CALM_TALK, "For da stabbie chucker, I's wanna hunt da chompy! Creature knows what Rantz wants... ...flyin' to stabbie da chompy!")
            simple("The ogre shows you a huge but crude bow and then starts to nod energetically in an effort to help you understand.")
            player(CALM_TALK, "I think I understand. You want me to make some arrows for you?")
            npc(npc, CALM_TALK, "Yeah, is what Rantz sayed, make da stabbers for da stabby chucker!")
            goto("questOffer")
        }
        op("What's a 'chompy'?") {
            player(CALM_TALK, "What's a 'chompy'?")
            npc(npc, CALM_TALK, "Da chompy is der bestest yummies for Rantz, Fycie and Bugs! We's looking for da yummies all da time. Da chompy is a big flapper, Rantz want's stabbers to sneaky, sneaky, stick da chompy.")
            player(CALM_TALK, "Ah, so 'da chompy' is some kind of bird?")
            npc(npc, CALM_TALK, "Yeah, is what Rantz sayed. Da chompy is da big flapper and is bestest yummies. But Rantz needs stabbers to stick da chompy... Will creatures make dem stabbers for us?")
            goto("questOffer")
        }
        op("How do I make the 'stabbers'?") {
            player(CALM_TALK, "How do I make the 'stabbers'?")
            npc(npc, CALM_TALK, "Ahhh, da creature wants to know Rantz secret for stabbers? Rantz not say till da creature says will make da stabbers for us else you Creature steals da chompy from us!")
            goto("questOffer")
        }
    }

    label("questOffer")
    if (!BigChompyBirdHunting.meetsRequirements(player)) {
        simple("You do not meet the requirements to start : 'Big Chompy Bird Hunting'.")
    } else {
        questStart(Quest.BIG_CHOMPY_BIRD_HUNTING)
        player(CALM_TALK, "OK, I'll make you some 'stabbers'.") {
            if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) == STAGE_UNSTARTED &&
                BigChompyBirdHunting.meetsRequirements(player)) {
                resetChompyHunt(player)
                player.setQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING, STAGE_MAKING_OGRE_ARROWS)
            }
        }
        npc(npc, CALM_TALK, "Good you creature, you need sticksies from achey tree and stabbies from dog bones.")
    }
}

private fun DialogueBuilder.makingOgreArrows(player: Player, npc: NPC) {
    npc(npc, CALM_TALK, "Hey you creature... Have you made me da stabbers? I wanna stick da chompy?")
    if (player.inventory.containsItem(Obj.ogre_arrow, 6)) {
        player(CALM_TALK, "Well, yes actually, as you asked so nicely. Here you go! Here's your 'stabbers'.")
        simple("Rantz takes six ogre arrows off you.", extraFunctionality = {
            if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) == STAGE_MAKING_OGRE_ARROWS &&
                player.inventory.containsItem(Obj.ogre_arrow, 6)) {
                player.inventory.deleteItem(Obj.ogre_arrow, 6)
                player.setQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING, STAGE_GETTING_BLOATED_TOAD)
            }
        })
        npc(npc, HAPPY_TALKING, "Ahh, der creature has dem...goodly, goodly. Now us can stick der chompy bird... But da chompy not coming without da fatsy toadies... Godda get der fatsy toadies to get da chompys. Den we put it over de're and sneaky, sneaky stick da chompy.")
        fatsyToadyOptions(npc)
    } else {
        player(CALM_TALK, "Er not exactly?")
        npc(npc, ANGRY, "You do stabbers quick!...quick!.. Or Rantz make stabbers for Rantz and then practice for chompy sticking on creature!")
        options {
            op("How do I make the 'stabbers' again?") {
                player(CALM_TALK, "How do I make the 'stabbers' again?")
                npc(npc, ANGRY, "Grrr creature...you's no good stabber maker! You's make da stabbie bit from da dog bones, and get da sticksies from da Achey tree...simple see! Oh and da flufsies from da flappers as well!")
                player(CALM_TALK, "Oh, so I need logs from the achey tree, bones from a canine...and feathers?")
                npc(npc, HAPPY_TALKING, "Is just what Rantz sayed!")
                simple("The hulking ogre nods excitedly.")
            }
            op("Ok, I'll make the 'stabbers' for you.") {
                player(CALM_TALK, "Ok, I'll make the 'stabbers' for you.")
                npc(npc, CALM_TALK, "Good creature, quickly, hurry bring da stabbers!")
            }
        }
    }
}

private fun DialogueBuilder.fatsyToadyOptions(npc: NPC) {
    label("fatsyToadyOptions")
    options {
        op("How do we make the chompys come?") {
            player(CALM_TALK, "How do we make the chompys come?")
            npc(npc, CALM_TALK, "Chompys love fa fatsy toadies. Toadies get big on der swamp gas and der chompys are licking der lips for em as me is licking lips for da chompy. Da chompys don't like da smaller toadies from nearby swampy. Dey's fussie eaters just like Rantz. Fycie an' Bugs play with toadies and blowers dey's all times making fatsy toadies.")
            goto("fatsyToadyOptions")
        }
        op("What are 'fatsy toadies'?") {
            player(CALM_TALK, "What are 'fatsy toadies'?")
            npc(npc, CALM_TALK, "Fatsy toadies are da chompy burds bestest yumms. But da toadies here are too small for da chompy. You've godda make da toadies big and round!")
            goto("fatsyToadyOptions")
        }
        op("Where do we put the fatsy toadies?") {
            player(CALM_TALK, "Where do we put the fatsy toadies?")
            npc(npc, CALM_TALK, "Over der!")
            simple("The ogre points to a small clearing to da south.")
            npc(npc, CALM_TALK, "Ok creature? You got dat? Over dere by der no tree's place.")
            goto("fatsyToadyOptions")
        }
        op("What do you mean 'sneaky..sneaky, stick da chompy?'") {
            player(CALM_TALK, "What do you mean 'sneaky..sneaky, stick da chompy?'")
            npc(npc, CALM_TALK, "Duh! You creature is a bit stoopid yes? US needs to sneaky, sneaky and stick da chompy! Den we can eat da chompy!")
            goto("fatsyToadyOptions")
        }
        op("Ok, thanks.") {
            player(CALM_TALK, "Ok, thanks.")
        }
    }
}

private fun DialogueBuilder.bloatedToad(player: Player, npc: NPC) {
    npc(npc, CALM_TALK, "Hey you creature, you still here? Da chompy still not coming! We need da fatsy toady to get da chompy, do you got it? Do you got da fatsy toady? Then we can sneaky, sneaky stick da chompy.")
    if (player.inventory.containsItem(Obj.bloated_toad, 1)) {
        player(CALM_TALK, "Yes, I have a 'fatsy toady' for you, here look!")
        simple("You show the bloated toad to Rantz. He nods with approval.")
        npc(npc, HAPPY_TALKING, "Dat's a good fatsy toady, now we's need to put it for da chompy to come.")
        player(CALM_TALK, "Where should I put the 'fatsy toadies'?")
        npc(npc, CALM_TALK, "Over 'dere creature, put da toadies over der!") {
            showChompyClearing(player)
        }
        simple("The ogre points to a clearing to the south.", extraFunctionality = {
            player.packets.sendResetCamera()
            if (player.inventory.containsItem(Obj.bloated_toad, 1))
                player.setQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING, STAGE_BAITING_CHOMPY)
        })
    } else {
        player(CALM_TALK, "No I haven't got the 'fatsy toady' yet!")
        npc(npc, CALM_TALK, "Dat's a pidy...but maybe Rantz can help da creature?")
        fatsyToadyOptions(npc)
    }
}

private fun showChompyClearing(player: Player) {
    val clearing = RANTZ_BAIT_TILE
    val camera = RANTZ_BAIT_TILE.transform(0, 10, 0)
    player.packets.sendCameraLook(clearing, 500, 5, 5)
    player.packets.sendCameraPos(camera, 2500, 5, 5)
    player.tasks.schedule(8) { player.packets.sendResetCamera() }
}

private fun DialogueBuilder.baitingChompy(npc: NPC) {
    npc(npc, CALM_TALK, "Hey you creature, you still here? Da chompy still not coming! We need da fatsy toady to get da chompy, do you got it? Do you got da fatsy toady? Then we can sneaky, sneaky stick da chompy.")
    player(CALM_TALK, "Yes, I know where to put the 'fatsy toady'.")
    npc(npc, CALM_TALK, "Over 'dere creature, put da toadies over der!")
    simple("The ogre points to a clearing to the south.")
}

private fun DialogueBuilder.rantzFailedHunt(player: Player, npc: NPC) {
    player(CALM_TALK, "Hey there, you keep missing the chompy bird.")
    npc(npc, ANGRY, "I knows, I keep missing...because your stabbers are worserer at flying than a dead dog.")
    options {
        op("Oh, keep trying then...you might hit one through pure luck.") {
            player(CALM_TALK, "Oh, keep trying then.. you might hit one through pure luck.")
            npc(npc, ANGRY, "Grrrr.... You lookin' like a chompy!")
        }
        op("Come on, let me have a go...") {
            player(CALM_TALK, "Come on, let me have a go...")
            npc(npc, ANGRY, "No, is Rantz stabby thrower... you too weedy...")
            options {
                op("I'm actually quite strong...please let me try.") {
                    player(CALM_TALK, "I'm actually quite strong... please let me try.")
                    npc(npc, CALM_TALK, "Oh, ok...I lend you other stabby thrower... but creature don't better cry when it hurts itself.")
                    item(Obj.ogre_bow, "Rantz hands over an ogre bow. It's huge! You can barely draw back the string!") {
                        if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) == STAGE_RANTZ_FAILED_HUNT) {
                            player.inventory.addItemDrop(Obj.ogre_bow, 1)
                            player.setQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING, STAGE_HUNTING_CHOMPY)
                        }
                    }
                }
                op("Oh suit yourself, you'll just have to go hungry.") {
                    player(CALM_TALK, "Oh suit yourself, you'll just have to go hungry.")
                    npc(npc, ANGRY, "Or I eat you instead!")
                }
            }
        }
    }
}

private fun DialogueBuilder.huntingChompy(player: Player, npc: NPC) {
    npc(npc, CALM_TALK, "Hey! You! Got da chompy yet?")
    if (!player.containsItem(Obj.ogre_bow) && !player.containsItem(Obj.obj_4827)) {
        item(Obj.ogre_bow, "Rantz lends you another ogre bow.") {
            if (!player.containsItem(Obj.ogre_bow) && !player.containsItem(Obj.obj_4827))
                player.inventory.addItemDrop(Obj.ogre_bow, 1)
        }
    }
    if (player.inventory.containsItem(Obj.raw_chompy, 1)) {
        showRawChompy(player, npc)
    } else {
        player(CALM_TALK, "Not yet!")
        npc(npc, CALM_TALK, "Well hurry up and get some, we is hungry!")
    }
}

private fun DialogueBuilder.showRawChompy(player: Player, npc: NPC) {
    player(CALM_TALK, "Yep, here's your chompy Bird!")
    simple("You show Rantz the freshly plucked chompy carcass.")
    npc(npc, HAPPY_TALKING, "Dat's a great chompy, you musta got a lucky shot wiv da stabbie chucker.")
    npc(npc, HAPPY_TALKING, "Okay's now you's needs to cook da chompy! Slurp! You's can cook it's over der!")
    simple("Rantz points to a nearby spit roast.")
    npc(npc, CALM_TALK, "But's we's particular about our chompy yumms. Me's wants Onion. wiv mine! Fycie and Bugs want something wiv der's as well, go and ask 'em wat dey want.")
    player(CALM_TALK, "What! Now I've gotten the chompy bird, you expect me to cook it as well?")
    npc(npc, CALM_TALK, "Yep, da split's over der! Last time Rantz did yummies, got very bad, did bad things to food...and belly.")
    player(AMAZED, "HUH!") {
        if (player.inventory.containsItem(Obj.raw_chompy, 1))
            player.setQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING, STAGE_COOKING_CHOMPY)
    }
}

private fun DialogueBuilder.cookingChompy(player: Player, npc: NPC) {
    if (player.inventory.containsItem(Obj.cooked_s_chompy, 1)) {
        npc(npc, CALM_TALK, "Hey creature, did you's get da cooked chompy yet? I smelled something cooking and it mades me 'ungry. Hand over da chompy if ya know what's good for ya.")
        player(CALM_TALK, "Yes, here you go, here's your cooked chompy bird.")
        simple("You hand over the cooked chompy bird to Rantz.")
        npc(npc, HAPPY_TALKING, "Hey hey! We got da delicious chompy bird Yay! This looks really tasty as well! Tank's very much for da chompy... Fycie an Bugs like very much da chompy yumms!")
        simple("The family of ogres sit down together", "and enjoy your well cooked chompy bird.")
        player(HAPPY_TALKING, "It's my pleasure!") {
            val attribs = player.questManager.getAttribs(Quest.BIG_CHOMPY_BIRD_HUNTING)
            if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) == STAGE_COOKING_CHOMPY &&
                player.inventory.containsItem(Obj.cooked_s_chompy, 1) &&
                attribs.getB(ASKED_FYCIE_CHOMPY_INGREDIENT) && attribs.getB(ASKED_BUGS_CHOMPY_INGREDIENT)) {
                player.inventory.deleteItem(Obj.cooked_s_chompy, 1)
                player.questManager.completeQuest(Quest.BIG_CHOMPY_BIRD_HUNTING)
            }
        }
        return
    }

    if (player.inventory.containsItem(Obj.raw_chompy, 1)) {
        npc(npc, CALM_TALK, "Hey! You! Got da chompy yet?")
        showRawChompy(player, npc)
        return
    }

    npc(npc, CALM_TALK, "Hey creature, did you's get da cooked chompy yet? I smelled something cooking and it mades me 'ungry. Hand over da chompy if ya know what's good for ya.")
}

private fun DialogueBuilder.postQuest(player: Player, npc: NPC) {
    npc(npc, HAPPY_TALKING, "Creature done good, cooking da chompy. Maybe you wants a free hatsie? Rantz got lots of hatsies for chompy shooters.")
    options {
        op("What are these 'hatsies'?") {
            player(CALM_TALK, "What are these 'hatsies'?")
            npc(npc, CALM_TALK, "Creature stupid? Hatsies to wear on head, make you look good. Huh huh huh.")
            player(CALM_TALK, "Ah, I see, you're offering me hats.")
            npc(npc, CALM_TALK, "Dat's what Rantz said. You want hatsies or not?")
        }
        op("Okay, show me your 'hatsies'.") {
            player(CALM_TALK, "Okay, show me your 'hatsies'.")
            val kills = getChompyKills(player)
            if (kills < CHOMPY_HATS.first().requiredKills) {
                npc(npc, CALM_TALK, "Okay, but Rantz only give hatsies if creature stabbied lotsa chompies. You not get hatsies until you stick more chompies.")
                return@op
            }
            val highestHat = highestEarnedChompyHat(player)
            npc(npc, HAPPY_TALKING, "Ahh, creature sticked $kills chompies! Dat makes creature ${highestHat?.rank ?: "chompy shooter"}!") {
                val hatsGiven = claimChompyHats(player)
                when {
                    hatsGiven > 0 -> player.sendMessage("Rantz gives you $hatsGiven chompy bird ${if (hatsGiven == 1) "hat" else "hats"}.")
                    highestHat != null -> player.sendMessage("Rantz has no new chompy bird hats for you.")
                }
            }
            val nextHat = nextChompyHat(player)
            if (nextHat != null) {
                npc(npc, CALM_TALK, "You stick ${nextHat.requiredKills} chompies, den Rantz got better hatsie for creature.")
            } else {
                npc(npc, HAPPY_TALKING, "Creature sticked lotsa chompies! Rantz got no better hatsie!")
            }
        }
    }
}
