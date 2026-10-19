package com.rs.game.content.quests.deserttreasure

import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.shop.ShopsHandler
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val DRINK_OPENED_ATTR = "desertTreasureBartenderDrink"

private fun banditCampBartenderDialogue(player: Player, npc: NPC) {
    val stage = player.questManager.getStage(Quest.DESERT_TREASURE)
    if (stage != STAGE_TALK_TO_BARTENDER) {
        ShopsHandler.openShop(player, "the_big_heist_lodge")
        return
    }

    val questAttrs = player.questManager.getAttribs(Quest.DESERT_TREASURE)
    val hasStartedBartenderConversation = questAttrs.getB(DRINK_OPENED_ATTR)

    player.startConversation {
        if (!hasStartedBartenderConversation) {
            npc(npc, FRUSTRATED, "If you're not buying anything, I have nothing to say to you.")
            options {
                op("Ask about Desert Treasure") {
                    player(CALM_TALK, "As I understand it there's some kind of hidden treasure in these parts...")
                    npc(npc, FRUSTRATED, "Look around ${if (player.appearance.isMale) "pal" else "lady"}. Does it look like there's any treasure near here?")
                    npc(npc, ANGRY, "If I were you I'd get lost before someone takes a dislike to your face and removes it for you.")
                }
                op("Buy a drink") {
                    player(CALM_TALK, "I'd like to buy a drink.")
                    if (!player.inventory.hasCoins(650)) {
                        npc(npc, FRUSTRATED, "You ain't got 650 coins. Come back when you do.")
                    } else if (!player.inventory.hasFreeSlots()) {
                        npc(npc, SHAKING_HEAD, "You ain't got room to carry it, and I'm glad 'cos I didn't want to serve you anyway.")
                    } else {
                        npc(npc, HAPPY_TALKING, "There you go.") {
                            player.inventory.removeCoins(650)
                            player.inventory.addItem(Obj.bandit_brew, 1)
                            questAttrs.setB(DRINK_OPENED_ATTR, true)
                        }
                    }
                }
            }
            return@startConversation
        }

        npc(npc, FRUSTRATED, "You've had your drink, now get out of here. I have nothing to say to the-")
        player(CALM_TALK, "No, wait! Look, I'm here on an archaeological expedition for the Museum of Varrock.")
        player(CALM_TALK, "I am only here looking for artefacts...")
        npc(npc, CALM_TALK, "Oh really? Our inheritance is only sand and death. What do you expect to find out here in this desert forsaken by the gods?")
        options {
            op("I heard about treasure...") {
                player(CALM_TALK, "As I understand it there's some kind of hidden treasure in these parts...")
                npc(npc, FRUSTRATED, "Look around ${if (player.appearance.isMale) "pal" else "lady"}. Does it look like there's any treasure near here?")
                npc(npc, ANGRY, "If I were you I'd get lost before someone takes a dislike to your face and removes it for you.")
            }
            op("I heard about four diamonds...") {
                player(CALM_TALK, "I heard a rumour about four diamonds or crystals...")
                npc(npc, AMAZED, "The four diamonds of Azzanadra? How did you hear about them?")
                player(CALM_TALK, "You've heard of them then?")
                npc(npc, CALM_TALK, "It's just a fairy tale for children. Maybe one of the village elders might know more, but it's not really something I care about.")
                npc(npc, FRUSTRATED, "Now get out of here, your sort isn't welcome in my bar.")
            }
            op("I heard about a fortress...") {
                player(CALM_TALK, "Certain things have led me to believe there may be some kind of ruined fortress around here...")
                npc(npc, SKEPTICAL_THINKING, "Doubt it. What in the world would someone need to guard against in the middle of this desert?")
                npc(npc, FRUSTRATED, "A bad attack of sand? I think you're on the wrong track here, stranger.")
            }
            op("I've heard of the Diamonds of Azzanadra.") {
                player(CALM_TALK, "Tell me all you know about the Diamonds of Azzanadra.")
                npc(npc, CALM_TALK, "Not that I think it's any of your business, but when I was a child I remember hearing the legend.")
                npc(npc, CALM_TALK, "I don't recall it particularly well, other than they are said to contain an incredible power.")
                npc(npc, CALM_TALK, "If you really want to hear more about it you'd be best off finding someone who cares about the past, and the history of this area, and stop bothering me.")
                exec {
                    questAttrs.setB(DRINK_OPENED_ATTR, false)
                    player.setQuestStage(Quest.DESERT_TREASURE, STAGE_TALK_TO_EBLIS)
                }
            }
        }
    }
}

@ServerStartupEvent
fun mapBanditCampBartenderDesertTreasure() {
    onNpcClick(Npc.fourdiamonds_bartender) { (player, npc) -> banditCampBartenderDialogue(player, npc) }
}
