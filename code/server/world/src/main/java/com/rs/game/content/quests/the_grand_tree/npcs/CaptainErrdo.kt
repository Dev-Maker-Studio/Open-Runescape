package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player

fun captainErrdoDialogue(player: Player, npc: NPC) {
    when (player.grandTreeStage) {
        STAGE_ESCAPED_PRISON -> grandTreeFlight(player, npc)
        STAGE_REACHED_SHIPYARD -> karamjaReminder(player, npc)
        STAGE_COMPLETE -> player.interfaceManager.sendInterface(Interface.interface_138)
        else -> normalErrdoDialogue(player, npc)
    }
}

private fun grandTreeFlight(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.HAPPY_TALKING, "Hi. The king said that you need to leave?")
        player(HeadE.WORRIED, "Apparently, humans are invading!")
        npc(npc, HeadE.SKEPTICAL, "I find that hard to believe. I have lots of human friends.")
        player(HeadE.CONFUSED, "I don't understand it either!")
        npc(npc, HeadE.CALM_TALK, "So, where to?")
        exec { player.interfaceManager.sendInterface(Interface.interface_138) }
    }
}

fun confirmGrandTreeQuestFlight(player: Player, travel: Runnable): Boolean {
    if (player.grandTreeStage != STAGE_ESCAPED_PRISON || player.plane != 3 ||
        player.x !in 2460..2470 || player.y !in 3498..3508)
        return false
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Take me to Karamja, please.")
        npc(Npc.npc_3811, HeadE.HAPPY_TALKING, "Okay, you're the boss! Hold on tight, it'll be a rough ride.")
        simple("You fly on the glider.") { travel.run() }
    }
    return true
}

fun finishGrandTreeQuestFlight(player: Player) {
    if (player.grandTreeStage != STAGE_ESCAPED_PRISON)
        return
    player.setGrandTreeStage(STAGE_REACHED_SHIPYARD)
    arrivalDialogue(player)
}

private fun arrivalDialogue(player: Player) {
    player.startConversation {
        simple("A crashing sound echoes through the jungle.")
        npc(Npc.npc_3811, HeadE.SAD_MILD_LOOK_DOWN, "Sorry about that.")
        npc(Npc.npc_3811, HeadE.WORRIED, "That turbulence over the Karamja volcano was a bit unexpected, and the area around here isn't well suited for an emergency landing. Still, we're alive, that's the main thing. Are you okay?")
        player(HeadE.CALM_TALK, "I'm fine. I can't say the same for your glider!")
        npc(Npc.npc_3811, HeadE.SAD_MILD_LOOK_DOWN, "I don't think I can fix this. Looks like we'll be heading back by foot. I might see if I can find Penwie while I'm here. I believe he's charting the area.")
        player(HeadE.CONFUSED, "Where's the shipyard from here?")
        npc(Npc.npc_3811, HeadE.CALM_TALK, "I think I saw some buildings on the coast east of here while we were crashing. I'd have a look there.")
        npc(Npc.npc_3811, HeadE.HAPPY_TALKING, "Take care, adventurer.")
        player(HeadE.HAPPY_TALKING, "Take care.")
    }
}

private fun karamjaReminder(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.CONFUSED, "Where am I meant to be going?")
        npc(npc, HeadE.CALM_TALK, "The shipyard is east of here. I thought I saw some buildings on the coast while we were crashing...")
    }
}

private fun normalErrdoDialogue(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.HAPPY_TALKING, "Hello, human. Welcome to Gnome Air.")
        options {
            op("What's Gnome Air?") {
                player(HeadE.CONFUSED, "What's Gnome Air?")
                npc(npc, HeadE.HAPPY_TALKING, "Gnome Air is the finest airline in Gielinor!")
                npc(npc, HeadE.CALM_TALK, "Well, it's the only real airline in Gielinor.")
                npc(npc, HeadE.LAUGH, "Ha!")
                player(HeadE.CONFUSED, "What do you mean by 'real airline'?")
                npc(npc, HeadE.LAUGH, "Well there's a dodgy magic carpet operation in the desert. I doubt it will ever take off. Ha!")
            }
            op("Where can you take me?") {
                player(HeadE.CONFUSED, "Where can you take me?")
                npc(npc, HeadE.CALM_TALK, "Glough has ordered that I only take gnomes.")
            }
            op("How much for a one-way to Varrock?") {
                player(HeadE.CONFUSED, "How much for a one-way to Varrock?")
                npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "I can't take you anywhere.")
                player(HeadE.CONFUSED, "How come?")
                npc(npc, HeadE.CALM_TALK, "I would, but Glough has told me not to take humans.")
            }
            op("Never mind.") { player(HeadE.CALM_TALK, "Never mind.") }
        }
    }
}
