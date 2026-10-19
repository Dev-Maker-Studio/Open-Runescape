package com.rs.game.content.quests.underground_pass.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.underground_pass.STAGE_FOUND_DWARVES
import com.rs.game.content.quests.underground_pass.STAGE_PASSED_PALADINS
import com.rs.game.content.quests.underground_pass.advanceUndergroundPass
import com.rs.game.content.quests.underground_pass.giveUniqueQuestItem
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private const val KAMEN_FOOD_PRICE = 75
private const val RECEIVED_NILOOF_FOOD_ATTR = "receivedNiloofFood"

@ServerStartupEvent
fun mapUndergroundPassDwarves() {
    onNpcClick(Npc.npc_994, Npc.npc_995, Npc.npc_996) { (player, npc) ->
        player.startConversation {
            when (npc.id) {
                Npc.npc_994 -> {
                    val attributes = player.questManager.getAttribs(Quest.UNDERGROUND_PASS)
                    val needsIntroduction = !player.isQuestComplete(Quest.UNDERGROUND_PASS) &&
                        (player.getQuestStage(Quest.UNDERGROUND_PASS) == STAGE_PASSED_PALADINS ||
                            !attributes.getB(RECEIVED_NILOOF_FOOD_ATTR))
                    if (needsIntroduction) {
                        npc(npc, SCARED, "Back away! Back away! ....Wait. ...You're human!")
                        player(CALM_TALK, "That's right, I'm on a quest for King Lathas. We need to find a way through these caverns.")
                        npc(npc, HAPPY_TALKING, "Ha ha, listen up. We came here as miners decades ago, completely unaware of the evil that lurked here. There's no way through, not while Iban still rules. He controls the gateway, the only way to the other side.")
                        player(CONFUSED, "What gateway?")
                        npc(npc, CALM_TALK, "It once stood as the 'Well of Voyage'. A gateway to the West. Now Iban's moulded it into a pit of the damned, a portal to Zamorak's darkest realms.")
                        npc(npc, SAD, "He sends his followers there, never to return. Only once Iban is destroyed can the well be restored.")
                        player(CONFUSED, "But how?")
                        npc(npc, SAD, "If I knew, I would have slain him already. Seek out the Witch, his guide, his only confidante. Only she knows how to rid us of Iban.")
                        npc(npc, CALM_TALK, "She lives on the platforms above, we dare not go there. Here, take some food to aid your journey.")
                        exec {
                            player.inventory.addItemDrop(Obj.meat_pie, 2)
                            player.inventory.addItemDrop(Obj.meat_pizza, 1)
                            attributes.setB(RECEIVED_NILOOF_FOOD_ATTR, true)
                            player.advanceUndergroundPass(STAGE_FOUND_DWARVES)
                            player.sendMessage("Niloof gives you some food...")
                        }
                        player(HAPPY_TALKING, "Thanks Niloof, take care.")
                        npc(npc, HAPPY_TALKING, "You too.")
                    } else {
                        player(CALM_TALK, "Hello Niloof.")
                        npc(npc, CALM_TALK, "So you still live, not many survive down here.")
                        player(SAD, "As I can see.")
                        npc(npc, SAD, "Don't stay too long traveller. Iban's calls will soon penetrate your delicate human mind.")
                        npc(npc, SAD, "You'll also become one of his minions... You must go above and find the witch Kardia. She holds the secret to Iban's destruction.")
                    }
                    if (player.isQuestStarted(Quest.UNDERGROUND_PASS) &&
                        !player.isQuestComplete(Quest.UNDERGROUND_PASS) &&
                        player.vars.getVarBit(Varbit.upass_gave_cat) == 1 &&
                        !player.hasQuestItem(Obj.obj_1494)
                    ) {
                        npc(npc, CALM_TALK, "You have lost Kardia's book. Take this replacement and keep it safe.")
                        exec { player.giveUniqueQuestItem(Obj.obj_1494) }
                    }
                }

                Npc.npc_995 -> {
                    npc(npc, CALM_TALK, "Those cages near the temple are vicious things. You'll want stout gloves before putting your hands inside.")
                    if (!player.inventory.containsItem(Obj.klanks_gauntlets)) {
                        npc(npc, HAPPY_TALKING, "Here, take my spare pair.")
                        exec { player.giveUniqueQuestItem(Obj.klanks_gauntlets) }
                    }
                }

                Npc.npc_996 -> {
                    exec {
                        player.sendMessage("He looks a little drunk.")
                        npc.forceTalk("Hic!")
                    }
                    player(CALM_TALK, "Hi there, you okay?")
                    npc(npc, DRUNK, "Ooooh, my head ...I'm fried.")
                    player(CONFUSED, "What's wrong?")
                    npc(npc, DRUNK, "Too much of this home brew my friend. We make it from plant roots, but it blows your head off.")
                    npc(npc, DRUNK, "You don't wanna put it near any naked flames. Want some?")
                    options {
                        op("Okay then.") {
                            player(CALM_TALK, "Okay then.")
                            npc(npc, DRUNK, "Here you go... Hic!")
                            exec {
                                player.forceTalk("Aaarrgghh!")
                                player.applyHit(Hit(50, Hit.HitLook.TRUE_DAMAGE))
                                player.skills.drainLevel(Skills.AGILITY, 3)
                                player.sendMessage("The dwarf kindly gives you some food to help you recover from the ghastly drink.")
                                player.giveKamenFood()
                            }
                            npc(npc, HAPPY_TALKING, "Ha ha! I warned you - it's strong stuff. Have a good meal; it may sober you up a bit.")
                        }
                        op("No thanks.") {
                            player(CALM_TALK, "No thanks.")
                            npc(npc, DRUNK, "Your losh... Hic!")
                            npc(npc, DRUNK, "Well maybe a good meal? I could part with some food for a few coins.")
                            options {
                                op("Okay then.") {
                                    player(CALM_TALK, "Okay then.")
                                    npc(npc, DRUNK, "75 coins pleashe.")
                                    if (player.inventory.hasCoins(KAMEN_FOOD_PRICE)) {
                                        exec { player.inventory.removeCoins(KAMEN_FOOD_PRICE) }
                                        player(CALM_TALK, "Here you go.")
                                        exec { player.giveKamenFood() }
                                        npc(npc, HAPPY_TALKING, "Thanks... Eat well.")
                                    } else {
                                        player(SAD, "Oh dear, I haven't enough money...")
                                        exec { player.sendMessage("You can not afford that.") }
                                        npc(npc, SAD, "Sorry, I can't just give it away to anyone that walks past.")
                                    }
                                }
                                op("No thanks.") {
                                    player(CALM_TALK, "No thanks.")
                                    npc(npc, CALM_TALK, "Well come back any time.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Player.giveKamenFood() {
    inventory.addItemDrop(Obj.stew, 1)
    inventory.addItemDrop(Obj.bread, 1)
    inventory.addItemDrop(Obj.meat_pie, 1)
}

private fun Player.hasQuestItem(itemId: Int): Boolean =
    inventory.containsItem(itemId) || bank.containsItem(itemId)
