package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.content.quests.the_grand_tree.npcs.spawnGrandTreeBlackDemon
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val GLOUGH_TUNNEL_ENTRANCE = Tile.of(2490, 9863, 0)

@ServerStartupEvent
fun mapGloughsTrapdoor() {
    onObjectClick(Loc.grandtree_trapdoorclosed) { (player) ->
        when {
            player.grandTreeStage < STAGE_OPENED_TRAPDOOR -> player.sendMessage("The trapdoor will not open.")
            player.grandTreeStage == STAGE_OPENED_TRAPDOOR -> {
                player.tele(GLOUGH_TUNNEL_ENTRANCE)
                player.startConversation {
                    player(HeadE.CONFUSED, "Hello?")
                    player(HeadE.CONFUSED, "Anybody?")
                    player(HeadE.CONFUSED, "Glough?")
                    npc(Npc.grandtree_glough, HeadE.ANGRY, "You really are becoming a headache! Well, at least now you can die knowing you were right, it will save me having to hunt you down like all the other human filth of Gielinor!")
                    player(HeadE.ANGRY, "You're crazy, Glough!")
                    npc(Npc.grandtree_glough, HeadE.ANGRY, "Bah! Well, soon you'll see, the gnomes are ready to fight, in three weeks this tree will be dead wood, in ten weeks it will be 30 battleships! Finally we will rid the world of the disease called humanity!")
                    player(HeadE.ANGRY, "What makes you think I'll let you get away with it?")
                    npc(Npc.grandtree_glough, HeadE.ANGRY, "Fool...meet my little friend!") { spawnGrandTreeBlackDemon(player) }
                }
            }
            else -> player.tele(GLOUGH_TUNNEL_ENTRANCE)
        }
    }
}
