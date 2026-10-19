package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.content.transportation.FadingScreen
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

private const val ATTR_FEMI_HELPED = "femiHelped"
private const val ATTR_FEMI_MET = "femiMet"

private val GRAND_TREE_INSIDE = Tile.of(2465, 3494, 0)

@ServerStartupEvent
fun mapFemiGrandTree() {
    onNpcClick(Npc.grandtree_femi, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            options {
                if (player.grandTreeStage in STAGE_LUMBER_ORDER until STAGE_FIND_DACONIA)
                    op("Talk about The Grand Tree.") { exec { femiGrandTreeDialogue(player, npc) } }
                op("Talk about the boxes.") { exec { femiNormalDialogue(player, npc) } }
                op("Never mind.")
            }
        }
    }
}

private fun femiNormalDialogue(player: Player, npc: NPC) {
    val attribs = player.grandTreeAttribs()
    if (attribs.getB(ATTR_FEMI_MET)) {
        player.startConversation { simple("The little gnome is too busy to talk.") }
        return
    }
    player.startConversation {
        npc(npc, HeadE.HAPPY_TALKING, "Hello there.")
        player(HeadE.HAPPY_TALKING, "Hi!")
        npc(npc, HeadE.WORRIED, "Could you help me lift this barrel? It's really heavy!")
        options {
            op("Sorry, I'm a bit busy.") {
                player(HeadE.SAD_MILD_LOOK_DOWN, "Sorry, I'm a bit busy.")
                npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "Oh, OK, I'll do it myself.") { attribs.setB(ATTR_FEMI_MET, true) }
            }
            op("OK then.") {
                player(HeadE.HAPPY_TALKING, "OK then.")
                npc(npc, HeadE.HAPPY_TALKING, "Thanks traveller!")
                simple("You help the gnome lift the barrel.")
                simple("It's very heavy and quite hard work.")
                npc(npc, HeadE.HAPPY_TALKING, "Thanks again friend!") {
                    attribs.setB(ATTR_FEMI_MET, true)
                    attribs.setB(ATTR_FEMI_HELPED, true)
                }
            }
        }
    }
}

private fun femiGrandTreeDialogue(player: Player, npc: NPC) {
    player.startConversation {
        player(HeadE.FRUSTRATED, "I can't believe they won't let me in!")
        npc(npc, HeadE.CALM_TALK, "I don't believe all this rubbish about an invasion. If mankind wanted to, they could have invaded before now.")
        player(HeadE.WORRIED, "I really need to see King Narnode. Could you help sneak me in?")
        if (player.grandTreeAttribs().getB(ATTR_FEMI_HELPED)) {
            npc(npc, HeadE.WORRIED, "Well, as you helped me I suppose I could. We'll have to be careful. If I get caught I'll be in the cage!")
            player(HeadE.CONFUSED, "OK, what should I do?")
            npc(npc, HeadE.CALM_TALK, "Jump in the back of the cart. It's a food delivery, we should be fine.")
            exec { sneakInside(player) }
        } else {
            npc(npc, HeadE.FRUSTRATED, "Why should I help you, you wouldn’t help me!")
            player(HeadE.NERVOUS, "Erm I know, but this is an emergency!")
            npc(npc, HeadE.FRUSTRATED, "So was lifting that barrel!")
            npc(npc, HeadE.CALM_TALK, "Tell you what, call it a round 1000 gold pieces.")
            player(HeadE.AMAZED_MILD, "1000 gold pieces!")
            npc(npc, HeadE.CALM_TALK, "That’s right, 1000 and I’ll sneak you in.")
            options {
                op("No chance!") { player(HeadE.FRUSTRATED, "No chance!") }
                op("OK then, here you go.") {
                    player(HeadE.CALM_TALK, "Okay then, here you go.")
                    if (!player.inventory.containsItem(Obj.coins, 1000)) {
                        simple("You can not afford that.")
                        player(HeadE.SAD_MILD_LOOK_DOWN, "Oops, I'll have to come back.")
                    } else {
                        simple("You give Femi 1000 coins.") { player.inventory.deleteItem(Obj.coins, 1000) }
                        npc(npc, HeadE.HAPPY_TALKING, "Alright, jump in the back of the cart. It's a food delivery, we should be fine.")
                        exec { sneakInside(player) }
                    }
                }
            }
        }
    }
}

private fun sneakInside(player: Player) {
    player.startConversation {
        simple("You hide in the back of Femi's food cart.")
        simple("Femi covers you with a sheet...")
        simple("...and drags the cart to the gate.")
        exec {
            FadingScreen.fade(player, 3) {
                player.tele(GRAND_TREE_INSIDE)
                player.startConversation {
                    simple("Femi pulls you into the Grand Tree.")
                    npc(Npc.grandtree_femi, HeadE.CALM_TALK, "OK traveller, you'd better get going.")
                    player(HeadE.HAPPY_TALKING, "Thanks again!")
                    npc(Npc.grandtree_femi, HeadE.HAPPY_TALKING, "That's OK, all the best.")
                }
            }
        }
    }
}
