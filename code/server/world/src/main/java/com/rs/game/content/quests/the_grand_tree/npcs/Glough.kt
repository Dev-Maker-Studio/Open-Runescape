package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private val CHARLIE_CELL = Tile.of(2464, 3496, 3)

@ServerStartupEvent
fun mapGloughGrandTree() {
    onNpcClick(Npc.grandtree_glough, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            options {
                if (player.grandTreeStage in STAGE_DACONIA_WARNING..STAGE_OPENED_TRAPDOOR || player.grandTreeStage == STAGE_COMPLETE)
                    op("Talk about The Grand Tree.") { exec { gloughGrandTreeDialogue(player, npc) } }
                op("Say hello.") { exec { hostileGlough(player, npc) } }
                op("Never mind.")
            }
        }
    }
}

private fun gloughGrandTreeDialogue(player: Player, npc: NPC) {
    when (player.grandTreeStage) {
        STAGE_DACONIA_WARNING, STAGE_FIND_GLOUGH -> firstWarning(player, npc)
        STAGE_SEARCH_GLOUGHS_HOME -> accuseGlough(player, npc)
        STAGE_LUMBER_ORDER -> fleetAccusation(player, npc)
        STAGE_GLOUGHS_KEY, STAGE_INVASION_PLANS -> missingKeys(player, npc)
        STAGE_COMPLETE -> postQuestGlough(player, npc)
        else -> hostileGlough(player, npc)
    }
}

private fun firstWarning(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello.") { player.setGrandTreeStage(STAGE_FIND_GLOUGH) }
        simple("The gnome is munching on a worm hole.")
        npc(npc, HeadE.FRUSTRATED, "Can I help human? Can't you see I'm eating?!")
        simple("The gnome continues to eat.")
        player(HeadE.WORRIED, "The King asked me to inform you that the Daconia rocks have been taken!")
        npc(npc, HeadE.AMAZED_MILD, "Surely not!")
        player(HeadE.CALM_TALK, "Apparently a human took them from Hazelmere. Hazelmere believed him; he had the King's seal!")
        npc(npc, HeadE.ANGRY, "I should've known! The humans are going to invade!")
        player(HeadE.FRUSTRATED, "Never!")
        npc(npc, HeadE.ANGRY, "Your type can't be trusted! I'll take care of this! Go back to the King.") { player.setGrandTreeStage(STAGE_GLOUGH_BLAMES_HUMANS) }
    }
}

private fun hostileGlough(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello there!")
        npc(npc, HeadE.ANGRY, "You shouldn't be here human!")
        player(HeadE.CONFUSED, "What do you mean?")
        npc(npc, HeadE.ANGRY, "The Gnome Stronghold is for gnomes alone!")
        player(HeadE.AMAZED_MILD, "Surely not!")
        npc(npc, HeadE.ANGRY, "We don't need your sort round here!")
        simple("He doesn't seem very nice!")
    }
}

private fun accuseGlough(player: Player, npc: NPC) {
    if (!player.inventory.containsOneItem(Obj.grandtree_journal)) {
        player.sendMessage("You should search Glough's home before confronting him.")
        return
    }
    player.startConversation {
        player(HeadE.ANGRY, "Glough! I don't know what you're up to but I know you paid Charlie to get those rocks!")
        npc(npc, HeadE.ANGRY, "You're a fool human! You have no idea what's going on.")
        player(HeadE.ANGRY, "I know the Grand Tree's dying! And I think you're part of the reason.")
        npc(npc, HeadE.ANGRY, "How dare you accuse me! I'm the head tree guardian!")
        npc(npc, HeadE.ANGRY, "Guards! Guards!")
        npc(Npc.gnomekingsguard, HeadE.ANGRY, "Come with me, human!") {
            player.grandTreeAttribs().setB(ATTR_ARRESTED, true)
            player.tele(CHARLIE_CELL.transform(0, -1))
            charliePrisonEscapeDialogue(player)
        }
    }
}

private fun fleetAccusation(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.ANGRY, "I know what you're up to Glough!")
        npc(npc, HeadE.ANGRY, "You have no idea human!")
        player(HeadE.ANGRY, "You may be able to make a fleet but the tree gnomes will never follow you into battle against humans.")
        npc(npc, HeadE.ANGRY, "So, you know more than I thought! The gnomes fear humanity more than any other race. I just need to give them a push in the right direction. There's nothing you can do traveller! Leave before it's too late!")
        player(HeadE.ANGRY, "King Narnode won't allow it!")
        npc(npc, HeadE.ANGRY, "The King's a fool and a coward! He'll bow to me! You'll soon be back in that cage!") {
            player.grandTreeAttribs().setB(ATTR_CONFRONTED_GLOUGH_ABOUT_FLEET, true)
        }
    }
}

private fun missingKeys(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.ANGRY, "I'm going to stop you, Glough!")
        npc(npc, HeadE.ANGRY, "You're becoming quite annoying traveller!")
        simple("Glough is searching his pockets.")
        npc(npc, HeadE.FRUSTRATED, "Where are those darn keys?")
        npc(npc, HeadE.ANGRY, "Leave human, before I have you put in the cage!")
    }
}

private fun postQuestGlough(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.CALM_TALK, "Hello?")
        npc(npc, HeadE.ANGRY, "I warn you now, human: stay out of my affairs!")
        player(HeadE.ANGRY, "I've stopped you before, Glough, and I'll stop you again!")
    }
}
