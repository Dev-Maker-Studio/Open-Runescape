package com.rs.game.content.minigames.mage_training_arena

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.NONE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.dialogue.statements.NPCStatement
import com.rs.game.model.entity.player.Player
import com.rs.plugin.PluginManager
import com.rs.plugin.events.DestroyItemEvent
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private const val ARENA_INTRODUCED = "mageTrainingArenaIntroduced"

private fun DialogueBuilder.guardian(id: Int, text: String, extraFunctionality: Runnable? = null) {
    var remaining = text
    while (remaining.length > 260) {
        val end = remaining.lastIndexOf(' ', 260)
        npc(id, NONE, remaining.substring(0, end))
        remaining = remaining.substring(end + 1)
    }
    npc(id, NONE, remaining, extraFunctionality)
}

internal fun hasProgressHat(player: Player) = MageTrainingArena.hats.any { player.inventory.containsItem(it) || player.equipment.hatId == it }

private fun hasStoredProgressHat(player: Player) = hasProgressHat(player) || MageTrainingArena.hats.any { player.bank.containsItem(it) }

fun talkArenaGuardian(player: Player, id: Int) {
    player.startConversation {
        when (id) {
            Npc.magictraining_guard_tele -> {
                player(CALM_TALK, "Hi.")
                guardian(id, "Greetings young one. What is it you wish to know?")
                label("menu")
                options {
                    op("What do I have to do in this room?") {
                        player(CALM_TALK, "What do I have to do in this room?")
                        guardian(id, "In this room you will see a maze within which one of my fellow Guardians has been turned to stone for the purposes of this exercise. You must move the statue using your telekinetic grab spell to the exit square at the edge of the maze to bring the Guardian back to life. Simply stand on the side that you wish for the statue to travel towards and cast the spell on the statue. Once you have solved the maze, the statue will change back into the Guardian and he will award you with Telekinetic Pizazz Points and teleport you to the next maze. You can switch to a better view of the maze by selecting the 'Observe' option on the statue and return your view to normal by selecting the same option again. There is also a 'Reset' option on the statue just in case things aren't going too well.")
                        goto("menu")
                    }
                    op("What are the rewards?") {
                        player(CALM_TALK, "What are the rewards?")
                        guardian(id, "As well as the experience in casting magic, you will get Telekinetic Pizazz Points for each maze successfully solved and bonus points for completing five mazes in a row without returning to the entrance.")
                        goto("menu")
                    }
                    op("Got any tips that may help me?") {
                        player(CALM_TALK, "Got any tips that may help me?")
                        guardian(id, "Have a good look at the maze before you try to solve it because this can save you time and runes required to navigate the maze. Although you will still be getting magic experience for moving the statue incorrectly, you won't be progressing towards collecting Telekinetic Pizazz Points. Lastly, all the mazes can be solved in ten moves or less.")
                        goto("menu")
                    }
                    op("Thanks, bye!") {
                        player(CALM_TALK, "Thanks, bye!")
                        guardian(id, "Use what you've learned, young one.")
                    }
                }
            }
            Npc.magictraining_guard_alchem -> {
                player(CALM_TALK, "Hi.")
                guardian(id, "Greetings young one. What wisdom do you seek?")
                label("menu")
                options {
                    op("What do I have to do in this room?") {
                        guardian(id, "In this room you will see various cupboards. It is your task to search the cupboards to find items to turn into gold using your low or high alchemy spells. You must deposit the money in the receptacle at the end of the hall in order to receive your Alchemist Pizazz Points, otherwise the money will be taken from you as you leave through the portal. This money is used for the upkeep of the training arena as well as magic shops all around Gielinor. Keep an eye on the costs of each item as these will change from time-to-time, as will the location of the items. Occasionally one of the items will be indicated as costing no runestones to convert to money.")
                        goto("menu")
                    }
                    op("How will I be rewarded?") {
                        guardian(id, "You will get magic experience from casting the alchemist spells, as well as 1 Alchemist Pizazz Point for every 100 coins you deposit, and 10% of the coins you deposit will be given to you as you leave. Keep in mind that you will not be able to take more than 1000 coins back out with you.")
                        goto("menu")
                    }
                    op("Got any tips that may help me?") {
                        guardian(id, "You must remember to keep an eye on the various costs of the items. If you watch the movements of the other players, you might be able to guess which are the best places to visit. You will get 1 Pizazz Point for every 100 coins, so if you have 190 coins, why not get an extra 10?")
                        player(CALM_TALK, "I see.")
                        guardian(id, "Oh, and a word of warning: should you decide to leave this room by a method other than the exit portals, you will be teleported to the entrance and have any items that you picked up in the room removed. And that includes gold coins, for which you won't get any Pizazz Points for!")
                        goto("menu")
                    }
                    op("Thanks, bye!") {
                        guardian(id, "Use what you've learned, young one.")
                    }
                }
            }
            Npc.magictraining_guard_grave -> {
                player(CALM_TALK, "Hello.")
                guardian(id, "Greetings young one. What knowledge do you seek?")
                label("menu")
                options {
                    op("What do I have to do in this room?") {
                        player(CALM_TALK, "What do I have to do in this room?")
                        guardian(id, "Have you noticed all the bones around the room? These are teleported here from all over Gielinor to help clean up the landscape of countless bones left behind from combat. What better use for these bones than to convert them to nutritious fruit to be eaten by you mortals? You have to use your Bones to Bananas spell to convert the bones and then place them in the holes on the walls to earn Graveyard Pizazz Points. Unluckily for you, your health will constantly decrease from getting hit by these dropping bones so you will probably want to eat some of the bananas yourself to increase your stay here.")
                        goto("menu")
                    }
                    op("What are the rewards?") {
                        player(CALM_TALK, "What are the rewards?")
                        guardian(id, "You will get experience from casting your bones to bananas spell and you will get Graveyard Pizazz Points when you put the bananase through the wall. Occasionally you will also be credited with a runestone to help you in your future spell casting.")
                        goto("menu")
                    }
                    op("Got any tips that may help me?") {
                        player(CALM_TALK, "Got any tips that may help me?")
                        guardian(id, "Different bones will provide you with different numbers of bananas, so try to find the best type. Collect enough points and you will be able to buy a 'Bones to Peaches' spell from my fellow guardian above the entrance hall. This spell can be used here just like the bones to bananas spell, except this spell will give you even more experience and the peaches will restore more health!")
                        player(CALM_TALK, "I see.")
                        guardian(id, "Oh, and a word of warning: should you decide to leave this room by a method other than the exit portals, you will be teleported to the entrance and have any items that you picked up in the room removed.")
                        goto("menu")
                    }
                    op("Thanks, bye!") {
                        player(CALM_TALK, "Thanks, bye!")
                        guardian(id, "Use what you've learned, young one.")
                    }
                }
            }
            Npc.magictraining_guard_encha -> {
                player(CALM_TALK, "Hello.")
                guardian(id, "Greetings young one. How can I enlighten you?")
                label("menu")
                options {
                    op("What do I have to do in this room?") {
                        player(CALM_TALK, "What do I have to do in this room?")
                        guardian(id, "In this chamber you will see various piles of shapes. You can pick up these shapes and enchant them using the enchant jewelry spells. By enchanting these shapes, you'll be converting them into orbs which you can place in the hole in the centre. You will get Enchantment Pizazz Points for every ten shapes that you convert and the points you get depends on the level of enchantment spell you cast. You will get a bonus Pizazz Point for enchanting a certain shape at a certain time, which I will periodically shout out. You will also be rewarded with an item for every so many orbs you deposit into the hole.")
                        goto("menu")
                    }
                    op("What are the rewards?") {
                        player(CALM_TALK, "What are the rewards?")
                        guardian(id, "As weill as the magic experience from casting your enchantment spells, you will get Enchantment Pizazz Points for converting so many shapes, plus a bonus point for converting a shape of the correct type. You should also note that you will occasionally be rewarded with items when you put one of the enchanted orbs in the floor.")
                        goto("menu")
                    }
                    op("Got any tips that may help me?") {
                        player(CALM_TALK, "Got any tips that may help me?")
                        guardian(id, "Try to guess or keep track of the time between the changes of the best shape to enchant. This means you can be ready to run to a different shape when you know it is about to change. Look out for the dragon gems, as these will get you more Pizazz Points!")
                        player(CALM_TALK, "I see.")
                        guardian(id, "Oh, and a word of warning: should you decide to leave this room by a method other than the exit portals, you will be teleported to the entrance and have any items that you picked up in the room removed.")
                        goto("menu")
                    }
                    op("Thanks, bye!") {
                        player(CALM_TALK, "Thanks, bye!")
                        guardian(id, "Use what you've learned, young one.")
                    }
                }
            }
        }
    }
}

fun talkProgressHat(player: Player) {
    val greetings = arrayOf("Hello... Mr Progress Hat?", "Erm, excuse me Mr Progress Hat?", "Mr Progress Hat... sir?", "Mr Progress Hat? Hello?")
    val responses = arrayOf(
        "That's me, why are you bothering me human?" to "Sorry, but do you think you could tell me my Pizazz Points?",
        "Can't you see I'm busy?" to "But you're just a hat? Can you tell me my Pizazz Point totals?",
        "What do you want?" to "Do you think you could tell me my Pizazz Points?",
        "I suppose you want to know your Pizazz Points." to "That would be nice, yes."
    )
    val response = responses[Utils.random(responses.size)]
    player.startConversation {
        player(CALM_TALK, greetings[Utils.random(greetings.size)])
        statement(NPCStatement("Pizazz Hat", Npc.magictraining_pizzaz_hat, NONE, response.first))
        player(CALM_TALK, response.second)
        val points = ArenaRoom.entries.map { MageTrainingArena.points(player, it) }
        statement(NPCStatement("Pizazz Hat", Npc.magictraining_pizzaz_hat, NONE, "Ok, I suppose it's my job. You have: ${points[0]} Telekinetic, ${points[1]} Alchemist, ${points[2]} Enchantment, and ${points[3]} Graveyard Pizazz Points."))
        player(CALM_TALK, "Thank you!")
    }
}

fun destroyProgressHat(player: Player, slot: Int, hat: Item) {
    player.startConversation {
        statement(NPCStatement("Pizazz Hat", Npc.magictraining_pizzaz_hat, NONE, "How dare you destroy me!"))
        options("Destroy hat?") {
            op("Yes") {
                exec {
                    if (player.inventory.getItem(slot)?.id == hat.id) {
                        PluginManager.handle(DestroyItemEvent(player, hat))
                        player.inventory.deleteItem(slot, hat)
                    }
                }
                simple("The hat whimpers as you destroy it. You can get another from the Entrance Guardian.")
            }
            op("No") {
                statement(NPCStatement("Pizazz Hat", Npc.magictraining_pizzaz_hat, NONE, "I should think so too!"))
            }
        }
    }
}

fun talkMazeGuardian(player: Player, nextMaze: () -> Unit) {
    player.startConversation {
        player(CALM_TALK, "Hi!")
        guardian(Npc.magictraining_guard_maze, "Well done on releasing me. Would you like to try another maze?")
        options {
            op("Yes please!") {
                player(CALM_TALK, "Yes please!")
                npc(Npc.magictraining_guard_maze, NONE, "Very well, I shall teleport you.") { nextMaze() }
            }
            op("No thanks.") {
                player(CALM_TALK, "No thanks.")
                guardian(Npc.magictraining_guard_maze, "Very well. Talk to me if you want to move onto the next maze, or you can return to the entrance hall through the portal.")
            }
        }
    }
}

fun talkRewardsGuardian(player: Player) {
    val id = Npc.magictraining_guard_rewards
    player.startConversation {
        player(CALM_TALK, "Hi.")
        guardian(id, "Greetings. What wisdom do you seek?")
        label("menu")
        options {
            op("Who are you?") {
                player(CALM_TALK, "Who are you?")
                guardian(id, "Me? I'm here to grant you rewards for any of the Pizazz Points you may have earned in this training arena. Like my fellow guardians I am part of the arena and live to ensure its safe running.")
                player(CALM_TALK, "I see.")
                goto("menu")
            }
            op("Can I trade my Pizazz Points, please?") {
                player(CALM_TALK, "Can I trade my Pizazz Points, please?")
                if (hasProgressHat(player)) {
                    npc(id, NONE, "Why, of course.") { MageTrainingArenaRewards.open(player) }
                } else guardian(id, "Sorry, you must be carrying a Pizazz Progress Hat so I can check what points you have. If you haven't got one, talk to my fellow guardian downstairs.")
            }
            op("Got anything else I can buy?") {
                player(CALM_TALK, "Got anything else I can buy?")
                guardian(id, "Well, we do stock a special book that you may be interested in, which provides a comprehensive guide to this training arena. It costs 200gp; would you like one?")
                if (player.inventory.containsItem(Obj.magictraining_arenalorebook) || player.bank.containsItem(Obj.magictraining_arenalorebook)) {
                    player(CALM_TALK, "No thanks, I already have one.")
                    guardian(id, "Let me know if you lose it.")
                } else options {
                    op("That sounds useful. Yes please.") {
                        player(CALM_TALK, "That sounds useful. Yes please.")
                        guardian(id, "Very good, that'll be 200gp please.")
                        if (!player.inventory.containsItem(Obj.coins, 200)) {
                            simple("You can not afford that.")
                            player(CALM_TALK, "Sorry, I don't have enough money.")
                        } else if (!player.inventory.hasRoomFor(deleting = arrayOf(Item(Obj.coins, 200)), adding = arrayOf(Item(Obj.magictraining_arenalorebook)))) {
                            simple("You don't have enough inventory space.")
                        } else {
                            exec {
                                if (player.inventory.containsItem(Obj.coins, 200) && player.inventory.hasRoomFor(deleting = arrayOf(Item(Obj.coins, 200)), adding = arrayOf(Item(Obj.magictraining_arenalorebook)))) {
                                    player.inventory.deleteItem(Obj.coins, 200)
                                    player.inventory.addItem(Obj.magictraining_arenalorebook, 1)
                                }
                            }
                            guardian(id, "Thank you.")
                        }
                    }
                    op("I don't want some dusty old book.") {
                        player(CALM_TALK, "I don't want some dusty old book.")
                        guardian(id, "It's not old and it certainly isn't dusty, but suit yourself.")
                    }
                }
            }
            op("Thanks, bye!") {
                player(CALM_TALK, "Thanks, bye!")
                guardian(id, "Use what you've learned, young one.")
            }
        }
    }
}
fun talkEntranceGuardian(player: Player) {
    val introduced = player.getBool(ARENA_INTRODUCED) || hasStoredProgressHat(player)
    val id = Npc.magictraining_guard_entrance
    player.startConversation {
        player(CALM_TALK, "Hi.")
        guardian(id, "Greetings. What wisdom do you seek?")
        label("menu")
        options {
            op(if (introduced) "Can you tell me about this place again?" else "I'm new to this place. Where am I?") {
                player(CALM_TALK, if (introduced) "Can you tell me about this place again?" else "I'm new to this place. Where am I?")
                if (introduced) {
                    guardian(id, "Very well. You have entered the magic training arena. It was built at the start of the Fifth Age, when runestones were first discovered. It was made because of the many pointless accidents caused by inexperienced mages.")
                    player(CALM_TALK, "Who created it?")
                    guardian(id, "Good question. It was originally made by the ancestors of the wizards in the Wizards Tower. However, it was destroyed by melee and ranged warriors who took offence at the use of this new 'Magic Art'. Recently, the current")
                    guardian(id, "denizens of the Wizards Tower have resurrected the arena including various Guardians you will see as you look around. We are here to help and to ensure things run smoothly.")
                    player(CALM_TALK, "Interesting. So what can I do here?")
                    guardian(id, "You may train up your skill in the magic arts by travelling through one of the portals at the back of this entrance hall. By training up in one of these areas you will be awarded special Pizazz Points unique to each room. With these points you")
                    guardian(id, "may claim a variety of items from my fellow guardian up the stairs.")
                    player(CALM_TALK, "How do you record the Pizazz Points I have earned?")
                    guardian(id, "With the special Pizazz Progress Hat I gave you, of course!")
                    player(CALM_TALK, "OK.")
                    guardian(id, "Oh, and a word of warning: should you decide to leave the rooms by any method other than the exit portals, you will be teleported to the entrance and have any items that you picked up in the room removed.")
                    goto("menu")
                } else {
                    guardian(id, "Well young one, you have entered the Magic Training Arena. It was built at the start of the Fifth Age, when runestones were first discovered. It was made because of the many pointless accidents caused by inexperienced")
                    guardian(id, "mages.")
                    player(CALM_TALK, "Who created it?")
                    guardian(id, "Good question. It was originally made by the ancestors of the wizards in the Wizards Tower. However, it was destroyed by melee and ranged warriors who took offence at the use of this new 'Magic Art'. Recently, the current")
                    guardian(id, "denizens of the Wizards Tower have resurrected the arena including various Guardians you will see as you look around. We are here to help and to ensure things run smoothly.")
                    player(CALM_TALK, "Interesting. So what can I do here?")
                    guardian(id, "You may train up your skill in the magic arts by travelling through one of the portals at the back of this entrance hall. By training up in one of these areas you will be awarded special Pizazz Points unique to each room. With")
                    guardian(id, "these points you may claim a variety of items from my fellow guardian up the stairs.")
                    player(CALM_TALK, "How do you record the points I have earned?")
                    guardian(id, "You really are full of questions! You will need a special Pizazz Progress Hat! I can give you one if you so wish to train here.")
                    player(CALM_TALK, "Yes please!")
                    if (!player.inventory.hasFreeSlots()) {
                        guardian(id, "Ah, I'm afraid I can't give it to you as you don't have room to carry it!")
                    } else {
                        guardian(id, "Here you go. Talk to the hat to find out your current Pizazz Point totals.") {
                            if (!hasStoredProgressHat(player) && player.inventory.addItem(Obj.magictraining_proghat_dull, 1))
                                player.set(ARENA_INTRODUCED, true)
                        }
                        player(CALM_TALK, "Talk to it?")
                        guardian(id, "Well of course, it's a magic Pizazz Progress Hat! Mind your manners though, hats have feelings too!")
                        player(CALM_TALK, "Er... if you insist.")
                        guardian(id, "Oh, and a word of warning: should you decide to leave the rooms by any method other than the exit portals, you will be teleported to the entrance and have any items that you picked up in the room removed.")
                        player(CALM_TALK, "Can you explain the different portals?")
                        portalDialogue()
                    }
                }
            }
            if (introduced) {
                op("Can you explain the different portals?") {
                    player(CALM_TALK, "Can you explain the different portals?")
                    portalDialogue()
                }
                op("About the progress hat...") {
                    player(CALM_TALK, "About the Progress Hat...")
                    if (hasStoredProgressHat(player)) {
                        guardian(id, "Which you have stored somewhere I'm sure.")
                        player(CALM_TALK, "Yes. What's it for?")
                        guardian(id, "Collect Pizazz Points from the various areas and the hat will remember your totals. Talk to the hat at any time to find out what points you have. Go upstairs and talk to the Rewards Guardian to claim items for the points.")
                    } else {
                        guardian(id, "You want another one don't you.")
                        player(CALM_TALK, "Sorry, can I?")
                        if (!player.inventory.hasFreeSlots()) guardian(id, "Ah, I'm afraid I can't give it to you as you don't have room to carry it!")
                        else {
                            guardian(id, "Here you go. Talk to the hat to find out your current Pizazz Point totals.") {
                                if (!hasStoredProgressHat(player) && player.inventory.addItem(Obj.magictraining_proghat_dull, 1))
                                    player.set(ARENA_INTRODUCED, true)
                            }
                        }
                    }
                    goto("menu")
                }
                op("Thanks, bye!") {
                    player(CALM_TALK, "Thanks, bye!")
                    guardian(id, "Use what you have learned.")
                }
            } else op("None, I don't really care.") {
                player(CALM_TALK, "None, I don't really care.")
                guardian(id, "You won't gain with that attitude.")
            }
        }
    }
}

private fun DialogueBuilder.portalDialogue() {
    val id = Npc.magictraining_guard_entrance
    guardian(id, "They lead to four areas to train your magic: The Telekinetic Theatre, The Alchemists' Playground, The Enchanting Chamber, and The Creature Graveyard.")
    label("portals")
    options {
        op("What's the Telekinetic Theatre?") {
            player(CALM_TALK, "What's the Telekinetic Theatre?")
            guardian(id, "In there you can earn Telekinetic Pizazz Points for trade upstairs.")
            player(CALM_TALK, "What will I be doing in there?")
            guardian(id, "That depends on how much of a time-waster you are! You are required to use the Telekinetic Grab spell in order to move a statue through a maze. Casting the spell will move the statue towards you until it reaches a wall. So by")
            guardian(id, "standing on the different sides of the maze you can move the statue North, East, South or West. You will be rewarded Pizazz Points for each maze successfully solved.")
            guardian(id, "It will be clearer when you arrive and there's a Guardian through the portal that will be able to tell you more. There's a book you can buy from the Rewards Guardian upstairs that will explain it all.")
            goto("portals")
        }
        op("What's the Alchemists' Playground?") {
            player(CALM_TALK, "What's the Alchemists' Playground?")
            guardian(id, "In the playground you can earn Alchemist Pizazz Points for trading with the rewards guardian upstairs.")
            player(CALM_TALK, "What's in there?")
            guardian(id, "You'll find eight cupboards containing items you can turn to gold using the low and high alchemy spells. The money you earn will be taken from you upon leaving the playground to pay for the upkeep of this training arena")
            guardian(id, "and to help fund magic shops around Gielinor. You will be rewarded with 1 Pizazz Point for every 100 coins deposited and a percentage of the money you create. Keep in mind that you will not be able to take more than 1000 coins")
            guardian(id, "back out with you.")
            player(CALM_TALK, "Sounds simple.")
            guardian(id, "I am afraid not, for the cost of the items varies, as does their locations in the cupboards. You'll see a list of item costs that will help you decide what you should be converting to gold.")
            guardian(id, "Fear not, it will be clearer when you arrive and there's a Guardian through the portal that will be able to tell you more. You can also buy a book from the Rewards Guardian upstairs that will explain it all.")
            goto("portals")
        }
        op("What's the Enchanting Chamber?") {
            player(CALM_TALK, "What's the Enchanting Chamber?")
            guardian(id, "In here you will be able to earn Enchantment Pizazz Points for trade upstairs.")
            player(CALM_TALK, "What will I have to do?")
            guardian(id, "You will find yourself amongst various piles of shapes. You can pick up these shapes and use one of your enchanting spells upon it to morph it into an orb. You'll also see a hole in the centre of the room; put the orbs in here and for")
            guardian(id, "every 20 we will credit you with a gift. You will get Pizazz Points for every 10 shapes that you convert and the amount of points depends on the spell you use.")
            player(CALM_TALK, "What's the difference between the shapes?")
            guardian(id, "Ah, you will be notified of a different shape that will give you a bonus Pizazz Point when you convert that shape. You should also look out for dragon stones, as these will give you more Pizazz Points when you convert them.")
            guardian(id, "It will be clearer when you arrive and there's a Guardian through the portal that will be able to tell you more. You can also buy a book from the Rewards Guardian upstairs that will explain it all.")
            goto("portals")
        }
        op("What's the Creature Graveyard?") {
            player(CALM_TALK, "What's the Creature Graveyard?")
            guardian(id, "In here you will be able to earn Graveyard Pizazz Points for trade upstairs.")
            player(CALM_TALK, "But what do I have to do in there?")
            guardian(id, "Patience young one. A great many creatures die in this world and much of their remains can cause clutter. We have taken it upon ourselves to teleport many bones from these creatures into this graveyard for a use. It's up to")
            guardian(id, "you to practice your bones to bananas spell in order to put these bones into immediate use - nutritious food for monsters! Just convert the bones and put them in the holes in the walls. There is a drawback to this room")
            guardian(id, "however, the bones often fall on people so you may want to eat some of those bananas to stay alive. When people die we confiscate Pizazz Points for the effort of teleporting incompetent mages.")
            player(CALM_TALK, "What else do I get from this besides bruises?")
            guardian(id, "As well as the Pizazz Points for giving us the bananas, you will be credited with runestones for your efforts, and you may be able to save up for a new kind of spell to use in the graveyard!")
            guardian(id, "It will be clearer when you arrive and one of my fellow guardians through the portal will be able to tell you more. You can also buy a book from the Rewards Guardian upstairs that will explain it all.")
            goto("portals")
        }
        op("Thanks, bye!") {
            player(CALM_TALK, "Thanks, bye!")
            guardian(id, "Use what you have learned.")
        }
    }
}

fun talkCharmedWarrior(player: Player, id: Int) {
    player.startConversation {
        when (id) {
            Npc.magictraining_ghost_shoes -> {
                player(CALM_TALK, "Hello?")
                guardian(id, "Hey! You haven't paid your Magic Training Arena Membership money!")
                player(CALM_TALK, "You're lying. I can see right through you!")
                guardian(id, "Oh, HA HA, very funny.")
            }
            Npc.magictraining_ghost_shoes3 -> {
                player(CALM_TALK, "Is there anybody there?")
                guardian(id, "What do you think?")
            }
            Npc.magictraining_ghost_shoes2 -> {
                player(CALM_TALK, "Hello?")
                guardian(id, "Can't you see I'm busy?")
                player(CALM_TALK, "Well, I can't really see YOU.")
            }
            Npc.magictraining_ghost_shoes4 -> {
                player(CALM_TALK, "Is there anybody there?")
                guardian(id, "Wooo wooo! Be afraid for I'm a scary ghost. Wooo!")
                player(CALM_TALK, "Er, whatever.")
            }
        }
    }
}
