package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.ownedNpc
import com.rs.plugin.kts.overrideNpcLOS

private const val TEMP_PRISON_KING = "grandTreePrisonKing"

private val PRISON_EXIT = Tile.of(2465, 3501, 3)
private val PRISON_KING_SPAWN = Tile.of(2465, 3495, 3)

@ServerStartupEvent
fun mapCharlieGrandTree() {
    overrideNpcLOS(Npc.npc_1621, Npc.grandtree_charlie)
    onNpcClick(Npc.npc_1621, Npc.grandtree_charlie, options = arrayOf("Talk-to")) { (player, npc) -> charlieGrandTreeDialogue(player, npc) }
}

private fun charlieGrandTreeDialogue(player: Player, npc: NPC) {
    when (player.grandTreeStage) {
        STAGE_FIND_CHARLIE -> firstInterrogation(player, npc)
        STAGE_SEARCH_GLOUGHS_HOME -> if (player.grandTreeAttribs().getB(ATTR_ARRESTED))
            charliePrisonEscapeDialogue(player)
        else
            afterInterrogation(player, npc)
        STAGE_ESCAPED_PRISON -> shipyardReminder(player, npc)
        STAGE_REACHED_SHIPYARD -> shipyardReminder(player, npc)
        STAGE_LUMBER_ORDER -> if (player.grandTreeAttribs().getB(ATTR_WARNED_KING_ABOUT_FLEET))
            chestKeyClue(player, npc)
        else
            afterShipyardReminder(player, npc)
        STAGE_GLOUGHS_KEY -> chestKeyClueReminder(player, npc)
        STAGE_INVASION_PLANS -> plansFound(player, npc)
        STAGE_OPENED_TRAPDOOR -> player.startConversation { simple("The prisoner is in no mood to talk.") }
        else -> player.startConversation {
            player(HeadE.HAPPY_TALKING, "Hello Charlie.")
            npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "Hello adventurer.")
        }
    }
}

private fun firstInterrogation(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.SKEPTICAL, "Tell me. Why would you want to kill the Grand Tree?")
        npc(npc, HeadE.CONFUSED, "What do you mean?!")
        player(HeadE.SKEPTICAL, "Don't tell me, you just happened to be caught carrying Daconia rocks!")
        npc(npc, HeadE.WORRIED, "All I know is that I did what I was asked.")
        player(HeadE.CONFUSED, "I don't understand.")
        npc(npc, HeadE.WORRIED, "Glough paid me to go to this gnome on a hill. I gave the gnome a seal and he gave me some rocks to give to Glough.")
        npc(npc, HeadE.WORRIED, "I've been doing it for weeks, this time though Glough locked me up here! I just don't understand it.")
        player(HeadE.WORRIED, "Sounds like Glough is hiding something!")
        npc(npc, HeadE.CALM_TALK, "I don't know what he's up to. If you want to find out you'd better search his home.")
        player(HeadE.HAPPY_TALKING, "OK. Thanks Charlie.")
        npc(npc, HeadE.HAPPY_TALKING, "Good luck!") { player.setGrandTreeStage(STAGE_SEARCH_GLOUGHS_HOME) }
    }
}

private fun afterInterrogation(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello Charlie.")
        npc(npc, HeadE.HAPPY_TALKING, "Hello adventurer. Have you figured out what's going on?")
        player(HeadE.SAD_MILD_LOOK_DOWN, "No idea.")
        npc(npc, HeadE.CALM_TALK, "To get to the bottom of this you'll need to search Glough's home.")
    }
}

internal fun charliePrisonEscapeDialogue(player: Player) {
    player.startConversation {
        npc(Npc.grandtree_charlie, HeadE.WORRIED, "So they got you as well?")
        player(HeadE.WORRIED, "It's Glough! He's trying to cover something up.")
        npc(Npc.grandtree_charlie, HeadE.WORRIED, "I shouldn't tell you this adventurer.")
        npc(Npc.grandtree_charlie, HeadE.CALM_TALK, "But if you want to get to the bottom of this you should go and talk to the Karamja Shipyard foreman.")
        player(HeadE.CONFUSED, "Why?")
        npc(Npc.grandtree_charlie, HeadE.CALM_TALK, "Glough sent me to Karamja to meet him. I delivered a large amount of gold. For what? I don't know. He may be able to tell you what Glough's up to.")
        npc(Npc.grandtree_charlie, HeadE.WORRIED, "You'll find him in the Karamja Shipyard, east of Shilo village. Be careful! If he discovers you're not working for Glough there'll be trouble! The sea men use the password Ka-Lu-Min.")
        player(HeadE.HAPPY_TALKING, "Thanks Charlie!") { kingPrisonReleaseDialogue(player) }
    }
}

private fun spawnPrisonKing(player: Player): OwnedNPC {
    val existing = player.tempAttribs.getO<OwnedNPC>(TEMP_PRISON_KING)
    if (existing != null && !existing.hasFinished())
        return existing
    return ownedNpc(player, Npc.grandtree_narnode, PRISON_KING_SPAWN) {
        setRandomWalk(false)
        setCantInteract(true)
    }.also {
        player.tempAttribs.setO<OwnedNPC>(TEMP_PRISON_KING, it)
    }
}

private fun kingPrisonReleaseDialogue(player: Player) {
    val king = spawnPrisonKing(player)
    player.startConversation {
        npc(king, HeadE.SAD_MILD_LOOK_DOWN, "Traveller please accept my apologies! Glough had no right to arrest you! I just think he's scared of humans. Let me get you out of there.")
        player(HeadE.WORRIED, "I don't think you can trust Glough, Your Highness. He seems to have an unnatural hatred for humans.")
        npc(king, HeadE.CALM_TALK, "I know he can be a bit extreme at times. But he's the best tree guardian I have, he has made the gnomes paranoid about humans though.")
        npc(king, HeadE.WORRIED, "I'm afraid Glough has placed guards on the front gate to stop you escaping! Let my glider pilot fly you away until things calm down around here.")
        player(HeadE.CALM_TALK, "Well, OK.")
        npc(king, HeadE.SAD_MILD_LOOK_DOWN, "I'm sorry again Traveller!") {
            player.grandTreeAttribs().setB(ATTR_ARRESTED, false)
            player.setGrandTreeStage(STAGE_ESCAPED_PRISON)
            player.tele(PRISON_EXIT)
            king.finish()
            player.tempAttribs.removeO<OwnedNPC>(TEMP_PRISON_KING)
        }
    }
}

private fun shipyardReminder(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.CONFUSED, "I can't figure this out Charlie!")
        npc(npc, HeadE.CALM_TALK, "Go and see the foreman in the Karamja jungle, there's a shipyard there, you might find some clues. Don't forget the password is Ka-Lu-Min; if they realise that you're not working for Glough there'll be trouble!")
    }
}

private fun afterShipyardReminder(player: Player, npc: NPC) {
    player.startConversation {
        if (!player.grandTreeAttribs().getB(ATTR_CONFRONTED_GLOUGH_ABOUT_FLEET))
            npc(npc, HeadE.CALM_TALK, "You should confront Glough with what you found at the shipyard.")
        else
            npc(npc, HeadE.CALM_TALK, "Tell King Narnode what you found at the shipyard. He needs to know what Glough is planning.")
    }
}

private fun chestKeyClue(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.CALM_TALK, "How are you doing Charlie?")
        npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "I've been better.")
        player(HeadE.WORRIED, "Glough has some plan to rule Gielinor!")
        npc(npc, HeadE.SKEPTICAL, "I wouldn't put it past him, the Gnome's crazy!")
        player(HeadE.WORRIED, "I need some proof to convince the King.")
        npc(npc, HeadE.CALM_TALK, "Hmm...you could be in luck! Before Glough had me locked up I heard him mention that he'd left his chest keys at his girlfriend's.")
        player(HeadE.CONFUSED, "Where does she live?")
        npc(npc, HeadE.CALM_TALK, "Just west of the toad swamp.")
        player(HeadE.HAPPY_TALKING, "OK, I'll see what I can find.") { player.grandTreeAttribs().setB(ATTR_CHARLIE_REVEALED_KEY, true) }
    }
}

private fun chestKeyClueReminder(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.CALM_TALK, "Glough's girlfriend lives just west of the toad swamp. His chest key should be there.")
    }
}

private fun plansFound(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "You were right! I searched Glough's chest and found some invasion plans!")
        npc(npc, HeadE.HAPPY_TALKING, "Go to the King and tell him! Finally you have proof. Surely King Narnode will belive you now and set me free!")
        player(HeadE.HAPPY_TALKING, "Hold tight Charlie!")
        npc(npc, HeadE.CALM_TALK, "I'm not going anywhere!")
    }
}
