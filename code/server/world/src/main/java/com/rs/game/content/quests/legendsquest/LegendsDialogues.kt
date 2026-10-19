package com.rs.game.content.quests.legendsquest

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.ANGRY
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
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapLegendsDialogues() {
    onNpcClick(Npc.npc_398, Npc.npc_399, options = arrayOf("Talk-to")) { (player, npc) -> legendsGuard(player, npc.id) }
    onNpcClick(Npc.radimus_erkle, options = arrayOf("Talk-to")) { (player, npc) -> if (npc !is OwnedNPC || npc.owner === player) radimusDialogue(player) }
    onNpcClick(Npc.jungleforester_m, Npc.jungleforester_f, options = arrayOf("Talk-to")) { (player, npc) -> foresterDialogue(player, npc.id) }
    onNpcClick(Npc.gujuo, options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcClick
        gujuoDialogue(player)
    }
    onNpcClick(Npc.ungadulu_good, Npc.ungadulu_bad, options = arrayOf("Talk-to", "Attack")) { (player, npc, option) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcClick
        if (option == "Attack") repelUngadulu(player) else ungaduluDialogue(player)
    }
    onNpcClick(Npc.echned_zekin, Npc.boulder_legends, options = arrayOf("Talk-to", "Push")) { (player, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcClick
        if (npc.id == Npc.boulder_legends) pushSourceBoulder(player, npc) else echnedDialogue(player)
    }
    onNpcClick(Npc.viyeldi, options = arrayOf("Talk-to", "Attack")) { (player, npc, option) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcClick
        if (option == "Attack" && player.equipment.weaponId == Obj.deathdagger) killViyeldi(player)
        else viyeldiDialogue(player, option == "Attack")
    }
    onNpcClick(Npc.npc_936, Npc.npc_937, Npc.npc_938, options = arrayOf("Talk-to")) { (player, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcClick
        val won = player.legends.getB("guardian${npc.id}")
        player.startConversation { npc(npc, CALM_TALK, when (npc.id) {
            Npc.npc_936 -> if (won) "You have defeated me in battle, and still you search for the source?" else "Are you ready to have your mettle tested?"
            Npc.npc_937 -> if (won) "You have bested me. Go forth, and discover the source." else "Your destiny awaits, adventurer..."
            else -> if (won) "My duty is done. I hope the source brings you all that you hoped for..." else "Your destiny awaits, adventurer..."
        }) }
    }
    onItemOnNpc(Npc.jungleforester_m, Npc.jungleforester_f) { (player, item, npc) ->
        if (item.id in intArrayOf(Obj.thkaramjamap, Obj.thkaramjamapcomp) && completedForesterMap(player)) foresterTrade(player, npc.id)
        else player.startConversation { npc(npc, CALM_TALK, "Sorry, but I have no use for that.") }
    }
    onItemOnNpc(Npc.gujuo) { (player, item, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onItemOnNpc
        if (item.id == Obj.goldbowl_empty && player.legendsStage >= RESCUE) blessBowl(player)
        else player.startConversation { npc(Npc.gujuo, CALM_TALK, "Sorry, but I don't need that, Bwana.") }
    }
    onItemOnNpc(Npc.ungadulu_good, Npc.ungadulu_bad) { (player, item, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onItemOnNpc
        when (item.id) {
            Obj.book_of_binding -> bindUngadulu(player)
            Obj.holy_water -> soakUngadulu(player)
            Obj.deathdagger -> holyForceDialogue(player)
            else -> player.sendMessage("Nothing interesting happens.")
        }
    }
    onItemOnNpc(Npc.echned_zekin, Npc.boulder_legends) { (player, item, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onItemOnNpc
        when (item.id) {
            Obj.holyforce -> revealEchned(player, true)
            Obj.deathdaggerdone -> revealEchned(player, false)
            Obj.deathdagger -> returnDarkDagger(player)
            else -> echnedDialogue(player)
        }
    }
    onItemOnNpc(Npc.viyeldi) { (player, item, npc) -> if ((npc !is OwnedNPC || npc.owner === player) && item.id == Obj.deathdagger) killViyeldi(player) }
    onItemOnNpc(Npc.radimus_erkle) { (player, item, npc) ->
        if (npc is OwnedNPC && npc.owner !== player) return@onItemOnNpc
        if (item.id in intArrayOf(Obj.thtotempolegift, Obj.thkaramjamapcomp)) radimusDialogue(player)
    }
}

internal fun legendsEligible(player: Player, message: Boolean = true): Boolean {
    val quests = arrayOf(Quest.HEROES_QUEST, Quest.FAMILY_CREST, Quest.SHILO_VILLAGE, Quest.UNDERGROUND_PASS, Quest.WATERFALL_QUEST)
    val ready = player.questManager.questPoints >= 107 && quests.all { player.isQuestComplete(it) }
    if (!ready && message) {
        player.sendMessage("You need 107 quest points and must complete Heroes' Quest, Family Crest, Shilo Village, Underground Pass and Waterfall Quest.")
    }
    return ready
}

internal fun legendsGuard(player: Player, id: Int = Npc.npc_398) {
    if (player.legendsStage > 0) {
        player.startConversation {
            simple("A guard nods at you as you walk past.")
            npc(id, CALM_TALK, "Hope the quest is going well!")
        }
        return
    }
    player.startConversation {
        simple("You approach a nearby guard...")
        npc(id, CALM_TALK, "Yes, how can I help you?")
        options {
            op("What is this place?") { legendsMenu(player, "guardPlace") }
            op("How do I get in here?") {
                player(CALM_TALK, "How do I get in here?")
                npc(id, CALM_TALK, "Well, you'll need to be a legendary citizen of Gielinor.")
                npc(id, CALM_TALK, "If you want to enter the Legends' Guild, you'll be invited to complete a quest. Once you have completed that quest, you'll be a fully fledged member of the Guild.")
                options {
                    op("What is this place?") { legendsMenu(player, "guardPlace") }
                    op("Can I speak to someone in charge?") { legendsMenu(player, "guardCharge") }
                    op("Can I go on the quest?") { legendsMenu(player, "guardQuest") }
                }
            }
            op("Can I speak to someone in charge?") { legendsMenu(player, "guardCharge") }
            op("It's okay, thanks.") { player(CALM_TALK, "It's ok thanks."); npc(id, CALM_TALK, "Very well!") }
        }
        label("guardPlace")
        player(CALM_TALK, "What is this place?")
        npc(id, CALM_TALK, "This is the Legends' Guild!")
        npc(id, CALM_TALK, "Legendary citizens of Gielinor are invited on a quest in order to become members of the guild.")
        options {
            op("Can I go on the quest?") { legendsMenu(player, "guardQuest") }
            op("What kind of quest is it?") { legendsMenu(player, "guardKind") }
        }
        label("guardCharge")
        player(CALM_TALK, "Can I speak to someone in charge?")
        npc(id, CALM_TALK, "Well, Radimus Erkle is the Grand Vizier of the Legends Guild. He's a very busy man. And he'll only talk to those people eligible for the quest.")
        options {
            op("Can I go on the quest?") { legendsMenu(player, "guardQuest") }
            op("What kind of quest is it?") { legendsMenu(player, "guardKind") }
        }
        label("guardKind")
        player(CALM_TALK, "What kind of quest is it?")
        npc(id, CALM_TALK, "Well, to be honest, I'm not really sure. You'll need to talk to Grand Vizier Erkle to find that out.")
        options {
            op("Can I go on the quest?") { legendsMenu(player, "guardQuest") }
            op("Thanks for your help.") {
                player(CALM_TALK, "Thanks for your help.")
                npc(id, CALM_TALK, "You're welcome...")
                simple("The Guard marches off on patrol again.")
            }
        }
        label("guardQuest")
        player(CALM_TALK, "Can I go on the quest?")
        simple("The guard gets out a scroll of paper and starts looking through it.")
        if (!legendsEligible(player, false)) {
            npc(id, CALM_TALK, "I'm very sorry but you need to complete more quests before you can go on this quest. Additionally you need to gain ${(107 - player.questManager.questPoints).coerceAtLeast(0)} more quest points. They don't call it the Legends Guild for nothing you know!")
            options {
                op("Which quests do I need to complete?") {
                    player(CALM_TALK, "Which quests do I need to complete?")
                    npc(id, CALM_TALK, "Well, you'll need to complete the following quests: Heroes' Quest, Family Crest, Shilo Village, Underground Pass and Waterfall Quest.")
                    npc(id, CALM_TALK, "You also need a total of 107 quest points.")
                    npc(id, CALM_TALK, "They don't call it the Legends Guild for nothing you know! Best of luck if you intend to become a member!")
                }
                op("Ok thanks.") { player(CALM_TALK, "Ok, thanks."); npc(id, CALM_TALK, "That's no problem... Best of luck if you intend to become a member!") }
            }
        } else {
            npc(id, CALM_TALK, "Well, it looks as if you are eligible for the quest. Grand Vizier Erkle will give you the details about the quest. You can go and talk to him about it if you like?")
            options {
                op("Who is Grand Vizier Erkle?") {
                    player(CALM_TALK, "Who is Grand Vizier Erkle?")
                    npc(id, CALM_TALK, "He is the head of the Legends Guild. His full name is Radimus Erkle. Would you like to talk to him about the quest?")
                    options {
                        opExec("Yes, I'd like to talk to Grand Vizier Erkle.") { admitToGuild(player, id) }
                        op("Some other time perhaps.") { player(CALM_TALK, "Some other time perhaps?") }
                    }
                }
                opExec("Yes, I'd like to talk to Grand Vizier Erkle.") { admitToGuild(player, id) }
                op("Some other time perhaps.") { player(CALM_TALK, "Some other time perhaps?") }
            }
        }
    }
}

private fun admitToGuild(player: Player, id: Int) {
    if (!legendsEligible(player)) return
    player.startConversation {
        player(CALM_TALK, "Yes, I'd like to talk to Grand Vizier Erkle.")
        npc(id, CALM_TALK, "Ok, very well... You need to go into the building on the left, he's in his study.")
        simple("The guard unlocks the gate and pushes it open for you.")
        npc(id, CALM_TALK, "Good Luck!")
        legendsExec(player) {
            if (legendsEligible(player)) {
                player.legends.setB("admitted", true)
                enterLegendsGate(player)
            }
        }
    }
}

private fun radimusDialogue(player: Player) {
    val stage = player.legendsStage
    if (stage >= TRAINING) {
        if (stage == COMPLETE) player.startConversation { npc(Npc.radimus_erkle, CALM_TALK, "Welcome to the Legends' Guild. Do feel free to explore the rest of the building.") }
        else if (player.y < 3374) player.startConversation { npc(Npc.radimus_erkle, CALM_TALK, "Please, go through to the main Legends' Guild building, and I will join you shortly.") }
        else trainingDialogue(player)
        return
    }
    if (stage == GIFT && player.inventory.containsItems(Obj.thtotempolegift, Obj.thkaramjamapcomp) && player.legends.getI("map") == 7) {
        player.startConversation {
            npc(Npc.radimus_erkle, CALM_TALK, "Well, it looks as if you have something interesting there. Let's have a look!")
            simple("You show the totem pole to Radimus.")
            npc(Npc.radimus_erkle, CALM_TALK, "This is truly amazing...")
            npc(Npc.radimus_erkle, CALM_TALK, "This totem pole will take pride of place in the Legends' Guild, as a reminder of your quest. The legends of the future will surely be inspired by such a feat of craftsmanship.")
            simple("Radimus Erkle orders some guards to take the totem pole into the main hall of the Guild.")
            npc(Npc.radimus_erkle, CALM_TALK, "Well, you've completed the tasks I set you. That map of the Kharazi jungle will be very helpful for future expeditions.")
            legendsExec(player) {
                if (player.legendsStage == GIFT && player.inventory.containsItems(Obj.thtotempolegift, Obj.thkaramjamapcomp)) {
                    player.inventory.deleteItem(Obj.thtotempolegift, 1)
                    player.inventory.deleteItem(Obj.thkaramjamapcomp, 1)
                    player.advanceLegends(TRAINING)
                }
            }
            npc(Npc.radimus_erkle, CALM_TALK, "Congratulations, welcome to the Legends' Guild. Please, go through to the main Legends' Guild building, and I will join you shortly.")
        }
        return
    }
    player.startConversation {
        if (stage == 0) {
            npc(Npc.radimus_erkle, CALM_TALK, "Good day to you, adventurer! No doubt you are keen to become a member of the Legends' Guild?")
            label("radimusStart")
            options {
                op("Yes actually, what's involved?") {
                    player(CALM_TALK, "Yes actually, what's involved?")
                    npc(Npc.radimus_erkle, CALM_TALK, "Well, you'll need to complete a quest for us as proof that you've got what it takes to be a Legend. How do you fancy mapping the Kharazi Jungle? It's the unexplored southern part of Karamja Island.")
                    npc(Npc.radimus_erkle, CALM_TALK, "We'd also like you to befriend a Kharazi tribesman, and bring back a token of the tribe's friendship. The Legends' Guild has allies in all corners of the map, and we could always use more.")
                    npc(Npc.radimus_erkle, CALM_TALK, "What do you say? Are you interested in this quest?")
                    questStart(Quest.LEGENDS_QUEST,
                        accepted = { legendsExec(player) { startLegendsQuest(player) } },
                        declined = {
                            player(CALM_TALK, "Not just at the moment.")
                            npc(Npc.radimus_erkle, CALM_TALK, "Very well, if you change your mind, please come back and see me.")
                        }
                    )
                }
                op("Maybe some other time.") { npc(Npc.radimus_erkle, CALM_TALK, "As you wish...") }
                op("Who are you?") { npc(Npc.radimus_erkle, CALM_TALK, "My name is Radimus Erkle. I am the Grand Vizier of the Legends' Guild. Are you interested in becoming a member?"); goto("radimusStart") }
            }
        } else {
            npc(Npc.radimus_erkle, CALM_TALK, "Hello there, how is the quest going?")
            label("radimusProgress")
            options {
                op("It's alright, but I've forgotten what to do.") {
                    npc(Npc.radimus_erkle, CALM_TALK, "Taken one too many hits to the noggin, have you? Here's what I want you to do.")
                    npc(Npc.radimus_erkle, CALM_TALK, "You need to find a way into the Kharazi Jungle.")
                    npc(Npc.radimus_erkle, CALM_TALK, "Then you need to explore and map the three areas I asked you to.")
                    npc(Npc.radimus_erkle, CALM_TALK, "While you're there you need to make contact with the Kharazi tribe and establish friendly relations.")
                    npc(Npc.radimus_erkle, CALM_TALK, "As proof of all this, I want you to bring back the completed map and a token of the Kharazi's esteem - something we can put on display.")
                    npc(Npc.radimus_erkle, CALM_TALK, "Did you get all that?")
                    goto("radimusProgress")
                }
                if (!player.hasLegendsItem(Obj.thkaramjamap) && !player.hasLegendsItem(Obj.thkaramjamapcomp)) op("Terrible, I lost my map of the Kharazi jungle.") {
                    npc(Npc.radimus_erkle, CALM_TALK, "Oh dear! Well, luckily one of those friendly foresters sent me a copy. But I'll need to charge you a fee of 30 Gold Pieces. The cost of postage these days is horrendous...")
                    options {
                        opExec("Yes, I'll pay for it.") {
                            if (player.inventory.containsItem(Obj.coins, 30) && player.inventory.hasFreeSlots()) {
                                val id = if (player.legends.getI("map") == 7) Obj.thkaramjamapcomp else Obj.thkaramjamap
                                if (player.giveLegendsItem(id)) player.inventory.deleteItem(Obj.coins, 30)
                            } else player.sendMessage("You need 30 coins and a free inventory slot.")
                        }
                        op("No, I won't pay for it.") { npc(Npc.radimus_erkle, CALM_TALK, "Well, that's your decision, of course... but you won't be able to complete the quest without it."); goto("radimusProgress") }
                    }
                }
                op("I need another machete.") { npc(Npc.radimus_erkle, CALM_TALK, "Well, just get another one from the cupboard."); goto("radimusProgress") }
                op("I've run out of charcoal.") { npc(Npc.radimus_erkle, CALM_TALK, "Well, get some more! Have a little initiative, would you?"); npc(Npc.radimus_erkle, CALM_TALK, "It's hardly legendary to fail a quest because you can't find a little bit of burnt wood!"); goto("radimusProgress") }
                op("I've run out of papyrus.") { npc(Npc.radimus_erkle, CALM_TALK, "Well, get some more! Have a little initiative, would you?"); npc(Npc.radimus_erkle, CALM_TALK, "It's hardly legendary to fail a quest because you can't find some papyrus!"); goto("radimusProgress") }
                op("I've completed the quest.") { npc(Npc.radimus_erkle, CALM_TALK, "Excellent! Does this mean you have a suitable gift from the Kharazi? Becoming a legend is more than just fighting you know. It also requires some careful diplomacy and problem solving skills."); npc(Npc.radimus_erkle, CALM_TALK, "I'd like to see the completed map, too."); goto("radimusProgress") }
            }
        }
    }
}

private val TRAINING_SKILLS = listOf(Skills.ATTACK, Skills.DEFENSE, Skills.STRENGTH, Skills.HITPOINTS, Skills.PRAYER, Skills.MAGIC, Skills.WOODCUTTING, Skills.CRAFTING, Skills.SMITHING, Skills.HERBLORE, Skills.AGILITY, Skills.THIEVING)
private fun startLegendsQuest(player: Player) {
    if (player.legendsStage != 0 || !legendsEligible(player)) { player.endConversation(); return }
    if (!player.inventory.hasFreeSlots() || !player.inventory.addItem(Obj.thkaramjamap, 1)) {
        player.endConversation()
        player.sendMessage("You need a free inventory slot for Radimus's notes.")
        return
    }
    player.setQuestStage(Quest.LEGENDS_QUEST, MAPPING)
    player.startConversation {
        player(CALM_TALK, "Yes, it sounds great!")
        npc(Npc.radimus_erkle, CALM_TALK, "Excellent! I've already started mapping the jungle myself. You can take my research to help you along.")
        item(Obj.thkaramjamap, "Radimus Erkle gives you some notes and a map.")
        npc(Npc.radimus_erkle, CALM_TALK, "Complete this map when you get to the Kharazi Jungle. It's in the southernmost part of Karamja.")
        npc(Npc.radimus_erkle, CALM_TALK, "There's three sections I want you to focus on.")
        npc(Npc.radimus_erkle, CALM_TALK, "You'll need additional papyrus and charcoal to complete the map, of course.")
        simple("Radimus shuffles around the back of his desk and gives you a stern look.")
        npc(Npc.radimus_erkle, CALM_TALK, "It is likely to be very tough going. You'll need an axe and machete to cut through the dense Kharazi Jungle, so collect a machete from the cupboard before you leave. You'll need to find your own axe, preferably a good one.")
        npc(Npc.radimus_erkle, CALM_TALK, "Finally, whatever token of friendship you bring back must be fit to display in our hall. I'll know if it's some cheap knock-off, and I will not be best pleased.")
        npc(Npc.radimus_erkle, CALM_TALK, "All that aside, I wish you the very best of luck!")
    }
}

private fun trainingDialogue(player: Player, page: Int = -1) {
    if (player.legendsStage != TRAINING) return
    if (player.legends.getI("training") >= 4) { finishLegendsTraining(player); return }
    if (page < 0) {
        player.startConversation {
            npc(Npc.radimus_erkle, CALM_TALK, "Welcome to the Legends' Guild Main Hall. As you can see, the totem pole you brought us has pride of place. Every member of the Legends' Guild can see it as soon as they come through the doors, and they'll know that it was you who brought it back.")
            npc(Npc.radimus_erkle, CALM_TALK, "Now that you're a fully-fledged member of the guild, I'd like to offer you some training. Shall we get started now?")
            options {
                opExec("Yes, I'll train now.") { trainingDialogue(player, 0) }
                op("No, I've got something else to do at the moment.") { npc(Npc.radimus_erkle, CALM_TALK, "Very well. Return when you are able, but don't leave it too long. You'll benefit considerably from this training.") }
            }
        }
        return
    }
    player.startConversation {
        npc(Npc.radimus_erkle, CALM_TALK, "You can choose ${4 - player.legends.getI("training")} skills to increase your abilities in.")
        options {
            for (skill in TRAINING_SKILLS.drop(page * 3).take(3)) opExec(Skills.SKILL_NAME[skill]) {
                if (player.legendsStage == TRAINING && player.legends.getI("training") < 4) {
                    player.legends.incI("training")
                    player.skills.addXpQuest(skill, 7650.0)
                    player.sendMessage("The training increases your ${Skills.SKILL_NAME[skill]} experience.")
                    if (player.legends.getI("training") == 4) finishLegendsTraining(player)
                    else trainingDialogue(player, page)
                }
            }
            opExec("Go to Skill Menu ${(page + 1) % 4 + 1}") { trainingDialogue(player, (page + 1) % 4) }
        }
    }
}

private fun finishLegendsTraining(player: Player) {
    player.startConversation {
        npc(Npc.radimus_erkle, CALM_TALK, "Right, that's all the training I can offer you. I hope you're happy with your new skills. Do feel free to explore the rest of the building.")
        legendsExec(player) {
            if (player.legendsStage == TRAINING && player.legends.getI("training") >= 4) player.questManager.completeQuest(Quest.LEGENDS_QUEST)
        }
    }
}

private fun foresterDialogue(player: Player, id: Int) {
    if (completedForesterMap(player) && !player.legends.getB("bullroarer")) { foresterTrade(player, id); return }
    player.startConversation {
        npc(id, CALM_TALK, "Hello friend. You're a long way from Lumbridge!")
        label("foresterMenu")
        options {
            op("How do I get into the Kharazi Jungle?") {
                player(CALM_TALK, "How do I get into the Kharazi Jungle?")
                npc(id, CALM_TALK, "Well, I don't recommend just walking in! The last guy I saw try that barely escaped with his life.")
                npc(id, CALM_TALK, "Apparently he was on a mission to map the area. How foolish is that?")
                options {
                    op("Actually, I plan to map the jungle myself.") {
                        player(CALM_TALK, "Actually, I plan to map the jungle myself.")
                        npc(id, CALM_TALK, "Oh. Oops. Sorry if I sounded rude, it just seems like a really bad idea.")
                        npc(id, CALM_TALK, "I mean it's a noble calling. Lots of people around here could use a proper map to navigate with. But, you know... there's something not quite right about that jungle.")
                        npc(id, CALM_TALK, "Hey, if you manage to complete it, be sure to let me take a look! Well, best of luck with it, I'm sure you're going to need it.")
                        goto("foresterMenu")
                    }
                    op("Are you calling me foolish?") { npc(id, CALM_TALK, "Er... maybe? I'll just be on my way...") }
                    op("Okay, thanks.") { npc(id, CALM_TALK, "You're welcome! See you around...") }
                }
            }
            op("What do you do here?") {
                npc(id, CALM_TALK, "I'm a forester, and I specialise in exotic woods. I haven't gone into the Kharazi myself, but I've found some interesting tree specimens out here.")
                npc(id, CALM_TALK, "If you do find a safe path into the jungle, do come and let me know. I'd love to have a nose around in there.")
                goto("foresterMenu")
            }
            op("Have you seen any Kharazi tribespeople in the jungle?") {
                npc(id, CALM_TALK, "Oh yes, I've met loads of them! Lovely lot. I haven't seen many about recently, though.")
                npc(id, CALM_TALK, "If you're looking to say hello, you'll need a Bullroarer. If you swing it above your head it makes a loud noise - that should attract someone's attention.")
                options {
                    op("Do you have a Bullroarer I could borrow?") {
                        npc(id, CALM_TALK, "I wish I did! But I just have the one, and I've grown fond of it. Besides, I might need it if I ever get lost in the jungle.")
                        opTradeOffer(player, id)
                    }
                    op("Okay, thanks.") { npc(id, CALM_TALK, "You're welcome! See you around...") }
                }
            }
            op("Do you have any other tips about the Kharazi Jungle?") { npc(id, CALM_TALK, "Not really. Just be careful - there's a lot of weird stuff going on in there lately.") }
            if (player.legends.getB("bullroarer") && !player.hasLegendsItem(Obj.bullroarer)) op("I've lost my Bullroarer.") {
                npc(id, CALM_TALK, "Bring me an oomlie wrap and I'll give you another bullroarer.")
                legendsExec(player) {
                    if (player.inventory.containsItem(Obj.cooked_oomlie) && player.giveLegendsItem(Obj.bullroarer)) player.inventory.deleteItem(Obj.cooked_oomlie, 1)
                }
            }
            op("Okay, thanks.") { npc(id, CALM_TALK, "You're welcome! See you around...") }
        }
    }
}

private fun DialogueBuilder.opTradeOffer(player: Player, id: Int) {
    options {
        op("Will you trade something for it?") {
            player(CALM_TALK, "Will you trade something for it?")
            npc(id, CALM_TALK, "Hmm, maybe. Tell you what, you show me something interesting and I'll see if it's worth my Bullroarer. How about that?")
            player(CALM_TALK, "It's a deal!")
            legendsExec(player) { if (completedForesterMap(player)) foresterTrade(player, id) }
        }
        op("Okay, thanks.")
    }
}

private fun completedForesterMap(player: Player) = player.legendsStage in MAPPING until TRAINING && player.legends.getI("map") == 7 && player.inventory.containsOneItem(Obj.thkaramjamap, Obj.thkaramjamapcomp)

private fun foresterTrade(player: Player, id: Int) {
    if (!completedForesterMap(player)) return
    player.replaceLegendsItem(Obj.thkaramjamap, Obj.thkaramjamapcomp)
    player.advanceLegends(CONTACT)
    if (player.legends.getB("bullroarer")) {
        player.startConversation {
            simple("You show the completed map of Kharazi Jungle to the Forester.")
            npc(id, CALM_TALK, "This is a great map, thanks for letting me have a copy! I'm excited to explore more of the jungle.")
        }
        return
    }
    player.startConversation {
        simple("You show the completed map of Kharazi Jungle to the Forester.")
        npc(id, CALM_TALK, "This is an impressive map! I'm amazed, it's just great! Do you mind if I make a copy of it? I'll give you my Bullroarer in return.")
        label("trade")
        options {
            opExec("Go ahead, make a copy!") { finishForesterTrade(player, id) }
            op("You'll give me your what in return?") {
                npc(id, CALM_TALK, "My Bullroarer! Look.")
                simple("The Jungle Forester takes out a strange looking object. It looks like a wooden pole, with string attached to one end. At the other end of the string is an odd-shaped piece of wood.")
                npc(id, CALM_TALK, "If you swing it above your head it makes a funny noise. It's great for attracting people's attention!")
                goto("trade")
            }
            op("Sorry, I'm busy questing.") { npc(id, CALM_TALK, "Oh, alright then.") }
        }
    }
}

private fun finishForesterTrade(player: Player, id: Int) {
    if (!completedForesterMap(player) || player.legends.getB("bullroarer")) { player.endConversation(); return }
    if (!player.giveLegendsItem(Obj.bullroarer)) {
        player.endConversation()
        if (player.hasLegendsItem(Obj.bullroarer)) player.legends.setB("bullroarer", true)
        return
    }
    player.legends.setB("bullroarer", true)
    player.startConversation {
        player(CALM_TALK, "Go ahead, make a copy!")
        npc(id, CALM_TALK, "Many thanks, friend.")
        simple("The Jungle Forester takes out a scrap of parchment and some charcoal. ${if (id == Npc.jungleforester_f) "She" else "He"} studiously renders another copy of your map.")
        npc(id, CALM_TALK, "Perfect.")
        item(Obj.bullroarer, "${if (id == Npc.jungleforester_f) "She" else "He"} takes out a strange looking object and hands it to you.")
        npc(id, CALM_TALK, "That's a Bullroarer. Swing it above your head and it'll make a funny noise. It's great for attracting people's attention!")
    }
}

internal fun gujuoDialogue(player: Player, offerBlessing: Boolean = true) {
    if (player.legendsStage < CONTACT) return
    if (player.legendsStage == GIFT && !player.hasLegendsItem(Obj.thtotempolegift)) { giveGildedTotem(player); return }
    if (offerBlessing && player.inventory.containsItem(Obj.goldbowl_empty) && player.legendsStage >= RESCUE) { blessBowl(player); return }
    val sourceExplained = player.legends.getB("sourceExplained") || player.legends.getB("braveryRecipe")
    player.startConversation {
        player(CALM_TALK, "Hello there.")
        npc(Npc.gujuo, CALM_TALK, when {
            player.legendsStage >= GIFT -> "Good day, Bwana. The Kharazi Jungle is especially beautiful today, isn't it?"
            player.legendsStage >= WATER_RESTORED -> "Bwana! You're alive! I'm pleased to see you. Something feels different about the jungle... I suspect it's down to your good deeds."
            player.legendsStage >= DRY_POOL -> if (player.legends.getB("braveryRecipe")) "I have visited Ungadulu in the caves. He's hard at work studying, but he looks well! How is your quest going, Bwana?" else "I have visited Ungadulu in the caves. He's hard at work studying, but he looks well! Have you grown the Yommi tree yet?"
            player.legendsStage == SEEDS -> "How goes your quest to free Ungadulu?"
            player.legendsStage >= RESCUE -> "Bwana! Have you found Ungadulu?"
            else -> "Greetings, Bwana. Why do you make such strange sounds and disturb the peace of the jungle?"
        })
        label("gujuoMenu")
        options {
            if (player.legendsStage == CONTACT) {
                op("I was hoping to attract the attention of a Kharazi tribe member.") {
                    player(CALM_TALK, "I was hoping to attract the attention of a Kharazi tribe member.")
                    npc(Npc.gujuo, CALM_TALK, "Well, it worked! I am Gujuo, proud member of the Kharazi tribe. What did you want to talk about, Bwana?")
                    goto("gujuoMenu")
                }
                op("I've been sent by the Legends' Guild.") {
                    player(CALM_TALK, "I've been sent by the Legends' Guild. We want to develop friendly relations with your people.")
                    simple("Gujuo smiles and shakes your hand warmly.")
                    npc(Npc.gujuo, CALM_TALK, "Very good, Bwana... this is indeed a friendly gesture. However, you've come at a bad time.")
                    npc(Npc.gujuo, CALM_TALK, "My people are scattered and in hiding.")
                    options {
                        op("Can you get everyone together?") {
                            npc(Npc.gujuo, CALM_TALK, "We normally meet regularly around our tribe's totem pole. Unfortunately, ours has been polluted by an evil spirit, and now we are all afraid to approach it.")
                            npc(Npc.gujuo, CALM_TALK, "We send our shaman, Ungadulu, to confront the spirit... but he has not returned.")
                            options {
                                op("Is there any way I could help?") {
                                    npc(Npc.gujuo, CALM_TALK, "We could work together to make a new totem pole - but I'm afraid it won't be easy. We carve them from the trunk of the sacred Yommi tree.")
                                    options {
                                        op("How do we make the totem pole?") {
                                            npc(Npc.gujuo, CALM_TALK, "First we need to plant a sacred Yommi tree. It is a magical tree of great power. Our shaman, Ungadulu, is the only one trusted with the seeds.")
                                            npc(Npc.gujuo, CALM_TALK, "And as I said, he hasn't returned from his mission. He was supposed to be communing with the spirits in some caves in the northwestern part of the jungle.")
                                            options {
                                                op("I'll go find Ungadulu.") {
                                                    npc(Npc.gujuo, CALM_TALK, "You mean it? That would be wonderful, Bwana! He was last seen in the northwestern part of the Kharazi Jungle, near the great cliffs. Look there, and you will see three rocks that form a triangle.")
                                                    npc(Npc.gujuo, CALM_TALK, "They mark the secret entrance to the caves where Ungadulu communes with the spirits. If you rescue him, he will surely entrust you with the Yommi tree seeds, and we can restore our totem.")
                                                    legendsExec(player) { player.advanceLegends(RESCUE) }
                                                    npc(Npc.gujuo, CALM_TALK, "I have to collect herbs now, Bwana...")
                                                }
                                                op("Oh well, sorry to hear about that.") { npc(Npc.gujuo, CALM_TALK, "If my people survive this calamity, it will be a miracle..."); goto("gujuoMenu") }
                                            }
                                        }
                                        op("I'm lost, can you show me the way out?") { gujuoExit(player) }
                                    }
                                }
                                op("I'm lost, can you show me the way out?") { gujuoExit(player) }
                            }
                        }
                        op("I'm lost, can you show me the way out?") { gujuoExit(player) }
                    }
                }
                op("Sorry, it was a mistake?") { npc(Npc.gujuo, CALM_TALK, "That's quite alright, Bwana. But I have to ask... what are you doing in the Kharazi Jungle?"); goto("gujuoMenu") }
            } else if (player.legendsStage < SEEDS) {
                op("I need more advice.") {
                    options {
                        op("I can't find the caves...") {
                            npc(Npc.gujuo, CALM_TALK, "Well, they were still there the last time I checked... Go as far to the west in the Kharazi Jungle as you can, until you reach the sea, then walk eastwards with the barrier rocks to the north.")
                            npc(Npc.gujuo, CALM_TALK, "Walk past the covered pass and you'll find some rocks on slightly elevated ground. You'll find the caves there.")
                            goto("gujuoMenu")
                        }
                        op("How did they summon the spirits?") {
                            npc(Npc.gujuo, CALM_TALK, "I am unlearned in such matters. But I am told of sacred patterns that are scored on the ground to bind the spirit and confine it... But that is all I know.")
                            goto("gujuoMenu")
                        }
                        op("I'm not sure what to do.") {
                            npc(Npc.gujuo, CALM_TALK, "I am at a loss as well Bwana. At this time Ungadulu is the only one who might know of a solution for this puzzle and at this time, he is part of that same puzzle.")
                            goto("gujuoMenu")
                        }
                        op("Back.") { legendsMenu(player, "gujuoMenu") }
                    }
                }
                op("I need some pure water to douse some magic flames.") {
                    player(CALM_TALK, "I need some pure water to douse some magic flames.")
                    npc(Npc.gujuo, CALM_TALK, "This all sounds very odd. You're not possessed, are you Bwana?")
                    player(CALM_TALK, "I don't think so. I need the water to help Ungadulu!")
                    npc(Npc.gujuo, CALM_TALK, "Well, then I suppose there's no harm in telling you. There is a sacred pool, right at the centre of the Kharazi jungle.")
                    npc(Npc.gujuo, CALM_TALK, "The water contains special properties, but they will only be retained if it's stored in a blessed vessel made from the metal of the sun. It's no easy task, but I'm sure an adventurer like you could manage it.")
                    vesselOptions(player)
                }
                op("Ungadulu looks a little strange.") {
                    npc(Npc.gujuo, CALM_TALK, "Be wary, Bwana. The jungle is full of strange and powerful spirits. You may have been tricked...")
                    npc(Npc.gujuo, CALM_TALK, "Our forefathers summoned many strange spirits, in an attempt to learn their wisdom. But something went wrong. Now they haunt the caves around this area, and make our lives miserable. Only the magic of the totem keeps them at bay.")
                    goto("gujuoMenu")
                }
                op("Ungadulu called me 'Vacu'. What does that mean?") {
                    npc(Npc.gujuo, CALM_TALK, "It seems that Ungadulu has started to lose his senses. In the days of our forefathers, the 'Vacu' were shamans who could summon powerful, wise spirits.")
                    npc(Npc.gujuo, CALM_TALK, "But they asked too much, and were enslaved by the spirits they sought to command.")
                    player(CALM_TALK, "How terrible...")
                    npc(Npc.gujuo, CALM_TALK, "Indeed. Every Kharazi child knows not to mess with evil spirits. What has our shaman gotten himself into?")
                }
            } else if (player.legendsStage >= GIFT) {
                op("Do you have any news?") {
                    player(CALM_TALK, "Do you have any news?")
                    npc(Npc.gujuo, CALM_TALK, "None at all! Everything is fine now, thanks to you.")
                    goto("gujuoMenu")
                }
                op("Where are all your people?") {
                    player(CALM_TALK, "Where are all your people?")
                    npc(Npc.gujuo, CALM_TALK, "The evil spirits you vanquished have haunted this jungle for many years. It will take some time for my people to fully trust strangers - even strangers as famous as you!")
                    npc(Npc.gujuo, CALM_TALK, "Rest assured, you will always be a friend of the Kharazi.")
                    goto("gujuoMenu")
                }
            } else if (player.legendsStage == SEEDS) {
                op("Ungadulu is free! He was possessed by a demon and I killed it.") {
                    player(CALM_TALK, "Ungadulu is free! He was possessed by a demon and I killed it.")
                    npc(Npc.gujuo, CALM_TALK, "A demon? You are truly brave, Bwana. Well done, and thank you for freeing our shaman.")
                    goto("gujuoMenu")
                }
                if (player.legends.getB("seedsGiven")) op("I have the Yommi tree seeds.") {
                    player(CALM_TALK, "I have the Yommi tree seeds.")
                    npc(Npc.gujuo, CALM_TALK, "That's great, Bwana. Now you just need to germinate the seeds and then plant them in some fertile soil. I'm sure that Ungadulu has explained all this to you already.")
                    goto("gujuoMenu")
                }
                op("What do I do now?") {
                    player(CALM_TALK, "What do I do now?")
                    npc(Npc.gujuo, CALM_TALK, "If you have the Yommi tree seeds, you will need to germinate them.")
                    npc(Npc.gujuo, CALM_TALK, "Place the seeds into pure water, and they will begin to sprout tiny shoots. You can then plant them in fertile soil.")
                    goto("gujuoMenu")
                }
            } else {
                if (player.legendsStage in DRY_POOL until WATER_RESTORED) op("The water pool has dried up, but I need more water.") {
                    player(CALM_TALK, "The sacred water pool has dried up, but I need more water.")
                    npc(Npc.gujuo, CALM_TALK, "This is indeed a bad omen, Bwana. I've never known the sacred pool to dry up before...")
                    goto("gujuoSourceMenu")
                }
                if (sourceExplained) op("Can you remind me where the source of the sacred water is?") { legendsMenu(player, "gujuoSourceLocation") }
                if (player.legendsStage in DRY_POOL until WATER_RESTORED) op(if (player.legends.getB("braveryRecipe")) "I've forgotten how to make the bravery potion." else "Can you help me reach the source?") {
                    legendsMenu(player, if (sourceExplained) "gujuoSourceHelp" else "gujuoSourceLocation")
                }
                if (player.legendsStage >= WATER_RESTORED && player.hasLegendsItem(Obj.yommiseeds_germ)) op("I have germinated the Yommi tree seeds.") {
                    player(CALM_TALK, "I have germinated the Yommi tree seeds.")
                    npc(Npc.gujuo, CALM_TALK, "Well done, Bwana. Thanks to your hard work we'll soon have a new totem pole. Now you need to plant the seed in the fertile earth.")
                    goto("gujuoMenu")
                }
                op("Where can I find fertile soil?") {
                    player(CALM_TALK, "Where can I find fertile soil?")
                    npc(Npc.gujuo, CALM_TALK, "There's lots of fertile ground in the Kharazi jungle. My people have marked the patches we've seen with old wood.")
                    npc(Npc.gujuo, CALM_TALK, "If you find a rotten tree, or what looks like a rotten totem pole, it's probably on fertile soil.")
                    npc(Npc.gujuo, CALM_TALK, "You'll need to chop the markers down with a sharp Woodcutting axe.")
                    goto("gujuoMenu")
                }
                if (player.legendsStage >= WATER_RESTORED) op("I found the source of the spring and I got some sacred water.") {
                    player(CALM_TALK, "I found the source of the spring and I got some sacred water.")
                    npc(Npc.gujuo, CALM_TALK, "You are truly a brave warrior, Bwana. Now your Yommi tree can grow to be just as strong as you are - and we'll have a new totem pole in no time.")
                    goto("gujuoMenu")
                }
                if (player.legendsStage >= DEMON_DEFEATED) op("I defeated the demon - for good this time!") {
                    player(CALM_TALK, "I defeated the demon - for good this time!")
                    npc(Npc.gujuo, CALM_TALK, "Can it be true? My people and I have noticed a difference in the jungle. The trees seem to sing again, the air tastes fresh and cool... and we have you to thank.")
                    goto("gujuoMenu")
                }
                op("How do I make the totem pole?") {
                    npc(Npc.gujuo, CALM_TALK, "First, your Yommi tree must be fully grown. Then, you must chop it down - quickly, before it rots!")
                    npc(Npc.gujuo, CALM_TALK, "Once you have felled the tree, trim the branches.")
                    npc(Npc.gujuo, CALM_TALK, "Finally, you can craft a new totem pole from the bare trunk.")
                    npc(Npc.gujuo, CALM_TALK, "You'll need a very sharp, very tough axe for all of this.")
                    npc(Npc.gujuo, CALM_TALK, "But when you're done, you can replace one of the rotten totem poles, and the Kharazi will be forever in your debt!")
                }
                if (player.legendsStage >= TOTEM) op("I've made the totem pole, now what do I do with it?") { npc(Npc.gujuo, CALM_TALK, "Now we need to replace an existing corrupted totem pole with your newly constructed one. You should be able to find totems within the Kharazi Jungle.") }
                if (player.legendsStage >= SOURCE) op("Do you know the name 'Viyeldi'?") { legendsMenu(player, "gujuoViyeldi") }
            }
            op("I'm lost, can you show me the way out?") { gujuoExit(player) }
            op("Okay, thanks for your help.") { legendsMenu(player, "gujuoThanks") }
        }
        if (player.legendsStage in DRY_POOL until GIFT) gujuoSourceOptions(player)
        label("gujuoThanks")
        player(CALM_TALK, "Okay, thanks for your help.")
        npc(Npc.gujuo, CALM_TALK, "You're more than welcome, Bwana.")
        npc(Npc.gujuo, CALM_TALK, "Ouch! The mosquitoes are biting! I'm going for a swim...")
        simple("Gujuo disappears into the Kharazi Jungle as swiftly as he appeared...")
    }
}

private fun DialogueBuilder.gujuoSourceOptions(player: Player) {
    val recipeKnown = player.legends.getB("braveryRecipe")
    val sourceExplained = player.legends.getB("sourceExplained") || recipeKnown
    val searchedSourceRoute = recipeKnown && player.legends.getB("orbGate")
    label("gujuoSourceMenu")
    options {
        if (player.legendsStage >= DAGGER) op("I have found the source of the sacred water!") {
            player(CALM_TALK, "I have found the source of the sacred water!")
            npc(Npc.gujuo, CALM_TALK, "That's great, Bwana! But please, be careful...")
            goto("gujuoSourceKnowledge")
        } else op("Does the Yommi tree have to have pure water?") {
            player(CALM_TALK, "Does the Yommi tree have to have pure water?")
            npc(Npc.gujuo, CALM_TALK, "Of course! It is a magical tree and can only survive on the water from the sacred pool.")
            goto("gujuoSourceMenu")
        }
        op(if (sourceExplained) "Can you remind me where the source of the sacred water is?" else "Where's the source of the pure water?") { legendsMenu(player, "gujuoSourceLocation") }
        if (searchedSourceRoute) op(if (player.legendsStage >= DAGGER) "I've forgotten how I got to the sacred water." else "I searched the caves thoroughly and didn't find any sacred water...") { legendsMenu(player, "gujuoSourceRoute") }
        if (sourceExplained) op(if (recipeKnown) "I've forgotten how to make the bravery potion." else "Can you help me reach the source?") { legendsMenu(player, "gujuoSourceHelp") }
        op("Okay, thanks for your help.") { legendsMenu(player, "gujuoThanks") }
    }
    label("gujuoSourceLocation")
    player(CALM_TALK, if (sourceExplained) "Can you remind me where the source of the sacred water is?" else "Where's the source of the pure water?")
    if (!sourceExplained) {
        npc(Npc.gujuo, CALM_TALK, "Oh, Bwana, it is a terrible place. Be thankful you don't have to go there.")
        npc(Npc.gujuo, CALM_TALK, "Bwana, you cannot be thinking of travelling to the source!")
        npc(Npc.gujuo, CALM_TALK, "But I suppose we have no choice...")
    }
    npc(Npc.gujuo, CALM_TALK, "Deeper in the caves where you found Ungadulu is a terrible place, guarded by the angry spirits of the undead.")
    npc(Npc.gujuo, CALM_TALK, "The legends say they died trying to find the source of the sacred water, and now they are cursed to guard it for all eternity.")
    npc(Npc.gujuo, CALM_TALK, "The first to die was a powerful sorcerer, and his strange magic lingers in the cave.")
    npc(Npc.gujuo, CALM_TALK, "Now all who venture near are struck by terror, and cannot proceed.")
    npc(Npc.gujuo, CALM_TALK, "With all my heart, Bwana, nothing could convince me to go near such a place.", Runnable {
        if (player.legendsStage in DRY_POOL until GIFT) player.legends.setB("sourceExplained", true)
    })
    options {
        op("If I looked for the source, could you help me?") { legendsMenu(player, "gujuoSourceHelp") }
        op("Okay, I won't go...") {
            player(CALM_TALK, "Okay, I won't go...")
            npc(Npc.gujuo, CALM_TALK, "That's sensible of you, Bwana. It would be a waste of a perfectly fine life.")
            npc(Npc.gujuo, CALM_TALK, "We will just have to restore the totem some other way... or else leave our home forever...")
            options {
                op("If I looked for the source, could you help me?") { legendsMenu(player, "gujuoSourceHelp") }
                op("Okay, thanks for your help.") { legendsMenu(player, "gujuoThanks") }
            }
        }
        if (searchedSourceRoute) op(if (player.legendsStage >= DAGGER) "I've forgotten how I got to the sacred water." else "I searched the caves thoroughly and didn't find any sacred water...") { legendsMenu(player, "gujuoSourceRoute") }
        op("Okay, thanks for your help.") { legendsMenu(player, "gujuoThanks") }
    }
    label("gujuoSourceRoute")
    if (player.legendsStage >= DAGGER) {
        player(CALM_TALK, "I've forgotten how I got to the sacred water.")
        npc(Npc.gujuo, CALM_TALK, "The path must be well hidden. You should take another look around the cave where you found Ungadulu. The entrance is most certainly there somewhere!")
    } else {
        player(CALM_TALK, "I searched the caves thoroughly and didn't find any sacred water...")
        npc(Npc.gujuo, CALM_TALK, "The source of the sacred water is a terribly dangerous place. Perhaps the elders have hidden the path, or buried it under rubble?")
    }
    options {
        op("Can you help me reach the source?") { legendsMenu(player, "gujuoSourceHelp") }
        op("Where's the source of the pure water?") { legendsMenu(player, "gujuoSourceLocation") }
        op("Okay, thanks for your help.") { legendsMenu(player, "gujuoThanks") }
    }
    label("gujuoSourceHelp")
    player(CALM_TALK, if (recipeKnown) "I've forgotten how to make the bravery potion." else "If I looked for the source, could you help me?")
    npc(Npc.gujuo, CALM_TALK, "If you're determined to find the source, I will help as much as I can.")
    npc(Npc.gujuo, CALM_TALK, "First, you must overcome the curse of fear that blocks the cave.")
    npc(Npc.gujuo, CALM_TALK, "I know a potion that may help with that.")
    npc(Npc.gujuo, CALM_TALK, "You will need to find two herbs, Snake Weed and Ardrigal.")
    npc(Npc.gujuo, CALM_TALK, "Add them both to a Vial of Water, and you will feel braver than a lion!", Runnable {
        if (player.legendsStage in DRY_POOL until GIFT) {
            player.legends.setB("sourceExplained", true)
            player.legends.setB("braveryRecipe", true)
        }
    })
    label("gujuoSourceHerbs")
    options {
        op("Where can I find Snake Weed?") {
            player(CALM_TALK, "Where can I find Snake Weed?")
            npc(Npc.gujuo, CALM_TALK, "Snake Weed is usually found in swampy, marshy areas.")
            npc(Npc.gujuo, CALM_TALK, "There's some marsh to the south of Tai Bwo Wannai village - maybe you could check there? It likes to grow near jungle vines, so look carefully.")
            goto("gujuoSourceHerbs")
        }
        op("Where can I find Ardrigal?") {
            player(CALM_TALK, "Where can I find Ardrigal?")
            npc(Npc.gujuo, CALM_TALK, "Ardrigal is often found growing near large groups of palms. If you head east out of Tai Bwo Wannai village you should come across a big group of them.")
            npc(Npc.gujuo, CALM_TALK, "The herb grows in the shade of the palm, so check carefully.")
            goto("gujuoSourceHerbs")
        }
        op("Will I really need this Potion? I'm already pretty brave.") {
            player(CALM_TALK, "Will I really need this Potion? I'm already pretty brave.")
            npc(Npc.gujuo, CALM_TALK, "The caves are protected by a supernatural curse which would leave even the bravest adventurer a trembling wreck.")
            npc(Npc.gujuo, CALM_TALK, "You must take the potion if you plan to journey there.")
            goto("gujuoSourceHerbs")
        }
        op("Where's the source of the pure water?") { legendsMenu(player, "gujuoSourceLocation") }
        op("Okay, thanks for your help.") { legendsMenu(player, "gujuoThanks") }
    }
    label("gujuoSourceKnowledge")
    options {
        op("Do you know anything more about the caves?") {
            player(CALM_TALK, "Do you know anything more about the caves?")
            npc(Npc.gujuo, CALM_TALK, "I have told you everything I know, Bwana.")
            goto("gujuoSourceMenu")
        }
        op("Do you know the name 'Viyeldi'?") { legendsMenu(player, "gujuoViyeldi") }
        op("Okay, thanks for your help.") { legendsMenu(player, "gujuoThanks") }
    }
    label("gujuoViyeldi")
    player(CALM_TALK, "Do you know the name 'Viyeldi'?")
    simple("Gujuo scratches his head for a moment.")
    npc(Npc.gujuo, CALM_TALK, "It seems familiar. Maybe I heard it from the Elders...?")
    npc(Npc.gujuo, CALM_TALK, "Of course! Viyeldi was the name of the sorcerer who first went in search of the sacred water's source.")
    npc(Npc.gujuo, CALM_TALK, "Have you encountered his spirit? Be careful, Bwana... he must be very powerful.")
    goto("gujuoSourceMenu")
}

private fun DialogueBuilder.legendsMenu(player: Player, stage: String) {
    legendsExec(player) { }
    goto(stage)
}

private fun DialogueBuilder.gujuoExit(player: Player) {
    npc(Npc.gujuo, CALM_TALK, "I can take you to the edge of the Kharazi Jungle. Shall we go now?")
    options { op("Yes please...") { npc(Npc.gujuo, CALM_TALK, "Follow me..."); legendsExec(player) { player.useStairs(-1, Tile.of(2818, 2941, 0), 1, 2) } }; op("No thanks...") }
}

private fun DialogueBuilder.vesselOptions(player: Player) {
    label("vessel")
    options {
        op("Metal of the sun... what is that?") {
            npc(Npc.gujuo, CALM_TALK, "It is a bright and precious metal, much sought after, but uncommonly found. It is the same bright colour as the sun, and yet its light never fades. A blessed vessel made of this metal protects the purity of the water.")
            options {
                op("Where can I find this metal?") { npc(Npc.gujuo, CALM_TALK, "By mining it, of course!"); npc(Npc.gujuo, CALM_TALK, "But be careful. This metal can influence people in strange ways. Fall under its power, and you will think of nothing else for the rest of your days, no matter how much of it you find."); goto("vessel") }
                op("What kind of vessel?") { legendsMenu(player, "bowlSketch") }
            }
        }
        op("What kind of vessel?") {
            label("bowlSketch")
            npc(Npc.gujuo, CALM_TALK, "A bowl would hold plenty of water. However, it must be made from the metal of the sun, and it must be blessed.")
            legendsExec(player) { player.legends.setB("bowlRecipe", true); if (!player.hasLegendsItem(Obj.goldbowlpic)) player.giveLegendsItem(Obj.goldbowlpic) }
            npc(Npc.gujuo, CALM_TALK, "Here, this is what the vessel should look like... I hope that helps.")
            simple("Use the sketch on an anvil with two gold bars and a hammer to make the bowl.")
            options {
                op("How do I bless the vessel?") { npc(Npc.gujuo, CALM_TALK, "When you have made the vessel, bring it to me. I will help but you need to ensure that you are devout and have faith. Your Prayer will be thoroughly tested.") }
                op("Okay, thanks for your help.")
            }
        }
        op("Okay, thanks for your help.") { npc(Npc.gujuo, CALM_TALK, "You're more than welcome, Bwana.") }
    }
}

private fun blessBowl(player: Player) {
    player.startConversation {
        npc(Npc.gujuo, CALM_TALK, "Greetings, Bwana. Ah, I see you have the golden bowl! Would you like me to bless it for you?")
        options {
            op("Yes please!") {
                if (player.skills.getLevel(Skills.PRAYER) < 42 || player.prayer.points < 420) {
                    npc(Npc.gujuo, CALM_TALK, "Bwana... your faith is not strong enough to bless this vessel.")
                    simple("You need a Prayer level of at least 42 and at least 42 Prayer points to complete this task.")
                } else {
                    npc(Npc.gujuo, CALM_TALK, "Very well, Bwana...")
                    simple("Gujuo places the bowl on the floor in front of you, and leads you into a deep meditation...")
                    npc(Npc.gujuo, CALM_TALK, "Ohhhhhmmmmmm.")
                    player(CALM_TALK, "Oooooommmmmmmmmm")
                    npc(Npc.gujuo, CALM_TALK, "Ohhhhhmmmmmm.")
                    player(CALM_TALK, "Oooooohhhhmmmmmmmmmm")
                    npc(Npc.gujuo, CALM_TALK, "Ohhhhhmmmmmm.")
                    legendsExec(player) {
                        if (!player.legendsLevel(Skills.PRAYER, 42) || player.prayer.points < 420 || !player.inventory.containsItem(Obj.goldbowl_empty)) {
                            player.sendMessage("You need the golden bowl and at least 42 Prayer points to attempt the blessing.")
                        } else if (!player.legendsTrial(Skills.PRAYER, 42)) {
                            player.prayer.drainPrayer(50.0)
                            player.sendMessage("You fail to bless the bowl. Your Prayer is drained by five points.")
                        } else if (player.replaceLegendsItem(Obj.goldbowl_empty, Obj.goldbowlbless_empty)) {
                            player.anim(Seq.human_pray)
                            player.sendMessage("You awaken from the trance feeling perfectly at ease. The bowl is blessed!")
                        }
                    }
                }
            }
            op("No thanks, I need help with something else.") {
                player(CALM_TALK, "No thanks, I need help with something else.")
                legendsExec(player) { gujuoDialogue(player, false) }
            }
        }
    }
}

internal fun fireWallDialogue(player: Player) {
    player.legends.setB("flamesSeen", true)
    player.advanceLegends(BINDING)
    player.startConversation {
        simple("The flames form a solid wall, rising straight up out of the ground. There's something odd about it...", "You see a white-clad figure in the midst of the flames...")
        player(CALM_TALK, "Wow, that looks hot!")
        npc(Npc.ungadulu_good, CALM_TALK, "Please, come no closer! The flames will incinerate you.")
        label("fireQuestions")
        options {
            op("How can I extinguish the flames?") {
                player(CALM_TALK, "How can I extinguish the flames?")
                npc(Npc.ungadulu_good, CALM_TALK, "Please, don't try-")
                npc(Npc.ungadulu_bad, ANGRY, "Yes, douse the flames with water... pure water...")
                npc(Npc.ungadulu_good, CALM_TALK, "Please, leave now! Don't listen... I beg you, leave now, and do not return!")
                goto("fireQuestions")
            }
            op("Where do I get pure water from?") {
                npc(Npc.ungadulu_good, CALM_TALK, "No... I will not tell you!")
                npc(Npc.ungadulu_bad, ANGRY, "...from the sacred pool, in the lands above... hurry and release me...")
                npc(Npc.ungadulu_good, CALM_TALK, "N-no! Please, Bwana, just leave while you still can!")
                npc(Npc.ungadulu_bad, ANGRY, "Hurry, Vacu, the heat of these flames is killing me... ha ha ha!")
                simple("The shaman throws himself to the floor and starts convulsing.")
            }
            op("Who are you?") { npc(Npc.ungadulu_good, CALM_TALK, "I am Ungadulu, shaman of the Kharazi tribe."); npc(Npc.ungadulu_bad, ANGRY, "Bring me pure water! Douse the flames, lest ye -"); npc(Npc.ungadulu_good, CALM_TALK, "Please, Bwana, don't listen to me! Run, tell my people I am lost..."); goto("fireQuestions") }
        }
    }
}

private fun ungaduluDialogue(player: Player) {
    if (player.legendsStage < RESCUE) { player.sendMessage("The shaman is too preoccupied to speak to you."); return }
    if (player.legendsStage < SEEDS) {
        if (!player.inOctagram()) { fireWallDialogue(player); return }
        player.startConversation {
            npc(Npc.ungadulu_good, CALM_TALK, "Please, just run...")
            simple("The shaman seems to be fighting an inner battle.")
            npc(Npc.ungadulu_good, CALM_TALK, "Go... go now...!")
            simple("The shaman's expression changes entirely.")
            npc(Npc.ungadulu_bad, ANGRY, "Welcome Vacu, to eternal...")
            npc(Npc.ungadulu_bad, ANGRY, "Foolish Vacu... now thou shalt be my pawn.")
            npc(Npc.ungadulu_bad, ANGRY, "Iles Resti Yam Darkus Spiritus Possesi Yanai...")
            simple("You feel a strange power coming over you...", "You feel very weak..", "The thing seems to get stronger!")
            legendsExec(player) { player.skills.adjustStat(-5, 0.0, Skills.ATTACK, Skills.STRENGTH) }
            npc(Npc.ungadulu_good, CALM_TALK, "Run, Bwana! Stay away from here!")
        }
        return
    }
    player.startConversation {
        npc(Npc.ungadulu_good, CALM_TALK, when {
            player.legendsStage >= GIFT -> "Tales of your exploits are told throughout the jungle! You have done well, Bwana, to have succeeded where so many others have failed. You are a true legend."
            player.legendsStage >= WATER_RESTORED -> "Blessings upon you Bwana. Did you use the spell I gave you to deal with the spirit? And did you find the sacred water?"
            player.legendsStage >= DAGGER -> "Hello, Bwana. How goes your search for the sacred water?"
            else -> "Bwana, you've saved me from the demon Nezikchened! How can I ever repay this favour?"
        })
        label("ungaduluMenu")
        options {
            op(if (player.legends.getB("seedsGiven")) "I need more Yommi tree seeds." else "I need to collect some Yommi tree seeds for Gujuo.") {
                if (player.hasLegendsItem(Obj.yommiseeds) || player.hasLegendsItem(Obj.yommiseeds_germ)) {
                    npc(Npc.ungadulu_good, CALM_TALK, "You already have some Yommi tree seeds. Use those first and then come back to me if you need any more.")
                } else {
                    npc(Npc.ungadulu_good, CALM_TALK, "Yommi tree seeds? To grow a new totem? Then Nezikchened's influence has spread further than I thought...")
                    npc(Npc.ungadulu_good, CALM_TALK, "You should know that growing a Yommi tree is no simple task - but I'll help you as much as I can.")
                    legendsExec(player) {
                        if (player.legendsStage >= SEEDS && player.giveLegendsItem(Obj.yommiseeds, 3)) {
                            player.legends.setB("seedsGiven", true)
                            player.startConversation {
                                item(Obj.yommiseeds, "Ungadulu gives you some Yommi tree seeds.")
                                npc(Npc.ungadulu_good, CALM_TALK, "Here you are, take these with my gratitude. You'll need to soak them in sacred water to germinate them before planting them.")
                            }
                        }
                    }
                }
            }
            op("How do I grow the Yommi tree?") {
                npc(Npc.ungadulu_good, CALM_TALK, "First you will need to soak the seeds in some sacred water. This will help them germinate and begin the growing process.")
                npc(Npc.ungadulu_good, CALM_TALK, "Next, you'll need to find a patch of fertile soil and plant the seeds.")
                npc(Npc.ungadulu_good, CALM_TALK, "Yommi trees are magical, and grow very quickly. You'll need to water them often with more sacred water - there should be plenty in the spring.")
                goto("ungaduluMenu")
            }
            op("What do you know about the source of the sacred water?") {
                npc(Npc.ungadulu_good, CALM_TALK, "The sacred water comes from a magical spring, the source of which lies deep underground.")
                npc(Npc.ungadulu_good, CALM_TALK, "Long ago, the elders blocked off the entrance to the Viyeldi caves, somewhere in this same cave system. There's bound to be evidence of the entrance somewhere.")
                npc(Npc.ungadulu_good, CALM_TALK, "Beware though - the caves are deeply cursed, and should you fall, your spirit will be trapped within the caves forever, sworn to protect their secret.")
                goto("ungaduluMenu")
            }
            op("What will you do now?") {
                player(CALM_TALK, "What will you do now?")
                npc(Npc.ungadulu_good, CALM_TALK, "I will remain here in the protection of the flaming octagram and continue my research into the spirit world. My recent experience has made me something of an authority on the subject!")
                npc(Npc.ungadulu_good, CALM_TALK, "But do please visit me here if you have any problems with your Yommi tree. I remember carving my first totem... it's no easy feat!")
                goto("ungaduluMenu")
            }
            if (player.legendsStage >= DAGGER && player.legendsStage < WATER_RESTORED) {
                op("I met a spirit in the Viyeldi Caves.") {
                    player(CALM_TALK, "I met a spirit in the Viyeldi Caves.")
                    npc(Npc.ungadulu_good, CALM_TALK, "You did well to come to me, Bwana. As I said before, I am an expert in these matters.")
                    npc(Npc.ungadulu_good, CALM_TALK, "Spirits are tricky creatures. In most circumstances you should just ignore them - but be especially wary if they ask you for favours. They may be trying to trick you...")
                    goto("ungaduluMenu")
                }
                op("The spirit told me to kill Viyeldi!") {
                    npc(Npc.ungadulu_good, CALM_TALK, "Goodness! That sounds like a fiendish spirit indeed. You're better off banishing it - I have a spell that will do the trick.")
                    npc(Npc.ungadulu_good, CALM_TALK, "But I need an item that belongs to the spirit to make it work.")
                    npc(Npc.ungadulu_good, CALM_TALK, "If you have something like that, please show it to me.")
                    goto("ungaduluMenu")
                }
                op("Do you know anything about daggers?") { npc(Npc.ungadulu_good, CALM_TALK, "I know some things about them - especially the magical kind. Do you have one of those?"); goto("ungaduluMenu") }
                if (player.legends.getB("lightPath") && !player.hasLegendsItem(Obj.holyforce)) op("I need another Holy Force spell.") {
                    npc(Npc.ungadulu_good, CALM_TALK, "Use this spell on the spirit. It will force it to show its true self and make it vulnerable to attack.")
                    legendsExec(player) { if (player.legendsStage == DAGGER && player.legends.getB("lightPath")) player.giveLegendsItem(Obj.holyforce) }
                }
                if (player.legends.getB("viyeldiDead")) op("I have killed Viyeldi!") {
                    npc(Npc.ungadulu_good, CALM_TALK, "Why would you do that?!")
                    player(CALM_TALK, "A spirit called Echned Zekin said I had to avenge his spirit by killing Viyeldi if I wanted him to unblock the sacred spring.")
                    npc(Npc.ungadulu_good, CALM_TALK, "Bwana, I'm afraid this spirit has tricked you! Viyeldi was the spirit of a powerful sorcerer, who controlled the spirits of those bound to the sacred water.")
                    npc(Npc.ungadulu_good, CALM_TALK, "If he is truly dead, then those spirits are now free to be controlled by other, far worse forces - that lying spirit is probably one of them.")
                }
            }
            if (player.legendsStage >= WATER_RESTORED) op("What do I do now?") { npc(Npc.ungadulu_good, CALM_TALK, "Well, you should be able to plant the Yommi tree and keep it alive by watering it with the sacred water. After that, you can carve your totem pole!"); npc(Npc.ungadulu_good, CALM_TALK, "Let us hope we've seen the last of Nezikchened... who knows what he might try next?") }
            if (player.legendsStage in TOTEM until GIFT) {
                op("I've already made the totem pole.") { player(CALM_TALK, "I've already made the totem pole."); npc(Npc.ungadulu_good, CALM_TALK, "That's great news, Bwana. Once you replace the corrupted one, our people will be able to gather together once more."); goto("ungaduluMenu") }
                op("I'm not sure what to do with the totem pole.") { player(CALM_TALK, "I'm not sure what to do with the totem pole."); npc(Npc.ungadulu_good, CALM_TALK, "Simply replace a corrupted totem pole with the one you've made. That should make any nearby spirits think twice about bothering my people!"); goto("ungaduluMenu") }
            }
            op("How do I get out of here?") { npc(Npc.ungadulu_good, CALM_TALK, "Now that you have defeated the demon you can come and go as you please. I cast a spell on you which means that you can pass through the flames without harm."); goto("ungaduluMenu") }
            op("Okay, thanks...") { npc(Npc.ungadulu_good, CALM_TALK, "It's the least I can do!") }
        }
    }
}

private fun holyForceDialogue(player: Player) {
    if (player.legendsStage != DAGGER || player.legends.getB("viyeldiDead") || !player.inventory.containsItem(Obj.deathdagger)) return
    player.startConversation {
        simple("You hand the dagger over to the shaman and his face turns pale.")
        npc(Npc.ungadulu_good, CALM_TALK, "This dagger has been made for one purpose only... thank goodness you brought it to me.")
        npc(Npc.ungadulu_good, CALM_TALK, "Now we can turn the tables! I can make you a spell with this item which will force the spirit to reveal its true self. Once activated, you will be able to attack it like any normal creature.")
        legendsExec(player) { if (player.replaceLegendsItem(Obj.deathdagger, Obj.holyforce)) player.legends.setB("lightPath", true) }
        npc(Npc.ungadulu_good, CALM_TALK, "Use this spell on the spirit. It will force it to show its true self and make it vulnerable to attack.")
    }
}

internal fun echnedDialogue(player: Player) {
    if (player.legendsStage !in SOURCE..DAGGER || !player.atLegendsSpring() || !player.legends.getB("barrier")) return
    if (player.inventory.containsItem(Obj.deathdaggerdone) || player.equipment.weaponId == Obj.deathdaggerdone || player.legends.getB("daggerReturned")) {
        revealEchned(player, false)
        return
    }
    if (activeLegendsFight(player)) return
    val spirit = ensureLegendsNpc(player, Npc.echned_zekin, ECHNED_SPAWN) ?: return
    spirit.tempAttribs.setB("legendsSpiritTalking", true)
    if (player.legendsStage == DAGGER) {
        val holyForce = player.inventory.containsItem(Obj.holyforce)
        player.startConversation {
            simple("The shapeless entity of Echned Zekin appears in front of you.")
            npc(Npc.echned_zekin, CALM_TALK, if (holyForce) "You seem filled with determination. Have you slain Viyeldi? Give me the dagger, quickly!" else "Why do you return when your task is still incomplete?")
            label("echnedRepeat")
            options {
                if (holyForce) {
                    op("I don't have the dagger.") {
                        player(CALM_TALK, "I don't have the dagger.")
                        simple("The spirit seems to shake with anger...")
                        npc(Npc.echned_zekin, CALM_TALK, "Bring it to me with all haste, or feel my wrath!")
                        simple("The spirit extends a wraithlike finger towards you. You feel a searing pain jolt through your body...")
                        legendsExec(player) { player.applyHit(Hit.flat(null, 50)) }
                    }
                    op("I haven't killed Viyeldi yet.") {
                        npc(Npc.echned_zekin, CALM_TALK, "Go now and slay him, as you agreed. Unless you'd like me to take vengeance on you, instead?")
                        goto("echnedRepeat")
                    }
                    op("I have something else in mind!") {
                        npc(Npc.echned_zekin, CALM_TALK, "Worthless Vacu! How dare you lie to me! Go and slay Viyeldi, as you promised, or I will lay upon you all the pain and torment I have endured these long years!")
                        goto("echnedRepeat")
                    }
                } else {
                    op("Who am I supposed to kill again?") {
                        npc(Npc.echned_zekin, CALM_TALK, "I seek revenge on Viyeldi, the cruel fiend who slew me centuries ago.")
                        npc(Npc.echned_zekin, CALM_TALK, "I sense he is nearby. Kill him, and I will grant you access to the sacred water.")
                        goto("echnedRepeat")
                    }
                    op("Er, I've had second thoughts.") {
                        npc(Npc.echned_zekin, CALM_TALK, "It is too late for second thoughts... do as you promised and return to me, with haste! Viyeldi's presence torments me so...")
                        goto("echnedRepeat")
                    }
                    if (!player.hasLegendsItem(if (player.legends.getB("viyeldiDead")) Obj.deathdaggerdone else Obj.deathdagger) && !player.legends.getB("lightPath")) {
                        opExec("I lost the dagger.") {
                            if (player.legendsStage == DAGGER && !player.legends.getB("lightPath") && !player.legends.getB("daggerReturned"))
                                player.giveLegendsItem(if (player.legends.getB("viyeldiDead")) Obj.deathdaggerdone else Obj.deathdagger)
                        }
                    }
                }
                op("I have to be going...") {
                    npc(Npc.echned_zekin, CALM_TALK, "Return the dagger to me when your task is complete.")
                    simple("The spirit slowly fades and then disappears.")
                }
            }
            legendsExec(player) { spirit.finish() }
        }
        return
    }
    player.startConversation {
        simple("A thick green mist seems to emanate from the water... It slowly congeals into the shape of a body.", "In a rasping, barely audible voice you hear the entity speak.")
        npc(Npc.echned_zekin, CALM_TALK, "Who disturbs the rocks of Zekin?")
        simple("There seems to be something slightly familiar about this presence.")
        options {
            op("Er... me?") {
                player(CALM_TALK, "Er... me?")
                npc(Npc.echned_zekin, CALM_TALK, "So, you desire the sacred water that flows here?")
                options {
                    op("Yes, I need it for my quest.") {
                        player(CALM_TALK, "Yes, I need it for my quest.")
                        npc(Npc.echned_zekin, CALM_TALK, "The water babbles so loudly and I am already so tortured. I cannot abide the sound, so I have stoppered the streams. Care you not for my torment and pain?")
                        goto("echnedTormentMenu")
                    }
                    op("Not really, I just wondered if I could push that big rock.") {
                        player(CALM_TALK, "Not really, I just wondered if I could push that big rock.")
                        npc(Npc.echned_zekin, CALM_TALK, "The rock must remain! I placed it there to stop the sound of the water's babbling. The noise troubles my soul and I seek a little rest... rest from this terrible torture...")
                        goto("echnedTormentMenu")
                    }
                }
            }
            op("Who's asking?") {
                player(CALM_TALK, "Who's asking?")
                simple("The hooded, headless figure turns to face you... It's quite unnerving.")
                npc(Npc.echned_zekin, CALM_TALK, "I am Echned Zekin, and I seek peace from my eternal torture...")
                options {
                    op("What can I do about that?") { legendsMenu(player, "echnedHelp") }
                    op("Do I know you?") {
                        player(CALM_TALK, "Do I know you?")
                        npc(Npc.echned_zekin, CALM_TALK, "I am long since dead and buried, lost in the passages of time. My kin have departed and I am forgotten by all who knew me... so it is unlikely that you were one of them. I am but a poor tortured soul, trying to get some rest...")
                        goto("echnedTormentMenu")
                    }
                    op("Why are you tortured?") { legendsMenu(player, "echnedTorment") }
                }
            }
        }
        label("echnedTormentMenu")
        options {
            op("Why are you tortured?") { legendsMenu(player, "echnedTorment") }
            op("What can I do about that?") { legendsMenu(player, "echnedHelp") }
        }
        label("echnedTorment")
        player(CALM_TALK, "Why are you tortured?")
        npc(Npc.echned_zekin, CALM_TALK, "I was robbed of my life by a cruel man named Viyeldi. For centuries I have searched for him, so I might enact my vengeance.")
        npc(Npc.echned_zekin, CALM_TALK, "But, oh! Tragedy of tragedies, while searching for the source of the sacred water, his spirit became trapped! Now he is neither living nor dead, and I will never be released from my torment.")
        options {
            op("What can I do about that?") { legendsMenu(player, "echnedHelp") }
            op("Can't I just get some water?") {
                player(CALM_TALK, "Can't I just get some water?")
                npc(Npc.echned_zekin, CALM_TALK, "First, you must help me. Revenge is the only thing that keeps my spirit in this place.")
                npc(Npc.echned_zekin, CALM_TALK, "Help me take vengeance on Viyeldi and I will gladly remove the rocks and allow you access to the water. What say you, adventurer?")
                goto("echnedChoice")
            }
        }
        label("echnedHelp")
        player(CALM_TALK, "What can I do about that?")
        npc(Npc.echned_zekin, CALM_TALK, "I was brutally murdered by a vicious man called Viyeldi. I sense his presence nearby.")
        npc(Npc.echned_zekin, CALM_TALK, "My spirit burns with the need for revenge. While that fiend yet lives, I cannot rest. If you seek the sacred water, you must ensure he meets his end.")
        npc(Npc.echned_zekin, CALM_TALK, "If you refuse, then I swear, the sacred water shall never flow again! Will you put an end to Viyeldi for me?")
        label("echnedChoice")
        options {
            op("I'll do what I must to get the water.") {
                player(CALM_TALK, "I'll do what I must to get the water.")
                simple("The shapeless spirit seems to crackle with energy.")
                npc(Npc.echned_zekin, CALM_TALK, "Destroy Viyeldi, and the sacred water is yours. But beware, for this is no simple task.")
                if (!player.hasLegendsItem(Obj.deathdagger)) {
                    npc(Npc.echned_zekin, CALM_TALK, "I will furnish you with a weapon which will destroy Viyeldi for good. Here, take this.")
                    legendsExec(player) {
                        if (!player.atLegendsSpring() || player.legendsStage !in SOURCE..DAGGER) player.endConversation()
                        else if (player.hasLegendsItem(Obj.deathdagger) || player.giveLegendsItem(Obj.deathdagger)) player.advanceLegends(DAGGER)
                        else player.endConversation()
                    }
                    simple("The spirit waves an arm and in front of you appears a dark dagger hewn from pure obsidian.")
                    npc(Npc.echned_zekin, CALM_TALK, "To complete your task you must use this weapon on Viyeldi.")
                } else {
                    npc(Npc.echned_zekin, CALM_TALK, "Use this dagger to slay the fiendish Viyeldi, and bring it back to me once he is dead... for good, this time.")
                    legendsExec(player) { if (player.legendsStage == SOURCE && player.atLegendsSpring() && player.hasLegendsItem(Obj.deathdagger)) player.advanceLegends(DAGGER) }
                }
                options {
                    op("Ok, I'll do it.") {
                        player(CALM_TALK, "Ok, I'll do it.")
                        simple("The formless shape shimmers brightly...")
                        npc(Npc.echned_zekin, CALM_TALK, "You've made the right decision. Soon, the sacred water will be yours. Bring the dagger back to me when you have completed your task.")
                    }
                    op("I've changed my mind, I can't do it.") {
                        player(CALM_TALK, "I've changed my mind, I can't do it.")
                        npc(Npc.echned_zekin, CALM_TALK, "That's your decision. Are you sure you don't need the sacred water, though? Deny me, and it will forever be out of your reach...")
                        goto("echnedChoice")
                    }
                }
            }
            op("No, I won't take someone's life for you.") {
                player(CALM_TALK, "No, I won't take someone's life for you.")
                npc(Npc.echned_zekin, CALM_TALK, "A noble sentiment, but Viyeldi is not alive! He is merely a vessel for the sacred water's power.")
                npc(Npc.echned_zekin, CALM_TALK, "If that is your decision then so be it, but I cannot allow access to the stream until Viyeldi is dead.")
            }
        }
        legendsExec(player) { spirit.finish() }
    }
}


private fun returnDarkDagger(player: Player) {
    if (player.legendsStage != DAGGER || !player.atLegendsSpring() || player.legends.getB("lightPath") || player.legends.getB("viyeldiDead")) return
    player.startConversation {
        npc(Npc.echned_zekin, CALM_TALK, "Does this mean you do not wish to help me take vengeance on Viyeldi?")
        options {
            op("Yes, take the dagger back.") {
                player(CALM_TALK, "Here, take the dagger back.")
                legendsExec(player) { if (player.legendsStage == DAGGER && !player.legends.getB("lightPath") && !player.legends.getB("viyeldiDead")) player.inventory.deleteItem(Obj.deathdagger, 1) }
                npc(Npc.echned_zekin, CALM_TALK, "Very well, I respect your decision - but I cannot allow you to access the sacred water while my tormentor lives.")
            }
            op("No, I'll complete the task.") { player(CALM_TALK, "No, I'll complete the task."); npc(Npc.echned_zekin, CALM_TALK, "Very well, return to me when it is done.") }
        }
    }
}

internal fun viyeldiDialogue(player: Player, attacked: Boolean = false) {
    if (player.legends.getB("viyeldiDead")) return
    val spirit = ensureLegendsNpc(player, Npc.viyeldi, Tile.of(2378, 4714, 0)) ?: return
    spirit.tempAttribs.setB("legendsSpiritTalking", true)
    player.startConversation {
        if (attacked) npc(Npc.viyeldi, CALM_TALK, "Adventurer, I am already deceased, and therefore impervious to your attacks. Still, I can tell when I'm not wanted...")
        else {
            simple("Your hand passes through the hat as though it wasn't there at all. Instantly, the other clothes begin to animate and walk towards you!")
            npc(Npc.viyeldi, CALM_TALK, "Beware, adventurer. You are brave, but braver folk than you have fallen in pursuit of the sacred water.")
            npc(Npc.viyeldi, CALM_TALK, "Perilous dangers do await thee, Tojalon, Senay and Devere make three. None hold malice but will test your might, pray you do not lose these fights.")
            npc(Npc.viyeldi, CALM_TALK, "If, however, you win this day, take heart that see the spring you may.")
            npc(Npc.viyeldi, CALM_TALK, "Through Dragon's eye will you gain new heart. Heed these words, else swiftly depart.")
        }
        legendsExec(player) { spirit.finish() }
        simple("The clothes slump to the floor again.")
    }
}

internal fun giveGildedTotem(player: Player) {
    if (player.legendsStage != GIFT || player.hasFinished() || player.isDead) return
    val gujuo = ensureLegendsNpc(player, Npc.gujuo, player.transform(1, 0, 0)) ?: return
    gujuo.faceTile(player.tile)
    gujuo.tempAttribs.setB("legendsSpiritTalking", true)
    player.startConversation {
        if (player.legends.getB("gildedTotemGiven")) {
            player(CALM_TALK, "I've lost the totem pole you gave me.")
            npc(gujuo, CALM_TALK, "Lost it? That totem pole was a sign of our friendship, Bwana...")
            npc(gujuo, CALM_TALK, "Luckily, I made an extra one. Please be more careful with it this time.")
            legendsExec(player) { if (player.legendsStage == GIFT) player.giveLegendsItem(Obj.thtotempolegift) }
        } else {
            npc(gujuo, CALM_TALK, "Greetings, Bwana! I can tell you've already placed the totem. The jungle feels alive again, and my people can finally gather without being bothered by evil spirits.")
            npc(gujuo, CALM_TALK, "You have done us a great service, Bwana.")
            legendsExec(player) {
                if (player.legendsStage == GIFT && player.giveLegendsItem(Obj.thtotempolegift)) player.legends.setB("gildedTotemGiven", true)
            }
            npc(gujuo, CALM_TALK, "Please accept this token of our friendship.")
            npc(gujuo, CALM_TALK, "Take this to your Legends' Guild - let them know that you are a friend of the Kharazi. And please, come back and visit us anytime.")
            npc(gujuo, CALM_TALK, "I must go and hunt now, Bwana...")
        }
        legendsExec(player) { gujuo.finish() }
    }
}
