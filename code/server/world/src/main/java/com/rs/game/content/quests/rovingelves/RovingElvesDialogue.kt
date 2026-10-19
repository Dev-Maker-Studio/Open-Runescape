package com.rs.game.content.quests.rovingelves

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.quest.Quest
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapRovingElvesDialogue() {
    onNpcClick(Npc.roving_bowyer, options = arrayOf("Talk-to")) { (player, npc) -> player.talkIslwyn(npc) }
    onNpcClick(Npc.npc_1679, Npc.roving_female_woodelf_temp_1, Npc.roving_female_woodelf_2ops, options = arrayOf("Talk-to", "Enchant")) { (player, npc, option) ->
        if (option == "Enchant" && player.rovingStage == COMPLETE) player.rechargeTeleportSeeds(npc)
        else player.talkEluned(npc)
    }
    onItemOnNpc(Npc.npc_1679, Npc.roving_female_woodelf_temp_1, Npc.roving_female_woodelf_2ops) { (player, item, npc) ->
        when (item.id) {
            Obj.roving_old_consecration_seed, Obj.roving_new_consecration_seed -> player.talkEluned(npc)
            Obj.obj_6103 -> if (player.rovingStage == COMPLETE) player.rechargeTeleportSeeds(npc)
        }
    }
    onNpcClick(Npc.roving_update_female_woodelf, Npc.glo3_ilfeen, options = arrayOf("Talk-to")) { (player, npc) -> player.talkIlfeen(npc) }
    onItemOnNpc(Npc.roving_update_female_woodelf, Npc.glo3_ilfeen) { (player, item, npc) ->
        if (player.rovingStage == COMPLETE) player.chantCrystalItem(npc, item)
    }
}

private fun Player.talkIslwyn(npc: NPC) {
    val owner = this
    startConversation {
        when (rovingStage) {
            UNSTARTED -> {
                player(CALM_TALK, "Hello there.")
                if (roving.getB("knowsRemains")) {
                    npc(npc, CALM_TALK, "Tell me again, what do you know of grandmother's remains?")
                    remains(owner, npc.id, roving.getB("lied"))
                } else {
                    npc(npc, ANGRY, "Leave me be, I have no time for easterners. Between your lot and them gnomes, all you do is take and destroy. No thought for others.")
                    player(CALM_TALK, "...But...")
                    npc(npc, ANGRY, "Save your excuses young one! It was one of your species that disturbed my grandmother's remains. Will she never get the peace she deserves?")
                    player(CONFUSED, "Grandmother?")
                    npc(npc, CALM_TALK, "Yes! Someone took her ashes from her tomb. If it wasn't for them gnomes she'd have been left in peace. But now I can sense her restlessness.")
                    player(CONFUSED, "Gnomes?")
                    npc(npc, ANGRY, "Yes gnomes! One of those little pests took the key to my grandmother's tomb. He must've given it to the human that desecrated the tomb.")
                    if (!canStartRoving()) simple("You must complete Regicide and Waterfall Quest before starting Roving Elves.")
                    else questStart(ROVING, accepted = {
                        player(CALM_TALK, "Was your grandmother's name Glarial?")
                        npc(npc, CONFUSED, "Yes... How did you know that?")
                        options("Do you want to;") {
                            op("Tell the truth?") { remains(owner, npc.id, false) }
                            op("Lie?") { remains(owner, npc.id, true) }
                            op("Leave the old elf be?") { player(CALM_TALK, "Sorry this holds no interest for me, good day.") }
                        }
                    }, declined = {})
                }
            }
            HELP, FIND_SEED -> {
                player(CALM_TALK, "Hello again.")
                npc(npc, CALM_TALK, "Good day. Have you spoken to Eluned yet?")
                if (rovingStage == HELP) {
                    player(CALM_TALK, "No, not yet.")
                    npc(npc, CALM_TALK, "She may be able to help with the consecration of grandmother's new resting place.")
                    player(CALM_TALK, "Ok... I'll see what she has to say.")
                } else if (inventory.containsItem(Obj.roving_old_consecration_seed, 1)) {
                    player(CALM_TALK, "Yes, she told me I must retrieve a crystal seed from Glarial's old tomb. Which I have right here.")
                    npc(npc, CALM_TALK, "Good, you should go and show it to her.")
                    player(CALM_TALK, "OK... Will do.")
                } else {
                    player(CALM_TALK, "Yes, she told me I must retrieve a crystal seed from Glarial's old tomb.")
                    npc(npc, CALM_TALK, "Well I wish I could help but the east lands are no place for my kind.")
                }
            }
            ENCHANTED -> {
                player(CALM_TALK, "Hello there.")
                npc(npc, CALM_TALK, "Good day. I hear you managed to retrieve the crystal seed.")
                player(CALM_TALK, "Indeed I have, all I need to do now is plant it.")
                npc(npc, CALM_TALK, "This is good to hear. Soon I will be able to sleep without my dreams plaguing me in the woken world.")
            }
            PLANTED -> {
                player(CALM_TALK, "Hello Islwyn...")
                npc(npc, HAPPY_TALKING, "You have returned! Thank you for all you have done. Now both me and my grandmother can rest in peace.")
                player(CONFUSED, "How did you know that I had consecrated the tomb?")
                npc(npc, CALM_TALK, "Her restlessness has finally left me. Here - I should give you something for your effort.")
                simple("Islwyn shows you a crystal bow and a crystal shield.")
                npc(npc, CALM_TALK, "Crystal equipment is at its best when it's new and fully charged. These I'm offering you only have a little charge left before they'll revert to seed.")
                player(CONFUSED, "Revert to seed? What do you mean?")
                npc(npc, CALM_TALK, "Ah, young one. It was thousands of years before we fully understood that ourselves. All will be explained if we feel you are ready. Now which one of these crystal creations would you like?")
                options {
                    op("Shields are for wimps! Give me the bow!") { reward(owner, npc.id, true) }
                    op("I don't like running and hiding behind mushrooms. Shield please!") { reward(owner, npc.id, false) }
                }
            }
            COMPLETE -> {
                player(CALM_TALK, "Hello again.")
                npc(npc, CALM_TALK, if (roving.getB("bowReward")) "Ah, good to see you back. Are you after another bow or one of my shields?" else "Ah, good to see you back. Are you after another shield or one of my bows?")
                options("Choose an option:") {
                    op("One of your bows, please.") { buyEquipment(owner, npc.id, true) }
                    op("I'd like to buy a shield.") { buyEquipment(owner, npc.id, false) }
                    op("No, I'm just passing by.") {
                        player(CALM_TALK, "No, I'm just passing by.")
                        if (!owner.isQuestStarted(Quest.MOURNINGS_ENDS_PART_I) && !owner.roving.getB("declinedMourning")) {
                            npc(npc, CALM_TALK, "Well it is good to see you again...")
                            npc(npc, CALM_TALK, "Eluned tells me that Arianwyn is looking for you. Perhaps she knows why.")
                        } else {
                            npc(npc, CALM_TALK, "Well it's good to see you again. I hope all is going well for you?")
                            player(CALM_TALK, "As well as can be expected.")
                        }
                    }
                }
            }
        }
    }
}

private fun DialogueBuilder.remains(owner: Player, id: Int, lied: Boolean) {
    player(CALM_TALK, if (lied) "I overheard someone in the pub talking about her, they talked of leaving her remains in the home of a guy called Baxtorian, under a waterfall." else "It's a bit of a long tale, but to cut the story short, her remains reside in Baxtorian's home. I thought it's where she'd want to be. It was I that removed your grandmother's ashes.")
    npc(id, CALM_TALK, if (lied) "Really... Grandfather's home? That's where we wanted to leave Glarial's ashes to rest, but we could never work out how to enter... This is no good... Her resting place must be consecrated." else "You've been in grandfather's home? That's where we originally wanted to leave Glarial's ashes to rest, but we could not understand how to enter.")
    if (!lied) npc(id, CALM_TALK, "This is gravely concerning... Her resting place must be consecrated.")
    exec { owner.roving.setB("knowsRemains", true); owner.roving.setB("lied", lied) }
    options {
        op("Maybe I could help.") {
            player(CALM_TALK, "Maybe I could help. What needs doing to consecrate her new tomb?")
            npc(id, AMAZED, "Are you offering to help?!? Maybe not all humans are as bad as I thought.")
            npc(id, CALM_TALK, "I don't know the consecration process. You should speak with Eluned... she is wise in the ways of the ritual.")
            player(CALM_TALK, "I'll see what I can do.")
            exec { if (owner.rovingStage == UNSTARTED && owner.canStartRoving()) owner.setQuestStage(ROVING, HELP) }
        }
        op("Sounds like you've got a lot to do.") { player(CALM_TALK, "Sounds like you've got a lot to do. I'll leave you to it.") }
    }
}

private fun DialogueBuilder.reward(owner: Player, id: Int, bow: Boolean) {
    if (!owner.inventory.hasFreeSlots()) npc(id, CALM_TALK, "Oh, you don't have space to take it. Make some room and talk to me again.")
    else {
        player(CALM_TALK, if (bow) "Shields are for wimps! Give me the bow!" else "I don't like running and hiding behind mushrooms. Shield please!")
        npc(id, CALM_TALK, "Alright. If you ever need a replacement I can sell you another.")
        exec {
            if (owner.rovingStage == PLANTED && owner.inventory.hasFreeSlots()) {
                owner.tempAttribs.setB("rovingBowReward", bow)
                owner.questManager.completeQuest(ROVING)
            }
        }
    }
}

private fun Player.talkEluned(npc: NPC) {
    val owner = this
    startConversation {
        when (rovingStage) {
            UNSTARTED -> {
                player(CALM_TALK, "Hello.")
                npc(npc, CALM_TALK, "Hello, excuse my friend. He has bad history with humans.")
                player(CALM_TALK, "No problem.")
                npc(npc, CALM_TALK, "Is there anything I can help you with.")
                player(CALM_TALK, "Not right now, thank you.")
            }
            HELP -> {
                player(CALM_TALK, "Hey there... Islwyn said you may be able to help me. He told me you know how to consecrate ground for an elven burial. I need to reconsecrate Glarial's resting place.")
                npc(npc, CALM_TALK, "That I do... It is elven tradition to plant a specially enchanted crystal seed at the graves of our ancestors. The seed will create guardians to protect the area.")
                npc(npc, CALM_TALK, "Unfortunately, the crystal seed must be attuned to the person it's protecting. A new seed won't do, but you should be able to recover the seed from her old tomb.")
                npc(npc, CALM_TALK, "The tomb's guardian will be protecting the seed, you'll need to defeat him to get it. Once you have it, return here and I will re-enchant it.")
                player(CALM_TALK, "Okay, I'll be back as soon as I have it.")
                exec { if (rovingStage == HELP) setQuestStage(ROVING, FIND_SEED) }
            }
            FIND_SEED -> {
                player(CALM_TALK, "Hello.")
                npc(npc, CALM_TALK, "Hello. Any luck finding the consecration seed?")
                if (inventory.containsItem(Obj.roving_old_consecration_seed, 1)) {
                    player(CALM_TALK, "Yes, I have it here.")
                    simple("You hand the crystal seed to Eluned.")
                    simple("Eluned's lips move silently as she chants over the seed.")
                    player(CONFUSED, "How odd. I can see her lips moving, but there's no sound.")
                    exec {
                        if (rovingStage == FIND_SEED && inventory.containsItem(Obj.roving_old_consecration_seed, 1)) {
                            inventory.deleteItem(Obj.roving_old_consecration_seed, 1)
                            inventory.addItem(Obj.roving_new_consecration_seed, 1)
                            setQuestStage(ROVING, ENCHANTED)
                        }
                    }
                    item(Obj.roving_new_consecration_seed, "Eluned hands you an enchanted crystal seed.")
                    npc(npc, CALM_TALK, "Plant this seed in Glarial's new tomb close to her remains and she will rest in peace.")
                    player(CALM_TALK, "Okay.")
                } else {
                    player(CALM_TALK, "No, not yet.")
                    npc(npc, CALM_TALK, "Well, bring it here as soon as you find it, then I can re-enchant it.")
                    player(CALM_TALK, "Okay. I'll see what I can do.")
                }
            }
            ENCHANTED -> {
                if (!containsItem(Obj.roving_new_consecration_seed)) {
                    player(CALM_TALK, "Hello. I lost the crystal seed.")
                    npc(npc, CALM_TALK, "Not to worry. Now I know the harmonic of Glarial's crystal I can reproduce another one.")
                    simple("Eluned's lips move silently as she chants.")
                    player(CONFUSED, "How odd. I can see her lips moving, but there's no sound.")
                    exec {
                        if (rovingStage == ENCHANTED && !containsItem(Obj.roving_new_consecration_seed)) {
                            if (inventory.hasFreeSlots()) inventory.addItem(Obj.roving_new_consecration_seed, 1)
                            else sendMessage("You need a free inventory space for the enchanted seed.")
                        }
                    }
                    npc(npc, CALM_TALK, "Plant this seed in Glarial's new tomb close to her remains and she will rest in peace.")
                } else {
                    player(CALM_TALK, "Hello.")
                    npc(npc, CALM_TALK, "Hey, how's it going? Have you managed to reconsecrate Glarial's resting place?")
                    player(CALM_TALK, "No, not yet.")
                    npc(npc, CALM_TALK, "I'm sure her family will appreciate your effort. Plant the seed in Glarial's new tomb close to her remains and she will rest in peace.")
                    player(CALM_TALK, "Well I'll go finish the job then. See you later.")
                }
            }
            PLANTED -> {
                player(CALM_TALK, "Hello.")
                npc(npc, CALM_TALK, "Hey, how's it going? Have you managed to reconsecrate Glarial's resting place?")
                player(CALM_TALK, "Yes, it's done.")
                npc(npc, HAPPY_TALKING, "Well done! You should go see Islwyn, but I'd guess he already knows.")
            }
            COMPLETE -> {
                player(CALM_TALK, "Hi there.")
                npc(npc, HAPPY_TALKING, "Hi! I hear you finished consecrating Glarial's tomb. Well done.")
                player(CALM_TALK, "It was a worthy deed.")
            }
        }
    }
}

private fun DialogueBuilder.buyEquipment(owner: Player, id: Int, bow: Boolean) {
    player(CALM_TALK, if (bow) "One of your bows, please." else "I'd like to buy a shield.")
    npc(id, CALM_TALK, "They're 1,000,000 coins each for new ones.")
    options {
        op("Pay 1,000,000 coins?") { exec { owner.payIslwyn(id, bow) } }
        op("No, thank you.") {}
    }
}

private fun Player.payIslwyn(id: Int, bow: Boolean) {
    if (rovingStage != COMPLETE) return
    if (!inventory.hasFreeSlots()) {
        startConversation { npc(id, CALM_TALK, "Oh, you don't have space to take it. Make some room and talk to me again.") }
        return
    }
    if (!inventory.hasCoins(1000000)) {
        startConversation {
            player(CALM_TALK, "Sorry but I don't have that much.")
            simple("You can not afford that.")
            npc(id, CALM_TALK, "Well sorry, but I can't let it go for anything less.")
            npc(id, CALM_TALK, if (bow) "Oh well... see you later." else "Oh well... never mind then.")
        }
        return
    }
    inventory.removeCoins(1000000)
    inventory.addItem(if (bow) Obj.crystal_bow_new else Obj.crystal_shield_new, 1)
    startConversation {
        player(CALM_TALK, "Here's 1,000,000 coins.")
        simple(if (bow) "You hand over 1,000,000 coins and get a crystal bow in return." else "You hand over 1,000,000 coins and get a crystal shield in return.")
        npc(id, CALM_TALK, if (bow) "Good hunting." else "Look after yourself.")
        player(CALM_TALK, if (bow) "Thanks... goodbye." else "Thank you... good bye.")
    }
}

private fun Player.talkIlfeen(npc: NPC) {
    val owner = this
    startConversation {
        if (rovingStage != COMPLETE) {
            player(CALM_TALK, "Hello.")
            npc(npc, CALM_TALK, "Hello, traveller.")
            return@startConversation
        }
        if (!roving.getB("metIlfeen")) {
            player(CALM_TALK, "Hello.")
            npc(npc, CALM_TALK, "Hey ya, you're the human that helped my friend Islwyn, aren't you? He told me that he'd given you some crystal equipment.")
            player(CALM_TALK, "That's right, why?")
            npc(npc, CALM_TALK, "When it reverts to seed, bring it to me and I'll chant it for you.")
            exec { owner.roving.setB("metIlfeen", true) }
        } else {
            player(CALM_TALK, "Hello again. Are you still offering to chant seeds?")
            npc(npc, CALM_TALK, "I am, but you’ll need your own seed. After I've made you a new bow or shield once, I'll be able to chant your bows and shields back to full charges.")
        }
        if (npc.id == Npc.glo3_ilfeen && owner.isQuestComplete(Quest.PRISONER_OF_GLOUPHRIE) && !owner.roving.getB("askedOaknock")) {
            player(CALM_TALK, "There is something I meant to ask you about. Do you remember Oaknock? He was a gnome inventor.")
            npc(npc, CALM_TALK, "Of course! I remember him from when I was living in the eastern lands.")
            npc(npc, CALM_TALK, "He was just a budding engineer then. I rather liked the young gnome: he was rather loud, but he had a good soul. He was very interested in elven magic.")
            npc(npc, CALM_TALK, "Last time we spoke, not too long ago, he was very interested in illusion magic and how to dispel it. I gave him some advice and a few pieces of equipment to help him.")
            player(CALM_TALK, "I think Oaknock died of old age a few hundred years ago now...")
            npc(npc, CALM_TALK, "I keep forgetting - a hundred years is a long time for you.")
            player(CALM_TALK, "Did you ever give him a seed?")
            npc(npc, CALM_TALK, "Of course. It grew into a saw. I hope he found it useful.")
            player(CALM_TALK, "I think he did! Thanks.")
            simple("Ilfeen smiles serenely.")
            exec { owner.roving.setB("askedOaknock", true) }
        }
        npc(npc, CALM_TALK, "Would you like me to enchant anything? Or I could give you a book about crystal singing?")
        options("What would you like?") {
            op("Enchant something.") { npc(npc, CALM_TALK, "Show me a crystal seed or crystal item, and I'll see what I can do with it.") }
            op("Crystal singing book.") { exec { owner.receiveCrystalBook(npc.id) } }
            op("Nothing, thanks.") {
                npc(npc, CALM_TALK, "Well, good luck.")
                player(CALM_TALK, "Goodbye, Ilfeen.")
            }
        }
    }
}

private fun Player.receiveCrystalBook(id: Int) {
    if (rovingStage != COMPLETE) return
    if (!inventory.hasFreeSlots()) {
        simpleDialogue("You need a free inventory space for the book.")
        return
    }
    inventory.addItem(Obj.roving_crystal_book, 1)
    startConversation {
        player(CALM_TALK, "Could I have the book, please?")
        item(Obj.roving_crystal_book, "Ilfeen hands you a book.")
        player(CALM_TALK, "Thanks, I'll read all about it.")
        npc(id, CALM_TALK, "Well, good luck.")
        player(CALM_TALK, "Goodbye, Ilfeen.")
    }
}

private fun Player.rechargeTeleportSeeds(npc: NPC) {
    val count = inventory.getNumberOf(Obj.obj_6103)
    if (count == 0) { simpleDialogue("You have no uncharged teleport seeds."); return }
    val cost = (750 - tinyCrystalSeedRepairs.coerceIn(0, 5) * 120) * count
    startConversation {
        npc(npc, CALM_TALK, "I can recharge your teleport seeds for $cost coins.")
        options {
            op("Yes, please.") { exec {
                if (rovingStage == COMPLETE && inventory.containsItem(Obj.obj_6103, count) && inventory.hasCoins(cost)) {
                    inventory.deleteItem(Obj.obj_6103, count)
                    inventory.removeCoins(cost)
                    inventory.addItem(Obj.obj_6099, count)
                    repeat(count) { incrementTinyCrystalSeedRepair() }
                } else sendMessage("You need the uncharged seeds and enough money.")
            } }
            op("No, thank you.") {}
        }
    }
}
