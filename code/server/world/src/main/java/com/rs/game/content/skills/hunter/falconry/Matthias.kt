package com.rs.game.content.skills.hunter.falconry

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

private val BIRD_FREQUENCY = Ticks.fromSeconds(15)
private var posts: List<GameObject> = emptyList()

@ServerStartupEvent
fun mapMatthias() {
    posts = World.getAllObjectsInChunkRange(Tile.of(2374, 3605, 0).getChunkId(), 2)
        .filter { it.id == Loc.matthias_falconry_postempty || it.id == Loc.matthias_falconry_post }

    instantiateNpc(Npc.hunting_npc_falconer, Npc.hunting_npc_falconer_nobird) { _, tile -> Matthias(tile) }

    onNpcClick(Npc.hunting_npc_falconer, Npc.hunting_npc_falconer_nobird) { (player, npc, option) ->
        when (option) {
            "Talk-to" -> player.startConversation {
                player(HeadE.CHEERFUL, "Hello there.")
                npc(npc.id, HeadE.CONFUSED, "Greetings. Can I help you at all?")
                options {
                    op("Do you have any quests I could do?") {
                        npc(npc.id, HeadE.CONFUSED, "A quest? What a strange notion. Do you normally go around asking complete strangers for quests?")
                        player(HeadE.SKEPTICAL, "Er, yes, now that you come to mention it.")
                        npc(npc.id, HeadE.CHEERFUL, "Oh, ok then. Well, no, I don't. Sorry.")
                    }
                    op("What is this place?") {
                        npc(npc.id, HeadE.CHEERFUL, "A good question; straight and to the point.")
                        npc(npc.id, HeadE.CHEERFUL, "My name is Matthias, I am a falconer, and this is where I train my birds.")
                        options {
                            opExec("That sounds like fun; could I have a go?") { FalconryController.beginFalconry(player) }
                            op("That doesn't sound like my sort of thing.") {
                                npc(npc.id, HeadE.CALM_TALK, "Fair enough; it does require a great deal of patience and skill, so I can understand if you might feel intimidated.")
                            }
                            op("What's this falconry thing all about then?") {
                                npc(npc.id, HeadE.CHEERFUL, "Well, some people see it as a sport, although such a term does not really convey the amount of patience and dedication to be profiecient at the task.")
                                npc(npc.id, HeadE.CHEERFUL, "Putting it simply, it is the training and use of birds of prey in hunting quarry.")
                            }
                        }
                    }
                }
            }
            "Falconry" -> FalconryController.beginFalconry(player)
        }
    }
}

private class Matthias(tile: Tile) : NPC(Npc.hunting_npc_falconer, tile) {
    override fun processNPC() {
        super.processNPC()
        if (tickCounter % BIRD_FREQUENCY == 0L)
            toggleBird()
    }

    private fun toggleBird() {
        val hasBird = id == Npc.hunting_npc_falconer
        val post = posts.filter { it.id == if (hasBird) Loc.matthias_falconry_postempty else Loc.matthias_falconry_post }.randomOrNull() ?: return
        resetWalkSteps()
        faceObject(post)
        freeze()
        if (hasBird) {
            if (Utils.random(10) == 0)
                forceTalk("Gouge 'em, Valor!")
            transformIntoNPC(Npc.hunting_npc_falconer_nobird)
            setBas(1)
            World.sendProjectile(this, post, Spotanim.falcon_travel, 41 to 16, 31, 5, 16, 0) {
                post.changeId(Loc.matthias_falconry_post)
                unfreeze()
            }
        } else {
            if (Utils.random(10) == 0)
                forceTalk("Valor, to me!")
            post.changeId(Loc.matthias_falconry_postempty)
            World.sendProjectile(post, this, Spotanim.falcon_travel, 41 to 16, 31, 5, 16, 0) {
                transformIntoNPC(Npc.hunting_npc_falconer)
                setBas(-1)
                unfreeze()
            }
        }
    }
}
