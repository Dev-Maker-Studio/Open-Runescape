package com.rs.game.content.quests.sea_slug.npcs

import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private const val ASKED_FOR_PASTE = "askedHolgartForPaste"
private const val BOAT_TRAVEL_FADE_DELAY = 4

@ServerStartupEvent
fun mapHolgartSeaSlug() {
    onNpcClick(Npc.holgartland, Npc.holgartplatform, Npc.holgartsunkboat) { (player, npc) ->
        if (!player.validateSeaSlugNpcOwnership(npc)) return@onNpcClick
        when (npc.id) {
            Npc.holgartland -> player.talkToBeachHolgart(npc)
            Npc.holgartplatform -> player.talkToPlatformHolgart(npc)
            Npc.holgartsunkboat -> player.talkToIslandHolgart(npc)
        }
    }
}

private fun Player.talkToBeachHolgart(npc: NPC) {
    val stage = getQuestStage(Quest.SEA_SLUG)
    if (stage == STAGE_UNSTARTED) {
        startConversation {
            player(CALM, "Hello.")
            npc(npc, HAPPY_TALKING, "Beautiful day, isn't it? Nothing like the smell of the sea air.")
        }
        return
    }
    if (stage == STAGE_FIND_HOLGART) {
        talkToHolgartAboutBoat(npc)
        return
    }
    if (isQuestStarted(Quest.SLUG_MENACE) && !isQuestComplete(Quest.SALT_IN_WOUND)) {
        startConversation {
            npc(npc, CALM_TALK, "Jeb is handling trips to the Fishing Platform at the moment, m'hearty.")
        }
        return
    }
    startConversation {
        player(CALM, "Hello Holgart.")
        npc(npc, CALM_TALK, "Hello again land lover. There's some strange goings on, on that platform, I tell you.")
        options {
            op("Will you take me there?") {
                player(CALM, "Will you take me there?")
                npc(npc, HAPPY_TALKING, "Of course m'hearty. If that's what you want.") {
                    travelByBoat(PLATFORM_ARRIVAL, "You arrive at the fishing platform.")
                }
            }
            op("I'm keeping away from there.") {
                player(CALM, "I'm keeping away from there.")
                npc(npc, CALM_TALK, "Fair enough m'hearty.")
            }
        }
    }
}

private fun Player.talkToHolgartAboutBoat(npc: NPC) {
    val askedBefore = questManager.getAttribs(Quest.SEA_SLUG).getB(ASKED_FOR_PASTE)
    val hasPaste = inventory.containsItem(Obj.swamppaste)
    startConversation {
        if (!askedBefore) {
            player(CALM, "Hello.")
            npc(npc, HAPPY_TALKING, "Hello m'hearty.")
            player(CALM, "I would like a ride on your boat to the fishing platform.")
            npc(npc, SAD, "I'm afraid it isn't sea worthy, it's full of holes. To fill the holes I'll need some swamp paste.")
            player(CONFUSED, "Swamp paste?")
            npc(npc, CALM_TALK, "Yes, swamp tar mixed with flour and heated over a fire.") {
                questManager.getAttribs(Quest.SEA_SLUG).setB(ASKED_FOR_PASTE, true)
            }
            if (hasPaste) {
                npc(npc, HAPPY_TALKING, "In fact, unless me nose be mistaken, you've got some in yer pack.")
                player(CALM, "Oh yes, I forgot about that stuff. Can you use it?")
                npc(npc, HAPPY_TALKING, "Aye ${getPronoun("lad", "lass")}. That be perfect.")
                simple("You give Holgart the swamp paste.") { repairHolgartsBoat(npc) }
            } else {
                player(CONFUSED, "Where can I find swamp tar?")
                npc(npc, CALM_TALK, "Unfortunately the only supply of swamp tar is in the swamps below Lumbridge. It's too far for an old man like me to travel. If you make me some swamp paste I'll give you a ride in my boat.")
                player(CALM, "I'll see what I can do.")
            }
        } else {
            player(CALM, "Hello Holgart.")
            npc(npc, CALM_TALK, "Hello m'hearty. Did you manage to make some swamp paste?")
            if (hasPaste) {
                player(HAPPY_TALKING, "Yes, I have some here.")
                simple("You give Holgart the swamp paste.") { repairHolgartsBoat(npc) }
            } else {
                player(SAD, "I'm afraid not.")
                npc(npc, CALM_TALK, "It's simply swamp tar mixed with flour heated over a fire. Unfortunately the only supply of swamp tar is in the swamps below Lumbridge. I can't fix my row boat without it.")
                player(CALM, "Ok, I'll try to find some.")
            }
        }
    }
}

private fun Player.repairHolgartsBoat(npc: NPC) {
    if (!inventory.containsItem(Obj.swamppaste)) return
    inventory.deleteItem(Obj.swamppaste, 1)
    lock(11)
    schedule("sea_slug_boat_repair") {
        try {
            interfaceManager.setFadingInterface(Interface.interface_115)
            wait(2)
            npc.forceTalk("Right me hearty. Let's get this boat fixed.")
            wait(3)
            if (getQuestStage(Quest.SEA_SLUG) != STAGE_FIND_HOLGART) return@schedule
            npc.forceTalk("Huzzah! We've fixed the leaky old tub.")
            setQuestStage(Quest.SEA_SLUG, STAGE_BOAT_REPAIRED)
            interfaceManager.setFadingInterface(Interface.interface_170)
            wait(4)
        } finally {
            interfaceManager.closeFadingInterface()
            unlock()
        }
    }
}

private fun Player.talkToPlatformHolgart(npc: NPC) {
    when (getQuestStage(Quest.SEA_SLUG)) {
        STAGE_FOUND_KENNITH -> startConversation {
            player(WORRIED, "Holgart, something strange is going on here.")
            npc(npc, WORRIED, "You're telling me, none of the sailors seem to remember who I am.")
            player(CALM, "Apparently Kennith's father left for help a couple of days ago.")
            npc(npc, WORRIED, "That's a worry, no-one's heard from him on shore. Come on, we'd better go look for him.") {
                travelByBoat(ISLAND_ARRIVAL, "You arrive on a small island.")
            }
        }

        STAGE_KENNITH_RESCUED -> startConversation {
            player(CALM, "Did you get the kid back to shore?")
            npc(npc, HAPPY_TALKING, "Yes, he's safe and sound with his parents. Your turn to return to land now adventurer.")
            player(HAPPY_TALKING, "Looking forward to it.") {
                travelByBoat(BEACH_ARRIVAL, "The boat arrives at Witchaven.")
            }
        }

        else -> startConversation {
            player(CALM, "Hey, Holgart.")
            npc(npc, WORRIED, "Have you had enough of this place yet? It's really starting to scare me.")
            options {
                op("No, I'm going to stay a while.") {
                    player(CALM, "No, I'm going to stay a while.")
                    npc(npc, WORRIED, "Okay... you're the boss.")
                }
                op("Okay, let's go back.") {
                    player(CALM, "Okay, let's go back.")
                    npc(npc, CALM_TALK, "Okay m'hearty, jump on.") {
                        travelByBoat(BEACH_ARRIVAL, "The boat arrives at Witchaven.")
                    }
                }
            }
        }
    }
}

private fun Player.talkToIslandHolgart(npc: NPC) {
    if (getQuestStage(Quest.SEA_SLUG) < STAGE_RESCUE_KENNITH) {
        startConversation {
            player(CONFUSED, "Where are we?")
            npc(npc, CALM_TALK, "Someway off mainland still. You'd better see if me old matey's okay.")
        }
    } else {
        startConversation {
            player(CALM, "We'd better get back to the platform so we can see what's going on.")
            npc(npc, WORRIED, "You're right. It all sounds pretty creepy.") {
                travelByBoat(PLATFORM_ARRIVAL, "You arrive at the fishing platform.")
            }
        }
    }
}

internal fun Player.travelByBoat(destination: Tile, arrivalMessage: String) {
    lock(12)
    schedule("sea_slug_boat_travel") {
        try {
            interfaceManager.setFadingInterface(Interface.interface_115)
            wait(BOAT_TRAVEL_FADE_DELAY)
            tele(destination)
            wait(2)
            interfaceManager.setFadingInterface(Interface.interface_170)
            sendMessage(arrivalMessage)
            refreshSeaSlugState()
            wait(4)
        } finally {
            interfaceManager.closeFadingInterface()
            unlock()
        }
    }
}
